package com.example.visync.ui

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.visync.ui.components.navigation.CollapsableNavigationDrawer
import com.example.visync.ui.components.navigation.CollapsableNavigationDrawerContent
import com.example.visync.ui.components.navigation.ModalNavigationDrawerContent
import com.example.visync.ui.components.navigation.CollapsableDrawerState
import com.example.visync.ui.components.navigation.Route
import com.example.visync.ui.components.navigation.VisyncBottomNavigationBar
import com.example.visync.ui.components.navigation.VisyncNavigationActions
import com.example.visync.ui.components.navigation.VisyncNavigationRail
import com.example.visync.ui.screens.PlayerScreen
import com.example.visync.ui.screens.PlayerScreenViewModel
import com.example.visync.ui.screens.PlaylistsScreen
import com.example.visync.ui.screens.PlaylistsScreenViewModel
import com.example.visync.ui.screens.RoomsScreen
import com.example.visync.ui.screens.RoomsScreenViewModel
import kotlinx.coroutines.launch

@Composable
fun VisyncApp(
    windowSize: WindowSizeClass,
) {
    val navigationType: NavigationType
    val preferredDisplayMode: ContentDisplayMode

    Log.i("WindowSize", "widthSizeClass=${windowSize.widthSizeClass}")
    Log.i("WindowSize", "heightSizeClass=${windowSize.heightSizeClass}")

    val visyncAppViewModel = hiltViewModel<VisyncAppViewModel>()
    val visyncAppUiState by visyncAppViewModel
        .uiState.collectAsStateWithLifecycle()

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

    VisyncNavigationWrapper(
        navigationType = navigationType,
        preferredDisplayMode = preferredDisplayMode,
        visyncAppUiState = visyncAppUiState,
        hideAllNavigation = visyncAppViewModel::hideNavigation,
        showNavigation = visyncAppViewModel::showNavigation,
    )
}

@Composable
fun VisyncNavigationWrapper(
    navigationType: NavigationType,
    preferredDisplayMode: ContentDisplayMode,
    visyncAppUiState: VisyncAppUiState,
    hideAllNavigation: () -> Unit,
    showNavigation: () -> Unit,
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
                        showMainDestinations = navigationType == NavigationType.RAIL_AND_DRAWER,
                        closeDrawer = {
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                },
                drawerState = drawerState,
                gesturesEnabled = visyncAppUiState.showNavigation
            ) {
                VisyncAppContent.ForModalDrawer(
                    navigationType = navigationType,
                    preferredDisplayMode = preferredDisplayMode,
                    visyncAppUiState = visyncAppUiState,
                    hideAllNavigation = hideAllNavigation,
                    showNavigation = showNavigation,
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
        NavigationType.CUSTOM_PERMANENT_DRAWER -> {
            val collapsableDrawerState = remember {
                mutableStateOf(CollapsableDrawerState.EXPANDED)
            }
            CollapsableNavigationDrawer(
                drawerContent = {
                    if (visyncAppUiState.showNavigation) {
                        CollapsableNavigationDrawerContent(
                            selectedDestination = selectedDestination,
                            navigateToDestination = navigationActions::navigateTo,
                            scrollState = rememberScrollState(),
                            drawerState = collapsableDrawerState,
                        )
                    }
                }
            ) {
                VisyncAppContent.ForCustomPermanentDrawer(
                    navigationType = navigationType,
                    preferredDisplayMode = preferredDisplayMode,
                    visyncAppUiState = visyncAppUiState,
                    hideAllNavigation = hideAllNavigation,
                    showNavigation = showNavigation,
                    navController = navController,
                    selectedDestination = selectedDestination,
                    railAndDrawerScrollState = railAndDrawerScrollState,
                    navigateToDestination = navigationActions::navigateTo,
                )
            }
        }
    }
}

