package com.sukisu.ultra.data.modulebanner

import androidx.compose.runtime.Immutable
import java.util.Locale

/**
 * One shared banner API endpoint listed by the marketplace.
 *
 * Ported from FinaSU (Milestone 3.5); package adapted to ZephyrSU. No behaviour change.
 */
@Immutable
data class ApiMarketplaceItem(
    val name: String,
    val url: String,
    val description: String,
    val descriptionEn: String,
) {
    fun localizedDescription(): String {
        val preferChinese = Locale.getDefault().language.let { it == "zh" || it == "mgl" }
        val primary = if (preferChinese) description else descriptionEn
        val fallback = if (preferChinese) descriptionEn else description
        return primary.ifBlank { fallback }
    }
}
