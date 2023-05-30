package com.example.visync.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.visync.ui.components.VisyncTopAppBar
import com.example.visync.ui.components.navigation.ModalNavigationDrawerContent
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.VisyncBottomNavigationBar
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
    val navigationType: NavigationType
    val preferredDisplayMode: ContentDisplayMode

    Log.i("WindowSize", "widthSizeClass=${windowSize.widthSizeClass}")
    Log.i("WindowSize", "heightSizeClass=${windowSize.heightSizeClass}")
    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            navigationType = NavigationType.BOTTOM_NAVBAR_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
        WindowWidthSizeClass.Medium -> {
            navigationType = NavigationType.RAIL_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
        WindowWidthSizeClass.Expanded -> {
            navigationType = NavigationType.PERMANENT_DRAWER
            preferredDisplayMode = ContentDisplayMode.DUAL_COLUMN
        }
        else -> {
            navigationType = NavigationType.BOTTOM_NAVBAR_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
    }

    VisyncNavigationWrapper(
        navigationType = navigationType,
        preferredDisplayMode = preferredDisplayMode,
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

    if (navigationType == NavigationType.BOTTOM_NAVBAR_AND_DRAWER) {
        ModalNavigationDrawer(
            drawerContent = {
                ModalNavigationDrawerContent(
                    selectedDestination = selectedDestination,
                    navigateToAccountDestination = {
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
                navController = navController,
                selectedDestination = selectedDestination,
                openDrawer = {
                    scope.launch {
                        drawerState.open()
                    }
                },
                navigateToMainDestination = navigationActions::navigateTo
            )
        }
    }
}

@Composable
fun VisyncAppContent(
    preferredDisplayMode: ContentDisplayMode,
    navController: NavHostController,
    selectedDestination: String,
    openDrawer: () -> Unit,
    navigateToMainDestination: (Route) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
    ) {
        VisyncTopAppBar(
            openDrawer = openDrawer,
            selectedDestination = selectedDestination,
        )
        VisyncNavHost(
            navController = navController,
            modifier = Modifier.weight(1f),
        )
        VisyncBottomNavigationBar(
            selectedDestination = selectedDestination,
            navigateToMainDestination = navigateToMainDestination
        )
    }

}

@Composable
fun VisyncNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Playlists.routeString,
        modifier = modifier,
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
    BOTTOM_NAVBAR_AND_DRAWER, RAIL_AND_DRAWER, PERMANENT_DRAWER
}

enum class ContentDisplayMode {
    SINGLE_COLUMN, DUAL_COLUMN
}
