package com.arnyminerz.androidmatic.storage.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "selected_stations",
)
data class SelectedStationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "station_uid") val stationUid: String,
    @ColumnInfo(name = "descriptor") val customDescriptor: String,
)
