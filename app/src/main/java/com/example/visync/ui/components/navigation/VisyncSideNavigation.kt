package com.example.visync.ui.components.navigation

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.example.visync.R

@Composable
fun ModalNavigationDrawerContent(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    showMainDestinations: Boolean,
    closeDrawer: () -> Unit = {},
) {
    ModalDrawerSheet {
        DrawerSheetContent(
            selectedDestination = selectedDestination,
            navigateToDestination = navigateToDestination,
            scrollState = scrollState,
            showCloseDrawerButton = true,
            showMainDestinations = showMainDestinations,
            closeDrawer = closeDrawer,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PermanentNavigationDrawerContent(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    collapseIntoRail: () -> Unit,
    drawerSheetModifier: Modifier = Modifier,
    drawerSheetContentModifier: Modifier = Modifier,
) {

    PermanentDrawerSheet(
        modifier = drawerSheetModifier
    ) {
        DrawerSheetContent(
            selectedDestination = selectedDestination,
            navigateToDestination = navigateToDestination,
            scrollState = scrollState,
            showCloseDrawerButton = true,
            showMainDestinations = true,
            closeDrawer = collapseIntoRail,
            modifier = drawerSheetContentModifier
        )
    }

}

@Composable
private fun DrawerSheetContent(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    showCloseDrawerButton: Boolean,
    showMainDestinations: Boolean,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.app_name).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (showCloseDrawerButton) {
                IconButton(onClick = closeDrawer) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(id = R.string.desc_navigation_drawer)
                    )
                }
            }
        }

        if (showMainDestinations) {
            MAIN_DESTINATIONS.forEach { mainDestination ->
                VisyncNavigationDrawerItem(
                    destination = mainDestination,
                    isSelected = selectedDestination == mainDestination.routeString,
                    navigateToDestination = navigateToDestination,
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = {},
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(id = R.string.navigation_drawer_fab),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(id = R.string.navigation_drawer_fab_text),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }

        ACCOUNT_RELATED_DESTINATIONS.forEach { accountDestination ->
            VisyncNavigationDrawerItem(
                destination = accountDestination,
                isSelected = selectedDestination == accountDestination.routeString,
                navigateToDestination = navigateToDestination,
            )
        }
    }
}

@Composable
private fun VisyncNavigationDrawerItem(
    destination: Route,
    isSelected: Boolean,
    navigateToDestination: (Route) -> Unit,
) {
    NavigationDrawerItem(
        modifier = Modifier.fillMaxWidth(),
        selected = isSelected,
        label = {
            Text(
                text = stringResource(id = destination.actionDescriptionId),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        icon = {
            Icon(
                imageVector = destination.getImageVectorIcon(),
                contentDescription = stringResource(
                    id = destination.actionDescriptionId
                )
            )
        },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        ),
        onClick = { navigateToDestination(destination) }
    )
}

@Composable
fun VisyncNavigationRail(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    openDrawer: () -> Unit,
    alwaysShowDestinationLabels: Boolean,
    modifier: Modifier = Modifier,
    railWidth: Dp = 80.dp,
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier
                .verticalScroll(scrollState)
        ) {
            NavigationRailItem(
                selected = false,
                onClick = openDrawer,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(id = R.string.desc_navigation_drawer)
                    )
                },
                // 10.dp for pixel-perfect alignment with drawers
                modifier = Modifier.padding(vertical = 10.dp),
            )
            MAIN_DESTINATIONS.forEach { accountDestination ->
                VisyncNavigationRailItem(
                    destination = accountDestination,
                    isSelected = selectedDestination == accountDestination.routeString,
                    navigateToDestination = navigateToDestination,
                    showDestinationLabels = alwaysShowDestinationLabels
                )
            }
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .align(CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(id = R.string.navigation_drawer_fab),
                )
            }
            ACCOUNT_RELATED_DESTINATIONS.forEach { accountDestination ->
                VisyncNavigationRailItem(
                    destination = accountDestination,
                    isSelected = selectedDestination == accountDestination.routeString,
                    navigateToDestination = navigateToDestination,
                    showDestinationLabels = alwaysShowDestinationLabels
                )
            }
        }
    }
}

