package com.example.visync.ui

import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.visync.ui.components.navigation.CollapsableNavigationDrawer
import com.example.visync.ui.components.navigation.ModalNavigationDrawerContent
import com.example.visync.ui.components.navigation.PermanentDrawerVisibility
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.VisyncBottomNavigationBar
import com.example.visync.ui.components.navigation.VisyncNavigationActions
import com.example.visync.ui.components.navigation.VisyncNavigationRail
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

    val railAndDrawerScrollState = rememberScrollState()

    when (navigationType) {
        NavigationType.BOTTOM_NAVBAR_AND_DRAWER,
        NavigationType.RAIL_AND_DRAWER-> {
            ModalNavigationDrawer(
                drawerContent = {
                    ModalNavigationDrawerContent(
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it)
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        scrollState = railAndDrawerScrollState,
                        showMainDestinations = true,
                        closeDrawer = {
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                },
                drawerState = drawerState
            ) {
                VisyncAppContent(
                    navigationType = navigationType,
                    preferredDisplayMode = preferredDisplayMode,
                    navController = navController,
                    selectedDestination = selectedDestination,
                    railAndDrawerScrollState = railAndDrawerScrollState,
                    openDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    navigateToDestination = navigationActions::navigateTo
                )
            }
        }
        NavigationType.PERMANENT_DRAWER -> {
            val collapsableDrawerState = remember {
                mutableStateOf(PermanentDrawerVisibility.EXPANDED)
            }
            CollapsableNavigationDrawer(
                selectedDestination = selectedDestination,
                navigateToDestination = navigationActions::navigateTo,
                scrollState = rememberScrollState(),
                drawerState = collapsableDrawerState,
            ) {
                VisyncAppContent(
                    navigationType = navigationType,
                    preferredDisplayMode = preferredDisplayMode,
                    navController = navController,
                    selectedDestination = selectedDestination,
                    railAndDrawerScrollState = railAndDrawerScrollState,
                    openDrawer = {},
                    navigateToDestination = navigationActions::navigateTo
                )
            }
        }
    }
}

@Composable
fun VisyncAppContent(
    navigationType: NavigationType,
    preferredDisplayMode: ContentDisplayMode,
    navController: NavHostController,
    selectedDestination: String,
    railAndDrawerScrollState: ScrollState?,
    openDrawer: () -> Unit,
    navigateToDestination: (Route) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (navigationType == NavigationType.RAIL_AND_DRAWER) {
            VisyncNavigationRail(
                selectedDestination = selectedDestination,
                navigateToDestination = navigateToDestination,
                scrollState = railAndDrawerScrollState!!,
                openDrawer = openDrawer,
                alwaysShowDestinationLabels = false
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.inverseOnSurface)
        ) {
            VisyncNavHost(
                navController = navController,
                modifier = Modifier.weight(1f),
                openDrawer = openDrawer
            )
            if (navigationType == NavigationType.BOTTOM_NAVBAR_AND_DRAWER) {
                VisyncBottomNavigationBar(
                    selectedDestination = selectedDestination,
                    navigateToDestination = navigateToDestination
                )
            }
        }
    }
}

@Composable
fun VisyncNavHost(
    navController: NavHostController,
    openDrawer: () -> Unit,
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
                playlistsUiState = playlistsUiState,
                openPlaylist = { playlistsScreenViewModel.setSelectedPlaylist(it.id) },
                closePlaylist = { playlistsScreenViewModel.closeDetailScreen() },
                openDrawer = openDrawer
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
