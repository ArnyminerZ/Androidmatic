package com.arnyminerz.androidmatic.storage.database

import androidx.room.RoomDatabase
import com.arnyminerz.androidmatic.storage.database.dao.StationsDao
import com.arnyminerz.androidmatic.storage.database.entity.SelectedStationEntity
import com.arnyminerz.androidmatic.storage.database.entity.StationEntity

@androidx.room.Database(
    entities = [StationEntity::class, SelectedStationEntity::class],
    version = 1,
)
abstract class Database: RoomDatabase() {
    abstract fun stationsDao(): StationsDao
}