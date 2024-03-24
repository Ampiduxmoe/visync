package com.example.visync.ui.components.navigation

import android.annotation.SuppressLint
import android.util.Log
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.example.visync.R
import com.example.visync.data.user.generateNickname
import com.example.visync.ui.screens.main.EditablePhysicalDevice
import com.example.visync.ui.screens.main.EditableUsername
import com.example.visync.ui.screens.main.getAvailableDrawingSize
import com.example.visync.ui.screens.main.getAvailableWindowSize
import com.example.visync.ui.screens.main.getDeviceRotation
import com.example.visync.ui.screens.player.VisyncPhysicalDevice
import kotlin.math.roundToInt

@Composable
fun ModalNavigationDrawerContent(
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    showMainDestinations: Boolean,
    closeDrawer: () -> Unit,
    editableUsername: EditableUsername,
    editablePhysicalDevice: EditablePhysicalDevice,
) {
    ModalDrawerSheet {
        DrawerSheetContent(
            isDarkTheme = isDarkTheme,
            setDarkTheme = setDarkTheme,
            selectedDestination = selectedDestination,
            navigateToDestination = navigateToDestination,
            scrollState = scrollState,
            showCloseDrawerButton = true,
            closeDrawerButtonClick = closeDrawer,
            showMainDestinations = showMainDestinations,
            editableUsername = editableUsername,
            editablePhysicalDevice = editablePhysicalDevice,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermanentNavigationDrawerContent(
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    showCloseDrawerButton: Boolean,
    closeDrawerButtonClick: () -> Unit,
    editableUsername: EditableUsername,
    editablePhysicalDevice: EditablePhysicalDevice,
    @SuppressLint("ModifierParameter")
    drawerSheetModifier: Modifier = Modifier,
    @SuppressLint("ModifierParameter")
    drawerSheetContentModifier: Modifier = Modifier,
) {
    PermanentDrawerSheet(
        modifier = drawerSheetModifier
    ) {
        DrawerSheetContent(
            isDarkTheme = isDarkTheme,
            setDarkTheme = setDarkTheme,
            selectedDestination = selectedDestination,
            navigateToDestination = navigateToDestination,
            scrollState = scrollState,
            showCloseDrawerButton = showCloseDrawerButton,
            closeDrawerButtonClick = closeDrawerButtonClick,
            showMainDestinations = true,
            editableUsername = editableUsername,
            editablePhysicalDevice = editablePhysicalDevice,
            modifier = drawerSheetContentModifier
        )
    }
}

@Composable
private fun DrawerSheetContent(
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    showCloseDrawerButton: Boolean,
    closeDrawerButtonClick: () -> Unit,
    showMainDestinations: Boolean,
    editableUsername: EditableUsername,
    editablePhysicalDevice: EditablePhysicalDevice,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f)
                .bottomInnerShadow(
                    height = 16.dp,
                    alpha = 0.05f,
                    color = LocalContentColor.current
                )
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            TopAppNameRow(
                showCloseDrawerButton = showCloseDrawerButton,
                closeDrawerButtonClick = closeDrawerButtonClick,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            UserInfo(
                editableUsername = editableUsername,
                editablePhysicalDevice = editablePhysicalDevice,
                modifier = Modifier.fillMaxWidth()
            )
            Divider(modifier = Modifier.padding(16.dp))
            DrawerDestinations(
                selectedDestination = selectedDestination,
                navigateToDestination = navigateToDestination,
                showMainDestinations = true || showMainDestinations,
            )
        }
        BottomSettingsRow(
            navigateToDestination = navigateToDestination,
            isDarkTheme = isDarkTheme,
            setDarkTheme = setDarkTheme,
        )
    }
}

@Composable
private fun DrawerDestinations(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    showMainDestinations: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        if (showMainDestinations) {
            CONNECTION_MODE_DESTINATIONS.forEach { mainDestination ->
                VisyncNavigationDrawerItem(
                    destination = mainDestination,
                    isSelected = selectedDestination == mainDestination.routeString,
                    navigateToDestination = navigateToDestination,
                )
            }
        }
        ACCOUNT_RELATED_DESTINATIONS.forEach { accountDestination ->
            VisyncNavigationDrawerItem(
                destination = accountDestination,
                isSelected = selectedDestination == accountDestination.routeString,
                navigateToDestination = navigateToDestination,
            )
        }
    }
}

@Composable
private fun TopAppNameRow(
    showCloseDrawerButton: Boolean,
    closeDrawerButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = R.string.app_name).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (showCloseDrawerButton) {
            IconButton(onClick = closeDrawerButtonClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(id = R.string.desc_navigation_drawer)
                )
            }
        }
    }
}

@Composable
private fun BottomSettingsRow(
    navigateToDestination: (Route) -> Unit,
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultContentColor = LocalContentColor.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(56.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(CircleShape)
                .clickable {
                    navigateToDestination(Route.AppSettings)
                }
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Route.AppSettings.getImageVectorIcon(),
                contentDescription = stringResource(
                    id = Route.AppSettings.actionDescriptionId
                ),
                tint = defaultContentColor,
            )
            Text(
                text = stringResource(id = Route.AppSettings.actionDescriptionId),
                modifier = Modifier.padding(horizontal = 16.dp),
                color = defaultContentColor,
            )
        }
        if (isDarkTheme) {
            IconButton(
                onClick = { setDarkTheme(false) },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_light_mode),
                    contentDescription = stringResource(id = R.string.desc_light_mode),
                    tint = defaultContentColor,
                )
            }
        } else {
            IconButton(
                onClick = { setDarkTheme(true) },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_dark_mode),
                    contentDescription = stringResource(id = R.string.desc_dark_mode),
                    tint = defaultContentColor,
                )
            }
        }
    }
}

