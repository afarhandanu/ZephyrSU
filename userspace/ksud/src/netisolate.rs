use crate::ksu_uapi;
use crate::{ksucalls, utils};
use anyhow::{Context, Error, Result, bail};
use log::{info, warn};
use serde::{Deserialize, Serialize};
use std::fs::{File, OpenOptions};
use std::io::{Read, Write};
use std::net::{Ipv4Addr, Ipv6Addr};
use std::os::fd::AsRawFd;
use std::os::unix::fs::{DirBuilderExt, MetadataExt, OpenOptionsExt, PermissionsExt};

const CONFIG_PATH: &str = "/data/adb/ksu/netisolate.json";
const LOCK_PATH: &str = "/data/adb/ksu/.netisolate.lock";
const WORKING_DIR: &str = "/data/adb/ksu";
const MAX_INPUT_BYTES: usize = 64 * 1024;
const FLAG_MASK: u32 = ksu_uapi::KSU_NETISOLATE_FLAG_MASK;
const SUPPORTED_SHIFT: u32 = ksu_uapi::KSU_NETISOLATE_SUPPORTED_SHIFT;
const STRICT_SOCKET_FLAG: u32 = ksu_uapi::KSU_NETISOLATE_FLAG_STRICT_SOCKET;
const ALLOW_LOOPBACK_FLAG: u32 = ksu_uapi::KSU_NETISOLATE_FLAG_ALLOW_LOOPBACK;
const ALLOW_PRIVATE_NETWORK_FLAG: u32 = ksu_uapi::KSU_NETISOLATE_FLAG_ALLOW_PRIVATE_NETWORK;
const ALLOW_LOCAL_DISCOVERY_FLAG: u32 = ksu_uapi::KSU_NETISOLATE_FLAG_ALLOW_LOCAL_DISCOVERY;
const LAN_ALLOW_FLAGS: u32 = ALLOW_PRIVATE_NETWORK_FLAG | ALLOW_LOCAL_DISCOVERY_FLAG;

