package com.example.visync.ui.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.visync.ui.screens.main.MainAppNavigationUiState
import kotlinx.coroutines.launch

@Composable
fun MainAppNavigation(
    uiState: MainAppNavigationUiState,
    navigationType: NavigationType,
    navController: NavHostController,
    appContent: @Composable MainAppNavigationScope.() -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val railAndDrawerScrollState = rememberScrollState()

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
                drawerContent = {
                    ModalNavigationDrawerContent(
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                            mainAppNavigationScope.closeDrawer()
                        },
                        scrollState = railAndDrawerScrollState,
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
                    modifier = Modifier.fillMaxSize()
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
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                            mainAppNavigationScope.closeDrawer()
                        },
                        scrollState = railAndDrawerScrollState,
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
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                        },
                        scrollState = railAndDrawerScrollState,
                        openDrawer = {
                            mainAppNavigationScope.openDrawer()
                        },
                        alwaysShowDestinationLabels = false
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
                        selectedDestination = selectedDestination,
                        navigateToDestination = {
                            navigationActions.navigateTo(it.routeString)
                        },
                        scrollState = rememberScrollState(),
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

class MainAppNavigationScope(
    val openDrawer: () -> Unit,
    val closeDrawer: () -> Unit,
)

enum class NavigationType {
    BOTTOM_NAVBAR_AND_DRAWER, RAIL_AND_DRAWER, CUSTOM_PERMANENT_DRAWER
}