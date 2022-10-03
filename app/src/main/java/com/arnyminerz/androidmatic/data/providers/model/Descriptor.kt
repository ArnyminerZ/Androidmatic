package com.arnyminerz.androidmatic.data.providers.model

import com.arnyminerz.androidmatic.data.numeric.GeoPoint
import com.arnyminerz.androidmatic.utils.allTrue
import kotlin.reflect.KClass

abstract class Descriptor {
    abstract val name: String

    protected abstract val parameters: Map<String, KClass<*>>

    /**
     * Defines which functions can be called in the Provider.
     * @author Arnau Mora
     * @since 20220926
     */
    abstract val capabilities: List<Capability>

    /**
     * Checks if the descriptor supports a [Capability].
     * @author Arnau Mora
     * @since 20220926
     * @param capability The [Capability] to check for.
     * @return `true` if the descriptor supports [capability], `false` otherwise.
     */
    fun supports(capability: Capability) = capabilities.contains(capability)

    /**
     * Checks if the [Descriptor] has a parameter key.
     * @author Arnau Mora
     * @since 20220926
     * @param key The key to search for.
     * @return `true` if the parameter key is present, `false` otherwise.
     */
    fun hasParameter(key: String): Boolean =
        parameters.containsKey(key)

    /**
     * Checks if the [Descriptor] has a parameter with the given key that matches a given type.
     * @author Arnau Mora
     * @since 20221001
     * @param key The key to search for.
     * @param type The type the key is supposed to have.
     * @return `true` if the parameter key is present, and the set type matches type. `false` otherwise.
     */
    fun hasParameterOfType(key: String, type: KClass<*>): Boolean =
        parameters[key] == type

    /**
     * Generates a [HoldingDescriptor] from some parameters and the defined data in `this`.
     * @author Arnau Mora
     * @since 20220926
     * @param params The parameters to use as [HoldingDescriptor.data]. Must match the keys and maps
     * specified at [parameters].
     * @return A new instance of [HoldingDescriptor].
     */
    fun provide(vararg params: Pair<String, Any>): HoldingDescriptor =
        params.takeIf {
            params
                .map { (key, value) ->
                    parameters.containsKey(key) && value::class == parameters.getValue(key)
                }
                .allTrue()
        }?.let {
            object : HoldingDescriptor() {
                override val name: String = this@Descriptor.name

                override val data: Map<String, Any> = params.toMap()

                override val capabilities: List<Capability> = this@Descriptor.capabilities

                override val parameters: Map<String, KClass<*>> = this@Descriptor.parameters
            }
        }
            ?: throw IllegalArgumentException(
                "The provided params (${
                    params.map {
                        "${it.first}(${it.first::class})=${
                            it.second.also { e ->
                                (e as? String)?.replace(
                                    "\n",
                                    "\\n"
                                )
                            }
                        }"
                    }
                }) have some parameters missing ($parameters) or types do not match."
            )

    /**
     * Checks if a list of parameters is correct.
     * @author Arnau Mora
     * @since 20221003
     * @param params The parameters to check for.
     * @see hasParameterOfType
     */
    fun check(vararg params: Pair<String, Any>): Boolean {
        for (param in parameters) {
            val parameter = params.find { it.first == param.key } ?: return false
            if (parameter.second::class != param.value) return false
        }
        return true
    }

    enum class Capability(
        val requireParameters: Map<String, KClass<*>> = emptyMap(),
        val requireExtends: KClass<*>? = null,
    ) {
        /**
         * Indicates that the provider supports listing for available stations. Because of this, the
         * provider must extend [WeatherListProvider].
         * @author Arnau Mora
         * @since 20220926
         */
        LISTING(requireExtends = WeatherListProvider::class),

        /**
         * Indicates that the provider has information about the location of the station. This
         * requires that the provider contains the parameters `location::String` and `point::GeoPoint`.
         * @author Arnau Mora
         * @since 20220926
         */
        LOCATION(mapOf("location" to String::class, "point" to GeoPoint::class)),

        /**
         * Indicates that the provider has to be configured manually. Because of this, the provider
         * must extend [WeatherManualProvider].
         * @author Arnau Mora
         * @since 20221003
         */
        MANUAL_SETUP(requireExtends = WeatherManualProvider::class),
    }
}

fun Map<String, KClass<*>>.paramsString() = map { "${it.key}::${it.value.simpleName}" }
