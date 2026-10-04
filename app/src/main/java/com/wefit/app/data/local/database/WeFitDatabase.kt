package com.wefit.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wefit.app.data.local.dao.ExerciseSessionDao
import com.wefit.app.data.local.entity.ExerciseSessionEntity

@Database(entities = [ExerciseSessionEntity::class], version = 1, exportSchema = false)
abstract class WeFitDatabase : RoomDatabase() {
    abstract fun exerciseSessionDao(): ExerciseSessionDao

    companion object {
        @Volatile
        private var INSTANCE: WeFitDatabase? = null

        fun getInstance(context: Context): WeFitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeFitDatabase::class.java,
                    "wefit_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}