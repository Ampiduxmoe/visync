package com.example.visync.ui.components.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
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

val SPECIAL_DESTINATIONS = listOf(
    Route.Player
)

val ALL_DESTINATIONS = listOf(
    MAIN_DESTINATIONS,
    ACCOUNT_RELATED_DESTINATIONS,
    SPECIAL_DESTINATIONS,
).flatten()

sealed class Route(
    val routeString: String,
    val icon: RouteIcon,
    @StringRes val actionDescriptionId: Int
) {
    @Composable
    fun getImageVectorIcon(): ImageVector = if (icon.resourceId != null) {
        ImageVector.vectorResource(id = icon.resourceId)
    } else {
        icon.imageVector
    }!!

    object Player : Route(
        routeString = "player",
        icon = RouteIcon(Icons.Outlined.PlayArrow),
        actionDescriptionId = R.string.tab_label_player
    )

    object Playlists : Route(
        routeString = "playlists",
        icon = RouteIcon(Icons.Filled.PlayArrow),
        actionDescriptionId = R.string.tab_label_playlists
    )
    object RoomsJoin : Route(
        routeString = "rooms_join",
        icon = RouteIcon(R.drawable.ic_phones),
        actionDescriptionId = R.string.tab_label_rooms_join
    )
    object MyProfile : Route(
        routeString = "my_profile",
        icon = RouteIcon(Icons.Filled.Person),
        actionDescriptionId = R.string.tab_label_my_profile
    )
    object Friends : Route(
        routeString = "my_friends",
        icon = RouteIcon(R.drawable.ic_friends),
        actionDescriptionId = R.string.tab_label_my_friends
    )
    object RoomsManage : Route(
        routeString = "rooms_manage",
        icon = RouteIcon(R.drawable.ic_group_add),
        actionDescriptionId = R.string.tab_label_rooms_manage
    )
    object AppSettings : Route(
        routeString = "app_settings",
        icon = RouteIcon(Icons.Filled.Settings),
        actionDescriptionId = R.string.tab_label_app_settings
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