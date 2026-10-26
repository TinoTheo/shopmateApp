package com.shopmate.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.shopmate.R
import com.shopmate.ui.components.ShopMateMark
import com.shopmate.ui.theme.CheckoutNeon
import com.shopmate.ui.theme.ShopBlack
import com.shopmate.ui.theme.ShopWhite
import com.shopmate.ui.theme.TradeGreen
import com.shopmate.ui.util.DeviceLayout

/**
 * Full-height Trade Green navigation sidebar: brand mark on top, main destinations in the
 * middle, Settings pinned at the bottom. Selection is a Checkout Neon pill behind the icon.
 * The sidebar itself has no background — it sits on the app frame's Trade Green canvas.
 *
 * **Two width variants, chosen by [DeviceLayout] — never a third** (CLAUDE.md § Navigation):
 * - [DeviceLayout.COMPACT] — 64dp, icons only. On a 390dp phone 84dp is 22% of the width, and
 *   every screen would pay it. Labels appear on **long-press** as a [ShopBlack] tooltip anchored
 *   to the right of the pressed item; selection is carried entirely by the Neon pill.
 * - [DeviceLayout.EXPANDED] — 84dp, icon over label, labels always visible.
 *
 * The rail is the only part of the frame that changes with device size. It is still a sidebar,
 * still green, still full height — **there is no bottom navigation bar in ShopMate, on any
 * device size.**
 *
 * The brand mark and the selected pill are the two **frame** uses of Checkout Neon
 * (§ The Checkout Neon rule): permanent identity, not feedback. They are exempt from the
 * content-sheet rule that reserves Neon for irreversible commits.
 */
@Composable
fun AppSidebar(
    currentDestination: NavDestination?,
    onNavigate: (Screen) -> Unit,
    deviceLayout: DeviceLayout,
    modifier: Modifier = Modifier,
    showSettings: Boolean = true,
) {
    val compact = deviceLayout == DeviceLayout.COMPACT

    fun isSelected(screen: Screen): Boolean =
        currentDestination?.hierarchy?.any { it.route == screen.route } == true

    Column(
        modifier = modifier
            .width(sidebarWidth(deviceLayout))
            .padding(top = if (compact) 10.dp else 14.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Brand mark (handoff 1c): a Neon circle, deliberately not the pill shape, so it can't be
        // read as a selected destination. Not interactive — no ripple.
        Box(
            modifier = Modifier
                .size(if (compact) 40.dp else 46.dp)
                .clip(CircleShape)
                .background(CheckoutNeon),
            contentAlignment = Alignment.Center,
        ) {
            ShopMateMark(
                color = TradeGreen,
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(
                    width = if (compact) 19.dp else 22.dp,
                    height = if (compact) 26.dp else 30.dp,
                ),
            )
        }

        Spacer(Modifier.height(18.dp))

        // Scrollable so all destinations stay reachable on short (landscape) screens;
        // Settings stays pinned below.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            sidebarNavItems.forEach { item ->
                SidebarItem(
                    icon = item.icon,
                    label = stringResource(item.labelRes),
                    selected = isSelected(item.screen),
                    compact = compact,
                    onClick = { onNavigate(item.screen) },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (showSettings) {
            SidebarItem(
                icon = Icons.Outlined.Settings,
                label = stringResource(R.string.screen_settings),
                selected = isSelected(Screen.Settings),
                compact = compact,
                onClick = { onNavigate(Screen.Settings) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val pillColor by animateColorAsState(
        if (selected) CheckoutNeon else Color.Transparent,
        animationSpec = tween(NAV_SELECTION_MILLIS),
        label = "sidebarPill",
    )
    val iconTint by animateColorAsState(
        if (selected) ShopBlack else ShopWhite.copy(alpha = 0.72f),
        animationSpec = tween(NAV_SELECTION_MILLIS),
        label = "sidebarIcon",
    )

    val item = @Composable {
        val base = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
        Column(
            modifier = if (compact) {
                base.height(COMPACT_TOUCH_HEIGHT)
            } else {
                base.padding(vertical = 7.dp)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = if (compact) 46.dp else 48.dp, height = 30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(pillColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    // The label is the only name a COMPACT item has, so it must reach a11y here.
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            // COMPACT carries selection on the pill alone — the label lives in the tooltip.
            if (!compact) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) ShopWhite else ShopWhite.copy(alpha = 0.72f),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }

    if (compact) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip(containerColor = ShopBlack, contentColor = ShopWhite) {
                    Text(label)
                }
            },
            state = rememberTooltipState(isPersistent = false),
            content = item,
        )
    } else {
        item()
    }
}

/** 180ms on the pill fill only — the one nav animation. */
private const val NAV_SELECTION_MILLIS = 180

/** Full-width touch target on COMPACT; the 46x30dp pill is centred inside it. */
private val COMPACT_TOUCH_HEIGHT = 44.dp

/**
 * 64dp icons-only on phones, 84dp icon-over-label on tablets. **Never a third value** — the
 * two-bucket rule in § Adaptive layouts applies to the rail as much as to the screens.
 */
fun sidebarWidth(deviceLayout: DeviceLayout) =
    if (deviceLayout == DeviceLayout.COMPACT) 64.dp else 84.dp
