package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.db.DatasetClassEntity
import com.example.data.db.DatasetSampleEntity
import com.example.data.model.DatasetFolderConfig
import com.example.data.model.FileNamingScheme
import com.example.data.model.FolderStructureType
import com.example.data.model.MetadataExportFormat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExportResult(
    val file: File,
    val format: MetadataExportFormat,
    val totalSamples: Int,
    val totalClasses: Int
)

object DatasetExporter {

    /**
     * Standard vision annotations format (Detailed hierarchical JSON).
     */
    fun generateJson(
        samples: List<DatasetSampleEntity>,
        classes: List<DatasetClassEntity>,
        datasetName: String = "Vision_Dataset",
        config: DatasetFolderConfig = DatasetFolderConfig()
    ): String {
        val root = JSONObject()
        root.put("dataset_name", datasetName)
        root.put("version", "1.1.0")
        root.put("generator", "Dataset Studio ML Pipeline Exporter")
        root.put("export_timestamp", System.currentTimeMillis())
        root.put(
            "export_date",
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        )
        root.put("total_samples", samples.size)
        root.put("folder_structure_type", config.structureType.name)
        root.put("file_naming_scheme", config.namingScheme.name)

        // Classes array
        val classArray = JSONArray()
        val classCounts = JSONObject()
        for (cls in classes) {
            val count = samples.count { it.className == cls.name }
            classCounts.put(cls.name, count)
            val cObj = JSONObject()
            cObj.put("id", cls.id)
            cObj.put("name", cls.name)
            cObj.put("color", cls.colorHex)
            cObj.put("sample_count", count)
            classArray.put(cObj)
        }
        root.put("classes", classArray)
        root.put("class_distribution", classCounts)

        // Split distribution
        val splits = JSONObject()
        splits.put("train", samples.count { it.split.equals("TRAIN", true) })
        splits.put("val", samples.count { it.split.equals("VAL", true) })
        splits.put("test", samples.count { it.split.equals("TEST", true) })
        root.put("split_distribution", splits)

        // Images array
        val imagesArray = JSONArray()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val classCounter = mutableMapOf<String, Int>()

        for (sample in samples) {
            val idx = (classCounter[sample.className] ?: 0) + 1
            classCounter[sample.className] = idx
            val relativePath = resolveExportPath(sample, config, idx)

            val imgObj = JSONObject()
            imgObj.put("id", sample.id)
            imgObj.put("filename", sample.fileName)
            imgObj.put("label", sample.className)
            imgObj.put("class_id", sample.classId)
            imgObj.put("relative_path", relativePath)
            imgObj.put("width", sample.width)
            imgObj.put("height", sample.height)
            imgObj.put("aspect_ratio", sample.aspectRatio)
            imgObj.put("resolution_preset", sample.resolutionLabel)
            imgObj.put("file_size_bytes", sample.fileSizeBytes)
            imgObj.put("format", sample.format)
            imgObj.put("is_segmented", sample.isSegmented)
            imgObj.put("is_augmented", sample.isAugmented)
            imgObj.put("augmentation_type", sample.augmentationType)
            imgObj.put("parent_sample_id", sample.parentSampleId ?: JSONObject.NULL)
            imgObj.put("split", sample.split)
            imgObj.put("created_at", dateFormat.format(Date(sample.createdAt)))
            imagesArray.put(imgObj)
        }
        root.put("images", imagesArray)

        return root.toString(2)
    }

    /**
     * Official Microsoft COCO Vision Format.
     */
    fun generateCocoJson(
        samples: List<DatasetSampleEntity>,
        classes: List<DatasetClassEntity>,
        datasetName: String = "Vision_Dataset",
        config: DatasetFolderConfig = DatasetFolderConfig()
    ): String {
        val root = JSONObject()

        // 1. Info
        val infoObj = JSONObject()
        infoObj.put("description", "$datasetName generated via Dataset Studio")
        infoObj.put("url", "https://ai.studio")
        infoObj.put("version", "1.0")
        infoObj.put("year", 2026)
        infoObj.put("contributor", "Dataset Studio User")
        infoObj.put("date_created", SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date()))
        root.put("info", infoObj)
        root.put("licenses", JSONArray())

