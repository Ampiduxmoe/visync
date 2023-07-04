package com.example.visync.ui.screens.main

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.visync.R
import com.example.visync.data.user.generateNickname
import com.example.visync.ui.screens.settings.getProfilePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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

    val usernamePlaceholder = "Loading..."

    private val _mainAppNavigationUiState = MutableStateFlow(
        MainAppNavigationUiState(
            editableUsername = EditableUsername(
                value = usernamePlaceholder,
                isEditable = false,
                enableEditing = this::enableUsernameEditing,
                disableEditing = this::disableUsernameEditing,
                setValue = this::setUsername,
                applyChanges = this::applyUsernameChanges
            )
        )
    )
    val mainAppNavigationUiState: StateFlow<MainAppNavigationUiState> = _mainAppNavigationUiState

    private var _editableUsername
        get() = _mainAppNavigationUiState.value.editableUsername
        set(value) = _mainAppNavigationUiState.update { it.copy(editableUsername = value) }

    init {

    }

    fun initializeNavigationUiState(context: Context) {
        val profilePrefs = getProfilePreferences(context)
        val usernameKey = context.getString(R.string.prefs_profile_username)
        val username = profilePrefs.getString(usernameKey, null) ?: generateNickname()
        _editableUsername = _editableUsername.copy(value = username)
        profilePrefs.edit().putString(usernameKey, username).apply()
    }

    fun enableUsernameEditing() {
        _editableUsername = _editableUsername.copy(isEditable = true)
    }

    fun disableUsernameEditing() {
        _editableUsername = _editableUsername.copy(isEditable = true)
    }

    fun setUsername(username: String) {
        _editableUsername = _editableUsername.copy(value = username)
    }

    fun applyUsernameChanges(context: Context) {
        val profilePrefs = getProfilePreferences(context)
        val usernameKey = context.getString(R.string.prefs_profile_username)
        val username = _editableUsername.value
        profilePrefs.edit().putString(usernameKey, username).apply()
    }
}

data class MainAppUiState(
    val isNavigationVisible: Boolean,
)

data class MainAppNavigationUiState(
    val editableUsername: EditableUsername,
)

data class EditableUsername(
    val value: String,
    val isEditable: Boolean,
    val enableEditing: () -> Unit,
    val disableEditing: () -> Unit,
    val setValue: (String) -> Unit,
    val applyChanges: (Context) -> Unit,
)