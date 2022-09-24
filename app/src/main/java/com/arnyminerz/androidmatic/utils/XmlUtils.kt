package com.arnyminerz.androidmatic.utils

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The formatter used for the `pubDate` parameter in RSS feeds.
 * @author Arnau Mora
 * @since 20220924
 */
val xmlPubDateFormatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)

@Throws(IOException::class, XmlPullParserException::class)
fun XmlPullParser.readText(): String {
    var result = ""
    if (next() == XmlPullParser.TEXT) {
        result = text
        nextTag()
    }
    return result
}

fun XmlPullParser.readString(tag: String, namespace: String? = null): String {
    require(XmlPullParser.START_TAG, namespace, tag)
    val text = readText()
    require(XmlPullParser.END_TAG, namespace, tag)
    return text
}

@Throws(XmlPullParserException::class, IOException::class, IllegalStateException::class)
fun XmlPullParser.skip() {
    if (eventType != XmlPullParser.START_TAG)
        throw IllegalStateException()
    var depth = 1
    while (depth != 0)
        when (next()) {
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.START_TAG -> depth++
        }
}
