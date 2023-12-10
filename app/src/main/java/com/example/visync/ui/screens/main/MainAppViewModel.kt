package com.example.visync.ui.screens.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.example.visync.R
import com.example.visync.data.user.generateNickname
import com.example.visync.ui.screens.player.VisyncPhysicalDevice
import com.example.visync.ui.screens.settings.getProfilePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    val physicalDevicePlaceholder = VisyncPhysicalDevice.NoDimensionsDevice

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

        val physicalDeviceKey = context.getString(R.string.prefs_profile_physical_device)
        val physicalDeviceString = profilePrefs.getString(physicalDeviceKey, null)
        if (physicalDeviceString.isNullOrEmpty()) {
            val device = _editablePhysicalDevice.value
            profilePrefs
                .edit()
                .putString(physicalDeviceKey,  Json.encodeToString(device))
                .apply()
        } else {
            try {
                _editablePhysicalDevice = _editablePhysicalDevice.copy(
                    value = Json.decodeFromString(physicalDeviceString)
                )
            } catch (e: Exception) {
                Log.w("Preferences", "Failed to decode physical device string", e)
            }
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
        val physicalDeviceKey = context.getString(R.string.prefs_profile_physical_device)
        val device = _editablePhysicalDevice.value
        profilePrefs
            .edit()
            .putString(physicalDeviceKey, Json.encodeToString(device))
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

tailrec fun Context.findActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.findActivity()
        else -> throw IllegalArgumentException("Could not find activity!")
    }

fun getAvailableWindowSize(context: Context): Size {
    val activity: Activity = try {
        context.findActivity()
    } catch (e: Exception) {
        // no activity probably means it is called from @Preview
        return Size(0, 0)
    }
    return if (Build.VERSION.SDK_INT >= 30) {
        val wMetrics = activity.windowManager.currentWindowMetrics
        val bounds = wMetrics.bounds
        Size(bounds.width(), bounds.height())
    } else {
        val display = activity.windowManager.defaultDisplay
        val realSize = Point()
        display.getRealSize(realSize)
        Size(realSize.x, realSize.y)
    }
}

fun getDeviceRotation(context: Context): Int {
    val activity: Activity = try {
        context.findActivity()
    } catch (e: Exception) {
        // no activity probably means it is called from @Preview
        return 0
    }
    val rotationCode = when {
        Build.VERSION.SDK_INT >= 30 -> context.display!!.rotation
        else -> activity.windowManager.defaultDisplay.rotation
    }
    return when (rotationCode) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

fun getAvailableDrawingSize(context: Context): Size {
    val displayMetrics = context.resources.displayMetrics
    return Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
}