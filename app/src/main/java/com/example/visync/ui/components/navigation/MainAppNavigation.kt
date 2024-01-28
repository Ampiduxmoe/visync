package com.example.visync.ui.components.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.visync.ui.screens.main.MainAppNavigationUiState
import kotlinx.coroutines.launch

@Composable
fun MainAppNavigation(
    isDarkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    uiState: MainAppNavigationUiState,
    navigationType: NavigationType,
    navController: NavHostController,
    appContent: @Composable MainAppNavigationScope.() -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val railScrollState = rememberScrollState()
    val drawerScrollState = rememberScrollState()
    val railAndDrawerScrollState = remember {
        RailAndDrawerScrollState(railScrollState, drawerScrollState)
    }
    SynchronizeScrolls(railScrollState, drawerScrollState)

    val coroutineScope = rememberCoroutineScope()


    val mainAppNavigationScope = remember {
        MainAppNavigationScope(
            openDrawer = {
                coroutineScope.launch { drawerState.open() }
            },
            closeDrawer = {
                coroutineScope.launch { drawerState.close() }
            }
        )
    }

    val navigationActions = remember(navController) {
        VisyncNavigationActions(navController)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination = navBackStackEntry?.destination?.route
        ?: Route.PlaybackSetup.routeString

    when (navigationType) {
        NavigationType.BOTTOM_NAVBAR_AND_DRAWER -> {
            ModalNavigationDrawer(
//                modifier = Modifier.consumeWindowInsets(WindowInsets.systemBars), // if we want to draw full screen
//                modifier = Modifier.statusBarsPadding().systemBarsPadding(), // if we need to draw safe
                drawerContent = {
                    ModalNavigationDrawerContent(
                        isDarkTheme = isDarkTheme,
                        setDarkTheme = setDarkTheme,
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                            mainAppNavigationScope.closeDrawer()
                        },
                        scrollState = drawerScrollState,
                        showMainDestinations = false,
                        closeDrawer = {
                            mainAppNavigationScope.closeDrawer()
                        },
                        editableUsername = uiState.editableUsername,
                        editablePhysicalDevice = uiState.editablePhysicalDevice
                    )
                },
                drawerState = drawerState
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().statusBarsPadding().systemBarsPadding()
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        mainAppNavigationScope.appContent()
                    }
                    VisyncBottomNavigationBar(
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                        }
                    )
                }
            }
        }
        NavigationType.RAIL_AND_DRAWER-> {
            ModalNavigationDrawer(
                drawerContent = {
                    ModalNavigationDrawerContent(
                        isDarkTheme = isDarkTheme,
                        setDarkTheme = setDarkTheme,
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                            mainAppNavigationScope.closeDrawer()
                        },
                        scrollState = drawerScrollState,
                        showMainDestinations = true,
                        closeDrawer = {
                            mainAppNavigationScope.closeDrawer()
                        },
                        editableUsername = uiState.editableUsername,
                        editablePhysicalDevice = uiState.editablePhysicalDevice
                    )
                },
                drawerState = drawerState
            ) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    VisyncNavigationRail(
                        isDarkTheme = isDarkTheme,
                        setDarkTheme = setDarkTheme,
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                        },
                        scrollState = railScrollState,
                        openDrawer = {
                            mainAppNavigationScope.openDrawer()
                        },
                        alwaysShowDestinationLabels = false,
                        editablePhysicalDevice = uiState.editablePhysicalDevice
                    )
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        mainAppNavigationScope.appContent()
                    }
                }
            }
        }
        NavigationType.CUSTOM_PERMANENT_DRAWER -> {
            val collapsableDrawerState = remember {
                mutableStateOf(CollapsableDrawerState.EXPANDED)
            }
            CollapsableNavigationDrawer(
                drawerContent = {
                    CollapsableNavigationDrawerContent(
                        isDarkTheme = isDarkTheme,
                        setDarkTheme = setDarkTheme,
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                        },
                        scrollState = railAndDrawerScrollState,
                        drawerState = collapsableDrawerState,
                        editableUsername = uiState.editableUsername,
                        editablePhysicalDevice = uiState.editablePhysicalDevice
                    )
                }
            ) {
                mainAppNavigationScope.appContent()
            }
        }
    }
}

@Composable
fun SynchronizeScrolls(
    scrollState1: ScrollState,
    scrollState2: ScrollState,
) {
    var lastScrollIsScroll1 by remember { mutableStateOf(true) }
    LaunchedEffect(scrollState1.value, scrollState2.value) {
        if (scrollState1.isScrollInProgress) {
            lastScrollIsScroll1 = true
        } else if (scrollState2.isScrollInProgress) {
            lastScrollIsScroll1 = false
        }
        if (lastScrollIsScroll1) {
            if (!scrollState2.isScrollInProgress) {
                scrollState2.scrollTo(scrollState1.value)
            }
        } else {
            scrollState1.scrollTo(scrollState2.value)
        }
    }
    val anyScrollInProgress = (
        scrollState1.isScrollInProgress ||
        scrollState2.isScrollInProgress
    )
    if (scrollState1.value != scrollState2.value && !anyScrollInProgress) {
        LaunchedEffect(Unit) {
            if (lastScrollIsScroll1) {
                scrollState2.scrollTo(scrollState1.value)
            } else {
                scrollState1.scrollTo(scrollState2.value)
            }
        }
    }
}

class MainAppNavigationScope(
    val openDrawer: () -> Unit,
    val closeDrawer: () -> Unit,
)

enum class NavigationType {
    BOTTOM_NAVBAR_AND_DRAWER, RAIL_AND_DRAWER, CUSTOM_PERMANENT_DRAWER
}

data class RailAndDrawerScrollState(
    val railScrollState: ScrollState,
    val drawerScrollState: ScrollState,
)