@Composable
private fun UserInfo(
    editableUsername: EditableUsername,
    editablePhysicalDevice: EditablePhysicalDevice,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = CenterHorizontally,
        modifier = modifier
    ) {
        RandomizableUsername(
            editableUsername = editableUsername,
            modifier = Modifier.padding(ButtonDefaults.ContentPadding),
        )
        var showPhysicalDeviceDialog by remember { mutableStateOf(false) }
        PhysicalDeviceButton(
            onClick = { showPhysicalDeviceDialog = true },
            physicalDevice = editablePhysicalDevice.value
        )
        if (showPhysicalDeviceDialog) {
            PhysicalDeviceDialog(
                editablePhysicalDevice = editablePhysicalDevice,
                onDismissRequest = { showPhysicalDeviceDialog = false }
            )
        }
    }
}

@Composable
private fun RandomizableUsername(
    editableUsername: EditableUsername,
    modifier: Modifier = Modifier,
) {
    val textStyle: TextStyle = MaterialTheme.typography.bodyLarge
    val context = LocalContext.current
    val fontSizeDp = with(LocalDensity.current) {
        textStyle.fontSize.toDp()
    }
    val spacing = fontSizeDp / 2
    val iconSize = fontSizeDp * 2
    val randomizeButtonSize = 20.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = stringResource(id = R.string.desc_username),
            modifier = Modifier.size(iconSize),
        )
        BoxWithConstraints {
            val textMaxWidth = maxWidth - spacing * 2 - randomizeButtonSize
            val textScrollState = rememberScrollState()
            Text(
                text = editableUsername.value,
                textAlign = TextAlign.Center,
                style = textStyle,
                maxLines = 1,
                modifier = Modifier
                    .widthIn(0.dp, textMaxWidth)
                    .horizontalScroll(textScrollState)
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_dice),
            contentDescription = stringResource(id = R.string.desc_username_randomize),
            modifier = Modifier
                .size(randomizeButtonSize)
                .clickable {
                    editableUsername.setValue(generateNickname())
                    editableUsername.applyChanges(context)
                }
        )
    }
}

@Composable
private fun PhysicalDeviceButton(
    onClick: () -> Unit,
    physicalDevice: VisyncPhysicalDevice,
) {
    val textStyle = MaterialTheme.typography.titleMedium
    val defaultContentColor = LocalContentColor.current
    ElevatedButton(onClick = onClick) {
        val fontSizeDp = with(LocalDensity.current) {
            textStyle.fontSize.toDp()
        }
        val finalTextStyle = textStyle.copy(color = defaultContentColor)
        val textSpacing = fontSizeDp / 2
        val iconSize = fontSizeDp * 2
        Icon(
            painter = painterResource(id = R.drawable.ic_phone),
            contentDescription = stringResource(R.string.desc_physical_device),
            tint = defaultContentColor,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(textSpacing))
        val displaySizeString = "%.2f\"".format(physicalDevice.inDisplaySize)
        val w = physicalDevice.pxDisplayWidth.roundToInt()
        val h = physicalDevice.pxDisplayHeight.roundToInt()
        val resolution = "%dx%d".format(w, h)
        val aspectRatioString = when (val gcd = getGreatestCommonDivisor(w, h)) {
            0 -> " (0:0)"
            else -> " (%d:%d)".format(h / gcd, w / gcd)
        }
        Text(
            text = displaySizeString,
            style = finalTextStyle,
        )
        Spacer(modifier = Modifier.width(textSpacing))
        var showResolutionStringCalculationPerformed by remember { mutableStateOf(false) }
        var showResolutionString by remember { mutableStateOf(true) }
        AnimatedVisibility(visible = showResolutionString) {
            Text(
                text = "$resolution$aspectRatioString",
                style = finalTextStyle,
                onTextLayout = {
                    if (!showResolutionStringCalculationPerformed) {
                        showResolutionStringCalculationPerformed = true
                        showResolutionString = it.lineCount == 1
                    }
                },
            )
        }
    }
}

@Composable
private fun PhysicalDeviceDialog(
    editablePhysicalDevice: EditablePhysicalDevice,
    onDismissRequest: () -> Unit,
) {
    GenericAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        PhysicalDeviceDialogContent(
            editablePhysicalDevice = editablePhysicalDevice,
            onCancel = onDismissRequest
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericAlertDialog(
    onDismissRequest: () -> Unit,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = surfaceColor
        ) {
            content()
        }
    }
}