#[derive(Debug, Clone, clap::Subcommand)]
pub enum Command {
    /// Read network isolation state
    Status,
    /// Validate and apply a JSON configuration from stdin
    Set,
    /// Enable the persisted configuration
    Enable,
    /// Disable isolation while retaining the UID list
    Disable,
    /// Disable isolation and remove persisted state
    Clear,
    /// Reapply the persisted configuration
    Apply,
    /// Read recent blocked attempts from the kernel ring
    Events,
    /// Clear recent blocked attempts without changing isolation state
    EventsClear,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
#[allow(clippy::struct_excessive_bools)]
struct ConfigFile {
    enabled: bool,
    uids: Vec<u32>,
    #[serde(default)]
    strict_socket: bool,
    #[serde(default)]
    allow_loopback: bool,
    #[serde(default)]
    allow_private_network: bool,
    #[serde(default)]
    allow_local_discovery: bool,
    #[serde(default)]
    policies: Option<Vec<NetIsolatePolicy>>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
struct NetIsolatePolicy {
    uid: u32,
    strict_socket: bool,
    allow_loopback: bool,
}

const fn empty_config() -> ConfigFile {
    ConfigFile {
        enabled: false,
        uids: Vec::new(),
        strict_socket: false,
        allow_loopback: false,
        allow_private_network: false,
        allow_local_discovery: false,
        policies: Some(Vec::new()),
    }
}

struct ConfigLock(File);

impl Drop for ConfigLock {
    fn drop(&mut self) {
        unsafe {
            libc::flock(self.0.as_raw_fd(), libc::LOCK_UN);
        }
    }
}

fn ensure_root() -> Result<()> {
    if unsafe { libc::geteuid() } != 0 {
        bail!("Root is required for network isolation management");
    }
    Ok(())
}

fn ensure_working_dir() -> Result<()> {
    match std::fs::symlink_metadata(WORKING_DIR) {
        Ok(metadata) => {
            if !metadata.is_dir() || metadata.uid() != 0 || metadata.mode() & 0o022 != 0 {
                bail!("Unsafe network isolation working directory");
            }
        }
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
            std::fs::DirBuilder::new()
                .mode(0o755)
                .create(WORKING_DIR)
                .context("Create network isolation working directory")?;
        }
        Err(error) => return Err(error).context("Inspect network isolation working directory"),
    }
    Ok(())
}

fn acquire_lock() -> Result<ConfigLock> {
    ensure_working_dir()?;
    let file = OpenOptions::new()
        .read(true)
        .write(true)
        .create(true)
        .truncate(false)
        .mode(0o600)
        .custom_flags(libc::O_NOFOLLOW)
        .open(LOCK_PATH)
        .context("Open network isolation lock")?;
    let metadata = file.metadata().context("Inspect network isolation lock")?;
    if !metadata.is_file() || metadata.uid() != 0 || metadata.mode() & 0o077 != 0 {
        bail!("Unsafe network isolation lock");
    }
    if unsafe { libc::flock(file.as_raw_fd(), libc::LOCK_EX | libc::LOCK_NB) } != 0 {
        bail!("Network isolation is busy; retry later");
    }
    Ok(ConfigLock(file))
}

fn normalize(mut config: ConfigFile) -> Result<ConfigFile> {
    config.uids.sort_unstable();
    if config.uids.len() > ksu_uapi::KSU_NETISOLATE_MAX_UIDS as usize {
        bail!(
            "Network isolation supports at most {} UIDs",
            ksu_uapi::KSU_NETISOLATE_MAX_UIDS
        );
    }
    if config.uids.windows(2).any(|uids| uids[0] == uids[1]) {
        bail!("Network isolation UIDs must be unique");
    }
    if config.uids.contains(&0) {
        bail!("UID 0 is permanently exempt and cannot be isolated");
    }
    if strict_conflict(&config) {
        bail!("Strict socket mode cannot be combined with an allow exemption");
    }

    let mut policies = if let Some(policies) = config.policies.take() {
        policies
    } else {
        let flags = requested_flags(&config);
        config
            .uids
            .iter()
            .map(|&uid| policy_from_flags(uid, flags))
            .collect()
    };
    if policies.len() > ksu_uapi::KSU_NETISOLATE_MAX_POLICIES as usize {
        bail!(
            "Network isolation supports at most {} policies",
            ksu_uapi::KSU_NETISOLATE_MAX_POLICIES
        );
    }
    policies.sort_unstable_by_key(|policy| policy.uid);
    if policies.windows(2).any(|pair| pair[0].uid == pair[1].uid) {
        bail!("Network isolation policy UIDs must be unique");
    }
    if policies.iter().any(|policy| policy.uid == 0) {
        bail!("UID 0 is permanently exempt and cannot be isolated");
    }
    if policies.iter().any(|policy| {
        policy.strict_socket
            && (policy.allow_loopback
                || config.allow_private_network
                || config.allow_local_discovery)
    }) {
        bail!("Strict socket mode cannot be combined with an allow exemption");
    }
    let policy_uids: Vec<u32> = policies.iter().map(|policy| policy.uid).collect();
    if policy_uids != config.uids {
        bail!("Network isolation policies must match the UID list exactly");
    }
    if config.enabled && config.uids.is_empty() {
        bail!("Network isolation requires at least one UID when enabled");
    }
    let common = common_policy_flags(&policies);
    config.strict_socket = common & STRICT_SOCKET_FLAG != 0;
    config.allow_loopback = common & ALLOW_LOOPBACK_FLAG != 0;
    config.policies = Some(policies);
    Ok(config)
}

const fn requested_flags(config: &ConfigFile) -> u32 {
    let mut flags = 0;

    if config.strict_socket {
        flags |= STRICT_SOCKET_FLAG;
    }
    if config.allow_loopback {
        flags |= ALLOW_LOOPBACK_FLAG;
    }
    if config.allow_private_network {
        flags |= ALLOW_PRIVATE_NETWORK_FLAG;
    }
    if config.allow_local_discovery {
        flags |= ALLOW_LOCAL_DISCOVERY_FLAG;
    }
    flags
}

const fn lan_allow_flags(config: &ConfigFile) -> u32 {
    requested_flags(config) & LAN_ALLOW_FLAGS
}

const fn strict_conflict(config: &ConfigFile) -> bool {
    config.strict_socket
        && (config.allow_loopback || config.allow_private_network || config.allow_local_discovery)
}

const fn effective_policy_flags(config: &ConfigFile, policy: &NetIsolatePolicy) -> u32 {
    policy_flags(policy) | lan_allow_flags(config)
}

const fn policy_flags(policy: &NetIsolatePolicy) -> u32 {
    let mut flags = 0;

    if policy.strict_socket {
        flags |= STRICT_SOCKET_FLAG;
    }
    if policy.allow_loopback {
        flags |= ALLOW_LOOPBACK_FLAG;
    }
    flags
}

const fn policy_from_flags(uid: u32, flags: u32) -> NetIsolatePolicy {
    NetIsolatePolicy {
        uid,
        strict_socket: flags & STRICT_SOCKET_FLAG != 0,
        allow_loopback: flags & ALLOW_LOOPBACK_FLAG != 0,
    }
}

fn common_policy_flags(policies: &[NetIsolatePolicy]) -> u32 {
    let Some(first) = policies.first() else {
        return 0;
    };
    policies[1..]
        .iter()
        .fold(policy_flags(first), |common, policy| {
            common & policy_flags(policy)
        })
}

fn common_kernel_policy_flags(policies: &[ksu_uapi::ksu_netisolate_policy]) -> u32 {
    let Some(first) = policies.first() else {
        return 0;
    };
    policies[1..]
        .iter()
        .fold(first.flags & FLAG_MASK, |common, policy| {
            common & policy.flags
        })
}

fn policies_have_same_flags(policies: &[NetIsolatePolicy]) -> bool {
    policies.first().is_none_or(|first| {
        policies
            .iter()
            .all(|policy| policy_flags(policy) == policy_flags(first))
    })
}

const fn kernel_flags(config: &ksu_uapi::ksu_netisolate_config) -> (u32, u32) {
    (
        config.reserved & FLAG_MASK,
        (config.reserved >> SUPPORTED_SHIFT) & FLAG_MASK,
    )
}

const fn normalize_snapshot(
    mut snapshot: ksu_uapi::ksu_netisolate_config,
) -> ksu_uapi::ksu_netisolate_config {
    snapshot.reserved &= FLAG_MASK;
    snapshot
}

struct KernelSnapshot {
    legacy: Option<ksu_uapi::ksu_netisolate_config>,
    policies: Option<ksu_uapi::ksu_netisolate_policies>,
}

fn supported_flags(snapshot: &KernelSnapshot) -> u32 {
    snapshot
        .policies
        .as_ref()
        .map(|kernel| kernel.supported_flags & FLAG_MASK)
        .or_else(|| {
            snapshot
                .legacy
                .as_ref()
                .map(|kernel| kernel_flags(kernel).1)
        })
        .unwrap_or(0)
}

fn config_with_supported_flags(mut config: ConfigFile, supported: u32) -> ConfigFile {
    config.strict_socket &= supported & STRICT_SOCKET_FLAG != 0;
    config.allow_loopback &= supported & ALLOW_LOOPBACK_FLAG != 0;
    config.allow_private_network &= supported & ALLOW_PRIVATE_NETWORK_FLAG != 0;
    config.allow_local_discovery &= supported & ALLOW_LOCAL_DISCOVERY_FLAG != 0;
    if let Some(policies) = config.policies.as_mut() {
        for policy in policies {
            policy.strict_socket &= supported & STRICT_SOCKET_FLAG != 0;
            policy.allow_loopback &= supported & ALLOW_LOOPBACK_FLAG != 0;
        }
    }
    config
}

fn ensure_policy_flags_supported(config: &ConfigFile, snapshot: &KernelSnapshot) -> Result<()> {
    let policies = config.policies.as_deref().unwrap_or_default();
    if let Some(kernel) = snapshot.policies.as_ref() {
        let supported = kernel.supported_flags & FLAG_MASK;
        if requested_flags(config) & !supported != 0
            || policies
                .iter()
                .any(|policy| effective_policy_flags(config, policy) & !supported != 0)
        {
            bail!("Kernel does not support the requested network isolation flags");
        }
        return Ok(());
    }

    if !policies_have_same_flags(policies) {
        bail!("Kernel does not support per-app network isolation policies");
    }
    let legacy = snapshot
        .legacy
        .as_ref()
        .context("Kernel does not support network isolation")?;
    let supported = kernel_flags(legacy).1;
    if requested_flags(config) & !supported != 0
        || policies
            .iter()
            .any(|policy| effective_policy_flags(config, policy) & !supported != 0)
    {
        bail!("Kernel does not support the requested network isolation flags");
    }
    Ok(())
}

fn kernel_unsupported(error: &Error) -> bool {
    error.chain().any(|cause| {
        cause
            .downcast_ref::<std::io::Error>()
            .and_then(std::io::Error::raw_os_error)
            .is_some_and(|errno| errno == libc::EOPNOTSUPP || errno == libc::ENOTTY)
    })
}

fn read_limited<R: Read>(reader: R, limit: usize, label: &str) -> Result<Vec<u8>> {
    let mut bytes = Vec::new();
    reader
        .take((limit + 1) as u64)
        .read_to_end(&mut bytes)
        .with_context(|| format!("Read {label}"))?;
    if bytes.len() > limit {
        bail!("{label} is too large");
    }
    Ok(bytes)
}

fn read_config() -> Result<Option<ConfigFile>> {
    let file = match OpenOptions::new()
        .read(true)
        .custom_flags(libc::O_NOFOLLOW | libc::O_NONBLOCK)
        .open(CONFIG_PATH)
    {
        Ok(file) => file,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return Ok(None),
        Err(error) => return Err(error).context("Open network isolation config"),
    };
    let metadata = file
        .metadata()
        .context("Inspect network isolation config")?;
    if !metadata.is_file() || metadata.uid() != 0 || metadata.mode() & 0o077 != 0 {
        bail!("Unsafe network isolation config permissions");
    }
    let bytes = read_limited(file, MAX_INPUT_BYTES, "network isolation config")?;
    let config = serde_json::from_slice(&bytes).context("Parse network isolation config")?;
    normalize(config).map(Some)
}

fn parse_input() -> Result<ConfigFile> {
    let bytes = read_limited(
        std::io::stdin().lock(),
        MAX_INPUT_BYTES,
        "network isolation input",
    )?;
    let config = serde_json::from_slice(&bytes).context("Parse network isolation input")?;
    normalize(config)
}

fn write_config(config: &ConfigFile) -> Result<()> {
    ensure_working_dir()?;
    let bytes = serde_json::to_vec_pretty(config).context("Serialize network isolation config")?;
    let mut temp = tempfile::NamedTempFile::new_in(WORKING_DIR)
        .context("Create network isolation temp file")?;
    temp.as_file()
        .set_permissions(std::fs::Permissions::from_mode(0o600))
        .context("Set network isolation temp permissions")?;
    temp.write_all(&bytes)
        .context("Write network isolation temp file")?;
    temp.as_file()
        .sync_all()
        .context("Sync network isolation temp file")?;
    temp.persist(CONFIG_PATH)
        .map_err(|error| error.error)
        .context("Persist network isolation config")?;
    File::open(WORKING_DIR)
        .context("Open network isolation directory")?
        .sync_all()
        .context("Sync network isolation directory")?;
    Ok(())
}

fn remove_config() -> Result<()> {
    match std::fs::remove_file(CONFIG_PATH) {
        Ok(()) => {}
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => return Ok(()),
        Err(error) => return Err(error).context("Remove network isolation config"),
    }
    File::open(WORKING_DIR)
        .context("Open network isolation directory")?
        .sync_all()
        .context("Sync network isolation directory")?;
    Ok(())
}

fn legacy_kernel_config(config: &ConfigFile) -> ksu_uapi::ksu_netisolate_config {
    let mut output = unsafe { std::mem::zeroed::<ksu_uapi::ksu_netisolate_config>() };
    output.enabled = u32::from(config.enabled);
    output.count = config.uids.len() as u32;
    output.reserved = requested_flags(config);
    output.uids[..config.uids.len()].copy_from_slice(&config.uids);
    output
}

fn kernel_policies(config: &ConfigFile) -> ksu_uapi::ksu_netisolate_policies {
    let mut output = unsafe { std::mem::zeroed::<ksu_uapi::ksu_netisolate_policies>() };
    let policies = config.policies.as_deref().unwrap_or_default();
    output.enabled = u32::from(config.enabled);
    output.count = policies.len() as u32;
    for (slot, policy) in output.policies.iter_mut().zip(policies) {
        slot.uid = policy.uid;
        slot.flags = effective_policy_flags(config, policy);
    }
    output
}

fn legacy_readback_matches(
    request: &ksu_uapi::ksu_netisolate_config,
    readback: &ksu_uapi::ksu_netisolate_config,
) -> bool {
    let count = request.count as usize;
    let readback_count = readback.count as usize;
    let max = ksu_uapi::KSU_NETISOLATE_MAX_UIDS as usize;

    count <= max
        && readback_count <= max
        && readback.enabled == request.enabled
        && readback.count == request.count
        && readback.uids[..count] == request.uids[..count]
        && kernel_flags(readback).0 == request.reserved & FLAG_MASK
}

fn policies_readback_matches(
    request: &ksu_uapi::ksu_netisolate_policies,
    readback: &ksu_uapi::ksu_netisolate_policies,
) -> bool {
    let count = request.count as usize;
    let readback_count = readback.count as usize;
    let max = ksu_uapi::KSU_NETISOLATE_MAX_POLICIES as usize;

    count <= max
        && readback_count <= max
        && readback.enabled == request.enabled
        && readback.count == request.count
        && (0..count).all(|index| {
            readback.policies[index].uid == request.policies[index].uid
                && readback.policies[index].flags == request.policies[index].flags
        })
}

fn apply_legacy_kernel_config(kernel: &ksu_uapi::ksu_netisolate_config) -> Result<()> {
    ksucalls::set_netisolate_config(kernel)?;
    let readback = ksucalls::get_netisolate_config()?;
    if !legacy_readback_matches(kernel, &readback) {
        bail!("Kernel network isolation config readback mismatch");
    }
    Ok(())
}

fn apply_kernel_policies(kernel: &ksu_uapi::ksu_netisolate_policies) -> Result<()> {
    ksucalls::set_netisolate_policies(kernel)?;
    let readback = ksucalls::get_netisolate_policies()?;
    if !policies_readback_matches(kernel, &readback) {
        bail!("Kernel network isolation policy readback mismatch");
    }
    Ok(())
}

fn read_kernel_snapshot() -> Result<KernelSnapshot> {
    let legacy = match ksucalls::get_netisolate_config() {
        Ok(config) => Some(config),
        Err(error) if kernel_unsupported(&error) => None,
        Err(error) => return Err(error.context("Read network isolation state")),
    };
    let policies = if legacy.is_some() {
        match ksucalls::get_netisolate_policies() {
            Ok(config) => Some(config),
            Err(error) if kernel_unsupported(&error) => None,
            Err(error) => return Err(error.context("Read network isolation policies")),
        }
    } else {
        None
    };
    Ok(KernelSnapshot { legacy, policies })
}

fn apply_config_to_kernel(config: &ConfigFile, snapshot: &KernelSnapshot) -> Result<()> {
    ensure_policy_flags_supported(config, snapshot)?;
    if snapshot.policies.is_some() {
        apply_kernel_policies(&kernel_policies(config))
    } else {
        apply_legacy_kernel_config(&legacy_kernel_config(config))
    }
}

fn restore(snapshot: &KernelSnapshot) -> Result<()> {
    snapshot.policies.as_ref().map_or_else(
        || {
            snapshot.legacy.as_ref().map_or(Ok(()), |legacy| {
                apply_legacy_kernel_config(&normalize_snapshot(*legacy))
            })
        },
        apply_kernel_policies,
    )
}

fn persist_with_rollback(config: &ConfigFile, snapshot: &KernelSnapshot) -> Result<()> {
    apply_config_to_kernel(config, snapshot)?;
    if let Err(error) = write_config(config) {
        if let Err(rollback) = restore(snapshot) {
            bail!("{error:#}; residual=netisolate-kernel:{rollback:#}");
        }
        return Err(error);
    }
    Ok(())
}

fn status() -> Result<()> {
    let legacy = match ksucalls::get_netisolate_config() {
        Ok(config) => Some(config),
        Err(error) if kernel_unsupported(&error) => None,
        Err(error) => return Err(error.context("Read network isolation status")),
    };
    let policies_kernel = if legacy.is_some() {
        match ksucalls::get_netisolate_policies() {
            Ok(config) => Some(config),
            Err(error) if kernel_unsupported(&error) => None,
            Err(error) => return Err(error.context("Read network isolation policies")),
        }
    } else {
        None
    };
    let supported = legacy.is_some();
    let policy_supported = policies_kernel.is_some();
    let enabled = policies_kernel.as_ref().map_or_else(
        || legacy.as_ref().is_some_and(|config| config.enabled != 0),
        |config| config.enabled != 0,
    );
    let compat_supported = legacy
        .as_ref()
        .is_some_and(|config| config.compat_supported != 0);
    let (supported_flags, policies, active_flags) =
        match (policies_kernel.as_ref(), legacy.as_ref()) {
            (Some(config), _) => {
                let count =
                    (config.count as usize).min(ksu_uapi::KSU_NETISOLATE_MAX_POLICIES as usize);
                let active = common_kernel_policy_flags(&config.policies[..count]);
                (
                    config.supported_flags & FLAG_MASK,
                    config.policies[..count]
                        .iter()
                        .map(|policy| policy_from_flags(policy.uid, policy.flags))
                        .collect::<Vec<_>>(),
                    active,
                )
            }
            (None, Some(config)) => {
                let count = (config.count as usize).min(ksu_uapi::KSU_NETISOLATE_MAX_UIDS as usize);
                let active = kernel_flags(config).0;
                (
                    kernel_flags(config).1,
                    config.uids[..count]
                        .iter()
                        .map(|&uid| policy_from_flags(uid, active))
                        .collect::<Vec<_>>(),
                    active,
                )
            }
            (None, None) => (0, Vec::new(), 0),
        };
    let common = common_policy_flags(&policies);
    let uids = policies.iter().map(|policy| policy.uid).collect::<Vec<_>>();
    println!(
        "{}",
        serde_json::json!({
            "supported": supported,
            "enabled": enabled,
            "compat_supported": compat_supported,
            "policy_supported": policy_supported,
            "strict_socket_supported": supported_flags & STRICT_SOCKET_FLAG != 0,
            "strict_socket": common & STRICT_SOCKET_FLAG != 0,
            "allow_loopback_supported": supported_flags & ALLOW_LOOPBACK_FLAG != 0,
            "allow_loopback": common & ALLOW_LOOPBACK_FLAG != 0,
            "allow_private_network_supported":
                supported_flags & ALLOW_PRIVATE_NETWORK_FLAG != 0,
            "allow_private_network": active_flags & ALLOW_PRIVATE_NETWORK_FLAG != 0,
            "allow_local_discovery_supported":
                supported_flags & ALLOW_LOCAL_DISCOVERY_FLAG != 0,
            "allow_local_discovery": active_flags & ALLOW_LOCAL_DISCOVERY_FLAG != 0,
            "uids": uids,
            "policies": policies,
        })
    );
    Ok(())
}

const NI_CONNECT: u8 = 0;
const NI_SENDTO: u8 = 1;
const NI_SENDMSG: u8 = 2;
const NI_SENDMMSG: u8 = 3;
const NI_SOCKET: u8 = 4;

const fn operation_name(operation: u8) -> &'static str {
    match operation {
        NI_CONNECT => "connect",
        NI_SENDTO => "sendto",
        NI_SENDMSG => "sendmsg",
        NI_SENDMMSG => "sendmmsg",
        NI_SOCKET => "socket",
        _ => "unknown",
    }
}