@Composable
fun VisyncNavHost(
    preferredDisplayMode: ContentDisplayMode,
    hideAllNavigation: () -> Unit,
    showNavigation: () -> Unit,
    navController: NavHostController,
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerScreenViewModel = hiltViewModel<PlayerScreenViewModel>()
    val closePlayer: () -> Unit = {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != Route.Player.routeString) {
            throw Exception("closePlayer was called when there is no player shown")
        }
        showNavigation()
        navController.navigateUp()
    }

    /*
        TODO:
            Fix UI jumping for a couple of frames.
            Navigation to and from player is janky
            because it consists of removing navigation + navigating.
            Nested navigation might be needed for this.
     */
    val noEnterTransitionFromPlayer: (
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?
    ) = {
        if (initialState.destination.route == Route.Player.routeString) {
            EnterTransition.None
        } else {
            null
        }
    }
    val noExitTransitionToPlayer: (
        AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?
    ) = {
        if (targetState.destination.route == Route.Player.routeString) {
            ExitTransition.None
        } else {
            null
        }
    }
    NavHost(
        navController = navController,
        startDestination = Route.Playlists.routeString,
        modifier = modifier,
    ) {
        composable(
            Route.Player.routeString,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            val playerScreenUiState by playerScreenViewModel
                .uiState.collectAsStateWithLifecycle()
            PlayerScreen(
                playerScreenUiState = playerScreenUiState,
                closePlayer = closePlayer
            ) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).also {
                            it.player = playerScreenViewModel.getPlayer()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                )
            }
        }
        composable(
            Route.Playlists.routeString,
            enterTransition = noEnterTransitionFromPlayer,
            exitTransition = noExitTransitionToPlayer
        ) {
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
                    playerScreenViewModel.setVideofilesToPlay(
                        videoFilesToPlay = playlistsUiState.playlists.find { playlistWithVideofiles ->
                            playlistWithVideofiles.playlist.id == videofile.playlistId
                        }?.videofiles ?: listOf(),
                        startFrom = videofile
                    )
                    hideAllNavigation()
                    navController.navigate(Route.Player.routeString)
                },
                openDrawer = openDrawer
            )
        }
        composable(
            Route.RoomsJoin.routeString,
            enterTransition = noEnterTransitionFromPlayer,
            exitTransition = noExitTransitionToPlayer
        ) {
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

private object VisyncAppContent {
    @Composable
    fun ForModalDrawer(
        navigationType: NavigationType,
        preferredDisplayMode: ContentDisplayMode,
        visyncAppUiState: VisyncAppUiState,
        hideAllNavigation: () -> Unit,
        showNavigation: () -> Unit,
        navController: NavHostController,
        selectedDestination: String,
        railAndDrawerScrollState: ScrollState?,
        openDrawer: () -> Unit,
        navigateToDestination: (Route) -> Unit,
    ) {
        UniversalContent(
            navigationType = navigationType,
            preferredDisplayMode = preferredDisplayMode,
            visyncAppUiState = visyncAppUiState,
            hideAllNavigation = hideAllNavigation,
            showNavigation = showNavigation,
            navController = navController,
            selectedDestination = selectedDestination,
            railAndDrawerScrollState = railAndDrawerScrollState,
            openDrawer = openDrawer,
            navigateToDestination = navigateToDestination,
        )
    }

    @Composable
    fun ForCustomPermanentDrawer(
        navigationType: NavigationType,
        preferredDisplayMode: ContentDisplayMode,
        visyncAppUiState: VisyncAppUiState,
        hideAllNavigation: () -> Unit,
        showNavigation: () -> Unit,
        navController: NavHostController,
        selectedDestination: String,
        railAndDrawerScrollState: ScrollState?,
        navigateToDestination: (Route) -> Unit,
    ) {
        UniversalContent(
            navigationType = navigationType,
            preferredDisplayMode = preferredDisplayMode,
            visyncAppUiState = visyncAppUiState,
            hideAllNavigation = hideAllNavigation,
            showNavigation = showNavigation,
            navController = navController,
            selectedDestination = selectedDestination,
            railAndDrawerScrollState = railAndDrawerScrollState,
            openDrawer = {},
            navigateToDestination = navigateToDestination,
        )
    }

    @Composable
    private fun UniversalContent(
        navigationType: NavigationType,
        preferredDisplayMode: ContentDisplayMode,
        visyncAppUiState: VisyncAppUiState,
        hideAllNavigation: () -> Unit,
        showNavigation: () -> Unit,
        navController: NavHostController,
        selectedDestination: String,
        railAndDrawerScrollState: ScrollState?,
        openDrawer: () -> Unit,
        navigateToDestination: (Route) -> Unit,
    ) {
        when (navigationType) {
            NavigationType.RAIL_AND_DRAWER -> {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedVisibility(
                        visible = visyncAppUiState.showNavigation,
                        enter = expandHorizontally(expandFrom = Alignment.End) { 0 }
                                + fadeIn(initialAlpha = 0f),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End) { 0 }
                                + fadeOut(targetAlpha = 0f)
                    ) {
                        VisyncNavigationRail(
                            selectedDestination = selectedDestination,
                            navigateToDestination = navigateToDestination,
                            scrollState = railAndDrawerScrollState!!,
                            openDrawer = openDrawer,
                            alwaysShowDestinationLabels = false
                        )
                    }
                    VisyncNavHost(
                        navController = navController,
                        preferredDisplayMode = preferredDisplayMode,
                        hideAllNavigation = hideAllNavigation,
                        showNavigation = showNavigation,
                        openDrawer = openDrawer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            NavigationType.BOTTOM_NAVBAR_AND_DRAWER -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    VisyncNavHost(
                        navController = navController,
                        preferredDisplayMode = preferredDisplayMode,
                        hideAllNavigation = hideAllNavigation,
                        showNavigation = showNavigation,
                        openDrawer = openDrawer,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedVisibility(
                        visible = visyncAppUiState.showNavigation,
                        enter = expandVertically(expandFrom = Alignment.Top) { 0 }
                                + fadeIn(initialAlpha = 0f),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) { 0 }
                                + fadeOut(targetAlpha = 0f)
                    ) {
                        VisyncBottomNavigationBar(
                            selectedDestination = selectedDestination,
                            navigateToDestination = navigateToDestination
                        )
                    }
                }
            }
            NavigationType.CUSTOM_PERMANENT_DRAWER -> {
                VisyncNavHost(
                    navController = navController,
                    preferredDisplayMode = preferredDisplayMode,
                    hideAllNavigation = hideAllNavigation,
                    showNavigation = showNavigation,
                    openDrawer = openDrawer,
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
