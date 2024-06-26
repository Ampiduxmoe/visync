package com.example.visync.ui.screens.main.playback_setup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.visync.R
import com.example.visync.data.videofiles.Videofile
import com.example.visync.messaging.SyncBallMessage
import com.example.visync.metadata.VideoMetadata
import com.example.visync.ui.components.navigation.GenericAlertDialog
import com.example.visync.ui.components.navigation.getGreatestCommonDivisor
import com.example.visync.ui.components.navigation.topInnerShadow
import com.example.visync.ui.screens.player.VisyncPhysicalDevice
import com.example.visync.ui.screens.settings.getProfilePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun SetupTabPeople(
    isUserHost: Boolean,
    isAdvertising: Boolean,
    startAdvertising: (() -> Unit)? = null,
    stopAdvertising: (() -> Unit)? = null,
    startGroupPlayback: (() -> Unit)? = null,
    hostAsWatcher: Watcher,
    selfAsWatcher: Watcher,
    allWatchers: List<Watcher>,
    watcherPings: List<EndpointPingData>?,
    videoMetadata: VideoMetadata?,
    positionsEditor: DevicePositionsEditor?,
    saveDevicePositions: (DevicePositionsEditor) -> Unit,
    sendSyncBall: suspend (Offset, Offset) -> Unit,
    setGuestCallbacks: (GuestSpecificCallbacks) -> Unit,
    notApprovedWatcherModifier: (Watcher) -> Modifier,
    approvedWatcherModifier: (Watcher) -> Modifier,
    toggleIsApproved: ((Watcher) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showWatcherInfoDialog by remember { mutableStateOf(false) }
    var watcherIdToShow by remember { mutableStateOf<String?>(null) }
    val watcherToShow = remember(watcherIdToShow, allWatchers) {
        allWatchers.find { it.endpointId == watcherIdToShow }
    }
    if (showWatcherInfoDialog && watcherToShow != null) {
        WatcherInfoDialog(
            watcher = watcherToShow,
            hostWatcher = hostAsWatcher,
            isUserHost = isUserHost,
            toggleIsApproved = { toggleIsApproved?.invoke(watcherToShow) },
            close = { showWatcherInfoDialog = false }
        )
    }
    var showDevicesPositionEditor by remember { mutableStateOf(false) }
    if (showDevicesPositionEditor && positionsEditor != null) {
        DevicesPositionConfigurationEditor(
            positionsEditor = positionsEditor,
            saveDevicePositions = saveDevicePositions,
            closeEditor = { showDevicesPositionEditor = false }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(
        label = "connectionInProgressTransition"
    )
    val connectingAnimationRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ), label = "connectionInProgressRotationAngle"
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
                            .find { it.endpointId == selfAsWatcher.endpointId } ?: return@launch
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
        modifier = if (!isUserHost) modifier else modifier
//            else (
//                modifier
//                .pointerInput(Unit) {
//                    detectDragGestures(
//                        onDragStart = {
//                            dragStartPos = it
//                            dragEndPos = it
//                        },
//                        onDragEnd = {
//                            launchBallAsHost(
//                                /* initialPosition = */ dragStartPos,
//                                /* initialVelocity = */ (dragEndPos - dragStartPos) * -6f
//                            )
//                        },
//                        onDrag = { _, dragAmount ->
//                            dragEndPos += dragAmount
//                        },
//                    )
//                }
//            )
//            .drawBehind {
//                val localSyncBallCopy = syncBall ?: return@drawBehind
//                drawCircle(
//                    color = Color.Yellow,
//                    radius = 50f,
//                    center = localSyncBallCopy,
//                    alpha = 0.5f,
//                    style = Fill,
//                )
//            }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            for (watcher in allWatchers) {
                val avgPing = watcherPings
                    ?.find { it.endpointId == watcher.endpointId }
                    ?.pingData
                    ?.weightedAverage
                    ?.toLong()
                WatcherItem(
                    watcher = watcher,
                    isHost = watcher == hostAsWatcher,
                    isSelf = watcher == selfAsWatcher,
                    pingAvg = if (watcher == hostAsWatcher) null else avgPing,
                    connectingAnimationRotationAngle = connectingAnimationRotationAngle,
                    showWatcherInfo = {
                        watcherIdToShow = watcher.endpointId
                        showWatcherInfoDialog = true
                    },
                    approveClickModifier = if (watcher.isApproved) approvedWatcherModifier(watcher) else notApprovedWatcherModifier(watcher),
                )
            }
        }
        Column(

        ) {
            val titleStyle = MaterialTheme.typography.titleSmall
            val subTitleStyle = MaterialTheme.typography.bodySmall
            val iconContainerSize = 36.dp
            val firstGradientColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
            val secondGradientColor = MaterialTheme.colorScheme.secondaryContainer
            val contentColor = contentColorFor(secondGradientColor)
            if (isUserHost) {
                var showLoadingIconForRoomVisibility by remember(isAdvertising) { mutableStateOf(false) }
                GradientButton(
                    onClick = {
                        if (isAdvertising) {
                            stopAdvertising?.invoke()
                        } else {
                            startAdvertising?.invoke()
                        }
                        showLoadingIconForRoomVisibility = true
                    },
                    gradient = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to firstGradientColor,
                            1f to secondGradientColor,
                        ),
                    ),
                    shape = RectangleShape,
                    contentColor = contentColor,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Toggle room visibility",
                            style = titleStyle
                        )
                        if (isAdvertising) {
                            Text(
                                text = "Status: anybody can connect",
                                style = subTitleStyle
                            )
                        } else {
                            Text(
                                text = "Status: no one can connect",
                                style = subTitleStyle
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(iconContainerSize)
                    ) {
                        if (showLoadingIconForRoomVisibility) {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_cut_circle
                                ),
                                contentDescription = stringResource(
                                    id = R.string.desc_loading_icon
                                ),
                                modifier = Modifier
                                    .graphicsLayer {
                                        this.rotationZ = connectingAnimationRotationAngle
                                    }
                            )
                        } else {
                            if (isAdvertising) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_visibility_on),
                                    contentDescription = stringResource(id = R.string.desc_room_is_visible)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_visibility_off),
                                    contentDescription = stringResource(id = R.string.desc_room_is_not_visible)
                                )
                            }
                        }
                    }
                }
            }
            GradientButton(
                onClick = {
                    showDevicesPositionEditor = true
                },
                gradient = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to secondGradientColor,
                        1f to firstGradientColor,
                    ),
                ),
                shape = RectangleShape,
                contentColor = contentColor,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(iconContainerSize)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_device_positions_configuration),
                        contentDescription = stringResource(id = R.string.desc_device_positions_configuration_editor)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(

                ) {
                    Text(
                        text = "View configuration",
                        style = titleStyle
                    )
                    Text(
                        text = stringResource(id = R.string.desc_device_positions_configuration_editor),
                        style = subTitleStyle
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            if (isUserHost) {
                GradientButton(
                    onClick = {
                        startGroupPlayback?.invoke()
                    },
                    gradient = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to firstGradientColor,
                            1f to secondGradientColor,
                        ),
                    ),
                    shape = RectangleShape,
                    contentColor = contentColor,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Play!",
                            style = titleStyle
                        )
                        Text(
                            text = stringResource(id = R.string.desc_start_group_playback),
                            style = subTitleStyle
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(iconContainerSize)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_arrow),
                            contentDescription = stringResource(id = R.string.desc_start_group_playback)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatcherItem(
    watcher: Watcher,
    isHost: Boolean,
    isSelf: Boolean,
    pingAvg: Long?,
    connectingAnimationRotationAngle: Float,
    showWatcherInfo: () -> Unit,
    errorColor: Color = if (MaterialTheme.colorScheme.isLight()) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.errorContainer,
    @SuppressLint("ModifierParameter")
    textSizeModifier: Modifier = Modifier
        .fillMaxWidth(0.75f)
        .padding(vertical = 12.dp),
    approveClickModifier: Modifier = Modifier,
) {
    val selectionBackground = MaterialTheme.colorScheme.secondaryContainer
    val selectionBackgroundModifier = when {
        watcher.isApproved -> Modifier.background(selectionBackground)
        else -> Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = selectionBackgroundModifier
            .fillMaxWidth()
            .clickable { showWatcherInfo() }
            .padding(horizontal = 24.dp)
    ) {
        val iconContainerSize = 36.dp
        val usernameStyle = MaterialTheme.typography.titleMedium
        val additionalInfoStyle = MaterialTheme.typography.bodyMedium
        val loadingIconSize = 16.dp
        Column(
            modifier = textSizeModifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val mehostString = when {
                    isSelf && isHost -> " (me, host)"
                    isSelf -> " (me)"
                    isHost -> " (host)"
                    else -> ""
                }
                Text(
                    text = "${watcher.username}${mehostString}",
                    color = if (watcher.canBeApproved) Color.Unspecified else errorColor,
                    style = usernameStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!watcher.hasCompletedHandshake) { // still in handshake phase
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(
                            id = R.drawable.ic_cut_circle
                        ),
                        contentDescription = stringResource(
                            id = R.string.desc_loading_icon
                        ),
                        modifier = Modifier
                            .size(loadingIconSize)
                            .graphicsLayer {
                                this.rotationZ = connectingAnimationRotationAngle
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier.size(loadingIconSize)
                    )
                }
            }

            Row {
                val endpointId = watcher.endpointId.ifEmpty { "UNKNOWN" }
                Text(
                    text = "ID: $endpointId",
                    style = additionalInfoStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(16.dp))
                if (pingAvg != null) {
                    Text(
                        text = "Ping: $pingAvg",
                        style = additionalInfoStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconContainerSize)
                .clip(shape = CircleShape)
                .then(approveClickModifier)
        ) {
            if (watcher.isApproved) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_monitor),
                    contentDescription = stringResource(id = R.string.desc_watcher_approved),
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_no_monitor),
                    contentDescription = stringResource(id = R.string.desc_watcher_not_approved),
                )
            }
        }

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
    val context = LocalContext.current
    val loadingIcon = ImageVector.vectorResource(R.drawable.ic_cut_circle)
    val loadingIconPainter = rememberVectorPainter(image = loadingIcon)
    var videoThumbnail by remember {
        mutableStateOf(ImageBitmap.imageResource(context.resources, R.drawable.black_square))
    }
    val infiniteTransition = rememberInfiniteTransition(
        label = "loadingInProgressRotationAngle"
    )
    val loadingAnimationRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ), label = "loadingInProgressRotationAngle"
    )
    var videoThumbnailShader by remember {
        mutableStateOf(ImageShader(
            image = videoThumbnail,
            tileModeX = TileMode.Mirror,
            tileModeY = TileMode.Mirror,
        ))
    }
    val videoOffset = remember(
        video.mmOffsetX,
        video.mmOffsetY,
        camera.mmViewOffsetX,
        camera.mmViewOffsetY
    ) {
        Offset(
            x = mmToPx(video.mmOffsetX - camera.mmViewOffsetX),
            y = mmToPx(video.mmOffsetY - camera.mmViewOffsetY),
        )
    }
    val videoThumbnailScale = remember(
        video,
        camera.zoom,
        videoThumbnail.width,
        videoThumbnail.height
    ) {
        val videoOnCanvasWidth = mmToPx(video.mmWidth)
        val videoOnCanvasHeight = mmToPx(video.mmHeight)
        val scaleToFitWidth = videoOnCanvasWidth / videoThumbnail.width
        val scaleToFitHeight = videoOnCanvasHeight / videoThumbnail.height
        min(scaleToFitWidth, scaleToFitHeight)
    }
    val videoThumbnailMatrix = remember(videoOffset.x, videoOffset.y, videoThumbnailScale) {
        val m = Matrix()
        m.preTranslate(videoOffset.x, videoOffset.y)
        m.postScale(videoThumbnailScale, videoThumbnailScale, videoOffset.x, videoOffset.y)
        videoThumbnailShader.setLocalMatrix(m)
        m
    }
    var videoThumbnailBrush by remember {
        mutableStateOf(ShaderBrush(
            videoThumbnailShader
        ))
    }
    var isThumbnailLoading by remember { mutableStateOf(false) }
    LaunchedEffect(video.videoUri) {
        launch(Dispatchers.Default) {
            isThumbnailLoading = true
            videoThumbnail = video.createThumbnail(context)
            videoThumbnailShader = ImageShader(
                image = videoThumbnail,
                tileModeX = TileMode.Mirror,
                tileModeY = TileMode.Mirror,
            )
            videoThumbnailShader.setLocalMatrix(videoThumbnailMatrix)
            videoThumbnailBrush = ShaderBrush(videoThumbnailShader)
            isThumbnailLoading = false
        }
    }

    val backgroundTileImage = ImageBitmap.imageResource(id = R.drawable.stripes_tile)
    val backgroundImageShader = remember {
        ImageShader(
            image = backgroundTileImage,
            tileModeX = TileMode.Repeated,
            tileModeY = TileMode.Repeated,
        )
    }
    val backgroundImageDefaultScale = 0.4f
    val offscreenTileCount = 1
    val backgroundImageScale = remember(camera.zoom) {
        backgroundImageDefaultScale * camera.zoom
    }
    val tileSize = remember(backgroundImageScale) {
        backgroundTileImage.width * backgroundImageScale
    }
    val backgroundImageOffset = remember(camera.mmViewOffsetX, camera.mmViewOffsetY) {
        val cameraOffset = Offset(
            x = -mmToPx(camera.mmViewOffsetX),
            y = -mmToPx(camera.mmViewOffsetY),
        )
        val fullTilesOffsetX = (cameraOffset.x / tileSize).toInt()
        val fullTilesOffsetY = (cameraOffset.y / tileSize).toInt()
        Offset(
            x = cameraOffset.x - (fullTilesOffsetX + offscreenTileCount) * tileSize,
            y = cameraOffset.y - (fullTilesOffsetY + offscreenTileCount) * tileSize,
        )
    }
    val backgroundTileMatrix = remember(
        backgroundImageOffset.x,
        backgroundImageOffset.y ,
        backgroundImageScale
    ) {
        val m = Matrix()
        m.preTranslate(
            backgroundImageOffset.x,
            backgroundImageOffset.y,
        )
        m.postScale(
            backgroundImageScale,
            backgroundImageScale,
            backgroundImageOffset.x,
            backgroundImageOffset.y,
        )
        m
    }
    backgroundImageShader.setLocalMatrix(backgroundTileMatrix)
    val tilePictureBrush = remember {
        ShaderBrush(
            backgroundImageShader
        )
    }

    val bgTintColor = MaterialTheme.colorScheme.primary
    val submenuTitleBgColor = bgTintColor
    val submenuTitleContentColor = contentColorFor(submenuTitleBgColor)
    val submenuContentBgColor = MaterialTheme.colorScheme.surface
    val submenuContentColor = contentColorFor(submenuContentBgColor)
    val textMeasurer = rememberTextMeasurer()
    val baseEndpointIdStyle = MaterialTheme.typography.titleMedium
    val endpointIdStyle = remember {
        baseEndpointIdStyle.copy(
            fontSize = TextUnit(6f * camera.zoom, TextUnitType.Em)
        )
    }
    val textLayouts = remember(devices, camera.zoom) {
        devices.map {
            it.watcherInfo.endpointId to textMeasurer.measure(
                it.watcherInfo.endpointId,
                endpointIdStyle
            )
        }
    }.toMap()

    Dialog(
        onDismissRequest = saveConfigAndCloseEditor,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
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
//                                Log.d("true pointer", "${it}")
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
                drawWithLayer {
                    val bgSize = Size(
                        width = size.width + tileSize * (offscreenTileCount * 2),
                        height = size.height + tileSize * (offscreenTileCount * 2)
                    )
                    val bgAlpha = 0.5f
                    drawRect(
                        brush = tilePictureBrush,
                        topLeft = backgroundImageOffset,
                        size = bgSize,
                        alpha = 1.0f,
//                        colorFilter = ColorFilter.tint(bgTintColor, blendMode = BlendMode.Overlay)
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = backgroundImageOffset,
                        size = bgSize,
                        alpha = 1f - bgAlpha,
                        blendMode = BlendMode.DstOut
                    )
                }

                val selectionColor = Color.White
                val selectionStrokeWidth = 48f * camera.zoom
                val selectionAlpha = 0.66f
                val mmCameraPosX = camera.mmViewOffsetX
                val mmCameraPosY = camera.mmViewOffsetY
                val selectedDevices = devices
                    .filter { it.watcherInfo in selectedDevicesWatcherInfo }
                drawWithLayer {
                    video.let {
                        val videoTopLeft = Offset(
                            x = mmToPx(it.mmOffsetX - mmCameraPosX),
                            y = mmToPx(it.mmOffsetY - mmCameraPosY)
                        )
                        val videoSize = Size(
                            width = mmToPx(it.mmWidth),
                            height = mmToPx(it.mmHeight)
                        )
                        val shadowSpread = videoSize.minDimension / 128
                        drawShadow(
                            color = Color.Black,
                            offset = videoTopLeft,
                            size = videoSize,
                            cornerRadius = 0f,
                            spread = shadowSpread,
                            blur = shadowSpread * 4,
                        )
                        drawRect(
                            brush = videoThumbnailBrush,
                            topLeft = videoTopLeft,
                            size = videoSize,
                        )
                        val loadingIconDimension = videoSize.minDimension / 3
                        val loadingIconTopLeft = Offset(
                            x = videoTopLeft.x + videoSize.width / 2 - loadingIconDimension / 2,
                            y = videoTopLeft.y + videoSize.height / 2 - loadingIconDimension / 2,
                        )
                        val loadingIconSize = Size(
                            width = loadingIconDimension,
                            height = loadingIconDimension,
                        )
                        val rotationPivotPoint = Offset(
                            x = loadingIconSize.width / 2,
                            y = loadingIconSize.height / 2,
                        )
                        if (isThumbnailLoading) {
                            translate(left = loadingIconTopLeft.x, top = loadingIconTopLeft.y) {
                                rotate(degrees = loadingAnimationRotationAngle, pivot = rotationPivotPoint) {
                                    with(loadingIconPainter) {
                                        draw(
                                            size = loadingIconSize,
                                            alpha = 0.67f,
                                            colorFilter = ColorFilter.tint(Color.White),
                                        )
                                    }
                                }
                            }
                        }
                        if (isVideoSelected) {
                            val videoBlurSpread = videoSize.minDimension / 4
                            drawInnerShadow(
                                color = selectionColor,
                                offset = videoTopLeft,
                                size = videoSize,
                                cornerRadius = 0f,
                                spread = videoBlurSpread,
                                blur = videoBlurSpread * 2,
                            )
                        }
                    }
                    devices.forEach {
                        val deviceColor = Color(it.brushColor)

                        val baseDeviceCornerRadius = 24f
                        val baseDisplayCornerRadius = 16f
                        val deviceCornerRadius = baseDeviceCornerRadius * camera.zoom
                        val displayCornerRadius = baseDisplayCornerRadius * camera.zoom

                        val deviceTopLeft = Offset(
                            x = mmToPx(it.mmOffsetX - mmCameraPosX),
                            y = mmToPx(it.mmOffsetY - mmCameraPosY)
                        )
                        val deviceSize = Size(
                            width = mmToPx(it.mmDeviceWidth),
                            height = mmToPx(it.mmDeviceHeight)
                        )

                        val halfDeviceSize = Offset(
                            deviceSize.width / 2,
                            deviceSize.height / 2
                        )
                        val textLayout = textLayouts[it.watcherInfo.endpointId]!!
                        val halfTextSize = Offset(
                            textLayout.size.width / 2f,
                            textLayout.size.height / 2f
                        )
                        drawWithLayer {
                            val colorMult = 1.33f
                            val deviceColorVariant = deviceColor.copy(
                                red = min(deviceColor.red * colorMult, 1f),
                                green = min(deviceColor.green * colorMult, 1f),
                                blue = min(deviceColor.blue * colorMult, 1f),
                            )
                            val gradientBrush = Brush.linearGradient(
                                colors = listOf(
                                    deviceColorVariant,
                                    deviceColorVariant,
                                    deviceColor,
                                    deviceColorVariant,
                                    deviceColorVariant,
                                )
                            )
                            drawRoundRect(
                                brush = gradientBrush,
                                topLeft = deviceTopLeft,
                                size = deviceSize,
                                alpha = 0.75f,
                                style = Fill,
                                cornerRadius = CornerRadius(
                                    deviceCornerRadius,
                                    deviceCornerRadius
                                ),
                            )
                            drawRoundRect(
                                color = Color(it.brushColor),
                                topLeft = Offset(
                                    x = mmToPx(it.displayLeft - mmCameraPosX),
                                    y = mmToPx(it.displayTop - mmCameraPosY)
                                ),
                                size = Size(
                                    width = mmToPx(it.mmDisplayWidth),
                                    height = mmToPx(it.mmDisplayHeight)
                                ),
                                alpha = 0.7f,
                                style = Fill,
                                cornerRadius = CornerRadius(
                                    displayCornerRadius,
                                    displayCornerRadius
                                ),
                                blendMode = BlendMode.DstOut
                            )
                        }
                        drawText(
                            textMeasurer = textMeasurer,
                            text = it.watcherInfo.endpointId,
                            style = endpointIdStyle,
                            topLeft = deviceTopLeft + halfDeviceSize - halfTextSize,
                            size = deviceSize,
                            blendMode = BlendMode.Overlay
                        )
                        if (it in selectedDevices) {
                            drawInnerShadow(
                                color = selectionColor,
                                offset = deviceTopLeft,
                                size = deviceSize,
                                cornerRadius = deviceCornerRadius,
                                spread = selectionStrokeWidth,
                                blur = selectionStrokeWidth * 2,
                            )
                        }
                    }
                }
                val drawDebug = false
                if (drawDebug) {
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
            }
            val iconSize = 52.dp
            var currentDialog by remember { mutableStateOf(ConfigurationEditorDialogs.NONE) }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val isAnythingSelected = isVideoSelected || selectedDevicesWatcherInfo.isNotEmpty()
                if (isAnythingSelected && currentDialog == ConfigurationEditorDialogs.NONE) {
                    Column(
                        modifier = Modifier
                            .background(submenuTitleBgColor)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        var selectionIndex = 1
                        var selectionString = ""
                        if (isVideoSelected) {
                            selectionString += video.videoMetadata.filename
                            selectionIndex++
                        }
                        selectedDevicesWatcherInfo.forEach { watcherInfo ->
                            selectionString += when (selectionIndex) {
                                1 -> watcherInfo.username
                                else -> ", ${watcherInfo.username}"
                            }
                            selectionIndex++
                        }
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Selected:",
                            color = submenuTitleContentColor,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = selectionString,
                            color = submenuTitleContentColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))

                val submenuTitleModifier = Modifier
                    .background(submenuTitleBgColor)
                    .padding(top = 12.dp, bottom = 12.dp, start = 24.dp, end = 24.dp)
                val submenuContentModifier = Modifier
                    .background(submenuContentBgColor)
                    .padding(top = 8.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
                if (currentDialog == ConfigurationEditorDialogs.HELP) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = submenuTitleModifier
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Help",
                                color = submenuTitleContentColor,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(iconSize * 0.67f)
                                    .clip(shape = CircleShape)
                                    .background(submenuTitleContentColor)
                                    .clickable { currentDialog = ConfigurationEditorDialogs.NONE }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = stringResource(id = R.string.desc_configuration_close_icon),
                                    tint = submenuTitleBgColor,
                                )
                            }
                        }
                        Column(
                            modifier = submenuContentModifier
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "This is the window where you can decide what part of the video each device should display.",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Controls:",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "To move this view, do a drag gesture.",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "To zoom in/out, do a pinch gesture.",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "To select a device, tap on it.",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "To select/unselect a device, tap on it.",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "To select/unselect the video, long press on it.",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "To resize selected video, do a pinch gesture.",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "To move selection, drag.",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (currentDialog == ConfigurationEditorDialogs.INFO) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = submenuTitleModifier
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Information",
                                color = submenuTitleContentColor,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(iconSize * 0.67f)
                                    .clip(shape = CircleShape)
                                    .background(submenuTitleContentColor)
                                    .clickable { currentDialog = ConfigurationEditorDialogs.NONE }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = stringResource(id = R.string.desc_configuration_close_icon),
                                    tint = submenuTitleBgColor,
                                )
                            }
                        }
                        Column(
                            modifier = submenuContentModifier
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Video:",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val videoName = video.videoMetadata.filename
                            val videoDurationSeconds = video.videoMetadata.duration / 1000
                            val videoDurationMillis = video.videoMetadata.duration
                            val videoDuration =
                                "${videoDurationSeconds}s (${videoDurationMillis}ms)"
                            val videoWidth = video.videoMetadata.width.toInt()
                            val videoHeight = video.videoMetadata.height.toInt()
                            val videoRes = "${videoWidth}x${videoHeight}"
                            val videoMmWidthString = "%.2f".format(video.mmWidth)
                            val videoMmHeightString = "%.2f".format(video.mmHeight)
                            val videoDimensions =
                                "${videoMmWidthString}mm x ${videoMmHeightString}mm"
                            Column {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Name: $videoName",
                                    color = submenuContentColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Duration: $videoDuration",
                                    color = submenuContentColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Resolution: $videoRes",
                                    color = submenuContentColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "Current dimensions: $videoDimensions",
                                    color = submenuContentColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Connected devices:",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            for (device in devices) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            modifier = Modifier.background(color = Color(device.brushColor)),
                                            text = "     ",
                                            color = submenuContentColor,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val endpointId =
                                            device.watcherInfo.endpointId.ifEmpty { "UNKNOWN" }
                                        Text(
                                            text = "${device.watcherInfo.username} [$endpointId]",
                                            color = submenuContentColor,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (currentDialog == ConfigurationEditorDialogs.SETTINGS) {
                    Column(
                        modifier = Modifier.fillMaxHeight(0.4f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = submenuTitleModifier,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Settings",
                                color = submenuTitleContentColor,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(iconSize * 0.67f)
                                    .clip(shape = CircleShape)
                                    .background(submenuTitleContentColor)
                                    .clickable { currentDialog = ConfigurationEditorDialogs.NONE }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = stringResource(id = R.string.desc_configuration_close_icon),
                                    tint = submenuTitleBgColor,
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .topInnerShadow(
                                    height = 16.dp,
                                    alpha = 0.05f,
                                    color = submenuContentColor
                                )
                                .verticalScroll(rememberScrollState())
                                .then(submenuContentModifier)
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "View:",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DecoratedSetting {
                                Column {
                                    Text(
                                        text = "zoom:  %.2fx".format(camera.zoom),
                                        color = submenuContentColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Slider(
                                        value = camera.zoom,
                                        onValueChange = {
                                            val mmCenter = Offset(
                                                x = pxToMm(pxEditorWidth / 2) + camera.mmViewOffsetX,
                                                y = pxToMm(pxEditorHeight / 2) + camera.mmViewOffsetY
                                            )
                                            camera = camera.zoomedTo(
                                                targetZoom = it,
                                                mmPivotPoint = mmCenter,
                                            )
                                        },
                                        valueRange = EditorCameraView.MIN_ZOOM..EditorCameraView.MAX_ZOOM,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            DecoratedSetting {
                                CoordinateShiftSetting(
                                    label = "x",
                                    value = camera.mmViewOffsetX,
                                    maxChange = 100f,
                                    onValueChange = {
                                        camera = camera.copy(
                                            mmViewOffsetX = it
                                        )
                                    },
                                    contentColor = submenuContentColor,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            DecoratedSetting {
                                CoordinateShiftSetting(
                                    label = "y",
                                    value = camera.mmViewOffsetY,
                                    maxChange = 100f,
                                    onValueChange = {
                                        camera = camera.copy(
                                            mmViewOffsetY = it
                                        )
                                    },
                                    contentColor = submenuContentColor,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "Video:",
                                color = submenuContentColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DecoratedSetting {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val textStyle = MaterialTheme.typography.bodyMedium
                                        Text(
                                            text = "width:  %.2fmm".format(video.mmWidth),
                                            color = submenuContentColor,
                                            style = textStyle,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "height:  %.2fmm".format(video.mmHeight),
                                            color = submenuContentColor,
                                            style = textStyle,
                                        )
                                    }
                                    Slider(
                                        value = video.mmWidth,
                                        onValueChange = {
                                            video = video.zoomedBy(
                                                zoomMultiplier = it / video.mmWidth,
                                                mmPivotPoint = video.mmTopLeft,
                                            )
                                        },
                                        valueRange = VideoOnEditor.MIN_WIDTH..VideoOnEditor.MAX_WIDTH,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            DecoratedSetting {
                                CoordinateShiftSetting(
                                    label = "x",
                                    value = video.mmOffsetX,
                                    maxChange = 100f,
                                    onValueChange = {
                                        video = video.copy(
                                            mmOffsetX = it
                                        )
                                    },
                                    contentColor = submenuContentColor,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            DecoratedSetting {
                                CoordinateShiftSetting(
                                    label = "y",
                                    value = video.mmOffsetY,
                                    maxChange = 100f,
                                    onValueChange = {
                                        video = video.copy(
                                            mmOffsetY = it
                                        )
                                    },
                                    contentColor = submenuContentColor,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            for (device in devices) {
                                val endpointId = device.watcherInfo.endpointId.ifEmpty { "UNKNOWN" }
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "${device.watcherInfo.username} [$endpointId]:",
                                    color = submenuContentColor,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                DecoratedSetting {
                                    CoordinateShiftSetting(
                                        labels = listOf("x (device)", "x (display)"),
                                        values = listOf(device.mmOffsetX, device.displayLeft),
                                        maxChange = 100f,
                                        onValueChange = {
                                            devices = devices.withMovedDevices(
                                                devices = listOf(device),
                                                offset = Offset(x = it - device.mmOffsetX, y = 0f),
                                                respectCollisions = false,
                                            )
                                        },
                                        contentColor = submenuContentColor,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                DecoratedSetting {
                                    CoordinateShiftSetting(
                                        labels = listOf("y (device)", "y (display)"),
                                        values = listOf(device.mmOffsetY, device.displayTop),
                                        maxChange = 100f,
                                        onValueChange = {
                                            devices = devices.withMovedDevices(
                                                devices = listOf(device),
                                                offset = Offset(x = 0f, y = it - device.mmOffsetY),
                                                respectCollisions = false,
                                            )
                                        },
                                        contentColor = submenuContentColor,
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                if (currentDialog == ConfigurationEditorDialogs.NONE) {
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(iconSize)
                                .clip(shape = CircleShape)
                                .background(submenuTitleBgColor)
                                .clickable { currentDialog = ConfigurationEditorDialogs.HELP }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_question_mark),
                                contentDescription = stringResource(id = R.string.desc_configuration_help_icon),
                                tint = submenuTitleContentColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(iconSize)
                                .clip(shape = CircleShape)
                                .background(submenuTitleBgColor)
                                .clickable { currentDialog = ConfigurationEditorDialogs.INFO }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_info_mark),
                                contentDescription = stringResource(id = R.string.desc_configuration_info_icon),
                                tint = submenuTitleContentColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(iconSize)
                                .clip(shape = CircleShape)
                                .background(submenuTitleBgColor)
                                .clickable { currentDialog = ConfigurationEditorDialogs.SETTINGS }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(id = R.string.desc_configuration_settings_icon),
                                tint = submenuTitleContentColor,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(iconSize)
                                .clip(shape = CircleShape)
                                .background(submenuTitleBgColor)
                                .clickable { saveConfigAndCloseEditor() }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = stringResource(id = R.string.desc_configuration_save_icon),
                                tint = submenuTitleContentColor,
                            )
                        }
                        if (false) {
                            val mmCenter = Offset(
                                x = pxToMm(pxEditorWidth / 2) + camera.mmViewOffsetX,
                                y = pxToMm(pxEditorHeight / 2) + camera.mmViewOffsetY
                            )
                            val zoomSteps = remember {
                                Steps(
                                    listOf(
                                        0.5f, 0.6f, 0.7f, 0.8f, 0.9f,
                                        1.0f,
                                        1.2f, 1.4f, 1.6f, 1.8f, 2.0f
                                    )
                                )
                            }
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
    }
}

private fun getUsername(context: Context): String? {
    val profilePrefs = getProfilePreferences(context)
    val usernameKey = context.getString(R.string.prefs_profile_username)
    return profilePrefs.getString(usernameKey, null)
}

private fun List<DeviceOnEditor>.withMovedDevices(
    devices: List<DeviceOnEditor>,
    offset: Offset,
    respectCollisions: Boolean = true,
): List<DeviceOnEditor> {
    val (selectedDevices, otherDevices) = this.partition { it in devices }
    val movedVersions = selectedDevices.map {
        it.copy(
            mmOffsetX = it.mmOffsetX + offset.x,
            mmOffsetY = it.mmOffsetY + offset.y
        )
    }
    if (respectCollisions) {
        if (movedVersions.any { it.isIntersectingAny(otherDevices) }) {
            return this
        }
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

@Composable
@Preview(widthDp=360, heightDp=760)
fun SetupTabPeoplePreview() {
    val watchers = getManyFakeWatchers()
    val videoMetadata = getFakeVideoMetadata("MyVideo.mp4")
    val videofile = Videofile(
        uri = Uri.EMPTY,
        metadata = videoMetadata
    )
    val editor = DevicePositionsEditor.create(
        watchers = watchers.filter { it.isApproved },
        videofile = videofile
    )
    suspend fun fakeSuspendFun(one: Offset, two: Offset) {}
    Surface(modifier = Modifier.fillMaxSize()) {
        SetupTabPeople(
            isUserHost = true,
            isAdvertising = true,
            startAdvertising = {},
            stopAdvertising = {},
            hostAsWatcher = watchers[0],
            selfAsWatcher = watchers[0],
            allWatchers = watchers,
            watcherPings = getPingsForWatchers(watchers),
            videoMetadata = videoMetadata,
            positionsEditor = editor,
            saveDevicePositions = {},
            sendSyncBall = ::fakeSuspendFun,
            setGuestCallbacks = {},
            notApprovedWatcherModifier = { Modifier.clickable {} },
            approvedWatcherModifier = { Modifier.clickable {} },
            modifier = Modifier,
        )
    }
}

fun getManyFakeWatchers(): List<Watcher> {
    return listOf(
        Watcher(
            endpointId = "FRST",
            username = "WatcherOne",
            messagingVersion = 1,
            physicalDevice = VisyncPhysicalDevice.NoDimensionsDevice.copy(inDisplaySize = 6.1f),
            isApproved = false,
            missingVideofileNames = emptyList(),
        ),
        Watcher(
            endpointId = "SCND",
            username = "WatcherwatcherTwotwo",
            messagingVersion = 1,
            physicalDevice = VisyncPhysicalDevice.NoDimensionsDevice.copy(inDisplaySize = 6.2f),
            isApproved = false,
            missingVideofileNames = emptyList(),
        ),
        Watcher(
            endpointId = "THRD",
            username = "ImThirdwatcher",
            messagingVersion = 0,
            physicalDevice = VisyncPhysicalDevice.NoDimensionsDevice.copy(inDisplaySize = 6.3f),
            isApproved = false,
            missingVideofileNames = emptyList(),
        ),
        Watcher(
            endpointId = "FRTH",
            username = "WatcherNumberfour",
            messagingVersion = 1,
            physicalDevice = VisyncPhysicalDevice.NoDimensionsDevice.copy(inDisplaySize = 6.4f),
            isApproved = false,
            missingVideofileNames = listOf("MyVideo.mp4"),
        ),
        Watcher(
            endpointId = "FFTH",
            username = "WatcherFive",
            messagingVersion = 1,
            physicalDevice = VisyncPhysicalDevice.NoDimensionsDevice.copy(inDisplaySize = 6.5f),
            isApproved = true,
            missingVideofileNames = emptyList(),
        ),
        Watcher(
            endpointId = "SXTH",
            username = "SixSixsix",
            messagingVersion = null,
            physicalDevice = null,
            isApproved = false,
            missingVideofileNames = emptyList(),
        )
    )
}

fun getPingsForWatchers(watchers: List<Watcher>): List<EndpointPingData> {
    val rnd = Random(42)
    return watchers.map {
        val firstTimestamp = 0L
        val pingInterval = 1000L
        val minPing = 1L
        val maxPing = 400L

        EndpointPingData(
            endpointId = it.endpointId,
            pingData = PingData(
                pingList = (1..5).map { i ->
                    val requestTimestamp = firstTimestamp + (i-1) * pingInterval
                    PingEntry(
                        requestTimestamp = requestTimestamp,
                        responseTimestamp = if (rnd.nextFloat() > 0.1f) {
                            requestTimestamp + rnd.nextLong(minPing, maxPing + 1)
                        } else null
                    )
                }
            )
        )
    }
}

@Composable
fun GradientButton(
    gradient: Brush,
    shape: Shape,
    contentColor: Color,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    content: @Composable () -> Unit,
) {
    Button(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .then(modifier),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

@Composable
fun WatcherInfoDialog(
    watcher: Watcher,
    hostWatcher: Watcher,
    isUserHost: Boolean,
    toggleIsApproved: () -> Unit,
    close: () -> Unit,
) {
    GenericAlertDialog(
        onDismissRequest = {
            close()
        },
    ) {
        val titleBgColor = MaterialTheme.colorScheme.primary
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "User information",
                style = MaterialTheme.typography.titleLarge,
                color = contentColorFor(titleBgColor),
                modifier = Modifier
                    .background(color = titleBgColor)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                val unknownString = "Unknown"
                UserAttribute(
                    attributeName = "Username",
                    attributeValue = watcher.username,
                    isBadValue = { it == unknownString },
                )
                UserAttribute(
                    attributeName = "Endpoint ID",
                    attributeValue = watcher.endpointId.ifEmpty { unknownString },
                    isBadValue = { false },
                )
                UserAttribute(
                    attributeName = "AP Version",
                    attributeValue = watcher.messagingVersion?.toString() ?: unknownString,
                    isBadValue = {
                        it == unknownString || it != hostWatcher.messagingVersion.toString()
                    },
                )
                Text(
                    text = "Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    VerticalDivider()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                    ) {
                        val device = watcher.physicalDevice
                        val displaySize = device?.inDisplaySize
                        val displaySizeString = when {
                            device != null && displaySize != 0f -> "$displaySize in."
                            else -> unknownString
                        }
                        val w = device?.pxDisplayWidth?.roundToInt()
                        val h = device?.pxDisplayHeight?.roundToInt()
                        val hasWh = w != 0 && h != 0
                        val isValidResolution = device != null && hasWh
                        val resolutionString = when {
                            isValidResolution -> "%dx%d".format(w, h)
                            else -> unknownString
                        }
                        val aspectRatioString = when {
                            isValidResolution -> {
                                val gcd = getGreatestCommonDivisor(w!!, h!!)
                                if (gcd != 0) {
                                    "%d:%d".format(h / gcd, w / gcd)
                                } else {
                                    unknownString
                                }
                            }
                            else -> unknownString
                        }
                        val bodyW = device?.mmDeviceWidth
                        val bodyH = device?.mmDeviceHeight
                        val hasBodyWh = bodyW != 0f && bodyH != 0f
                        val bodyWhString = when {
                            device != null && hasBodyWh -> "%.1fmm*%.1fmm".format(bodyW, bodyH)
                            else -> unknownString
                        }
                        UserAttribute(
                            attributeName = "Display size",
                            attributeValue = displaySizeString,
                            isBadValue = { it == unknownString },
                        )
                        UserAttribute(
                            attributeName = "Resolution",
                            attributeValue = resolutionString,
                            isBadValue = { it == unknownString },
                        )
                        UserAttribute(
                            attributeName = "Aspect ratio",
                            attributeValue = aspectRatioString,
                            isBadValue = { it == unknownString },
                        )
                        UserAttribute(
                            attributeName = "Body",
                            attributeValue = bodyWhString,
                            isBadValue = { it == unknownString },
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    if (watcher.isApproved) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_monitor),
                            contentDescription = stringResource(id = R.string.desc_watcher_approved),
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_no_monitor),
                            contentDescription = stringResource(id = R.string.desc_watcher_not_approved),
                        )
                    }
                    Text(
                        text = when {
                            watcher.isApproved -> "This user will be participating in group playback"
                            else -> "This user will NOT be participating in group playback"
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
            ) {
                if (isUserHost) {
                    Button(
                        onClick = {
                            toggleIsApproved()
                            close()
                        },
                        enabled = watcher.canBeApproved,
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight()
                    ) {
                        val buttonText = when {
                            watcher.isApproved -> "Disallow participation"
                            else -> "Allow participation"
                        }
                        Text(
                            text = buttonText,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Button(
                    onClick = {
                        close()
                    },
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                ) {
                    Text(text = "Close")
                }
            }
        }
    }
}

@Composable
private fun UserAttribute(
    attributeName: String,
    attributeValue: String,
    isBadValue: (String) -> Boolean,
    badValueColor: Color = when (MaterialTheme.colorScheme.isLight()) {
        true -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.errorContainer
    }
) {
    val attributeNameStyle = MaterialTheme.typography.titleMedium
    val attributeValueStyle = MaterialTheme.typography.bodyLarge
    Row(
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = attributeName,
            style = attributeNameStyle,
            textAlign = TextAlign.End,
            modifier = Modifier
        )
        Spacer(modifier = Modifier.requiredWidth(8.dp))
        Spacer(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    this.translationY -= 4.dp.toPx()
                }
                .bottomDots(
                    strokeWidth = 1.dp,
                    interval = 3.dp,
                    color = LocalContentColor.current
                )
        )
        Spacer(modifier = Modifier.requiredWidth(8.dp))
        Text(
            text = attributeValue,
            style = attributeValueStyle,
            color = if (isBadValue(attributeValue)) badValueColor else Color.Unspecified,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}

fun Modifier.bottomDots(
    strokeWidth: Dp,
    interval: Dp,
    color: Color
) = composed(
    factory = {
        val density = LocalDensity.current
        val dotSizePx = density.run { strokeWidth.toPx() }
        val intervalPx = density.run { interval.toPx() }
        this.then(
            Modifier.drawWithCache {
                onDrawBehind {
                    val oneUnitLength = dotSizePx + intervalPx
                    val unitsToFitSize = (this.size.width / oneUnitLength).toInt()
                    val defaultLineLength = oneUnitLength * unitsToFitSize
                    val canFitLastDot = defaultLineLength + dotSizePx <= this.size.width
                    val lineLength = when {
                        canFitLastDot -> defaultLineLength + dotSizePx
                        else -> defaultLineLength
                    }
                    drawLine(
                        color = color,
                        strokeWidth = dotSizePx,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(dotSizePx, intervalPx),
                            phase = 0f
                        ),
                        start = Offset(0f, this.size.height),
                        end = Offset(lineLength, this.size.height)
                    )
                }
            }
        )
    }
)

@Composable
@Preview
fun WatcherInfoDialogPreview() {
    WatcherInfoDialog(
        watcher = getFakeWatchers()[0].copy(isApproved = false),
        hostWatcher = getFakeWatchers()[0],
        isUserHost = true,
        toggleIsApproved = {},
        close = {},
    )
}

@Composable
@Preview
fun DevicesPositionConfigurationEditorPreview() {
    val rnd = Random(42)
    val getRandomDisplaySize = { 5f + rnd.nextFloat() * 2f }
    val getRandomPxWidth = { 720f + rnd.nextFloat() * 720f }
    val getRandomPxHeight = { 1440f + rnd.nextFloat() * 1440f }
    val getRandomBodyWidth = { displaySize: Float, pxWidth: Float, pxHeight: Float ->
        val displayWh = VisyncPhysicalDevice.getWidthAndHeightMm(displaySize, pxWidth, pxHeight)
        displayWh.width + rnd.nextFloat() * 4f + 2f
    }
    val getRandomBodyHeight = { displaySize: Float, pxWidth: Float, pxHeight: Float ->
        val displayWh = VisyncPhysicalDevice.getWidthAndHeightMm(displaySize, pxWidth, pxHeight)
        displayWh.height + rnd.nextFloat() * 10f + 4f
    }
    val watchers = getManyFakeWatchers().map {
        val displaySize = getRandomDisplaySize()
        val pxWidth = getRandomPxWidth()
        val pxHeight = getRandomPxHeight()
        val bodyWidth = getRandomBodyWidth(displaySize, pxWidth, pxHeight)
        val bodyHeight = getRandomBodyHeight(displaySize, pxWidth, pxHeight)
        it.copy(
            isApproved = true,
            physicalDevice = VisyncPhysicalDevice(
                inDisplaySize = displaySize,
                pxDisplayWidth = pxWidth,
                pxDisplayHeight = pxHeight,
                mmDeviceWidth = bodyWidth,
                mmDeviceHeight = bodyHeight,
            )
        )
    }
    val videoMetadata = getFakeVideoMetadata("MyVideo.mp4")
    val videofile = Videofile(
        uri = Uri.EMPTY,
        metadata = videoMetadata
    )
    val baseEditor = DevicePositionsEditor.create(
        watchers = watchers.filter { it.isApproved },
        videofile = videofile
    )
    val editor = baseEditor.copy(
        cameraView = baseEditor.cameraView.zoomedTo(
            targetZoom = 0.5f,
            mmPivotPoint = Offset(25f, 100f)
        )
    )
    DevicesPositionConfigurationEditor(
        positionsEditor = editor,
        saveDevicePositions = {},
        closeEditor = {}
    )
}

fun DrawScope.drawWithLayer(block: DrawScope.() -> Unit) {
    with(drawContext.canvas.nativeCanvas) {
        val checkPoint = saveLayer(null, null)
        block()
        restoreToCount(checkPoint)
    }
}

fun DrawScope.drawInnerShadow(
    color: Color = Color.Black,
    offset: Offset,
    size: Size,
    cornerRadius: Float = 0f,
    spread: Float = 0f,
    blur: Float = 0f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
) {
    val rect = Rect(offset, size)
    val paint = Paint()

    drawIntoCanvas {
        it.saveLayer(rect, paint)
        paint.color = color
        paint.isAntiAlias = true
        it.drawRoundRect(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            cornerRadius,
            cornerRadius,
            paint
        )
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        if (blur > 0) {
            frameworkPaint.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
        }
        val left = if (offsetX > 0f) {
            rect.left + offsetX
        } else {
            rect.left
        }
        val top = if (offsetY > 0f) {
            rect.top + offsetY
        } else {
            rect.top
        }
        val right = if (offsetX < 0f) {
            rect.right + offsetX
        } else {
            rect.right
        }
        val bottom = if (offsetY < 0f) {
            rect.bottom + offsetY
        } else {
            rect.bottom
        }
        paint.color = color
        it.drawRoundRect(
            left = left + spread / 2,
            top = top + spread / 2,
            right = right - spread / 2,
            bottom = bottom - spread / 2,
            cornerRadius,
            cornerRadius,
            paint
        )
        frameworkPaint.xfermode = null
        frameworkPaint.maskFilter = null
        it.restore()
    }
}

fun DrawScope.drawShadow(
    color: Color = Color.Black,
    offset: Offset,
    size: Size,
    cornerRadius: Float = 0f,
    spread: Float = 0f,
    blur: Float = 0f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
) {
    val paint = Paint()

    drawIntoCanvas {
        it.save()
        paint.color = color
        paint.isAntiAlias = true
        val left = (offset.x - spread) + offsetX
        val top = (offset.y - spread) + offsetY
        val right = (offset.x + size.width + spread)
        val bottom = (offset.y + size.height + spread)

        val frameworkPaint = paint.asFrameworkPaint()
        if (blur != 0f) {
            frameworkPaint.maskFilter =
                (BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL))
        }

        it.drawRoundRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            radiusX = cornerRadius,
            radiusY = cornerRadius,
            paint
        )
        frameworkPaint.xfermode = null
        it.restore()
    }
}

enum class ConfigurationEditorDialogs {
    NONE, HELP, INFO, SETTINGS
}

@Composable
fun CoordinateShiftSlider(
    /** Value from 0f to 1f */
    startingPosition: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float>,
    transformation: (Float) -> Float,
    onValueChangeFinished: (UnevenSensitivitySliderScope.() -> Unit)?,
) {
    val minValue = valueRange.start
    val maxValue = valueRange.endInclusive
    val diff = maxValue - minValue
    val coroutineScope = rememberCoroutineScope()
    val animatedPosition = remember { Animatable(startingPosition) }
    val unevenSensitivitySliderScope = remember {
        UnevenSensitivitySliderScope(
            animateSliderValue = {
                coroutineScope.launch {
                    animatedPosition.animateTo(
                        targetValue = it,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing
                        ),
                    )
                }
            },
            setSliderValue = {
                coroutineScope.launch {
                    animatedPosition.snapTo(it)
                }
            }
        )
    }
    Slider(
        value = animatedPosition.value,
        onValueChange = {
            unevenSensitivitySliderScope.setSliderPosition(it)
            val newValue = minValue + diff * transformation(it)
            onValueChange(newValue)
        },
        modifier = modifier,
        valueRange = 0f..1f,
        onValueChangeFinished = {
            onValueChangeFinished?.let { unevenSensitivitySliderScope.it() }
        },
    )
}

@Composable
fun CoordinateShiftSetting(
    label: String,
    value: Float,
    maxChange: Float,
    onValueChange: (Float) -> Unit,
    contentColor: Color,
) {
    val textStyle = MaterialTheme.typography.bodyMedium
    val flatAroundTheMiddleFunction = { it: Float ->
        /** Describes how flat graph is around x=0.5.
         *
         * 1f means linear interpolation between 0 and 1: y=x.
         * Values above 1f increase flatness and values below decrease it. */
        val middleFlatness = 2.0f
        if (it <= 0.5) {
            0.5f * (2 * it).pow(1f / middleFlatness)
        } else {
            -0.5f * (2 * (-it + 1f)).pow(1f / middleFlatness)+ 1f
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "$label:  %.2f".format(value),
            color = contentColor,
            style = textStyle,
        )
        var currentLeftBoundary by remember { mutableFloatStateOf(value - maxChange) }
        var currentRightBoundary by remember { mutableFloatStateOf(value + maxChange) }
        CoordinateShiftSlider(
            startingPosition = 0.5f,
            onValueChange = onValueChange,
            valueRange = currentLeftBoundary..currentRightBoundary,
            transformation = flatAroundTheMiddleFunction,
            onValueChangeFinished = {
                currentLeftBoundary = value - 100f
                currentRightBoundary = value + 100f
                animateSliderPosition(to = 0.5f)
            },
        )
    }
}

@Composable
fun CoordinateShiftSetting(
    labels: List<String>,
    values: List<Float>,
    maxChange: Float,
    onValueChange: (Float) -> Unit,
    contentColor: Color,
) {
    val textStyle = MaterialTheme.typography.bodyMedium
    val flatAroundTheMiddleFunction = { it: Float ->
        /** Describes how flat graph is around x=0.5.
         *
         * 1f means linear interpolation between 0 and 1: y=x.
         * Values above 1f increase flatness and values below decrease it. */
        val middleFlatness = 2.0f
        if (it <= 0.5) {
            0.5f * (2 * it).pow(1f / middleFlatness)
        } else {
            -0.5f * (2 * (-it + 1f)).pow(1f / middleFlatness)+ 1f
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            for ((label, value) in labels.zip(values)) {
                Text(
                    text = "$label:  %.2f".format(value),
                    color = contentColor,
                    style = textStyle,
                )
            }
        }

        var currentLeftBoundary by remember { mutableFloatStateOf(values[0] - maxChange) }
        var currentRightBoundary by remember { mutableFloatStateOf(values[0] + maxChange) }
        CoordinateShiftSlider(
            startingPosition = 0.5f,
            onValueChange = onValueChange,
            valueRange = currentLeftBoundary..currentRightBoundary,
            transformation = flatAroundTheMiddleFunction,
            onValueChangeFinished = {
                currentLeftBoundary = values[0] - 100f
                currentRightBoundary = values[0] + 100f
                animateSliderPosition(to = 0.5f)
            },
        )
    }
}

@Composable
fun DecoratedSetting(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        VerticalDivider()
        Spacer(modifier = Modifier.width(8.dp))
        content()
    }
}

class UnevenSensitivitySliderScope(
    private val setSliderValue: (Float) -> Unit,
    private val animateSliderValue: (Float) -> Unit,
) {
    fun setSliderPosition(position: Float) {
        setSliderValue(position)
    }
    fun animateSliderPosition(to: Float) {
        animateSliderValue(to)
    }
}

@Composable
fun VerticalDivider(
    horizontalPadding: Dp = 8.dp,
    verticalPadding: Dp = 4.dp,
    color: Color = LocalContentColor.current,
) {
    Divider(
        modifier = Modifier
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .width(1.dp)
            .fillMaxHeight()
            .background(color)
    )
}