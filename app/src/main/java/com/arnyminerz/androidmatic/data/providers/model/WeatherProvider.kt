package com.arnyminerz.androidmatic.data.providers.model

import android.content.Context
import androidx.annotation.WorkerThread
import com.android.volley.VolleyError
import com.arnyminerz.androidmatic.data.StationFeedResult
import com.arnyminerz.androidmatic.data.WeatherState
import com.arnyminerz.androidmatic.utils.takeOrThrow
import com.arnyminerz.androidmatic.utils.tryTaking
import timber.log.Timber
import java.io.InvalidClassException
import java.text.ParseException
import kotlin.reflect.KClass

abstract class WeatherProvider {
    /**
     * The name that will be displayed to the user and represents the provider.
     * @author Arnau Mora
     * @since 20220927
     */
    abstract val displayName: String

    abstract val providerName: String

    abstract val descriptor: Descriptor

    @WorkerThread
    suspend fun check(context: Context, vararg params: Pair<String, Any>): Boolean = try {
        fetch(context, *params)
        true
    } catch (e: Exception) {
        false
    }

    /**
     * Tries to fetch an station's data.
     * @author Arnau Mora
     * @since 20220926
     * @param context The [Context] that is requesting the data.
     * @param params The parameters to use. Specific to each provider.
     * @return A new [StationFeedResult] with the loaded data.
     * @throws IllegalArgumentException When the given [params] have some data missing.
     * @throws ParseException When a value is missing on the response to be parsed.
     * @throws VolleyError When there's an error whilst fetching some data from an API.
     */
    @Throws(IllegalArgumentException::class, ParseException::class, VolleyError::class)
    @WorkerThread
    abstract suspend fun fetch(
        context: Context,
        vararg params: Pair<String, Any>
    ): StationFeedResult

    @WorkerThread
    abstract suspend fun fetchWeather(
        context: Context,
        vararg params: Pair<String, Any>
    ): WeatherState

    companion object {
        @Volatile
        var providers: Collection<KClass<*>> = emptyList()
            private set

        /**
         * Tries calling the constructor of a given class.
         * @author Arnau Mora
         * @since 20220926
         * @param providerClass The class to construct.
         * @return A [WeatherProvider] instance.
         * @throws InvalidClassException If the class doesn't have any constructors.
         */
        private fun buildProvider(providerClass: KClass<*>): WeatherProvider =
            providerClass
                .constructors
                .takeOrThrow(
                    { it.isNotEmpty() },
                    InvalidClassException("There are no constructors available"),
                )
                .first()
                .takeOrThrow(
                    { it.parameters.isEmpty() },
                    NoSuchMethodException("The given class $providerClass doesn't have a constructor with no parameters.")
                )
                .call() as WeatherProvider

        /**
         * Registers a new provider into the registry.
         * @author Arnau Mora
         * @since 20220926
         * @param providerClass The class of the [WeatherProvider] to add.
         * @return The built provider if valid.
         * @throws IllegalArgumentException If the given provider is already registered.
         * @throws InvalidClassException If the given provider doesn't have a valid constructor.
         */
        @Throws(IllegalArgumentException::class, InvalidClassException::class)
        fun register(providerClass: KClass<*>) = providerClass
            .tryTaking { buildProvider(it) }
            ?.also { provider ->
                providers
                    // Try is not required here since if the provider is added it means it can be
                    // constructed.
                    .map { buildProvider(it) }
                    .find { provider.providerName == it.providerName }
                    ?.let { throw IllegalArgumentException("Provider named ${it.providerName} already registered.") }
            }
            ?.also {
                if (!it.descriptor.hasParameter("name"))
                    throw IllegalArgumentException("The provider's descriptor doesn't have a \"name\" field.")
                if (!it.descriptor.hasParameter("uid"))
                    throw IllegalArgumentException("The provider's descriptor doesn't have a \"uid\" field.")
            }
            ?.also {
                providers = providers
                    .toMutableList()
                    .also { it.add(providerClass) }
                    .also { Timber.i("Registered weather provider with $providerClass") }
            }
            ?: throw InvalidClassException("The given class ($providerClass) doesn't have a valid constructor.")

        /**
         * Finds a registered [WeatherProvider] whose descriptor has as name [descriptorName].
         * @author Arnau Mora
         * @since 20220926
         * @param descriptorName The [Descriptor.name] of the descriptor of the provider to find.
         * @return A [WeatherProvider] if available, or null if none.
         */
        fun firstWithDescriptor(descriptorName: String): WeatherProvider? =
            providers
                .map { buildProvider(it) }
                .find { it.descriptor.name == descriptorName }

        /**
         * Finds all providers that support the given capabilities.
         * @author Arnau Mora
         * @since 20220927
         * @param capabilities The capabilities to search for.
         * @return A list with all the providers available. May be empty if no matches.
         */
        fun getWithCapabilities(vararg capabilities: Descriptor.Capability) =
            providers
                .map { buildProvider(it) }
                .filter { provider -> capabilities.all { provider.descriptor.supports(it) } }
    }
}
