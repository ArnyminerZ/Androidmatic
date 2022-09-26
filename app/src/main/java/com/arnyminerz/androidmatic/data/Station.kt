package com.arnyminerz.androidmatic.data

import android.content.Context
import android.nfc.FormatException
import androidx.annotation.WorkerThread
import com.android.volley.VolleyError
import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.putSerializable
import com.arnyminerz.androidmatic.data.numeric.GeoPoint
import com.arnyminerz.androidmatic.data.providers.MeteoclimaticProvider
import com.arnyminerz.androidmatic.data.providers.model.Descriptor
import com.arnyminerz.androidmatic.data.providers.model.HoldingDescriptor
import com.arnyminerz.androidmatic.data.providers.model.WeatherProvider
import com.arnyminerz.androidmatic.utils.readString
import com.arnyminerz.androidmatic.utils.skip
import com.arnyminerz.androidmatic.utils.xmlPubDateFormatter
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Date
import kotlin.reflect.KClass

data class Station(
    val descriptor: HoldingDescriptor,
) : JsonSerializable {
    companion object {
        /**
         * Creates a new station from the given [XmlPullParser].
         * @author Arnau Mora
         * @since 20220923
         * @param parser The parser to get the data from.
         * @return A new [Station] instance with the loaded data, and the timestamp for when it was
         * submit.
         * @throws FormatException When the format of the item's `pubDate` field is not correct.
         */
        @Throws(
            XmlPullParserException::class,
            IOException::class,
            NullPointerException::class,
            FormatException::class,
        )
        fun fromXml(parser: XmlPullParser): Pair<Date, Station> {
            parser.require(XmlPullParser.START_TAG, null, "item")
            var title: String? = null
            var link: String? = null
            var pubDate: String? = null
            var guid: String? = null
            var point: String? = null
            var description: String? = null
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                when (parser.name) {
                    "title" -> title = parser.readString("title")
                    "link" -> link = parser.readString("link")
                    "pubDate" -> pubDate = parser.readString("pubDate")
                    "guid" -> guid = parser.readString("guid")
                    "description" -> description = parser.readString("description")
                    "georss:point" -> point = parser.readString("georss:point")
                    else -> parser.skip()
                }
            }

            val timestamp = xmlPubDateFormatter.parse(pubDate!!)
                ?: throw FormatException("The pubDate's format is not correct")
            val uid = link!!.substring(link.lastIndexOf('/') + 1)
            val name = title!!.substring(0, title.lastIndexOf('(')).trim()
            val location =
                title.substring(title.lastIndexOf('(') + 1, title.lastIndexOf(')')).trim()

            return timestamp to Station(
                MeteoclimaticProvider.ProviderDescriptor.provide(
                    uid,
                    name,
                    location,
                    guid!!,
                    GeoPoint.fromString(point!!),
                    description ?: String(),
                ),
            )
        }

        /**
         * Instantiates a new [Station] from its definition in json.
         * @author Arnau Mora
         * @since 20220924
         * @param json The [JSONObject] to convert.
         * @return A new [Station] instance from the data in [json].
         * @throws JSONException When there's an error parsing the JSON object.
         */
        @Suppress("UNCHECKED_CAST")
        fun fromJson(json: JSONObject): Station =
            Station(
                HoldingDescriptor.fromJson(json.getJSONObject("descriptor")),
            )
    }

    val name: String = descriptor.getValue("name")

    val uid: String = descriptor.getValue("uid")

    val provider: WeatherProvider = WeatherProvider.firstWithDescriptor(descriptor.name)
        ?: throw ClassNotFoundException("Could not find provider with descriptor named ${descriptor.name}")

    /**
     * Gets the value at [key] from [descriptor].
     * @author Arnau Mora
     * @since 20220926
     * @param key The key of the element to get.
     * @param T The type of the element to get.
     * @see HoldingDescriptor.get
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: String): T? = descriptor[key] as? T?

    /**
     * Gets the value at [key] from [descriptor].
     * @author Arnau Mora
     * @since 20220926
     * @param key The key of the element to get.
     * @param T The type of the element to get.
     * @see HoldingDescriptor.getValue
     */
    fun <T : Any> getValue(key: String): T = descriptor.getValue(key)

    /**
     * Checks if the descriptor supports a given capability.
     * @author Arnau Mora
     * @since 20220926
     * @param capability The capability to check for.
     * @return `true` if the descriptor supports [capability], `false` otherwise.
     */
    fun descriptorSupports(capability: Descriptor.Capability) = descriptor.supports(capability)

    override fun toString(): String = name

    /**
     * Tries to get the station's current weather.
     * @author Arnau Mora
     * @since 20220923
     * @throws VolleyError When there's an exception while loading the data from the server.
     * @throws ArrayIndexOutOfBoundsException When any station could be found on the server's response.
     * @throws IllegalStateException When the loaded station doesn't have any description data.
     * @throws IllegalArgumentException When the station's description doesn't have the weather
     * information in it.
     * @throws NullPointerException When the received station data doesn't contain a timestamp.
     */
    @Throws(
        VolleyError::class,
        ArrayIndexOutOfBoundsException::class,
        IllegalStateException::class,
        IllegalArgumentException::class,
        NullPointerException::class,
    )
    @WorkerThread
    suspend fun fetchWeather(context: Context): WeatherState =
        provider.fetchWeather(context, *descriptor.expand())

    override fun toJson(): JSONObject = JSONObject().apply {
        putSerializable("descriptor", descriptor)
    }
}

val StationSample: Station
    get() = Station(
        object : HoldingDescriptor() {
            override val name: String = "sample"

            override val data: Map<String, Any> = mapOf("name" to "Testing Station")

            override val parameters: Map<String, KClass<*>> = mapOf("name" to String::class)

            override val capabilities: List<Capability> = emptyList()
        }
    )
