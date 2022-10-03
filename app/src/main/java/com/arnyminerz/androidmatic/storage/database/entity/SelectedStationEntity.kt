package com.arnyminerz.androidmatic.storage.database.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arnyminerz.androidmatic.data.providers.model.HoldingDescriptor
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
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
         * Instantiates a new [SelectedStationEntity] from a pending dynamic link.
         * @author Arnau Mora
         * @since 20221003
         */
        fun fromDynamicLink(pendingDynamicLinkData: PendingDynamicLinkData?): SelectedStationEntity {
            // Get deep link from result (may be null if no link is found)
            val deepLink: Uri = pendingDynamicLinkData?.link
                ?: throw NullPointerException("The dynamic link doesn't have any deep link.")

            Timber.i("Link: $deepLink. Extensions: ${pendingDynamicLinkData.extensions}")

            Timber.i("Loading link...")
            if (!deepLink.queryParameterNames.contains("descriptor"))
                throw IllegalStateException("The deep link doesn't contain any descriptor: $deepLink")
            val rawDescriptorJson: String = deepLink.getQueryParameter("descriptor")!!
            val descriptor = try {
                val jsonObject = JSONObject(rawDescriptorJson)
                HoldingDescriptor.fromJson(jsonObject)
            } catch (e: JSONException) {
                Timber.e(e, "Could not parse descriptor JSON: $rawDescriptorJson")
                throw e
            }
            val uid = descriptor.getValue<String>("uid")
            return SelectedStationEntity(0, uid, rawDescriptorJson)
        }
    }

    override fun toString(): String = stationUid
}
