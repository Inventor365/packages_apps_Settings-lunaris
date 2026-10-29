/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SettingsScrollPreferences {
    private var current by mutableStateOf(SpringOverscrollPhysics.DEFAULT)
    private var initialized = false

    @JvmStatic
    fun get(context: Context): SpringOverscrollPhysics {
        if (!initialized) {
            initialized = true
            val resolver = context.applicationContext.contentResolver
            fun read() {
                fun int(key: String, default: Int, min: Int, max: Int) =
                    Settings.System.getInt(resolver, key, default).coerceIn(min, max)
                current = SpringOverscrollPhysics(
                    Settings.System.getInt(resolver, "settings_spring_scroll", 1) != 0,
                    int("settings_scroll_stiffness", 200, 50, 1000).toFloat(),
                    int("settings_scroll_damping", 50, 20, 100) / 100f,
                    int("settings_scroll_stretch", 15, 5, 30) / 100f,
                    int("settings_scroll_velocity", 25, 0, 100) / 100f,
                )
            }
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = read()
            }
            listOf(
                "settings_spring_scroll", "settings_scroll_stiffness", "settings_scroll_damping",
                "settings_scroll_stretch", "settings_scroll_velocity",
            ).forEach { resolver.registerContentObserver(Settings.System.getUriFor(it), false, observer) }
            read()
        }
        return current
    }
}
