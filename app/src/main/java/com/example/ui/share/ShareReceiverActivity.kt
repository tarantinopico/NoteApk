package com.example.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.theme.CortexTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        setContent {
            CortexTheme(appConfig = com.example.core.config.AppConfig()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (uiState.isSaved) {
                            Text("Odkaz uložen", style = MaterialTheme.typography.titleLarge)
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(1000)
                                finish()
                            }
                        } else if (uiState.isLoading) {
                            CircularProgressIndicator()
                        } else if (uiState.error != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Chyba: ${uiState.error}")
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { finish() }) {
                                    Text("Zavřít")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                viewModel.saveSharedLink(sharedText)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }
}
