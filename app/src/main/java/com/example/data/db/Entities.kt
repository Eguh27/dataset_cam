package com.example.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "dataset_classes")
data class DatasetClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
        )
    ],
    indices = [
        Index(value = ["classId"]),
        Index(value = ["className"]),
        Index(value = ["parentSampleId"])
    ]
)
data class DatasetSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
