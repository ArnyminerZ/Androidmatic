package com.arnyminerz.androidmatic.data.numeric

import org.json.JSONException
import org.json.JSONObject

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        /**
         * Converts an string to a [GeoPoint].
         *
         * Example:
         * `"43.56 -6.83" -> GeoPoint(43.56, -6.83)`
         * @author Arnau Mora
         * @since 20220923
         * @param str The string to convert.
         * @param glue The character between the components of the coordinates.
         * @return The converted [GeoPoint] instance.
         */
        @Throws(IllegalArgumentException::class)
        fun fromString(str: String, glue: Char = ' '): GeoPoint {
            if (!str.contains(glue))
                throw IllegalArgumentException("The given string \"$str\" doesn't contain the glue character '$glue'")
            val comp = str.split(glue)
            val lat = comp[0].toDoubleOrNull() ?: throw IllegalArgumentException("The given string doesn't contain valid doubles.")
            val lon = comp[1].toDoubleOrNull() ?: throw IllegalArgumentException("The given string doesn't contain valid doubles.")
            return GeoPoint(lat, lon)
        }

        /**
         * Instantiates a new [GeoPoint] from its definition in json.
         * @author Arnau Mora
         * @since 20220924
         * @param json The [JSONObject] to convert.
         * @return A new [GeoPoint] instance from the data in [json].
         * @throws JSONException When there's an error parsing the JSON object.
         */
        @Throws(JSONException::class)
        fun fromJson(json: JSONObject) = GeoPoint(
            json.getDouble("lat"),
            json.getDouble("lon"),
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("lat", latitude)
        put("lon", longitude)
    }
}
