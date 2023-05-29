package com.example.visync.ui.components.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource

@Composable
fun VisyncBottomNavigationBar(
    selectedDestination: String,
    navigateToMainDestination: (Route) -> Unit,
) {
    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        MAIN_DESTINATIONS.forEach { mainDestination ->
            NavigationBarItem(
                selected = selectedDestination == mainDestination.routeString,
                onClick = { navigateToMainDestination(mainDestination) },
                icon = {
                    val routeIcon = mainDestination.icon
                    val icon = if (routeIcon.resourceId != null) {
                        ImageVector.vectorResource(id = routeIcon.resourceId)
                    } else {
                        routeIcon.imageVector
                    }!! // since you must construct RouteIcon through either of those
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(
                            id = mainDestination.actionDescriptionId
                        )
                    )
                }
            )
        }
    }
}