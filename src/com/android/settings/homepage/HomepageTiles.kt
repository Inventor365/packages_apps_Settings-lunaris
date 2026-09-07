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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val GRID_TILE_SPACING = 12.dp
internal val SEARCH_BAR_HEIGHT = 56.dp
internal val SEARCH_BAR_RADIUS = 28.dp
internal val SEARCH_BAR_HORIZONTAL_PADDING = 20.dp
internal val SEARCH_BAR_BOTTOM_PADDING = 20.dp

private val SMALL_TILE_HEIGHT = 78.dp
private val MEDIUM_TILE_HEIGHT = 132.dp
private val LARGE_TILE_HEIGHT = 156.dp

private val GROUP_DIVIDER_START_INSET = 72.dp
private const val ICON_BACKGROUND_ALPHA = 0.5f
private val CHEVRON_CONTAINER_PADDING = 4.dp

private val ROW_GAP_THICKNESS = 2.dp
private val SPLIT_CARD_GAP = 3.dp

internal val SEARCH_HINTS = listOf(
    "Are you searching something?",
    "Click here to search",
    "Lost? Tap to search",
    "What are we looking for today?",
    "Psst… search lives down here",
)

data class TileStyle(
    val corner: Dp,
    val iconStyle: IconStyle,
    val showSummaries: Boolean,
    val dividerStyle: DividerStyle,
    val iconColor: Color? = null,
    val iconShape: IconShape = IconShape.CIRCLE,
    val iconPosition: IconPosition = IconPosition.START,
    val chevronStyle: ChevronStyle = ChevronStyle.PLAIN,
)

fun HomepageLayout.tileStyle(): TileStyle = TileStyle(
    corner = cornerStyle.radius,
    iconStyle = iconStyle,
    showSummaries = showSummaries,
    dividerStyle = dividerStyle,
    iconColor = iconColor?.let { Color(it) },
    iconShape = iconShape,
    iconPosition = iconPosition,
    chevronStyle = chevronStyle,
)

internal fun TileSize.fixedHeight(): Dp? = when (this) {
    TileSize.SMALL -> SMALL_TILE_HEIGHT
    TileSize.MEDIUM -> MEDIUM_TILE_HEIGHT
    TileSize.LARGE -> LARGE_TILE_HEIGHT
    TileSize.ROW -> null
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun IconShape.containerShape(): Shape = when (this) {
    IconShape.CIRCLE -> CircleShape
    IconShape.ARROW -> MaterialShapes.Arrow.toShape()
    IconShape.PILL -> MaterialShapes.Pill.toShape()
    IconShape.TRIANGLE -> MaterialShapes.Triangle.toShape()
    IconShape.GEM -> MaterialShapes.Gem.toShape()
    IconShape.COOKIE_4 -> MaterialShapes.Cookie4Sided.toShape()
    IconShape.COOKIE_6 -> MaterialShapes.Cookie6Sided.toShape()
    IconShape.CLOVER_4 -> MaterialShapes.Clover4Leaf.toShape()
}

@Composable
private fun rememberDrawableBitmap(
    @DrawableRes resId: Int,
    size: Dp,
    tint: Color,
): ImageBitmap? {
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

internal fun Color.onColor(): Color = if (luminance() > 0.5f) Color.Black else Color.White

@Composable
internal fun HomepageIcon(
    icon: ImageVector?,
    @DrawableRes iconRes: Int? = null,
    style: IconStyle = IconStyle.ACCENT,
    tinted: Boolean = true,
    containerSize: Dp = 40.dp,
    glyphSize: Dp = 22.dp,
    color: Color? = null,
    shape: Shape = CircleShape,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = color ?: scheme.primary
    val background = when {
        !tinted && style == IconStyle.FILLED -> color ?: scheme.surfaceContainerHighest
        style == IconStyle.ACCENT -> accent.copy(alpha = ICON_BACKGROUND_ALPHA)
        style == IconStyle.FILLED -> accent
        else -> Color.Transparent
    }
    val tint = when {
        !tinted -> Color.Unspecified
        style == IconStyle.FILLED -> if (color == null) scheme.onPrimary else color.onColor()
        style == IconStyle.PLAIN -> accent
        else -> scheme.onSurface
    }
    Box(
        modifier = Modifier
            .size(containerSize)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = if (iconRes != null && tinted) {
            rememberDrawableBitmap(iconRes, glyphSize, tint)
        } else {
            null
        }
        when {
            bitmap != null -> Icon(
                bitmap = bitmap,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(glyphSize),
            )

            iconRes != null -> Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(glyphSize + 8.dp),
            )

            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(glyphSize),
            )
        }
    }
}

