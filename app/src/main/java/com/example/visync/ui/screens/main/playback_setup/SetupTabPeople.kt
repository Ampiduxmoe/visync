package com.example.visync.ui.screens.main.playback_setup

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.visync.R
import com.example.visync.messaging.SyncBallMessage
import com.example.visync.metadata.VideoMetadata
import com.example.visync.ui.screens.settings.getProfilePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import kotlin.math.absoluteValue

@Composable
fun SetupTabPeople(
    isUserHost: Boolean,
    isAdvertising: Boolean,
    hostAsWatcher: Watcher,
    meAsWatcher: Watcher,
    notApprovedWatchers: List<Watcher>,
    approvedWatchers: List<Watcher>,
    watcherPings: List<SingleEndpointPings>?,
    videoMetadata: VideoMetadata?,
    positionsEditor: DevicePositionsEditor?,
    saveDevicePositions: (DevicePositionsEditor) -> Unit,
    sendSyncBall: suspend (Offset, Offset) -> Unit,
    setGuestCallbacks: (GuestSpecificCallbacks) -> Unit,
    notApprovedWatcherModifier: (Watcher) -> Modifier,
    approvedWatcherModifier: (Watcher) -> Modifier,
    modifier: Modifier = Modifier
) {
    val watcherColor: (Watcher) -> Color = { watcher ->
        when (watcher.messagingVersion != meAsWatcher.messagingVersion) {
            true -> Color.Red // versions conflict
            false -> Color.Unspecified // versions match
        }
    }
    val watcherText: (Watcher) -> String = { watcher ->
        val maybeHost = if (watcher === hostAsWatcher) " (host)" else ""
        val maybeMe = if (watcher === meAsWatcher) " (me)" else ""
        "${watcher.username} [${watcher.endpointId}]$maybeHost$maybeMe"
    }
    var showDevicesPositionEditor by remember { mutableStateOf(false) }
    if (showDevicesPositionEditor && positionsEditor != null) {
        DevicesPositionConfigurationEditor(
            positionsEditor = positionsEditor,
            saveDevicePositions = saveDevicePositions,
            closeEditor = { showDevicesPositionEditor = false }
        )
    }

    val infiniteTransition = rememberInfiniteTransition()
    val connectingAnimationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    var dragStartPos by remember { mutableStateOf(Offset.Zero) }
    var dragEndPos by remember { mutableStateOf(Offset.Zero) }
    var syncBall by remember { mutableStateOf<Offset?>(null) }
    var syncBallLaunchTimestamp by remember { mutableLongStateOf(0L) }
    var syncBallVelocity by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()
    var syncBallHandlerJob by remember { mutableStateOf<Job?>(null) }
    val launchBallAsHost = { initialPosition: Offset, initialVelocity: Offset ->
        coroutineScope.launch(Dispatchers.IO) {
            syncBallHandlerJob?.cancelAndJoin()
            sendSyncBall(initialPosition, initialVelocity) // suspend call to wait until all receivers have the message
            syncBall = initialPosition
            syncBallLaunchTimestamp = getCurrentTimestamp()
            syncBallVelocity = initialVelocity
            syncBallHandlerJob = launch {
                var prevTimestamp = syncBallLaunchTimestamp
                while (true) {
                    if (syncBall == null) { break }
                    val currentTimestamp = getCurrentTimestamp()
                    val deltaTime = currentTimestamp - prevTimestamp
                    prevTimestamp = currentTimestamp
                    syncBall = syncBall?.let { it + syncBallVelocity * deltaTime.toFloat() / 1000f }
                    syncBallVelocity *= 0.95f
                    if (syncBallVelocity.getDistanceSquared() < 0.01f) {
                        syncBallVelocity = Offset.Zero
                    }
                    delay(16L)
                }
            }
            launch {
                delay(3000L)
                syncBallHandlerJob?.cancelAndJoin()
                syncBallHandlerJob = null
                syncBall = null
            }
        }
    }
    val launchBallAsGuest = { initialPosition: Offset, initialVelocity: Offset ->
        coroutineScope.launch(Dispatchers.IO) {
            syncBallHandlerJob?.cancelAndJoin()
            syncBall = initialPosition
            syncBallLaunchTimestamp = getCurrentTimestamp()
            syncBallVelocity = initialVelocity
            syncBallHandlerJob = launch {
                var prevTimestamp = syncBallLaunchTimestamp
                while (true) {
                    if (syncBall == null) { break }
                    val currentTimestamp = getCurrentTimestamp()
                    val deltaTime = currentTimestamp - prevTimestamp
                    prevTimestamp = currentTimestamp
                    syncBall = syncBall?.let { it + syncBallVelocity * deltaTime.toFloat() / 1000f }
                    syncBallVelocity *= 0.95f
                    if (syncBallVelocity.getDistanceSquared() < 0.01f) {
                        syncBallVelocity = Offset.Zero
                    }
                    delay(16L)
                }
            }
            launch {
                delay(3000L)
                syncBallHandlerJob?.cancelAndJoin()
                syncBallHandlerJob = null
                syncBall = null
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!isUserHost) {
            Log.d("Test", "there goes sync ball callback set action")
            return@LaunchedEffect
            setGuestCallbacks(object: EmptyGuestSpecificCallbacks() {
                override fun onSyncBallMessage(sender: EndpointInfo, message: SyncBallMessage) {
                    coroutineScope.launch {
                        val myPingEntry = message.pingData
                            .find { it.endpointId == meAsWatcher.endpointId } ?: return@launch
                        val maxPing = message.pingData
                            .maxBy { it.pingData.weightedAverage }.pingData.weightedAverage
                        val pingDelta = maxPing - myPingEntry.pingData.weightedAverage
                        delay(pingDelta.toLong() / 2)
                        launchBallAsGuest(
                            /* initialPosition = */ Offset(x = message.posX, y = message.posY),
                            /* initialVelocity = */ Offset(x = message.velocityX, y = message.velocityY)
                        )
                    }
                }
            })
        }
    }
    Column(
        modifier = if (!isUserHost) modifier
            else (
                modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragStartPos = it
                            dragEndPos = it
                        },
                        onDragEnd = {
                            launchBallAsHost(
                                /* initialPosition = */ dragStartPos,
                                /* initialVelocity = */ (dragEndPos - dragStartPos) * -6f
                            )
                        },
                        onDrag = { _, dragAmount ->
                            dragEndPos += dragAmount
                        },
                    )
                }
            )
            .drawBehind {
                val localSyncBallCopy = syncBall ?: return@drawBehind
                drawCircle(
                    color = Color.Yellow,
                    radius = 50f,
                    center = localSyncBallCopy,
                    alpha = 0.5f,
                    style = Fill,
                )
            }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("requests:")
            if (isAdvertising) {
                Icon(
                    painter = painterResource(
                        id = R.drawable.ic_cut_circle
                    ),
                    contentDescription = stringResource(
                        id = R.string.desc_loading_icon
                    ),
                    modifier = Modifier.graphicsLayer {
                        this.rotationZ = connectingAnimationAngle
                    }
                )
            }
            for (watcher in notApprovedWatchers) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text(
                        text = watcherText(watcher),
                        color = watcherColor(watcher),
                        modifier = notApprovedWatcherModifier(watcher)
                    )
                    if (watcherPings != null) {
                        val avgPing = watcherPings.find { it.endpointId == watcher.endpointId }
                        if (avgPing != null) {
                            Text("${avgPing.pingData.weightedAverage}")
                        }
                    }
                    if (!watcher.canBeApproved) { // still in handshake phase
                        Icon(
                            painter = painterResource(
                                id = R.drawable.ic_cut_circle
                            ),
                            contentDescription = stringResource(
                                id = R.string.desc_loading_icon
                            ),
                            modifier = Modifier.graphicsLayer {
                                this.rotationZ = connectingAnimationAngle
                            }
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("approved:")
            for (watcher in approvedWatchers) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text(
                        text = watcherText(watcher),
                        color = watcherColor(watcher),
                        modifier = approvedWatcherModifier(watcher)
                    )
                    if (watcherPings != null) {
                        val avgPing = watcherPings.find { it.endpointId == watcher.endpointId }
                        if (avgPing != null) {
                            Text("${avgPing.pingData.weightedAverage}")
                        }
                    }
                }
            }
        }
        Text(
            text = "show config",
            modifier = Modifier.clickable { showDevicesPositionEditor = true }
        )
    }
}