const fn family_name(family: u8) -> &'static str {
    match family {
        value if value == libc::AF_INET as u8 => "inet",
        value if value == libc::AF_INET6 as u8 => "inet6",
        _ => "unknown",
    }
}

fn address_text(family: u8, operation: u8, address: &[u8; 16]) -> String {
    if operation == NI_SOCKET {
        return String::new();
    }
    match family {
        value if value == libc::AF_INET as u8 => {
            Ipv4Addr::new(address[0], address[1], address[2], address[3]).to_string()
        }
        value if value == libc::AF_INET6 as u8 => Ipv6Addr::from(*address).to_string(),
        _ => String::new(),
    }
}

fn event_value(event: &ksu_uapi::ksu_netisolate_event) -> serde_json::Value {
    serde_json::json!({
        "timestamp_ns": event.timestamp_ns,
        "uid": event.uid,
        "operation": operation_name(event.operation),
        "family": family_name(event.family),
        "address": address_text(event.family, event.operation, &event.address),
        "port": event.port,
    })
}

fn events_value(events: &ksu_uapi::ksu_netisolate_events) -> serde_json::Value {
    let count = (events.count as usize).min(ksu_uapi::KSU_NETISOLATE_EVENT_MAX as usize);
    let rows = events.events[..count]
        .iter()
        .map(event_value)
        .collect::<Vec<_>>();
    serde_json::json!({
        "supported": true,
        "capacity": events.capacity,
        "dropped": events.dropped,
        "events": rows,
    })
}

