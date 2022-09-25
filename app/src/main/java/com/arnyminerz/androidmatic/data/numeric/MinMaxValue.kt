package com.arnyminerz.androidmatic.data.numeric

import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.JsonSerializer
import org.json.JSONObject

data class MinMaxValue(
    val value: Double,
    val min: Double,
    val max: Double,
): JsonSerializable {
    companion object: JsonSerializer<MinMaxValue> {
        override fun fromJson(json: JSONObject): MinMaxValue = MinMaxValue(
            json.getDouble("value"),
            json.getDouble("min"),
            json.getDouble("max"),
        )
    }

    override fun toJson(): JSONObject = JSONObject().apply {
        put("value", value)
        put("min", min)
        put("max", max)
    }
}
