package com.example.visync.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.visync.ui.components.navigation.ModalNavigationDrawerContent
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.VisyncNavigationActions
import com.example.visync.ui.screens.PlaylistsScreen
import com.example.visync.ui.screens.PlaylistsScreenViewModel
import com.example.visync.ui.screens.RoomsScreen
import com.example.visync.ui.screens.RoomsScreenViewModel
import kotlinx.coroutines.launch

@Composable
fun VisyncApp(
    windowSize: WindowSizeClass
) {
    /*
        TODO some adaptivity calculations
     */

    VisyncNavigationWrapper(
        navigationType = NavigationType.DRAWER_AND_BOTTOM_NAVBAR,
        preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
    )
}


@Composable
fun VisyncNavigationWrapper(
    navigationType: NavigationType,
    preferredDisplayMode: ContentDisplayMode
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        VisyncNavigationActions(navController)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination =
        navBackStackEntry?.destination?.route ?: Route.Playlists.routeString

    if (navigationType == NavigationType.DRAWER_AND_BOTTOM_NAVBAR) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalNavigationDrawerContent(
                    selectedDestination = selectedDestination,
                    navigateTo = {
                        navigationActions.navigateTo(it)
                        scope.launch {
                            drawerState.close()
                        }
                    },
                    onDrawerClicked = {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            },
            drawerState = drawerState
        ) {
            VisyncAppContent(
                preferredDisplayMode = preferredDisplayMode,
                navController = navController
            )
        }
    }
}

@Composable
fun VisyncAppContent(
    preferredDisplayMode: ContentDisplayMode,
    navController: NavHostController
) {
    VisyncNavHost(
        navController = navController
    )
}

@Composable
fun VisyncNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Route.Playlists.routeString,
    ) {
        composable(Route.Playlists.routeString) {
            val playlistsScreenViewModel = hiltViewModel<PlaylistsScreenViewModel>()
            val playlistsUiState by playlistsScreenViewModel
                .uiState.collectAsStateWithLifecycle()
            PlaylistsScreen(
                playlistsUiState = playlistsUiState
            )
        }
        composable(Route.RoomsJoin.routeString) {
            val roomsScreenViewModel = hiltViewModel<RoomsScreenViewModel>()
            val roomsUiState by roomsScreenViewModel
                .uiState.collectAsStateWithLifecycle()
            RoomsScreen(
                roomsUiState = roomsUiState
            )
        }
        composable(Route.MyProfile.routeString) {
            Column() {

            }
        }
        composable(Route.Friends.routeString) {
            Column() {

            }
        }
        composable(Route.RoomsManage.routeString) {
            Column() {

            }
        }
        composable(Route.AppSettings.routeString) {
            Column() {

            }
        }
    }
}

enum class NavigationType {
    DRAWER_AND_BOTTOM_NAVBAR, RAIL, PERMANENT_DRAWER
}

enum class ContentDisplayMode {
    SINGLE_COLUMN, DUAL_COLUMN
}