fn unsupported_events_value() -> serde_json::Value {
    serde_json::json!({
        "supported": false,
        "capacity": 0,
        "dropped": 0,
        "events": [],
    })
}

fn events_report(result: Result<ksu_uapi::ksu_netisolate_events>) -> Result<serde_json::Value> {
    match result {
        Ok(events) => Ok(events_value(&events)),
        Err(error) if kernel_unsupported(&error) => Ok(unsupported_events_value()),
        Err(error) => Err(error.context("Read network isolation events")),
    }
}

fn clear_events_report(result: Result<()>) -> Result<serde_json::Value> {
    match result {
        Ok(()) => Ok(serde_json::json!({ "supported": true, "cleared": true })),
        Err(error) if kernel_unsupported(&error) => Ok(unsupported_events_value()),
        Err(error) => Err(error.context("Clear network isolation events")),
    }
}

fn events() -> Result<()> {
    println!("{}", events_report(ksucalls::get_netisolate_events())?);
    Ok(())
}

fn events_clear() -> Result<()> {
    println!(
        "{}",
        clear_events_report(ksucalls::clear_netisolate_events())?
    );
    Ok(())
}

fn enable() -> Result<()> {
    let _lock = acquire_lock()?;
    let mut config = read_config()?.context("Network isolation is not configured")?;
    if config.uids.is_empty() {
        bail!("Network isolation requires at least one UID");
    }
    config.enabled = true;
    let snapshot = read_kernel_snapshot()?;
    persist_with_rollback(&config, &snapshot)
}

