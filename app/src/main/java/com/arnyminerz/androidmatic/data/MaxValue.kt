package com.arnyminerz.androidmatic.data

import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.JsonSerializer
import org.json.JSONObject

data class MaxValue(
    val value: Double,
    val max: Double,
) : JsonSerializable {
    companion object : JsonSerializer<MaxValue> {
        override fun fromJson(json: JSONObject): MaxValue = MaxValue(
            json.getDouble("value"),
            json.getDouble("max"),
        )
    }

    override fun toJson(): JSONObject = JSONObject().apply {
        put("value", value)
        put("max", max)
    }
}
