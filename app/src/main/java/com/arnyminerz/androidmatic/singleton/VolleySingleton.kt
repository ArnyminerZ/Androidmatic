package com.arnyminerz.androidmatic.singleton

import android.content.Context
import androidx.annotation.WorkerThread
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * The Singleton for making HTTP requests.
 *
 * Use [addToRequestQueue] to add new requests to the queue, and they will be ran asap.
 * @author Arnau Mora
 * @since 20220923
 */
class VolleySingleton private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: VolleySingleton? = null

        /**
         * Gets the [VolleySingleton] instance, or instantiates a new one if none available.
         * @author Arnau Mora
         * @since 20220923
         * @return The [VolleySingleton] instance
         */
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolleySingleton(context).also {
                    INSTANCE = it
                }
            }
    }

    private val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }

    /**
     * Adds a request to the singleton's thread.
     * @author Arnau Mora
     * @since 20220923
     * @param req The request to add.
     * @return [req]
     */
    fun <T> addToRequestQueue(req: Request<T>): Request<T> = requestQueue.add(req)

    /**
     * Runs a GET request for the given [url] asynchronously.
     * @author Arnau Mora
     * @since 20220924
     * @param url The url to make the request to.
     * @return The contents of the request's body.
     * @throws VolleyError When there's an exception while making the request.
     */
    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun getString(url: String): String =
        suspendCoroutine { cont ->
            addToRequestQueue(
                StringRequest(
                    Request.Method.GET,
                    url,
                    { cont.resume(it) },
                    { cont.resumeWithException(it) },
                )
            )
        }
}