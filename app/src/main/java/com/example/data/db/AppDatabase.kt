package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DatasetClassEntity::class, DatasetSampleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun datasetDao(): DatasetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ml_dataset_studio.db"
                ).addCallback(DatabaseCallback(scope)).build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialClasses(database.datasetDao())
                    }
                }
            }

            suspend fun populateInitialClasses(dao: DatasetDao) {
                if (dao.getClassCount() == 0) {
                    dao.insertClass(
                        DatasetClassEntity(
                            name = "class_a",
                            colorHex = "#38BDF8", // Cyan
                            description = "Primary target subject"
                        )
                    )
                    dao.insertClass(
                        DatasetClassEntity(
                            name = "class_b",
                            colorHex = "#A855F7", // Purple
                            description = "Secondary comparison subject"
                        )
                    )
                    dao.insertClass(
                        DatasetClassEntity(
                            name = "normal",
                            colorHex = "#10B981", // Emerald Green
                            description = "Standard baseline condition"
                        )
                    )
                    dao.insertClass(
                        DatasetClassEntity(
                            name = "anomaly",
                            colorHex = "#F43F5E", // Rose Red
                            description = "Defect or outlier condition"
                        )
                    )
                }
            }
        }
    }
}