@Composable
private fun PhysicalDeviceDialogContent(
    editablePhysicalDevice: EditablePhysicalDevice,
    onCancel: () -> Unit,
) {
    val sectionSpacerHeight = 16.dp
    val context = LocalContext.current
    Column(modifier = Modifier.height(IntrinsicSize.Min)) {
        val device = editablePhysicalDevice.value

        val currentDeviceRotation = getDeviceRotation(context)
        val probableResolution = when (currentDeviceRotation) {
            90, 270 -> getAvailableWindowSize(context).withDimensionsSwapped()
            else -> getAvailableWindowSize(context)
        }
        val availableResolution = getAvailableDrawingSize(context)

        val displaySizeInchesText = remember(device) {
            when (device.inDisplaySize) {
                0f -> mutableStateOf("")
                else -> mutableStateOf(device.inDisplaySize.toString())
            }
        }
        val displayWidthMmText = remember(device) {
            mutableStateOf(device.mmDisplayWidth.toString())
        }
        val displayHeightMmText = remember(device) {
            mutableStateOf(device.mmDisplayHeight.toString())
        }

        val displayWidthPxText = remember(device) {
            when (device.pxDisplayWidth) {
                0f -> mutableStateOf("")
                else -> mutableStateOf(device.pxDisplayWidth.roundToInt().toString())
            }
        }
        val displayHeightPxText = remember(device) {
            when (device.pxDisplayHeight) {
                0f -> mutableStateOf("")
                else -> mutableStateOf(device.pxDisplayHeight.roundToInt().toString())
            }
        }
        val rotationString = when (currentDeviceRotation) {
            0 -> ""
            else -> " ($currentDeviceRotation°)"
        }
        val availableDisplayWidthPxText = "${availableResolution.width}$rotationString"
        val availableDisplayHeightPxText = "${availableResolution.height}$rotationString"

        Log.d("Resolution", "Probable resolution: $probableResolution")
        Log.d("Resolution", "Available resolution: $availableResolution")

        val deviceWidthMmText = remember(device) {
            when (device.mmDeviceWidth) {
                0f -> mutableStateOf("")
                else -> mutableStateOf(device.mmDeviceWidth.toString())
            }
        }
        val deviceHeightMmText = remember(device) {
            when (device.mmDeviceHeight) {
                0f -> mutableStateOf("")
                else -> mutableStateOf(device.mmDeviceHeight.toString())
            }
        }

        val validateFloatNumber: (String) -> ValidationResult = {
            val floatValue = it.toFloatOrNull()
            when {
                it.isEmpty() -> ValidationResultFail("Can't be empty")
                floatValue == null -> ValidationResultFail("Invalid decimal number")
                floatValue <= 0f -> ValidationResultFail("Must be a positive number")
                else -> ValidationResultOk()
            }
        }
        val validateIntNumber: (String) -> ValidationResult = {
            val intValue = it.toIntOrNull()
            when {
                it.isEmpty() -> ValidationResultFail("Can't be empty")
                intValue == null -> ValidationResultFail("Invalid integer")
                intValue <= 0 -> ValidationResultFail("Must be a positive number")
                else -> ValidationResultOk()
            }
        }
        val displayFloatAsInt: (String) -> String = {
            it.toFloat().roundToInt().toString()
        }
        val displayFloatConcise: (String) -> String = {
            "%.2f".format(it.toFloat())
        }

        val errorMessages = remember { mutableStateMapOf<String, String>() }

        val displaySizeInFloatField = remember {
            EditableFloatField(
                label = "size (in.)",
                state = displaySizeInchesText,
                placeholder = "0.00",
                validate = validateFloatNumber
            )
        }
        val displayWidthMmFloatField = remember {
            NonEditableFloatField(
                label = "width (mm)",
                state = displayWidthMmText,
                stateToDisplayText = displayFloatConcise
            )
        }
        val displayHeightMmFloatField = remember {
            NonEditableFloatField(
                label = "height (mm)",
                state = displayHeightMmText,
                stateToDisplayText = displayFloatConcise
            )
        }
        val displayWidthPxFloatField = remember {
            EditableFloatField(
                label = "width (px)",
                state = displayWidthPxText,
                placeholder = probableResolution.width.toString(),
                validate = validateIntNumber
            )
        }
        val displayHeightPxFloatField = remember {
            EditableFloatField(
                label = "height (px)",
                state = displayHeightPxText,
                placeholder = probableResolution.height.toString(),
                validate = validateIntNumber
            )
        }
        val availableDisplayWidthPxFloatField = remember {
            NonEditableFloatField(
                label = "available w. (px)",
                state = mutableStateOf(availableDisplayWidthPxText),
            )
        }
        val availableDisplayHeightPxFloatField = remember {
            NonEditableFloatField(
                label = "available h. (px)",
                state = mutableStateOf(availableDisplayHeightPxText),
            )
        }

        val deviceWidthMmFloatField = remember {
            EditableFloatField(
                label = "width (mm)",
                state = deviceWidthMmText,
                placeholder = "0.00",
                validate = {
                    val result = validateFloatNumber(it)
                    if (result is ValidationResultFail) return@EditableFloatField result
                    val displayWidth = displayWidthMmText.value.toFloat()
                    val deviceWidth = it.toFloat()
                    when {
                        deviceWidth < displayWidth -> ValidationResultFail(
                            "Device can not be smaller than its display"
                        )
                        else -> ValidationResultOk()
                    }
                }
            )
        }
        val deviceHeightMmFloatField = remember {
            EditableFloatField(
                label = "height (mm)",
                state = deviceHeightMmText,
                placeholder = "0.00",
                validate = {
                    val result = validateFloatNumber(it)
                    if (result is ValidationResultFail) return@EditableFloatField result
                    val displayHeight = displayHeightMmText.value.toFloat()
                    val deviceHeight = it.toFloat()
                    when {
                        deviceHeight < displayHeight -> ValidationResultFail(
                            "Device can not be smaller than its display"
                        )
                        else -> ValidationResultOk()
                    }
                }
            )
        }

        val fieldsToValidate = listOf(
            displaySizeInFloatField,
            displayWidthPxFloatField,
            displayHeightPxFloatField,
            deviceWidthMmFloatField,
            deviceHeightMmFloatField,
        )
        val validateAll = {
            fieldsToValidate.forEach {
                val result = it.validate()
                if (result is ValidationResultFail) {
                    errorMessages += it.label to result.message
                } else {
                    errorMessages -= it.label
                }
            }
        }
        val updateWidthAndHeightMm = {
            val newSize = VisyncPhysicalDevice.getWidthAndHeightMm(
                inDisplaySize = displaySizeInchesText.value.toFloat(),
                pxDisplayWidth = displayWidthPxText.value.toFloat(),
                pxDisplayHeight = displayHeightPxText.value.toFloat(),
            )
            displayWidthMmText.value = newSize.width.toString()
            displayHeightMmText.value = newSize.height.toString()
        }
        val tryUpdateWidthAndHeightMm = {
            val requiredFields = listOf(
                displaySizeInFloatField,
                displayWidthPxFloatField,
                displayHeightPxFloatField,
            )
            if (requiredFields.all { it.isValid }) {
                updateWidthAndHeightMm()
            }
        }
        val tryUpdateWidthAndHeightMmAndValidateDeviceSize = {
            tryUpdateWidthAndHeightMm()
            val relatedFields = listOf(
                deviceWidthMmFloatField,
                deviceHeightMmFloatField
            )
            relatedFields.forEach {
                val result = it.validate()
                if (result is ValidationResultFail) {
                    errorMessages += it.label to result.message
                } else {
                    errorMessages -= it.label
                }
            }
        }
        var isInitialValidationCompleted by remember { mutableStateOf(false) }
        val physicalDeviceFromTextFields: () -> VisyncPhysicalDevice = {
            when {
                isInitialValidationCompleted -> VisyncPhysicalDevice(
                    pxDisplayWidth = displayWidthPxText.value.toFloat(),
                    pxDisplayHeight = displayHeightPxText.value.toFloat(),
                    inDisplaySize = displaySizeInchesText.value.toFloat(),
                    mmDeviceWidth = deviceWidthMmText.value.toFloat(),
                    mmDeviceHeight = deviceHeightMmText.value.toFloat(),
                )
                else -> device
            }

        }
        val isFieldsChanged: () -> Boolean = {
            physicalDeviceFromTextFields() != device
        }
        LaunchedEffect(Unit) {
            validateAll()
            isInitialValidationCompleted = true
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .bottomInnerShadow(
                    height = 16.dp,
                    alpha = 0.05f,
                    color = LocalContentColor.current
                )
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "My device",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.primary)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(sectionSpacerHeight / 2))
                PhysicalDeviceTextField(
                    floatField = displaySizeInFloatField,
                    onValidationSuccess = {
                        tryUpdateWidthAndHeightMmAndValidateDeviceSize()
                    },
                    errorMessages = errorMessages,
                )
                val rowSpacing = 8.dp
                // row for display width and height (in mm)
                Row(horizontalArrangement = Arrangement.spacedBy(rowSpacing)) {
                    listOf(
                        displayWidthMmFloatField,
                        displayHeightMmFloatField
                    ).forEach { field ->
                        PhysicalDeviceTextField(
                            floatField = field,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // row for display resolution
                Row(horizontalArrangement = Arrangement.spacedBy(rowSpacing)) {
                    listOf(
                        displayWidthPxFloatField,
                        displayHeightPxFloatField
                    ).forEach { field ->
                        PhysicalDeviceTextField(
                            floatField = field,
                            onValidationSuccess = {
                                tryUpdateWidthAndHeightMmAndValidateDeviceSize()
                            },
                            errorMessages = errorMessages,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // row for available display resolution
                Row(horizontalArrangement = Arrangement.spacedBy(rowSpacing)) {
                    listOf(
                        availableDisplayWidthPxFloatField,
                        availableDisplayHeightPxFloatField
                    ).forEach { field ->
                        PhysicalDeviceTextField(
                            floatField = field,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(sectionSpacerHeight))
                Text(
                    text = "Body",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(sectionSpacerHeight / 2))
                // row for device size
                Row(horizontalArrangement = Arrangement.spacedBy(rowSpacing)) {
                    listOf(
                        deviceWidthMmFloatField,
                        deviceHeightMmFloatField
                    ).forEach { field ->
                        PhysicalDeviceTextField(
                            floatField = field,
                            errorMessages = errorMessages,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Button(
                enabled = errorMessages.isEmpty() && isFieldsChanged(),
                onClick = {
                    editablePhysicalDevice.setValue(
                        physicalDeviceFromTextFields()
                    )
                    editablePhysicalDevice.applyChanges(context)
                },
                modifier = Modifier.weight(3f)
            ) {
                Text(text = "Save")
            }
            Button(
                onClick = {
                    displaySizeInchesText.value = device.inDisplaySize.toString()
                    displayWidthPxText.value = device.pxDisplayWidth.toString()
                    displayHeightPxText.value = device.pxDisplayHeight.toString()
                    deviceWidthMmText.value = device.mmDeviceWidth.toString()
                    deviceHeightMmText.value = device.mmDisplayHeight.toString()
                    onCancel()
                },
                modifier = Modifier.weight(2f)
            ) {
                Text(text = "Cancel")
            }
        }
    }
}

@Composable
private fun PhysicalDeviceTextField(
    floatField: EditableFloatField,
    onValidationSuccess: EditableFloatField.() -> Unit = {},
    onValidationFail: EditableFloatField.(ValidationResultFail) -> Unit = {},
    errorMessages: SnapshotStateMap<String, String>,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        value = floatField.state.value,
        placeholder = floatField.placeholder?.let {{ Text(it) }},
        label = { Text(floatField.label) },
        onValueChange = {
            floatField.state.value = it
            val result = floatField.validate()
            if (result is ValidationResultFail) {
                errorMessages += floatField.label to result.message
                floatField.onValidationFail(result)
            } else {
                errorMessages -= floatField.label
                floatField.onValidationSuccess()
            }
        },
        isError = errorMessages.keys.contains(floatField.label),
        supportingText = errorMessages[floatField.label]?.let {{ Text(it) }},
        modifier = modifier,
    )
}

@Composable
private fun PhysicalDeviceTextField(
    floatField: NonEditableFloatField,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        enabled = false,
        value = floatField.state.value.let { floatField.getDisplayText() },
        label = {
            Text(
                text = floatField.label,
                maxLines = 1,
            )
        },
        onValueChange = {},
        modifier = modifier,
    )
}

@Composable
private fun VisyncNavigationDrawerItem(
    destination: Route,
    isSelected: Boolean,
    navigateToDestination: (Route) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItem(
        modifier = modifier.fillMaxWidth(),
        selected = isSelected,
        label = {
            Text(
                text = stringResource(id = destination.actionDescriptionId),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        icon = {
            Icon(
                imageVector = destination.getImageVectorIcon(),
                contentDescription = stringResource(
                    id = destination.actionDescriptionId
                )
            )
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        ),
        onClick = { navigateToDestination(destination) }
    )
}

@Composable
fun VisyncNavigationRail(
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    openDrawer: () -> Unit,
    alwaysShowDestinationLabels: Boolean,
    editablePhysicalDevice: EditablePhysicalDevice,
    modifier: Modifier = Modifier,
    railWidth: Dp = 80.dp,
) {
    ModifiedComposeNavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .bottomInnerShadow(
                    height = 16.dp,
                    alpha = 0.05f,
                    color = LocalContentColor.current
                )
                .verticalScroll(scrollState)
                .padding(top = 16.dp, bottom = 0.dp)
        ) {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(id = R.string.desc_navigation_drawer)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            UserInfoOnRail(
                editablePhysicalDevice = editablePhysicalDevice,
                onUsernameClick = openDrawer,
                modifier = Modifier.fillMaxWidth()
            )
            Divider(modifier = Modifier.padding(16.dp))
            RailDestinations(
                selectedDestination = selectedDestination,
                navigateToDestination = navigateToDestination,
                alwaysShowDestinationLabels = alwaysShowDestinationLabels,
            )
        }
        RailBottomSettingsColumn(
            navigateToDestination = navigateToDestination,
            isDarkTheme = isDarkTheme,
            setDarkTheme = setDarkTheme,
            modifier = Modifier.height(56.dp)
        )
    }
}

@Composable
private fun ModifiedComposeNavigationRail(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    containerColor: Color = NavigationRailDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    windowInsets: WindowInsets = NavigationRailDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(windowInsets)
                .widthIn(min = 80.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = verticalArrangement
        ) {
            content()
        }
    }
}

@Composable
private fun RailBottomSettingsColumn(
    navigateToDestination: (Route) -> Unit,
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultContentColor = LocalContentColor.current
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = CenterHorizontally,
        modifier = modifier
    ) {
        IconButton(
            onClick = { navigateToDestination(Route.AppSettings) },
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Route.AppSettings.getImageVectorIcon(),
                contentDescription = stringResource(
                    id = Route.AppSettings.actionDescriptionId
                ),
                tint = defaultContentColor,
            )
        }
    }
}

@Composable
private fun UserInfoOnRail(
    editablePhysicalDevice: EditablePhysicalDevice,
    onUsernameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyleOnDrawer: TextStyle = MaterialTheme.typography.bodyLarge
    val defaultContentColor = LocalContentColor.current
    val fontSizeDp = with(LocalDensity.current) {
        textStyleOnDrawer.fontSize.toDp()
    }
    val iconSize = fontSizeDp * 2
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = stringResource(id = R.string.desc_username),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onUsernameClick)
                .padding(ButtonDefaults.ContentPadding)
                .size(iconSize)
        )
        var showPhysicalDeviceDialog by remember { mutableStateOf(false) }
        Icon(
            painter = painterResource(id = R.drawable.ic_phone),
            contentDescription = stringResource(R.string.desc_physical_device),
            tint = defaultContentColor,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { showPhysicalDeviceDialog = true }
                .padding(ButtonDefaults.ContentPadding)
                .size(iconSize)
        )
        if (showPhysicalDeviceDialog) {
            PhysicalDeviceDialog(
                editablePhysicalDevice = editablePhysicalDevice,
                onDismissRequest = { showPhysicalDeviceDialog = false }
            )
        }
    }
}

@Composable
private fun RailDestinations(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    alwaysShowDestinationLabels: Boolean,
) {
    CONNECTION_MODE_DESTINATIONS.forEach { accountDestination ->
        VisyncNavigationRailItem(
            destination = accountDestination,
            isSelected = selectedDestination == accountDestination.routeString,
            navigateToDestination = navigateToDestination,
            showDestinationLabels = alwaysShowDestinationLabels
        )
    }
    ACCOUNT_RELATED_DESTINATIONS.forEach { accountDestination ->
        VisyncNavigationRailItem(
            destination = accountDestination,
            isSelected = selectedDestination == accountDestination.routeString,
            navigateToDestination = navigateToDestination,
            showDestinationLabels = alwaysShowDestinationLabels
        )
    }
}

@Composable
private fun VisyncNavigationRailItem(
    destination: Route,
    isSelected: Boolean,
    navigateToDestination: (Route) -> Unit,
    showDestinationLabels: Boolean,
) {
    NavigationRailItem(
        selected = isSelected,
        label = {
            Text(
                text = stringResource(id = destination.actionDescriptionId),
            )
        },
        alwaysShowLabel = showDestinationLabels,
        icon = {
            Icon(
                imageVector = destination.getImageVectorIcon(),
                contentDescription = stringResource(
                    id = destination.actionDescriptionId
                )
            )
        },
        onClick = { navigateToDestination(destination) }
    )
}


/**
 *  Component that allows smooth toggling between
 *  PermanentNavigationDrawer and NavigationRail
 *  through poorly written band-aid-animations.
 */
@Composable
fun CollapsableNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    PermanentNavigationDrawer(
        drawerContent = drawerContent,
        content = content
    )
}

