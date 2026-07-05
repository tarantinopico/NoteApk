package com.example.ui.notes.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.UUID
import com.example.core.config.FontStyle
import com.example.core.theme.LocalAppConfig
import com.example.core.config.NoteMode
import com.example.core.theme.LocalCortexSpacing
import com.example.ui.components.CortexTopBar
import com.example.ui.components.markdown.MarkdownRenderer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val appConfig = LocalAppConfig.current
    val spacing = LocalCortexSpacing.current

    var showUnsavedDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isDirty) {
        showUnsavedDialog = true
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Neuložené změny") },
            text = { Text("Máte neuložené změny. Chcete je uložit před odchodem?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        viewModel.onEvent(NoteDetailEvent.OnSave)
                        onNavigateBack()
                    }
                ) { Text("Uložit a odejít") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnsavedDialog = false
                        onNavigateBack()
                    }
                ) { Text("Zahodit") }
            }
        )
    }

    var showAddTagDialog by remember { mutableStateOf(false) }

    if (showAddTagDialog) {
        var newTagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Přidat štítek") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Název štítku") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.onEvent(NoteDetailEvent.OnTagAdd(com.example.domain.model.Tag(UUID.randomUUID().toString(), newTagName, 0xFF607D8B)))
                        }
                        showAddTagDialog = false
                    }
                ) {
                    Text("Přidat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text("Zrušit") }
            }
        )
    }

    Scaffold(
        topBar = {
            CortexTopBar(
                title = if (uiState.title.isBlank()) "Nová poznámka" else uiState.title,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = spacing.medium).size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(NoteDetailEvent.OnToggleMode) }) {
                        Icon(
                            imageVector = if (uiState.mode == NoteMode.EDIT) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = "Přepnout režim"
                        )
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Možnosti")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Smazat") },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            }
                        )
                    }
                    
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Smazat poznámku") },
                            text = { Text("Opravdu chcete smazat tuto poznámku? Tuto akci nelze vrátit.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteDialog = false
                                        viewModel.onEvent(NoteDetailEvent.OnDelete)
                                        onNavigateBack()
                                    }
                                ) {
                                    Text("Smazat", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Zrušit")
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.mode == NoteMode.EDIT) {
                    EditorToolbar(
                        onInsert = { text, cursorOffset ->
                            val current = uiState.content
                            // Simplified insertion at the end for now, normally you'd use TextFieldValue to get cursor
                            viewModel.onEvent(NoteDetailEvent.OnContentChange(current + text))
                        }
                    )
                }

                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(spacing.medium)
                ) {
                    if (uiState.mode == NoteMode.EDIT) {
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = { viewModel.onEvent(NoteDetailEvent.OnTitleChange(it)) },
                            placeholder = { Text("Název poznámky", style = MaterialTheme.typography.headlineMedium) },
                            textStyle = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.tags.forEach { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text(tag.name) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Odebrat",
                                            modifier = Modifier.size(16.dp).clickable { viewModel.onEvent(NoteDetailEvent.OnTagRemove(tag)) }
                                        )
                                    }
                                )
                            }
                            InputChip(
                                selected = false,
                                onClick = { showAddTagDialog = true },
                                label = { Text("Přidat štítek") },
                                trailingIcon = { Icon(Icons.Default.Add, contentDescription = "Přidat", modifier = Modifier.size(16.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))
                        OutlinedTextField(
                            value = uiState.content,
                            onValueChange = { viewModel.onEvent(NoteDetailEvent.OnContentChange(it)) },
                            placeholder = { Text("Pište sem...") },
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = if (appConfig.fontStyle == FontStyle.MONOSPACE) FontFamily.Monospace else FontFamily.Default
                            ),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = spacing.small)
                        )
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = spacing.large),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.tags.forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(tag.name) }
                                )
                            }
                        }

                        MarkdownRenderer(
                            content = uiState.content,
                            onChecklistToggle = { originalLine, isChecked, lineIndex ->
                                val newCheckChar = if (isChecked) "x" else " "
                                val newLine = originalLine.replaceFirst(Regex("-\\[.\\]"), "-[$newCheckChar]")
                                    .replaceFirst("- [ ]", "- [$newCheckChar]")
                                    .replaceFirst("- [x]", "- [$newCheckChar]")
                                    .replaceFirst("- [X]", "- [$newCheckChar]")
                                
                                val lines = uiState.content.lines().toMutableList()
                                if (lineIndex in lines.indices) {
                                    lines[lineIndex] = newLine
                                    viewModel.onEvent(NoteDetailEvent.OnContentChange(lines.joinToString("\n")))
                                }
                            }
                        )
                    }

                    if (uiState.linkedCalendarItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(spacing.large))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(spacing.small))
                        Text("Navázané události a úkoly", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(spacing.small))
                        uiState.linkedCalendarItems.forEach { item ->
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                            val timeStr = java.time.Instant.ofEpochMilli(item.startAt).atZone(java.time.ZoneId.systemDefault()).format(formatter)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (item.isTask) Icons.Default.Checklist else Icons.Default.Event, 
                                        contentDescription = null,
                                        tint = if (item.isTask && item.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = item.title, 
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (item.isTask && item.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(text = timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorToolbar(
    onInsert: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { onInsert("**text**", 2) }) { Icon(Icons.Default.FormatBold, "Tučně") }
            IconButton(onClick = { onInsert("*text*", 1) }) { Icon(Icons.Default.FormatItalic, "Kurzíva") }
            IconButton(onClick = { onInsert("\n# ", 3) }) { Icon(Icons.Default.Title, "Nadpis") }
            IconButton(onClick = { onInsert("\n- [ ] ", 7) }) { Icon(Icons.Default.Checklist, "Checklist") }
            IconButton(onClick = { onInsert("\n- ", 3) }) { Icon(Icons.Default.List, "Seznam") }
            IconButton(onClick = { onInsert("\n> ", 3) }) { Icon(Icons.Default.FormatQuote, "Citace") }
            IconButton(onClick = { onInsert("\n```\ncode\n```\n", 4) }) { Icon(Icons.Default.Code, "Kód") }
            IconButton(onClick = { onInsert("[odkaz](url)", 1) }) { Icon(Icons.Default.Link, "Odkaz") }
        }
    }
}
