package com.sukisu.ultra.data.boot

import com.sukisu.ultra.ui.util.getKsuDaemonPath
import com.sukisu.ultra.ui.util.getRootShell
import com.sukisu.ultra.ui.util.shellQuote
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

interface KernelFactsSource {
    suspend fun load(): KernelFacts
}

/** One bounded, read-only probe shared by SUSFS and UTS View. */
class KernelFactsRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : KernelFactsSource {
    override suspend fun load(): KernelFacts = withContext(dispatcher) {
        // ZephyrSU has no RootShell.awaitRoot wrapper (FinaSU-only): the shared
        // root shell is built at KsuCli init, so check isRoot directly instead.
        // A failed probe must never silently run on a non-root fallback shell.
        val shell = getRootShell()
        check(shell.isRoot) { "Root shell is not available" }
        val output = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val result = withTimeout(45_000L) {
            shell.newJob()
                .add("${getKsuDaemonPath().shellQuote()} boot-info kernel-facts")
                .to(output, errors)
                .exec()
        }
        check(result.isSuccess) {
            errors.joinToString("\n").ifEmpty { "Boot facts failed (${result.code})" }
        }
        KernelFacts.parse(output.joinToString("\n"))
    }
}
