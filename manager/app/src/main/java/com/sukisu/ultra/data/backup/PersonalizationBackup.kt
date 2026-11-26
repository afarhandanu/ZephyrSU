package com.sukisu.ultra.data.backup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Process
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backs up and restores the manager's personalization: every settings store (wallpaper, opacity,
 * theme, font, music, sounds, profile, layout…) plus the media files they point at, as one zip
 * the user can keep and bring to another phone or back after a reset.
 *
 * Left out on purpose: the app lock, account sign-in, temporary-root grants and one-shot root
 * scripts (restoring those from a file would be a security hole), and download caches.
 */
object PersonalizationBackup {
    private const val FORMAT = 1
    private const val KIND = "zephyrsu-personalization"
    private const val MANIFEST = "manifest.json"
    private const val PREFS = "prefs.json"
    private const val FILES_PREFIX = "files/"

    private val EXCLUDED_PREFS = setOf(
        "authentication",
        "theme_account",
        "theme_device_access",
        "temporary_root",
        "theme_trial",
        "oneshot_scripts",
        "WebViewChromiumPrefs",
    )

    /** Keys that stay with the device even inside a store that is backed up. */
    private val EXCLUDED_KEYS = mapOf("settings" to setOf("intent_token"))

    private val EXCLUDED_FILES = setOf(
        "module_registry",
        "registry_downloads",
        "oneshot",
        "kernel_flash",
        "theme_trial",
        "theme_passthrough",
        "api_banners",
        "profileInstalled",
    )

    private val PREF_NAME = Regex("[A-Za-z0-9_.-]+")

    fun fileName(): String {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        return "ZephyrSU-personalization-$stamp.zip"
    }

    fun export(context: Context, uri: Uri): Result<Unit> = runCatching {
        val output = context.contentResolver.openOutputStream(uri)
            ?: error("Cannot open the destination file")
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            val manifest = JSONObject()
                .put("kind", KIND)
                .put("format", FORMAT)
                .put("package", context.packageName)
                .put("created", System.currentTimeMillis())
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest.toString().toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(PREFS))
            zip.write(readPrefs(context).toString().toByteArray())
            zip.closeEntry()

