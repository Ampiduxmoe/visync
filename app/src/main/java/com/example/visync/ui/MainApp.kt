package com.example.visync.ui

import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DrawerValue
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
import com.example.visync.data.playlists.PlaylistWithVideofiles
import com.example.visync.ui.components.navigation.CollapsableNavigationDrawer
import com.example.visync.ui.components.navigation.CollapsableNavigationDrawerContent
import com.example.visync.ui.components.navigation.ModalNavigationDrawerContent
import com.example.visync.ui.components.navigation.CollapsableDrawerState
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.VisyncBottomNavigationBar
import com.example.visync.ui.components.navigation.AppNavigationActions
import com.example.visync.ui.components.navigation.VisyncNavigationRail
import com.example.visync.ui.screens.PlaylistsScreen
import com.example.visync.ui.screens.PlaylistsScreenViewModel
import com.example.visync.ui.screens.RoomsScreen
import com.example.visync.ui.screens.RoomsScreenViewModel
import kotlinx.coroutines.launch

@Composable
fun MainApp(
    windowSize: WindowSizeClass,
    mainAppUiState: MainAppUiState,
    playPlaylist: (playlist: PlaylistWithVideofiles, startFrom: Int) -> Unit,
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
            navigationType = NavigationType.CUSTOM_PERMANENT_DRAWER
            preferredDisplayMode = ContentDisplayMode.DUAL_COLUMN
        }
        else -> {
            navigationType = NavigationType.BOTTOM_NAVBAR_AND_DRAWER
            preferredDisplayMode = ContentDisplayMode.SINGLE_COLUMN
        }
    }

    MainNavigationWrapper(
        navigationType = navigationType,
        preferredDisplayMode = preferredDisplayMode,
        mainAppUiState = mainAppUiState,
        playPlaylist = playPlaylist
    )
}

@Composable
fun MainNavigationWrapper(
    navigationType: NavigationType,
    preferredDisplayMode: ContentDisplayMode,
    mainAppUiState: MainAppUiState,
    playPlaylist: (playlist: PlaylistWithVideofiles, startFrom: Int) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        AppNavigationActions(navController)
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
                            navigationActions.navigateTo(it.routeString)
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        scrollState = railAndDrawerScrollState,
                        showMainDestinations = navigationType == NavigationType.RAIL_AND_DRAWER,
                        closeDrawer = {
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                },
                drawerState = drawerState
            ) {
                MainAppContent.ForModalDrawer(
                    navigationType = navigationType,
                    preferredDisplayMode = preferredDisplayMode,
                    mainAppUiState = mainAppUiState,
                    navController = navController,
                    selectedDestination = selectedDestination,
                    railAndDrawerScrollState = railAndDrawerScrollState,
                    openDrawer = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    navigateToDestination = {
                        navigationActions.navigateTo(it.routeString)
                    },
                    playPlaylist = playPlaylist
                )
            }
        }
        NavigationType.CUSTOM_PERMANENT_DRAWER -> {
            val collapsableDrawerState = remember {
                mutableStateOf(CollapsableDrawerState.EXPANDED)
            }
            CollapsableNavigationDrawer(
                drawerContent = {
                    CollapsableNavigationDrawerContent(
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                        },
                        scrollState = rememberScrollState(),
                        drawerState = collapsableDrawerState,
                    )
                }
            ) {
                MainAppContent.ForCustomPermanentDrawer(
                    navigationType = navigationType,
                    preferredDisplayMode = preferredDisplayMode,
                    mainAppUiState = mainAppUiState,
                    navController = navController,
                    selectedDestination = selectedDestination,
                    railAndDrawerScrollState = railAndDrawerScrollState,
                    navigateToDestination = {
                        navigationActions.navigateTo(it.routeString)
                    },
                    playPlaylist = playPlaylist
                )
            }
        }
    }
}

@Composable
fun MainAppNavHost(
    preferredDisplayMode: ContentDisplayMode,
    navController: NavHostController,
    openDrawer: () -> Unit,
    playPlaylist: (playlist: PlaylistWithVideofiles, startFrom: Int) -> Unit,
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
                preferredDisplayMode = preferredDisplayMode,
                playlistsUiState = playlistsUiState,
                openPlaylist = { playlistsScreenViewModel.setSelectedPlaylist(it.id) },
                closePlaylist = playlistsScreenViewModel::closeDetailScreen,
                addVideoToPlaylistFromUri = playlistsScreenViewModel::addVideoToPlaylistFromUri,
                playVideofile = { videofile ->
                    val parentPlaylistWithVideofiles = playlistsUiState.playlists
                        .find { playlistWithVideofiles ->
                            playlistWithVideofiles.playlist.id == videofile.playlistId
                        }!! // since videofile always belongs to a playlist
                    val videofileIndex = parentPlaylistWithVideofiles.videofiles
                        .indexOf(videofile)
                    playPlaylist(
                        /* playlist = */ parentPlaylistWithVideofiles,
                        /* startFrom */ videofileIndex
                    )
                },
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
            Column {

            }
        }
        composable(Route.Friends.routeString) {
            Column {

            }
        }
        composable(Route.RoomsManage.routeString) {
            Column {

            }
        }
        composable(Route.AppSettings.routeString) {
            Column {

            }
        }
    }
}

