package com.arnyminerz.androidmatic.data.providers.model

import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.JsonSerializer
import com.arnyminerz.androidmatic.utils.findClassForName
import org.json.JSONObject
import kotlin.reflect.KClass

abstract class HoldingDescriptor : Descriptor(), JsonSerializable {
    companion object : JsonSerializer<HoldingDescriptor> {
        override fun fromJson(json: JSONObject): HoldingDescriptor {
            return object : HoldingDescriptor() {
                override val name: String = json.getString("name")

                override val data: Map<String, Any> = json
                    .keys()
                    .asSequence()
                    .filter { json.get(it) is JSONObject }
                    .associateWith { json.getJSONObject(it).get("value") }

                override val parameters: Map<String, KClass<*>> = json
                    .keys()
                    .asSequence()
                    .filter { json.get(it) is JSONObject }
                    .associateWith {
                        findClassForName(json.getJSONObject(it).getString("type"))::class
                    }
            }
        }
    }

    abstract val data: Map<String, Any>

    fun expand(): Array<out Pair<String, Any>> = data.toList().toTypedArray()

    override fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        for ((key, type) in parameters)
            put(
                key,
                JSONObject().apply {
                    put("type", type.qualifiedName)
                    put("value", data[key])
                }
            )
    }
}