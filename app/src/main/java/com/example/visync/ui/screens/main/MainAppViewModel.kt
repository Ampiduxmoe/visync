package com.example.visync.ui.screens.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainAppViewModel @Inject constructor(

) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainAppUiState(
            isNavigationVisible = true,
        )
    )
    val uiState: StateFlow<MainAppUiState> = _uiState

    init {

    }

    fun hideNavigation() {
        _uiState.value = _uiState.value.copy(
            isNavigationVisible = false,
        )
    }

    fun showNavigation() {
        _uiState.value = _uiState.value.copy(
            isNavigationVisible = true,
        )
    }
}

data class MainAppUiState(
    val isNavigationVisible: Boolean,
)