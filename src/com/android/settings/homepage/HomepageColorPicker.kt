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

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val HUE_SEGMENTS = 12

private val WHEEL_SIZE = 216.dp
private val SWATCH_SIZE = 36.dp

private val PRESET_HUES = listOf(0f, 18f, 40f, 62f, 96f, 150f, 190f, 214f, 262f, 320f)
private const val PRESET_SATURATION = 0.68f
private const val PRESET_VALUE = 0.94f

private const val MIN_BRIGHTNESS = 0.25f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ColorPickerSheet(
    title: String,
    initial: Color?,
    themeColor: Color,
    onPick: (Color) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    followLabel: String = "theme accent",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val seed = remember { ((initial ?: themeColor).toHsv()) }
    var hue by remember { mutableStateOf(seed[0]) }
    var saturation by remember { mutableStateOf(seed[1]) }
    var brightness by remember { mutableStateOf(seed[2].coerceIn(MIN_BRIGHTNESS, 1f)) }
    var hexDraft by remember { mutableStateOf((initial ?: themeColor).toHex()) }

    val current = Color.hsv(hue, saturation, brightness)

    val apply: (Float, Float, Float) -> Unit = { h, s, v ->
        val value = v.coerceIn(MIN_BRIGHTNESS, 1f)
        hue = h
        saturation = s
        brightness = value
        val picked = Color.hsv(h, s, value)
        hexDraft = picked.toHex()
        onPick(picked)
    }

    HomepageModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (initial == null) {
                    "Following the $followLabel. Pick a colour to override it."
                } else {
                    "Overriding the $followLabel."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(current)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp),
                        ),
                )
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    value = hexDraft,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { it.isHexDigit() }.take(6).uppercase()
                        if (cleaned.length == 6) {
                            val hsv = Color((0xFF000000L or cleaned.toLong(16)).toInt()).toHsv()
                            apply(hsv[0], hsv[1], hsv[2])
                        }
                        hexDraft = cleaned
                    },
                    label = { Text("Hex") },
                    placeholder = { Text("RRGGBB") },
                    prefix = { Text("#") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ColorWheel(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onChange = { h, s -> apply(h, s, brightness) },
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Brightness",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                value = brightness,
                onValueChange = { apply(hue, saturation, it) },
                valueRange = MIN_BRIGHTNESS..1f,
            )

            Text(
                text = "Presets",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            PresetSwatches(
                current = current,
                onSelect = { presetHue -> apply(presetHue, PRESET_SATURATION, PRESET_VALUE) },
            )

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initial != null) {
                    OutlinedButton(onClick = onReset) {
                        Icon(
                            Icons.Outlined.RestartAlt,
                            contentDescription = null,
                            Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Use $followLabel")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onDismiss) {
                    Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hues = remember {
        List(HUE_SEGMENTS + 1) { Color.hsv(360f * it / HUE_SEGMENTS % 360f, 1f, 1f) }
    }
    val liveChange = rememberUpdatedState(onChange)

    Canvas(
        modifier = modifier
            .size(WHEEL_SIZE)
            .pointerInput(Unit) {
                val report: (Offset) -> Unit = { position ->
                    val extent = minOf(size.width, size.height)
                    if (extent > 0) {
                        val radius = extent / 2f
                        val dx = position.x - size.width / 2f
                        val dy = position.y - size.height / 2f
                        val sat = (hypot(dx, dy) / radius).coerceIn(0f, 1f)
                        val deg = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
                        liveChange.value(deg, sat)
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    report(down.position)
                    down.consume()
                    drag(down.id) { change ->
                        report(change.position)
                        change.consume()
                    }
                }
            },
    ) {
        val radius = size.minDimension / 2f
        val centre = Offset(size.width / 2f, size.height / 2f)
        drawCircle(brush = Brush.sweepGradient(hues, centre), radius = radius, center = centre)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = centre,
                radius = radius,
            ),
            radius = radius,
            center = centre,
        )
        if (brightness < 1f) {
            drawCircle(
                color = Color.Black.copy(alpha = 1f - brightness),
                radius = radius,
                center = centre,
            )
        }
        val radians = hue * PI.toFloat() / 180f
        val marker = Offset(
            centre.x + cos(radians) * radius * saturation,
            centre.y + sin(radians) * radius * saturation,
        )
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = marker,
            style = Stroke(width = 2.dp.toPx()),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = 10.dp.toPx(),
            center = marker,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetSwatches(current: Color, onSelect: (hue: Float) -> Unit) {
    val currentArgb = current.toArgb()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PRESET_HUES.forEach { presetHue ->
            val color = Color.hsv(presetHue, PRESET_SATURATION, PRESET_VALUE)
            val selected = color.toArgb() == currentArgb
            Box(
                modifier = Modifier
                    .size(SWATCH_SIZE)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(presetHue) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = color.onColor(),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private fun Color.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    return hsv
}

private fun Color.toHex(): String = "%06X".format(toArgb() and 0xFFFFFF)

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
