package com.arnyminerz.androidmatic.utils

import timber.log.Timber

fun <T, R> T.tryTaking(operation: (value: T) -> R): R? =
    try {
        operation(this)
    } catch (e: Exception) {
        Timber.e(e, "Could not take.")
        null
    }

fun <T> T.takeOrThrow(predicate: (value: T) -> Boolean, throwable: Throwable): T =
    if (predicate(this))
        this
    else
        throw throwable
