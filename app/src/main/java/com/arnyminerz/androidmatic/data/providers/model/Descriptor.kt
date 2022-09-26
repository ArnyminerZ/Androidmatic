package com.arnyminerz.androidmatic.data.providers.model

import kotlin.reflect.KClass

abstract class Descriptor {
    abstract val name: String

    abstract val parameters: Map<String, KClass<*>>

    fun provide(vararg params: Pair<String, Any>): HoldingDescriptor =
        object : HoldingDescriptor() {
            override val name: String = this@Descriptor.name

            override val data: Map<String, Any> = params.toMap()

            override val parameters: Map<String, KClass<*>> = this@Descriptor.parameters
        }

    fun check(vararg params: Pair<String, Any>): Boolean {
        for (param in parameters) {
            val parameter = params.find { it.first == param.key } ?: return false
            if (parameter.second::class != param.value) return false
        }
        return true
    }
}
