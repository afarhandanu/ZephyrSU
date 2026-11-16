package com.sukisu.ultra.data.model

/**
 * Tokens persisted under the `support_links_style` preference key.
 *
 * The bottom card of the home screen can present several shapes; the token decides which one, so
 * the preference maps onto the same card across restarts. Unknown or legacy tokens fall back to
 * [LEGACY] instead of failing. [supported] is the single source of truth the settings dropdown
 * iterates, so a new shape is added by extending this object alone.
 *
 * Ported from FinaSU (`me.weishu.kernelsu.data.model.SupportLinksStyle`): FinaSU's `FINASU`
 * intro card is adapted to ZephyrSU (`ZEPHYRSU`, linking to this repo), and the default is
 * [LEGACY] so the home screen keeps its current KernelSU support card unless the user picks
 * another shape.
 */
object SupportLinksStyle {
    /** The ZephyrSU introduction card, linking to this repo. */
    const val ZEPHYRSU = "zephyrsu"

    /** The upstream KernelSU pair: a donation row and a KernelSU documentation row. */
    const val LEGACY = "kernelsu"

    /** A single card whose title, subtitle and link are entered by the user. */
    const val CUSTOM = "custom"

    /** Draw nothing at the bottom of the home screen. */
    const val HIDDEN = "hidden"

    /** Style used when the preference is missing or holds an unsupported token. */
    const val DEFAULT = LEGACY

    /** Normalise a stored value to a supported token. */
    fun fromValue(value: String?): String = when (value) {
        LEGACY -> LEGACY
        CUSTOM -> CUSTOM
        HIDDEN -> HIDDEN
        ZEPHYRSU -> ZEPHYRSU
        else -> DEFAULT
    }

    /** Every supported token, in display order. */
    val supported: List<String> = listOf(ZEPHYRSU, LEGACY, CUSTOM, HIDDEN)
}
