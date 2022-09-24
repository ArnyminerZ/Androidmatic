package com.arnyminerz.androidmatic.singleton

import android.content.Context
import androidx.room.Room
import com.arnyminerz.androidmatic.storage.database.Database

class DatabaseSingleton {
    companion object {
        @Volatile
        private var INSTANCE: Database? = null

        /**
         * Gets the [DatabaseSingleton] instance, or instantiates a new one if none available.
         * @author Arnau Mora
         * @since 20220923
         * @return The [DatabaseSingleton] instance
         */
        fun getInstance(applicationContext: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    applicationContext,
                    Database::class.java,
                    "androidmatic",
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}