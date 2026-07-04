package com.example.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core.theme.LocalCortexSpacing
import com.example.domain.model.Folder
import com.example.domain.model.Note
import com.example.ui.components.CortexChip
import com.example.ui.components.CortexEmptyState
import com.example.ui.components.CortexTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onNavigateToNote: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = LocalCortexSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isSearchActive by remember { mutableStateOf(false) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }

    if (showCreateFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Nová složka") },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Název") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.onEvent(NotesEvent.OnCreateFolder(folderName))
                        }
                        showCreateFolderDialog = false
                    }
                ) {
                    Text("Vytvořit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Zrušit")
                }
            }
        )
    }

    var showSortDialog by remember { mutableStateOf(false) }

    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Řazení") },
            text = {
                Column {
                    com.example.core.config.SortOrder.entries.forEach { order ->
                        val label = when (order) {
                            com.example.core.config.SortOrder.NAME_ASC -> "Název (A-Z)"
                            com.example.core.config.SortOrder.NAME_DESC -> "Název (Z-A)"
                            com.example.core.config.SortOrder.DATE_MODIFIED_DESC -> "Naposledy upraveno"
                            com.example.core.config.SortOrder.DATE_CREATED_DESC -> "Nejnovější"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onEvent(NotesEvent.OnSortOrderChange(order))
                                    showSortDialog = false
                                }
                                .padding(vertical = spacing.small)
                        ) {
                            RadioButton(
                                selected = uiState.sortOrder == order,
                                onClick = {
                                    viewModel.onEvent(NotesEvent.OnSortOrderChange(order))
                                    showSortDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Zavřít") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearchActive) {
                SearchBarTopBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onEvent(NotesEvent.OnSearchQueryChange(it)) },
                    onClose = {
                        isSearchActive = false
                        viewModel.onEvent(NotesEvent.OnSearchQueryChange(""))
                    }
                )
            } else {
                CortexTopBar(
                    title = "Poznámky",
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Hledat")
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Možnosti")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Řazení") },
                                onClick = {
                                    showMenu = false
                                    showSortDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nová složka") },
                                onClick = {
                                    showMenu = false
                                    showCreateFolderDialog = true
                                }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToNote("new") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nová poznámka")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Folder Breadcrumbs
            if (uiState.currentFolderPath.isNotEmpty() || uiState.currentFolderId != null) {
                FolderBreadcrumbs(
                    path = uiState.currentFolderPath,
                    onFolderClick = { viewModel.onEvent(NotesEvent.OnFolderClick(it)) },
                    modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small)
                )
            }

            // Tags Row
            if (uiState.tags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    items(uiState.tags) { tag ->
                        CortexChip(
                            text = tag.name,
                            selected = uiState.activeTagIds.contains(tag.id),
                            onClick = { viewModel.onEvent(NotesEvent.OnTagFilterClick(tag.id)) },
                            color = Color(tag.color)
                        )
                    }
                }
            }

            // Sub-folders (if any)
            val subFolders = uiState.folders.filter { it.parentId == uiState.currentFolderId }
            if (subFolders.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.medium, vertical = spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    items(subFolders) { folder ->
                        ElevatedCard(
                            onClick = { viewModel.onEvent(NotesEvent.OnFolderClick(folder.id)) },
                            modifier = Modifier.padding(vertical = spacing.small)
                        ) {
                            Row(
                                modifier = Modifier.padding(spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(spacing.small))
                                Text(folder.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Main Content
            AnimatedVisibility(
                visible = !uiState.isLoading,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                if (uiState.notes.isEmpty() && subFolders.isEmpty()) {
                    CortexEmptyState(title = "Žádné poznámky", message = "Vytvořte svou první poznámku stisknutím tlačítka plus.", icon = Icons.Default.EditNote, modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteItem(
                                note = note,
                                onClick = { onNavigateToNote(note.id) },
                                onDelete = {
                                    viewModel.onEvent(NotesEvent.OnDeleteNote(note))
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Poznámka smazána",
                                            actionLabel = "Zpět"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.onEvent(NotesEvent.OnUndoDelete)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Hledat...") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Zavřít hledání")
            }
        },
        modifier = modifier
    )
}

@Composable
fun FolderBreadcrumbs(
    path: List<Folder>,
    onFolderClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalCortexSpacing.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Vault",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onFolderClick(null) }
                .padding(spacing.small)
        )
        path.forEach { folder ->
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onFolderClick(folder.id) }
                    .padding(spacing.small)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalCortexSpacing.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.errorContainer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(horizontal = spacing.large),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(spacing.medium)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.contentSnippet.isNotBlank()) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = note.contentSnippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        note.tags.take(3).forEach { tag ->
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(tag.color)
                            )
                        }
                        if (note.tags.size > 3) {
                            Text(text = "+${note.tags.size - 3}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    val spacing = LocalCortexSpacing.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        Text(
            text = "Žádné poznámky",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = "Klepněte na + pro vytvoření nové poznámky",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
