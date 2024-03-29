package com.shirosoftware.sealprogrammingmobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsDataStore @Inject constructor(val context: Context) {
    val threshold: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_THRESHOLD] ?: DEFAULT_THRESHOLD
        }

    suspend fun updateThreshold(value: Float) {
        context.dataStore.edit { settings ->
            settings[KEY_THRESHOLD] = value
        }
    }

    val showScore: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_SCORE] ?: false
        }

    suspend fun updateShowScore(value: Boolean) {
        context.dataStore.edit { settings ->
            settings[KEY_SHOW_SCORE] = value
        }
    }


    val lastAddress: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_ADDRESS] ?: ""
        }

    suspend fun updateLastAddress(value: String) {
        context.dataStore.edit { settings ->
            settings[KEY_LAST_ADDRESS] = value
        }
    }

    private companion object {
        val KEY_THRESHOLD = floatPreferencesKey("threshold")
        const val DEFAULT_THRESHOLD = 0.4f

        val KEY_SHOW_SCORE = booleanPreferencesKey("show_score")

        val KEY_LAST_ADDRESS = stringPreferencesKey("last_address")
    }
}