@Composable
fun DevicesPositionConfigurationEditor(
    positionsEditor: DevicePositionsEditor,
    saveDevicePositions: (DevicePositionsEditor) -> Unit,
    closeEditor: () -> Unit,
) {
    val dialogMinWidth = 280.dp
    val dialogMaxWidth = 560.dp

    val pxScreenWidth = 1080f // TODO: get real device dimensions
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

    val mmOfRealWidthEditorShouldFit = 150f

    var video by remember { mutableStateOf(positionsEditor.videoOnEditor) }
    var devices by remember { mutableStateOf(positionsEditor.devicesOnEditor) }
    var camera by remember { mutableStateOf(positionsEditor.cameraView) }

    LaunchedEffect(positionsEditor.devicesOnEditor) {
        // TODO remove or add local device
    }

    val localDensity = LocalDensity.current.density
    val mmToPxMultiplier = pxEditorWidth / mmOfRealWidthEditorShouldFit * camera.zoom
    val pxToMmMultiplier = mmOfRealWidthEditorShouldFit / (pxEditorWidth * camera.zoom)
    val mmToDpMultiplier = mmToPxMultiplier / localDensity
    val dpToMmMultiplier = localDensity / mmToPxMultiplier

    val mmToPx = { mm: Float -> (mm * mmToPxMultiplier) }
    val pxToMm = { px: Float -> (px * pxToMmMultiplier) }
    val mmToDp = { mm: Float -> (mm * mmToDpMultiplier).dp }
    val dpToMm = { dp: Dp -> (dp.value * dpToMmMultiplier) }

    var selectedDevicesWatcherInfo by remember {
        mutableStateOf(emptyList<WatcherInfo>())
    }
    val addDeviceToSelection = { device: DeviceOnEditor ->
        selectedDevicesWatcherInfo = selectedDevicesWatcherInfo + device.watcherInfo
    }
    val removeDeviceFromSelection = { device: DeviceOnEditor ->
        selectedDevicesWatcherInfo = selectedDevicesWatcherInfo.filter { it != device.watcherInfo }
    }
    var isVideoSelected by remember { mutableStateOf(false) }
    val clearDeviceSelection = {
        selectedDevicesWatcherInfo = emptyList()
        isVideoSelected = false
    }

    var mmPointerOnEditor by remember { mutableStateOf(Offset(0f, 0f)) }
    var truePointerPos by remember { mutableStateOf(Offset(0f, 0f)) }

    val isPointInSelection: (Offset) -> Boolean = { mmPoint: Offset ->
        val selectedDevices = devices
            .filter { it.watcherInfo in selectedDevicesWatcherInfo }
        val isOnDevice = selectedDevices.any { it.containsPoint(mmPoint) }
        val isOnVideo = isVideoSelected && video.containsPoint(mmPoint)
        isOnDevice || isOnVideo
    }

    val saveConfigAndCloseEditor = {
        saveDevicePositions(DevicePositionsEditor(video, devices, camera))
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
                        val localPxToMm =
                            { px: Float -> px * mmOfRealWidthEditorShouldFit / (pxEditorWidth * camera.zoom) }
                        detectTapGestures(
                            onTap = {
                                truePointerPos = it
                                Log.d("true pointer", "${it}")
                                mmPointerOnEditor = Offset(
                                    x = localPxToMm(it.x) + camera.mmViewOffsetX,
                                    y = localPxToMm(it.y) + camera.mmViewOffsetY,
                                )
                                val selectedDevice = devices.find { device ->
                                    device.containsPoint(mmPointerOnEditor)
                                }
                                if (selectedDevice != null) {
                                    if (selectedDevice.watcherInfo in selectedDevicesWatcherInfo) {
                                        removeDeviceFromSelection(selectedDevice)
                                    } else {
                                        addDeviceToSelection(selectedDevice)
                                    }
                                } else {
                                    if (!video.containsPoint(mmPointerOnEditor)) {
                                        clearDeviceSelection() // user clicked elsewhere
                                    }
                                }

                            },
                            onLongPress = {
                                mmPointerOnEditor = Offset(
                                    x = localPxToMm(it.x) + camera.mmViewOffsetX,
                                    y = localPxToMm(it.y) + camera.mmViewOffsetY,
                                )
                                if (video.containsPoint(mmPointerOnEditor)) {
                                    isVideoSelected = !isVideoSelected
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val localPxToMm =
                                { px: Float -> px * mmOfRealWidthEditorShouldFit / (pxEditorWidth * camera.zoom) }
                            truePointerPos = centroid
                            mmPointerOnEditor = Offset(
                                x = localPxToMm(centroid.x) + camera.mmViewOffsetX,
                                y = localPxToMm(centroid.y) + camera.mmViewOffsetY,
                            )
                            val mmPan = Offset(
                                x = localPxToMm(pan.x),
                                y = localPxToMm(pan.y)
                            )
                            var nothingWasSelected = true
                            if (selectedDevicesWatcherInfo.isNotEmpty()) {
                                val selectedDevices = devices
                                    .filter { it.watcherInfo in selectedDevicesWatcherInfo }
                                devices = devices.withMovedDevices(selectedDevices, mmPan)
                                nothingWasSelected = false
                            }
                            if (isVideoSelected) {
                                video = video
                                    .withAddedOffset(mmPan.x, mmPan.y)
                                    .zoomedBy(zoom, video.mmCenter)
                                nothingWasSelected = false
                                mmPointerOnEditor = video.mmCenter
                            }
                            if (nothingWasSelected) {
                                camera = camera
                                    .zoomedBy(zoom, mmPointerOnEditor)
                                    .withAddedOffset(-mmPan.x, -mmPan.y)
                            }
                        }
                    },
            ) {
                val mmCameraPosX = camera.mmViewOffsetX
                val mmCameraPosY = camera.mmViewOffsetY
                val selectedDevices = devices
                    .filter { it.watcherInfo in selectedDevicesWatcherInfo }
                video.let {
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
                    if (isVideoSelected) {
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
                devices.forEach {
                    drawRect(
                        color = Color(it.brushColor),
                        topLeft = Offset(
                            x = mmToPx(it.mmOffsetX - mmCameraPosX),
                            y = mmToPx(it.mmOffsetY - mmCameraPosY)
                        ),
                        size = Size(
                            width = mmToPx(it.mmDeviceWidth),
                            height = mmToPx(it.mmDeviceHeight)
                        ),
                        alpha = 0.5f,
                        style = Fill,
                    )
                    drawRect(
                        color = Color(it.brushColor),
                        topLeft = Offset(
                            x = mmToPx(it.displayLeft - mmCameraPosX),
                            y = mmToPx(it.displayTop - mmCameraPosY)
                        ),
                        size = Size(
                            width = mmToPx(it.mmDisplayWidth),
                            height = mmToPx(it.mmDisplayHeight)
                        ),
                        alpha = 0.5f,
                        style = Fill,
                    )
                    if (it in selectedDevices) {
                        drawRect(
                            color = Color.Magenta,
                            topLeft = Offset(
                                x = mmToPx(it.mmOffsetX - mmCameraPosX),
                                y = mmToPx(it.mmOffsetY - mmCameraPosY)
                            ),
                            size = Size(
                                width = mmToPx(it.mmDeviceWidth),
                                height = mmToPx(it.mmDeviceHeight)
                            ),
                            alpha = 0.5f,
                            style = Stroke(width = 4f),
                        )
                    }
                }
                mmPointerOnEditor.let {
                    val pointerWidth = mmToPx(25f / camera.zoom)
                    val pointerHeight = mmToPx(25f / camera.zoom)
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
                truePointerPos.let {
                    drawCircle(
                        color = Color.Magenta,
                        radius = 50f,
                        center = it,
                        alpha = 0.5f,
                        style = Fill,
                    )
                }
                Offset(x=0f, y=pxEditorHeight/2).let { left ->
                    drawCircle(
                        color = Color.Green,
                        radius = 50f,
                        center = left,
                        alpha = 0.5f,
                        style = Fill,
                    )
                }
                Offset(x=pxEditorWidth/2, y=pxEditorHeight/2).let { center ->
                    drawCircle(
                        color = Color.Green,
                        radius = 50f,
                        center = center,
                        alpha = 0.5f,
                        style = Fill,
                    )
                }
                Offset(x=pxEditorWidth, y=pxEditorHeight/2).let { right ->
                    drawCircle(
                        color = Color.Green,
                        radius = 50f,
                        center = right,
                        alpha = 0.5f,
                        style = Fill,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isVideoSelected) {
                    val resString = "${video.videoMetadata.width}x${video.videoMetadata.height}"
                    val mmString = "${video.mmWidth}mm x ${video.mmHeight}mm"
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "${video.videoMetadata.filename} [$resString] [$mmString]",
                    )
                }
                selectedDevicesWatcherInfo.forEach { watcherInfo ->
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "${watcherInfo.username} [${watcherInfo.endpointId}]",
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
                    val mmCenter = Offset(
                        x = pxToMm(pxEditorWidth / 2) + camera.mmViewOffsetX,
                        y = pxToMm(pxEditorHeight / 2) + camera.mmViewOffsetY
                    )
                    val zoomSteps = remember { Steps(listOf(
                        0.5f, 0.6f, 0.7f, 0.8f, 0.9f,
                        1.0f,
                        1.2f, 1.4f, 1.6f, 1.8f, 2.0f
                    )) }
                    Text(
                        text = "+",
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clickable {
                                mmPointerOnEditor = mmCenter
                                camera = camera.zoomedTo(
                                    targetZoom = zoomSteps.nextToClosestStep(camera.zoom),
                                    mmPivotPoint = mmCenter
                                )
                            }
                    )
                    Text(
                        text = String.format("%.1f", camera.zoom),
                        modifier = Modifier
                    )
                    Text(
                        text = "-",
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clickable {
                                mmPointerOnEditor = mmCenter
                                camera = camera.zoomedTo(
                                    targetZoom = zoomSteps.prevToClosestStep(camera.zoom),
                                    mmPivotPoint = mmCenter
                                )
                            }
                    )
                }
            }
        }
    }
}

private fun getUsername(context: Context): String? {
    val profilePrefs = getProfilePreferences(context)
    val usernameKey = context.getString(R.string.prefs_profile_username)
    return profilePrefs.getString(usernameKey, null)
}

private fun List<DeviceOnEditor>.withMovedDevices(
    devices: List<DeviceOnEditor>,
    offset: Offset
): List<DeviceOnEditor> {
    val (selectedDevices, otherDevices) = this.partition { it in devices }
    val movedVersions = selectedDevices.map {
        it.copy(
            mmOffsetX = it.mmOffsetX + offset.x,
            mmOffsetY = it.mmOffsetY + offset.y
        )
    }
    if (movedVersions.any { it.isIntersectingAny(otherDevices) }) {
        return this
    }
    return this.toMutableList().apply {
        forEachIndexed { index, device ->
            if (device in selectedDevices) {
                val indexInSelection = selectedDevices.indexOf(device)
                this[index] = movedVersions[indexInSelection]
            }
        }
    }
}

class Steps(
    val stepList: List<Float>
) {
    init {
        if (stepList.isEmpty()) { throw IllegalArgumentException() }
    }
    private val minIndex = 0
    private val maxIndex = stepList.count() - 1
    var currentIndex = 0
        private set
    val currentValue
        get() = stepList[currentIndex]

    fun nextToClosestStep(value: Float): Float {
        currentIndex = stepList.withIndex()
            .minBy { (value - it.value).absoluteValue }
            .index.plus(1)
            .coerceAtMost(maxIndex)
        return currentValue
    }

    fun prevToClosestStep(value: Float): Float {
        currentIndex = stepList.withIndex()
            .minBy { (value - it.value).absoluteValue }
            .index.minus(1)
            .coerceAtLeast(minIndex)
        return currentValue
    }
}