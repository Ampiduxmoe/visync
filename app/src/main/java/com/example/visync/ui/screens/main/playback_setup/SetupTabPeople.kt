package com.example.visync.ui.screens.main.playback_setup

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.visync.R
import com.example.visync.metadata.VideoMetadata
import com.example.visync.ui.screens.player.VisyncPhysicalDevice
import com.example.visync.ui.screens.settings.getProfilePreferences
import kotlinx.serialization.Serializable

@Composable
fun SetupTabPeople(
    isUserHost: Boolean,
    hostAsWatcher: Watcher,
    meAsWatcher: Watcher,
    notApprovedWatchers: List<Watcher>,
    approvedWatchers: List<Watcher>,
    videoMetadata: VideoMetadata,
    setFinalDevicePositionConfiguration: (FinalDevicePositionConfiguration) -> Unit,
    notApprovedWatcherModifier: (Watcher) -> Modifier,
    approvedWatcherModifier: (Watcher) -> Modifier,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val watcherColor: (Watcher) -> Color = { watcher ->
        when (watcher.messagingVersion != meAsWatcher.messagingVersion) {
            true -> Color.Red // versions conflict
            false -> Color.Unspecified // versions match
        }
    }
    val watcherText: (Watcher) -> String = { watcher ->
        if (watcher == meAsWatcher && meAsWatcher.username.isEmpty()) {
            val backupUsername = getUsername(context)
            val maybeHost = if (hostAsWatcher.username == backupUsername) " (host)" else "" // redo
            "$backupUsername$maybeHost (me)"
        } else {
            val maybeHost = if (watcher == hostAsWatcher) " (host)" else ""
            val maybeMe = if (watcher == meAsWatcher) " (me)" else ""
            "${watcher.username} [${watcher.endpointId}]$maybeHost$maybeMe"
        }
    }

    val distinctColors = remember { listOf(
        0xFF4cb4f0,
        0xFFff9a00,
        0xFF0b9f57,
        0xFFb0daec,
        0xFFffea87,
        0xFFffcdd7,
    ) }
    val colorsCount = remember { distinctColors.count() }
    var nextColorIndex = 0
    val nextColor = {
        distinctColors[nextColorIndex].also { nextColorIndex = (nextColorIndex + 1) % colorsCount }
    }
    var showDevicesPositionEditor by remember { mutableStateOf(false) }
    if (showDevicesPositionEditor) {
        DevicesPositionConfigurationEditor(
            approvedWatchers = approvedWatchers.map {
                WatcherTag(
                    username = it.username,
                    endpointId = it.endpointId,
                    colorValue = nextColor(),
                    physicalDevice = it.physicalDevice
                )
            },
            videoMetadata = videoMetadata,
            setFinalDevicePositionConfiguration = setFinalDevicePositionConfiguration,
            closeEditor = { showDevicesPositionEditor = false }
        )
    }

    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            Text("requests:")
            for (watcher in notApprovedWatchers) {
                Text(
                    text = watcherText(watcher),
                    color = watcherColor(watcher),
                    modifier = notApprovedWatcherModifier(watcher)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("approved:")
            for (watcher in approvedWatchers) {
                Text(
                    text = watcherText(watcher),
                    color = watcherColor(watcher),
                    modifier = approvedWatcherModifier(watcher)
                )
            }
        }
    }
    Text(
        text = "show config",
        modifier = Modifier.clickable { showDevicesPositionEditor = true }
    )
}