        // 2. Categories
        val categoriesArray = JSONArray()
        val classIdMap = mutableMapOf<String, Long>()
        for (cls in classes) {
            classIdMap[cls.name] = cls.id
            val catObj = JSONObject()
            catObj.put("id", cls.id)
            catObj.put("name", cls.name)
            catObj.put("supercategory", "object")
            categoriesArray.put(catObj)
        }
        root.put("categories", categoriesArray)

        // 3. Images & 4. Annotations
        val imagesArray = JSONArray()
        val annotationsArray = JSONArray()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val classCounter = mutableMapOf<String, Int>()

        for (sample in samples) {
            val idx = (classCounter[sample.className] ?: 0) + 1
            classCounter[sample.className] = idx
            val targetRelativePath = resolveExportPath(sample, config, idx)

            val imgObj = JSONObject()
            imgObj.put("id", sample.id)
            imgObj.put("file_name", targetRelativePath)
            imgObj.put("width", sample.width)
            imgObj.put("height", sample.height)
            imgObj.put("date_captured", dateFormat.format(Date(sample.createdAt)))
            imagesArray.put(imgObj)

            // Image-level category annotation
            val annObj = JSONObject()
            annObj.put("id", sample.id)
            annObj.put("image_id", sample.id)
            annObj.put("category_id", classIdMap[sample.className] ?: sample.classId)
            annObj.put("area", sample.width * sample.height)
            annObj.put("bbox", JSONArray(listOf(0, 0, sample.width, sample.height)))
            annObj.put("iscrowd", 0)
            annotationsArray.put(annObj)
        }
        root.put("images", imagesArray)
        root.put("annotations", annotationsArray)

