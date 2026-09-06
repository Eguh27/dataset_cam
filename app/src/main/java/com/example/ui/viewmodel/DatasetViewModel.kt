package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.DatasetClassEntity
import com.example.data.db.DatasetProjectEntity
import com.example.data.db.DatasetSampleEntity
import com.example.data.model.AspectRatioPreset
import com.example.data.model.AugmentationConfig
import com.example.data.model.AugmentationParams
import com.example.data.model.DatasetFolderConfig
import com.example.data.model.FileNamingScheme
import com.example.data.model.FolderStructureType
import com.example.data.model.MetadataExportFormat
import com.example.data.model.MlArchitecturePreset
import com.example.data.model.ResolutionDimension
import com.example.data.model.ResolutionPresets
import com.example.data.model.ResolutionQuality
import com.example.data.model.SegmentationConfig
import com.example.data.repository.DatasetRepository
import com.example.export.DatasetExporter
import com.example.processing.BlurAnalysisResult
import com.example.processing.BlurDetector
import com.example.processing.ImageProcessingUtility
import com.example.processing.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatasetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = DatasetRepository(application, database.datasetDao())

    // All available projects
    val allProjects: StateFlow<List<DatasetProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project state (null indicates user is at initial Project Hub screen)
    private val _activeProject = MutableStateFlow<DatasetProjectEntity?>(null)
    val activeProject: StateFlow<DatasetProjectEntity?> = _activeProject.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val allClasses: StateFlow<List<DatasetClassEntity>> = _activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList())
            else repository.getClassesForProject(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSamples: StateFlow<List<DatasetSampleEntity>> = _activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList())
            else repository.getSamplesForProject(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Camera Presets
    private val _aspectRatio = MutableStateFlow(AspectRatioPreset.SQUARE_1_1)
    val aspectRatio: StateFlow<AspectRatioPreset> = _aspectRatio.asStateFlow()

    private val _resolutionQuality = MutableStateFlow(ResolutionQuality.LITE)
    val resolutionQuality: StateFlow<ResolutionQuality> = _resolutionQuality.asStateFlow()

    // Configs
    private val _segmentationConfig = MutableStateFlow(SegmentationConfig())
    val segmentationConfig: StateFlow<SegmentationConfig> = _segmentationConfig.asStateFlow()

    private val _augmentationConfig = MutableStateFlow(AugmentationConfig())
    val augmentationConfig: StateFlow<AugmentationConfig> = _augmentationConfig.asStateFlow()

    // Active Category Selection
    private val _selectedClass = MutableStateFlow<DatasetClassEntity?>(null)
    val selectedClass: StateFlow<DatasetClassEntity?> = _selectedClass.asStateFlow()

    // Auto-categorization sequence mode (e.g. switch to next class after N photos)
    private val _autoCategorization = MutableStateFlow(false)
    val autoCategorization: StateFlow<Boolean> = _autoCategorization.asStateFlow()

    private val _shotsPerClassInAutoMode = MutableStateFlow(5)
    val shotsPerClassInAutoMode: StateFlow<Int> = _shotsPerClassInAutoMode.asStateFlow()

    private val _currentClassShotCount = MutableStateFlow(0)
    val currentClassShotCount: StateFlow<Int> = _currentClassShotCount.asStateFlow()

    // Processing & Status
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _hudNotification = MutableStateFlow<String?>(null)
    val hudNotification: StateFlow<String?> = _hudNotification.asStateFlow()

    private val _lastCapturedSample = MutableStateFlow<DatasetSampleEntity?>(null)
    val lastCapturedSample: StateFlow<DatasetSampleEntity?> = _lastCapturedSample.asStateFlow()

    // Smart ML Model Preset Recommendations
    private val _selectedMlPreset = MutableStateFlow<MlArchitecturePreset?>(MlArchitecturePreset.VISION_TRANSFORMER)
    val selectedMlPreset: StateFlow<MlArchitecturePreset?> = _selectedMlPreset.asStateFlow()

    // Compression Quality (JPEG 50 - 100)
    private val _compressionQuality = MutableStateFlow(88)
    val compressionQuality: StateFlow<Int> = _compressionQuality.asStateFlow()

    // Dataset Folder Configuration & Metadata Export State
    private val _folderConfig = MutableStateFlow(DatasetFolderConfig())
    val folderConfig: StateFlow<DatasetFolderConfig> = _folderConfig.asStateFlow()

    fun setFolderStructure(type: FolderStructureType) {
        _folderConfig.value = _folderConfig.value.copy(structureType = type)
        _hudNotification.value = "Struktur folder: ${type.shortLabel}"
    }

    fun setFileNamingScheme(scheme: FileNamingScheme) {
        _folderConfig.value = _folderConfig.value.copy(namingScheme = scheme)
        _hudNotification.value = "Format nama: ${scheme.example}"
    }

    fun setMetadataFormat(format: MetadataExportFormat) {
        _folderConfig.value = _folderConfig.value.copy(metadataFormat = format)
        _hudNotification.value = "Format metadata: ${format.shortName}"
    }

    fun setRootFolderName(name: String) {
        val sanitized = name.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifEmpty { "vision_dataset" }
        _folderConfig.value = _folderConfig.value.copy(rootFolderName = sanitized)
    }

    fun setSplitPercentages(train: Int, valPercent: Int, test: Int) {
        val safeTrain = train.coerceIn(0, 100)
        val safeVal = valPercent.coerceIn(0, 100 - safeTrain)
        val safeTest = (100 - safeTrain - safeVal).coerceAtLeast(0)
        _folderConfig.value = _folderConfig.value.copy(
            trainSplitPercent = safeTrain,
            valSplitPercent = safeVal,
            testSplitPercent = safeTest
        )
    }

    fun setIncludeAugmentedInExport(include: Boolean) {
        _folderConfig.value = _folderConfig.value.copy(includeAugmented = include)
    }

    fun setIncludeSegmentedInExport(include: Boolean) {
        _folderConfig.value = _folderConfig.value.copy(includeSegmented = include)
    }

    fun toggleClassFilterInExport(classId: Long) {
        val current = _folderConfig.value.selectedClassIds.toMutableSet()
        if (current.contains(classId)) {
            current.remove(classId)
        } else {
            current.add(classId)
        }
        _folderConfig.value = _folderConfig.value.copy(selectedClassIds = current)
    }

    fun selectAllClassesInExport() {
        _folderConfig.value = _folderConfig.value.copy(selectedClassIds = emptySet())
    }

    /**
     * Stratified split assignment across classes into TRAIN, VAL, TEST.
     */
    fun applyStratifiedSplits(
        trainPercent: Int = _folderConfig.value.trainSplitPercent,
        valPercent: Int = _folderConfig.value.valSplitPercent,
        testPercent: Int = _folderConfig.value.testSplitPercent
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            withContext(Dispatchers.IO) {
                val samples = allSamples.value
                val classes = allClasses.value
                val updatedSamples = mutableListOf<DatasetSampleEntity>()

                for (cls in classes) {
                    val classSamples = samples.filter { it.classId == cls.id }.shuffled()
                    val total = classSamples.size
                    if (total == 0) continue

                    val trainCount = ((total * trainPercent) / 100).coerceAtLeast(if (total > 0) 1 else 0)
                    val valCount = ((total * valPercent) / 100)

                    classSamples.forEachIndexed { index, sample ->
                        val targetSplit = when {
                            index < trainCount -> "TRAIN"
                            index < trainCount + valCount -> "VAL"
                            else -> "TEST"
                        }
                        if (sample.split != targetSplit) {
                            updatedSamples.add(sample.copy(split = targetSplit))
                        }
                    }
                }

                if (updatedSamples.isNotEmpty()) {
                    repository.updateSamples(updatedSamples)
                }
            }
            _isProcessing.value = false
            _hudNotification.value = "Split $trainPercent% Train, $valPercent% Val, $testPercent% Test berhasil diterapkan!"
        }
    }

    // Real-Time Blur Detection Analysis State
    private val _blurAnalysisResult = MutableStateFlow(BlurAnalysisResult())
    val blurAnalysisResult: StateFlow<BlurAnalysisResult> = _blurAnalysisResult.asStateFlow()

    private val _blurDetectionEnabled = MutableStateFlow(true)
    val blurDetectionEnabled: StateFlow<Boolean> = _blurDetectionEnabled.asStateFlow()

    private val _blurSensitivityThreshold = MutableStateFlow(BlurDetector.DEFAULT_BLUR_THRESHOLD)
    val blurSensitivityThreshold: StateFlow<Double> = _blurSensitivityThreshold.asStateFlow()

    private val _preventBlurryCapture = MutableStateFlow(false)
    val preventBlurryCapture: StateFlow<Boolean> = _preventBlurryCapture.asStateFlow()

    fun updateBlurResult(result: BlurAnalysisResult) {
        _blurAnalysisResult.value = result
    }

    fun setBlurDetectionEnabled(enabled: Boolean) {
        _blurDetectionEnabled.value = enabled
        _hudNotification.value = if (enabled) "Deteksi blur real-time aktif" else "Deteksi blur dinonaktifkan"
    }

    fun toggleBlurDetection(enabled: Boolean) = setBlurDetectionEnabled(enabled)

    fun setBlurSensitivityThreshold(threshold: Double) {
        _blurSensitivityThreshold.value = threshold
    }

    fun setPreventBlurryCapture(prevent: Boolean) {
        _preventBlurryCapture.value = prevent
        _hudNotification.value = if (prevent) "Pencegahan foto buram diaktifkan" else "Pencegahan foto buram dimatikan"
    }

    fun togglePreventBlurryCapture(prevent: Boolean) = setPreventBlurryCapture(prevent)

    // Custom Dimension Override (if user adjusted slider or chose specific option)
    private val _customDimension = MutableStateFlow<ResolutionDimension?>(null)
    val customDimension: StateFlow<ResolutionDimension?> = _customDimension.asStateFlow()

    fun applyMlPreset(preset: MlArchitecturePreset) {
        _selectedMlPreset.value = preset
        _aspectRatio.value = preset.recommendedAspect
        _resolutionQuality.value = preset.recommendedQuality
        _customDimension.value = null
        _segmentationConfig.value = _segmentationConfig.value.copy(
            autoSegmentBackground = preset.autoSegmentation,
            thresholdSensitivity = preset.autoSegmentationSensitivity
        )
        _augmentationConfig.value = _augmentationConfig.value.copy(
            enableAugmentation = preset.multiplier > 1,
            multiplier = preset.multiplier,
            rotateAngles = preset.rotate,
            horizontalFlip = preset.flip,
            lightingVariations = preset.lighting,
            translationShift = preset.shift
        )
        _hudNotification.value = "Preset ${preset.title} diterapkan!"
    }

    init {
        viewModelScope.launch {
            allClasses.collect { classes ->
                if (_selectedClass.value == null && classes.isNotEmpty()) {
                    _selectedClass.value = classes.first()
                }
            }
        }
    }

    val currentDimension: ResolutionDimension
        get() = _customDimension.value ?: ResolutionPresets.getDimension(_aspectRatio.value, _resolutionQuality.value)

    fun setAspectRatio(preset: AspectRatioPreset) {
        _aspectRatio.value = preset
        // If current custom dimension exists, recalculate it with the new aspect ratio
        val current = _customDimension.value
        if (current != null) {
            _customDimension.value = ResolutionPresets.createCustomDimension(preset, current.width, true)
        }
        _hudNotification.value = "Rasio ${preset.displayName} (${currentDimension.displayString} • ~${currentDimension.estimatedKb} KB)"
    }

    fun setResolutionQuality(quality: ResolutionQuality) {
        _resolutionQuality.value = quality
        _customDimension.value = null
        _hudNotification.value = "Resolusi: ${currentDimension.displayString} (~${currentDimension.estimatedKb} KB)"
    }

    fun setResolutionOption(option: com.example.data.model.ResolutionOption) {
        _aspectRatio.value = option.aspectPreset
        _resolutionQuality.value = option.quality
        _customDimension.value = ResolutionDimension(
            width = option.width,
            height = option.height,
            aspectPreset = option.aspectPreset,
            quality = option.quality,
            customLabel = option.label
        )
        _hudNotification.value = "${option.label} • ~${option.estimatedKb} KB"
    }

    fun setCustomResolutionSize(baseSize: Int, isWidthBase: Boolean = true) {
        val newDim = ResolutionPresets.createCustomDimension(_aspectRatio.value, baseSize, isWidthBase)
        _customDimension.value = newDim
        _resolutionQuality.value = ResolutionQuality.CUSTOM
    }

    fun setCompressionQuality(quality: Int) {
        _compressionQuality.value = quality.coerceIn(40, 100)
    }

    fun toggleSegmentation(enabled: Boolean) {
        _segmentationConfig.value = _segmentationConfig.value.copy(autoSegmentBackground = enabled)
    }

    fun updateSegmentationThreshold(threshold: Float) {
        _segmentationConfig.value = _segmentationConfig.value.copy(thresholdSensitivity = threshold)
    }

    fun toggleAugmentation(enabled: Boolean) {
        _augmentationConfig.value = _augmentationConfig.value.copy(enableAugmentation = enabled)
    }

    fun setAugmentationMultiplier(multiplier: Int) {
        _augmentationConfig.value = _augmentationConfig.value.copy(multiplier = multiplier)
    }

    fun toggleAugmentationOption(
        rotate: Boolean? = null,
        flip: Boolean? = null,
        verticalFlip: Boolean? = null,
        lighting: Boolean? = null,
        shift: Boolean? = null
    ) {
        val cur = _augmentationConfig.value
        _augmentationConfig.value = cur.copy(
            rotateAngles = rotate ?: cur.rotateAngles,
            horizontalFlip = flip ?: cur.horizontalFlip,
            verticalFlip = verticalFlip ?: cur.verticalFlip,
            lightingVariations = lighting ?: cur.lightingVariations,
            translationShift = shift ?: cur.translationShift
        )
    }

    fun selectClass(cls: DatasetClassEntity) {
        _selectedClass.value = cls
        _currentClassShotCount.value = 0
    }

    // ==========================================
    // PROJECT MANAGEMENT (Hub, Create, Switch, Delete)
    // ==========================================

    fun selectProject(project: DatasetProjectEntity) {
        _activeProject.value = project
        _selectedClass.value = null
        val matchedAspect = AspectRatioPreset.entries.find { it.displayName == project.targetAspectRatio }
        if (matchedAspect != null) {
            _aspectRatio.value = matchedAspect
        }
        val sanitizedName = project.name.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifEmpty { "dataset" }
        _folderConfig.value = _folderConfig.value.copy(rootFolderName = sanitizedName)
        _hudNotification.value = "Proyek aktif: ${project.name}"
    }

    fun switchProject() {
        _activeProject.value = null
        _selectedClass.value = null
    }

    fun createNewProject(
        name: String,
        description: String = "",
        aspectRatio: AspectRatioPreset = AspectRatioPreset.SQUARE_1_1,
        resolution: String = "224x224",
        classNames: List<String> = emptyList(),
        onCreated: (DatasetProjectEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val palette = listOf("#38BDF8", "#A855F7", "#10B981", "#F43F5E", "#F59E0B", "#EC4899", "#8B5CF6")
            val initialClasses = if (classNames.isNotEmpty()) {
                classNames.filter { it.isNotBlank() }.mapIndexed { idx, cName ->
                    cName.trim() to palette[idx % palette.size]
                }
            } else {
                listOf(
                    "class_a" to "#38BDF8",
                    "class_b" to "#A855F7",
                    "normal" to "#10B981",
                    "anomaly" to "#F43F5E"
                )
            }
            val newProject = repository.createProject(
                name = name,
                description = description,
                targetAspectRatio = aspectRatio.displayName,
                defaultResolution = resolution,
                initialClasses = initialClasses
            )
            selectProject(newProject)
            withContext(Dispatchers.Main) {
                onCreated(newProject)
            }
        }
    }

    fun deleteProject(project: DatasetProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
                _selectedClass.value = null
            }
            _hudNotification.value = "Proyek '${project.name}' berhasil dihapus"
        }
    }

    fun toggleAutoCategorization(enabled: Boolean) {
        _autoCategorization.value = enabled
        _currentClassShotCount.value = 0
    }

    fun addNewClass(name: String, colorHex: String, description: String = "") {
        val projId = _activeProject.value?.id ?: 1L
        viewModelScope.launch {
            val newId = repository.addClassToProject(projId, name, colorHex, description)
            _hudNotification.value = "Kategori folder '$name' berhasil dibuat"
        }
    }

    fun deleteClass(cls: DatasetClassEntity) {
        viewModelScope.launch {
            repository.deleteClass(cls)
            if (_selectedClass.value?.id == cls.id) {
                _selectedClass.value = allClasses.value.firstOrNull { it.id != cls.id }
            }
            _hudNotification.value = "Kategori '${cls.name}' dan foldernya dihapus"
        }
    }

    fun deleteSample(sample: DatasetSampleEntity) {
        viewModelScope.launch {
            repository.deleteSampleWithAugmentations(sample)
            _hudNotification.value = "Sample dihapus"
        }
    }

    fun deleteSamplesBatch(samples: List<DatasetSampleEntity>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isProcessing.value = true
            val count = samples.size
            repository.deleteSamplesBatch(samples)
            _hudNotification.value = "$count gambar berhasil dihapus"
            _isProcessing.value = false
            onComplete()
        }
    }

    fun clearNotification() {
        _hudNotification.value = null
    }

    /**
     * Primary image capture handler:
     * 1. Standardizes crop & resolution to exact aspect ratio and dimensions.
     * 2. Runs background auto-segmentation if active.
     * 3. Saves base sample into category folder.
     * 4. Multiplies dataset with augmentations (rotations, flips, shifts, lighting).
     * 5. Persists metadata to Room.
     * 6. Advances auto-category counter if auto mode is enabled.
     */
    fun processAndSaveCapturedImage(sourceBitmap: Bitmap, rotationDegrees: Int = 0) {
        val targetClass = _selectedClass.value ?: return
        val targetDimension = currentDimension
        val segConfig = _segmentationConfig.value
        val augConfig = _augmentationConfig.value

        viewModelScope.launch {
            _isProcessing.value = true
            withContext(Dispatchers.IO) {
                try {
                    // 1. Standardize aspect ratio and resolution
                    val standardized = ImageProcessor.standardizeBitmap(
                        source = sourceBitmap,
                        targetDimension = targetDimension,
                        rotationDegrees = rotationDegrees
                    )

                    // 2. Auto Segmentation if enabled
                    val processedBase = if (segConfig.autoSegmentBackground) {
                        ImageProcessor.segmentBackground(standardized, segConfig)
                    } else {
                        standardized
                    }

                    val isPng = segConfig.autoSegmentBackground && segConfig.replaceWithColor == null
                    val ext = if (isPng) "png" else "jpg"
                    val timestamp = System.currentTimeMillis()
                    val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))

                    val classFolder = repository.getDatasetFolder(targetClass.projectId, targetClass.name)
                    val baseFileName = "${targetClass.name}_${timeStr}_orig.$ext"
                    val baseFile = File(classFolder, baseFileName)

                    val qualitySetting = _compressionQuality.value
                    val fileSize = ImageProcessor.saveBitmapToFile(processedBase, baseFile, isPng, qualitySetting)

                    // Insert base sample in Room
                    val baseSample = DatasetSampleEntity(
                        projectId = targetClass.projectId,
                        classId = targetClass.id,
                        className = targetClass.name,
                        filePath = baseFile.absolutePath,
                        fileName = baseFileName,
                        relativePath = "${targetClass.name}/$baseFileName",
                        width = targetDimension.width,
                        height = targetDimension.height,
                        aspectRatio = targetDimension.aspectPreset.displayName,
                        resolutionLabel = targetDimension.displayString,
                        fileSizeBytes = fileSize,
                        format = ext.uppercase(),
                        isSegmented = segConfig.autoSegmentBackground,
                        isAugmented = false,
                        augmentationType = "ORIGINAL",
                        parentSampleId = null,
                        split = "TRAIN",
                        createdAt = timestamp
                    )
                    val parentId = repository.insertSample(baseSample)
                    val savedBase = baseSample.copy(id = parentId)

                    val newSamples = mutableListOf<DatasetSampleEntity>()
                    newSamples.add(savedBase)

                    // 3. Multi-variation augmentations if enabled
                    if (augConfig.enableAugmentation && augConfig.multiplier > 0) {
                        val variations = ImageProcessor.generateAugmentations(processedBase, augConfig)
                        for (variant in variations) {
                            val augFileName = "${targetClass.name}_${timeStr}_${variant.filenameSuffix}.$ext"
                            val augFile = File(classFolder, augFileName)
                            val augSize = ImageProcessor.saveBitmapToFile(variant.bitmap, augFile, isPng, qualitySetting)

                            val augSample = DatasetSampleEntity(
                                projectId = targetClass.projectId,
                                classId = targetClass.id,
                                className = targetClass.name,
                                filePath = augFile.absolutePath,
                                fileName = augFileName,
                                relativePath = "${targetClass.name}/$augFileName",
                                width = targetDimension.width,
                                height = targetDimension.height,
                                aspectRatio = targetDimension.aspectPreset.displayName,
                                resolutionLabel = targetDimension.displayString,
                                fileSizeBytes = augSize,
                                format = ext.uppercase(),
                                isSegmented = segConfig.autoSegmentBackground,
                                isAugmented = true,
                                augmentationType = variant.type.name,
                                parentSampleId = parentId,
                                split = "TRAIN",
                                createdAt = timestamp + 1
                            )
                            newSamples.add(augSample)
                        }
                        if (newSamples.size > 1) {
                            repository.insertSamples(newSamples.drop(1))
                        }
                    }

                    _lastCapturedSample.value = savedBase

                    withContext(Dispatchers.Main) {
                        val totalCreated = newSamples.size
                        val augCount = totalCreated - 1
                        val sizeKb = fileSize / 1024
                        _hudNotification.value = if (augCount > 0) {
                            "Tersimpan: 1 asli + $augCount variasi (~${sizeKb} KB/file, ${targetDimension.displayString}) di '${targetClass.name}'!"
                        } else {
                            "Tersimpan: 1 sample (${targetDimension.displayString} • ~${sizeKb} KB) di '${targetClass.name}'!"
                        }

                        // Auto-categorization check
                        if (_autoCategorization.value) {
                            val count = _currentClassShotCount.value + 1
                            if (count >= _shotsPerClassInAutoMode.value) {
                                // Auto advance to next class
                                val classes = allClasses.value
                                val currentIndex = classes.indexOfFirst { it.id == targetClass.id }
                                if (currentIndex != -1 && classes.size > 1) {
                                    val nextIndex = (currentIndex + 1) % classes.size
                                    _selectedClass.value = classes[nextIndex]
                                    _currentClassShotCount.value = 0
                                    _hudNotification.value = "Otomatis beralih ke folder '${classes[nextIndex].name}'"
                                }
                            } else {
                                _currentClassShotCount.value = count
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _hudNotification.value = "Gagal memproses gambar: ${e.localizedMessage}"
                    }
                } finally {
                    _isProcessing.value = false
                }
            }
        }
    }

    // Export helpers
    fun exportSingleMetadataFile(
        format: MetadataExportFormat = _folderConfig.value.metadataFormat,
        onReady: (File) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            withContext(Dispatchers.IO) {
                val samples = allSamples.value
                val classes = allClasses.value
                val config = _folderConfig.value

                val (content, filename) = when (format) {
                    MetadataExportFormat.JSON_STANDARD -> {
                        Pair(DatasetExporter.generateJson(samples, classes, config.rootFolderName, config), format.defaultFilename)
                    }
                    MetadataExportFormat.JSON_COCO -> {
                        Pair(DatasetExporter.generateCocoJson(samples, classes, config.rootFolderName, config), format.defaultFilename)
                    }
                    MetadataExportFormat.CSV_PANDAS_PYTORCH -> {
                        Pair(DatasetExporter.generateCsv(samples, config), format.defaultFilename)
                    }
                    MetadataExportFormat.CSV_FASTAI -> {
                        Pair(DatasetExporter.generateFastAiCsv(samples, config), format.defaultFilename)
                    }
                    MetadataExportFormat.YAML_YOLO -> {
                        Pair(DatasetExporter.generateYoloYaml(classes, config.rootFolderName, config), format.defaultFilename)
                    }
                }

                val file = DatasetExporter.writeSingleMetadataFile(getApplication(), content, filename)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onReady(file)
                }
            }
        }
    }

    fun exportDatasetZipWithConfig(
        config: DatasetFolderConfig = _folderConfig.value,
        onReady: (File) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            withContext(Dispatchers.IO) {
                val samples = allSamples.value
                val classes = allClasses.value
                val zip = DatasetExporter.createDatasetZip(getApplication(), samples, classes, config)
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                    onReady(zip)
                }
            }
        }
    }

    fun exportDatasetJson(onReady: (File) -> Unit) {
        exportSingleMetadataFile(MetadataExportFormat.JSON_STANDARD, onReady)
    }

    fun exportDatasetCsv(onReady: (File) -> Unit) {
        exportSingleMetadataFile(MetadataExportFormat.CSV_PANDAS_PYTORCH, onReady)
    }

    fun exportDatasetZip(onReady: (File) -> Unit) {
        exportDatasetZipWithConfig(_folderConfig.value, onReady)
    }

    /**
     * Seeds realistic ML demo dataset samples across multiple categories and aspect ratios
     * for previewing charts and analytics immediately.
     */
    fun seedDemoSamples() {
        viewModelScope.launch {
            _isProcessing.value = true
            withContext(Dispatchers.IO) {
                try {
                    val currentProjId = _activeProject.value?.id ?: 1L
                    val classA = repository.getOrCreateClassInProject(currentProjId, "class_a", "#38BDF8")
                    val classB = repository.getOrCreateClassInProject(currentProjId, "class_b", "#C084FC")
                    val classC = repository.getOrCreateClassInProject(currentProjId, "class_c", "#F472B6")

                    val presets = listOf(
                        Triple(classA, AspectRatioPreset.SQUARE_1_1, "TRAIN"),
                        Triple(classA, AspectRatioPreset.SQUARE_1_1, "TRAIN"),
                        Triple(classA, AspectRatioPreset.STANDARD_4_3, "VAL"),
                        Triple(classA, AspectRatioPreset.WIDESCREEN_16_9, "TEST"),
                        Triple(classA, AspectRatioPreset.SQUARE_1_1, "TRAIN"),
                        Triple(classB, AspectRatioPreset.SQUARE_1_1, "TRAIN"),
                        Triple(classB, AspectRatioPreset.STANDARD_4_3, "TRAIN"),
                        Triple(classB, AspectRatioPreset.WIDESCREEN_16_9, "VAL"),
                        Triple(classB, AspectRatioPreset.SQUARE_1_1, "TEST"),
                        Triple(classC, AspectRatioPreset.SQUARE_1_1, "TRAIN"),
                        Triple(classC, AspectRatioPreset.STANDARD_4_3, "TRAIN"),
                        Triple(classC, AspectRatioPreset.SQUARE_1_1, "VAL")
                    )

                    val now = System.currentTimeMillis()
                    presets.forEachIndexed { idx, (cls, aspect, split) ->
                        val (w, h) = when (aspect) {
                            AspectRatioPreset.SQUARE_1_1 -> 640 to 640
                            AspectRatioPreset.STANDARD_4_3 -> 640 to 480
                            AspectRatioPreset.WIDESCREEN_16_9 -> 640 to 360
                        }

                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.rgb(34 + idx * 2, 20 + idx, 24 + idx * 3))

                        val paint = android.graphics.Paint().apply { isAntiAlias = true }
                        paint.color = android.graphics.Color.parseColor(cls.colorHex)
                        canvas.drawCircle(w / 2f, h / 2f, minOf(w, h) * 0.28f, paint)

                        paint.color = android.graphics.Color.WHITE
                        canvas.drawCircle(w / 2f, h / 2f, minOf(w, h) * 0.12f, paint)

                        val folder = repository.getDatasetFolder(currentProjId, cls.name)
                        val fileName = "${cls.name}_demo_${now + idx}_${aspect.displayName.replace(':', '_')}.jpg"
                        val file = File(folder, fileName)
                        val sizeBytes = ImageProcessor.saveBitmapToFile(bitmap, file, false, 85)

                        val sample = DatasetSampleEntity(
                            projectId = currentProjId,
                            classId = cls.id,
                            className = cls.name,
                            filePath = file.absolutePath,
                            fileName = fileName,
                            relativePath = "${cls.name}/$fileName",
                            width = w,
                            height = h,
                            aspectRatio = aspect.displayName,
                            resolutionLabel = "${w}x${h}",
                            fileSizeBytes = sizeBytes,
                            format = "JPG",
                            isSegmented = false,
                            isAugmented = false,
                            augmentationType = "ORIGINAL",
                            parentSampleId = null,
                            split = split,
                            createdAt = now + idx * 100
                        )
                        repository.insertSample(sample)
                    }

                    _hudNotification.value = "12 sampel demo ML berhasil ditambahkan ke dataset"
                } catch (e: Exception) {
                    _hudNotification.value = "Gagal membuat sampel: ${e.message}"
                } finally {
                    _isProcessing.value = false
                }
            }
        }
    }

    // =========================================================================
    // IMAGE AUGMENTATION UTILITY METHODS (Rotation, Brightness, Flips, Pipeline)
    // =========================================================================

    private val _batchAugmentationProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val batchAugmentationProgress: StateFlow<Pair<Int, Int>?> = _batchAugmentationProgress.asStateFlow()

    fun loadSampleBitmap(sample: DatasetSampleEntity, maxDimension: Int = 512): Bitmap? {
        val file = File(sample.filePath)
        return ImageProcessingUtility.loadBitmapFromFile(file, maxDimension)
    }

    /**
     * Applies user-configured augmentations (rotation, brightness, horizontal/vertical flip)
     * to a specific sample in the dataset:
     * - [saveAsNew] = true -> creates a new augmented sample file and adds a record to Room DB.
     * - [saveAsNew] = false -> updates the existing sample file in place.
     */
    fun applyAugmentationToSample(
        sample: DatasetSampleEntity,
        params: AugmentationParams,
        saveAsNew: Boolean = true,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val success = withContext(Dispatchers.IO) {
                try {
                    val srcFile = File(sample.filePath)
                    if (!srcFile.exists()) return@withContext false

                    val isPng = sample.format.equals("PNG", ignoreCase = true)
                    val timestamp = System.currentTimeMillis()
                    val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))

                    if (saveAsNew) {
                        val classFolder = repository.getDatasetFolder(sample.projectId, sample.className)
                        val ext = if (isPng) "png" else "jpg"
                        val newFileName = "${sample.className}_${timeStr}_custom_aug.$ext"
                        val newFile = File(classFolder, newFileName)

                        val newSize = ImageProcessingUtility.createAugmentedFile(
                            sourceFile = srcFile,
                            outputFile = newFile,
                            params = params,
                            isPng = isPng,
                            quality = _compressionQuality.value
                        )

                        val newEntity = sample.copy(
                            id = 0L,
                            projectId = sample.projectId,
                            filePath = newFile.absolutePath,
                            fileName = newFileName,
                            relativePath = "${sample.className}/$newFileName",
                            fileSizeBytes = newSize,
                            isAugmented = true,
                            augmentationType = "CUSTOM_AUGMENTATION",
                            parentSampleId = if (sample.isAugmented) sample.parentSampleId else sample.id,
                            createdAt = timestamp
                        )
                        repository.insertSample(newEntity)
                    } else {
                        // Update in-place
                        ImageProcessingUtility.createAugmentedFile(
                            sourceFile = srcFile,
                            outputFile = srcFile,
                            params = params,
                            isPng = isPng,
                            quality = _compressionQuality.value
                        )
                        val updatedEntity = sample.copy(
                            fileSizeBytes = srcFile.length(),
                            isAugmented = true,
                            augmentationType = "CUSTOM_AUGMENTATION"
                        )
                        repository.updateSamples(listOf(updatedEntity))
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            _isProcessing.value = false
            if (success) {
                _hudNotification.value = if (saveAsNew) "Variasi augmentasi baru berhasil disimpan!" else "Berkas sampel berhasil diperbarui!"
            } else {
                _hudNotification.value = "Gagal memproses augmentasi gambar"
            }
            onComplete?.invoke(success)
        }
    }

    /**
     * Batch applies augmentations to all primary samples in a category (or entire dataset if classId is null).
     */
    fun batchApplyAugmentation(
        classId: Long?,
        params: AugmentationParams,
        onComplete: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            var processedCount = 0
            withContext(Dispatchers.IO) {
                try {
                    val targetSamples = if (classId == null) {
                        allSamples.value.filter { !it.isAugmented }
                    } else {
                        allSamples.value.filter { it.classId == classId && !it.isAugmented }
                    }

                    val total = targetSamples.size
                    if (total == 0) return@withContext

                    val newEntities = mutableListOf<DatasetSampleEntity>()
                    val quality = _compressionQuality.value

                    targetSamples.forEachIndexed { index, sample ->
                        _batchAugmentationProgress.value = Pair(index + 1, total)
                        val srcFile = File(sample.filePath)
                        if (srcFile.exists()) {
                            val isPng = sample.format.equals("PNG", ignoreCase = true)
                            val ext = if (isPng) "png" else "jpg"
                            val timestamp = System.currentTimeMillis()
                            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))
                            val classFolder = repository.getDatasetFolder(sample.className)
                            val newFileName = "${sample.className}_${timeStr}_batch_aug_$index.$ext"
                            val newFile = File(classFolder, newFileName)

                            val newSize = ImageProcessingUtility.createAugmentedFile(
                                sourceFile = srcFile,
                                outputFile = newFile,
                                params = params,
                                isPng = isPng,
                                quality = quality
                            )

                            newEntities.add(
                                sample.copy(
                                    id = 0L,
                                    filePath = newFile.absolutePath,
                                    fileName = newFileName,
                                    relativePath = "${sample.className}/$newFileName",
                                    fileSizeBytes = newSize,
                                    isAugmented = true,
                                    augmentationType = "BATCH_AUGMENTATION",
                                    parentSampleId = sample.id,
                                    createdAt = timestamp + index
                                )
                            )
                            processedCount++
                        }
                    }

                    if (newEntities.isNotEmpty()) {
                        repository.insertSamples(newEntities)
                    }
                } catch (e: Exception) {
                    // Handled gracefully
                } finally {
                    _batchAugmentationProgress.value = null
                }
            }
            _isProcessing.value = false
            _hudNotification.value = "Berhasil membuat $processedCount variasi dataset augmentasi!"
            onComplete?.invoke(processedCount)
        }
    }
}
