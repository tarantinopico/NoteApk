package com.example.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.config.AppConfig
import com.example.core.config.ConfigRepository
import com.example.core.config.ThemeMode
import com.example.core.theme.LocalAppConfig
import com.example.core.theme.LocalCortexSpacing
import com.example.ui.components.CortexButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {
    val appConfig: Flow<AppConfig> = configRepository.appConfig
    
    fun setVaultUri(uri: String) {
        viewModelScope.launch {
            configRepository.updateConfig { it.copy(vaultUri = uri) }
        }
    }
    
    fun updateConfig(update: (AppConfig) -> AppConfig) {
        viewModelScope.launch {
            configRepository.updateConfig(update)
        }
    }
}

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val spacing = LocalCortexSpacing.current
    val appConfig = LocalAppConfig.current
    
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    
    val vaultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.setVaultUri(uri.toString())
            coroutineScope.launch {
                pagerState.animateScrollToPage(2)
            }
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        coroutineScope.launch {
            pagerState.animateScrollToPage(3)
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> WelcomeStep(
                        onNext = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                    )
                    1 -> VaultStep(
                        vaultUri = appConfig.vaultUri,
                        onSelectVault = { vaultLauncher.launch(null) },
                        onNext = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                    )
                    2 -> ThemeStep(
                        appConfig = appConfig,
                        onConfigUpdate = { viewModel.updateConfig(it) },
                        onNext = { coroutineScope.launch { pagerState.animateScrollToPage(3) } }
                    )
                    3 -> PermissionsStep(
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // Not needed on < 33, finish onboarding? 
                                // Actually, finishing onboarding means we are done, but wait, the App needs vault to proceed.
                                // If vault is set, the CortexApp automatically navigates to NotesDestination.
                                // But maybe we want to trigger something else? The user is stuck if they don't have vault, but they did set it in step 1.
                                // If they get here, vault is set, so they can just proceed.
                            }
                        },
                        onFinish = { viewModel.updateConfig { it.copy(isOnboardingCompleted = true) }
                            // If they are on step 3, vault is set. They are ready to go.
                            // But what if they skip vault? We shouldn't let them skip vault.
                        }
                    )
                }
            }
            
            // Indicators
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    val spacing = LocalCortexSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Vítejte v Cortex",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = spacing.medium)
        )
        Text(
            text = "Váš osobní druhý mozek.\nSpojuje poznámky, záložky a kalendář s úkoly do jedné offline aplikace plně pod vaší kontrolou.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = spacing.large)
        )
        CortexButton(onClick = onNext, text = "Začít")
    }
}

@Composable
fun VaultStep(vaultUri: String?, onSelectVault: () -> Unit, onNext: () -> Unit) {
    val spacing = LocalCortexSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Kde budou vaše data?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = spacing.medium)
        )
        Text(
            text = "Cortex ukládá vše lokálně jako Markdown soubory. Vyberte složku (Vault), kam se mají data ukládat.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = spacing.large)
        )
        
        if (vaultUri != null) {
            Text(text = "Zvolená složka: $vaultUri", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = spacing.medium))
        }
        
        CortexButton(onClick = onSelectVault, text = if (vaultUri == null) "Vybrat složku" else "Změnit složku")
        if (vaultUri != null) {
            Spacer(modifier = Modifier.height(spacing.medium))
            TextButton(onClick = onNext) { Text("Pokračovat") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStep(appConfig: AppConfig, onConfigUpdate: ((AppConfig) -> AppConfig) -> Unit, onNext: () -> Unit) {
    val spacing = LocalCortexSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Přizpůsobte si Cortex",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = spacing.medium)
        )
        
        Text("Téma", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = spacing.small))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), modifier = Modifier.padding(bottom = spacing.medium)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = appConfig.themeMode == mode,
                    onClick = { onConfigUpdate { it.copy(themeMode = mode) } },
                    label = { Text(mode.name) }
                )
            }
        }
        
        Text("Akcentní barva", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = spacing.small))
        val colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF9C27B0))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), modifier = Modifier.padding(bottom = spacing.large)) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (Color(appConfig.accentColor) == color) 3.dp else 0.dp,
                            color = if (Color(appConfig.accentColor) == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onConfigUpdate { it.copy(accentColor = color.toArgb().toLong() and 0xFFFFFFFF) } }
                )
            }
        }
        
        CortexButton(onClick = onNext, text = "Pokračovat")
    }
}

@Composable
fun PermissionsStep(onRequestPermission: () -> Unit, onFinish: () -> Unit) {
    val spacing = LocalCortexSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.extraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Zůstaňte v obraze",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = spacing.medium)
        )
        Text(
            text = "Aby vás mohl Cortex upozornit na nadcházející události a úkoly, povolte prosím oznámení.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = spacing.large)
        )
        
        CortexButton(onClick = onRequestPermission, text = "Povolit upozornění")
        Spacer(modifier = Modifier.height(spacing.medium))
        TextButton(onClick = onFinish) { Text("Hotovo (Zavřít)") }
    }
}
