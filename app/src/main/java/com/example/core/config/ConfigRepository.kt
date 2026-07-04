package com.example.core.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface ConfigRepository {
    val appConfig: Flow<AppConfig>
    suspend fun updateConfig(update: (AppConfig) -> AppConfig)
    suspend fun resetConfig()
}

class ConfigRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ConfigRepository {
    
    private val configKey = stringPreferencesKey("app_config")
    
    private val json = Json { ignoreUnknownKeys = true }

    override val appConfig: Flow<AppConfig> = dataStore.data.map { prefs ->
        val jsonString = prefs[configKey]
        if (jsonString != null) {
            try {
                json.decodeFromString<AppConfig>(jsonString)
            } catch (e: Exception) {
                AppConfig()
            }
        } else {
            AppConfig()
        }
    }

    override suspend fun updateConfig(update: (AppConfig) -> AppConfig) {
        dataStore.edit { prefs ->
            val currentJson = prefs[configKey]
            val currentConfig = if (currentJson != null) {
                try {
                    json.decodeFromString<AppConfig>(currentJson)
                } catch (e: Exception) {
                    AppConfig()
                }
            } else {
                AppConfig()
            }
            val newConfig = update(currentConfig)
            prefs[configKey] = json.encodeToString(newConfig)
        }
    }

    override suspend fun resetConfig() {
        dataStore.edit { prefs ->
            prefs[configKey] = json.encodeToString(AppConfig())
        }
    }
}
