package com.arnyminerz.androidmatic.data.model

import org.json.JSONObject

interface JsonSerializer <T> {
    fun fromJson(json: JSONObject): T
}