@Composable
internal fun HomepageTile(
    item: HomepageItem,
    size: TileSize,
    style: TileStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickable: Boolean = true,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    searchHint: String = "",
    backdrop: ImageBitmap? = null,
) {
    val height = size.fixedHeight()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier)
            .clip(RoundedCornerShape(style.corner))
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(style.corner),
        color = cardSurfaceColor(backdrop),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box {
            if (backdrop != null) CardBackdrop(backdrop)
            TileBody(
                item = item,
                size = size,
                style = style,
                checked = checked,
                onCheckedChange = onCheckedChange,
                interactive = clickable,
                searchHint = searchHint,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TileBody(
    item: HomepageItem,
    size: TileSize,
    style: TileStyle,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    interactive: Boolean,
    searchHint: String,
) {
    if (item.kind == ItemKind.SEARCH_BAR) {
        SearchBarBody(size = size, hint = searchHint)
        return
    }
    val summary = item.summary?.takeIf { style.showSummaries }
    val isToggle = item.kind == ItemKind.TOGGLE
    val iconColor = item.iconColor ?: style.iconColor
    val iconShape = (item.iconShape ?: style.iconShape).containerShape()
    val iconAtEnd = (item.iconPosition ?: style.iconPosition) == IconPosition.END
    val chevron = style.chevronStyle

    val iconSlot: @Composable (Dp, Dp) -> Unit = { containerSize, glyphSize ->
        HomepageIcon(
            icon = item.icon,
            iconRes = item.iconRes,
            style = style.iconStyle,
            tinted = item.tintIcon,
            containerSize = containerSize,
            glyphSize = glyphSize,
            color = iconColor,
            shape = iconShape,
        )
    }

    when (size) {
        TileSize.ROW -> {
            val showChevron = chevron != ChevronStyle.HIDDEN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!iconAtEnd) {
                    iconSlot(40.dp, 22.dp)
                    Spacer(Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    TileTitle(item.title)
                    if (summary != null) {
                        TileSummary(
                            text = summary,
                            color = if (isToggle) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 2,
                        )
                    }
                }
                if (iconAtEnd) {
                    Spacer(Modifier.width(16.dp))
                    iconSlot(40.dp, 22.dp)
                }
                if (isToggle) {
                    Spacer(Modifier.width(8.dp))
                    HomepageSwitch(checked, onCheckedChange, interactive)
                } else if (showChevron) {
                    Spacer(Modifier.width(8.dp))
                    Chevron(chevron)
                }
            }
        }

        TileSize.SMALL -> Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!iconAtEnd) {
                iconSlot(40.dp, 22.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                TileTitle(item.title)
                if (summary != null) TileSummary(summary, small = true)
            }
            if (iconAtEnd) {
                Spacer(Modifier.width(10.dp))
                iconSlot(40.dp, 22.dp)
            }
            if (isToggle) {
                HomepageSwitch(checked, onCheckedChange, interactive, compact = true)
            }
        }

        TileSize.MEDIUM, TileSize.LARGE -> {
            val large = size == TileSize.LARGE
            val showChevron = large && chevron != ChevronStyle.HIDDEN
            val container = if (large) 52.dp else 44.dp
            val glyph = if (large) 28.dp else 24.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (large) 20.dp else 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (iconAtEnd) {
                        if (isToggle) {
                            HomepageSwitch(checked, onCheckedChange, interactive, compact = !large)
                        } else if (showChevron) {
                            Chevron(chevron)
                        }
                        Spacer(Modifier.weight(1f))
                        iconSlot(container, glyph)
                    } else {
                        iconSlot(container, glyph)
                        Spacer(Modifier.weight(1f))
                        if (isToggle) {
                            HomepageSwitch(checked, onCheckedChange, interactive, compact = !large)
                        } else if (showChevron) {
                            Chevron(chevron)
                        }
                    }
                }
                Column {
                    TileTitle(item.title, large = large)
                    if (summary != null) {
                        TileSummary(summary, small = !large, maxLines = if (large) 2 else 2)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomepageCard(
    entries: List<HomepageItem>,
    style: TileStyle,
    onEntryClick: (HomepageItem) -> Unit,
    checkedOf: (HomepageItem) -> Boolean,
    onToggle: (HomepageItem, Boolean) -> Unit,
    searchHint: String,
    modifier: Modifier = Modifier,
    backdrop: ImageBitmap? = null,
) {
    if (style.dividerStyle == DividerStyle.SPLIT) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SPLIT_CARD_GAP),
        ) {
            entries.forEach { item ->
                HomepageTile(
                    item = item,
                    size = TileSize.ROW,
                    style = style,
                    onClick = { onEntryClick(item) },
                    checked = checkedOf(item),
                    onCheckedChange = { onToggle(item, it) },
                    searchHint = searchHint,
                    backdrop = backdrop,
                )
            }
        }
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(style.corner),
        color = cardSurfaceColor(backdrop),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box {
            if (backdrop != null) CardBackdrop(backdrop)
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                entries.forEachIndexed { index, item ->
                    Box(modifier = Modifier.clickable { onEntryClick(item) }) {
                        TileBody(
                            item = item,
                            size = TileSize.ROW,
                            style = style,
                            checked = checkedOf(item),
                            onCheckedChange = { onToggle(item, it) },
                            interactive = true,
                            searchHint = searchHint,
                        )
                    }
                    if (index < entries.lastIndex) {
                        HomepageDivider(style.dividerStyle, style.iconPosition)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomepageDivider(
    style: DividerStyle,
    iconPosition: IconPosition = IconPosition.START,
) {
    val scheme = MaterialTheme.colorScheme
    when (style) {
        DividerStyle.NONE -> Unit

        DividerStyle.GAP -> HorizontalDivider(
            thickness = ROW_GAP_THICKNESS,
            color = scheme.surfaceContainer,
        )

        DividerStyle.INSET -> HorizontalDivider(
            modifier = if (iconPosition == IconPosition.END) {
                Modifier.padding(start = 16.dp, end = GROUP_DIVIDER_START_INSET)
            } else {
                Modifier.padding(start = GROUP_DIVIDER_START_INSET, end = 16.dp)
            },
            color = scheme.outlineVariant.copy(alpha = 0.4f),
        )

        DividerStyle.FULL -> HorizontalDivider(
            color = scheme.outlineVariant.copy(alpha = 0.4f),
        )

        DividerStyle.THICK -> HorizontalDivider(
            thickness = 2.dp,
            color = scheme.outlineVariant.copy(alpha = 0.8f),
        )

        DividerStyle.SPLIT -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TileTitle(text: String, large: Boolean = false) {
    Text(
        text = text,
        style = if (large) {
            MaterialTheme.typography.titleLargeEmphasized
        } else {
            MaterialTheme.typography.titleMediumEmphasized
        },
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = if (large) 2 else 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TileSummary(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    small: Boolean = false,
    maxLines: Int = 1,
) {
    Text(
        text = text,
        style = if (small) {
            MaterialTheme.typography.bodySmall
        } else {
            MaterialTheme.typography.bodyMedium
        },
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Chevron(style: ChevronStyle, size: Dp = 24.dp) {
    val glyph: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size),
        )
    }
    if (style == ChevronStyle.CONTAINED) {
        Box(
            modifier = Modifier
                .size(size + CHEVRON_CONTAINER_PADDING * 2)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            glyph()
        }
    } else {
        glyph()
    }
}

@Composable
internal fun HomepageSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    interactive: Boolean = true,
    compact: Boolean = false,
) {
    Switch(
        checked = checked,
        onCheckedChange = if (interactive) onCheckedChange else null,
        modifier = if (compact) Modifier.scale(0.75f) else Modifier,
        thumbContent = {
            Crossfade(targetState = checked, label = "homepage_switch_icon") { isChecked ->
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

@Composable
private fun SearchBarBody(size: TileSize, hint: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (size.width == TileWidth.FULL) 20.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (size.width == TileWidth.FULL) {
            Arrangement.Start
        } else {
            Arrangement.Center
        },
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = if (size.width == TileWidth.FULL) null else "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        if (size.width == TileWidth.FULL) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PinnedSearchBar(
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
    glass: HomepageGlassBackdrop? = null,
) {
    val shape = RoundedCornerShape(SEARCH_BAR_RADIUS)
    Surface(
        modifier = modifier
            .height(SEARCH_BAR_HEIGHT)
            .then(
                if (glass != null) {
                    Modifier.homepageGlass(
                        backdrop = glass,
                        shape = shape,
                        tint = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.6f),
                        highlight = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f),
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (glass != null) Color.Transparent else MaterialTheme.colorScheme.surfaceBright,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
        shadowElevation = if (elevated) 12.dp else 0.dp,
        tonalElevation = if (elevated && glass == null) 6.dp else 0.dp,
    ) {
        SearchBarBody(size = TileSize.ROW, hint = hint)
    }
}

@Composable
internal fun InlineSearchBarTile(
    size: TileSize,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickable: Boolean = true,
    backdrop: ImageBitmap? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(size.fixedHeight() ?: SEARCH_BAR_HEIGHT)
            .clip(RoundedCornerShape(SEARCH_BAR_RADIUS))
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(SEARCH_BAR_RADIUS),
        color = cardSurfaceColor(backdrop),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
    ) {
        Box {
            if (backdrop != null) CardBackdrop(backdrop)
            SearchBarBody(size = size, hint = hint)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomepageHeader(
    config: HeaderConfig,
    title: String,
    greeting: String?,
    avatarBitmap: Bitmap?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = config.size == HeaderSize.COMPACT
    if (!config.showTitle && !config.showAvatar && greeting == null) {
        Spacer(modifier.height(if (compact) 16.dp else 32.dp))
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = if (compact) 24.dp else 74.dp,
                bottom = if (compact) 4.dp else 12.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (greeting != null) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
            }
            if (config.showTitle) {
                Text(
                    text = title,
                    style = if (compact) {
                        MaterialTheme.typography.headlineMediumEmphasized
                    } else {
                        MaterialTheme.typography.displayMediumEmphasized
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (config.showAvatar) {
            Spacer(Modifier.width(12.dp))
            val avatarSize = if (compact) 44.dp else 52.dp
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(config.avatarShape.containerShape())
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
}