        return root.toString(2)
    }

    /**
     * Complete CSV index matrix for Pandas DataFrame or PyTorch custom Dataset.
     */
    fun generateCsv(
        samples: List<DatasetSampleEntity>,
        config: DatasetFolderConfig = DatasetFolderConfig()
    ): String {
        val sb = StringBuilder()
        sb.append("id,filename,label,class_id,split,relative_path,width,height,aspect_ratio,resolution_preset,file_size_bytes,format,is_segmented,is_augmented,augmentation_type,created_at\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val classCounter = mutableMapOf<String, Int>()

        for (s in samples) {
            val idx = (classCounter[s.className] ?: 0) + 1
            classCounter[s.className] = idx
            val targetPath = resolveExportPath(s, config, idx)

            sb.append(s.id).append(",")
            sb.append(escapeCsv(s.fileName)).append(",")
            sb.append(escapeCsv(s.className)).append(",")
            sb.append(s.classId).append(",")
            sb.append(s.split).append(",")
            sb.append(escapeCsv(targetPath)).append(",")
            sb.append(s.width).append(",")
            sb.append(s.height).append(",")
            sb.append(s.aspectRatio).append(",")
            sb.append(escapeCsv(s.resolutionLabel)).append(",")
            sb.append(s.fileSizeBytes).append(",")
            sb.append(s.format).append(",")
            sb.append(s.isSegmented).append(",")
            sb.append(s.isAugmented).append(",")
            sb.append(escapeCsv(s.augmentationType)).append(",")
            sb.append(dateFormat.format(Date(s.createdAt))).append("\n")
        }
        return sb.toString()
    }

    /**
     * Fast.ai / Keras minimal CSV format: name, label, is_valid.
     */
    fun generateFastAiCsv(
        samples: List<DatasetSampleEntity>,
        config: DatasetFolderConfig = DatasetFolderConfig()
    ): String {
        val sb = StringBuilder()
        sb.append("name,label,is_valid\n")
        val classCounter = mutableMapOf<String, Int>()

        for (s in samples) {
            val idx = (classCounter[s.className] ?: 0) + 1
            classCounter[s.className] = idx
            val targetPath = resolveExportPath(s, config, idx)
            val isValid = s.split.equals("VAL", true) || s.split.equals("TEST", true)
            sb.append(escapeCsv(targetPath)).append(",")
            sb.append(escapeCsv(s.className)).append(",")
            sb.append(isValid).append("\n")
        }
        return sb.toString()
    }

    /**
     * YOLOv8 / YOLOv9 data.yaml file specification.
     */
    fun generateYoloYaml(
        classes: List<DatasetClassEntity>,
        datasetName: String = "vision_dataset",
        config: DatasetFolderConfig = DatasetFolderConfig()
    ): String {
        val sb = StringBuilder()
        sb.append("# Ultralytics YOLO dataset configuration for $datasetName\n")
        sb.append("path: .\n")
        when (config.structureType) {
            FolderStructureType.TRAIN_VAL_TEST_SPLIT -> {
                sb.append("train: train/\n")
                sb.append("val: val/\n")
                sb.append("test: test/\n")
            }
            FolderStructureType.CLASS_DIRECTORIES -> {
                sb.append("train: images/\n")
                sb.append("val: images/\n")
            }
            else -> {
                sb.append("train: images/\n")
                sb.append("val: images/\n")
            }
        }
        sb.append("\n# Number of classes\n")
        sb.append("nc: ").append(classes.size).append("\n\n")
        sb.append("# Class names\n")
        sb.append("names:\n")
        classes.forEachIndexed { index, cls ->
            sb.append("  ").append(index).append(": ").append(cls.name).append("\n")
        }
        return sb.toString()
    }

    /**
     * Resolves the target relative path inside the dataset bundle based on selected folder structure and naming scheme.
     */
    fun resolveExportPath(
        sample: DatasetSampleEntity,
        config: DatasetFolderConfig,
        indexInClass: Int
    ): String {
        val ext = if (sample.format.equals("PNG", true)) "png" else "jpg"
        val cleanClassName = sample.className.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()

        // Calculate file name based on namingScheme
        val resolvedFileName = when (config.namingScheme) {
            FileNamingScheme.CLASS_INDEX -> {
                "${cleanClassName}_${String.format(Locale.US, "%04d", indexInClass)}.$ext"
            }
            FileNamingScheme.SPLIT_CLASS_INDEX -> {
                val splitPrefix = sample.split.lowercase()
                "${splitPrefix}_${cleanClassName}_${String.format(Locale.US, "%04d", indexInClass)}.$ext"
            }
            FileNamingScheme.CLEAN_HASH -> {
                val hash = Integer.toHexString(sample.filePath.hashCode()).takeLast(6)
                "sample_${sample.id}_$hash.$ext"
            }
            FileNamingScheme.PRESERVE_ORIGINAL -> {
                sample.fileName
            }
        }

        // Calculate folder hierarchy based on structureType
        return when (config.structureType) {
            FolderStructureType.CLASS_DIRECTORIES -> {
                "images/$cleanClassName/$resolvedFileName"
            }
            FolderStructureType.TRAIN_VAL_TEST_SPLIT -> {
                val splitFolder = when {
                    sample.split.equals("VAL", true) -> "val"
                    sample.split.equals("TEST", true) -> "test"
                    else -> "train"
                }
                "$splitFolder/$cleanClassName/$resolvedFileName"
            }
            FolderStructureType.FLAT_WITH_METADATA -> {
                "images/$resolvedFileName"
            }
            FolderStructureType.PIPELINE_STATUS -> {
                val statusFolder = when {
                    sample.isSegmented -> "segmented"
                    sample.isAugmented -> "augmented"
                    else -> "raw"
                }
                "$statusFolder/$cleanClassName/$resolvedFileName"
            }
        }
    }

    /**
     * Python starter code snippet tailored to the chosen structure and metadata format.
     */
    fun generatePythonLoaderCode(
        config: DatasetFolderConfig,
        classes: List<DatasetClassEntity>
    ): String {
        return when (config.structureType) {
            FolderStructureType.CLASS_DIRECTORIES -> """
# ==========================================================
# 1. PyTorch ImageFolder DataLoader
# ==========================================================
import torch
from torchvision import datasets, transforms
from torch.utils.data import DataLoader

data_transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

dataset = datasets.ImageFolder(
    root='images/',
    transform=data_transform
)
train_loader = DataLoader(dataset, batch_size=32, shuffle=True, num_workers=2)
print(f"Total sampel: {len(dataset)}, Kelas: {dataset.classes}")

# ==========================================================
# 2. TensorFlow / Keras image_dataset_from_directory
# ==========================================================
import tensorflow as tf

train_ds = tf.keras.utils.image_dataset_from_directory(
    'images/',
    image_size=(224, 224),
    batch_size=32,
    shuffle=True
)
print("Daftar kelas Keras:", train_ds.class_names)
            """.trimIndent()

            FolderStructureType.TRAIN_VAL_TEST_SPLIT -> """
# ==========================================================
# PyTorch Train / Val / Test Loaders
# ==========================================================
from torchvision import datasets, transforms
from torch.utils.data import DataLoader

transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
])

train_set = datasets.ImageFolder('train/', transform=transform)
val_set = datasets.ImageFolder('val/', transform=transform)
test_set = datasets.ImageFolder('test/', transform=transform)

train_loader = DataLoader(train_set, batch_size=32, shuffle=True)
val_loader = DataLoader(val_set, batch_size=32, shuffle=False)
test_loader = DataLoader(test_set, batch_size=32, shuffle=False)

print(f"Train: {len(train_set)} | Val: {len(val_set)} | Test: {len(test_set)}")
            """.trimIndent()

            FolderStructureType.FLAT_WITH_METADATA -> """
# ==========================================================
# Custom PyTorch Dataset menggunakan dataset_index.csv
# ==========================================================
import os
import pandas as pd
from PIL import Image
from torch.utils.data import Dataset, DataLoader
import torchvision.transforms as transforms

class CustomCsvDataset(Dataset):
    def __init__(self, csv_file, transform=None):
        self.df = pd.read_csv(csv_file)
        self.transform = transform
        self.classes = sorted(self.df['label'].unique())
        self.class_to_idx = {cls_name: i for i, cls_name in enumerate(self.classes)}

    def __len__(self):
        return len(self.df)

    def __getitem__(self, idx):
        row = self.df.iloc[idx]
        img_path = row['relative_path']
        image = Image.open(img_path).convert('RGB')
        label = self.class_to_idx[row['label']]

        if self.transform:
            image = self.transform(image)
        return image, label

dataset = CustomCsvDataset(
    csv_file='dataset_index.csv',
    transform=transforms.Compose([transforms.Resize((224, 224)), transforms.ToTensor()])
)
loader = DataLoader(dataset, batch_size=32, shuffle=True)
print(f"Loaded {len(dataset)} images from CSV.")
            """.trimIndent()

            FolderStructureType.PIPELINE_STATUS -> """
# ==========================================================
# Inspeksi Variasi Augmentasi & Data Asli
# ==========================================================
import pandas as pd
df = pd.read_csv('dataset_index.csv')

print("Distribusi Status:")
print(df['is_augmented'].value_counts())
print("\nTipe Augmentasi:")
print(df['augmentation_type'].value_counts())

# Filter hanya data asli untuk baseline:
df_raw = df[df['is_augmented'] == False]
print(f"Data Asli (Raw): {len(df_raw)} sampel")
            """.trimIndent()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Exports a single metadata file (JSON, CSV, or YAML) to app cache and returns the File.
     */
    fun writeSingleMetadataFile(
        context: Context,
        content: String,
        filename: String
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, filename)
        file.writeText(content)
        return file
    }

    /**
     * Creates a complete dataset ZIP bundle adhering to the user's configured folder structure and metadata preferences.
     */
    fun createDatasetZip(
        context: Context,
        samples: List<DatasetSampleEntity>,
        classes: List<DatasetClassEntity>,
        config: DatasetFolderConfig = DatasetFolderConfig()
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val safeName = config.rootFolderName.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
        val zipFile = File(exportDir, "${safeName}_bundle.zip")
        if (zipFile.exists()) zipFile.delete()

        // Filter samples if specific class IDs selected
        val filteredSamples = if (config.selectedClassIds.isNotEmpty()) {
            samples.filter { config.selectedClassIds.contains(it.classId) }
        } else {
            samples
        }.filter { sample ->
            if (!config.includeAugmented && sample.isAugmented) false
            else if (!config.includeSegmented && sample.isSegmented) false
            else true
        }

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Add annotations JSON (Standard)
            val jsonStandard = generateJson(filteredSamples, classes, config.rootFolderName, config)
            val jsonEntry = ZipEntry("dataset_annotations.json")
            zos.putNextEntry(jsonEntry)
            zos.write(jsonStandard.toByteArray())
            zos.closeEntry()

            // 2. Add COCO format JSON
            val cocoJson = generateCocoJson(filteredSamples, classes, config.rootFolderName, config)
            val cocoEntry = ZipEntry("coco_annotations.json")
            zos.putNextEntry(cocoEntry)
            zos.write(cocoJson.toByteArray())
            zos.closeEntry()

            // 3. Add CSV Matrix
            val csvContent = generateCsv(filteredSamples, config)
            val csvEntry = ZipEntry("dataset_index.csv")
            zos.putNextEntry(csvEntry)
            zos.write(csvContent.toByteArray())
            zos.closeEntry()

            // 4. Add Fast.ai CSV
            val fastAiCsv = generateFastAiCsv(filteredSamples, config)
            val fastAiEntry = ZipEntry("labels_fastai.csv")
            zos.putNextEntry(fastAiEntry)
            zos.write(fastAiCsv.toByteArray())
            zos.closeEntry()

            // 5. Add YOLO data.yaml
            val yoloYaml = generateYoloYaml(classes, config.rootFolderName, config)
            val yamlEntry = ZipEntry("data.yaml")
            zos.putNextEntry(yamlEntry)
            zos.write(yoloYaml.toByteArray())
            zos.closeEntry()

            // 6. Add README.md with ML pipeline guidelines & Python starter script
            val readmeContent = """
# ${config.rootFolderName.uppercase()} - Machine Learning Dataset Bundle
Generated by AI Studio Dataset Studio on ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}

## Metadata Ringkasan
- Total Sampel: ${filteredSamples.size}
- Total Kelas: ${classes.size} (${classes.joinToString(", ") { it.name }})
- Struktur Folder: ${config.structureType.name} (${config.structureType.title})
- Konvensi Nama: ${config.namingScheme.name}

## Berkas Anotasi & Metadata yang Disertakan:
1. `dataset_annotations.json`: Format JSON lengkap dengan informasi resolusi, rasio aspek, dan split train/val/test.
2. `coco_annotations.json`: Format Microsoft COCO resmi untuk kompatibilitas framework vision modern.
3. `dataset_index.csv`: Matriks CSV untuk Pandas / PyTorch custom Dataset loader.
4. `labels_fastai.csv`: Format name, label, is_valid untuk Fast.ai.
5. `data.yaml`: Konfigurasi Ultralytics YOLOv8/v9.

## Cuplikan Kode Python:
```python
${generatePythonLoaderCode(config, classes)}
```
            """.trimIndent()
            val readmeEntry = ZipEntry("README.md")
            zos.putNextEntry(readmeEntry)
            zos.write(readmeContent.toByteArray())
            zos.closeEntry()

            // 7. Add image files placed into the configured folder paths
            val classCounters = mutableMapOf<String, Int>()
            for (sample in filteredSamples) {
                val file = File(sample.filePath)
                if (file.exists()) {
                    val idx = (classCounters[sample.className] ?: 0) + 1
                    classCounters[sample.className] = idx

                    val relativeZipPath = resolveExportPath(sample, config, idx)
                    val entry = ZipEntry(relativeZipPath)
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }

        return zipFile
    }

    /**
     * Utility to share file via Android Intent FileProvider.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
