package com.sukisu.ultra.data.model

/**
 * One shared kernel feature whose value changed outside ZephyrSU since boot
 * (M6.7 external feature-change detection). [expected] is the value ZephyrSU
 * applied at boot, [actual] is the value currently in the kernel.
 */
data class FeatureChange(
    val name: String,
    val expected: Long,
    val actual: Long,
)
