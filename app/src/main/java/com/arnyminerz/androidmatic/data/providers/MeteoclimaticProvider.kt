package com.arnyminerz.androidmatic.data.providers

import android.content.Context
import android.util.Xml
import com.android.volley.VolleyError
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.StationFeedResult
import com.arnyminerz.androidmatic.data.WeatherState
import com.arnyminerz.androidmatic.data.numeric.GeoPoint
import com.arnyminerz.androidmatic.data.numeric.MaxValue
import com.arnyminerz.androidmatic.data.numeric.MinMaxValue
import com.arnyminerz.androidmatic.data.providers.model.Descriptor
import com.arnyminerz.androidmatic.data.providers.model.WeatherListProvider
import com.arnyminerz.androidmatic.exceptions.MissingParameterException
import com.arnyminerz.androidmatic.singleton.VolleySingleton
import com.arnyminerz.androidmatic.utils.skip
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import timber.log.Timber
import java.io.IOException
import java.util.Date
import kotlin.reflect.KClass

// We don't use namespaces
private val ns: String? = null

class MeteoclimaticProvider : WeatherListProvider() {
    override val displayName: String = "Meteoclimatic"

    override val providerName: String = "meteoclimatic"

    override val descriptor = ProviderDescriptor

    object ProviderDescriptor : Descriptor() {
        override val name: String = "meteoclimatic_descriptor"

        override val parameters: Map<String, KClass<*>> = mapOf(
            "name" to String::class,
            "uid" to String::class,
            "guid" to String::class,
            "location" to String::class,
            "point" to GeoPoint::class,
            "description" to String::class,
        )

        override val capabilities: List<Capability> =
            listOf(Capability.LISTING, Capability.LOCATION)

        fun provide(
            uid: String,
            name: String,
            location: String,
            guid: String,
            point: GeoPoint,
            description: String
        ) =
            provide(
                "uid" to uid,
                "name" to name,
                "location" to location,
                "guid" to guid,
                "point" to point,
                "description" to description,
            )
    }

    override suspend fun list(context: Context): List<Station> =
        fetch(context, mapOf("uid" to "ES")).stations

    override suspend fun fetch(
        context: Context,
        params: Map<String, Any>,
    ): StationFeedResult {
        Timber.d("Getting station ($this) data from Meteoclimatic...")
        val uid = params["uid"]
            ?: throw IllegalArgumentException("params doesn't have any \"uid\".")
        Timber.v("Found uid in parameters: $uid")
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
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(feed.reader())
        }
        parser.nextTag()
        return readFeed(parser)
    }

    /**
     * Fetches the feather state for the given station.
     * @author Arnau Mora
     * @since 20221029
     * @param context The context that is requesting the data.
     * @param params Extra parameters for describing the data load. Requires:
     * * `uid`: The uid of the station.
     * * `description`: The description of the station, contains the weather data.
     * @throws MissingParameterException When the given [params] doesn't contain a required parameter.
     * @throws ArrayIndexOutOfBoundsException When the fetch result has not given any stations.
     * @throws IllegalStateException When the loaded description misses some data.
     */
    @Throws(
        MissingParameterException::class,
        ArrayIndexOutOfBoundsException::class,
        IllegalStateException::class,
    )
    override suspend fun fetchWeather(
        context: Context,
        params: Map<String, Any>
    ): WeatherState {
        val uid = params["uid"]
            ?: throw MissingParameterException("params doesn't have any \"uid\".")
        val descriptionRaw = params["description"] as? String
            ?: throw MissingParameterException("params doesn't have any \"description\".")

        val feedResult = fetch(context, params)
        val station = feedResult.stations
            // There should only be one station. Handle null just in case it wasn't found
            .firstOrNull()
            ?: throw ArrayIndexOutOfBoundsException("Could not find any stations in feed.")
        val description = descriptionRaw
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val dataRaw = description
            .find { it.startsWith("[[<$uid", true) }
            ?: error("Could not find weather data (<$uid) in description. Description: $description")
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
            station.name,
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

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): StationFeedResult {
        val entries = mutableListOf<Station>()
        var timestamp: Date? = null

        parser.require(XmlPullParser.START_TAG, ns, "rss")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue

            parser.require(XmlPullParser.START_TAG, ns, "channel")
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG)
                    continue
                if (parser.name.equals("item", true))
                    Station.fromXml(parser).let { (date, station) ->
                        entries.add(station)
                        date.takeIf { it.time > (timestamp?.time ?: 0) }
                            ?.let { timestamp = it }
                    }
                else
                    parser.skip()
            }
        }
        return StationFeedResult(timestamp ?: Date(), entries)
    }
}