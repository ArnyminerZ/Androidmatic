package com.arnyminerz.androidmatic.data

import android.util.Xml
import androidx.annotation.WorkerThread
import com.arnyminerz.androidmatic.utils.skip
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Date

// We don't use namespaces
private val ns: String? = null

@WorkerThread
@Throws(XmlPullParserException::class, IOException::class)
fun parseFeed(feed: String): StationFeedResult {
    val parser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(feed.reader())
    }
    parser.nextTag()
    return readFeed(parser)
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
