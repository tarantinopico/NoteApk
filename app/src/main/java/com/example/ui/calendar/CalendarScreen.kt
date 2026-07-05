package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.core.theme.LocalCortexSpacing
import com.example.ui.components.CortexEmptyState
import com.example.domain.model.CalendarItem
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val spacing = LocalCortexSpacing.current

    if (uiState.isEditorVisible) {
        CalendarItemEditor(
            item = uiState.itemToEdit,
            initialDate = uiState.selectedDate,
            availableNotes = uiState.availableNotes,
            onSave = { viewModel.onEvent(CalendarEvent.OnSaveItem(it)) },
            onDelete = { viewModel.onEvent(CalendarEvent.OnDeleteItem(it)) },
            onDismiss = { viewModel.onEvent(CalendarEvent.OnEditorDismiss) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(CalendarEvent.OnEditItem(null)) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Přidat událost")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: Month/Year selector and View mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.onEvent(CalendarEvent.OnDateSelected(uiState.selectedDate.minusMonths(1))) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Předchozí měsíc")
                    }
                    Text(
                        text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = { viewModel.onEvent(CalendarEvent.OnDateSelected(uiState.selectedDate.plusMonths(1))) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Další měsíc")
                    }
                }
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ViewAgenda, contentDescription = "Změnit zobrazení")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Měsíc") },
                            onClick = { 
                                viewModel.onEvent(CalendarEvent.OnViewModeChanged(CalendarViewMode.MONTH))
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Agenda") },
                            onClick = { 
                                viewModel.onEvent(CalendarEvent.OnViewModeChanged(CalendarViewMode.AGENDA))
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.viewMode) {
                    CalendarViewMode.MONTH -> {
                        MonthView(
                            selectedDate = uiState.selectedDate,
                            items = uiState.items,
                            firstDayOfWeek = uiState.firstDayOfWeek,
                            onDateSelected = { viewModel.onEvent(CalendarEvent.OnDateSelected(it)) }
                        )
                        HorizontalDivider()
                        // Daily Agenda
                        val dailyItems = uiState.items.filter { 
                            val itemDate = java.time.Instant.ofEpochMilli(it.startAt).atZone(ZoneId.systemDefault()).toLocalDate()
                            itemDate == uiState.selectedDate
                        }
                        if (dailyItems.isEmpty()) {
                            CortexEmptyState(
                                title = "Žádné události",
                                message = "Tento den je bez plánů.",
                                icon = Icons.Default.EventBusy,
                                modifier = Modifier.fillMaxWidth().padding(top = spacing.large)
                            )
                        } else {
                            LazyColumn(contentPadding = PaddingValues(spacing.medium)) {
                                items(dailyItems) { item ->
                                    CalendarItemRow(
                                        item = item,
                                        onToggleDone = { viewModel.onEvent(CalendarEvent.OnToggleTaskDone(item)) },
                                        onClick = { viewModel.onEvent(CalendarEvent.OnEditItem(item)) }
                                    )
                                }
                            }
                        }
                    }
                    CalendarViewMode.AGENDA -> {
                        // Agenda View
                        if (uiState.items.isEmpty()) {
                            CortexEmptyState(
                                title = "Kalendář je prázdný",
                                message = "Nemáte naplánovány žádné události.",
                                icon = Icons.Default.EventNote,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(contentPadding = PaddingValues(spacing.medium)) {
                                items(uiState.items) { item ->
                                    CalendarItemRow(
                                        item = item,
                                        onToggleDone = { viewModel.onEvent(CalendarEvent.OnToggleTaskDone(item)) },
                                        onClick = { viewModel.onEvent(CalendarEvent.OnEditItem(item)) }
                                    )
                                }
                            }
                        }
                    }
                    CalendarViewMode.WEEK -> {
                        // Week view placeholder (fallback to Agenda)
                        Text("Týdenní zobrazení v přípravě")
                    }
                }
            }
        }
    }
}

@Composable
fun MonthView(
    selectedDate: LocalDate,
    items: List<CalendarItem>,
    firstDayOfWeek: java.time.DayOfWeek,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = selectedDate.lengthOfMonth()
    val firstDayOfMonth = selectedDate.with(TemporalAdjusters.firstDayOfMonth())
    val startOffset = (firstDayOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val spacing = LocalCortexSpacing.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.medium)) {
        // Day names row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            val days = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")
            val orderedDays = if (firstDayOfWeek == java.time.DayOfWeek.SUNDAY) listOf("Ne", "Po", "Út", "St", "Čt", "Pá", "So") else days
            orderedDays.forEach { day ->
                Text(text = day, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(spacing.small))

        var currentDay = 1
        for (week in 0..5) {
            if (currentDay > daysInMonth) break
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                for (dayOfWeek in 0..6) {
                    if (week == 0 && dayOfWeek < startOffset || currentDay > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = selectedDate.withDayOfMonth(currentDay)
                        val isSelected = date == selectedDate
                        val dayItems = items.filter { 
                            java.time.Instant.ofEpochMilli(it.startAt).atZone(ZoneId.systemDefault()).toLocalDate() == date 
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentDay.toString(),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (dayItems.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        dayItems.take(3).forEach {
                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                        }
                                    }
                                }
                            }
                        }
                        currentDay++
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarItemRow(
    item: CalendarItem,
    onToggleDone: (CalendarItem) -> Unit,
    onClick: () -> Unit
) {
    val spacing = LocalCortexSpacing.current
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val time = java.time.Instant.ofEpochMilli(item.startAt).atZone(ZoneId.systemDefault()).format(timeFormatter)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small).clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isTask) {
                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = { onToggleDone(item) }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.isTask && item.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Text(text = time, style = MaterialTheme.typography.labelLarge)
        }
    }
}