@Composable
fun CollapsableNavigationDrawerContent(
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: RailAndDrawerScrollState,
    drawerState: MutableState<CollapsableDrawerState>,
    editableUsername: EditableUsername,
    editablePhysicalDevice: EditablePhysicalDevice,
    permanentDrawerWidth: Dp? = null,
    railWidth: Dp = 80.dp,
) {
    val defaultDrawerWidth = with(LocalDensity.current) {
        LocalContext.current.resources.displayMetrics.widthPixels.toDp() * 6 / 15
    }
    Log.d("OneThirdAppWidth", "$defaultDrawerWidth")
    val permanentDrawerTransitionState = remember { mutableStateOf(drawerState.value) }
    val railTransitionState = remember { mutableStateOf(drawerState.value) }
    val (drawerSheetModifier, railModifier) = getDrawerAndRailAnimationModifiers(
        CollapsableDrawerPermanentDrawerAnimation.SLIDE,
        CollapsableDrawerRailAnimation.FADE,
        collapsableDrawerState = drawerState,
        initialDrawerWidth = permanentDrawerWidth ?: defaultDrawerWidth,
        initialRailWidth = railWidth,
        permanentDrawerTransitionState = permanentDrawerTransitionState,
        railTransitionState = railTransitionState
    )
    PermanentNavigationDrawerContent(
        isDarkTheme = isDarkTheme,
        setDarkTheme = setDarkTheme,
        selectedDestination = selectedDestination,
        navigateToDestination = navigateToDestination,
        scrollState = scrollState.drawerScrollState,
        showCloseDrawerButton = true,
        closeDrawerButtonClick = {
            permanentDrawerTransitionState.value =
                CollapsableDrawerState.COLLAPSED
        },
        editableUsername = editableUsername,
        editablePhysicalDevice = editablePhysicalDevice,
        drawerSheetContentModifier = Modifier
            .width(permanentDrawerWidth ?: defaultDrawerWidth),
        drawerSheetModifier = drawerSheetModifier
    )
    VisyncNavigationRail(
        isDarkTheme = isDarkTheme,
        setDarkTheme = setDarkTheme,
        selectedDestination = selectedDestination,
        navigateToDestination = navigateToDestination,
        scrollState = scrollState.railScrollState,
        openDrawer = {
            railTransitionState.value =
                CollapsableDrawerState.EXPANDED
        },
        alwaysShowDestinationLabels = false,
        editablePhysicalDevice = editablePhysicalDevice,
        modifier = railModifier
    )
}

