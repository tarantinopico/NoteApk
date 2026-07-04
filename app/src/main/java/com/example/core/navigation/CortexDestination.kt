package com.example.core.navigation

import kotlinx.serialization.Serializable

sealed interface CortexDestination

@Serializable
data object OnboardingDestination : CortexDestination

@Serializable
data class NoteDetailDestination(val noteId: String = "new") : CortexDestination

@Serializable
data object NotesDestination : CortexDestination

@Serializable
data object LinksDestination : CortexDestination

@Serializable
data object CalendarDestination : CortexDestination

@Serializable
data object SettingsDestination : CortexDestination
