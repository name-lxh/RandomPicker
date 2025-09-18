package com.lxh.randompicker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object Keys {
    val DEFAULT_TABLE_ID = longPreferencesKey("default_table_id")
    val NO_REPEAT = booleanPreferencesKey("no_repeat")
}

class SettingsStore(private val context: Context) {

    val defaultTableIdFlow: Flow<Long?> =
        context.dataStore.data.map { prefs -> prefs[Keys.DEFAULT_TABLE_ID] }

    val noRepeatFlow: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.NO_REPEAT] ?: true } // 默认开启不重复

    suspend fun setDefaultTableId(id: Long) {
        context.dataStore.edit { it[Keys.DEFAULT_TABLE_ID] = id }
    }

    suspend fun setNoRepeat(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NO_REPEAT] = enabled }
    }

    suspend fun clearDefaultTableId() {
        context.dataStore.edit { it.remove(Keys.DEFAULT_TABLE_ID) }
    }

}
