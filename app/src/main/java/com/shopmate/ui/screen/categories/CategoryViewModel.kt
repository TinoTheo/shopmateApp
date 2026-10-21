package com.shopmate.ui.screen.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shopmate.domain.model.Category
import com.shopmate.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class CategoryUiState {
    object Idle : CategoryUiState()
    object Loading : CategoryUiState()
    object Success : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Idle)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun addCategory(name: String, colour: String) = viewModelScope.launch {
        _uiState.value = CategoryUiState.Loading
        runCatching { categoryRepository.add(Category(UUID.randomUUID().toString(), name, colour)) }
            .onSuccess { _uiState.value = CategoryUiState.Success }
            .onFailure { _uiState.value = CategoryUiState.Error(it.message ?: "Error") }
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        _uiState.value = CategoryUiState.Loading
        runCatching { categoryRepository.update(category) }
            .onSuccess { _uiState.value = CategoryUiState.Success }
            .onFailure { _uiState.value = CategoryUiState.Error(it.message ?: "Error") }
    }

    fun deleteCategory(id: String) = viewModelScope.launch {
        runCatching { categoryRepository.delete(id) }
            .onFailure { _uiState.value = CategoryUiState.Error(it.message ?: "Error") }
    }

    fun resetState() { _uiState.value = CategoryUiState.Idle }
}
