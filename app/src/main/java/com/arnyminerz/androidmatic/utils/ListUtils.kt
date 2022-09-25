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

/**
 * Checks if all the elements of the list are true.
 * @author Arnau Mora
 * @since 20220925
 * @return `true` if all the elements are `true`, `false` otherwise.
 */
fun Iterable<Boolean>.allTrue(): Boolean {
    for (b in this)
        if (!b)
            return false
    return true
}

/**
 * Filters the contents of a collection based on a query.
 * @author Arnau Mora
 * @since 20220925
 * @param query The query parameter to use as search.
 * @param predicate The conversion to use for passing from [T] to a [String] to use for searching.
 * @return A new list filtered with the given [query].
 */
fun <T> Collection<T>.filterSearch(query: String, predicate: (item: T) -> String): List<T> =
    filter { item ->
        if (query.isBlank()) return@filter true

        val value = predicate(item).lowercase()
        val queryVal = query.lowercase()
        val queryParams = queryVal.split(' ')
        return@filter if (value.contains(queryVal))
            true
        else
            queryParams.map { value.contains(it) }.allTrue()
    }
