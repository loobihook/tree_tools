package me.rpgz.treetools.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.rpgz.treetools.models.PlanetInferModel
import me.rpgz.treetools.preferences.UserPreferences
import javax.inject.Inject

@HiltViewModel
class SettingPageViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    
    private val _dashscopeApiKey = MutableStateFlow("")
    val dashscopeApiKey: StateFlow<String> = _dashscopeApiKey.asStateFlow()
    
    private val _isApiKeyEditable = MutableStateFlow(false)
    val isApiKeyEditable: StateFlow<Boolean> = _isApiKeyEditable.asStateFlow()
    
    private val _planetInferModel = MutableStateFlow(PlanetInferModel.THIRD_PARTY_API)
    val planetInferModel: StateFlow<PlanetInferModel> = _planetInferModel.asStateFlow()
    
    init {
        // Load current API key
        viewModelScope.launch {
            userPreferences.dashscopeApiKey.collect { apiKey ->
                _dashscopeApiKey.value = apiKey
            }
        }
        
        // Load current planet infer model
        viewModelScope.launch {
            userPreferences.planetInferModel.collect { model ->
                _planetInferModel.value = model
            }
        }
    }
    
    fun updateDashscopeApiKey(apiKey: String) {
        _dashscopeApiKey.value = apiKey
    }
    
    fun toggleApiKeyEditable() {
        _isApiKeyEditable.value = !_isApiKeyEditable.value
    }
    
    fun saveDashscopeApiKey() {
        viewModelScope.launch {
            userPreferences.setDashscopeApiKey(_dashscopeApiKey.value)
            _isApiKeyEditable.value = false // Exit edit mode after saving
        }
    }
    
    fun cancelApiKeyEdit() {
        viewModelScope.launch {
            // Reload the original value from preferences
            userPreferences.dashscopeApiKey.collect { apiKey ->
                _dashscopeApiKey.value = apiKey
                _isApiKeyEditable.value = false
                return@collect // Only take the first value
            }
        }
    }
    
    fun updatePlanetInferModel(model: PlanetInferModel) {
        viewModelScope.launch {
            userPreferences.setPlanetInferModel(model)
            _planetInferModel.value = model
        }
    }
}