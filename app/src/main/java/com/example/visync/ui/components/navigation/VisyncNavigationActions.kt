package com.example.visync.ui.components.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.visync.R

class VisyncNavigationActions(private val navController: NavHostController) {

    fun navigateTo(destination: Route) {
        navController.navigate(destination.routeString) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }
}

val MAIN_DESTINATIONS = listOf(
    Route.Playlists,
    Route.RoomsJoin
)

val ACCOUNT_RELATED_DESTINATIONS = listOf(
    Route.MyProfile,
    Route.Friends,
    Route.RoomsManage,
    Route.AppSettings
)

sealed class Route(
    val routeString: String,
    val icon: RouteIcon,
    @StringRes val actionDescriptionId: Int
) {
    object Playlists : Route(
        routeString = "playlists",
        icon = RouteIcon(Icons.Default.PlayArrow),
        actionDescriptionId = R.string.tab_playlists
    )
    object RoomsJoin : Route(
        routeString = "rooms_join",
        icon = RouteIcon(R.drawable.ic_phones),
        actionDescriptionId = R.string.tab_rooms_join
    )
    object MyProfile : Route(
        routeString = "my_profile",
        icon = RouteIcon(Icons.Default.Person),
        actionDescriptionId = R.string.tab_my_profile
    )
    object Friends : Route(
        routeString = "my_friends",
        icon = RouteIcon(R.drawable.ic_friends),
        actionDescriptionId = R.string.tab_my_friends
    )
    object RoomsManage : Route(
        routeString = "rooms_manage",
        icon = RouteIcon(R.drawable.ic_group_add),
        actionDescriptionId = R.string.tab_rooms_manage
    )
    object AppSettings : Route(
        routeString = "app_settings",
        icon = RouteIcon(Icons.Default.Settings),
        actionDescriptionId = R.string.tab_app_settings
    )
}

class RouteIcon {

    @DrawableRes
    val resourceId: Int?

    val imageVector: ImageVector?

    constructor(@DrawableRes resourceId: Int) {
        this.resourceId = resourceId
        this.imageVector = null
    }

    constructor(imageVector: ImageVector) {
        this.resourceId = null
        this.imageVector = imageVector
    }
}