package com.arnyminerz.androidmatic.data

import android.content.Context
import android.nfc.FormatException
import androidx.annotation.WorkerThread
import com.android.volley.VolleyError
import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.putSerializable
import com.arnyminerz.androidmatic.data.numeric.GeoPoint
import com.arnyminerz.androidmatic.data.providers.MeteoclimaticProvider
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
    val title: String,
    val uid: String,
    val guid: String,
    val point: GeoPoint?,
    val description: String?,
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

            return timestamp to Station(
                title!!,
                uid,
                guid!!,
                GeoPoint.fromString(point!!),
                description!!,
                MeteoclimaticProvider.ProviderDescriptor.provide(uid),
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
                json.getString("title"),
                json.getString("uid"),
                json.getString("guid"),
                json.takeIf { it.has("point") }
                    ?.getJSONObject("point")
                    ?.let { GeoPoint.fromJson(it) },
                json.takeIf { it.has("description") }?.let { json.getString("description") },
                HoldingDescriptor.fromJson(json.getJSONObject("descriptor")),
            )
    }

    val name = title.substring(0, title.lastIndexOf('(')).trim()

    val location: String = title.substring(
        title.lastIndexOf('(') + 1,
        title.lastIndexOf(')'),
    )

    val provider: WeatherProvider = WeatherProvider.firstWithDescriptor(descriptor.name)
        ?: throw ClassNotFoundException("Could not find provider with descriptor named ${descriptor.name}")

    override fun toString(): String = uid

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
        put("title", title)
        put("uid", uid)
        put("guid", guid)
        put("point", point?.toJson())
        put("description", description)
        putSerializable("descriptor", descriptor)
    }
}

val StationSample: Station
    get() = Station(
        "Testing Station (Location)",
        "1234",
        "1234",
        GeoPoint(0.0, 0.0),
        null,
        object : HoldingDescriptor() {
            override val name: String = "sample"

            override val data: Map<String, Any> = emptyMap()

            override val parameters: Map<String, KClass<*>> = emptyMap()
        }
    )
