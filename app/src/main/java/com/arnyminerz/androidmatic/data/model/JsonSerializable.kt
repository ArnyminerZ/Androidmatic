package com.arnyminerz.androidmatic.data.model

import org.json.JSONException
import org.json.JSONObject

interface JsonSerializable {
    fun toJson(): JSONObject
}

/**
 * Runs [JSONObject.put] after serializing the [serializable].
 * @author Arnau Mora
 * @since 20220926
 */
@Throws(JSONException::class)
fun JSONObject.putSerializable(key: String, serializable: JsonSerializable) =
    put(key, serializable.toJson())
