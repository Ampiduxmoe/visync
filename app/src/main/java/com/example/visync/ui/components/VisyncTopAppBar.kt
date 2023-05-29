package com.example.visync.ui.components

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.visync.R
import com.example.visync.ui.components.navigation.ALL_DESTINATIONS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisyncTopAppBar(
    openDrawer: () -> Unit,
    selectedDestination: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            val selectedRoute = ALL_DESTINATIONS.find { it.routeString == selectedDestination }
            /*
                double bang `selectedRoute` since we should not select destination
                that is not in ALL_DESTINATIONS
             */
            val routeNameId = selectedRoute!!.actionDescriptionId
            Text(text = stringResource(id = routeNameId))
        },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(id = R.string.navigation_drawer)
                )
            }
        },
        actions = {
            IconButton(onClick = {
                Toast.makeText(
                    context,
                    "Search is not implemented",
                    Toast.LENGTH_SHORT
                ).show()
            }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search_button)
                )
            }
        },
        modifier = modifier
    )
}