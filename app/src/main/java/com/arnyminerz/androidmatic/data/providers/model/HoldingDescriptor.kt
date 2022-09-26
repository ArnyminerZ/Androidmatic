package com.arnyminerz.androidmatic.data.providers.model

import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.JsonSerializer
import com.arnyminerz.androidmatic.utils.findClassForName
import com.arnyminerz.androidmatic.utils.toJSONArray
import com.arnyminerz.androidmatic.utils.toList
import org.json.JSONObject
import kotlin.reflect.KClass

abstract class HoldingDescriptor : Descriptor(), JsonSerializable {
    companion object : JsonSerializer<HoldingDescriptor> {
        override fun fromJson(json: JSONObject): HoldingDescriptor {
            return object : HoldingDescriptor() {
                override val name: String = json.getString("name")

                override val capabilities: List<Capability> = json
                    .getJSONArray("capabilities")
                    .toList<String, Capability> { Capability.valueOf(it) }

                override val data: Map<String, Any> = json.getJSONObject("params").let { json ->
                    json.keys()
                        .asSequence()
                        .filter { json.get(it) is JSONObject }
                        .associateWith { json.getJSONObject(it).get("value") }
                }

                override val parameters: Map<String, KClass<*>> =
                    json.getJSONObject("params").let { json ->
                        json.keys()
                            .asSequence()
                            .filter { json.get(it) is JSONObject }
                            .associateWith {
                                findClassForName(json.getJSONObject(it).getString("type"))::class
                            }
                    }
            }
        }
    }

    protected abstract val data: Map<String, Any>

    operator fun get(key: String): Any? = data[key]

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValue(key: String): T = data.getValue(key) as T

    fun expand(): Array<out Pair<String, Any>> = data.toList().toTypedArray()

    override fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("capabilities", capabilities.map { it.name }.toJSONArray())
        val paramsObject = JSONObject()
        for ((key, type) in parameters)
            paramsObject.put(
                key,
                JSONObject().apply {
                    put("type", type.qualifiedName)
                    put("value", data[key])
                }
            )
        put("params", paramsObject)
    }
}