fn disable() -> Result<()> {
    let _lock = acquire_lock()?;
    let existing = read_config()?;
    let snapshot = read_kernel_snapshot()?;
    if snapshot.legacy.is_none() {
        if let Some(mut config) = existing {
            config.enabled = false;
            write_config(&config)?;
        }
        return Ok(());
    }
    let mut config = existing.unwrap_or_else(empty_config);
    config.enabled = false;
    let effective = config_with_supported_flags(config.clone(), supported_flags(&snapshot));
    if snapshot.policies.is_some() {
        apply_kernel_policies(&kernel_policies(&effective))?;
    } else {
        apply_legacy_kernel_config(&legacy_kernel_config(&effective))?;
    }
    if let Err(error) = write_config(&config) {
        if let Err(rollback) = restore(&snapshot) {
            bail!("{error:#}; residual=netisolate-kernel:{rollback:#}");
        }
        return Err(error);
    }
    Ok(())
}

fn clear() -> Result<()> {
    let _lock = acquire_lock()?;
    let snapshot = read_kernel_snapshot()?;
    let empty = empty_config();
    if snapshot.policies.is_some() {
        apply_kernel_policies(&kernel_policies(&empty))?;
    } else if snapshot.legacy.is_some() {
        apply_legacy_kernel_config(&legacy_kernel_config(&empty))?;
    }
    remove_config()
}

fn apply_persisted() -> Result<()> {
    let _lock = acquire_lock()?;
    let Some(config) = read_config()? else {
        return Ok(());
    };
    let snapshot = read_kernel_snapshot()?;
    if snapshot.legacy.is_none() {
        return Ok(());
    }
    apply_config_to_kernel(&config, &snapshot)
}

pub fn run(command: &Command) -> Result<()> {
    ensure_root()?;
    match command {
        Command::Status => status(),
        Command::Set => {
            let config = parse_input()?;
            let _lock = acquire_lock()?;
            let snapshot = read_kernel_snapshot()?;
            persist_with_rollback(&config, &snapshot)?;
            info!("netisolate: event=set enabled={}", config.enabled);
            Ok(())
        }
        Command::Enable => enable(),
        Command::Disable => disable(),
        Command::Clear => clear(),
        Command::Apply => apply_persisted(),
        Command::Events => events(),
        Command::EventsClear => events_clear(),
    }
}

pub fn boot_apply() {
    if utils::is_safe_mode() {
        info!("netisolate: event=boot-apply skipped=safe-mode");
        return;
    }
    if let Err(error) = apply_persisted() {
        warn!("netisolate: event=boot-apply error={error:#}");
    }
}
