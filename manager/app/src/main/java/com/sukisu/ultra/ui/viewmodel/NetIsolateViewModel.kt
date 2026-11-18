package com.sukisu.ultra.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.sukisu.ultra.R
import com.sukisu.ultra.data.netisolate.NetIsolateDataSource
import com.sukisu.ultra.data.netisolate.NetIsolateEvents
import com.sukisu.ultra.data.netisolate.NetIsolateMode
import com.sukisu.ultra.data.netisolate.NetIsolatePolicy
import com.sukisu.ultra.data.netisolate.NetIsolateRepository
import com.sukisu.ultra.data.netisolate.NetIsolateStatus
import com.sukisu.ultra.data.netisolate.supportsMode
import com.sukisu.ultra.data.uts.UtsTarget
import com.sukisu.ultra.data.uts.UtsTargetApp
import com.sukisu.ultra.ksuApp

@Immutable
data class NetIsolateUiState(
    val status: NetIsolateStatus = NetIsolateStatus(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val hasLoaded: Boolean = false,
    val message: String? = null,
    val apps: List<UtsTargetApp> = emptyList(),
    val appsLoading: Boolean = false,
    val appsLoaded: Boolean = false,
    val events: NetIsolateEvents = NetIsolateEvents(),
    val eventsLoading: Boolean = false,
    val eventsLoaded: Boolean = false,
)

class NetIsolateViewModel(
    private val repository: NetIsolateDataSource = NetIsolateRepository(),
    // ZephyrSU has no shell-generation counter (FinaSU's RootShell.generation):
    // KsuCli holds one shared root shell, so stale-result guards keyed on a
    // generation change do not apply; the request token still guards them.
    private val shellGeneration: () -> Long = { 0L },
    private val getString: (Int) -> String = { ksuApp.getString(it) },
) : ViewModel() {
    private val _uiState = MutableStateFlow(NetIsolateUiState())
    val uiState: StateFlow<NetIsolateUiState> = _uiState.asStateFlow()
    private var requestToken = 0L
    private var eventsRequestToken = 0L

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.busy) return
        val request = ++requestToken
        val generation = shellGeneration()
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val status = repository.status()
                if (request != requestToken || generation != shellGeneration()) return@launch
                _uiState.update { it.copy(status = status, hasLoaded = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == requestToken && generation == shellGeneration()) {
                    _uiState.update {
                        it.copy(message = e.message ?: getString(R.string.netisolate_status_failed))
                    }
                }
            } finally {
                if (request == requestToken) _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        val status = _uiState.value.status
        if (enabled && status.uids.isEmpty()) {
            _uiState.update { it.copy(message = getString(R.string.netisolate_enter_targets)) }
            return
        }
        operate {
            if (enabled) {
                repository.savePolicies(
                    true,
                    status.policies,
                    status.allowPrivateNetwork,
                    status.allowLocalDiscovery,
                )
            } else {
                repository.disable()
            }
        }
    }

    fun setMode(mode: NetIsolateMode) {
        val status = _uiState.value.status
        if (!status.supportsMode(mode)) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_mode_unsupported))
            }
            return
        }
        operate {
            repository.save(
                status.enabled,
                status.uids,
                mode == NetIsolateMode.Strict,
                mode == NetIsolateMode.Loopback,
                false,
                false,
            )
        }
    }

    fun setTargets(targets: List<UtsTarget>) {
        val status = _uiState.value.status
        val uids = targets.map(UtsTarget::uid).distinct()
        if (status.enabled && uids.isEmpty()) {
            _uiState.update { it.copy(message = getString(R.string.netisolate_enter_targets)) }
            return
        }
        val existing = status.policies.associateBy(NetIsolatePolicy::uid)
        val policies = uids.sorted().map { uid ->
            existing[uid] ?: NetIsolatePolicy(
                uid = uid,
                strictSocket = status.strictSocket,
                allowLoopback = status.allowLoopback,
            )
        }
        operate {
            repository.savePolicies(
                status.enabled,
                policies,
                status.allowPrivateNetwork,
                status.allowLocalDiscovery,
            )
        }
    }

    fun setPolicy(uid: Int, strictSocket: Boolean, allowLoopback: Boolean) {
        val status = _uiState.value.status
        if (!status.policySupported) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_policy_unsupported))
            }
            return
        }
        if (strictSocket && !status.strictSocketSupported) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_strict_socket_unsupported))
            }
            return
        }
        if (allowLoopback && !status.allowLoopbackSupported) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_allow_loopback_unsupported))
            }
            return
        }
        if (strictSocket && allowLoopback) return
        if (strictSocket && (status.allowPrivateNetwork || status.allowLocalDiscovery)) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_strict_allow_conflict))
            }
            return
        }
        val policies = status.policies.map { policy ->
            if (policy.uid == uid) {
                policy.copy(
                    strictSocket = strictSocket,
                    allowLoopback = allowLoopback,
                )
            } else {
                policy
            }
        }
        if (policies.none { it.uid == uid }) return
        operate {
            repository.savePolicies(
                status.enabled,
                policies,
                status.allowPrivateNetwork,
                status.allowLocalDiscovery,
            )
        }
    }

    fun setStrictSocket(strictSocket: Boolean) {
        val status = _uiState.value.status
        if (strictSocket && !status.strictSocketSupported) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_strict_socket_unsupported))
            }
            return
        }
        operate {
            repository.save(
                status.enabled,
                status.uids,
                strictSocket,
                if (strictSocket) false else status.allowLoopback,
                if (strictSocket) false else status.allowPrivateNetwork,
                if (strictSocket) false else status.allowLocalDiscovery,
            )
        }
    }

    fun setAllowLoopback(allowLoopback: Boolean) {
        val status = _uiState.value.status
        if (allowLoopback && !status.allowLoopbackSupported) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_allow_loopback_unsupported))
            }
            return
        }
        operate {
            repository.save(
                status.enabled,
                status.uids,
                if (allowLoopback) false else status.strictSocket,
                allowLoopback,
                status.allowPrivateNetwork,
                status.allowLocalDiscovery,
            )
        }
    }

    fun setAllowPrivateNetwork(allowPrivateNetwork: Boolean) {
        val status = _uiState.value.status
        if (allowPrivateNetwork && !status.allowPrivateNetworkSupported) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_allow_private_network_unsupported))
            }
            return
        }
        operate {
            repository.save(
                status.enabled,
                status.uids,
                if (allowPrivateNetwork) false else status.strictSocket,
                status.allowLoopback,
                allowPrivateNetwork,
                status.allowLocalDiscovery,
            )
        }
    }

    fun setAllowLocalDiscovery(allowLocalDiscovery: Boolean) {
        val status = _uiState.value.status
        if (allowLocalDiscovery && !status.allowLocalDiscoverySupported) {
            _uiState.update {
                it.copy(message = getString(R.string.netisolate_allow_local_discovery_unsupported))
            }
            return
        }
        operate {
            repository.save(
                status.enabled,
                status.uids,
                if (allowLocalDiscovery) false else status.strictSocket,
                status.allowLoopback,
                status.allowPrivateNetwork,
                allowLocalDiscovery,
            )
        }
    }

    fun clear() {
        operate { repository.clear() }
    }

    fun loadEvents() {
        loadEvents(clearFirst = false)
    }

    fun clearEvents() {
        loadEvents(clearFirst = true)
    }

    private fun loadEvents(clearFirst: Boolean) {
        if (_uiState.value.eventsLoading) return
        val request = ++eventsRequestToken
        val generation = shellGeneration()
        _uiState.update { it.copy(eventsLoading = true, message = null) }
        viewModelScope.launch {
            try {
                if (clearFirst) repository.clearEvents()
                val events = repository.events()
                if (request != eventsRequestToken || generation != shellGeneration()) return@launch
                _uiState.update {
                    it.copy(events = events, eventsLoaded = true, message = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == eventsRequestToken && generation == shellGeneration()) {
                    _uiState.update {
                        it.copy(
                            message = e.message ?: getString(R.string.netisolate_operation_failed),
                        )
                    }
                }
            } finally {
                if (request == eventsRequestToken) {
                    _uiState.update { it.copy(eventsLoading = false) }
                }
            }
        }
    }

    fun loadApps() {
        if (_uiState.value.appsLoading) return
        _uiState.update { it.copy(appsLoading = true) }
        viewModelScope.launch {
            try {
                val apps = repository.loadApps()
                _uiState.update { it.copy(apps = apps, appsLoaded = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(message = e.message ?: getString(R.string.netisolate_apps_failed))
                }
            } finally {
                _uiState.update { it.copy(appsLoading = false) }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun operate(block: suspend () -> Unit) {
        if (_uiState.value.busy) return
        val request = ++requestToken
        val generation = shellGeneration()
        _uiState.update { it.copy(busy = true, loading = false, message = null) }
        viewModelScope.launch {
            var refreshAfterFailure = false
            try {
                block()
                val status = repository.status()
                if (request != requestToken || generation != shellGeneration()) return@launch
                _uiState.update {
                    it.copy(
                        status = status,
                        hasLoaded = true,
                        message = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                refreshAfterFailure = true
                if (request == requestToken) {
                    _uiState.update {
                        it.copy(
                            message = e.message ?: getString(R.string.netisolate_operation_failed),
                        )
                    }
                }
            } finally {
                if (request == requestToken) {
                    _uiState.update { it.copy(busy = false) }
                    if (refreshAfterFailure) refresh()
                }
            }
        }
    }
}
