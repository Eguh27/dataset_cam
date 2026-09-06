package com.example.ui.screens.explorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.DatasetSampleEntity
import com.example.ui.components.DatasetAugmentationDialog
import com.example.ui.components.GlassBox
import com.example.ui.components.ProjectSwitchBottomSheet
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRose
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DatasetViewModel
import java.io.File

@Composable
fun DatasetExplorerScreen(
    viewModel: DatasetViewModel,
    onBackToStudio: (() -> Unit)? = null,
    onNavigateToStats: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allClasses by viewModel.allClasses.collectAsState()
    val allSamples by viewModel.allSamples.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    var showProjectSwitchSheet by remember { mutableStateOf(false) }
    var selectedFilterClassId by remember { mutableStateOf<Long?>(null) }
    var inspectingSample by remember { mutableStateOf<DatasetSampleEntity?>(null) }
    var augmentingSample by remember { mutableStateOf<DatasetSampleEntity?>(null) }
    var isGridView by remember { mutableStateOf(true) }

    // Multi-Select Batch Delete State
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedSampleIds = remember { mutableStateListOf<Long>() }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }

    val filteredSamples = remember(allSamples, selectedFilterClassId) {
        if (selectedFilterClassId == null) {
            allSamples
        } else {
            allSamples.filter { it.classId == selectedFilterClassId }
        }
    }

    val totalBytes = remember(allSamples) {
        allSamples.sumOf { it.fileSizeBytes }
    }
    val totalMbStr = remember(totalBytes) {
        String.format("%.2f MB", totalBytes / (1024.0 * 1024.0))
    }
    val augmentedCount = remember(allSamples) {
        allSamples.count { it.isAugmented }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            // Header Top Bar: Seamlessly switches between normal and Selection Mode
            if (isSelectionMode) {
                // Contextual Selection Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassBorder, CircleShape)
                                .clickable {
                                    isSelectionMode = false
                                    selectedSampleIds.clear()
                                }
                                .testTag("exit_selection_mode_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Batal Mode Pilih",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "${selectedSampleIds.size} Gambar Dipilih",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            )
                            Text(
                                text = "Ketuk gambar untuk memilih / membatalkan",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Select All / Deselect All Toggle
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (selectedSampleIds.size == filteredSamples.size && filteredSamples.isNotEmpty()) NeonCyan else GlassBorder,
                                    CircleShape
                                )
                                .clickable {
                                    if (selectedSampleIds.size == filteredSamples.size) {
                                        selectedSampleIds.clear()
                                    } else {
                                        selectedSampleIds.clear()
                                        selectedSampleIds.addAll(filteredSamples.map { it.id })
                                    }
                                }
                                .testTag("select_all_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Pilih Semua",
                                tint = if (selectedSampleIds.size == filteredSamples.size && filteredSamples.isNotEmpty()) NeonCyan else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Header Batch Delete Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (selectedSampleIds.isNotEmpty()) Color(0xFFF43F5E).copy(alpha = 0.2f) else DarkSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (selectedSampleIds.isNotEmpty()) Color(0xFFF43F5E) else GlassBorder,
                                    CircleShape
                                )
                                .clickable(enabled = selectedSampleIds.isNotEmpty()) {
                                    showBatchDeleteConfirmDialog = true
                                }
                                .testTag("header_batch_delete_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Terpilih",
                                tint = if (selectedSampleIds.isNotEmpty()) Color(0xFFF43F5E) else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // Standard Header Top Bar: Responsive and cleanly spaced
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onBackToStudio != null) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .clickable { onBackToStudio() }
                                    .testTag("explorer_back_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Kembali ke Studio",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Dataset Explorer",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                // Project Switch Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceElevated)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                                        .clickable { showProjectSwitchSheet = true }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                        .testTag("explorer_project_pill"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "Ganti Proyek",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = (activeProject?.name?.take(10) ?: "Proyek") + " ▾",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NeonCyan
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${filteredSamples.size} dari ${allSamples.size} gambar",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    // Action controls: Selection Mode, View mode toggle, Augmentation utility, Stats shortcut
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Multi-Select Mode Button
                        if (allSamples.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .clickable { isSelectionMode = true }
                                    .testTag("toggle_selection_mode_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = "Mode Pilih Gambar",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // View Mode Toggle (Grid vs List / File Manager)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassBorder, CircleShape)
                            .clickable { isGridView = !isGridView }
                            .testTag("toggle_view_mode_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.ViewModule,
                            contentDescription = if (isGridView) "Beralih ke Tampilan Daftar" else "Beralih ke Tampilan Grid",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Augment Utility Button
                    if (allSamples.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, NeonPurple.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    val candidate = filteredSamples.firstOrNull() ?: allSamples.firstOrNull()
                                    if (candidate != null) {
                                        augmentingSample = candidate
                                    }
                                }
                                .testTag("explorer_augmentation_utility_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Utilitas Augmentasi",
                                tint = NeonPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Stats shortcut
                    if (onNavigateToStats != null) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassBorder, CircleShape)
                                .clickable { onNavigateToStats() }
                                .testTag("explorer_stats_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Statistik Dataset",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

            // Summary Stats Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(label = "Total", value = "${allSamples.size} File")
                StatPill(label = "Ukuran", value = totalMbStr)
                if (augmentedCount > 0) {
                    StatPill(label = "Augmentasi", value = "$augmentedCount Citra", color = NeonPurple)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Class Filter Horizontal Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Semua" Chip
                val isAllSelected = selectedFilterClassId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isAllSelected) NeonCyan.copy(alpha = 0.15f) else DarkSurfaceElevated)
                        .border(
                            1.dp,
                            if (isAllSelected) NeonCyan else GlassBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedFilterClassId = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("filter_all_chip")
                ) {
                    Text(
                        text = "Semua (${allSamples.size})",
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllSelected) NeonCyan else TextSecondary
                    )
                }

                // Category Chips
                allClasses.forEach { cls ->
                    val isSelected = selectedFilterClassId == cls.id
                    val classCount = allSamples.count { it.classId == cls.id }
                    val classColor = try {
                        Color(android.graphics.Color.parseColor(cls.colorHex))
                    } catch (_: Exception) {
                        NeonCyan
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) classColor.copy(alpha = 0.15f) else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSelected) classColor else GlassBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilterClassId = cls.id }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_chip_${cls.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(classColor)
                            )
                            Text(
                                text = "${cls.name} ($classCount)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content Area: Zero overlap, responsive grid or list
            if (filteredSamples.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Belum Ada Sampel Gambar",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gunakan tab Studio kamera untuk memotret citra dengan resolusi standar ML.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isGridView) {
                // Grid View: Perfectly uniform cards with zero height mismatch or overlapping
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = 4.dp,
                        bottom = if (isSelectionMode) 120.dp else 90.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSamples, key = { it.id }) { sample ->
                        val isSelected = sample.id in selectedSampleIds
                        SampleGridCard(
                            sample = sample,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) {
                                        selectedSampleIds.remove(sample.id)
                                    } else {
                                        selectedSampleIds.add(sample.id)
                                    }
                                } else {
                                    inspectingSample = sample
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedSampleIds.add(sample.id)
                                }
                            }
                        )
                    }
                }
            } else {
                // List / File Manager View: Organized row-by-row file manager layout
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = 4.dp,
                        bottom = if (isSelectionMode) 120.dp else 90.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSamples, key = { it.id }) { sample ->
                        val isSelected = sample.id in selectedSampleIds
                        SampleListItem(
                            sample = sample,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) {
                                        selectedSampleIds.remove(sample.id)
                                    } else {
                                        selectedSampleIds.add(sample.id)
                                    }
                                } else {
                                    inspectingSample = sample
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedSampleIds.add(sample.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Bottom Action Bar for Multi-Selection
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SlateDark900,
                border = BorderStroke(1.dp, if (selectedSampleIds.isNotEmpty()) NeonCyan else GlassBorder),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selection_bottom_action_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${selectedSampleIds.size} dipilih",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedSampleIds.isNotEmpty()) NeonCyan else TextSecondary
                        )
                        TextButton(
                            onClick = {
                                if (selectedSampleIds.size == filteredSamples.size && filteredSamples.isNotEmpty()) {
                                    selectedSampleIds.clear()
                                } else {
                                    selectedSampleIds.clear()
                                    selectedSampleIds.addAll(filteredSamples.map { it.id })
                                }
                            },
                            modifier = Modifier.testTag("select_all_toggle_btn")
                        ) {
                            Text(
                                text = if (selectedSampleIds.size == filteredSamples.size && filteredSamples.isNotEmpty()) "Batal Semua" else "Pilih Semua (${filteredSamples.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan
                            )
                        }
                    }

                    Button(
                        onClick = { showBatchDeleteConfirmDialog = true },
                        enabled = selectedSampleIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF43F5E),
                            disabledContainerColor = Color(0x33F43F5E),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("batch_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hapus (${selectedSampleIds.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Batch Delete Confirmation AlertDialog
        if (showBatchDeleteConfirmDialog && selectedSampleIds.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirmDialog = false },
                containerColor = SlateDark900,
                title = {
                    Text(
                        text = "Hapus ${selectedSampleIds.size} Gambar?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus ${selectedSampleIds.size} gambar terpilih dari dataset? File gambar dan variasi augmentasinya akan dihapus secara permanen.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val samplesToDelete = allSamples.filter { it.id in selectedSampleIds }
                            viewModel.deleteSamplesBatch(samplesToDelete) {
                                selectedSampleIds.clear()
                                isSelectionMode = false
                                showBatchDeleteConfirmDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF43F5E),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_batch_delete_btn")
                    ) {
                        Text("Hapus ${selectedSampleIds.size} Gambar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showBatchDeleteConfirmDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Batal")
                    }
                }
            )
        }

        // Inspection Detail Modal
        inspectingSample?.let { sample ->
            SampleInspectionModal(
                sample = sample,
                onDismiss = { inspectingSample = null },
                onOpenAugment = { augmentingSample = sample },
                onDelete = {
                    viewModel.deleteSample(sample)
                    inspectingSample = null
                }
            )
        }

        // Augmentation Utility Dialog
        augmentingSample?.let { sample ->
            DatasetAugmentationDialog(
                sample = sample,
                viewModel = viewModel,
                onDismiss = { augmentingSample = null },
                onApplied = {
                    augmentingSample = null
                    inspectingSample = null
                }
            )
        }

        // Project Switcher Bottom Sheet
        if (showProjectSwitchSheet) {
            ProjectSwitchBottomSheet(
                viewModel = viewModel,
                onDismissRequest = { showProjectSwitchSheet = false },
                onGoToProjectHub = {
                    showProjectSwitchSheet = false
                    viewModel.switchProject()
                }
            )
        }
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color = NeonCyan) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, fontSize = 10.sp, color = TextMuted)
            Text(text = "•", fontSize = 10.sp, color = TextMuted)
            Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

