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

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "HomepageBackdrop"

private const val BACKDROP_MAX_PX = 256

private val BACKDROP_BLUR = 10.dp

private const val BACKDROP_DIM_ALPHA = 0.16f
private const val BACKDROP_VEIL_ALPHA = 0.52f

@Composable
internal fun rememberWallpaperBackdrop(enabled: Boolean): ImageBitmap? {
    val context = LocalContext.current
    val backdrop by produceState<ImageBitmap?>(null, context, enabled) {
        value = if (!enabled) null else withContext(Dispatchers.IO) { loadBackdrop(context) }
    }
    return backdrop
}

@Composable
internal fun BoxScope.CardBackdrop(backdrop: ImageBitmap) {
    Image(
        bitmap = backdrop,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .matchParentSize()
            .blur(BACKDROP_BLUR, BlurredEdgeTreatment.Unbounded),
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = BACKDROP_DIM_ALPHA))
            .background(MaterialTheme.colorScheme.surfaceBright.copy(alpha = BACKDROP_VEIL_ALPHA))
    )
}

@Composable
internal fun cardSurfaceColor(backdrop: ImageBitmap?): Color =
    if (backdrop == null) MaterialTheme.colorScheme.surfaceBright else Color.Transparent

private fun loadBackdrop(context: Context): ImageBitmap? = try {
    val manager = WallpaperManager.getInstance(context)
    val source = manager.drawable ?: manager.builtInDrawable
    source?.downscaled()?.asImageBitmap()
} catch (e: Exception) {
    Log.w(TAG, "No wallpaper available for card backdrops", e)
    null
}

private fun Drawable.downscaled(): Bitmap? {
    val width = intrinsicWidth
    val height = intrinsicHeight
    if (width <= 0 || height <= 0) return null
    val scale = (BACKDROP_MAX_PX.toFloat() / maxOf(width, height)).coerceAtMost(1f)
    val outWidth = (width * scale).toInt().coerceAtLeast(1)
    val outHeight = (height * scale).toInt().coerceAtLeast(1)

    val target = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(target)
    val hardware = (this as? BitmapDrawable)?.bitmap
        ?.takeIf { it.config == Bitmap.Config.HARDWARE }
        ?.copy(Bitmap.Config.ARGB_8888, false)
    if (hardware != null) {
        canvas.drawBitmap(hardware, null, Rect(0, 0, outWidth, outHeight), Paint(Paint.FILTER_BITMAP_FLAG))
        hardware.recycle()
    } else {
        setBounds(0, 0, outWidth, outHeight)
        draw(canvas)
    }
    return target
}
