package com.sukisu.ultra.data.security

enum class CredentialKind(val key: String) {
    PIN("pin"),
    PATTERN("pattern");

    companion object {
        fun fromKey(key: String): CredentialKind? = entries.firstOrNull { it.key == key }
    }
}