/*
    TODO:
        check if there is a better way of chaining animations so
        instead of linking two transitions to each other
        and constructing hacky modifiers
        we can rewrite this in a concise way.
 */
@Composable
private fun getDrawerAndRailAnimationModifiers(
    @Suppress("SameParameterValue")
    collapsableDrawerPermanentDrawerAnimation: CollapsableDrawerPermanentDrawerAnimation,
    @Suppress("SameParameterValue")
    collapsableDrawerRailAnimation: CollapsableDrawerRailAnimation,
    collapsableDrawerState: MutableState<CollapsableDrawerState>,
    initialDrawerWidth: Dp,
    initialRailWidth: Dp,
    permanentDrawerTransitionState: MutableState<CollapsableDrawerState>,
    railTransitionState: MutableState<CollapsableDrawerState>,
): Pair<Modifier, Modifier> {

    val permanentDrawerTransition = updateTransition(
        targetState = permanentDrawerTransitionState.value,
        label = "PermanentDrawer visibility transition"
    )
    val railTransition = updateTransition(
        targetState = railTransitionState.value,
        label = "NavigationRail visibility transition"
    )

    LaunchedEffect(
        permanentDrawerTransition.currentState
    ) {
        if (permanentDrawerTransition.currentState != railTransitionState.value) {
            collapsableDrawerState.value = permanentDrawerTransition.currentState
            railTransitionState.value = permanentDrawerTransition.currentState
        }
    }
    LaunchedEffect(
        railTransition.currentState
    ) {
        if (railTransition.currentState != permanentDrawerTransitionState.value) {
            collapsableDrawerState.value = railTransition.currentState
            permanentDrawerTransitionState.value = railTransition.currentState
        }
    }

    val drawerSheetWidth by permanentDrawerTransition.animateDp(
        label = "Drawer sheet width",
    ) { state ->
        when (state) {
            CollapsableDrawerState.COLLAPSED -> 0.dp
            CollapsableDrawerState.EXPANDED -> initialDrawerWidth
        }
    }

    val railWidth by railTransition.animateDp(
        label = "Rail width",
    ) { state ->
        when (state) {
            CollapsableDrawerState.COLLAPSED -> initialRailWidth
            CollapsableDrawerState.EXPANDED -> 0.dp
        }
    }
    val railAlpha by railTransition.animateFloat(
        label = "Rail alpha"
    ) { state ->
        when (state) {
            CollapsableDrawerState.COLLAPSED -> 1f
            CollapsableDrawerState.EXPANDED -> 0f
        }
    }

    // animation ends when all states hold same value
    val permanentDrawerIsInAnimation = setOf(
        permanentDrawerTransition.targetState,
        permanentDrawerTransition.currentState,
        collapsableDrawerState.value
    ).count() != 1
    val drawerSheetModifier = if (permanentDrawerIsInAnimation) {
        when (collapsableDrawerPermanentDrawerAnimation) {
            CollapsableDrawerPermanentDrawerAnimation.CLIP ->
                Modifier.overflowHiddenForDrawer(
                    clipWidth = drawerSheetWidth,
                    minimumActualWidth = initialRailWidth
                )
            CollapsableDrawerPermanentDrawerAnimation.SLIDE ->
                Modifier.slideInAndOutForDrawer(
                    clipWidth = drawerSheetWidth,
                    minimumActualWidth = initialRailWidth,
                    maximumActualWidth = initialDrawerWidth
                )
        }
    } else if (collapsableDrawerState.value == CollapsableDrawerState.COLLAPSED) {
        Modifier.width(0.dp)
    } else {
        Modifier
    }

    // animation ends when all states hold same value
    val railIsInAnimation = setOf(
        railTransition.targetState,
        railTransition.currentState,
        collapsableDrawerState.value
    ).count() != 1
    val railModifier = if (railIsInAnimation) {
        when (collapsableDrawerRailAnimation) {
            CollapsableDrawerRailAnimation.CLIP ->
                Modifier.overflowHiddenForRail(
                    clipWidth = railWidth
                )
            CollapsableDrawerRailAnimation.FADE ->
                Modifier.fadeInAndOutForRail(
                    alpha = railAlpha
                )
        }
    } else if (collapsableDrawerState.value == CollapsableDrawerState.EXPANDED) {
        Modifier.width(0.dp)
    } else {
        Modifier
    }

    return Pair(drawerSheetModifier, railModifier)
}

