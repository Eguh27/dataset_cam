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
    // Classes
    @Query("SELECT * FROM dataset_classes ORDER BY name ASC")
    fun getAllClasses(): Flow<List<DatasetClassEntity>>

    @Query("SELECT * FROM dataset_classes WHERE id = :id LIMIT 1")
    suspend fun getClassById(id: Long): DatasetClassEntity?

    @Query("SELECT * FROM dataset_classes WHERE name = :name LIMIT 1")
    suspend fun getClassByName(name: String): DatasetClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(datasetClass: DatasetClassEntity): Long

    @Update
    suspend fun updateClass(datasetClass: DatasetClassEntity)

    @Delete
    suspend fun deleteClass(datasetClass: DatasetClassEntity)

    // Samples
    @Query("SELECT * FROM dataset_samples ORDER BY createdAt DESC")
    fun getAllSamples(): Flow<List<DatasetSampleEntity>>

    @Query("SELECT * FROM dataset_samples WHERE classId = :classId ORDER BY createdAt DESC")
    fun getSamplesByClass(classId: Long): Flow<List<DatasetSampleEntity>>

    @Query("SELECT * FROM dataset_samples WHERE isAugmented = 0 ORDER BY createdAt DESC")
    fun getPrimarySamples(): Flow<List<DatasetSampleEntity>>

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

    @Query("DELETE FROM dataset_samples WHERE id = :id")
    suspend fun deleteSampleById(id: Long)

    @Query("DELETE FROM dataset_samples WHERE classId = :classId")
    suspend fun deleteSamplesByClass(classId: Long)

    @Query("SELECT COUNT(*) FROM dataset_samples")
    fun getTotalSampleCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM dataset_samples WHERE classId = :classId")
    fun getSampleCountForClass(classId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM dataset_classes")
    suspend fun getClassCount(): Int
}
