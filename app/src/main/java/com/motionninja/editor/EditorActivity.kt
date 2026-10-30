package com.motionninja.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.motionninja.domain.ExportConfig
import com.motionninja.editor.export.ExportManager
import com.motionninja.editor.export.ExportSettingsScreen
import com.motionninja.editor.export.ExportViewModel
import com.motionninja.ui.theme.Theme
import kotlinx.coroutines.launch

class EditorActivity : ComponentActivity() {
    private val exportViewModel: ExportViewModel by viewModels()
    private val exportManager: ExportManager by lazy { ExportManager(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Theme { EditorScreen(exportManager, exportViewModel) } }
    }
    
    @Composable
    fun ComposeEntryPoint(projectId: Long = 0L) {
        EditorScreen(exportManager, exportViewModel)
    }
}

@Composable
fun EditorScreen(
    exportManager: ExportManager,
    exportViewModel: ExportViewModel
) {
    var showExportSettings by remember { mutableStateOf(false) }
    var selectedExportConfig by remember { 
        mutableStateOf(exportViewModel.deviceCapabilities.value?.getRecommendedPreset() 
            ?: ExportConfig.HD_1080P) 
    }
    var exportProgress by remember { mutableStateOf(0f) }
    var isExporting by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf("") }
    var exportStage by remember { mutableStateOf(ExportManager.ExportStage.PREPARING) }
    
    Theme.Surface {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isExporting || exportProgress > 0f) 120.dp else 0.dp)
            ) {
                TopAppBar(
                    title = { Text("Editor", fontWeight = FontWeight.Medium) },
                    actions = {
                        Button(onClick = { showExportSettings = true }, enabled = !isExporting) {
                            Icon(androidx.compose.material.icons.defaults.Icons.Default.FileDownload, "Export")
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                            Text("Export")
                        }
                    }
                )
                
                // Export config indicator
                if (selectedExportConfig.isHighResolution) {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Theme.ColorScheme.primaryContainer
                        )
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            Text("${selectedExportConfig.name} • ${selectedExportConfig.width}x${selectedExportConfig.height} @ ${selectedExportConfig.fps}fps",
                                style = Theme.Typography.bodyMedium,
                                color = Theme.ColorScheme.onPrimaryContainer)
                            Text("~${String.format("%.0f", selectedExportConfig.estimatedFileSizePerMinuteMB)} MB/min",
                                style = Theme.Typography.bodySmall,
                                color = Theme.ColorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
                
                // Timeline / Canvas placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme.ColorScheme.surfaceContainerHighest)
                ) {
                    Text("Timeline / Canvas Area", 
                        style = Theme.Typography.headlineSmall,
                        color = Theme.ColorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center))
                }
            }
            
            // Bottom export progress
            if (isExporting || exportProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Theme.ColorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (isExporting) "$exportStage" else exportStatus,
                                style = Theme.Typography.bodyMedium)
                            LinearProgressIndicator(
                                progress = exportProgress,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            
            // Export Settings Modal
            if (showExportSettings) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    ExportSettingsScreen(
                        onExportConfigSelected = { config ->
                            selectedExportConfig = config
                            showExportSettings = false
                        },
                        initialConfig = selectedExportConfig,
                        onDismiss = { showExportSettings = false }
                    )
                }
            }
        }
    }
}