/**
 *  Returns modifier that emulates ***overflow: hidden*** rule from CSS.
 *  HorizontalScroll is used to achieve this, use this with caution.
 *  @param clipWidth width available for a content. All content
 *  wider than that will be cropped.
 *  @param minimumActualWidth width that should remain there, even if not visible.
 */
fun Modifier.overflowHiddenForDrawer(
    clipWidth: Dp,
    minimumActualWidth: Dp,
): Modifier = this.composed {
    val pxClipValue = with(LocalDensity.current) { clipWidth.toPx() }
    this
        .width(clipWidth.coerceAtLeast(minimumActualWidth))
        .horizontalScroll(ScrollState(0))
        .drawWithContent {
            clipRect(right = pxClipValue) {
                this@drawWithContent.drawContent()
            }
        }
}

fun Modifier.slideInAndOutForDrawer(
    clipWidth: Dp,
    minimumActualWidth: Dp,
    maximumActualWidth: Dp,
): Modifier = this.composed {
    this
        .width(clipWidth.coerceAtLeast(minimumActualWidth))
        .horizontalScroll(remember { ScrollState(0) }, false)
        .offset(x = clipWidth - maximumActualWidth)
}

fun Modifier.overflowHiddenForRail(
    clipWidth: Dp,
): Modifier = this.composed {
    val  pxClipValue = with(LocalDensity.current) { clipWidth.toPx() }
    drawWithContent {
        clipRect(right = pxClipValue) {
            this@drawWithContent.drawContent()
        }
    }
}

