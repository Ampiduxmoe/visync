package com.example.visync.ui.screens.main

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.visync.R
import com.example.visync.data.user.generateNickname
import com.example.visync.ui.screens.player.VisyncPhysicalDevice
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
    val physicalDevicePlaceholder = VisyncPhysicalDevice(
        mmDeviceWidth = 0f,
        mmDeviceHeight = 0f,
        mmDisplayWidth = 0f,
        mmDisplayHeight = 0f,
        pxDisplayWidth = 0f,
        pxDisplayHeight = 0f
    )

    private val _mainAppNavigationUiState = MutableStateFlow(
        MainAppNavigationUiState(
            editableUsername = EditableUsername(
                value = usernamePlaceholder,
                isEditable = false,
                enableEditing = this::enableUsernameEditing,
                disableEditing = this::disableUsernameEditing,
                setValue = this::setUsername,
                applyChanges = this::applyUsernameChanges
            ),
            editablePhysicalDevice = EditablePhysicalDevice(
                value = physicalDevicePlaceholder,
                isEditable = false,
                enableEditing = this::enablePhysicalDeviceEditing,
                disableEditing = this::disablePhysicalDeviceEditing,
                setValue = this::setPhysicalDevice,
                applyChanges = this::applyPhysicalDeviceChanges
            )
        )
    )
    val mainAppNavigationUiState: StateFlow<MainAppNavigationUiState> = _mainAppNavigationUiState

    private var _editableUsername
        get() = _mainAppNavigationUiState.value.editableUsername
        set(value) = _mainAppNavigationUiState.update { it.copy(editableUsername = value) }
    private var _editablePhysicalDevice
        get() = _mainAppNavigationUiState.value.editablePhysicalDevice
        set(value) = _mainAppNavigationUiState.update { it.copy(editablePhysicalDevice = value) }

    init {

    }

    fun initializeNavigationUiState(context: Context) {
        val profilePrefs = getProfilePreferences(context)

        val usernameKey = context.getString(R.string.prefs_profile_username)
        val username = profilePrefs.getString(usernameKey, null) ?: generateNickname()
        _editableUsername = _editableUsername.copy(value = username)
        profilePrefs.edit().putString(usernameKey, username).apply()

        val displayWidthPxKey = context.getString(R.string.prefs_profile_display_width_px)
        val displayHeightPxKey = context.getString(R.string.prefs_profile_display_height_px)
        val displayWidthMmKey = context.getString(R.string.prefs_profile_display_width_mm)
        val displayHeightMmKey = context.getString(R.string.prefs_profile_display_height_mm)
        val deviceWidthMmKey = context.getString(R.string.prefs_profile_device_width_mm)
        val deviceHeightMmKey = context.getString(R.string.prefs_profile_device_height_mm)

        val displayWidthPx = profilePrefs.getFloat(displayWidthPxKey, -1f)

        if (displayWidthPx == -1f) {
            val device = _editablePhysicalDevice.value
            profilePrefs
                .edit()
                .putFloat(displayWidthPxKey, physicalDevicePlaceholder.pxDisplayWidth)
                .putFloat(displayHeightPxKey, physicalDevicePlaceholder.pxDisplayHeight)
                .putFloat(displayWidthMmKey, physicalDevicePlaceholder.mmDisplayWidth)
                .putFloat(displayHeightMmKey, physicalDevicePlaceholder.mmDisplayHeight)
                .putFloat(deviceWidthMmKey, physicalDevicePlaceholder.mmDeviceWidth)
                .putFloat(deviceHeightMmKey, physicalDevicePlaceholder.mmDeviceHeight)
                .apply()
        } else {
            val displayHeightPx = profilePrefs.getFloat(displayHeightPxKey, -1f)
            val displayWidthMm = profilePrefs.getFloat(displayWidthMmKey, -1f)
            val displayHeightMm = profilePrefs.getFloat(displayHeightMmKey, -1f)
            val deviceWidthMm = profilePrefs.getFloat(deviceWidthMmKey, -1f)
            val deviceHeightMm = profilePrefs.getFloat(deviceHeightMmKey, -1f)
            _editablePhysicalDevice = _editablePhysicalDevice.copy(
                value = VisyncPhysicalDevice(
                    mmDeviceWidth = deviceWidthMm,
                    mmDeviceHeight = deviceHeightMm,
                    mmDisplayWidth = displayWidthMm,
                    mmDisplayHeight = displayHeightMm,
                    pxDisplayWidth = displayWidthPx,
                    pxDisplayHeight = displayHeightPx
                )
            )
        }
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

    fun enablePhysicalDeviceEditing() {
        _editablePhysicalDevice = _editablePhysicalDevice.copy(isEditable = true)
    }

    fun disablePhysicalDeviceEditing() {
        _editablePhysicalDevice = _editablePhysicalDevice.copy(isEditable = true)
    }

    fun setPhysicalDevice(device: VisyncPhysicalDevice) {
        _editablePhysicalDevice = _editablePhysicalDevice.copy(value = device)
    }

    fun applyPhysicalDeviceChanges(context: Context) {
        val profilePrefs = getProfilePreferences(context)
        val displayWidthPxKey = context.getString(R.string.prefs_profile_display_width_px)
        val displayHeightPxKey = context.getString(R.string.prefs_profile_display_height_px)
        val displayWidthMmKey = context.getString(R.string.prefs_profile_display_width_mm)
        val displayHeightMmKey = context.getString(R.string.prefs_profile_display_height_mm)
        val deviceWidthMmKey = context.getString(R.string.prefs_profile_device_width_mm)
        val deviceHeightMmKey = context.getString(R.string.prefs_profile_device_height_mm)
        val device = _editablePhysicalDevice.value

        profilePrefs
            .edit()
            .putFloat(displayWidthPxKey, device.pxDisplayWidth)
            .putFloat(displayHeightPxKey, device.pxDisplayHeight)
            .putFloat(displayWidthMmKey, device.mmDisplayWidth)
            .putFloat(displayHeightMmKey, device.mmDisplayHeight)
            .putFloat(deviceWidthMmKey, device.mmDeviceWidth)
            .putFloat(deviceHeightMmKey, device.mmDeviceHeight)
            .apply()
    }
}

data class MainAppUiState(
    val isNavigationVisible: Boolean,
)

data class MainAppNavigationUiState(
    val editableUsername: EditableUsername,
    val editablePhysicalDevice: EditablePhysicalDevice
)

data class EditableUsername(
    val value: String,
    val isEditable: Boolean,
    val enableEditing: () -> Unit,
    val disableEditing: () -> Unit,
    val setValue: (String) -> Unit,
    val applyChanges: (Context) -> Unit,
)

data class EditablePhysicalDevice(
    val value: VisyncPhysicalDevice,
    val isEditable: Boolean,
    val enableEditing: () -> Unit,
    val disableEditing: () -> Unit,
    val setValue: (VisyncPhysicalDevice) -> Unit,
    val applyChanges: (Context) -> Unit,
)