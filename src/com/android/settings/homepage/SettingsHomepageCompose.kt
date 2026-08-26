/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.homepage

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.axion.compose.theme.AxionTheme
import com.android.settings.R
import java.util.function.Consumer

private val SEARCH_BAR_HEIGHT = 56.dp
private val SEARCH_BAR_HORIZONTAL_PADDING = 20.dp
private val SEARCH_BAR_BOTTOM_PADDING = 20.dp
private val SEARCH_BAR_SHADOW_ELEVATION = 12.dp
private val SEARCH_BAR_TONAL_ELEVATION = 6.dp
private val GRID_TILE_SPACING = 12.dp
private val CORNER_RADIUS = 24.dp
private val SEARCH_BAR_RADIUS = 28.dp
private val GROUP_DIVIDER_START_INSET = 72.dp

private const val ICON_BACKGROUND_ALPHA = 0.5f

private val SEARCH_HINTS = listOf(
    "Are you searching something?",
    "Click here to search",
    "Lost? Tap to search",
    "What are we looking for today?",
    "Psst… search lives down here",
)

private data class SettingsEntry(
    val icon: ImageVector?,
    val title: String,
    val key: String,
    val summary: String? = null,
    @DrawableRes val iconRes: Int? = null,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val AvatarShape
    @Composable get() = MaterialShapes.Cookie4Sided.toShape()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsHomepageScreen(
    onSearchClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onPreferenceClick: (String) -> Unit,
    userName: String? = null,
    avatarBitmap: Bitmap? = null,
    isCommunalAvailable: Boolean = false,
    isSafetyCenterAvailable: Boolean = false,
    isEmergencyAvailable: Boolean = true,
    isSupportAvailable: Boolean = false,
    isWellbeingAvailable: Boolean = false,
    isGoogleAvailable: Boolean = false,
    isWifiEnabled: Boolean = true,
    wifiSummary: String? = null,
    onWifiToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val networkTileEntry = SettingsEntry(Icons.Outlined.Wifi, "Network", key = "top_level_network")
    val connectedDevicesTileEntry = SettingsEntry(Icons.Outlined.Devices, "Connectivity", key = "top_level_connected_devices")
    val customizeItems = listOf(
        SettingsEntry(
            icon = null,
            iconRes = R.drawable.ic_settings_lunaris_filled,
            title = "Singularity",
            key = "top_level_lunaris",
            summary = stringResource(R.string.lunaris_dashboard_summary),
        ),
        SettingsEntry(
            icon = Icons.Outlined.Wallpaper,
            title = "Wallpaper & styles",
            key = "top_level_wallpaper",
            summary = stringResource(R.string.wallpaper_dashboard_summary),
        ),
    )
    val displayItems = buildList {
        if (isCommunalAvailable) {
            add(
                SettingsEntry(
                    icon = Icons.Outlined.Home,
                    title = stringResource(R.string.communal_settings_title),
                    key = "top_level_communal",
                    summary = stringResource(R.string.communal_settings_summary),
                )
            )
        }
        add(
            SettingsEntry(
                icon = Icons.Outlined.LightMode,
                title = stringResource(R.string.display_settings),
                key = "top_level_display",
                summary = stringResource(R.string.display_dashboard_summary),
            )
        )
        add(
            SettingsEntry(
                icon = Icons.Outlined.VolumeUp,
                title = stringResource(R.string.sound_settings),
                key = "top_level_sound",
                summary = stringResource(R.string.sound_dashboard_summary),
            )
        )
        add(
            SettingsEntry(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.configure_notification_settings),
                key = "top_level_notifications",
                summary = stringResource(R.string.notification_dashboard_summary),
            )
        )
        add(
            SettingsEntry(
                icon = Icons.Outlined.DoNotDisturb,
                title = stringResource(R.string.zen_modes_list_title),
                key = "top_level_priority_modes",
                summary = "Do Not Disturb, custom modes",
            )
        )
    }
    val appsItems = listOf(
        SettingsEntry(
            icon = Icons.Outlined.Apps,
            title = stringResource(R.string.apps_dashboard_title),
            key = "top_level_apps",
            summary = stringResource(R.string.app_and_notification_dashboard_summary),
        ),
        SettingsEntry(
            icon = Icons.Outlined.BatteryStd,
            title = stringResource(R.string.power_usage_summary_title),
            key = "top_level_battery",
            summary = "Battery usage, Battery Saver",
        ),
        SettingsEntry(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.storage_settings),
            key = "top_level_storage",
            summary = "Used and free space",
        ),
    )
    val securityItems = buildList {
        add(
            SettingsEntry(
                icon = Icons.Outlined.Accessibility,
                title = stringResource(R.string.accessibility_settings),
                key = "top_level_accessibility",
                summary = stringResource(R.string.accessibility_settings_summary),
            )
        )
        if (isSafetyCenterAvailable) {
            add(
                SettingsEntry(
                    icon = Icons.Outlined.GppGood,
                    title = stringResource(R.string.safety_center_title),
                    key = "top_level_safety_center",
                    summary = stringResource(R.string.safety_center_summary),
                )
            )
        } else {
            add(
                SettingsEntry(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.security_settings_title),
                    key = "top_level_security",
                    summary = stringResource(R.string.security_dashboard_summary),
                )
            )
            add(
                SettingsEntry(
                    icon = Icons.Outlined.PrivacyTip,
                    title = stringResource(R.string.privacy_dashboard_title),
                    key = "top_level_privacy",
                    summary = stringResource(R.string.privacy_dashboard_summary),
                )
            )
        }
        add(
            SettingsEntry(
                icon = Icons.Outlined.LocationOn,
                title = stringResource(R.string.location_settings_title),
                key = "top_level_location",
                summary = "App access to location",
            )
        )
        if (isEmergencyAvailable) {
            add(
                SettingsEntry(
                    icon = Icons.Outlined.Emergency,
                    title = stringResource(R.string.emergency_settings_preference_title),
                    key = "top_level_emergency",
                    summary = stringResource(R.string.emergency_dashboard_summary),
                )
            )
        }
        add(
            SettingsEntry(
                icon = Icons.Outlined.AccountCircle,
                title = stringResource(R.string.account_dashboard_title_with_passkeys),
                key = "top_level_accounts",
                summary = "Accounts, passkeys, sync",
            )
        )
        if (isWellbeingAvailable) {
            add(
                SettingsEntry(
                    icon = Icons.Outlined.SelfImprovement,
                    title = stringResource(R.string.wellbeing_title),
                    key = "top_level_wellbeing",
                    summary = "Screen time, app timers",
                )
            )
        }
    }
    val generalItems = buildList {
        add(
            SettingsEntry(
                icon = Icons.Outlined.Settings,
                title = stringResource(R.string.header_category_system),
                key = "top_level_system",
                summary = stringResource(R.string.system_dashboard_summary),
            )
        )
        if (isSupportAvailable) {
            add(
                SettingsEntry(
                    icon = Icons.Outlined.HelpOutline,
                    title = stringResource(R.string.page_tab_title_support),
                    key = "top_level_support",
                    summary = stringResource(R.string.support_summary),
                )
            )
        }
        add(
            SettingsEntry(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.about_settings),
                key = "top_level_about_device",
                summary = "Model, Android version, legal info",
            )
        )
    }

    val groups = listOf(
        customizeItems,
        displayItems,
        appsItems,
        securityItems,
        generalItems,
    )

    val listState = rememberLazyListState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    AxionTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarPadding,
                        bottom = SEARCH_BAR_HEIGHT + SEARCH_BAR_BOTTOM_PADDING * 2 + 24.dp,
                    ),
                ) {
                    item {
                        HomepageHeader(
                            avatarBitmap = avatarBitmap,
                            onAvatarClick = onAvatarClick,
                        )
                    }

                item { Spacer(Modifier.height(24.dp)) }

                    if (isGoogleAvailable) {
                        item {
                            GoogleAccountCard(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                onClick = { onPreferenceClick("top_level_google") },
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }

                item {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(GRID_TILE_SPACING),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(GRID_TILE_SPACING),
                            ) {
                                NavigationQuickTile(
                                    modifier = Modifier.weight(1f),
                                    icon = connectedDevicesTileEntry.icon,
                                    title = connectedDevicesTileEntry.title,
                                    subtitle = "Bluetooth, pairing",
                                    onClick = { onPreferenceClick(connectedDevicesTileEntry.key) },
                                )
                                NavigationQuickTile(
                                    modifier = Modifier.weight(1f),
                                    icon = networkTileEntry.icon,
                                    title = networkTileEntry.title,
                                    subtitle = "Network & connectivity",
                                    onClick = { onPreferenceClick(networkTileEntry.key) },
                                )
                            }
                            ToggleLongTile(
                                icon = Icons.Outlined.Wifi,
                                title = "Wi-Fi",
                                subtitle = wifiSummary,
                                checked = isWifiEnabled,
                                onCheckedChange = onWifiToggle,
                                onClick = { onPreferenceClick(networkTileEntry.key) },
                            )
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }

                groups.forEachIndexed { groupIndex, entries ->
                        item {
                            HomepageGroupCard(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                entries = entries,
                                onEntryClick = onPreferenceClick,
                            )
                        }
                        if (groupIndex < groups.lastIndex) {
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
                FloatingSearchBar(
                    onClick = onSearchClick,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomepageHeader(
    avatarBitmap: Bitmap?,
    onAvatarClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 74.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_label),
            style = MaterialTheme.typography.displayMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(AvatarShape)
                .background(MaterialTheme.colorScheme.surfaceBright)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GoogleAccountCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Google",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Google services and preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun rememberDrawableBitmap(@DrawableRes resId: Int, size: Dp, tint: Color): ImageBitmap? {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val tintArgb = tint.toArgb()
    return remember(context, resId, sizePx, tintArgb) {
        if (sizePx <= 0) return@remember null
        val drawable = context.getDrawable(resId)?.mutate() ?: return@remember null
        drawable.alpha = 255
        drawable.setTintList(ColorStateList.valueOf(tintArgb))
        drawable.setTintMode(PorterDuff.Mode.SRC_IN)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(Canvas(bitmap))
        bitmap.asImageBitmap()
    }
}

@Composable
private fun AccentIcon(
    icon: ImageVector?,
    @DrawableRes iconRes: Int? = null,
    size: Dp = 40.dp,
    glyphSize: Dp = 22.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = ICON_BACKGROUND_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        val tint = MaterialTheme.colorScheme.onSurface
        val iconBitmap =
            if (iconRes != null) rememberDrawableBitmap(iconRes, glyphSize, tint) else null
        if (iconBitmap != null) {
            Icon(
                bitmap = iconBitmap,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(glyphSize),
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToggleLongTile(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(icon = icon, size = 40.dp, glyphSize = 22.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Crossfade(targetState = checked, label = "wifi_switch_icon") { isChecked ->
                        Icon(
                            imageVector = if (isChecked) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/** Compact half-width navigation tile: accent icon, label, summary, chevron. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigationQuickTile(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIcon(icon = icon, size = 40.dp, glyphSize = 22.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun HomepageGroupCard(
    entries: List<SettingsEntry>,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            entries.forEachIndexed { index, entry ->
                HomepageEntryRow(
                    entry = entry,
                    onClick = { onEntryClick(entry.key) },
                )
                if (index < entries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = GROUP_DIVIDER_START_INSET, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

/** Preference row used inside every homepage group. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomepageEntryRow(
    entry: SettingsEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentIcon(icon = entry.icon, iconRes = entry.iconRes, size = 40.dp, glyphSize = 22.dp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (entry.summary != null) {
                Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun FloatingSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceContainer),
                )
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SEARCH_BAR_HORIZONTAL_PADDING, vertical = SEARCH_BAR_BOTTOM_PADDING)
                .height(SEARCH_BAR_HEIGHT)
                .clip(RoundedCornerShape(SEARCH_BAR_RADIUS))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(SEARCH_BAR_RADIUS),
            color = MaterialTheme.colorScheme.surfaceBright,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            shadowElevation = SEARCH_BAR_SHADOW_ELEVATION,
            tonalElevation = SEARCH_BAR_TONAL_ELEVATION,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = remember { SEARCH_HINTS.random() },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

object SettingsHomepageComposeInterop {
    private var userNameState: MutableState<String?>? = null
    private var avatarBitmapState: MutableState<Bitmap?>? = null
    private var wifiEnabledState: MutableState<Boolean>? = null
    private var wifiSummaryState: MutableState<String?>? = null

    @JvmStatic
    fun setContent(
        view: ComposeView,
        onSearchClick: Runnable,
        onAvatarClick: Runnable,
        onPreferenceClick: Consumer<String>,
        onWifiToggle: Consumer<Boolean>,
        userName: String?,
        avatarBitmap: Bitmap?,
        isWifiEnabled: Boolean,
        wifiSummary: String?,
        isCommunalAvailable: Boolean,
        isSafetyCenterAvailable: Boolean,
        isEmergencyAvailable: Boolean,
        isSupportAvailable: Boolean,
        isWellbeingAvailable: Boolean,
        isGoogleAvailable: Boolean,
    ) {
        view.setContent {
            val userNameMutableState = remember { mutableStateOf(userName) }
            val avatarBitmapMutableState = remember { mutableStateOf(avatarBitmap) }
            val wifiEnabledMutableState = remember { mutableStateOf(isWifiEnabled) }
            val wifiSummaryMutableState = remember { mutableStateOf(wifiSummary) }
            userNameState = userNameMutableState
            avatarBitmapState = avatarBitmapMutableState
            wifiEnabledState = wifiEnabledMutableState
            wifiSummaryState = wifiSummaryMutableState

            AxionTheme {
                SettingsHomepageScreen(
                    onSearchClick = { onSearchClick.run() },
                    onAvatarClick = { onAvatarClick.run() },
                    onPreferenceClick = { onPreferenceClick.accept(it) },
                    userName = userNameMutableState.value,
                    avatarBitmap = avatarBitmapMutableState.value,
                    isWifiEnabled = wifiEnabledMutableState.value,
                    wifiSummary = wifiSummaryMutableState.value,
                    onWifiToggle = { enabled ->
                        wifiEnabledMutableState.value = enabled
                        onWifiToggle.accept(enabled)
                    },
                    isCommunalAvailable = isCommunalAvailable,
                    isSafetyCenterAvailable = isSafetyCenterAvailable,
                    isEmergencyAvailable = isEmergencyAvailable,
                    isSupportAvailable = isSupportAvailable,
                    isWellbeingAvailable = isWellbeingAvailable,
                    isGoogleAvailable = isGoogleAvailable,
                )
            }
        }
    }

    @JvmStatic
    fun updateUserInfo(userName: String?, avatarBitmap: Bitmap?) {
        userNameState?.value = userName
        avatarBitmapState?.value = avatarBitmap
    }

    @JvmStatic
    fun updateWifiState(isWifiEnabled: Boolean, wifiSummary: String?) {
        wifiEnabledState?.value = isWifiEnabled
        wifiSummaryState?.value = wifiSummary
    }
}
