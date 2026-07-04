package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.config.*
import com.example.core.theme.LocalCortexSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val exportVaultUseCase: com.example.domain.usecase.ExportVaultUseCase,
    private val importVaultUseCase: com.example.domain.usecase.ImportVaultUseCase
) : ViewModel() {
    val appConfig: Flow<AppConfig> = configRepository.appConfig

    fun updateConfig(update: (AppConfig) -> AppConfig) {
        viewModelScope.launch {
            configRepository.updateConfig(update)
        }
    }

    fun resetConfig() {
        viewModelScope.launch {
            configRepository.resetConfig()
        }
    }
    
    fun exportVault(uri: android.net.Uri) {
        viewModelScope.launch {
            exportVaultUseCase(uri)
        }
    }
    
    fun importVault(uri: android.net.Uri) {
        viewModelScope.launch {
            importVaultUseCase(uri)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle(initialValue = AppConfig())
    val spacing = LocalCortexSpacing.current
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { viewModel.exportVault(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importVault(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // VZHLED (Appearance)
            item { SettingsCategoryHeader(title = "Vzhled", icon = Icons.Default.Palette) }
            item {
                SettingsDropdown(
                    title = "Režim motivu",
                    options = ThemeMode.entries.map { it.name },
                    selectedOption = appConfig.themeMode.name,
                    onOptionSelected = { viewModel.updateConfig { config -> config.copy(themeMode = ThemeMode.valueOf(it)) } }
                )
            }
            item {
                SettingsColorPicker(
                    title = "Akcentní barva",
                    selectedColor = Color(appConfig.accentColor),
                    onColorSelected = { viewModel.updateConfig { config -> config.copy(accentColor = it.toArgb().toLong() and 0xFFFFFFFF) } }
                )
            }
            item {
                SettingsSwitch(
                    title = "Dynamické barvy (Material You)",
                    checked = appConfig.useDynamicColors,
                    onCheckedChange = { viewModel.updateConfig { config -> config.copy(useDynamicColors = it) } }
                )
            }
            item {
                SettingsSlider(
                    title = "Zaoblení rohů",
                    value = appConfig.baseRadiusDp,
                    valueRange = 0f..24f,
                    onValueChange = { viewModel.updateConfig { config -> config.copy(baseRadiusDp = it) } }
                )
            }
            item {
                SettingsSlider(
                    title = "Hustota / Rozestupy",
                    value = appConfig.baseSpacingDp,
                    valueRange = 4f..16f,
                    onValueChange = { viewModel.updateConfig { config -> config.copy(baseSpacingDp = it) } }
                )
            }
            item {
                SettingsSlider(
                    title = "Zvětšení písma",
                    value = appConfig.textScale,
                    valueRange = 0.8f..1.5f,
                    onValueChange = { viewModel.updateConfig { config -> config.copy(textScale = it) } }
                )
            }
            item {
                SettingsSwitch(
                    title = "Animace",
                    checked = appConfig.isAnimationEnabled,
                    onCheckedChange = { viewModel.updateConfig { config -> config.copy(isAnimationEnabled = it) } }
                )
            }

            // EDITOR
            item { SettingsCategoryHeader(title = "Editor", icon = Icons.Default.Edit) }
            item {
                SettingsDropdown(
                    title = "Výchozí režim",
                    options = NoteMode.entries.map { it.name },
                    selectedOption = appConfig.defaultNoteMode.name,
                    onOptionSelected = { viewModel.updateConfig { config -> config.copy(defaultNoteMode = NoteMode.valueOf(it)) } }
                )
            }
            item {
                SettingsDropdown(
                    title = "Styl písma (Font)",
                    options = FontStyle.entries.map { it.name },
                    selectedOption = appConfig.fontStyle.name,
                    onOptionSelected = { viewModel.updateConfig { config -> config.copy(fontStyle = FontStyle.valueOf(it)) } }
                )
            }
            item {
                SettingsSwitch(
                    title = "Automatické ukládání",
                    checked = appConfig.autosaveEnabled,
                    onCheckedChange = { viewModel.updateConfig { config -> config.copy(autosaveEnabled = it) } }
                )
            }

            // ORGANIZACE
            item { SettingsCategoryHeader(title = "Organizace", icon = Icons.Default.Folder) }
            item {
                SettingsDropdown(
                    title = "Výchozí řazení",
                    options = SortOrder.entries.map { it.name },
                    selectedOption = appConfig.notesSortOrder.name,
                    onOptionSelected = { viewModel.updateConfig { config -> config.copy(notesSortOrder = SortOrder.valueOf(it)) } }
                )
            }

            // KALENDÁŘ
            item { SettingsCategoryHeader(title = "Kalendář", icon = Icons.Default.Event) }
            item {
                SettingsDropdown(
                    title = "První den v týdnu",
                    options = DayOfWeek.entries.map { it.name },
                    selectedOption = appConfig.firstDayOfWeek.name,
                    onOptionSelected = { viewModel.updateConfig { config -> config.copy(firstDayOfWeek = DayOfWeek.valueOf(it)) } }
                )
            }
            item {
                SettingsDropdown(
                    title = "Výchozí zobrazení",
                    options = CalendarViewModeConfig.entries.map { it.name },
                    selectedOption = appConfig.defaultCalendarView.name,
                    onOptionSelected = { viewModel.updateConfig { config -> config.copy(defaultCalendarView = CalendarViewModeConfig.valueOf(it)) } }
                )
            }
            item {
                SettingsDropdown(
                    title = "Formát data",
                    options = listOf("dd.MM.yyyy", "MM/dd/yyyy", "yyyy-MM-dd"),
                    selectedOption = appConfig.dateFormat,
                    onOptionSelected = { viewModel.updateConfig { config -> config.copy(dateFormat = it) } }
                )
            }

            // NOTIFIKACE
            item { SettingsCategoryHeader(title = "Notifikace", icon = Icons.Default.Notifications) }
            item {
                SettingsSwitch(
                    title = "Zvuk připomínek",
                    checked = appConfig.soundEnabled,
                    onCheckedChange = { viewModel.updateConfig { config -> config.copy(soundEnabled = it) } }
                )
            }
            item {
                SettingsSwitch(
                    title = "Vibrace",
                    checked = appConfig.vibrationEnabled,
                    onCheckedChange = { viewModel.updateConfig { config -> config.copy(vibrationEnabled = it) } }
                )
            }

            // CHOVÁNÍ
            item { SettingsCategoryHeader(title = "Chování", icon = Icons.Default.Settings) }
            item {
                SettingsDropdown(
                    title = "Výchozí obrazovka po startu",
                    options = listOf("notes", "links", "calendar"),
                    selectedOption = appConfig.startScreen,
                    onOptionSelected = { viewModel.updateConfig { config -> config.copy(startScreen = it) } }
                )
            }

            // DATA
            item { SettingsCategoryHeader(title = "Data a obnova", icon = Icons.Default.Storage) }
            item {
                ListItem(
                    headlineContent = { Text("Vault Cesta") },
                    supportingContent = { Text(appConfig.vaultUri ?: "Neuvedeno") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Exportovat Vault (ZIP)") },
                    supportingContent = { Text("Zálohuje všechny poznámky a data") },
                    modifier = Modifier.clickable { exportLauncher.launch("cortex_backup.zip") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Importovat Vault") },
                    supportingContent = { Text("Obnoví data ze ZIP souboru") },
                    modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/zip")) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Obnovit výchozí nastavení", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { viewModel.resetConfig() }
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val spacing = LocalCortexSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.medium, vertical = spacing.small)
            .padding(top = spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(spacing.small))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SettingsSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val spacing = LocalCortexSpacing.current
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun SettingsSlider(title: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    val spacing = LocalCortexSpacing.current
    Column(modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small)) {
        Text(text = title)
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(title: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    val spacing = LocalCortexSpacing.current
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                Text(
                    text = selectedOption,
                    modifier = Modifier.menuAnchor(),
                    color = MaterialTheme.colorScheme.primary
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onOptionSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun SettingsColorPicker(title: String, selectedColor: Color, onColorSelected: (Color) -> Unit) {
    val spacing = LocalCortexSpacing.current
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF00BCD4), // Cyan
    )

    Column(modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small)) {
        Text(text = title)
        Spacer(modifier = Modifier.height(spacing.small))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (color == selectedColor) 3.dp else 0.dp,
                            color = if (color == selectedColor) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}

