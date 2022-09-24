package com.arnyminerz.androidmatic.utils

/**
 * Adds [element] to the collection if not present, or removes it otherwise.
 * @author Arnau Mora
 * @since 20220923
 * @param element The element to toggle.
 */
fun <T> MutableCollection<T>.toggle(element: T) {
    remove(element).takeIf { it } ?: add(element)
}
