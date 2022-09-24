package com.arnyminerz.androidmatic.utils

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs the code in [block] in the UI's thread.
 * @author Arnau Mora
 * @since 20220923
 * @param block The block of code to run.
 * @return The result of [block].
 */
suspend fun <R> ui(@UiThread block: suspend CoroutineScope.() -> R): R =
    withContext(Dispatchers.Main, block)

/**
 * Runs the code in [block] in the IO's thread.
 * @author Arnau Mora
 * @since 20220923
 * @param block The block of code to run.
 * @return The result of [block].
 */
suspend fun <R> io(@WorkerThread block: suspend CoroutineScope.() -> R): R =
    withContext(Dispatchers.Main, block)

/**
 * Runs the code in [block] in the IO's thread asynchronously and suspendfully.
 * @author Arnau Mora
 * @since 20220923
 * @param block The block of code to run.
 * @return A job observing the state of [block].
 */
fun doAsync(@WorkerThread block: suspend CoroutineScope.() -> Unit) =
    CoroutineScope(Dispatchers.IO).launch(block = block)

/**
 * Runs the code in [block] in the IO thread using the [viewModelScope].
 * @author Arnau Mora
 * @since 20220923
 * @param block The block of code to run.
 * @see viewModelScope
 */
fun ViewModel.launch(@WorkerThread block: suspend CoroutineScope.() -> Unit) =
    viewModelScope.launch(context = Dispatchers.IO, block = block)
