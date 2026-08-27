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

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.Apps
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
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.android.settings.R

enum class ItemKind {
    NAVIGATION,
    TOGGLE,
    SEARCH_BAR,
}

data class HomepageItem(
    val key: String,
    val title: String,
    val summary: String? = null,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
    val monoIcon: ImageVector? = null,
    val tintIcon: Boolean = true,
    val iconColor: Color? = null,
    val iconShape: IconShape? = null,
    val iconPosition: IconPosition? = null,
    val defaultSize: TileSize = TileSize.ROW,
    val kind: ItemKind = ItemKind.NAVIGATION,
    val navKey: String = key,
)

fun HomepageLayout.resolve(item: HomepageItem): HomepageItem {
    val themed = item.key in themedIcons && item.monoIcon != null
    val color = iconColors[item.key]
    val shape = iconShapes[item.key]
    val position = iconPositions[item.key]
    if (!themed && color == null && shape == null && position == null &&
        !hasLabelOverride(item.key)
    ) {
        return item
    }
    return item.copy(
        title = titleOf(item.key, item.title),
        summary = summaryOf(item.key, item.summary),
        icon = if (themed) item.monoIcon else item.icon,
        iconRes = if (themed) null else item.iconRes,
        tintIcon = if (themed) true else item.tintIcon,
        iconColor = color?.let { Color(it) },
        iconShape = shape,
        iconPosition = position,
    )
}

class HomepageCatalog(
    val items: List<HomepageItem>,
    val defaultOrder: List<String>,
    val defaultHidden: Set<String> = emptySet(),
) {
    private val index: Map<String, HomepageItem> = items.associateBy { it.key }
    fun find(key: String): HomepageItem? = index[key]
    fun isKnown(key: String): Boolean = isSeparatorKey(key) || index.containsKey(key)
}

private class CatalogBuilder {
    private val items = mutableListOf<HomepageItem>()
    private val order = mutableListOf<String>()
    private val hidden = mutableSetOf<String>()
    private var separators = 0

    fun add(item: HomepageItem, hiddenByDefault: Boolean = false) {
        items += item
        order += item.key
        if (hiddenByDefault) hidden += item.key
    }

    fun addIf(available: Boolean, item: HomepageItem) {
        if (available) add(item)
    }

    fun cardBreak() {
        order += "$SEPARATOR_PREFIX${separators++}"
    }

    fun build() = HomepageCatalog(items.toList(), order.toList(), hidden.toSet())
}

