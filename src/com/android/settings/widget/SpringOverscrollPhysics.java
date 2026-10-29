/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

public final class SpringOverscrollPhysics {
    public static final SpringOverscrollPhysics DEFAULT =
            new SpringOverscrollPhysics(true, 200f, 0.5f, 0.15f, 0.25f);

    public final boolean enabled;
    public final float stiffness;
    public final float dampingRatio;
    public final float maxStretchFraction;
    public final float velocityScale;

    public SpringOverscrollPhysics(boolean enabled, float stiffness, float dampingRatio,
            float maxStretchFraction, float velocityScale) {
        this.enabled = enabled;
        this.stiffness = stiffness;
        this.dampingRatio = dampingRatio;
        this.maxStretchFraction = maxStretchFraction;
        this.velocityScale = velocityScale;
    }

    public float offset(float distance, float extent) {
        float limit = extent * maxStretchFraction;
        if (limit <= 0f) return 0f;
        float resisted = Math.abs(distance) * 0.5f;
        return Math.copySign(limit * resisted / (limit + resisted), distance);
    }

    public float distance(float offset, float extent) {
        float limit = extent * maxStretchFraction;
        if (limit <= 0f) return 0f;
        float magnitude = Math.min(Math.abs(offset), limit * 0.99f);
        return Math.copySign(magnitude * limit / (0.5f * (limit - magnitude)), offset);
    }

    public float springVelocity(float velocity, float extent, float offset) {
        float limit = extent * maxStretchFraction;
        float maximum = (float) Math.sqrt(Math.max(0f, limit * limit - offset * offset)
                * stiffness);
        return Math.max(-maximum, Math.min(maximum, velocity * velocityScale));
    }
}
