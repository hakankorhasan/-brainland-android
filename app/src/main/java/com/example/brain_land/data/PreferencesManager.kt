package com.example.brain_land.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "brainland_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("onboarding_hasSeen")
        val KEY_HAS_PROFILE = booleanPreferencesKey("has_profile")
        val KEY_NICKNAME = stringPreferencesKey("nickname")
        val KEY_AVATAR_URL = stringPreferencesKey("avatar_url")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_HAS_SEEN_ONBOARDING] ?: false }

    val hasProfile: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_HAS_PROFILE] ?: false }

    val nickname: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_NICKNAME] ?: "" }

    val avatarUrl: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_AVATAR_URL] ?: "" }

    val deviceId: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_DEVICE_ID] ?: "" }

    suspend fun markOnboardingSeen() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_ONBOARDING] = true
        }
    }

    suspend fun saveProfile(nickname: String, avatarUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_PROFILE] = true
            prefs[KEY_NICKNAME] = nickname
            prefs[KEY_AVATAR_URL] = avatarUrl
        }
    }

    suspend fun saveDeviceId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEVICE_ID] = id
        }
    }
}