/**
 * Grid layout card with uniform 1:1 square preview and neatly separated metadata.
 * Completely eliminates any badge collision or jagged layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SampleGridCard(
    sample: DatasetSampleEntity,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val file = remember(sample.filePath) { File(sample.filePath) }
    val kbStr = remember(sample.fileSizeBytes) {
        String.format("%.1f KB", sample.fileSizeBytes / 1024.0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) SlateDark800 else DarkSurfaceElevated)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) NeonCyan else if (sample.isAugmented) NeonPurple.copy(alpha = 0.4f) else GlassBorder,
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("sample_card_${sample.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Square Thumbnail Container with Non-Overlapping Tags
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.0f)
                    .background(Color(0xFF0B0F17))
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = sample.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Bar: Resolution tag on left, Status badges or Selection Indicator on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Resolution Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${sample.width}px",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Feature badges (Augmentation / Segmentation) or Selection Check
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSelectionMode) {
                            if (sample.isSegmented) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(NeonEmerald)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCut,
                                        contentDescription = "Segmented",
                                        tint = Color.Black,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            if (sample.isAugmented) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(NeonPurple)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Augmented",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        } else {
                            // Selection Indicator Circle
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) NeonCyan else Color.Black.copy(alpha = 0.65f))
                                    .border(
                                        1.5.dp,
                                        if (isSelected) NeonCyan else Color.White.copy(alpha = 0.8f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Dipilih",
                                        tint = Color(0xFF090D14),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dedicated Card Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = sample.className,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sample.aspectRatio} • $kbStr",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    if (sample.isAugmented) {
                        Text(
                            text = sample.augmentationType.take(10),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonPurple,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * File Manager list row layout: Clean horizontal rows, ideal for browsing large datasets neatly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SampleListItem(
    sample: DatasetSampleEntity,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val file = remember(sample.filePath) { File(sample.filePath) }
    val kbStr = remember(sample.fileSizeBytes) {
        String.format("%.1f KB", sample.fileSizeBytes / 1024.0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SlateDark800 else DarkSurfaceElevated)
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) NeonCyan else GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(10.dp)
            .testTag("sample_list_item_${sample.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection Checkbox in List Mode
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) NeonCyan else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (isSelected) NeonCyan else Color.White.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Dipilih",
                            tint = Color(0xFF090D14),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B0F17))
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = sample.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = sample.fileName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = sample.className,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Text(
                        text = "${sample.width}x${sample.height} (${sample.aspectRatio}) • $kbStr",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // Badges & Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (sample.isAugmented) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonPurple.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sample.augmentationType,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonPurple
                        )
                    }
                }
                if (sample.isSegmented) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Cutout",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonEmerald
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SampleInspectionModal(
    sample: DatasetSampleEntity,
    onDismiss: () -> Unit,
    onOpenAugment: () -> Unit,
    onDelete: () -> Unit
) {
    val file = remember(sample.filePath) { File(sample.filePath) }
    val kbStr = remember(sample.fileSizeBytes) {
        String.format("%.1f KB", sample.fileSizeBytes / 1024.0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() }
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .background(DarkSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                .clickable(enabled = false) {}
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail Sampel Dataset",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High Resolution Image Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = file,
                        contentDescription = sample.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Breakdown Table
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetaRow("Folder Kategori", sample.className, NeonCyan)
                    MetaRow("Nama Berkas", sample.fileName, TextPrimary)
                    MetaRow("Dimensi Pixel", "${sample.width} x ${sample.height} (${sample.aspectRatio})", TextPrimary)
                    MetaRow("Ukuran Berkas", kbStr, TextPrimary)
                    MetaRow("Format Encoding", "${sample.format} (Standar ML)", TextPrimary)
                    MetaRow("Segmentasi", if (sample.isSegmented) "Ya (Cutout Background)" else "Tidak", if (sample.isSegmented) NeonEmerald else TextSecondary)
                    MetaRow("Tipe Sampel", if (sample.isAugmented) "Augmentasi (${sample.augmentationType})" else "Original Capture", if (sample.isAugmented) NeonPurple else TextPrimary)
                    MetaRow("Partisi ML", sample.split, NeonCyan)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonRose.copy(alpha = 0.12f))
                            .border(1.dp, NeonRose.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable { onDelete() }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("delete_sample_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = NeonRose,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Hapus",
                                color = NeonRose,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Augment Utility Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonPurple.copy(alpha = 0.15f))
                            .border(1.dp, NeonPurple.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { onOpenAugment() }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("modal_augment_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Augmentasi",
                                tint = NeonPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Utilitas Augmentasi",
                                color = NeonPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable { onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "Tutup",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetaRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
