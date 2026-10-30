package com.motionninja.editor.export

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.spacing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.motionninja.domain.ExportConfig
import com.motionninja.domain.VideoCodec
import com.motionninja.ui.theme.Theme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSettingsScreen(
    onExportConfigSelected: (ExportConfig) -> Unit,
    initialConfig: ExportConfig? = null,
    onDismiss: () -> Unit
) {
    var selectedConfig by remember { mutableStateOf(initialConfig ?: ExportConfig.HD_1080P) }
    var selectedCodec by remember { mutableStateOf(VideoCodec.H264) }
    var enableHdr by remember { mutableStateOf(false) }
    var colorSpace by remember { mutableStateOf(ExportConfig.ColorSpace.REC709) }
    
    val viewModel: ExportViewModel = viewModel()
    val deviceCapabilities by viewModel.deviceCapabilities.collectAsStateWithLifecycle()
    
    Theme.Surface {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Top App Bar
            TopAppBar(
                title = { Text("Export Settings", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.ic_close), "Close")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onExportConfigSelected(selectedConfig.copy(
                            codec = selectedCodec,
                            enableHdr = enableHdr,
                            colorSpace = colorSpace
                        ))
                        onDismiss()
                    }) {
                        Text("Apply", fontWeight = FontWeight.Medium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Theme.ColorScheme.surfaceContainer)
            )
            
            Divider()
            
            // Quality Presets
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(ExportConfig.getAllPresets().groupBy { it.isHighResolution }) { (isHighRes, presets) ->
                    if (isHighRes) {
                        // High-res section header
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Text("High Resolution", 
                                    style = Theme.Typography.titleMedium,
                                    color = Theme.ColorScheme.primary)
                                Text("Requires powerful device & more storage",
                                    style = Theme.Typography.bodySmall,
                                    color = Theme.ColorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        // Standard section header
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Text("Standard Quality",
                                    style = Theme.Typography.titleMedium,
                                    color = Theme.ColorScheme.primary)
                            }
                        }
                    }
                    
                    items(presets) { preset ->
                        item {
                            QualityPresetCard(
                                preset = preset,
                                isSelected = selectedConfig == preset,
                                isDeviceCapable = deviceCapabilities?.isResolutionSupported(preset.width, preset.height) ?: true,
                                onClick = {
                                    if (deviceCapabilities?.isResolutionSupported(preset.width, preset.height) ?: true) {
                                        selectedConfig = preset
                                        // Auto-enable HDR for HDR presets
                                        enableHdr = preset.enableHdr
                                        colorSpace = preset.colorSpace
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            Divider()
            
            // Advanced Options
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Advanced Options", style = Theme.Typography.titleMedium)
                
                // Codec Selection
                CodecSelector(
                    selectedCodec = selectedCodec,
                    onCodecChange = { selectedCodec = it },
                    deviceCapabilities = deviceCapabilities
                )
                
                // HDR Toggle
                if (selectedConfig.enableHdr || deviceCapabilities?.hdrSupported == true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("HDR (High Dynamic Range)", style = Theme.Typography.bodyLarge)
                            Text("Richer colors, brighter highlights", 
                                style = Theme.Typography.bodySmall, 
                                color = Theme.ColorScheme.onSurfaceVariant)
                        }
                        Checkbox(
                            checked = enableHdr,
                            onCheckedChange = { enableHdr = it },
                            enabled = deviceCapabilities?.hdrSupported == true
                        )
                    }
                }
                
                // Color Space
                if (enableHdr) {
                    ColorSpaceSelector(
                        selectedColorSpace = colorSpace,
                        onChange = { colorSpace = it }
                    )
                }
            }
        }
    }
}

