package com.arnyminerz.androidmatic.storage.database.dao

import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.arnyminerz.androidmatic.data.Station
import com.arnyminerz.androidmatic.storage.database.entity.SelectedStationEntity
import com.arnyminerz.androidmatic.storage.database.entity.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationsDao {
    @WorkerThread
    @Query("SELECT * FROM stations")
    suspend fun getAll(): List<StationEntity>

    @Query("SELECT * FROM stations")
    fun getAllFlow(): Flow<List<StationEntity>>

    @Query("SELECT * FROM selected_stations")
    fun getEnabledStations(): Flow<List<SelectedStationEntity>>

    @Query("DELETE FROM stations")
    @WorkerThread
    suspend fun deleteAll()

    @Insert
    @WorkerThread
    suspend fun insertAll(vararg stations: StationEntity)

    @Insert
    @WorkerThread
    suspend fun enableStation(station: SelectedStationEntity)

    @Query("DELETE FROM selected_stations WHERE station_uid=:stationUid")
    @WorkerThread
    suspend fun disableStation(stationUid: String)
}

@WorkerThread
suspend fun StationsDao.enableStation(station: Station) =
    enableStation(
        SelectedStationEntity(
            0,
            station.uid,
            station.descriptor.toJson().toString(),
        )
    )
