package com.example.data.model

/**
 * Supported dataset directory organization schemes for machine learning pipelines.
 */
enum class FolderStructureType(
    val title: String,
    val shortLabel: String,
    val description: String,
    val treeRepresentation: String,
    val frameworkTarget: String,
    val iconTag: String
) {
    CLASS_DIRECTORIES(
        title = "Struktur Hierarki Kelas",
        shortLabel = "ImageFolder (Kelas)",
        description = "Setiap kelas memiliki subdirektori tersendiri di dalam folder 'images/'.",
        treeRepresentation = """
dataset/
├── dataset_annotations.json
├── dataset_index.csv
└── images/
    ├── kucing/
    │   ├── kucing_0001.jpg
    │   └── kucing_0002.jpg
    └── anjing/
        ├── anjing_0001.jpg
        └── anjing_0002.jpg
        """.trimIndent(),
        frameworkTarget = "PyTorch torchvision.datasets.ImageFolder, tf.keras.utils.image_dataset_from_directory",
        iconTag = "📁"
    ),

    TRAIN_VAL_TEST_SPLIT(
        title = "Struktur Split (Train / Val / Test)",
        shortLabel = "Train / Val / Test",
        description = "Membagi berkas ke direktori train/, val/, dan test/ sesuai persentase yang Anda atur.",
        treeRepresentation = """
dataset/
├── dataset_annotations.json
├── dataset_index.csv
├── train/
│   ├── kucing/ (70%)
│   └── anjing/ (70%)
├── val/
│   ├── kucing/ (20%)
│   └── anjing/ (20%)
└── test/
    ├── kucing/ (10%)
    └── anjing/ (10%)
        """.trimIndent(),
        frameworkTarget = "Evaluasi Model Mandiri, Sklearn train_test_split, Keras DirectoryIterator",
        iconTag = "🔀"
    ),

    FLAT_WITH_METADATA(
        title = "Direktori Datar + File Metadata",
        shortLabel = "Flat + Metadata",
        description = "Semua gambar diletakkan di direktori tunggal images/, dipetakan melalui JSON/CSV metadata.",
        treeRepresentation = """
dataset/
├── dataset_annotations.json (atau COCO)
├── dataset_index.csv
└── images/
    ├── sample_0001.jpg
    ├── sample_0002.jpg
    └── sample_0003.jpg
        """.trimIndent(),
        frameworkTarget = "HuggingFace Datasets, YOLOv8, COCO API, Pandas DataFrame / PyTorch custom Dataset",
        iconTag = "📄"
    ),

    PIPELINE_STATUS(
        title = "Berdasarkan Status Pemrosesan",
        shortLabel = "Raw / Augmented / Mask",
        description = "Memisahkan sampel asli (raw), augmentasi variasi (augmented), dan segmentasi mask (segmented).",
        treeRepresentation = """
dataset/
├── dataset_annotations.json
├── raw/
│   └── kucing/
├── augmented/
│   └── kucing/ (rotasi, flip, lighting)
└── segmented/
    └── kucing/ (background transparent)
        """.trimIndent(),
        frameworkTarget = "Dataset Debugging, Augmentation Ablation Studies, Segmentation Training",
        iconTag = "⚡"
    )
}

/**
 * File renaming conventions for exported image assets.
 */
enum class FileNamingScheme(
    val title: String,
    val example: String,
    val description: String
) {
    CLASS_INDEX(
        title = "Nama Kelas + Nomor Urut",
        example = "kucing_0001.jpg",
        description = "Sangat rapi dan mudah dibaca manusia saat inspeksi dataset."
    ),
    SPLIT_CLASS_INDEX(
        title = "Prefix Split + Kelas + Nomor",
        example = "train_kucing_0001.jpg",
        description = "Mencegah duplikasi nama antar split dalam direktori gabungan."
    ),
    CLEAN_HASH(
        title = "ID Unik + Hash Ringkas",
        example = "sample_42_a8f9.jpg",
        description = "Mencegah tabrakan nama dan cocok untuk hashing ID dataset."
    ),
    PRESERVE_ORIGINAL(
        title = "Pertahankan Nama Asli",
        example = "img_20260905_120344.jpg",
        description = "Menjaga metadata penamaan kamera atau file import asli."
    )
}

/**
 * Supported metadata export formats for training scripts.
 */
enum class MetadataExportFormat(
    val title: String,
    val shortName: String,
    val extension: String,
    val mimeType: String,
    val defaultFilename: String,
    val description: String
) {
    JSON_STANDARD(
        title = "JSON Anotasi Standar",
        shortName = "JSON Standar",
        extension = "json",
        mimeType = "application/json",
        defaultFilename = "dataset_annotations.json",
        description = "Struktur JSON lengkap: detail resolusi, rasio aspek, kategori, split, dan path berkas."
    ),
    JSON_COCO(
        title = "JSON Standar Format COCO",
        shortName = "COCO JSON",
        extension = "json",
        mimeType = "application/json",
        defaultFilename = "coco_annotations.json",
        description = "Format Microsoft COCO baku: images, annotations, categories untuk computer vision modern."
    ),
    CSV_PANDAS_PYTORCH(
        title = "CSV Indeks Matriks (PyTorch/Pandas)",
        shortName = "CSV Matriks",
        extension = "csv",
        mimeType = "text/csv",
        defaultFilename = "dataset_index.csv",
        description = "Tabel terstruktur rapi untuk 'pd.read_csv()' atau PyTorch custom Dataset loader."
    ),
    CSV_FASTAI(
        title = "CSV Minimalis (Fast.ai / Keras)",
        shortName = "CSV Fast.ai",
        extension = "csv",
        mimeType = "text/csv",
        defaultFilename = "labels_fastai.csv",
        description = "Format minimal kolom 'name,label,is_valid' untuk databunch cepat."
    ),
    YAML_YOLO(
        title = "YOLO Data Configuration (data.yaml)",
        shortName = "YOLO YAML",
        extension = "yaml",
        mimeType = "text/yaml",
        defaultFilename = "data.yaml",
        description = "Konfigurasi path training, validasi, dan daftar kelas untuk Ultralytics YOLOv8/v9."
    )
}

/**
 * Complete folder & export configuration state.
 */
data class DatasetFolderConfig(
    val structureType: FolderStructureType = FolderStructureType.CLASS_DIRECTORIES,
    val namingScheme: FileNamingScheme = FileNamingScheme.CLASS_INDEX,
    val metadataFormat: MetadataExportFormat = MetadataExportFormat.JSON_STANDARD,
    val rootFolderName: String = "vision_dataset",
    val trainSplitPercent: Int = 70,
    val valSplitPercent: Int = 20,
    val testSplitPercent: Int = 10,
    val includeAugmented: Boolean = true,
    val includeSegmented: Boolean = true,
    val selectedClassIds: Set<Long> = emptySet()
)