@Composable
fun QualityPresetCard(
    preset: ExportConfig,
    isSelected: Boolean,
    isDeviceCapable: Boolean,
    onClick: () -> Unit
) {
    val estimatedSize = preset.estimatedFileSizePerMinuteMB
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
        onClick = if (isDeviceCapable) onClick else null,
        enabled = isDeviceCapable,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Theme.ColorScheme.primaryContainer 
                else Theme.ColorScheme.surfaceContainerHighest,
            contentColor = if (isSelected) 
                Theme.ColorScheme.onPrimaryContainer 
                else Theme.ColorScheme.onSurface
        ),
        border = if (isSelected) 
            androidx.compose.ui.graphics.Outline.Border(2.dp, Theme.ColorScheme.primary) 
            else null
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(preset.name, 
                        style = Theme.Typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    
                    if (preset.isHighResolution) {
                        androidx.compose.material3.Badge(
                            badgeContent = { Text("HIGH RES", style = Theme.Typography.labelSmall, fontWeight = FontWeight.Bold) },
                            colors = androidx.compose.material3.BadgeDefaults.badgeColors(
                                containerColor = Theme.ColorScheme.secondaryContainer,
                                contentColor = Theme.ColorScheme.onSecondaryContainer
                            )
                        )
                    }
                    
                    if (preset.enableHdr) {
                        androidx.compose.material3.Badge(
                            badgeContent = { Text("HDR", style = Theme.Typography.labelSmall, fontWeight = FontWeight.Bold) },
                            colors = androidx.compose.material3.BadgeDefaults.badgeColors(
                                containerColor = Theme.ColorScheme.tertiaryContainer,
                                contentColor = Theme.ColorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${preset.width}x${preset.height} • ${preset.fps}fps • ${preset.bitrateMbps}Mbps",
                        style = Theme.Typography.bodySmall,
                        color = Theme.ColorScheme.onSurfaceVariant)
                    Text("~${String.format("%.0f", estimatedSize)} MB/min",
                        style = Theme.Typography.bodySmall,
                        color = Theme.ColorScheme.onSurfaceVariant)
                }
                
                if (!isDeviceCapable) {
                    Text("Device may not support this resolution",
                        style = Theme.Typography.bodySmall,
                        color = Theme.ColorScheme.error)
                }
            }
            
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = isDeviceCapable,
                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = Theme.ColorScheme.primary,
                    unselectedColor = Theme.ColorScheme.outline
                )
            )
        }
    }
}

@Composable
fun CodecSelector(
    selectedCodec: VideoCodec,
    onCodecChange: (VideoCodec) -> Unit,
    deviceCapabilities: ExportViewModel.DeviceCapabilities?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Video Codec", style = Theme.Typography.bodyLarge)
        Text("H.264 = best compatibility, HEVC = smaller files, AV1 = smallest (newest devices)",
            style = Theme.Typography.bodySmall,
            color = Theme.ColorScheme.onSurfaceVariant)
        
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                VideoCodec.H264 to "H.264 (AVC)",
                VideoCodec.HEVC to "HEVC (H.265)",
                VideoCodec.VP9 to "VP9",
                VideoCodec.AV1 to "AV1"
            ).forEach { (codec, label) ->
                val isAvailable = deviceCapabilities?.isCodecAvailable(codec) ?: true
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp),
                    onClick = if (isAvailable) { onCodecChange(codec) } else null,
                    enabled = isAvailable,
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = if (selectedCodec == codec) 
                            Theme.ColorScheme.primaryContainer 
                            else Theme.ColorScheme.surfaceContainerHighest,
                        contentColor = if (selectedCodec == codec) 
                            Theme.ColorScheme.onPrimaryContainer 
                            else Theme.ColorScheme.onSurface
                    ),
                    border = if (selectedCodec == codec) 
                        androidx.compose.ui.graphics.Outline.Border(2.dp, Theme.ColorScheme.primary) 
                        else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(label, 
                            style = Theme.Typography.labelLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = if (selectedCodec == codec) FontWeight.Bold else FontWeight.Normal)
                        if (!isAvailable) {
                            Text("Unsupported", 
                                style = Theme.Typography.labelSmall,
                                color = Theme.ColorScheme.error,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSpaceSelector(
    selectedColorSpace: ExportConfig.ColorSpace,
    onChange: (ExportConfig.ColorSpace) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Color Space", style = Theme.Typography.bodyLarge)
        
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportConfig.ColorSpace.values().forEach { space ->
                val label = when (space) {
                    ExportConfig.ColorSpace.REC709 -> "Rec.709 (SDR)"
                    ExportConfig.ColorSpace.REC2020 -> "Rec.2020 (HDR)"
                    ExportConfig.ColorSpace.P3_D65 -> "P3 (Wide Gamut)"
                }
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    onClick = { onChange(space) },
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = if (selectedColorSpace == space) 
                            Theme.ColorScheme.primaryContainer 
                            else Theme.ColorScheme.surfaceContainerHighest,
                        contentColor = if (selectedColorSpace == space) 
                            Theme.ColorScheme.onPrimaryContainer 
                            else Theme.ColorScheme.onSurface
                    ),
                    border = if (selectedColorSpace == space) 
                        androidx.compose.ui.graphics.Outline.Border(2.dp, Theme.ColorScheme.primary) 
                        else null
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, 
                            style = Theme.Typography.labelLarge,
                            textAlign = TextAlign.Center,
                            fontWeight = if (selectedColorSpace == space) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}