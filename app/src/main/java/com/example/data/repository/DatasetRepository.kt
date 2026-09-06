package com.example.data.repository

import android.content.Context
import com.example.data.db.DatasetClassEntity
import com.example.data.db.DatasetDao
import com.example.data.db.DatasetSampleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DatasetRepository(
    private val context: Context,
    private val dao: DatasetDao
) {
    val allClasses: Flow<List<DatasetClassEntity>> = dao.getAllClasses()
    val allSamples: Flow<List<DatasetSampleEntity>> = dao.getAllSamples()
    val totalSampleCount: Flow<Int> = dao.getTotalSampleCount()

    fun getSamplesByClass(classId: Long): Flow<List<DatasetSampleEntity>> {
        return dao.getSamplesByClass(classId)
    }

    suspend fun getOrCreateClass(name: String, colorHex: String = "#38BDF8"): DatasetClassEntity {
        return withContext(Dispatchers.IO) {
            val sanitized = sanitizeFolderName(name)
            val existing = dao.getClassByName(sanitized)
            if (existing != null) {
                existing
            } else {
                val newEntity = DatasetClassEntity(name = sanitized, colorHex = colorHex)
                val id = dao.insertClass(newEntity)
                newEntity.copy(id = id)
            }
        }
    }

    suspend fun addClass(name: String, colorHex: String, description: String = ""): Long {
        return withContext(Dispatchers.IO) {
            val sanitized = sanitizeFolderName(name)
            val existing = dao.getClassByName(sanitized)
            if (existing != null) {
                existing.id
            } else {
                dao.insertClass(
                    DatasetClassEntity(
                        name = sanitized,
                        colorHex = colorHex,
                        description = description
                    )
                )
            }
        }
    }

    suspend fun deleteClass(datasetClass: DatasetClassEntity) {
        withContext(Dispatchers.IO) {
            // Delete folder on disk
            val folder = getDatasetFolder(datasetClass.name)
            if (folder.exists()) {
                folder.deleteRecursively()
            }
            dao.deleteClass(datasetClass)
        }
    }

    suspend fun insertSample(sample: DatasetSampleEntity): Long {
        return withContext(Dispatchers.IO) {
            dao.insertSample(sample)
        }
    }

    suspend fun insertSamples(samples: List<DatasetSampleEntity>): List<Long> {
        return withContext(Dispatchers.IO) {
            dao.insertSamples(samples)
        }
    }

    suspend fun updateSamples(samples: List<DatasetSampleEntity>) {
        withContext(Dispatchers.IO) {
            dao.updateSamples(samples)
        }
    }

    suspend fun updateSampleSplit(id: Long, split: String) {
        withContext(Dispatchers.IO) {
            dao.updateSampleSplit(id, split)
        }
    }

    suspend fun deleteSample(sample: DatasetSampleEntity) {
        withContext(Dispatchers.IO) {
            val file = File(sample.filePath)
            if (file.exists()) {
                file.delete()
            }
            dao.deleteSample(sample)
        }
    }

    suspend fun deleteSampleWithAugmentations(sample: DatasetSampleEntity) {
        withContext(Dispatchers.IO) {
            // Delete related augmentations if this is the parent
            val augmentations = dao.getAugmentationsForSample(sample.id)
            for (aug in augmentations) {
                val augFile = File(aug.filePath)
                if (augFile.exists()) augFile.delete()
                dao.deleteSample(aug)
            }
            val mainFile = File(sample.filePath)
            if (mainFile.exists()) mainFile.delete()
            dao.deleteSample(sample)
        }
    }

    fun getDatasetFolder(className: String): File {
        val root = File(context.filesDir, "datasets")
        val classDir = File(root, sanitizeFolderName(className))
        if (!classDir.exists()) {
            classDir.mkdirs()
        }
        return classDir
    }

    private fun sanitizeFolderName(name: String): String {
        return name.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase().ifEmpty { "unclassified" }
    }
}
