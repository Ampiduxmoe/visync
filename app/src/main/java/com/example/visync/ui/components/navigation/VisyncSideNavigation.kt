package com.example.visync.ui.components.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.visync.R

@Composable
fun ModalNavigationDrawerContent(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    showMainDestinations: Boolean,
    closeDrawer: () -> Unit = {},
) {
    ModalDrawerSheet {
        DrawerSheetContent(
            selectedDestination = selectedDestination,
            navigateToDestination = navigateToDestination,
            scrollState = scrollState,
            showCloseDrawerButton = true,
            showMainDestinations = showMainDestinations,
            closeDrawer = closeDrawer,
        )
    }
}

@Composable
fun PermanentNavigationDrawerContent(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
) {
    PermanentDrawerSheet(
        modifier = Modifier.width(256.dp)
    ) {
        DrawerSheetContent(
            selectedDestination = selectedDestination,
            navigateToDestination = navigateToDestination,
            scrollState = scrollState,
            showCloseDrawerButton = false,
            showMainDestinations = true,
            closeDrawer = {},
        )
    }
}

@Composable
private fun DrawerSheetContent(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    showCloseDrawerButton: Boolean,
    showMainDestinations: Boolean,
    closeDrawer: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.app_name).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (showCloseDrawerButton) {
                IconButton(onClick = closeDrawer) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(id = R.string.navigation_drawer)
                    )
                }
            }
        }

        if (showMainDestinations) {
            MAIN_DESTINATIONS.forEach { mainDestination ->
                VisyncNavigationDrawerItem(
                    destination = mainDestination,
                    isSelected = selectedDestination == mainDestination.routeString,
                    navigateToDestination = navigateToDestination,
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(id = R.string.navigation_drawer_fab),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(id = R.string.navigation_drawer_fab_text),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
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
private fun VisyncNavigationDrawerItem(
    destination: Route,
    isSelected: Boolean,
    navigateToDestination: (Route) -> Unit,
) {
    NavigationDrawerItem(
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
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    openDrawer: () -> Unit,
    showDestinationLabels: Boolean,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.inverseOnSurface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier
                .verticalScroll(scrollState)
        ) {
            NavigationRailItem(
                selected = false,
                onClick = openDrawer,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(id = R.string.navigation_drawer)
                    )
                },
                modifier = Modifier.padding(vertical = 16.dp),
            )
            MAIN_DESTINATIONS.forEach { accountDestination ->
                VisyncNavigationRailItem(
                    destination = accountDestination,
                    isSelected = selectedDestination == accountDestination.routeString,
                    navigateToDestination = navigateToDestination,
                    showDestinationLabels = showDestinationLabels
                )
            }
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .align(CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(id = R.string.navigation_drawer_fab),
                )
            }
            ACCOUNT_RELATED_DESTINATIONS.forEach { accountDestination ->
                VisyncNavigationRailItem(
                    destination = accountDestination,
                    isSelected = selectedDestination == accountDestination.routeString,
                    navigateToDestination = navigateToDestination,
                    showDestinationLabels = showDestinationLabels
                )
            }
        }
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
                modifier = Modifier.padding(horizontal = 16.dp)
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
        colors = NavigationRailItemDefaults.colors(

        ),
        onClick = { navigateToDestination(destination) }
    )
}