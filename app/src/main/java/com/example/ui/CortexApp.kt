package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.navigation.CalendarDestination
import com.example.core.navigation.LinksDestination
import com.example.core.navigation.NotesDestination
import com.example.core.navigation.SettingsDestination
import com.example.ui.calendar.CalendarScreen
import com.example.ui.links.LinksScreen
import com.example.ui.notes.NotesScreen
import com.example.core.theme.LocalAppConfig
import com.example.core.navigation.OnboardingDestination
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.settings.SettingsScreen
import androidx.compose.runtime.LaunchedEffect

import com.example.ui.notes.detail.NoteDetailScreen
import com.example.core.navigation.NoteDetailDestination

@Composable
fun CortexApp() {
    val navController = rememberNavController()
    val appConfig = LocalAppConfig.current
    val hasVault = appConfig.vaultUri != null
    val isOnboardingCompleted = appConfig.isOnboardingCompleted

    // We can observe the nav stack to determine if we should show the bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Check if the current route is not Onboarding
    val showBottomBar = currentDestination?.hierarchy?.any { 
        val r = it.route ?: ""
        r.contains(OnboardingDestination::class.simpleName ?: "") ||
        r.contains(NoteDetailDestination::class.simpleName ?: "")
    } != true && isOnboardingCompleted

    // When hasVault changes to true, navigate to Notes (if we are on onboarding)
    LaunchedEffect(isOnboardingCompleted) {
        if (isOnboardingCompleted) {
            val isOnboarding = navController.currentBackStackEntry?.destination?.route?.contains(OnboardingDestination::class.simpleName ?: "") == true
            if (isOnboarding || navController.currentBackStackEntry == null) {
                 navController.navigate(NotesDestination) {
                     popUpTo(0) { inclusive = true }
                 }
            }
        } else {
             navController.navigate(OnboardingDestination) {
                 popUpTo(0) { inclusive = true }
             }
        }
    }

    val startDestination = remember(isOnboardingCompleted, appConfig.startScreen) {
        if (!isOnboardingCompleted) {
            OnboardingDestination
        } else {
            when (appConfig.startScreen) {
                "notes" -> NotesDestination
                "links" -> LinksDestination
                "calendar" -> CalendarDestination
                "settings" -> SettingsDestination
                else -> NotesDestination
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val allItems = mapOf(
                        "notes" to Triple(NotesDestination, stringResource(R.string.nav_notes), Icons.Default.Edit),
                        "links" to Triple(LinksDestination, stringResource(R.string.nav_links), Icons.Default.Link),
                        "calendar" to Triple(CalendarDestination, stringResource(R.string.nav_calendar), Icons.Default.DateRange),
                        "settings" to Triple(SettingsDestination, stringResource(R.string.nav_settings), Icons.Default.Settings)
                    )
                    val items = appConfig.bottomNavOrder.mapNotNull { allItems[it] }

                    items.forEach { (destination, label, icon) ->
                        val isSelected = currentDestination?.hierarchy?.any { 
                            it.route?.contains(destination::class.simpleName ?: "") == true 
                        } == true
                        
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(destination) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<OnboardingDestination> { OnboardingScreen() }
            composable<NotesDestination> { 
                NotesScreen(
                    onNavigateToNote = { noteId ->
                        navController.navigate(NoteDetailDestination(noteId))
                    }
                ) 
            }
            composable<NoteDetailDestination> {
                NoteDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<LinksDestination> { LinksScreen() }
            composable<CalendarDestination> { CalendarScreen() }
            composable<SettingsDestination> { SettingsScreen() }
        }
    }
}