@Composable
private fun VisyncNavigationRailItem(
    destination: Route,
    isSelected: Boolean,
    navigateToDestination: (Route) -> Unit,
    showDestinationLabels: Boolean,
) {
    NavigationRailItem(
        selected = isSelected,
        label = {
            Text(
                text = stringResource(id = destination.actionDescriptionId),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        alwaysShowLabel = showDestinationLabels,
        icon = {
            Icon(
                imageVector = destination.getImageVectorIcon(),
                contentDescription = stringResource(
                    id = destination.actionDescriptionId
                )
            )
        },
        colors = NavigationRailItemDefaults.colors(

        ),
        onClick = { navigateToDestination(destination) }
    )
}


/**
 *  Component that allows smooth toggling between
 *  PermanentNavigationDrawer and NavigationRail
 *  through poorly written band-aid-animations.
 */
@Composable
fun CollapsableNavigationDrawer(
    selectedDestination: String,
    navigateToDestination: (Route) -> Unit,
    scrollState: ScrollState,
    drawerState: MutableState<PermanentDrawerVisibility>,
    permanentDrawerWidth: Dp = 256.dp,
    railWidth: Dp = 80.dp,
    content: @Composable () -> Unit,
) {

    val permanentDrawerTransitionState = remember { mutableStateOf(drawerState.value) }
    val railTransitionState = remember { mutableStateOf(drawerState.value) }
    val (drawerSheetModifier, railModifier) = getDrawerAndRailAnimationModifiers(
        collapsableDrawerState = drawerState,
        initialDrawerWidth = permanentDrawerWidth,
        initialRailWidth = railWidth,
        permanentDrawerTransitionState = permanentDrawerTransitionState,
        railTransitionState = railTransitionState
    )

    when (drawerState.value) {
        PermanentDrawerVisibility.EXPANDED -> {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentNavigationDrawerContent(
                        selectedDestination = selectedDestination,
                        navigateToDestination = navigateToDestination,
                        scrollState = scrollState,
                        collapseIntoRail = {
                            permanentDrawerTransitionState.value =
                                PermanentDrawerVisibility.COLLAPSED
                        },
                        drawerSheetContentModifier = Modifier
                            .width(permanentDrawerWidth),
                        drawerSheetModifier = drawerSheetModifier
                    )
                },
                content = content
            )
        }
        PermanentDrawerVisibility.COLLAPSED -> {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                VisyncNavigationRail(
                    selectedDestination = selectedDestination,
                    navigateToDestination = navigateToDestination,
                    scrollState = scrollState,
                    openDrawer = {
                        railTransitionState.value =
                            PermanentDrawerVisibility.EXPANDED
                    },
                    alwaysShowDestinationLabels = false,
                    modifier = railModifier
                )
                content()
            }
        }
    }
}

@Composable
private fun getDrawerAndRailAnimationModifiers(
    collapsableDrawerState: MutableState<PermanentDrawerVisibility>,
    initialDrawerWidth: Dp,
    initialRailWidth: Dp,
    permanentDrawerTransitionState: MutableState<PermanentDrawerVisibility>,
    railTransitionState: MutableState<PermanentDrawerVisibility>
): Pair<Modifier, Modifier> {

    val permanentDrawerTransition = updateTransition(
        targetState = permanentDrawerTransitionState.value,
        label = "PermanentDrawer visibility transition"
    )
    val railTransition = updateTransition(
        targetState = railTransitionState.value,
        label = "NavigationRail visibility transition"
    )

    LaunchedEffect(
        permanentDrawerTransition.currentState
    ) {
        if (permanentDrawerTransition.currentState != railTransitionState.value) {
            collapsableDrawerState.value = permanentDrawerTransition.currentState
            railTransitionState.value = permanentDrawerTransition.currentState
        }
    }
    LaunchedEffect(
        railTransition.currentState
    ) {
        if (railTransition.currentState != permanentDrawerTransitionState.value) {
            collapsableDrawerState.value = railTransition.currentState
            permanentDrawerTransitionState.value = railTransition.currentState
        }
    }

    val drawerSheetWidth by permanentDrawerTransition.animateDp(
        label = "Drawer sheet width",
    ) { state ->
        when (state) {
            PermanentDrawerVisibility.COLLAPSED -> 0.dp
            PermanentDrawerVisibility.EXPANDED -> initialDrawerWidth
        }
    }

    val railWidth by railTransition.animateDp(
        label = "Rail width",
    ) { state ->
        when (state) {
            PermanentDrawerVisibility.COLLAPSED -> initialRailWidth
            PermanentDrawerVisibility.EXPANDED -> 0.dp
        }
    }

    val permanentDrawerIsInAnimation = !(
        permanentDrawerTransition.targetState == PermanentDrawerVisibility.EXPANDED &&
        permanentDrawerTransition.currentState == PermanentDrawerVisibility.EXPANDED &&
        collapsableDrawerState.value == PermanentDrawerVisibility.EXPANDED
    )
    val drawerSheetModifier = if (permanentDrawerIsInAnimation) {
        Modifier.overflowHiddenForDrawer(
            clipWidth = drawerSheetWidth,
            minimumActualWidth = initialRailWidth
        )
    } else {
        Modifier
    }

    val railIsInAnimation = !(
        railTransition.targetState == PermanentDrawerVisibility.COLLAPSED &&
        railTransition.currentState == PermanentDrawerVisibility.COLLAPSED &&
        collapsableDrawerState.value == PermanentDrawerVisibility.COLLAPSED
    )
    val railModifier = if (railIsInAnimation) {
        Modifier.overflowHiddenForRail(
            clipWidth = railWidth
        )
    } else {
        Modifier
    }

    return Pair(drawerSheetModifier, railModifier)
}

/**
 *  Returns modifier that emulates ***overflow: hidden*** rule from CSS.
 *  HorizontalScroll is used to achieve this, use this with caution.
 *  @param clipWidth width available for a content. All content
 *  wider than that will be cropped.
 *  @param minimumActualWidth width that should remain there, even if not visible.
 */
private fun Modifier.overflowHiddenForDrawer(
    clipWidth: Dp,
    minimumActualWidth: Dp
): Modifier = this.composed {
    val pxClipValue = with(LocalDensity.current) { clipWidth.toPx() }
    this.width(max(clipWidth, minimumActualWidth))
        .horizontalScroll(ScrollState(0))
        .drawWithContent {
            clipRect(right = pxClipValue) {
                this@drawWithContent.drawContent()
            }
        }
}

private fun Modifier.overflowHiddenForRail(
    clipWidth: Dp,
): Modifier = this.composed {
    val  pxClipValue = with(LocalDensity.current) { clipWidth.toPx() }
    drawWithContent {
        clipRect(right = pxClipValue) {
            this@drawWithContent.drawContent()
        }
    }
}

enum class PermanentDrawerVisibility {
    EXPANDED, COLLAPSED
}