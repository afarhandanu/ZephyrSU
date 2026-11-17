package com.sukisu.ultra.data.backup.device

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sukisu.ultra.ui.util.getRootShell

/**
 * Wraps [value] in single quotes for a shell command line.
 *
 * The embedded `'` is closed, a literal `"'"` is spliced in and the quote is reopened, which is the
 * only safe way to nest a single quote inside a single-quoted shell token.
 */
internal fun shellQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"

/**
 * Runs [command] on Nova's shared root shell and returns its result.
 *
 * Standard output and errors are collected explicitly: a bare `exec()` would report empty output
 * for otherwise successful commands.
 *
 * This throws when no root shell is available, so privileged backup work can never silently run on a
 * non-root fallback shell.
 */
internal suspend fun runRootCommand(command: String): Shell.Result = withContext(Dispatchers.IO) {
    val stdout = ArrayList<String>()
    val stderr = ArrayList<String>()
    val shell = getRootShell()
    check(shell.isRoot) { "Root shell is not available" }
    shell.newJob().add(command).to(stdout, stderr).exec()
}
