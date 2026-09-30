package com.apps.unsealed.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.apps.unsealed.core.util.LetterlySpring
import com.apps.unsealed.core.util.reducedMotionSpring
import com.apps.unsealed.core.util.rememberIsReducedMotion
import com.apps.unsealed.navigation.Destinations
import com.apps.unsealed.ui.theme.UnsealedTheme
import kotlin.math.roundToInt

private val PillShape = RoundedCornerShape(32.dp)

/**
 * iOS-style floating bottom navigation bar — inset from the screen edges, rounded,
 * elevated above content, rather than docked flush to the bottom. Bespoke (not
 * Material3 NavigationBar) because NavigationBarItem bakes in ripple by default,
 * which /docs/motion-rules.md §4 forbids. Feedback is purely the sliding indicator
 * + icon scale below.
 */
@Composable
fun UnsealedBottomNavBar(
    currentRoute: String?,
    modifier: Modifier = Modifier,
    destinations: List<Destinations> = Destinations.entries,
    badgeCounts: Map<Destinations, Int> = emptyMap(),
    onTabSelected: (Destinations) -> Unit,
) {
    val isReducedMotion = rememberIsReducedMotion()
    // startsWith, not ==: stack-only variants of a tab route (e.g. the reply
    // Compose route "write/reply/{...}") register under a route *pattern*
    // that extends the tab's base route rather than matching it exactly —
    // same rationale as MainActivity's isMailboxThreadRoute.
    val selectedIndex = destinations.indexOfFirst { currentRoute?.startsWith(it.route) == true }.coerceAtLeast(0)

    var barWidthPx by remember { mutableIntStateOf(0) }
    val tabWidthPx = if (destinations.isNotEmpty()) barWidthPx / destinations.size else 0

    val indicatorFraction by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = reducedMotionSpring(LetterlySpring.Snappy, isReducedMotion),
        label = "indicatorFraction",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .onSizeChanged { barWidthPx = it.width }
            .shadow(elevation = 14.dp, shape = PillShape, clip = false)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surface)
            .height(64.dp),
    ) {
        val tabWidthDp = with(LocalDensity.current) { tabWidthPx.toDp() }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset((indicatorFraction * tabWidthPx).roundToInt(), 0) }
                .width(tabWidthDp)
                .padding(horizontal = 28.dp)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            destinations.forEach { destination ->
                BottomNavTab(
                    destination = destination,
                    isSelected = currentRoute?.startsWith(destination.route) == true,
                    isReducedMotion = isReducedMotion,
                    badgeCount = badgeCounts[destination] ?: 0,
                    onClick = { onTabSelected(destination) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun BottomNavTab(
    destination: Destinations,
    isSelected: Boolean,
    isReducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = reducedMotionSpring(LetterlySpring.Bouncy, isReducedMotion),
        label = "iconScale",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val label = stringResource(destination.labelRes)
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge { Text(if (badgeCount > 99) "99+" else badgeCount.toString()) }
                }
            },
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UnsealedBottomNavBarPreview() {
    UnsealedTheme {
        UnsealedBottomNavBar(currentRoute = Destinations.Write.route, onTabSelected = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UnsealedBottomNavBarPreviewDark() {
    UnsealedTheme {
        UnsealedBottomNavBar(currentRoute = Destinations.Stamps.route, onTabSelected = {})
    }
}
