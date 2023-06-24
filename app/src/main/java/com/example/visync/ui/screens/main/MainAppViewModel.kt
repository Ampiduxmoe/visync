package com.example.visync.ui.screens.main

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.visync.R
import com.example.visync.data.user.generateNickname
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

    private val _mainAppNavigationUiState = MutableStateFlow(
        MainAppNavigationUiState(
            editableUsername = EditableUsername(
                value = "Loading...",
                isEditable = false,
                enableEditing = this::enableUsernameEditing,
                disableEditing = this::disableUsernameEditing,
                setValue = this::setUsername,
                applyChanges = this::applyUsernameChanges
            )
        )
    )
    val mainAppNavigationUiState: StateFlow<MainAppNavigationUiState> = _mainAppNavigationUiState

    init {

    }

    fun initializeNavigationUiState(context: Context) {
        val packageName = context.packageName
        val profilePrefsKey = context.getString(R.string.prefs_profile_file_key)
        val profilePrefs: SharedPreferences = context.getSharedPreferences(
            "$packageName.$profilePrefsKey",
            Context.MODE_PRIVATE
        )
        val usernameKey = context.getString(R.string.prefs_profile_username)
        val username = profilePrefs.getString(usernameKey, null) ?: generateNickname()
        updateUsernameState(
            editableUsername = getUsernameState().copy(
                value = username
            )
        )
        profilePrefs.edit().putString(usernameKey, username).apply()
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

    fun enableUsernameEditing() {
        updateUsernameState(
            editableUsername = getUsernameState().copy(
                isEditable = true
            )
        )
    }

    fun disableUsernameEditing() {
        updateUsernameState(
            editableUsername = getUsernameState().copy(
                isEditable = true
            )
        )
    }

    fun setUsername(username: String) {
        updateUsernameState(
            editableUsername = getUsernameState().copy(
                value = username
            )
        )
    }

    fun applyUsernameChanges(context: Context) {
        val packageName = context.packageName
        val profilePrefsKey = context.getString(R.string.prefs_profile_file_key)
        val profilePrefs: SharedPreferences = context.getSharedPreferences(
            "$packageName.$profilePrefsKey",
            Context.MODE_PRIVATE
        )
        val usernameKey = context.getString(R.string.prefs_profile_username)
        val username = getUsernameState().value
        profilePrefs.edit().putString(usernameKey, username).apply()
    }

    private fun getUsernameState(): EditableUsername {
        return _mainAppNavigationUiState.value.editableUsername
    }

    private fun updateUsernameState(editableUsername: EditableUsername) {
        _mainAppNavigationUiState.value = _mainAppNavigationUiState.value.copy(
            editableUsername = editableUsername
        )
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