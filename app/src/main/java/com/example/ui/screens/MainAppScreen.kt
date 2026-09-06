package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassBox
import com.example.ui.screens.augmentation.AugmentationSettingsScreen
import com.example.ui.screens.capture.CaptureScreen
import com.example.ui.screens.explorer.DatasetExplorerScreen
import com.example.ui.screens.export.ExportScreen
import com.example.ui.screens.stats.DatasetStatsScreen
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmPlumCanvas
import com.example.ui.viewmodel.DatasetViewModel

enum class NavigationTab(
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    STUDIO("Studio", Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera),
    DATASET("Dataset", Icons.Filled.FolderOpen, Icons.Outlined.FolderOpen),
    STATS("Statistik", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    AUGMENT("Augment", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    EXPORT("Ekspor", Icons.Filled.FileDownload, Icons.Outlined.FileDownload)
}

@Composable
fun MainAppScreen(
    viewModel: DatasetViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavigationTab.STUDIO) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Content Area with smooth animated crossfade
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(220),
            label = "tab_transition",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                NavigationTab.STUDIO -> CaptureScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { selectedTab = NavigationTab.AUGMENT },
                    onNavigateToExplorer = { selectedTab = NavigationTab.DATASET },
                    onNavigateToStats = { selectedTab = NavigationTab.STATS }
                )
                NavigationTab.DATASET -> DatasetExplorerScreen(
                    viewModel = viewModel,
                    onBackToStudio = { selectedTab = NavigationTab.STUDIO },
                    onNavigateToStats = { selectedTab = NavigationTab.STATS }
                )
                NavigationTab.STATS -> DatasetStatsScreen(
                    viewModel = viewModel,
                    onNavigateToExplorer = { classId ->
                        selectedTab = NavigationTab.DATASET
                    },
                    onNavigateToStudio = { selectedTab = NavigationTab.STUDIO }
                )
                NavigationTab.AUGMENT -> AugmentationSettingsScreen(viewModel = viewModel)
                NavigationTab.EXPORT -> ExportScreen(viewModel = viewModel)
            }
        }

        // When not in full-screen Studio viewfinder, show the sleek frosted bottom navigation bar
        AnimatedVisibility(
            visible = selectedTab != NavigationTab.STUDIO,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            GlassBox(
                shape = RoundedCornerShape(28.dp),
                borderColor = GlassBorder,
                backgroundBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xEE0F172A), Color(0xF2090D14))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("liquid_bottom_navigation")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        val icon = if (isSelected) tab.filledIcon else tab.outlinedIcon

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("nav_tab_${tab.name}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) NeonPurple else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isSelected) {
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPurple
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
