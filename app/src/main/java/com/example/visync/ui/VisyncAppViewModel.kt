package com.example.visync.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VisyncAppViewModel @Inject constructor(

) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VisyncAppUiState(
            showNavigation = true,
        )
    )
    val uiState: StateFlow<VisyncAppUiState> = _uiState

    init {

    }

    fun hideNavigation() {
        _uiState.value = _uiState.value.copy(
            showNavigation = false,
        )
    }

    fun showNavigation() {
        _uiState.value = _uiState.value.copy(
            showNavigation = true,
        )
    }
}

data class VisyncAppUiState(
    val showNavigation: Boolean,
)