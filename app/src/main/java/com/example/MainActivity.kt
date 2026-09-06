package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.project.ProjectHubScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DatasetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val datasetViewModel: DatasetViewModel = viewModel()
                val activeProject by datasetViewModel.activeProject.collectAsState()

                Crossfade(
                    targetState = activeProject != null,
                    animationSpec = tween(250),
                    label = "root_navigation"
                ) { hasProject ->
                    if (hasProject) {
                        MainAppScreen(
                            viewModel = datasetViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ProjectHubScreen(
                            viewModel = datasetViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
