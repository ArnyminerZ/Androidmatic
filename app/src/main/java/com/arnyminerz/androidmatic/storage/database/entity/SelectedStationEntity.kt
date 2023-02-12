package com.arnyminerz.androidmatic.storage.database.entity

import android.net.Uri
import android.util.Base64
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.providers.model.HoldingDescriptor
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

@Entity(
    tableName = "selected_stations",
)
data class SelectedStationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "station_uid") val stationUid: String,
    @ColumnInfo(name = "descriptor") val customDescriptor: String,
) {
    companion object {
        /**
         * Instantiates a new [SelectedStationEntity] from a link shared from the app.
         * @author Arnau Mora
         * @since 20230212
         * @param link The link to process.
         * @throws JSONException If the descriptor included in the URL is not valid.
         * @see [Station.share]
         */
        fun fromLink(link: Uri): SelectedStationEntity {
            val encodedDescriptor = link.path
            val decodedDescriptor = Base64.decode(encodedDescriptor, Base64.DEFAULT).toString()
            val descriptor = try {
                val jsonObject = JSONObject(decodedDescriptor)
                HoldingDescriptor.fromJson(jsonObject)
            } catch (e: JSONException) {
                Timber.e(e, "Could not parse descriptor JSON: $decodedDescriptor")
                throw e
            }
            val uid = descriptor.getValue<String>("uid")
            return SelectedStationEntity(0, uid, decodedDescriptor)
        }
    }

    override fun toString(): String = stationUid
}
