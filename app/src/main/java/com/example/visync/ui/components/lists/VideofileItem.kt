package com.example.visync.ui.components.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.visync.data.videofiles.Videofile

@Composable
fun VideofileItem(
    videofile: Videofile,
    onClick: (Videofile) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick(videofile) }
    ) {
        Text(
            text = videofile.metadata.filename,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = videofile.uri.path ?: "path unknown",
            style = MaterialTheme.typography.titleSmall
        )
    }
}