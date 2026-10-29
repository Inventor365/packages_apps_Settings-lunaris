/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManagerGlobal;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.AbstractComposeView;
import androidx.core.widget.NestedScrollView;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SettingsViewOverscroll {
    private final Map<View, ViewTreeObserver.OnGlobalLayoutListener> mRoots = new HashMap<>();

    private SettingsViewOverscroll() {}

    public static void install(Application application) {
        SettingsScrollPreferences.get(application);
        SettingsViewOverscroll installer = new SettingsViewOverscroll();
        WindowManagerGlobal.getInstance().addWindowViewsListener(
                application.getMainExecutor(), installer::onWindowsChanged);
    }

    private void onWindowsChanged(List<View> windows) {
        mRoots.entrySet().removeIf(entry -> {
            if (windows.contains(entry.getKey())) return false;
            entry.getKey().getViewTreeObserver().removeOnGlobalLayoutListener(entry.getValue());
            return true;
        });
        for (View root : windows) {
            if (mRoots.containsKey(root)) continue;
            ViewTreeObserver.OnGlobalLayoutListener listener = () -> installInTree(root);
            mRoots.put(root, listener);
            root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
            installInTree(root);
        }
    }

    static void installInTree(View view) {
        if (view instanceof AbstractComposeView) return;
        if (view.getOverScrollMode() != View.OVER_SCROLL_NEVER) {
            if (view instanceof RecyclerView) {
                RecyclerView list = (RecyclerView) view;
                if (!(list.getEdgeEffectFactory() instanceof SpringEdgeEffectFactory)) {
                    list.setEdgeEffectFactory(new SpringEdgeEffectFactory());
                }
            } else if (view instanceof NestedScrollView) {
                installNestedScrollEdges((NestedScrollView) view);
            } else if (view instanceof ScrollView) {
                ScrollView scroll = (ScrollView) view;
                if (!(scroll.mEdgeGlowTop instanceof SpringEdgeEffect)) {
                    SpringOffset offset = new SpringOffset(scroll, true);
                    scroll.mEdgeGlowTop = new SpringEdgeEffect(offset, 1f);
                    scroll.mEdgeGlowBottom = new SpringEdgeEffect(offset, -1f);
                }
            } else if (view instanceof HorizontalScrollView) {
                HorizontalScrollView scroll = (HorizontalScrollView) view;
                if (!(scroll.mEdgeGlowLeft instanceof SpringEdgeEffect)) {
                    SpringOffset offset = new SpringOffset(scroll, false);
                    scroll.mEdgeGlowLeft = new SpringEdgeEffect(offset, 1f);
                    scroll.mEdgeGlowRight = new SpringEdgeEffect(offset, -1f);
                }
            } else if (view instanceof AbsListView) {
                AbsListView list = (AbsListView) view;
                if (!(list.mEdgeGlowTop instanceof SpringEdgeEffect)) {
                    SpringOffset offset = new SpringOffset(list, true);
                    list.mEdgeGlowTop = new SpringEdgeEffect(offset, 1f);
                    list.mEdgeGlowBottom = new SpringEdgeEffect(offset, -1f);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) installInTree(group.getChildAt(i));
        }
    }

    @SuppressLint({"RestrictedApi", "VisibleForTests"})
    private static void installNestedScrollEdges(NestedScrollView scroll) {
        if (!(scroll.mEdgeGlowTop instanceof SpringEdgeEffect)) {
            SpringOffset offset = new SpringOffset(scroll, true);
            scroll.mEdgeGlowTop = new SpringEdgeEffect(offset, 1f);
            scroll.mEdgeGlowBottom = new SpringEdgeEffect(offset, -1f);
        }
    }

    static final class SpringEdgeEffectFactory extends RecyclerView.EdgeEffectFactory {
        @Nullable private SpringOffset mVertical;
        @Nullable private SpringOffset mHorizontal;

        @NonNull
        @Override
        protected EdgeEffect createEdgeEffect(@NonNull RecyclerView view, int direction) {
            boolean vertical = direction == DIRECTION_TOP || direction == DIRECTION_BOTTOM;
            if (vertical && mVertical == null) mVertical = new SpringOffset(view, true);
            if (!vertical && mHorizontal == null) mHorizontal = new SpringOffset(view, false);
            return new SpringEdgeEffect(Objects.requireNonNull(vertical ? mVertical : mHorizontal),
                    direction == DIRECTION_TOP || direction == DIRECTION_LEFT ? 1f : -1f);
        }
    }

    static final class SpringOffset implements View.OnAttachStateChangeListener {
        final View view;
        final boolean vertical;
        final SpringAnimation animation;
        SpringOverscrollPhysics physics;
        float activeSign;

        SpringOffset(View view, boolean vertical) {
            this.view = view;
            this.vertical = vertical;
            physics = SettingsScrollPreferences.get(view.getContext());
            animation = new SpringAnimation(view,
                    vertical ? DynamicAnimation.TRANSLATION_Y : DynamicAnimation.TRANSLATION_X);
            animation.setSpring(new SpringForce(0f));
            configure();
            view.addOnAttachStateChangeListener(this);
        }

        void configure() {
            Objects.requireNonNull(animation.getSpring()).setStiffness(physics.stiffness)
                    .setDampingRatio(physics.dampingRatio);
        }

        boolean enabled() {
            SpringOverscrollPhysics latest = SettingsScrollPreferences.get(view.getContext());
            if (physics != latest) {
                reset();
                physics = latest;
                configure();
            }
            if (!physics.enabled || !ValueAnimator.areAnimatorsEnabled()) {
                reset();
                return false;
            }
            return true;
        }

        float extent() { return vertical ? view.getHeight() : view.getWidth(); }
        float value() { return vertical ? view.getTranslationY() : view.getTranslationX(); }

        void setValue(float value) {
            if (vertical) view.setTranslationY(value);
            else view.setTranslationX(value);
        }

        void release(float velocity) {
            animation.setStartVelocity(physics.springVelocity(velocity, extent(), value()));
            animation.animateToFinalPosition(0f);
        }

        void reset() {
            animation.cancel();
            setValue(0f);
            activeSign = 0f;
        }

        @Override
        public void onViewAttachedToWindow(View view) {}

        @Override
        public void onViewDetachedFromWindow(View view) { reset(); }
    }

    static final class SpringEdgeEffect extends EdgeEffect {
        private final SpringOffset mOffset;
        private final float mSign;

        SpringEdgeEffect(SpringOffset offset, float sign) {
            super(offset.view.getContext());
            mOffset = offset;
            mSign = sign;
        }

        @Override
        public void onPull(float deltaDistance) { onPullDistance(deltaDistance, 0.5f); }

        @Override
        public void onPull(float deltaDistance, float displacement) {
            onPullDistance(deltaDistance, displacement);
        }

        @Override
        public float onPullDistance(float deltaDistance, float displacement) {
            if (!mOffset.enabled() || mOffset.extent() <= 0f) return 0f;
            if (deltaDistance == 0f && mOffset.activeSign == mSign) {
                mOffset.animation.cancel();
                return 0f;
            }
            float oldDistance = getDistance();
            if (oldDistance == 0f && deltaDistance <= 0f) return 0f;
            mOffset.animation.cancel();
            mOffset.activeSign = mSign;
            float newDistance = Math.max(0f, Math.min(4f, oldDistance + deltaDistance));
            mOffset.setValue(mSign * mOffset.physics.offset(
                    newDistance * mOffset.extent(), mOffset.extent()));
            return deltaDistance > 0f ? deltaDistance : newDistance - oldDistance;
        }

        @Override
        public float getDistance() {
            if (mOffset.activeSign != mSign || mOffset.extent() <= 0f) return 0f;
            return Math.max(0f, mSign * mOffset.physics.distance(
                    mOffset.value(), mOffset.extent()) / mOffset.extent());
        }

        @Override
        public void onRelease() {
            if (mOffset.enabled() && mOffset.activeSign == mSign
                    && !mOffset.animation.isRunning() && mOffset.value() != 0f) {
                if (mSign * mOffset.value() > 0.5f) {
                    ScrollEdgeHaptics.perform(mOffset.view.getContext(), 0f);
                }
                mOffset.release(0f);
            }
        }

        @Override
        public void onAbsorb(int velocity) {
            if (!mOffset.enabled()) return;
            boolean rebounding = mOffset.activeSign == mSign && mOffset.animation.isRunning();
            mOffset.animation.cancel();
            mOffset.activeSign = mSign;
            if (!rebounding && velocity > 1 && mOffset.extent() > 0f) {
                ScrollEdgeHaptics.perform(mOffset.view.getContext(), velocity);
            }
            mOffset.release(mSign * velocity);
        }

        @Override
        public boolean isFinished() {
            return mOffset.activeSign != mSign
                    || (!mOffset.animation.isRunning() && mOffset.value() == 0f);
        }

        @Override
        public void finish() {
            if (mOffset.activeSign == mSign) mOffset.reset();
        }

        @Override
        public boolean draw(Canvas canvas) { return false; }
    }
}
