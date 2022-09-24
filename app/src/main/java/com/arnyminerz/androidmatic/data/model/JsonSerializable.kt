package com.arnyminerz.androidmatic.data.model

import org.json.JSONObject

interface JsonSerializable {
    fun toJson(): JSONObject
}