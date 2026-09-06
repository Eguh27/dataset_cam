package com.example.data.repository

import android.content.Context
import com.example.data.db.DatasetClassEntity
import com.example.data.db.DatasetDao
import com.example.data.db.DatasetProjectEntity
import com.example.data.db.DatasetSampleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class DatasetRepository(
    private val context: Context,
    private val dao: DatasetDao
) {
    // Projects
    val allProjects: Flow<List<DatasetProjectEntity>> = dao.getAllProjects()

    suspend fun getProjectById(id: Long): DatasetProjectEntity? {
        return withContext(Dispatchers.IO) {
            dao.getProjectById(id)
        }
    }

    suspend fun getProjectCount(): Int {
        return withContext(Dispatchers.IO) {
            dao.getProjectCount()
        }
    }

    suspend fun createProject(
        name: String,
        description: String = "",
        targetAspectRatio: String = "1:1",
        defaultResolution: String = "224x224",
        initialClasses: List<Pair<String, String>> = emptyList()
    ): DatasetProjectEntity {
        return withContext(Dispatchers.IO) {
            val project = DatasetProjectEntity(
                name = name.trim().ifEmpty { "Proyek Tanpa Nama" },
                description = description.trim(),
                targetAspectRatio = targetAspectRatio,
                defaultResolution = defaultResolution,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val projectId = dao.insertProject(project)

            // Create initial classes if provided
            if (initialClasses.isNotEmpty()) {
                for ((className, color) in initialClasses) {
                    val sanitized = sanitizeFolderName(className)
                    if (sanitized.isNotEmpty()) {
                        dao.insertClass(
                            DatasetClassEntity(
                                projectId = projectId,
                                name = sanitized,
                                colorHex = color,
                                description = "Kategori untuk ${project.name}"
                            )
                        )
                    }
                }
            } else {
                // Default starter classes
                dao.insertClass(DatasetClassEntity(projectId = projectId, name = "class_a", colorHex = "#38BDF8", description = "Target utama"))
                dao.insertClass(DatasetClassEntity(projectId = projectId, name = "class_b", colorHex = "#A855F7", description = "Target sekunder"))
            }

            project.copy(id = projectId)
        }
    }

    suspend fun deleteProject(project: DatasetProjectEntity) {
        withContext(Dispatchers.IO) {
            // Delete files on disk for this project
            val projectDir = File(context.filesDir, "datasets/proj_${project.id}")
            if (projectDir.exists()) {
                projectDir.deleteRecursively()
            }
            dao.deleteProject(project)
        }
    }

    // Classes
    val allClasses: Flow<List<DatasetClassEntity>> = dao.getAllClasses()

    fun getClassesForProject(projectId: Long): Flow<List<DatasetClassEntity>> {
        return dao.getClassesForProject(projectId)
    }

    suspend fun addClassToProject(
        projectId: Long,
        name: String,
        colorHex: String,
        description: String = ""
    ): Long {
        return withContext(Dispatchers.IO) {
            val sanitized = sanitizeFolderName(name)
            val existing = dao.getClassByNameInProject(projectId, sanitized)
            if (existing != null) {
                existing.id
            } else {
                dao.insertClass(
                    DatasetClassEntity(
                        projectId = projectId,
                        name = sanitized,
                        colorHex = colorHex,
                        description = description
                    )
                )
            }
        }
    }

    suspend fun getOrCreateClassInProject(
        projectId: Long,
        name: String,
        colorHex: String = "#38BDF8"
    ): DatasetClassEntity {
        return withContext(Dispatchers.IO) {
            val sanitized = sanitizeFolderName(name)
            val existing = dao.getClassByNameInProject(projectId, sanitized)
            if (existing != null) {
                existing
            } else {
                val newEntity = DatasetClassEntity(projectId = projectId, name = sanitized, colorHex = colorHex)
                val id = dao.insertClass(newEntity)
                newEntity.copy(id = id)
            }
        }
    }

    suspend fun addClass(name: String, colorHex: String, description: String = ""): Long {
        return addClassToProject(1L, name, colorHex, description)
    }

    suspend fun getOrCreateClass(name: String, colorHex: String = "#38BDF8"): DatasetClassEntity {
        return getOrCreateClassInProject(1L, name, colorHex)
    }

    suspend fun deleteClass(datasetClass: DatasetClassEntity) {
        withContext(Dispatchers.IO) {
            val folder = getDatasetFolder(datasetClass.projectId, datasetClass.name)
            if (folder.exists()) {
                folder.deleteRecursively()
            }
            dao.deleteClass(datasetClass)
        }
    }

    // Samples
    val allSamples: Flow<List<DatasetSampleEntity>> = dao.getAllSamples()
    val totalSampleCount: Flow<Int> = dao.getTotalSampleCount()

    fun getSamplesForProject(projectId: Long): Flow<List<DatasetSampleEntity>> {
        return dao.getSamplesForProject(projectId)
    }

    fun getSamplesByClass(classId: Long): Flow<List<DatasetSampleEntity>> {
        return dao.getSamplesByClass(classId)
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

    suspend fun deleteSamplesBatch(samples: List<DatasetSampleEntity>) {
        withContext(Dispatchers.IO) {
            for (sample in samples) {
                val augmentations = dao.getAugmentationsForSample(sample.id)
                for (aug in augmentations) {
                    val augFile = File(aug.filePath)
                    if (augFile.exists()) augFile.delete()
                    dao.deleteSample(aug)
                }
                val mainFile = File(sample.filePath)
                if (mainFile.exists()) mainFile.delete()
            }
            dao.deleteSamples(samples)
        }
    }

    fun getDatasetFolder(projectId: Long, className: String): File {
        val root = File(context.filesDir, "datasets/proj_${projectId}")
        val classDir = File(root, sanitizeFolderName(className))
        if (!classDir.exists()) {
            classDir.mkdirs()
        }
        return classDir
    }

    fun getDatasetFolder(className: String): File {
        return getDatasetFolder(1L, className)
    }

    private fun sanitizeFolderName(name: String): String {
        return name.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase().ifEmpty { "unclassified" }
    }
}
