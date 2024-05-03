package com.example.nfccontrol

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// data key(s)
val NFC_DATA_KEY = stringPreferencesKey("nfc_data_key")

// data storage
val Context.localDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class LocalDataStorage(private val context: Context) {

    suspend fun saveStringData(key: Preferences.Key<String>, value: String) {
        // Save the string data to the local storage
        if(value.isNotEmpty()) {
            context.localDataStore.edit { settings ->
                settings[key] = value
            }
        }
    }

    suspend fun getStringData(key: Preferences.Key<String>): String {
        // Get the string data from the local storage
        val strDataFlow: Flow<String> = context.localDataStore.data
            .map { preferences ->
                preferences[key] ?: ""
            }
        return strDataFlow.first()
    }
}