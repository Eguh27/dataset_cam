package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DatasetDao {

    // ==========================================
    // PROJECTS
    // ==========================================
    @Query("SELECT * FROM dataset_projects ORDER BY updatedAt DESC, id DESC")
    fun getAllProjects(): Flow<List<DatasetProjectEntity>>

    @Query("SELECT * FROM dataset_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): DatasetProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: DatasetProjectEntity): Long

    @Update
    suspend fun updateProject(project: DatasetProjectEntity)

    @Delete
    suspend fun deleteProject(project: DatasetProjectEntity)

    @Query("DELETE FROM dataset_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("SELECT COUNT(*) FROM dataset_projects")
    suspend fun getProjectCount(): Int

    // ==========================================
    // CLASSES (Global & Scoped by Project)
    // ==========================================
    @Query("SELECT * FROM dataset_classes ORDER BY name ASC")
    fun getAllClasses(): Flow<List<DatasetClassEntity>>

    @Query("SELECT * FROM dataset_classes WHERE projectId = :projectId ORDER BY name ASC")
    fun getClassesForProject(projectId: Long): Flow<List<DatasetClassEntity>>

    @Query("SELECT * FROM dataset_classes WHERE id = :id LIMIT 1")
    suspend fun getClassById(id: Long): DatasetClassEntity?

    @Query("SELECT * FROM dataset_classes WHERE name = :name LIMIT 1")
    suspend fun getClassByName(name: String): DatasetClassEntity?

    @Query("SELECT * FROM dataset_classes WHERE projectId = :projectId AND name = :name LIMIT 1")
    suspend fun getClassByNameInProject(projectId: Long, name: String): DatasetClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(datasetClass: DatasetClassEntity): Long

    @Update
    suspend fun updateClass(datasetClass: DatasetClassEntity)

    @Delete
    suspend fun deleteClass(datasetClass: DatasetClassEntity)

    @Query("SELECT COUNT(*) FROM dataset_classes")
    suspend fun getClassCount(): Int

    @Query("SELECT COUNT(*) FROM dataset_classes WHERE projectId = :projectId")
    suspend fun getClassCountForProject(projectId: Long): Int

    // ==========================================
    // SAMPLES (Global & Scoped by Project)
    // ==========================================
    @Query("SELECT * FROM dataset_samples ORDER BY createdAt DESC")
    fun getAllSamples(): Flow<List<DatasetSampleEntity>>

    @Query("SELECT * FROM dataset_samples WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getSamplesForProject(projectId: Long): Flow<List<DatasetSampleEntity>>

    @Query("SELECT * FROM dataset_samples WHERE classId = :classId ORDER BY createdAt DESC")
    fun getSamplesByClass(classId: Long): Flow<List<DatasetSampleEntity>>

    @Query("SELECT * FROM dataset_samples WHERE projectId = :projectId AND classId = :classId ORDER BY createdAt DESC")
    fun getSamplesByClassInProject(projectId: Long, classId: Long): Flow<List<DatasetSampleEntity>>

    @Query("SELECT * FROM dataset_samples WHERE isAugmented = 0 ORDER BY createdAt DESC")
    fun getPrimarySamples(): Flow<List<DatasetSampleEntity>>

    @Query("SELECT * FROM dataset_samples WHERE projectId = :projectId AND isAugmented = 0 ORDER BY createdAt DESC")
    fun getPrimarySamplesForProject(projectId: Long): Flow<List<DatasetSampleEntity>>

    @Query("SELECT * FROM dataset_samples WHERE parentSampleId = :parentId")
    suspend fun getAugmentationsForSample(parentId: Long): List<DatasetSampleEntity>

    @Query("SELECT * FROM dataset_samples WHERE id = :id LIMIT 1")
    suspend fun getSampleById(id: Long): DatasetSampleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: DatasetSampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<DatasetSampleEntity>): List<Long>

    @Update
    suspend fun updateSamples(samples: List<DatasetSampleEntity>)

    @Query("UPDATE dataset_samples SET split = :split WHERE id = :id")
    suspend fun updateSampleSplit(id: Long, split: String)

    @Delete
    suspend fun deleteSample(sample: DatasetSampleEntity)

    @Delete
    suspend fun deleteSamples(samples: List<DatasetSampleEntity>)

    @Query("DELETE FROM dataset_samples WHERE id IN (:ids)")
    suspend fun deleteSamplesByIds(ids: List<Long>)

    @Query("DELETE FROM dataset_samples WHERE id = :id")
    suspend fun deleteSampleById(id: Long)

    @Query("DELETE FROM dataset_samples WHERE classId = :classId")
    suspend fun deleteSamplesByClass(classId: Long)

    @Query("DELETE FROM dataset_samples WHERE projectId = :projectId")
    suspend fun deleteSamplesForProject(projectId: Long)

    @Query("SELECT COUNT(*) FROM dataset_samples")
    fun getTotalSampleCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dataset_samples WHERE projectId = :projectId")
    fun getSampleCountForProject(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM dataset_samples WHERE classId = :classId")
    fun getSampleCountForClass(classId: Long): Flow<Int>
}