@Composable
fun DevicesPositionConfigurationEditor(
    approvedWatchers: List<WatcherTag>,
    videoMetadata: VideoMetadata,
    setFinalDevicePositionConfiguration: (FinalDevicePositionConfiguration) -> Unit,
    closeEditor: () -> Unit,
) {
    val dialogMinWidth = 280.dp
    val dialogMaxWidth = 560.dp

    val pxScreenWidth = 1080f
    val fractionEditorWidth = 0.8f
    val pxEditorWidth = pxScreenWidth * fractionEditorWidth
    val targetDialogWidth = with(LocalDensity.current) {
        pxEditorWidth.toDp()
    }

    val pxScreenHeight = 2160f
    val fractionEditorHeight = 0.6f
    val pxEditorHeight = pxScreenHeight * fractionEditorHeight
    val targetDialogHeight = with(LocalDensity.current) {
        pxEditorHeight.toDp()
    }
    val editorHeight = targetDialogHeight
    val editorWidth = targetDialogWidth

    var devicesInfo by remember { mutableStateOf(
        approvedWatchers.mapIndexed { index, watcherTag ->
            val physicalDevice = watcherTag.physicalDevice
            watcherTag to DeviceOnCanvas(
                mmOffsetX = index * 100f,
                mmOffsetY = 0f,
                mmDisplayWidth = physicalDevice.mmDisplayWidth,
                mmDisplayHeight = physicalDevice.mmDisplayHeight,
                mmDeviceWidth = physicalDevice.mmDisplayWidth, // TODO: do not forget to change it back to device width
                mmDeviceHeight = physicalDevice.mmDisplayHeight,
            )
        }.toMap()
    ) }
    Log.d("tag", "approved count = ${approvedWatchers.count()}, devicesInfo count = ${devicesInfo.entries.count()}")
    var videoOnCanvas by remember { mutableStateOf(
        VideoOnCanvas(
            mmOffsetX = 0f,
            mmOffsetY = 0f,
            mmWidth = videoMetadata.width / 10,
            mmHeight = videoMetadata.height / 10,
        )
    ) }
    var canvasCameraView by remember { mutableStateOf(
        CanvasCameraView(
            mmViewOffsetX = 0f,
            mmViewOffsetY = 0f,
            zoom = 1f
        )
    ) }
    val mmOfRealWidthEditorShouldFit = 150f

    val mmToPxMultiplier = pxEditorWidth / mmOfRealWidthEditorShouldFit * canvasCameraView.zoom
    val pxToMmMultiplier = mmOfRealWidthEditorShouldFit / (pxEditorWidth * canvasCameraView.zoom)
    val mmToDpMultiplier = mmToPxMultiplier / LocalDensity.current.density
    val dpToMmMultiplier = LocalDensity.current.density / mmToPxMultiplier

    val mmToPx = { mm: Float -> (mm * mmToPxMultiplier) }
    val pxToMm = { px: Float -> (px * pxToMmMultiplier) }
    val mmToDp = { mm: Float -> (mm * mmToDpMultiplier).dp }
    val dpToMm = { dp: Dp -> (dp.value * dpToMmMultiplier) }

    var selectedTags by remember { mutableStateOf(emptyList<WatcherTag>()) }
    val addTagToSelection = { tag: WatcherTag ->
        selectedTags = selectedTags + tag
    }
    val removeTagFromSelection = { tag: WatcherTag ->
        selectedTags = selectedTags.filter { it != tag }
    }
    val clearSelectedTags = {
        selectedTags = emptyList()
    }
    val fakeTag = remember {
        WatcherTag(
            username = "FakeName",
            endpointId = "FakeEndpointId",
            physicalDevice = VisyncPhysicalDevice(
                mmDeviceWidth = 0f,
                mmDeviceHeight = 0f,
                mmDisplayWidth = 0f,
                mmDisplayHeight = 0f,
                pxDisplayWidth = 0f,
                pxDisplayHeight = 0f
            )
        )
    }
    val videoTag = remember {
        WatcherTag(
            username = "${videoMetadata.filename}",
            endpointId = "${videoMetadata.width} * ${videoMetadata.height}",
            physicalDevice = VisyncPhysicalDevice(
                mmDeviceWidth = 0f,
                mmDeviceHeight = 0f,
                mmDisplayWidth = 0f,
                mmDisplayHeight = 0f,
                pxDisplayWidth = 0f,
                pxDisplayHeight = 0f
            )
        )
    }

    LaunchedEffect(approvedWatchers) {
        val storedTagsSorted = devicesInfo.keys.sortedBy { it.endpointId }
        val newTagsSorted = approvedWatchers.sortedBy { it.endpointId }
        if (storedTagsSorted != newTagsSorted) {
            // filter
            val newDevicesInfo = devicesInfo.filterKeys { it in approvedWatchers }.toMutableMap()

            // add new
            val newWatchers = approvedWatchers.filter { it !in newDevicesInfo.keys }
            newDevicesInfo += newWatchers.mapIndexed { index, watcherTag ->
                val physicalDevice = watcherTag.physicalDevice
                watcherTag to DeviceOnCanvas(
                    mmOffsetX = index * 100f,
                    mmOffsetY = 0f,
                    mmDisplayWidth = physicalDevice.mmDisplayWidth,
                    mmDisplayHeight = physicalDevice.mmDisplayHeight,
                    mmDeviceWidth = physicalDevice.mmDisplayWidth, // TODO: do not forget to change it back to device width
                    mmDeviceHeight = physicalDevice.mmDisplayHeight,
                )
            }.toMap()

            // replace
            devicesInfo = newDevicesInfo
        }
    }
    var mmPointerOnEditor by remember { mutableStateOf(Offset(0f, 0f)) }
    var truePointerPos by remember { mutableStateOf(Offset(0f, 0f)) }

    val isPointInSelection: (Offset) -> Boolean = { mmPoint: Offset ->
        val isOnDevice = devicesInfo
            .filter { it.key in selectedTags }
            .any { it.value.containsPoint(mmPoint) }
        val isOnVideo = videoTag in selectedTags && videoOnCanvas.containsPoint(mmPoint)

        isOnDevice || isOnVideo
    }

    val tryMoveSelection = { mmOffset: Offset ->
        val (selectedDevicesInfo, otherDevicesInfo) = devicesInfo.entries.partition {
            it.key in selectedTags
        }
        val selectedDevices = selectedDevicesInfo.map { it.value }
        val otherDevices = otherDevicesInfo.map { it.value }

        val movedVersions = selectedDevices.map {
            it.copy(
                mmOffsetX = it.mmOffsetX + mmOffset.x,
                mmOffsetY = it.mmOffsetY + mmOffset.y
            )
        }
        if (movedVersions.none { it.isIntersectingAny(otherDevices) }) {
            devicesInfo = devicesInfo.map { deviceInfo ->
                val tag = deviceInfo.key
                val device = deviceInfo.value
                val newDevice = when (device in selectedDevices) {
                    false -> device
                    true -> {
                        val indexInSelection = selectedDevices.indexOf(device)
                        movedVersions[indexInSelection]
                    }
                }
                tag to newDevice
            }.toMap()
            if (videoTag in selectedTags) {
                videoOnCanvas = videoOnCanvas.copy(
                    mmOffsetX = videoOnCanvas.mmOffsetX + mmOffset.x,
                    mmOffsetY = videoOnCanvas.mmOffsetY + mmOffset.y
                )
            }
        }
    }

    val saveConfigAndCloseEditor = {
        setFinalDevicePositionConfiguration(
            FinalDevicePositionConfiguration(
                videoOnCanvas = videoOnCanvas,
                devicesOnCanvas = devicesInfo.map { WatcherTagAndDeviceOnCanvas(it.key, it.value) }
            )
        )
        closeEditor()
    }
    Dialog(
        onDismissRequest = saveConfigAndCloseEditor,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .requiredSize(editorWidth, editorHeight)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                mmPointerOnEditor = Offset(
                                    x = pxToMm(it.x / canvasCameraView.zoom) + canvasCameraView.mmViewOffsetX,
                                    y = pxToMm(it.y / canvasCameraView.zoom) + canvasCameraView.mmViewOffsetY,
                                )
                                val selectedDeviceInfo = devicesInfo.entries.find { deviceInfo ->
                                    val device = deviceInfo.value
                                    device.containsPoint(mmPointerOnEditor)
                                }
                                if (selectedDeviceInfo == null) {
                                    val isPointerOnVideo = videoOnCanvas.containsPoint(mmPointerOnEditor)
                                    if (!isPointerOnVideo) {
                                        clearSelectedTags()
                                    }
                                } else {
                                    val tag = selectedDeviceInfo.key
                                    if (tag in selectedTags) {
                                        removeTagFromSelection(tag)
                                    } else {
                                        addTagToSelection(tag)
                                    }
                                }

                            },
                            onLongPress = {
                                mmPointerOnEditor = Offset(
                                    x = pxToMm(it.x / canvasCameraView.zoom) + canvasCameraView.mmViewOffsetX,
                                    y = pxToMm(it.y / canvasCameraView.zoom) + canvasCameraView.mmViewOffsetY,
                                )
                                if (videoOnCanvas.containsPoint(mmPointerOnEditor)) {
                                    if (videoTag in selectedTags) {
                                        removeTagFromSelection(videoTag)
                                    } else {
                                        addTagToSelection(videoTag)
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, rotation ->
                            truePointerPos = centroid
                            mmPointerOnEditor = Offset(
                                x = pxToMm(centroid.x / canvasCameraView.zoom) + canvasCameraView.mmViewOffsetX,
                                y = pxToMm(centroid.y / canvasCameraView.zoom) + canvasCameraView.mmViewOffsetY,
                            )
                            val mmPan = Offset(
                                x = pxToMm(pan.x / canvasCameraView.zoom),
                                y = pxToMm(pan.y / canvasCameraView.zoom)
                            )
                            if (selectedTags.isNotEmpty()) {
                                tryMoveSelection(mmPan)
                                if (videoTag in selectedTags) {
                                    videoOnCanvas = videoOnCanvas.zoomedBy(zoom, videoOnCanvas.mmCenter)
                                }
                            } else {
                                canvasCameraView = canvasCameraView
                                    .zoomedBy(zoom, mmPointerOnEditor)
                                    .withAddedOffset(-mmPan.x, -mmPan.y)
                            }
                        }
                    },
            ) {
                val mmCameraPosX = canvasCameraView.mmViewOffsetX
                val mmCameraPosY = canvasCameraView.mmViewOffsetY
                videoOnCanvas.let {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(
                            x = mmToPx(it.mmOffsetX - mmCameraPosX),
                            y = mmToPx(it.mmOffsetY - mmCameraPosY)
                        ),
                        size = Size(
                            width = mmToPx(it.mmWidth),
                            height = mmToPx(it.mmHeight)
                        ),
                        alpha = 0.5f,
                        style = Fill,
                    )
                    if (videoTag in selectedTags) {
                        drawRect(
                            color = Color.Magenta,
                            topLeft = Offset(
                                x = mmToPx(it.mmOffsetX - mmCameraPosX),
                                y = mmToPx(it.mmOffsetY - mmCameraPosY)
                            ),
                            size = Size(
                                width = mmToPx(it.mmWidth),
                                height = mmToPx(it.mmHeight)
                            ),
                            alpha = 0.5f,
                            style = Stroke(width = 4f),
                        )
                    }
                }
                Log.d("tag", "approved count = ${approvedWatchers.count()}, devicesInfo count = ${devicesInfo.entries.count()}")
                devicesInfo.forEach {
                    val tag = it.key
                    val device = it.value
                    Log.d("tag", "tag: $tag, device info: $device")
                    drawRect(
                        color = tag.color,
                        topLeft = Offset(
                            x = mmToPx(device.mmOffsetX - mmCameraPosX),
                            y = mmToPx(device.mmOffsetY - mmCameraPosY)
                        ),
                        size = Size(
                            width = mmToPx(device.mmDeviceWidth),
                            height = mmToPx(device.mmDeviceHeight)
                        ),
                        alpha = 0.5f,
                        style = Fill,
                    )
                    if (tag in selectedTags) {
                        drawRect(
                            color = Color.Magenta,
                            topLeft = Offset(
                                x = mmToPx(device.mmOffsetX - mmCameraPosX),
                                y = mmToPx(device.mmOffsetY - mmCameraPosY)
                            ),
                            size = Size(
                                width = mmToPx(device.mmDeviceWidth),
                                height = mmToPx(device.mmDeviceHeight)
                            ),
                            alpha = 0.5f,
                            style = Stroke(width = 4f),
                        )
                    }
                }
                mmPointerOnEditor.let {
                    val pointerWidth = mmToPx(25f / canvasCameraView.zoom)
                    val pointerHeight = mmToPx(25f / canvasCameraView.zoom)
                    drawRect(
                        color = Color.Magenta,
                        topLeft = Offset(
                            x = mmToPx(it.x - mmCameraPosX) - pointerWidth / 2,
                            y = mmToPx(it.y - mmCameraPosY) - pointerHeight / 2
                        ),
                        size = Size(
                            width = pointerWidth,
                            height = pointerHeight
                        ),
                        alpha = 0.5f,
                        style = Fill,
                    )
                }
//                truePointerPos.let {
//                    drawCircle(
//                        color = Color.Magenta,
//                        radius = 50f,
//                        center = it,
//                        alpha = 0.5f,
//                        style = Fill,
//                    )
//                }
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val selectedTagsSnapshot = selectedTags
                selectedTagsSnapshot.forEach { tag ->
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "${tag.username} [${tag.endpointId}]",
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = saveConfigAndCloseEditor
                    ) {
                        Text("Confirm")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.1f)
                        .alpha(0.5f)
                        .background(Color.White)
                ) {
                    val mmCenter = with(LocalDensity.current) {
                        Offset(
                            x = pxToMm(editorWidth.toPx() / 2) - canvasCameraView.mmViewOffsetX,
                            y = pxToMm(editorHeight.toPx() / 2) - canvasCameraView.mmViewOffsetY
                        )
                    }
                    Text(
                        text = "+",
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clickable {
                                canvasCameraView = canvasCameraView.zoomedTo(
                                    targetZoom = canvasCameraView.zoom + 0.1f,
                                    mmPivotPoint = mmCenter
                                )
                            }
                    )
                    Text(
                        text = String.format("%.1f", canvasCameraView.zoom),
                        modifier = Modifier
                    )
                    Text(
                        text = "-",
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clickable {
                                canvasCameraView = canvasCameraView.zoomedTo(
                                    targetZoom = canvasCameraView.zoom - 0.1f,
                                    mmPivotPoint = mmCenter
                                )
                            }
                    )
                }
            }
        }
    }
}

