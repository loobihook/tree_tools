package me.rpgz.treetools.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.rpgz.treetools.models.PlanetInferModel
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val DASHSCOPE_API_KEY = stringPreferencesKey("dashscope_api_key")
        private val PLANET_INFER_MODEL = stringPreferencesKey("planet_infer_model")
    }
    
    val dashscopeApiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[DASHSCOPE_API_KEY] ?: ""
    }
    
    val planetInferModel: Flow<PlanetInferModel> = dataStore.data.map { preferences ->
        val value = preferences[PLANET_INFER_MODEL] ?: PlanetInferModel.THIRD_PARTY_API.value
        PlanetInferModel.fromValue(value)
    }
    
    suspend fun setDashscopeApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[DASHSCOPE_API_KEY] = apiKey
        }
    }
    
    suspend fun setPlanetInferModel(model: PlanetInferModel) {
        dataStore.edit { preferences ->
            preferences[PLANET_INFER_MODEL] = model.value
        }
    }
}