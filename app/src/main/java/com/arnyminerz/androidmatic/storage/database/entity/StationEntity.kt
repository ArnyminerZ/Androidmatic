package com.arnyminerz.androidmatic.storage.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.numeric.GeoPoint

@Entity(
    tableName = "stations",
)
data class StationEntity(
    @PrimaryKey val uid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "guid") val guid: String,
    @ColumnInfo(name = "latitude") val latitude: Double?,
    @ColumnInfo(name = "longitude") val longitude: Double?,
) {
    fun toStation(): Station =
        Station(
            title,
            uid,
            guid,
            if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
            null
        )
}

/**
 * Converts the [Station]'s data into a [StationEntity].
 * @author Arnau Mora
 * @since 20220923
 */
fun Station.toEntity(): StationEntity =
    StationEntity(uid, title, guid, point?.latitude, point?.longitude)
