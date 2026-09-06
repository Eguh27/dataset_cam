package com.example.ui.screens.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DatasetFolderConfig
import com.example.data.model.FileNamingScheme
import com.example.data.model.FolderStructureType
import com.example.data.model.MetadataExportFormat
import com.example.export.DatasetExporter
import com.example.ui.components.GlassBox
import com.example.ui.components.LiquidGlowBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DatasetViewModel

enum class ExportScreenTab(val title: String, val iconTag: String) {
    FOLDER_STRUCTURE("Struktur Folder", "📁"),
    METADATA_FILES("Ekspor Metadata", "📄"),
    ZIP_BUNDLE("Paket Arsip ZIP", "📦"),
    ML_PIPELINE("Pipeline Script", "⚡")
}

@Composable
fun ExportScreen(
    viewModel: DatasetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allClasses by viewModel.allClasses.collectAsState()
    val allSamples by viewModel.allSamples.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val folderConfig by viewModel.folderConfig.collectAsState()

    var activeScreenTab by remember { mutableStateOf(ExportScreenTab.FOLDER_STRUCTURE) }
    var selectedMetadataFormat by remember { mutableStateOf(MetadataExportFormat.JSON_STANDARD) }

    // Live preview generation based on configuration
    val activeMetadataPreview = remember(allSamples, allClasses, folderConfig, selectedMetadataFormat) {
        when (selectedMetadataFormat) {
            MetadataExportFormat.JSON_STANDARD -> DatasetExporter.generateJson(allSamples, allClasses, folderConfig.rootFolderName, folderConfig)
            MetadataExportFormat.JSON_COCO -> DatasetExporter.generateCocoJson(allSamples, allClasses, folderConfig.rootFolderName, folderConfig)
            MetadataExportFormat.CSV_PANDAS_PYTORCH -> DatasetExporter.generateCsv(allSamples, folderConfig)
            MetadataExportFormat.CSV_FASTAI -> DatasetExporter.generateFastAiCsv(allSamples, folderConfig)
            MetadataExportFormat.YAML_YOLO -> DatasetExporter.generateYoloYaml(allClasses, folderConfig.rootFolderName, folderConfig)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .padding(bottom = 90.dp)
    ) {
        // 1. Header with Stats & Fast Export Trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ekspor & Struktur Dataset",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "${allSamples.size} Sampel • ${allClasses.size} Kategori • Siap Pipeline ML",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            // Quick Download ZIP button
            GlassBox(
                shape = RoundedCornerShape(12.dp),
                borderColor = NeonCyan,
                modifier = Modifier
                    .clickable {
                        viewModel.exportDatasetZipWithConfig(folderConfig) { zipFile ->
                            DatasetExporter.shareFile(
                                context,
                                zipFile,
                                "application/zip",
                                "${folderConfig.rootFolderName}_bundle.zip"
                            )
                        }
                    }
                    .testTag("export_zip_header_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Ekspor ZIP",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Unduh ZIP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Navigation Tabs (Glass Horizontal Carousel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportScreenTab.entries.forEach { tab ->
                val isSelected = tab == activeScreenTab
                GlassBox(
                    shape = RoundedCornerShape(14.dp),
                    borderColor = if (isSelected) NeonCyan else GlassBorder,
                    backgroundBrush = if (isSelected) {
                        Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.35f), NeonPurple.copy(alpha = 0.2f)))
                    } else null,
                    backgroundColor = if (!isSelected) GlassBackground else null,
                    modifier = Modifier
                        .clickable { activeScreenTab = tab }
                        .testTag("export_screen_tab_${tab.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = tab.iconTag, fontSize = 13.sp)
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Tab Body
        when (activeScreenTab) {
            ExportScreenTab.FOLDER_STRUCTURE -> {
                FolderStructureSection(
                    viewModel = viewModel,
                    config = folderConfig,
                    samplesCount = allSamples.size,
                    classes = allClasses
                )
            }
            ExportScreenTab.METADATA_FILES -> {
                MetadataFilesSection(
                    viewModel = viewModel,
                    config = folderConfig,
                    selectedFormat = selectedMetadataFormat,
                    onFormatSelected = { selectedMetadataFormat = it },
                    previewContent = activeMetadataPreview,
                    samplesCount = allSamples.size,
                    classesCount = allClasses.size
                )
            }
            ExportScreenTab.ZIP_BUNDLE -> {
                ZipBundleSection(
                    viewModel = viewModel,
                    config = folderConfig,
                    samples = allSamples,
                    classes = allClasses,
                    isProcessing = isProcessing
                )
            }
            ExportScreenTab.ML_PIPELINE -> {
                MlPipelineCodeSection(
                    config = folderConfig,
                    classes = allClasses
                )
            }
        }
    }
}

/**
 * 1. FOLDER STRUCTURE CONFIGURATION SECTION
 */
@Composable
private fun FolderStructureSection(
    viewModel: DatasetViewModel,
    config: DatasetFolderConfig,
    samplesCount: Int,
    classes: List<com.example.data.db.DatasetClassEntity>
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Card: Folder Organization Schemes
        GlassBox(
            shape = RoundedCornerShape(18.dp),
            borderColor = NeonCyan.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Skema Pengorganisasian Folder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Pilih arsitektur direktori yang cocok dengan pipeline pelatihan framework ML Anda.",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FolderStructureType.entries.forEach { scheme ->
                        val isSelected = scheme == config.structureType
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x3300F0FF) else Color(0x18000000))
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else GlassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setFolderStructure(scheme) }
                                .padding(12.dp)
                                .testTag("folder_scheme_${scheme.name}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = scheme.iconTag, fontSize = 18.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = scheme.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) NeonCyan else TextPrimary
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Dipilih",
                                                tint = NeonCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = scheme.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Text(
                                        text = "Target: ${scheme.frameworkTarget}",
                                        fontSize = 9.5.sp,
                                        color = if (isSelected) NeonEmerald else TextMuted,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card: Live Folder Tree Visualizer
        GlassBox(
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visualisasi Hirarki Berkas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = config.structureType.shortLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = config.structureType.treeRepresentation,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NeonEmerald,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Card: Train / Val / Test Split Configurator
        GlassBox(
            shape = RoundedCornerShape(18.dp),
            borderColor = NeonPurple.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Rasio Pembagian Dataset (Split)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "${config.trainSplitPercent}% Train : ${config.valSplitPercent}% Val : ${config.testSplitPercent}% Test",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple(70, 20, 10),
                        Triple(80, 10, 10),
                        Triple(80, 20, 0),
                        Triple(100, 0, 0)
                    ).forEach { (tr, va, te) ->
                        val isCurrent = config.trainSplitPercent == tr && config.valSplitPercent == va && config.testSplitPercent == te
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) NeonPurple.copy(alpha = 0.35f) else Color(0x22000000))
                                .border(1.dp, if (isCurrent) NeonPurple else GlassBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setSplitPercentages(tr, va, te) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (te > 0) "$tr:$va:$te" else "$tr:$va",
                                fontSize = 10.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Train Split Slider
                var trainSlider by remember(config.trainSplitPercent) { mutableFloatStateOf(config.trainSplitPercent.toFloat()) }
                Text(
                    text = "Persentase Train: ${trainSlider.toInt()}%",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Slider(
                    value = trainSlider,
                    onValueChange = {
                        trainSlider = it
                        val tr = it.toInt()
                        val remaining = 100 - tr
                        val va = (remaining * 0.67).toInt()
                        val te = remaining - va
                        viewModel.setSplitPercentages(tr, va, te)
                    },
                    valueRange = 50f..100f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Apply splits to database button
                GlassBox(
                    shape = RoundedCornerShape(10.dp),
                    borderColor = NeonPurple,
                    backgroundBrush = Brush.linearGradient(
                        listOf(NeonPurple.copy(alpha = 0.35f), Color(0x15FFFFFF))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.applyStratifiedSplits()
                            Toast.makeText(context, "Split ${config.trainSplitPercent}% Train / ${config.valSplitPercent}% Val diterapkan!", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("apply_stratified_split_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Terapkan Pembagian ke Database Sekarang",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Card: File Naming Convention
        GlassBox(
            shape = RoundedCornerShape(18.dp),
            borderColor = GlassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Konvensi Penamaan Berkas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Tentukan format nama berkas gambar di dalam arsip keluaran.",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FileNamingScheme.entries.forEach { scheme ->
                        val isSelected = scheme == config.namingScheme
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0x18000000))
                                .border(1.dp, if (isSelected) NeonCyan else GlassBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setFileNamingScheme(scheme) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = scheme.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) NeonCyan else TextPrimary
                                    )
                                    Text(
                                        text = "Contoh: ${scheme.example}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) NeonEmerald else TextSecondary
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. METADATA EXPORT SECTION (JSON / CSV / YAML)
 */
@Composable
private fun MetadataFilesSection(
    viewModel: DatasetViewModel,
    config: DatasetFolderConfig,
    selectedFormat: MetadataExportFormat,
    onFormatSelected: (MetadataExportFormat) -> Unit,
    previewContent: String,
    samplesCount: Int,
    classesCount: Int
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Format Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MetadataExportFormat.entries.forEach { format ->
                val isSelected = format == selectedFormat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) NeonCyan.copy(alpha = 0.35f) else Color(0x22000000)
                        )
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else GlassBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onFormatSelected(format) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("metadata_format_${format.name}")
                ) {
                    Text(
                        text = format.shortName,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }

        // Active Format Info Card
        GlassBox(
            shape = RoundedCornerShape(16.dp),
            borderColor = NeonCyan.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedFormat.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = selectedFormat.description,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // File extension badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = ".${selectedFormat.extension.uppercase()}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }

        // Action Buttons: Share/Download File + Copy Content
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Download / Share file button
            GlassBox(
                shape = RoundedCornerShape(12.dp),
                borderColor = NeonCyan,
                backgroundBrush = Brush.linearGradient(
                    listOf(NeonCyan.copy(alpha = 0.35f), Color(0x15FFFFFF))
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        viewModel.exportSingleMetadataFile(selectedFormat) { file ->
                            DatasetExporter.shareFile(
                                context,
                                file,
                                selectedFormat.mimeType,
                                selectedFormat.defaultFilename
                            )
                        }
                    }
                    .testTag("download_metadata_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Unduh / Bagikan Berkas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Copy to clipboard button
            GlassBox(
                shape = RoundedCornerShape(12.dp),
                borderColor = GlassBorder,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(selectedFormat.defaultFilename, previewContent)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Konten ${selectedFormat.defaultFilename} disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    }
                    .testTag("copy_metadata_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Salin Teks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Live Code / File Syntax Viewer
        GlassBox(
            shape = RoundedCornerShape(16.dp),
            borderColor = GlassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald)
                        )
                        Text(
                            text = selectedFormat.defaultFilename,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "${previewContent.lines().size} Baris",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = previewContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

/**
 * 3. FULL DATASET ZIP ARCHIVE BUNDLE SECTION
 */
@Composable
private fun ZipBundleSection(
    viewModel: DatasetViewModel,
    config: DatasetFolderConfig,
    samples: List<com.example.data.db.DatasetSampleEntity>,
    classes: List<com.example.data.db.DatasetClassEntity>,
    isProcessing: Boolean
) {
    val context = LocalContext.current

    val rawCount = samples.count { !it.isAugmented && !it.isSegmented }
    val augmentedCount = samples.count { it.isAugmented }
    val segmentedCount = samples.count { it.isSegmented }
    val totalSizeBytes = samples.sumOf { it.fileSizeBytes }
    val totalMb = (totalSizeBytes / (1024.0 * 1024.0))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Summary & Metrics Hero Card
        GlassBox(
            shape = RoundedCornerShape(20.dp),
            borderColor = NeonCyan.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${config.rootFolderName}_bundle.zip",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Arsip Siap Pelatihan PyTorch / TF / YOLO",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Size Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3010B981))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f MB", totalMb),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3 Metrics Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricMiniCard("Gambar Asli", "$rawCount", NeonCyan)
                    MetricMiniCard("Augmentasi", "$augmentedCount", NeonPurple)
                    MetricMiniCard("Segmentasi", "$segmentedCount", NeonAmber)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inclusion Toggles
                Text(
                    text = "Filter Konten Ekspor:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sertakan Gambar Augmentasi", fontSize = 12.sp, color = TextPrimary)
                    Switch(
                        checked = config.includeAugmented,
                        onCheckedChange = { viewModel.setIncludeAugmentedInExport(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = Color(0x5500F0FF))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sertakan Segmentasi Mask", fontSize = 12.sp, color = TextPrimary)
                    Switch(
                        checked = config.includeSegmented,
                        onCheckedChange = { viewModel.setIncludeSegmentedInExport(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = Color(0x5500F0FF))
                    )
                }
            }
        }

        // Main Glow Export Button
        LiquidGlowBorder(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.exportDatasetZipWithConfig(config) { zipFile ->
                        DatasetExporter.shareFile(
                            context,
                            zipFile,
                            "application/zip",
                            "${config.rootFolderName}_bundle.zip"
                        )
                    }
                }
                .testTag("main_export_zip_action")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                        )
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = if (isProcessing) "Sedang Mengompres Arsip ZIP..." else "Ekspor & Unduh Paket ZIP Lengkap",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(title: String, value: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x18000000))
            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(text = title, fontSize = 9.5.sp, color = TextSecondary)
        }
    }
}

/**
 * 4. ML TRAINING PIPELINE CODE SNIPPETS
 */
@Composable
private fun MlPipelineCodeSection(
    config: DatasetFolderConfig,
    classes: List<com.example.data.db.DatasetClassEntity>
) {
    val context = LocalContext.current
    val pythonScript = remember(config, classes) {
        DatasetExporter.generatePythonLoaderCode(config, classes)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassBox(
            shape = RoundedCornerShape(16.dp),
            borderColor = NeonCyan.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kode Pelatihan Python Siap Pakai",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextPrimary
                        )
                    }

                    // Copy snippet button
                    GlassBox(
                        shape = RoundedCornerShape(8.dp),
                        borderColor = NeonCyan.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("load_dataset.py", pythonScript)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Kode Python disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("copy_python_script_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin",
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(text = "Salin", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                }

                Text(
                    text = "Cuplikan ini disesuaikan otomatis dengan skema '${config.structureType.shortLabel}' untuk PyTorch dan TensorFlow.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = pythonScript,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = NeonEmerald,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label disalin ke clipboard!", Toast.LENGTH_SHORT).show()
}
