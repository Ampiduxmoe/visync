package com.example.visync.ui.screens.main.playback_setup

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.visync.R
import com.example.visync.data.videofiles.Videofile
import com.example.visync.metadata.MetadataReader
import com.example.visync.metadata.VideoMetadata
import com.example.visync.metadata.VisyncMetadataReader

@Composable
fun SetupTabSelectFiles(
    isUserHost: Boolean,
    selectedVideofiles: List<Videofile>,
    playbackSetupOptions: PlaybackSetupOptions,
    setSelectedVideofiles: (List<Videofile>) -> Unit,
    addVideofiles: (List<Videofile>) -> Unit,
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
                        duration = metadata.duration
                    )
                )
            ))
        }
    }

    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
    ) {
        HostSelectionColumn(
            isUserHost = isUserHost,
            playbackSetupOptions = playbackSetupOptions,
            selectVideofiles = reselectVideos,
            addVideofiles = addVideos,
            setSelectedVideofileIndex = setSelectedVideofileIndex,
            scrollState = scrollState,
            modifier = Modifier.fillMaxSize(.5f)
        )
        if (!isUserHost) {
            GuestSelectionColumn(
                hostSelection = playbackSetupOptions.videofilesMetadata,
                selectedVideofiles = selectedVideofiles,
                missingFilenames = missingFilenames,
                selectVideofiles = reselectVideos,
                addVideofiles = addVideos,
                selectVideofileAndMatchTo = addMatchToFilename,
                scrollState = scrollState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HostSelectionColumn(
    isUserHost: Boolean,
    playbackSetupOptions: PlaybackSetupOptions,
    selectVideofiles: () -> Unit,
    addVideofiles: () -> Unit,
    setSelectedVideofileIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val videofilesMetadata = playbackSetupOptions.videofilesMetadata
    val videofileNames = videofilesMetadata.map { it.filename }
    val hasSelectedVideofiles = videofilesMetadata.isNotEmpty()

    Column(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Host-selected files")
            if (isUserHost && hasSelectedVideofiles) {
                Row {
                    AddVideofilesSmallButton(onClick = addVideofiles)
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectVideofilesSmallButton(onClick = selectVideofiles)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            if (!hasSelectedVideofiles) {
                if (isUserHost) {
                    SelectVideofilesBigButton(onClick = selectVideofiles)
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text("Host did not select anything yet")
                    }
                }
            }
            else {
                videofileNames.forEachIndexed { index, videofileName ->
                    VideofileItem(
                        videofileName = videofileName,
                        isSelected = index == playbackSetupOptions.selectedVideofileIndex,
                        clickModifier = Modifier.clickable {
                            if (isUserHost) {
                                setSelectedVideofileIndex(index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestSelectionColumn(
    hostSelection: List<VideoMetadata>,
    selectedVideofiles: List<Videofile>,
    missingFilenames: List<String>,
    selectVideofiles: () -> Unit,
    addVideofiles: () -> Unit,
    selectVideofileAndMatchTo: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val hasHostSelectedVideofiles = hostSelection.isNotEmpty()
    val hasUserSelectedVideofiles = selectedVideofiles.isNotEmpty()
    Column(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Your files")
            Row {
                AddVideofilesSmallButton(onClick = addVideofiles)
                Spacer(modifier = Modifier.width(8.dp))
                SelectVideofilesSmallButton(onClick = selectVideofiles)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            if (!hasHostSelectedVideofiles) {

            }
            else {
                hostSelection.forEach { metadata ->
                    val hasMatchingHostFile = metadata.filename !in missingFilenames
                    GuestVideofileItem(
                        hasMatchingHostFile = hasMatchingHostFile,
                        clickModifier = Modifier.clickable {
                            if (hasMatchingHostFile) {
                                // TODO show match
                            } else {
                                selectVideofileAndMatchTo(metadata.filename)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestVideofileItem(
    hasMatchingHostFile: Boolean,
    @SuppressLint("ModifierParameter")
    sizeModifier: Modifier = Modifier.height(24.dp),
    clickModifier: Modifier,
) {
    if (hasMatchingHostFile) {
        Text(
            text = "Ok match",
            modifier = sizeModifier.then(clickModifier),
        )
    } else {
        Text(
            text = "No match",
            modifier = sizeModifier.then(clickModifier)
        )
    }
}

@Composable
private fun AddVideofilesSmallButton(
    onClick: () -> Unit,
) {
    Text(
        text = "add files",
        modifier = Modifier.clickable {
            onClick()
        }
    )
}

@Composable
private fun SelectVideofilesSmallButton(
    onClick: () -> Unit,
) {
    Text(
        text = "reselect",
        modifier = Modifier.clickable {
            onClick()
        }
    )
}

@Composable
private fun SelectVideofilesBigButton(
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.clickable { onClick() }
        ) {
            Text("Select videofiles")
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.desc_select_videofiles)
            )
        }
    }

}

@Composable
private fun VideofileItem(
    videofileName: String,
    isSelected: Boolean,
    @SuppressLint("ModifierParameter")
    sizeModifier: Modifier = Modifier.height(24.dp),
    clickModifier: Modifier = Modifier,
) {
    if (isSelected) {
        Text(
            text = videofileName,
            fontWeight = FontWeight.ExtraBold,
            modifier = sizeModifier
        )
    } else {
        Text(
            text = videofileName,
            modifier = sizeModifier.then(clickModifier)
        )
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
                duration = -1
            )
    )
}