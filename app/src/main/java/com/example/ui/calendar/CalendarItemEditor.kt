package com.example.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.theme.LocalCortexSpacing
import com.example.domain.model.CalendarItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

import com.example.domain.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarItemEditor(
    item: CalendarItem?,
    initialDate: LocalDate,
    availableNotes: List<Note>,
    onSave: (CalendarItem) -> Unit,
    onDelete: (CalendarItem) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalCortexSpacing.current
    var isTask by remember { mutableStateOf(item?.isTask ?: false) }
    var title by remember { mutableStateOf(item?.title ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var selectedNoteId by remember { mutableStateOf(item?.noteId) }
    var noteDropdownExpanded by remember { mutableStateOf(false) }
    
    // Simplification for time selection (could use TimePickerDialog)
    val defaultTime = LocalTime.now().plusHours(1).withMinute(0)
    var startTimeStr by remember { 
        val initTime = if (item != null) {
            java.time.Instant.ofEpochMilli(item.startAt).atZone(ZoneId.systemDefault()).toLocalTime()
        } else defaultTime
        mutableStateOf(String.format(java.util.Locale.US, "%02d:%02d", initTime.hour, initTime.minute)) 
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(spacing.medium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text("Zrušit") }
                    TextButton(
                        onClick = {
                            try {
                                val parts = startTimeStr.split(":")
                                val time = LocalTime.of(parts[0].toInt(), parts[1].toInt())
                                val startAt = initialDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                
                                val savedItem = CalendarItem(
                                    id = item?.id ?: UUID.randomUUID().toString(),
                                    title = title.takeIf { it.isNotBlank() } ?: "Bez názvu",
                                    description = description.takeIf { it.isNotBlank() },
                                    startAt = startAt,
                                    endAt = startAt + 3600000, // +1h
                                    isTask = isTask,
                                    isDone = item?.isDone ?: false,
                                    reminderAt = startAt, // default reminder at start time
                                    noteId = selectedNoteId
                                )
                                onSave(savedItem)
                            } catch (e: Exception) {
                                // Ignore format error for now
                            }
                        },
                        enabled = title.isNotBlank() && startTimeStr.matches(Regex("\\d{1,2}:\\d{2}"))
                    ) { Text("Uložit") }
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    SegmentedButton(
                        selected = !isTask,
                        onClick = { isTask = false },
                        text = "Událost"
                    )
                    SegmentedButton(
                        selected = isTask,
                        onClick = { isTask = true },
                        text = "Úkol"
                    )
                }

                Spacer(modifier = Modifier.height(spacing.medium))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Název") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(spacing.small))

                OutlinedTextField(
                    value = startTimeStr,
                    onValueChange = { startTimeStr = it },
                    label = { Text("Čas (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(spacing.small))

                ExposedDropdownMenuBox(
                    expanded = noteDropdownExpanded,
                    onExpandedChange = { noteDropdownExpanded = !noteDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = availableNotes.find { it.id == selectedNoteId }?.title ?: "Žádná",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Propojená poznámka") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = noteDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = noteDropdownExpanded,
                        onDismissRequest = { noteDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Žádná") },
                            onClick = { 
                                selectedNoteId = null
                                noteDropdownExpanded = false
                            }
                        )
                        availableNotes.forEach { note ->
                            DropdownMenuItem(
                                text = { Text(note.title) },
                                onClick = {
                                    selectedNoteId = note.id
                                    noteDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.small))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )

                if (item != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onDelete(item) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Smazat")
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    text: String
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text)
    }
}