data class CanvasCameraView(
    val mmViewOffsetX: Float,
    val mmViewOffsetY: Float,
    val zoom: Float
) {
    fun withAddedOffset(x: Float, y: Float): CanvasCameraView {
        return copy(
            mmViewOffsetX = mmViewOffsetX + x,
            mmViewOffsetY = mmViewOffsetY + y
        )
    }

    fun zoomedBy(zoomMultiplier: Float, mmPivotPoint: Offset): CanvasCameraView {
        return zoomedTo(
            targetZoom = zoom * zoomMultiplier,
            mmPivotPoint = mmPivotPoint
        )
    }

    fun zoomedTo(targetZoom: Float, mmPivotPoint: Offset): CanvasCameraView {
        val newZoom = targetZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val zoomMultiplier = newZoom / zoom
        val cameraTopLeft = Offset(x = mmViewOffsetX, y = mmViewOffsetY)
        val offsetChange = (mmPivotPoint - cameraTopLeft) * ((zoomMultiplier - 1) / zoomMultiplier)
        return copy(
            mmViewOffsetX = mmViewOffsetX + offsetChange.x,
            mmViewOffsetY = mmViewOffsetY + offsetChange.y,
            zoom = zoom * zoomMultiplier
        )
    }

    companion object {
        const val MAX_ZOOM = 2f
        const val MIN_ZOOM = 0.5f
    }
}

