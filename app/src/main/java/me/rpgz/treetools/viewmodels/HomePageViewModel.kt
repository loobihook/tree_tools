package me.rpgz.treetools.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.rpgz.treetools.db.entities.TreeAnalysisRecordEntity
import me.rpgz.treetools.repositories.TreeAnalysisReportRepository
import javax.inject.Inject

data class HomePageUiState(
    val records: List<TreeAnalysisRecordEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMorePages: Boolean = true,
    val currentPage: Int = 0,
    val searchQuery: String = "",
    val error: String? = null
)

@HiltViewModel
class HomePageViewModel @Inject constructor(
    private val repository: TreeAnalysisReportRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomePageUiState())
    val uiState: StateFlow<HomePageUiState> = _uiState.asStateFlow()
    
    private val pageSize = 5
    private var currentFlow: Flow<List<TreeAnalysisRecordEntity>>? = null
    private var flowJob: Job? = null
    
    init {
        loadRecords()
    }
    
    fun loadRecords(refresh: Boolean = false) {
        if (refresh) {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                currentPage = 0,
                records = emptyList(),
                hasMorePages = true,
                error = null
            )
            startCollectingRecords()
        } else if (_uiState.value.currentPage == 0) {
            // Initial load
            startCollectingRecords()
        } else {
            // Load next page - we'll handle this differently
            loadNextPageData()
        }
    }
    
    private fun startCollectingRecords() {
        // Cancel previous flow subscription
        flowJob?.cancel()
        
        val flow = if (_uiState.value.searchQuery.isBlank()) {
            repository.getAllRecords()
        } else {
            repository.searchRecordsByName(_uiState.value.searchQuery)
        }
        
        flowJob = viewModelScope.launch {
            flow.collect { allRecords ->
                _uiState.value = _uiState.value.copy(
                    records = allRecords.take(pageSize * (_uiState.value.currentPage.coerceAtLeast(1))),
                    isLoading = false,
                    isRefreshing = false,
                    hasMorePages = allRecords.size > pageSize * (_uiState.value.currentPage.coerceAtLeast(1)),
                    currentPage = if (_uiState.value.currentPage == 0) 1 else _uiState.value.currentPage
                )
            }
        }
    }
    
    private fun loadNextPageData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val currentState = _uiState.value
                val offset = currentState.currentPage * pageSize
                
                val newRecords = if (currentState.searchQuery.isBlank()) {
                    repository.getRecordsPaginated(pageSize, offset).first()
                } else {
                    repository.searchRecordsByNamePaginated(
                        currentState.searchQuery,
                        pageSize,
                        offset
                    ).first()
                }
                
                val updatedRecords = currentState.records + newRecords
                
                _uiState.value = _uiState.value.copy(
                    records = updatedRecords,
                    isLoading = false,
                    hasMorePages = newRecords.size == pageSize,
                    currentPage = currentState.currentPage + 1
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }
    
    fun loadNextPage() {
        if (_uiState.value.hasMorePages && !_uiState.value.isLoading) {
            loadRecords()
        }
    }
    
    fun refresh() {
        loadRecords(refresh = true)
    }
    
    fun searchRecords(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            currentPage = 0,
            records = emptyList(),
            hasMorePages = true,
            isLoading = true
        )
        startCollectingRecords()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(recordId)
                // The reactive flow will automatically update the UI
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除失败: ${e.message}"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        flowJob?.cancel()
    }
}