fun Modifier.fadeInAndOutForRail(
    alpha: Float,
): Modifier {
    return alpha(alpha)
}

enum class CollapsableDrawerState {
    EXPANDED, COLLAPSED
}

private enum class CollapsableDrawerRailAnimation {
    CLIP, FADE
}

private enum class CollapsableDrawerPermanentDrawerAnimation {
    CLIP, SLIDE
}

@Preview
@Composable
private fun PhysicalDeviceButtonPreview() {
    PhysicalDeviceButton(
        onClick = {},
        physicalDevice = getSimplePhysicalDevice(),
    )
}

@Preview(widthDp = 250)
@Composable
private fun PhysicalDeviceButtonWithLessWidthPreview() {
    PhysicalDeviceButton(
        onClick = {},
        physicalDevice = getSimplePhysicalDevice(),
    )
}

@Preview
@Composable
private fun PhysicalDeviceDialogPreview() {
    PhysicalDeviceDialog(
        editablePhysicalDevice = getSimpleEditablePhysicalDevice(),
        onDismissRequest = {},
    )
}

@Preview
@Composable
private fun ModalNavigationDrawerContentPreview() {
    ModalNavigationDrawerContent(
        isDarkTheme = false,
        setDarkTheme = {},
        selectedDestination = Route.PlaybackSetup.routeString,
        navigateToDestination = {},
        scrollState = rememberScrollState(),
        showMainDestinations = true,
        closeDrawer = {},
        editableUsername = getSimpleEditableUsername(),
        editablePhysicalDevice = getSimpleEditablePhysicalDevice(),
    )
}