private object MainAppContent {
    @Composable
    fun ForModalDrawer(
        navigationType: NavigationType,
        preferredDisplayMode: ContentDisplayMode,
        mainAppUiState: MainAppUiState,
        navController: NavHostController,
        selectedDestination: String,
        railAndDrawerScrollState: ScrollState?,
        openDrawer: () -> Unit,
        navigateToDestination: (Route) -> Unit,
        playPlaylist: (playlist: PlaylistWithVideofiles, startFrom: Int) -> Unit,
    ) {
        UniversalContent(
            navigationType = navigationType,
            preferredDisplayMode = preferredDisplayMode,
            mainAppUiState = mainAppUiState,
            navController = navController,
            selectedDestination = selectedDestination,
            railAndDrawerScrollState = railAndDrawerScrollState,
            openDrawer = openDrawer,
            navigateToDestination = navigateToDestination,
            playPlaylist = playPlaylist,
        )
    }

    @Composable
    fun ForCustomPermanentDrawer(
        navigationType: NavigationType,
        preferredDisplayMode: ContentDisplayMode,
        mainAppUiState: MainAppUiState,
        navController: NavHostController,
        selectedDestination: String,
        railAndDrawerScrollState: ScrollState?,
        navigateToDestination: (Route) -> Unit,
        playPlaylist: (playlist: PlaylistWithVideofiles, startFrom: Int) -> Unit,
    ) {
        UniversalContent(
            navigationType = navigationType,
            preferredDisplayMode = preferredDisplayMode,
            mainAppUiState = mainAppUiState,
            navController = navController,
            selectedDestination = selectedDestination,
            railAndDrawerScrollState = railAndDrawerScrollState,
            openDrawer = {},
            navigateToDestination = navigateToDestination,
            playPlaylist = playPlaylist,
        )
    }

    @Composable
    private fun UniversalContent(
        navigationType: NavigationType,
        preferredDisplayMode: ContentDisplayMode,
        mainAppUiState: MainAppUiState,
        navController: NavHostController,
        selectedDestination: String,
        railAndDrawerScrollState: ScrollState?,
        openDrawer: () -> Unit,
        navigateToDestination: (Route) -> Unit,
        playPlaylist: (playlist: PlaylistWithVideofiles, startFrom: Int) -> Unit,
    ) {
        when (navigationType) {
            NavigationType.RAIL_AND_DRAWER -> {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    VisyncNavigationRail(
                        selectedDestination = selectedDestination,
                        navigateToDestination = navigateToDestination,
                        scrollState = railAndDrawerScrollState!!,
                        openDrawer = openDrawer,
                        alwaysShowDestinationLabels = false
                    )
                    MainAppNavHost(
                        preferredDisplayMode = preferredDisplayMode,
                        navController = navController,
                        openDrawer = openDrawer,
                        playPlaylist = playPlaylist,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            NavigationType.BOTTOM_NAVBAR_AND_DRAWER -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainAppNavHost(
                        preferredDisplayMode = preferredDisplayMode,
                        navController = navController,
                        openDrawer = openDrawer,
                        playPlaylist = playPlaylist,
                        modifier = Modifier.weight(1f)
                    )
                    VisyncBottomNavigationBar(
                        selectedDestination = selectedDestination,
                        navigateToDestination = navigateToDestination
                    )
                }
            }
            NavigationType.CUSTOM_PERMANENT_DRAWER -> {
                MainAppNavHost(
                    preferredDisplayMode = preferredDisplayMode,
                    navController = navController,
                    openDrawer = openDrawer,
                    playPlaylist = playPlaylist,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

enum class NavigationType {
    BOTTOM_NAVBAR_AND_DRAWER, RAIL_AND_DRAWER, CUSTOM_PERMANENT_DRAWER
}

enum class ContentDisplayMode {
    SINGLE_COLUMN, DUAL_COLUMN
}
