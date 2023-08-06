package com.example.visync.ui.components.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.visync.R

class VisyncNavigationActions(private val navController: NavHostController) {

    fun navigateTo(destination: String) {
        navController.navigate(destination) {
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

    fun back() {
        navController.navigateUp()
    }
}

val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelRoute.MainApp,
    TopLevelRoute.Player,
)

/**
 *  Destinations indicating whether user will be
 *  hosting video playback to a room themselves or
 *  join existing room with someone who already started hosting.
 */
val CONNECTION_MODE_DESTINATIONS = listOf(
    Route.PlaybackSetup,
    Route.RoomsJoin
)

/**
 *  Destinations for managing your account:
 *  your profile, friends, rooms you are part of,
 *  or just app settings.
 */
val ACCOUNT_RELATED_DESTINATIONS = listOf(
    Route.MyProfile,
    Route.Friends,
    Route.RoomsManage,
    Route.AppSettings
)

val MAIN_APP_DESTINATIONS = listOf(
    CONNECTION_MODE_DESTINATIONS,
    ACCOUNT_RELATED_DESTINATIONS,
).flatten()

sealed class TopLevelRoute(
    val routeString: String,
) {
    object MainApp : TopLevelRoute(
        routeString = "mainApp"
    )
    object Player : TopLevelRoute(
        routeString = "player"
    )
}

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

    object PlaybackSetup : Route(
        routeString = "playback_setup",
        icon = RouteIcon(Icons.Filled.PlayArrow),
        actionDescriptionId = R.string.tab_label_playback_setup
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

    // TODO: either remove imageVector or make separate class with it

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