@Preview
@Composable
private fun VisyncNavigationRailPreview() {
    VisyncNavigationRail(
        isDarkTheme = false,
        setDarkTheme = {},
        selectedDestination = Route.PlaybackSetup.routeString,
        navigateToDestination = {},
        scrollState = rememberScrollState(),
        openDrawer = {},
        alwaysShowDestinationLabels = false,
        editablePhysicalDevice = getSimpleEditablePhysicalDevice(),
    )
}
@Preview
@Composable
private fun RailAndDrawerSideBySidePreview() {
    Row {
        VisyncNavigationRailPreview()
        ModalNavigationDrawerContentPreview()
    }
}

fun getSimplePhysicalDevice(): VisyncPhysicalDevice {
    return VisyncPhysicalDevice(
        inDisplaySize = 6.3f,
        pxDisplayWidth = 1080f,
        pxDisplayHeight = 2280f,
        mmDeviceWidth = 75.50f,
        mmDeviceHeight = 157.90f,
    )
}

fun getSimpleEditablePhysicalDevice(): EditablePhysicalDevice {
    return EditablePhysicalDevice(
        value = getSimplePhysicalDevice(),
        isEditable = true,
        enableEditing = {},
        disableEditing = {},
        setValue = {},
        applyChanges = {},
    )
}
fun getSimpleEditableUsername(): EditableUsername {
    return EditableUsername(
        value = "AdventurousCapybara",
        isEditable = true,
        enableEditing = {},
        disableEditing = {},
        setValue = {},
        applyChanges = {},
    )
}

tailrec fun getGreatestCommonDivisor(a: Int, b: Int): Int {
    return if (b == 0) a else getGreatestCommonDivisor(b, a % b)
}

open class FloatField(
    val label: String,
    val state: MutableState<String>,
)

class EditableFloatField(
    label: String,
    state: MutableState<String>,
    val placeholder: String? = null,
    private val validate: ((String) -> ValidationResult)? = null,
): FloatField(label, state) {
    fun validate(): ValidationResult {
        return validate?.invoke(state.value) ?: ValidationResultOk()
    }
    val isValid: Boolean
        get() = validate().success
}

class NonEditableFloatField(
    label: String,
    state: MutableState<String>,
    private val stateToDisplayText: ((String) -> String) = { it },
): FloatField(label, state) {
    fun getDisplayText(): String {
        return stateToDisplayText(state.value)
    }
}

interface ValidationResult {
    val success: Boolean
}

class ValidationResultOk: ValidationResult {
    override val success = true
}

class ValidationResultFail(
    val message: String,
): ValidationResult {
    override val success = false
}

fun Size.withDimensionsSwapped(): Size {
    return Size(height, width)
}

fun Modifier.bottomInnerShadow(
    height: Dp,
    alpha: Float,
    color: Color,
): Modifier = composed {
    val shadowHeightPx = with(LocalDensity.current) {
        height.toPx()
    }
    drawWithContent {
        drawContent()
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                color,
            ),
            startY = size.height - shadowHeightPx,
            endY = size.height
        )
        drawRect(
            brush = gradientBrush,
            alpha = alpha
        )
    }
}

fun Modifier.topInnerShadow(
    height: Dp,
    alpha: Float,
    color: Color,
): Modifier = composed {
    val shadowHeightPx = with(LocalDensity.current) {
        height.toPx()
    }
    drawWithContent {
        drawContent()
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                color,
                Color.Transparent,
            ),
            startY = 0f,
            endY = shadowHeightPx
        )
        drawRect(
            brush = gradientBrush,
            alpha = alpha
        )
    }
}