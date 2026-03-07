/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Dashboard
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.axion.compose.preferences.ClickablePreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.theme.AxionTheme
import com.android.settings.R
import java.util.function.Consumer

private val SEARCH_BAR_RADIUS = 28.dp
private const val TITLE_ITEM_INDEX = 1

private data class SettingsEntry(
    val icon: ImageVector,
    val title: String,
    val key: String,
)

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
    modifier: Modifier = Modifier,
) {
    val connectionItems = listOf(
        SettingsEntry(Icons.Outlined.Wifi, stringResource(R.string.network_dashboard_title), key = "top_level_network"),
        SettingsEntry(Icons.Outlined.Devices, stringResource(R.string.connected_devices_dashboard_title), key = "top_level_connected_devices"),
    )
    val customizeItems = listOf(
        SettingsEntry(Icons.Outlined.Dashboard, "Personalizations", key = "axion_hub"),
        SettingsEntry(Icons.Outlined.Wallpaper, "Wallpaper & styles", key = "axion_themepicker"),
    )
    val displayItems = buildList {
        if (isCommunalAvailable) add(SettingsEntry(Icons.Outlined.Home, stringResource(R.string.communal_settings_title), key = "top_level_communal"))
        add(SettingsEntry(Icons.Outlined.LightMode, stringResource(R.string.display_settings), key = "top_level_display"))
        add(SettingsEntry(Icons.Outlined.VolumeUp, stringResource(R.string.sound_settings), key = "top_level_sound"))
        add(SettingsEntry(Icons.Outlined.Notifications, stringResource(R.string.configure_notification_settings), key = "top_level_notifications"))
        add(SettingsEntry(Icons.Outlined.DoNotDisturb, stringResource(R.string.zen_modes_list_title), key = "top_level_priority_modes"))
    }
    val appsItems = listOf(
        SettingsEntry(Icons.Outlined.Apps, stringResource(R.string.apps_dashboard_title), key = "top_level_apps"),
        SettingsEntry(Icons.Outlined.BatteryStd, stringResource(R.string.power_usage_summary_title), key = "top_level_battery"),
        SettingsEntry(Icons.Outlined.Storage, stringResource(R.string.storage_settings), key = "top_level_storage"),
    )
    val securityItems = buildList {
        add(SettingsEntry(Icons.Outlined.Accessibility, stringResource(R.string.accessibility_settings), key = "top_level_accessibility"))
        if (isSafetyCenterAvailable) {
            add(SettingsEntry(Icons.Outlined.GppGood, stringResource(R.string.safety_center_title), key = "top_level_safety_center"))
        } else {
            add(SettingsEntry(Icons.Outlined.Lock, stringResource(R.string.security_settings_title), key = "top_level_security"))
            add(SettingsEntry(Icons.Outlined.PrivacyTip, stringResource(R.string.privacy_dashboard_title), key = "top_level_privacy"))
        }
        add(SettingsEntry(Icons.Outlined.LocationOn, stringResource(R.string.location_settings_title), key = "top_level_location"))
        if (isEmergencyAvailable) add(SettingsEntry(Icons.Outlined.Emergency, stringResource(R.string.emergency_settings_preference_title), key = "top_level_emergency"))
        add(SettingsEntry(Icons.Outlined.AccountCircle, stringResource(R.string.account_dashboard_title_with_passkeys), key = "top_level_accounts"))
        if (isWellbeingAvailable) add(SettingsEntry(Icons.Outlined.SelfImprovement, stringResource(R.string.wellbeing_title), key = "top_level_wellbeing"))
        if (isGoogleAvailable) add(SettingsEntry(Icons.Outlined.Cloud, stringResource(R.string.gms_enabled_title), key = "top_level_google"))
    }
    val generalItems = buildList {
        add(SettingsEntry(Icons.Outlined.Settings, stringResource(R.string.header_category_system), key = "top_level_system"))
        if (isSupportAvailable) add(SettingsEntry(Icons.Outlined.HelpOutline, stringResource(R.string.page_tab_title_support), key = "top_level_support"))
        add(SettingsEntry(Icons.Outlined.Info, stringResource(R.string.about_settings), key = "top_level_about_device"))
    }

    val groups = listOf(
        null to connectionItems,
        null to customizeItems,
        null to displayItems,
        null to appsItems,
        null to securityItems,
        null to generalItems,
    )

    val listState = rememberLazyListState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val isTitleScrolledAway by remember {
        derivedStateOf { listState.firstVisibleItemIndex > TITLE_ITEM_INDEX }
    }

    val searchTopPadding by animateDpAsState(
        targetValue = if (isTitleScrolledAway) statusBarPadding + 8.dp else 24.dp,
        animationSpec = tween(200),
        label = "searchTopPadding",
    )

    val searchBottomPadding by animateDpAsState(
        targetValue = if (isTitleScrolledAway) 12.dp else 0.dp,
        animationSpec = tween(200),
        label = "searchBottomPadding",
    )

    val density = LocalDensity.current
    val titleAlpha by remember {
        derivedStateOf {
            val info = listState.layoutInfo.visibleItemsInfo.find { it.index == TITLE_ITEM_INDEX }
                ?: return@derivedStateOf 0f
            val statusBarPx = with(density) { statusBarPadding.toPx() }
            val fadeStartOffset = statusBarPx + with(density) { 32.dp.toPx() }
            (info.offset.toFloat() / fadeStartOffset).coerceIn(0f, 1f)
        }
    }

    AxionTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusBarPadding,
                    bottom = 32.dp,
                ),
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 24.dp, top = 8.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
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
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.settings_label),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 24.dp, top = 56.dp)
                            .graphicsLayer { alpha = titleAlpha },
                    )
                }

                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = searchTopPadding, bottom = searchBottomPadding, start = 12.dp, end = 12.dp)
                                .height(56.dp)
                                .clickable(onClick = onSearchClick),
                            shape = RoundedCornerShape(SEARCH_BAR_RADIUS),
                            color = MaterialTheme.colorScheme.surfaceBright,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp),
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
                                    text = stringResource(R.string.search_settings),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }

                groups.forEachIndexed { groupIndex, (title, entries) ->
                    item {
                        PreferenceGroup(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            title = title,
                        ) {
                            entries.forEach { entry ->
                                item {
                                    ClickablePreference(
                                        title = entry.title,
                                        icon = entry.icon,
                                        onClick = { onPreferenceClick(entry.key) },
                                        enlargeTitle = true,
                                    )
                                }
                            }
                        }
                    }
                    if (groupIndex < groups.lastIndex) {
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

object SettingsHomepageComposeInterop {
    private var userNameState: MutableState<String?>? = null
    private var avatarBitmapState: MutableState<Bitmap?>? = null

    @JvmStatic
    fun setContent(
        view: ComposeView,
        onSearchClick: Runnable,
        onAvatarClick: Runnable,
        onPreferenceClick: Consumer<String>,
        userName: String?,
        avatarBitmap: Bitmap?,
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
            userNameState = userNameMutableState
            avatarBitmapState = avatarBitmapMutableState

            AxionTheme {
                SettingsHomepageScreen(
                    onSearchClick = { onSearchClick.run() },
                    onAvatarClick = { onAvatarClick.run() },
                    onPreferenceClick = { onPreferenceClick.accept(it) },
                    userName = userNameMutableState.value,
                    avatarBitmap = avatarBitmapMutableState.value,
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
}