@Composable
fun rememberHomepageCatalog(
    isCommunalAvailable: Boolean,
    isSafetyCenterAvailable: Boolean,
    isEmergencyAvailable: Boolean,
    isSupportAvailable: Boolean,
    isWellbeingAvailable: Boolean,
    isGoogleAvailable: Boolean,
    wifiSummary: String?,
    airplaneSummary: String? = null,
): HomepageCatalog {
    val builder = CatalogBuilder()

    builder.addIf(
        isGoogleAvailable,
        HomepageItem(
            key = "top_level_google",
            title = "Google",
            summary = "Google services and preferences",
            iconRes = R.drawable.ic_google,
            monoIcon = Icons.Outlined.AccountCircle,
            tintIcon = false,
        ),
    )
    if (isGoogleAvailable) builder.cardBreak()

    builder.add(
        HomepageItem(
            key = "top_level_connected_devices",
            title = "Connectivity",
            summary = "Bluetooth, pairing",
            icon = Icons.Outlined.Devices,
            defaultSize = TileSize.SMALL,
        )
    )
    builder.add(
        HomepageItem(
            key = "top_level_network",
            title = "Network",
            summary = "Network & connectivity",
            icon = Icons.Outlined.Wifi,
            defaultSize = TileSize.SMALL,
        )
    )
    builder.add(
        HomepageItem(
            key = KEY_WIFI_TOGGLE,
            title = "Wi-Fi",
            summary = wifiSummary,
            icon = Icons.Outlined.Wifi,
            kind = ItemKind.TOGGLE,
            navKey = "top_level_network",
        )
    )
    builder.add(
        HomepageItem(
            key = KEY_AIRPLANE_TOGGLE,
            title = "Airplane mode",
            summary = airplaneSummary,
            icon = Icons.Outlined.AirplanemodeActive,
            kind = ItemKind.TOGGLE,
            navKey = "top_level_network",
        ),
        hiddenByDefault = true,
    )
    builder.cardBreak()

    builder.add(
        HomepageItem(
            key = "top_level_lunaris",
            title = "Singularity",
            summary = stringResource(R.string.lunaris_dashboard_summary),
            iconRes = R.drawable.ic_settings_lunaris_filled,
        )
    )
    builder.add(
        HomepageItem(
            key = "top_level_wallpaper",
            title = "Wallpaper & styles",
            summary = stringResource(R.string.wallpaper_dashboard_summary),
            icon = Icons.Outlined.Wallpaper,
        )
    )
    builder.cardBreak()

    builder.addIf(
        isCommunalAvailable,
        HomepageItem(
            key = "top_level_communal",
            title = stringResource(R.string.communal_settings_title),
            summary = stringResource(R.string.communal_settings_summary),
            icon = Icons.Outlined.Home,
        ),
    )
    builder.add(
        HomepageItem(
            key = "top_level_display",
            title = stringResource(R.string.display_settings),
            summary = stringResource(R.string.display_dashboard_summary),
            icon = Icons.Outlined.LightMode,
        )
    )
    builder.add(
        HomepageItem(
            key = "top_level_sound",
            title = stringResource(R.string.sound_settings),
            summary = stringResource(R.string.sound_dashboard_summary),
            icon = Icons.Outlined.VolumeUp,
        )
    )
    builder.add(
        HomepageItem(
            key = "top_level_notifications",
            title = stringResource(R.string.configure_notification_settings),
            summary = stringResource(R.string.notification_dashboard_summary),
            icon = Icons.Outlined.Notifications,
        )
    )
    builder.add(
        HomepageItem(
            key = "top_level_priority_modes",
            title = stringResource(R.string.zen_modes_list_title),
            summary = "Do Not Disturb, custom modes",
            icon = Icons.Outlined.DoNotDisturb,
        )
    )
    builder.cardBreak()

    builder.add(
        HomepageItem(
            key = "top_level_apps",
            title = stringResource(R.string.apps_dashboard_title),
            summary = stringResource(R.string.app_and_notification_dashboard_summary),
            icon = Icons.Outlined.Apps,
        )
    )
    builder.add(
        HomepageItem(
            key = "top_level_battery",
            title = stringResource(R.string.power_usage_summary_title),
            summary = "Battery usage, Battery Saver",
            icon = Icons.Outlined.BatteryStd,
        )
    )
    builder.add(
        HomepageItem(
            key = "top_level_storage",
            title = stringResource(R.string.storage_settings),
            summary = "Used and free space",
            icon = Icons.Outlined.Storage,
        )
    )
    builder.cardBreak()

    builder.add(
        HomepageItem(
            key = "top_level_accessibility",
            title = stringResource(R.string.accessibility_settings),
            summary = stringResource(R.string.accessibility_settings_summary),
            icon = Icons.Outlined.Accessibility,
        )
    )
    if (isSafetyCenterAvailable) {
        builder.add(
            HomepageItem(
                key = "top_level_safety_center",
                title = stringResource(R.string.safety_center_title),
                summary = stringResource(R.string.safety_center_summary),
                icon = Icons.Outlined.GppGood,
            )
        )
    } else {
        builder.add(
            HomepageItem(
                key = "top_level_security",
                title = stringResource(R.string.security_settings_title),
                summary = stringResource(R.string.security_dashboard_summary),
                icon = Icons.Outlined.Lock,
            )
        )
        builder.add(
            HomepageItem(
                key = "top_level_privacy",
                title = stringResource(R.string.privacy_dashboard_title),
                summary = stringResource(R.string.privacy_dashboard_summary),
                icon = Icons.Outlined.PrivacyTip,
            )
        )
    }
    builder.add(
        HomepageItem(
            key = "top_level_location",
            title = stringResource(R.string.location_settings_title),
            summary = "App access to location",
            icon = Icons.Outlined.LocationOn,
        )
    )
    builder.addIf(
        isEmergencyAvailable,
        HomepageItem(
            key = "top_level_emergency",
            title = stringResource(R.string.emergency_settings_preference_title),
            summary = stringResource(R.string.emergency_dashboard_summary),
            icon = Icons.Outlined.Emergency,
        ),
    )
    builder.add(
        HomepageItem(
            key = "top_level_accounts",
            title = stringResource(R.string.account_dashboard_title_with_passkeys),
            summary = "Accounts, passkeys, sync",
            icon = Icons.Outlined.AccountCircle,
        )
    )
    builder.addIf(
        isWellbeingAvailable,
        HomepageItem(
            key = "top_level_wellbeing",
            title = stringResource(R.string.wellbeing_title),
            summary = "Screen time, app timers",
            icon = Icons.Outlined.SelfImprovement,
        ),
    )
    builder.cardBreak()

    builder.add(
        HomepageItem(
            key = "top_level_system",
            title = stringResource(R.string.header_category_system),
            summary = stringResource(R.string.system_dashboard_summary),
            icon = Icons.Outlined.Settings,
        )
    )
    builder.addIf(
        isSupportAvailable,
        HomepageItem(
            key = "top_level_support",
            title = stringResource(R.string.page_tab_title_support),
            summary = stringResource(R.string.support_summary),
            icon = Icons.Outlined.HelpOutline,
        ),
    )
    builder.add(
        HomepageItem(
            key = "top_level_about_device",
            title = stringResource(R.string.about_settings),
            summary = "Model, Android version, legal info",
            icon = Icons.Outlined.Info,
        )
    )
    builder.cardBreak()
    builder.add(
        HomepageItem(
            key = SEARCH_BAR_KEY,
            title = "Search",
            icon = Icons.Outlined.Search,
            kind = ItemKind.SEARCH_BAR,
        )
    )

    return builder.build()
}

const val KEY_WIFI_TOGGLE = "top_level_wifi_toggle"
const val KEY_AIRPLANE_TOGGLE = "top_level_airplane_toggle"
