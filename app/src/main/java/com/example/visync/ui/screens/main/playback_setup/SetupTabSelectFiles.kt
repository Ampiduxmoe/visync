package com.example.visync.ui.screens.main.playback_setup

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.visync.R
import com.example.visync.data.videofiles.Videofile
import com.example.visync.metadata.MetadataReader
import com.example.visync.metadata.VideoMetadata
import com.example.visync.metadata.VisyncMetadataReader
import com.example.visync.ui.components.navigation.GenericAlertDialog
import kotlin.math.roundToInt

@Composable
fun SetupTabSelectFiles(
    isUserHost: Boolean,
    localSelectedVideofiles: List<Videofile>,
    playbackOptions: PlaybackOptions,
    setSelectedVideofiles: (List<Videofile>) -> Unit,
    addVideofiles: (List<Videofile>) -> Unit,
    removeVideoByMetadata: (VideoMetadata) -> Unit,
    setSelectedVideofileIndex: (Int) -> Unit,
    missingFilenames: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val metadataReader: MetadataReader = remember { VisyncMetadataReader(context) }
    val pickVideoOnly = PickVisualMediaRequest(
        ActivityResultContracts.PickVisualMedia.VideoOnly
    )

    val reselectVideosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            uris.ifEmpty { return@rememberLauncherForActivityResult }
            val videofiles = urisToVideofiles(uris, metadataReader)
            setSelectedVideofiles(videofiles)
        }
    )
    val reselectVideos = { reselectVideosLauncher.launch(pickVideoOnly) }

    val addVideosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            uris.ifEmpty { return@rememberLauncherForActivityResult }
            val newVideofiles = urisToVideofiles(uris, metadataReader)
            addVideofiles(newVideofiles)
        }
    )
    val addVideos = { addVideosLauncher.launch(pickVideoOnly) }

    val uriToMatch = remember { mutableStateOf<Uri?>(null) }
    val matchFilename = remember { mutableStateOf<String?>(null)}
    val addMatchToVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uriToMatch.value = uri
        }
    )
    val addMatchToFilename = { filename: String ->
        matchFilename.value = filename
        addMatchToVideoLauncher.launch(pickVideoOnly)
    }

    LaunchedEffect(uriToMatch.value, matchFilename.value) {
        val uri = uriToMatch.value
        val altFilename = matchFilename.value
        if (uri != null && altFilename != null) {
            val metadata = metadataReader.getVideoMetadataFromUri(uri)
            metadata ?: return@LaunchedEffect
            addVideofiles(listOf(
                Videofile(
                    uri = uri,
                    metadata = VideoMetadata(
                        filename = metadata.filename,
                        altFilename = altFilename,
                        duration = metadata.duration,
                        width = metadata.width,
                        height = metadata.height
                    )
                )
            ))
        }
    }

    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
    ) {
        SelectedVideosList(
            isUserHost = isUserHost,
            playbackOptions = playbackOptions,
            selectVideofiles = reselectVideos,
            addVideofiles = addVideos,
            addMatchToFilename = addMatchToFilename,
            removeVideoByMetadata = removeVideoByMetadata,
            setSelectedVideofileIndex = setSelectedVideofileIndex,
            missingFilenames = missingFilenames,
            localSelection = localSelectedVideofiles,
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun SelectedVideosList(
    isUserHost: Boolean,
    playbackOptions: PlaybackOptions,
    selectVideofiles: () -> Unit,
    addVideofiles: () -> Unit,
    addMatchToFilename: (String) -> Unit,
    removeVideoByMetadata: (VideoMetadata) -> Unit,
    setSelectedVideofileIndex: (Int) -> Unit,
    missingFilenames: List<String> = emptyList(), // only for guest
    localSelection: List<Videofile> = emptyList(), // only for guest
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val videofilesMetadata = playbackOptions.videofilesMetadata
    val hasSelectedVideofiles = videofilesMetadata.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .then(modifier)
    ) {
        if (!isUserHost && !hasSelectedVideofiles) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Host did not select anything yet")
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                var showVideoMatchInfoDialog by remember { mutableStateOf(false) }
                var matchDialogVideofile by remember { mutableStateOf<VideoMetadata?>(null) }
                var matchDialogMetadata by remember { mutableStateOf<VideoMetadata?>(null) }
                val showVideoMatchInfo: (VideoMetadata, VideoMetadata?) -> Unit = { referenceMetadata, metadata ->
                    matchDialogVideofile = referenceMetadata
                    matchDialogMetadata = metadata
                    showVideoMatchInfoDialog = true
                }
                val isLightTheme = MaterialTheme.colorScheme.isLight()
                val errorColor =  when (isLightTheme) {
                    true -> MaterialTheme.colorScheme.error
                    false -> MaterialTheme.colorScheme.errorContainer
                }
                val warningColor = when (isLightTheme) {
                    true -> MaterialTheme.colorScheme.errorContainer
                    false -> MaterialTheme.colorScheme.error
                }
                videofilesMetadata.forEachIndexed { index, metadata ->
                    val isGuestError = metadata.filename in missingFilenames
                    val localMatchMetadata = when {
                        isGuestError -> null
                        else -> localSelection.find {
                            it.metadata.filename == metadata.filename ||
                            it.metadata.altFilename == metadata.filename
                        }!!.metadata
                    }
                    val isGuestWarning = when {
                        isGuestError -> null
                        else -> !localMatchMetadata!!.equalsByContent(metadata)
                    }
                    VideofileItem(
                        isUserHost = isUserHost,
                        videofileMetadata = metadata,
                        isSelected = index == playbackOptions.selectedVideofileIndex,
                        isGuestError = isGuestError,
                        isGuestWarning = isGuestWarning,
                        errorColor = errorColor,
                        warningColor = warningColor,
                        removeVideo = { removeVideoByMetadata(metadata) },
                        clickModifier = Modifier.clickable {
                            if (isUserHost) {
                                setSelectedVideofileIndex(index)
                            } else {
                                showVideoMatchInfo(metadata, localMatchMetadata)
                            }
                        }
                    )
                }
                if (isUserHost) {
                    if (hasSelectedVideofiles) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    VideofileItemAdd(
                        onClick = { addVideofiles() }
                    )
                }
                if (showVideoMatchInfoDialog) {
                    VideoMatchInfoDialog(
                        referenceMetadata = matchDialogVideofile!!,
                        metadataToCompare = matchDialogMetadata,
                        removeVideoByMetadata = removeVideoByMetadata,
                        addMatchToFilename = addMatchToFilename,
                        close = { showVideoMatchInfoDialog = false },
                        errorColor = errorColor,
                        warningColor = warningColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideofileItem(
    isUserHost: Boolean,
    videofileMetadata: VideoMetadata,
    isSelected: Boolean,
    isGuestError: Boolean? = null,
    isGuestWarning: Boolean? = null,
    errorColor: Color = MaterialTheme.colorScheme.error,
    warningColor: Color = MaterialTheme.colorScheme.errorContainer,
    removeVideo: () -> Unit,
    @SuppressLint("ModifierParameter")
    textSizeModifier: Modifier = Modifier
        .fillMaxWidth(0.75f)
        .padding(vertical = 12.dp),
    clickModifier: Modifier = Modifier,
) {
    val selectionBackground = MaterialTheme.colorScheme.secondaryContainer
    val selectionBackgroundModifier = when {
        isSelected -> Modifier.background(selectionBackground)
        else -> Modifier
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = selectionBackgroundModifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 12.dp)
    ) {
        val iconSize = 36.dp
        val videoNameStyle = MaterialTheme.typography.titleMedium
        val videoDurationStyle = MaterialTheme.typography.bodyMedium
        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(iconSize)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = stringResource(id = R.string.desc_current_video),
                    modifier = Modifier.size(iconSize)
                )
            }
        } else {
            Box(modifier = Modifier.size(iconSize))
        }

        Column(
            modifier = textSizeModifier
        ) {
            if (isSelected) {
                Text(
                    text = videofileMetadata.filename,
                    style = videoNameStyle,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = videofileMetadata.filename,
                    style = videoNameStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val durationString = videoDurationPrettyString(videofileMetadata.duration)
            Text(
                text = "Duration: $durationString",
                style = videoDurationStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

        }
        Spacer(modifier = Modifier.weight(1f))
        if (isUserHost) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(iconSize)
                    .clip(shape = CircleShape)
                    .clickable { removeVideo() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_remove),
                    contentDescription = stringResource(id = R.string.desc_remove_video),
                )
            }
        } else {
            val iconBgColor = when {
                isGuestError == true -> errorColor
                isGuestWarning == true -> warningColor
                else -> Color.Unspecified
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(iconSize)
                    .clip(shape = CircleShape)
                    .background(iconBgColor)
            ) {
                when {
                    isGuestError == true -> {
                        Text(
                            text = "X",
                            style = videoNameStyle,
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColorFor(iconBgColor)
                        )
                    }
                    isGuestWarning == true -> {
                        Text(
                            text = "!",
                            style = videoNameStyle,
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColorFor(iconBgColor)
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = stringResource(id = R.string.desc_video_is_matching),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideofileItemAdd(
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .dashedBorder(
                strokeWidth = 1.dp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                cornerRadiusDp = 16.dp
            )
            .clip(
                shape = RoundedCornerShape(
                    size = 16.dp
                )
            )
            .clickable { onClick() }
    ) {
        Text(
            text = "+",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(vertical = 16.dp)
        )
    }
}

@Composable
fun VideoMatchInfoDialog(
    referenceMetadata: VideoMetadata,
    metadataToCompare: VideoMetadata?,
    removeVideoByMetadata: (VideoMetadata) -> Unit,
    addMatchToFilename: (String) -> Unit,
    close: () -> Unit,
    errorColor: Color = MaterialTheme.colorScheme.error,
    warningColor: Color = MaterialTheme.colorScheme.errorContainer,
) {
    GenericAlertDialog(
        onDismissRequest = {
            close()
        },
    ) {
        val noFile = metadataToCompare == null
        val metadataMismatch = when {
            noFile -> false
            else -> !metadataToCompare!!.equalsByContent(referenceMetadata)
        }
        val titleBgColor = when {
            noFile -> errorColor
            metadataMismatch -> warningColor
            else -> MaterialTheme.colorScheme.primary
        }
        Column {
            Text(
                text = referenceMetadata.filename,
                style = MaterialTheme.typography.titleLarge,
                color = contentColorFor(titleBgColor),
                modifier = Modifier
                    .shadow(elevation = 2.dp)
                    .background(color = titleBgColor)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Column(
                modifier = Modifier
            ) {
                if (noFile) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Text(
                            text = "No file was selected for this video"
                        )
                    }
                } else {
                    val rowColNamesStyle = MaterialTheme.typography.titleMedium
                    val rowPadding = 4.dp
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(rowPadding)
                        ) {
                            Spacer(
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Host's video",
                                style = rowColNamesStyle,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Your file",
                                style = rowColNamesStyle,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val durationMismatch = referenceMetadata.duration != metadataToCompare!!.duration
                        val durationBgColor = if (durationMismatch) errorColor else Color.Unspecified
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(durationBgColor)
                                .padding(rowPadding)
                        ) {
                            Text(
                                text = "Duration",
                                color = contentColorFor(durationBgColor),
                                style = rowColNamesStyle,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = referenceMetadata.duration.toString(),
                                color = contentColorFor(durationBgColor),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = metadataToCompare.duration.toString(),
                                color = contentColorFor(durationBgColor),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val resolutionMismatch = referenceMetadata.let {
                            it.width != metadataToCompare.width ||
                            it.height != metadataToCompare.height
                        }
                        val toResolutionString: (VideoMetadata) -> String = {
                            val w = it.width.roundToInt()
                            val h = it.height.roundToInt()
                            "${w}x${h}"
                        }
                        val resolutionBgColor = if (resolutionMismatch) errorColor else Color.Unspecified
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(resolutionBgColor)
                                .padding(rowPadding)
                        ) {
                            Text(
                                text = "Resolution",
                                color = contentColorFor(resolutionBgColor),
                                style = rowColNamesStyle,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = toResolutionString(referenceMetadata),
                                color = contentColorFor(resolutionBgColor),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = toResolutionString(metadataToCompare),
                                color = contentColorFor(resolutionBgColor),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val anyMismatch = durationMismatch || resolutionMismatch
                        if (anyMismatch) {
                            Text(
                                text = "Issues are not critical but can cause problems synchronizing this video",
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
            ) {
                ElevatedButton(
                    onClick = {
                        if (noFile) {
                            addMatchToFilename(referenceMetadata.filename)
                        } else if (metadataMismatch) {
                            removeVideoByMetadata(metadataToCompare!!)
                        }
                        close()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = titleBgColor,
                        contentColor = contentColorFor(titleBgColor)
                    ),
                    modifier = Modifier.weight(3f)
                ) {
                    val buttonText = when {
                        noFile -> "Select file"
                        else -> "Remove file"
                    }
                    Text(text = buttonText)
                }
                ElevatedButton(
                    onClick = {
                        close()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = titleBgColor,
                        contentColor = contentColorFor(titleBgColor)
                    ),
                    modifier = Modifier.weight(2f)
                ) {
                    Text(text = "Close")
                }
            }
        }
    }
}

fun urisToVideofiles(
    uris: List<Uri>,
    metadataReader: MetadataReader
): List<Videofile> = uris.map {
    Videofile(
        uri = it,
        metadata = metadataReader.getVideoMetadataFromUri(it)
            ?: VideoMetadata(
                filename = "unknown file",
                duration = -1,
                width = -1f,
                height = -1f
            )
    )
}

@Composable
@Preview(widthDp=360, heightDp=760)
fun HostSetupTabSelectFilesPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val fakePlaybackOptions = getFakePlaybackOptions()
        val fakeVideofiles = getFakeVideofiles()
        val extraFakeVideofiles = getExtraFakeVideofiles()
        val finalFakeVideofiles = listOf(
            extraFakeVideofiles[0]
        ) + fakeVideofiles + listOf(
            extraFakeVideofiles[1].copy(
                metadata = extraFakeVideofiles[1].metadata.copy(duration = 123123123)
            ),
            extraFakeVideofiles[2]
        )
        val finalPlaybackOptions = fakePlaybackOptions.copy(
            videofilesMetadata = listOf(
                extraFakeVideofiles[0].metadata
            ) + fakePlaybackOptions.videofilesMetadata + listOf(
                extraFakeVideofiles[1].metadata.copy(duration = 123123123),
                extraFakeVideofiles[2].metadata
            ),
            selectedVideofileIndex = 1
        )
        SetupTabSelectFiles(
            isUserHost = true,
            localSelectedVideofiles = finalFakeVideofiles,
            playbackOptions = finalPlaybackOptions,
            setSelectedVideofiles = {},
            addVideofiles = {},
            removeVideoByMetadata = {},
            setSelectedVideofileIndex = {},
            missingFilenames = emptyList(),
            modifier = Modifier,
        )
    }
}

@Composable
@Preview(widthDp=360, heightDp=760)
fun GuestSetupTabSelectFilesPreview() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val fakePlaybackOptions = getFakePlaybackOptions()
        val fakeVideofiles = getFakeVideofiles()
        val extraFakeVideofiles = getExtraFakeVideofiles()
        val finalFakeVideofiles = listOf(
            extraFakeVideofiles[0].copy(
                metadata = extraFakeVideofiles[0].metadata.copy(width = 123f)
            )
        ) + listOf(fakeVideofiles[0]) + listOf(
            extraFakeVideofiles[1].copy(
                metadata = extraFakeVideofiles[1].metadata.copy(duration = 123123123L)
            ),
            extraFakeVideofiles[2].copy(
                metadata = extraFakeVideofiles[2].metadata.copy(duration = 666L)
            )
        )
        val finalPlaybackOptions = fakePlaybackOptions.copy(
            videofilesMetadata = listOf(
                extraFakeVideofiles[0].metadata
            ) + fakePlaybackOptions.videofilesMetadata + listOf(
                extraFakeVideofiles[1].metadata.copy(duration = 123123123L),
                extraFakeVideofiles[2].metadata
            ),
            selectedVideofileIndex = 1
        )
        SetupTabSelectFiles(
            isUserHost = false,
            localSelectedVideofiles = finalFakeVideofiles,
            playbackOptions = finalPlaybackOptions,
            setSelectedVideofiles = {},
            addVideofiles = {},
            removeVideoByMetadata = {},
            setSelectedVideofileIndex = {},
            missingFilenames = listOf("MyVideo.mp4"),
            modifier = Modifier,
        )
    }
}

@Composable
@Preview
fun VideoMatchInfoDialogPreviewNoMatch() {
    VideoMatchInfoDialog(
        referenceMetadata = getFakeVideofiles()[0].metadata.copy(
            filename = "longVideoTitle.withLongExtensionName"
        ),
        metadataToCompare = null,
        removeVideoByMetadata = {},
        addMatchToFilename = {},
        close = {}
    )
}

@Composable
@Preview
fun VideoMatchInfoDialogPreviewPartialMatch() {
    VideoMatchInfoDialog(
        referenceMetadata = getFakeVideofiles()[0].metadata.copy(
            filename = "longVideoTitle.withLongExtensionName"
        ),
        metadataToCompare = getFakeVideofiles()[0].metadata.copy(
            width = 666f
        ),
        removeVideoByMetadata = {},
        addMatchToFilename = {},
        close = {}
    )
}

@Composable
@Preview
fun VideoMatchInfoDialogPreviewFullMatch() {
    VideoMatchInfoDialog(
        referenceMetadata = getFakeVideofiles()[0].metadata.copy(
            filename = "longVideoTitle.withLongExtensionName"
        ),
        metadataToCompare = getFakeVideofiles()[0].metadata,
        removeVideoByMetadata = {},
        addMatchToFilename = {},
        close = {}
    )
}

fun getExtraFakeVideofiles(): List<Videofile> {
    return listOf(
        Videofile(
            uri = Uri.EMPTY,
            metadata = getFakeVideoMetadata("Video0"),
        ),
        Videofile(
            uri = Uri.EMPTY,
            metadata = getFakeVideoMetadata("Video2longlonglonglonglonglong"),
        ),
        Videofile(
            uri = Uri.EMPTY,
            metadata = getFakeVideoMetadata("Video3"),
        ),
    )
}

fun Modifier.dashedBorder(strokeWidth: Dp, color: Color, cornerRadiusDp: Dp) = composed(
    factory = {
        val density = LocalDensity.current
        val strokeWidthPx = density.run { strokeWidth.toPx() }
        val cornerRadiusPx = density.run { cornerRadiusDp.toPx() }

        this.then(
            Modifier.drawWithCache {
                onDrawBehind {
                    val stroke = Stroke(
                        width = strokeWidthPx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                    )

                    drawRoundRect(
                        color = color,
                        style = stroke,
                        cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                }
            }
        )
    }
)

fun videoDurationPrettyString(duration: Long): String {
    val toSecondsMult = 1000L
    val toMinutesMult = toSecondsMult * 60L
    val toHoursMult = toMinutesMult * 60L

    val hours = duration / toHoursMult
    val hoursTotalDuration = hours * toHoursMult
    val minutes = (duration - hoursTotalDuration) / toMinutesMult
    val minutesTotalDuration = minutes * toMinutesMult
    val seconds = (duration - hoursTotalDuration - minutesTotalDuration) / toSecondsMult

    val hoursString = when {
        hours == 0L -> ""
        else -> "${hours}h "
    }
    val minutesString = when {
        hours == 0L && minutes == 0L -> ""
        else -> "${minutes}m "
    }
    val secondsString = "${seconds}s "

    return "$hoursString$minutesString$secondsString"
}

@Composable
fun ColorScheme.isLight() = this.background.luminance() > 0.5