            val filesDir = context.filesDir
            filesDir.listFiles().orEmpty()
                .filter { it.name !in EXCLUDED_FILES }
                .forEach { top ->
                    top.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relative = file.relativeTo(filesDir).invariantSeparatorsPath
                        zip.putNextEntry(ZipEntry(FILES_PREFIX + relative))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
        }
    }

    /** Replaces the current personalization with the backup. The app must restart afterwards. */
    fun restore(context: Context, uri: Uri): Result<Unit> = runCatching {
        val stage = File(context.cacheDir, "personalization-restore")
        stage.deleteRecursively()
        check(stage.mkdirs()) { "Cannot prepare the restore folder" }
        val stageRoot = stage.canonicalPath + File.separator
        var manifest: JSONObject? = null
        var prefs: JSONObject? = null

        val input = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open the backup file")
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory) {
                    // Folders are created together with their files.
                } else if (name == MANIFEST) {
                    manifest = JSONObject(zip.readBytes().decodeToString())
                } else if (name == PREFS) {
                    prefs = JSONObject(zip.readBytes().decodeToString())
                } else if (name.startsWith(FILES_PREFIX)) {
                    val relative = name.removePrefix(FILES_PREFIX)
                    val top = relative.substringBefore('/')
                    val target = File(stage, relative).canonicalFile
                    if (top.isNotEmpty() && top !in EXCLUDED_FILES && target.path.startsWith(stageRoot)) {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
        }

        val info = manifest
        val stores = prefs
        if (info == null || stores == null || info.optString("kind") != KIND || info.optInt("format") != FORMAT) {
            stage.deleteRecursively()
            error("Not a ZephyrSU personalization backup")
        }

        // The whole archive was read and checked; only now touch the live data.
        val filesDir = context.filesDir
        filesDir.listFiles().orEmpty()
            .filter { it.name !in EXCLUDED_FILES }
            .forEach { it.deleteRecursively() }
        stage.listFiles().orEmpty().forEach { source ->
            val target = File(filesDir, source.name)
            if (!source.renameTo(target)) {
                check(source.copyRecursively(target, overwrite = true)) { "Cannot restore ${source.name}" }
            }
        }
        stage.deleteRecursively()

        writePrefs(context, stores, info.optString("package"))
    }

    /** Puts the personalization back to the app defaults. The app must restart afterwards. */
    fun reset(context: Context): Result<Unit> = runCatching {
        context.filesDir.listFiles().orEmpty()
            .filter { it.name !in EXCLUDED_FILES }
            .forEach { it.deleteRecursively() }
        File(context.applicationInfo.dataDir, "shared_prefs").listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".xml") }
            .map { it.name.removeSuffix(".xml") }
            .filter { it !in EXCLUDED_PREFS && PREF_NAME.matches(it) }
            .forEach { name ->
                val skipped = EXCLUDED_KEYS[name].orEmpty()
                val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                val kept = prefs.all.filterKeys { it in skipped }
                val editor = prefs.edit().clear()
                kept.forEach { (key, value) -> putValue(editor, key, value) }
                check(editor.commit()) { "Cannot reset the $name settings" }
            }
    }

    /** Relaunches the manager so every screen reads the restored settings from scratch. */
    fun restart(context: Context) {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
        Process.killProcess(Process.myPid())
    }

    private fun readPrefs(context: Context): JSONObject {
        val result = JSONObject()
        File(context.applicationInfo.dataDir, "shared_prefs").listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".xml") }
            .map { it.name.removeSuffix(".xml") }
            .filter { it !in EXCLUDED_PREFS && PREF_NAME.matches(it) }
            .sorted()
            .forEach { name ->
                val skipped = EXCLUDED_KEYS[name].orEmpty()
                val store = JSONObject()
                context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                    if (key !in skipped) encode(value)?.let { store.put(key, it) }
                }
                result.put(name, store)
            }
        return result
    }

    private fun writePrefs(context: Context, stores: JSONObject, sourcePackage: String) {
        val oldPath = "/$sourcePackage/"
        val newPath = "/${context.packageName}/"
        val rewrite = sourcePackage.isNotEmpty() && sourcePackage != context.packageName
        stores.keys().forEach { name ->
            if (name in EXCLUDED_PREFS || !PREF_NAME.matches(name)) return@forEach
            val store = stores.optJSONObject(name) ?: return@forEach
            val skipped = EXCLUDED_KEYS[name].orEmpty()
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val kept = prefs.all.filterKeys { it in skipped }
            val editor = prefs.edit().clear()
            store.keys().forEach { key ->
                val item = store.optJSONObject(key)
                if (key !in skipped && item != null) {
                    when (item.optString("t")) {
                        "s" -> {
                            val text = item.optString("v")
                            editor.putString(key, if (rewrite) text.replace(oldPath, newPath) else text)
                        }
                        "i" -> editor.putInt(key, item.optInt("v"))
                        "l" -> editor.putLong(key, item.optLong("v"))
                        "f" -> editor.putFloat(key, item.optDouble("v").toFloat())
                        "b" -> editor.putBoolean(key, item.optBoolean("v"))
                        "ss" -> {
                            val array = item.optJSONArray("v") ?: JSONArray()
                            val set = HashSet<String>()
                            for (index in 0 until array.length()) {
                                val text = array.optString(index)
                                set.add(if (rewrite) text.replace(oldPath, newPath) else text)
                            }
                            editor.putStringSet(key, set)
                        }
                    }
                }
            }
            kept.forEach { (key, value) -> putValue(editor, key, value) }
            check(editor.commit()) { "Cannot restore the $name settings" }
        }
    }

    private fun encode(value: Any?): JSONObject? = when (value) {
        is String -> JSONObject().put("t", "s").put("v", value)
        is Int -> JSONObject().put("t", "i").put("v", value)
        is Long -> JSONObject().put("t", "l").put("v", value)
        is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
        is Boolean -> JSONObject().put("t", "b").put("v", value)
        is Set<*> -> JSONObject().put("t", "ss").put("v", JSONArray(value.filterIsInstance<String>()))
        else -> null
    }

    private fun putValue(editor: SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
    }
}