@Serializable
data class VideoOnCanvas(
    val mmOffsetX: Float,
    val mmOffsetY: Float,
    val mmWidth: Float,
    val mmHeight: Float,
) {
    val mmLeft
        get() = mmOffsetX
    val mmRight
        get() = mmLeft + mmWidth
    val mmTop
        get() = mmOffsetY
    val mmBottom
        get() = mmTop + mmHeight
    val mmCenter
        get() = Offset(x = mmOffsetX + mmWidth / 2, y = mmOffsetY + mmHeight / 2)

    fun zoomedBy(zoomMultiplier: Float, mmPivotPoint: Offset): VideoOnCanvas {
        if (zoomMultiplier == 1f) return this
        val videoTopLeft = Offset(x = mmOffsetX, y = mmOffsetY)
        val offsetChange = (mmPivotPoint - videoTopLeft) * ((zoomMultiplier - 1) / zoomMultiplier)
        return copy(
            mmOffsetX = mmOffsetX + offsetChange.x,
            mmOffsetY = mmOffsetY + offsetChange.y,
            mmWidth = mmWidth * zoomMultiplier,
            mmHeight = mmHeight * zoomMultiplier,
        )
    }
}

@Serializable
data class FinalDevicePositionConfiguration(
    val videoOnCanvas: VideoOnCanvas,
    val devicesOnCanvas: List<WatcherTagAndDeviceOnCanvas>
)

