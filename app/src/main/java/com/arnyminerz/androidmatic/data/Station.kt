package com.arnyminerz.androidmatic.data

import android.content.Context
import android.nfc.FormatException
import androidx.annotation.WorkerThread
import com.android.volley.VolleyError
import com.arnyminerz.androidmatic.data.model.JsonSerializable
import com.arnyminerz.androidmatic.data.model.JsonSerializer
import com.arnyminerz.androidmatic.data.numeric.GeoPoint
import com.arnyminerz.androidmatic.data.numeric.MaxValue
import com.arnyminerz.androidmatic.data.numeric.MinMaxValue
import com.arnyminerz.androidmatic.singleton.VolleySingleton
import com.arnyminerz.androidmatic.utils.readString
import com.arnyminerz.androidmatic.utils.skip
import com.arnyminerz.androidmatic.utils.xmlPubDateFormatter
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.util.Date

data class Station(
    val title: String,
    val uid: String,
    val guid: String,
    val point: GeoPoint,
    val description: String?,
): JsonSerializable {
    companion object: JsonSerializer<Station> {
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

            return timestamp to Station(
                title!!,
                link!!.substring(link.lastIndexOf('/') + 1),
                guid!!,
                GeoPoint.fromString(point!!),
                description!!,
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
        override fun fromJson(json: JSONObject): Station =
            Station(
                json.getString("title"),
                json.getString("uid"),
                json.getString("guid"),
                GeoPoint.fromJson(json.getJSONObject("point")),
                json.takeIf { it.has("description") }?.let { json.getString("description") }
            )
    }

    val name = title.substring(0, title.lastIndexOf('(')).trim()

    val location = title.substring(
        title.lastIndexOf('(') + 1,
        title.lastIndexOf(')'),
    )

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
    suspend fun fetchWeather(context: Context): WeatherState {
        Timber.d("Getting station ($this) data from Meteoclimatic...")
        val url = "https://www.meteoclimatic.net/feed/rss/$uid"
        val feed: String = try {
            VolleySingleton
                .getInstance(context)
                .getString(url)
                // Remove all comments otherwise the comments skips them
                .replace("<!--", "").replace("-->", "")
                // Remove all data blocks
                .let {
                    var fd = it
                    while (fd.contains("<![CDATA[")) {
                        val i = fd.indexOf("<![CDATA[")
                        fd = fd.removeRange(i, fd.indexOf("]]>", i) + 3)
                    }
                    fd
                }
                // Add new cdata tags
                .replace("<description>", "<description><![CDATA[")
                .replace("</description>", "]]></description>")
        } catch (e: VolleyError) {
            Timber.e(e, "Could not fetch data from Meteoclimatic ($url).")
            throw e
        }
        val feedResult = parseFeed(feed)
        val station = feedResult.stations
            // There should only be one station. Handle null just in case it wasn't found
            .firstOrNull()
            ?: throw ArrayIndexOutOfBoundsException("Could not find any stations in feed.")
        val description = station.description?.split('\n')
            ?: throw IllegalStateException("Station doesn't have any description data.")
        val dataRaw = description.find { it.startsWith("[[<$uid") }
            ?: throw IllegalArgumentException("Could not find weather data in description. Description: $description")
        val dataBlocks = dataRaw
            .substring(dataRaw.indexOf(';') + 1, dataRaw.lastIndexOf(">]]"))
            // Remove all parenthesis
            .replace("(", "")
            .replace(")", "")
            // Replace all colons with dots for float conversion
            .replace(',', '.')
            // Split on semicolons
            .split(';')
        Timber.i("dataBlocks: {${dataBlocks.joinToString(", ")}}")
        return WeatherState(
            feedResult.timestamp,
            MinMaxValue(
                dataBlocks[0].toDouble(),
                dataBlocks[1].toDouble(),
                dataBlocks[2].toDouble(),
            ),
            MinMaxValue(
                dataBlocks[4].toDouble(),
                dataBlocks[5].toDouble(),
                dataBlocks[6].toDouble(),
            ),
            MinMaxValue(
                dataBlocks[7].toDouble(),
                dataBlocks[8].toDouble(),
                dataBlocks[9].toDouble(),
            ),
            MaxValue(
                dataBlocks[10].toDouble(),
                dataBlocks[11].toDouble(),
            ),
            dataBlocks[12].toInt(),
            dataBlocks[13].toDouble(),
            dataBlocks[3],
        )
    }

    override fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("uid", uid)
        put("guid", guid)
        put("point", point.toJson())
        put("description", description)
    }
}

val StationSample: Station
    get() = Station(
        "Testing Station (Location)",
        "1234",
        "1234",
        GeoPoint(0.0, 0.0),
        null,
    )
