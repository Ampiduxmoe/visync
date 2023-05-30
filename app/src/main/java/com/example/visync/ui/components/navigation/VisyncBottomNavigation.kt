package com.example.visync.ui.components.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun VisyncBottomNavigationBar(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
) {
    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        MAIN_DESTINATIONS.forEach { mainDestination ->
            NavigationBarItem(
                selected = selectedDestination == mainDestination.routeString,
                onClick = { navigateToDestination(mainDestination) },
                label = {
                    Text(
                        text = stringResource(id = mainDestination.actionDescriptionId),
                    )
                },
                icon = {
                    Icon(
                        imageVector = mainDestination.getImageVectorIcon(),
                        contentDescription = stringResource(
                            id = mainDestination.actionDescriptionId
                        )
                    )
                }
            )
        }
    }
}