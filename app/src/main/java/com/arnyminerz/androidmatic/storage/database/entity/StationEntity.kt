package com.arnyminerz.androidmatic.storage.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.data.providers.model.HoldingDescriptor
import org.json.JSONObject

@Entity(
    tableName = "stations",
)
data class StationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "descriptor") val descriptor: String,
) {
    fun toStation(): Station =
        Station(
            HoldingDescriptor.fromJson(JSONObject(descriptor))
        )
}

/**
 * Converts the [Station]'s data into a [StationEntity].
 * @author Arnau Mora
 * @since 20220923
 */
fun Station.toEntity(): StationEntity =
    StationEntity(
        0,
        descriptor.toJson().toString()
    )