@Serializable
data class WatcherTagAndDeviceOnCanvas(
    val watcherTag: WatcherTag,
    val deviceOnCanvas: DeviceOnCanvas
)

@Serializable
data class DeviceOnCanvas(
    val mmOffsetX: Float,
    val mmOffsetY: Float,
    val mmDisplayWidth: Float,
    val mmDisplayHeight: Float,
    val mmDeviceWidth: Float,
    val mmDeviceHeight: Float,
) {
    val mmLeft
        get() = mmOffsetX
    val mmRight
        get() = mmLeft + mmDeviceWidth
    val mmTop
        get() = mmOffsetY
    val mmBottom
        get() = mmTop + mmDeviceHeight
}

@Serializable
data class WatcherTag(
    val username: String,
    val endpointId: String,
    val colorValue: Long = Color.Red.toArgb().toLong(),
    val physicalDevice: VisyncPhysicalDevice,
) {
    val color
        get() = Color(colorValue)
}

private fun getUsername(context: Context): String? {
    val profilePrefs = getProfilePreferences(context)
    val usernameKey = context.getString(R.string.prefs_profile_username)
    return profilePrefs.getString(usernameKey, null)
}

fun VideoOnCanvas.containsPoint(point: Offset): Boolean {
    return  point.x > mmLeft &&
            point.x < mmRight &&
            point.y > mmTop &&
            point.y < mmBottom
}

fun DeviceOnCanvas.containsPoint(point: Offset): Boolean {
    return  point.x > mmLeft &&
            point.x < mmRight &&
            point.y > mmTop &&
            point.y < mmBottom
}

fun DeviceOnCanvas.isIntersecting(other: DeviceOnCanvas): Boolean {
    return  mmLeft <= other.mmRight &&
            mmRight >= other.mmLeft &&
            mmTop <= other.mmBottom &&
            mmBottom >= other.mmTop
}

fun DeviceOnCanvas.isIntersectingAny(others: List<DeviceOnCanvas>): Boolean {
    return others.any { this.isIntersecting(it) }
}