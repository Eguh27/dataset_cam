package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "dataset_projects")
data class DatasetProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val targetAspectRatio: String = "1:1",
    val defaultResolution: String = "224x224",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dataset_classes",
    foreignKeys = [
        ForeignKey(
            entity = DatasetProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["projectId", "name"], unique = true)
    ]
)
data class DatasetClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 1L,
    val name: String,
    val colorHex: String = "#38BDF8", // Cyan / Neon blue accent
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dataset_samples",
    foreignKeys = [
        ForeignKey(
            entity = DatasetClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DatasetProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["classId"]),
        Index(value = ["projectId"]),
        Index(value = ["className"]),
        Index(value = ["parentSampleId"])
    ]
)
data class DatasetSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 1L,
    val classId: Long,
    val className: String,
    val filePath: String,
    val fileName: String,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val aspectRatio: String,
    val resolutionLabel: String,
    val fileSizeBytes: Long,
    val format: String, // PNG or JPG
    val isSegmented: Boolean = false,
    val isAugmented: Boolean = false,
    val augmentationType: String = "ORIGINAL",
    val parentSampleId: Long? = null,
    val split: String = "TRAIN", // TRAIN, VAL, TEST
    val createdAt: Long = System.currentTimeMillis()
)
