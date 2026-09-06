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
    entities = [DatasetProjectEntity::class, DatasetClassEntity::class, DatasetSampleEntity::class],
    version = 2,
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
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
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
                        populateInitialData(database.datasetDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: DatasetDao) {
                if (dao.getProjectCount() == 0) {
                    val defaultProjectId = dao.insertProject(
                        DatasetProjectEntity(
                            name = "Klasifikasi Umum (Standar)",
                            description = "Proyek dataset awal dengan kategori baseline machine learning.",
                            targetAspectRatio = "1:1",
                            defaultResolution = "224x224"
                        )
                    )

                    dao.insertClass(
                        DatasetClassEntity(
                            projectId = defaultProjectId,
                            name = "class_a",
                            colorHex = "#38BDF8", // Cyan
                            description = "Target subjek utama"
                        )
                    )
                    dao.insertClass(
                        DatasetClassEntity(
                            projectId = defaultProjectId,
                            name = "class_b",
                            colorHex = "#A855F7", // Purple
                            description = "Subjek pembanding sekunder"
                        )
                    )
                    dao.insertClass(
                        DatasetClassEntity(
                            projectId = defaultProjectId,
                            name = "normal",
                            colorHex = "#10B981", // Emerald Green
                            description = "Kondisi standar / baseline"
                        )
                    )
                    dao.insertClass(
                        DatasetClassEntity(
                            projectId = defaultProjectId,
                            name = "anomaly",
                            colorHex = "#F43F5E", // Rose Red
                            description = "Cacat atau kondisi outlier"
                        )
                    )
                }
            }
        }
    }
}
