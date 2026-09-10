/*
 * Copyright (C) 2024-2026 The Lunaris Project
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

package com.android.settings.applications;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

public class GameSpaceController extends BasePreferenceController {

    private static final String PACKAGE_GAME_SPACE = "io.chaldeaprjkt.gamespace";
    private static final String CLASS_GAME_SPACE = "io.chaldeaprjkt.gamespace.settings.SettingsActivity";

    public GameSpaceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return isPackageInstalled(mContext, PACKAGE_GAME_SPACE)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setIntent(getGameSpaceIntent());
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }
        final Intent intent = getGameSpaceIntent();
        if (intent != null) {
            mContext.startActivity(intent);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    private Intent getGameSpaceIntent() {
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(PACKAGE_GAME_SPACE);
        if (intent == null) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(PACKAGE_GAME_SPACE, CLASS_GAME_SPACE));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
