package de.thorstream.butler.core.common

/**
 * Resolves localized strings outside of composables (services, view models,
 * quality evaluation). Keeps the domain and data layers testable without
 * an Android context.
 */
interface StringProvider {
    fun get(resId: Int, vararg args: Any): String
}
