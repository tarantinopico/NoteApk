package com.example.ui.links

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.theme.LocalCortexSpacing
import com.example.domain.model.Bookmark
import com.example.ui.components.CortexEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(
    modifier: Modifier = Modifier,
    viewModel: LinksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = LocalCortexSpacing.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(uiState.error!!)
            viewModel.onEvent(LinksEvent.ClearError)
        }
    }

    if (uiState.isAddDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(LinksEvent.OnAddCancel) },
            title = { Text("Přidat odkaz") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.urlInput,
                        onValueChange = { viewModel.onEvent(LinksEvent.OnUrlInputChange(it)) },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.isAdding) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = spacing.small))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(LinksEvent.OnAddConfirm) },
                    enabled = !uiState.isAdding && uiState.urlInput.isNotBlank()
                ) {
                    Text("Přidat")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(LinksEvent.OnAddCancel) }, enabled = !uiState.isAdding) {
                    Text("Zrušit")
                }
            }
        )
    }

    if (uiState.bookmarkToEditTags != null) {
        val currentBookmark = uiState.bookmarkToEditTags!!
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(LinksEvent.OnEditTagsDismiss) },
            title = { Text("Štítky") },
            text = {
                LazyColumn {
                    items(uiState.availableTags, key = { it.id }) { tag ->
                        val hasTag = currentBookmark.tags.any { it.id == tag.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onEvent(LinksEvent.OnToggleTag(tag)) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = hasTag, onCheckedChange = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tag.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(LinksEvent.OnEditTagsDismiss) }) {
                    Text("Hotovo")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(LinksEvent.OnAddClick) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Přidat odkaz")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onEvent(LinksEvent.OnSearchQueryChange(it)) },
                    placeholder = { Text("Hledat odkazy...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.bookmarks.isEmpty()) {
                CortexEmptyState(
                    title = "Žádné odkazy",
                    message = "Uložte si své oblíbené webové stránky pro pozdější přečtení.",
                    icon = Icons.Default.Link,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    items(uiState.bookmarks, key = { it.id }) { bookmark ->
                        BookmarkItem(
                            bookmark = bookmark,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url))
                                context.startActivity(intent)
                            },
                            onDelete = { viewModel.onEvent(LinksEvent.OnDelete(bookmark)) },
                            onEditTags = { viewModel.onEvent(LinksEvent.OnEditTagsClick(bookmark)) },
                            onRefresh = { viewModel.onEvent(LinksEvent.OnRefreshPreview(bookmark)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditTags: () -> Unit,
    onRefresh: () -> Unit
) {
    val spacing = LocalCortexSpacing.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            if (bookmark.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bookmark.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
            
            Column(modifier = Modifier.padding(spacing.medium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp).padding(start = 4.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Možnosti")
                        }
                        
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Otevřít") },
                                onClick = {
                                    showMenu = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Štítky") },
                                onClick = {
                                    showMenu = false
                                    onEditTags()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Aktualizovat náhled") },
                                onClick = {
                                    showMenu = false
                                    onRefresh()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Smazat", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
                
                if (bookmark.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        bookmark.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(tag.name) }
                            )
                        }
                    }
                }
                
                if (!bookmark.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = bookmark.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(spacing.small))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = bookmark.domain,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
