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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val RESIZE_STEP = 44.dp
private val DEFAULT_AVATAR_SHAPE = HeaderConfig().avatarShape

@Composable
internal fun EditModeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glass: HomepageGlassBackdrop? = null,
) {
    Surface(
        modifier = modifier
            .size(SEARCH_BAR_HEIGHT)
            .clip(CircleShape)
            .then(
                if (glass != null) {
                    Modifier
                        .homepageGlass(
                            backdrop = glass,
                            shape = CircleShape,
                            tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            highlight = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f),
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            CircleShape,
                        )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (glass != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = if (glass != null) 0.dp else 12.dp,
        tonalElevation = if (glass != null) 0.dp else 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Customise homepage",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EditActionBar(
    dirty: Boolean,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    glass: HomepageGlassBackdrop? = null,
) {
    val shape = RoundedCornerShape(SEARCH_BAR_RADIUS)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (glass != null) {
                    Modifier
                        .homepageGlass(
                            backdrop = glass,
                            shape = shape,
                            tint = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.3f),
                            highlight = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.5f),
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            shape,
                        )
                } else {
                    Modifier
                }
            )
            .clip(shape),
        shape = shape,
        color = if (glass != null) Color.Transparent else MaterialTheme.colorScheme.surfaceBright,
        shadowElevation = if (glass != null) 0.dp else 12.dp,
        tonalElevation = if (glass != null) 0.dp else 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Discard")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset")
            }
            Spacer(Modifier.width(4.dp))
            Button(onClick = onSave) {
                Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (dirty) "Save" else "Done")
            }
        }
    }
}

@Composable
internal fun BoxScope.TileEditControls(
    size: TileSize,
    reorderState: GridReorderState,
    onOpenOptions: () -> Unit,
    onResize: (TileSize) -> Unit,
) {
    val liveSize = rememberUpdatedState(size)
    val stepPx = with(LocalDensity.current) { RESIZE_STEP.toPx() }

    HandleChip(
        icon = Icons.Filled.MoreVert,
        description = "Tile options",
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .clickable(onClick = onOpenOptions),
    )
    HandleChip(
        icon = Icons.Outlined.OpenInFull,
        description = "Resize tile",
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp)
            .tileResizeGesture(
                reorderState = reorderState,
                size = { liveSize.value },
                onResize = onResize,
                stepPx = stepPx,
            ),
    )
}

@Composable
private fun HandleChip(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
internal fun CardBreakCell(
    editing: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Card break",
) {
    if (!editing) {
        Spacer(modifier.height(4.dp))
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.6f))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Remove card break",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TileOptionsSheet(
    item: HomepageItem,
    size: TileSize,
    title: String,
    summary: String,
    hidden: Boolean,
    hasSizeOverride: Boolean,
    hasLabelOverride: Boolean,
    themedIcon: Boolean,
    wallpaperBackground: Boolean,
    iconColor: Color,
    hasIconColorOverride: Boolean,
    inheritedColorLabel: String,
    iconShape: IconShape,
    hasIconShapeOverride: Boolean,
    iconPosition: IconPosition,
    hasIconPositionOverride: Boolean,
    iconStyle: IconStyle,
    onTitle: (String) -> Unit,
    onSummary: (String) -> Unit,
    onResetLabel: () -> Unit,
    onSize: (TileSize) -> Unit,
    onResetSize: () -> Unit,
    onHidden: (Boolean) -> Unit,
    onThemedIcon: (Boolean) -> Unit,
    onWallpaperBackground: (Boolean) -> Unit,
    onPickIconColor: () -> Unit,
    onResetIconColor: () -> Unit,
    onPickIconShape: () -> Unit,
    onResetIconShape: () -> Unit,
    onIconPosition: (IconPosition) -> Unit,
    onResetIconPosition: () -> Unit,
    onMoveToTop: () -> Unit,
    onAddBreakAfter: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = title.ifEmpty { item.title },
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.kind != ItemKind.SEARCH_BAR) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Label and summary")
                    Spacer(Modifier.weight(1f))
                    if (hasLabelOverride) {
                        CornerResetButton(
                            description = "Restore original label and summary",
                            onClick = onResetLabel,
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitle,
                    label = { Text("Label") },
                    placeholder = { Text(item.title) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = summary,
                    onValueChange = onSummary,
                    label = { Text("Summary") },
                    placeholder = { Text("No summary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Tile size")
            ChipOptions(
                options = TileSize.values().toList(),
                selected = size,
                labelOf = { it.displayName() },
                onSelect = onSize,
            )
            if (hasSizeOverride) {
                TextButton(onClick = onResetSize, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Use default size")
                }
            }

            if (item.kind != ItemKind.SEARCH_BAR) {
                val themed = themedIcon && item.monoIcon != null
                val tinted = item.tintIcon || themed
                SectionLabel("Icon")
                ColorRow(
                    label = "Icon colour",
                    color = iconColor,
                    overridden = hasIconColorOverride,
                    inheritedFrom = inheritedColorLabel,
                    onClick = onPickIconColor,
                    onReset = onResetIconColor,
                )
                ShapeRow(
                    label = "Icon shape",
                    shape = iconShape,
                    overridden = hasIconShapeOverride,
                    inheritedFrom = "all tile icons",
                    onClick = onPickIconShape,
                    onReset = onResetIconShape,
                    icon = if (themed) item.monoIcon else item.icon,
                    iconRes = if (themed) null else item.iconRes,
                    tinted = tinted,
                    iconStyle = iconStyle,
                    iconColor = iconColor,
                )
                if (!tinted) {
                    Text(
                        text = "This entry's artwork keeps its own colours. The colour applies to " +
                            "the shape behind it, on the Accent and Filled styles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Icon position")
                    Spacer(Modifier.weight(1f))
                    if (hasIconPositionOverride) {
                        CornerResetButton(
                            description = "Follow the page's icon position",
                            onClick = onResetIconPosition,
                        )
                    }
                }
                ChipOptions(
                    options = IconPosition.values().toList(),
                    selected = iconPosition,
                    labelOf = { it.displayName() },
                    onSelect = onIconPosition,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (item.monoIcon != null) {
                SheetAction(
                    icon = Icons.Outlined.Palette,
                    label = if (themedIcon) "Use original icon" else "Use themed icon",
                    onClick = { onThemedIcon(!themedIcon) },
                )
            }
            SheetAction(
                icon = Icons.Outlined.Wallpaper,
                label = if (wallpaperBackground) "Use card colour" else "Wallpaper background",
                onClick = { onWallpaperBackground(!wallpaperBackground) },
            )
            SheetAction(
                icon = if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                label = if (hidden) "Show on homepage" else "Hide from homepage",
                onClick = { onHidden(!hidden) },
            )
            SheetAction(
                icon = Icons.Outlined.ArrowUpward,
                label = "Move to top",
                onClick = onMoveToTop,
            )
            SheetAction(
                icon = Icons.Outlined.Add,
                label = "Add card break below",
                onClick = onAddBreakAfter,
            )
        }
    }
}

@Composable
private fun CornerResetButton(description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.RestartAlt,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun PickerRow(
    label: String,
    overridden: Boolean,
    inheritedFrom: String,
    onClick: () -> Unit,
    onReset: () -> Unit,
    preview: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        preview()
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (overridden) "Custom" else "Following $inheritedFrom",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (overridden) {
            Spacer(Modifier.width(8.dp))
            CornerResetButton(description = "Reset $label", onClick = onReset)
        }
    }
}

@Composable
private fun ColorRow(
    label: String,
    color: Color,
    overridden: Boolean,
    inheritedFrom: String,
    onClick: () -> Unit,
    onReset: () -> Unit,
) {
    PickerRow(
        label = label,
        overridden = overridden,
        inheritedFrom = inheritedFrom,
        onClick = onClick,
        onReset = onReset,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
    }
}

@Composable
private fun ShapeRow(
    label: String,
    shape: IconShape,
    overridden: Boolean,
    inheritedFrom: String,
    onClick: () -> Unit,
    onReset: () -> Unit,
    icon: ImageVector? = Icons.Outlined.Settings,
    @DrawableRes iconRes: Int? = null,
    tinted: Boolean = true,
    iconStyle: IconStyle = IconStyle.ACCENT,
    iconColor: Color? = null,
) {
    PickerRow(
        label = label,
        overridden = overridden,
        inheritedFrom = inheritedFrom,
        onClick = onClick,
        onReset = onReset,
    ) {
        HomepageIcon(
            icon = icon,
            iconRes = iconRes,
            style = iconStyle.previewable(),
            tinted = tinted,
            containerSize = 32.dp,
            glyphSize = 18.dp,
            color = iconColor,
            shape = shape.containerShape(),
        )
    }
}

private fun IconStyle.previewable(): IconStyle =
    if (this == IconStyle.PLAIN) IconStyle.ACCENT else this

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val SHAPE_SWATCH_WIDTH = 62.dp
private val SHAPE_SWATCH_RING = 50.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ShapePickerDialog(
    title: String,
    selected: IconShape,
    onPick: (IconShape) -> Unit,
    onDismiss: () -> Unit,
    previewIcon: ImageVector? = Icons.Outlined.Settings,
    @DrawableRes previewIconRes: Int? = null,
    previewTinted: Boolean = true,
    iconStyle: IconStyle = IconStyle.ACCENT,
    iconColor: Color? = null,
    onReset: (() -> Unit)? = null,
    followLabel: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 4,
                ) {
                    IconShape.values().forEach { shape ->
                        ShapeSwatch(
                            shape = shape,
                            selected = shape == selected,
                            icon = previewIcon,
                            iconRes = previewIconRes,
                            tinted = previewTinted,
                            iconStyle = iconStyle,
                            iconColor = iconColor,
                            onClick = { onPick(shape) },
                        )
                    }
                }
                if (iconStyle == IconStyle.PLAIN) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "The Plain icon style draws no container, so shapes only show on " +
                            "the Accent and Filled styles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onReset == null && followLabel != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Following $followLabel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = onReset?.let {
            {
                TextButton(onClick = it) {
                    Text(if (followLabel != null) "Follow $followLabel" else "Reset")
                }
            }
        },
    )
}

@Composable
private fun ShapeSwatch(
    shape: IconShape,
    selected: Boolean,
    icon: ImageVector?,
    @DrawableRes iconRes: Int?,
    tinted: Boolean,
    iconStyle: IconStyle,
    iconColor: Color?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(SHAPE_SWATCH_WIDTH)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(SHAPE_SWATCH_RING)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            HomepageIcon(
                icon = icon,
                iconRes = iconRes,
                style = iconStyle.previewable(),
                tinted = tinted,
                containerSize = 38.dp,
                glyphSize = 20.dp,
                color = iconColor,
                shape = shape.containerShape(),
            )
        }
        Text(
            text = shape.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppearancePanel(
    layout: HomepageLayout,
    defaultTitle: String,
    onLayout: (HomepageLayout) -> Unit,
    onPickIconColor: () -> Unit,
    onPickIconShape: () -> Unit,
    onPickAvatarShape: () -> Unit,
    onAddBreak: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(layout.cornerStyle.radius),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Page style",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Long press a tile to move it. Drag its corner to resize.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.ExpandLess
                    } else {
                        Icons.Outlined.ExpandMore
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    SectionLabel("Search bar")
                    ChipOptions(
                        options = SearchBarPosition.values().toList(),
                        selected = layout.searchBar,
                        labelOf = { it.displayName() },
                        onSelect = { onLayout(layout.copy(searchBar = it)) },
                    )
                    if (layout.searchBar == SearchBarPosition.TOP) {
                        SwitchRow(
                            label = "Card break below search bar",
                            checked = layout.searchBarBreak,
                            onCheckedChange = { onLayout(layout.copy(searchBarBreak = it)) },
                        )
                    }
                    if (layout.searchBar == SearchBarPosition.BOTTOM) {
                        SwitchRow(
                            label = "Glass blur behind bottom bar",
                            checked = layout.bottomBarGlass,
                            onCheckedChange = { onLayout(layout.copy(bottomBarGlass = it)) },
                        )
                    }

                    SectionLabel("Header")
                    OutlinedTextField(
                        value = layout.header.customTitle ?: "",
                        onValueChange = {
                            val title = it.takeIf { t -> t.isNotEmpty() }
                            onLayout(layout.copy(header = layout.header.copy(customTitle = title)))
                        },
                        label = { Text("Title") },
                        placeholder = { Text(defaultTitle) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    ChipOptions(
                        options = HeaderSize.values().toList(),
                        selected = layout.header.size,
                        labelOf = { it.displayName() },
                        onSelect = { onLayout(layout.copy(header = layout.header.copy(size = it))) },
                    )
                    SwitchRow(
                        label = "Show title",
                        checked = layout.header.showTitle,
                        onCheckedChange = {
                            onLayout(layout.copy(header = layout.header.copy(showTitle = it)))
                        },
                    )
                    SwitchRow(
                        label = "Show avatar",
                        checked = layout.header.showAvatar,
                        onCheckedChange = {
                            onLayout(layout.copy(header = layout.header.copy(showAvatar = it)))
                        },
                    )
                    if (layout.header.showAvatar) {
                        ShapeRow(
                            label = "Avatar shape",
                            shape = layout.header.avatarShape,
                            overridden = layout.header.avatarShape != DEFAULT_AVATAR_SHAPE,
                            inheritedFrom = "the default shape",
                            onClick = onPickAvatarShape,
                            onReset = {
                                onLayout(
                                    layout.copy(
                                        header = layout.header.copy(
                                            avatarShape = DEFAULT_AVATAR_SHAPE,
                                        )
                                    )
                                )
                            },
                            icon = Icons.Outlined.AccountCircle,
                            iconStyle = IconStyle.FILLED,
                            iconColor = MaterialTheme.colorScheme.surfaceBright,
                        )
                    }
                    SwitchRow(
                        label = "Show greeting",
                        checked = layout.header.showGreeting,
                        onCheckedChange = {
                            onLayout(layout.copy(header = layout.header.copy(showGreeting = it)))
                        },
                    )

                    SectionLabel("Tiles")
                    SwitchRow(
                        label = "Show summaries",
                        checked = layout.showSummaries,
                        onCheckedChange = { onLayout(layout.copy(showSummaries = it)) },
                    )

                    SectionLabel("Icons")
                    ChipOptions(
                        options = IconStyle.values().toList(),
                        selected = layout.iconStyle,
                        labelOf = { it.displayName() },
                        onSelect = { onLayout(layout.copy(iconStyle = it)) },
                    )
                    ColorRow(
                        label = "All tile icons",
                        color = layout.iconColor?.let { Color(it) }
                            ?: MaterialTheme.colorScheme.primary,
                        overridden = layout.iconColor != null,
                        inheritedFrom = "theme accent",
                        onClick = onPickIconColor,
                        onReset = { onLayout(layout.resetIconColor()) },
                    )
                    val colorOverrides = layout.iconColors.size
                    if (colorOverrides > 0) {
                        TextButton(onClick = { onLayout(layout.resetEntryIconColors()) }) {
                            Text(
                                if (colorOverrides == 1) {
                                    "1 tile has its own colour — clear it"
                                } else {
                                    "$colorOverrides tiles have their own colour — clear them"
                                }
                            )
                        }
                    }
                    ShapeRow(
                        label = "Icon shape",
                        shape = layout.iconShape,
                        overridden = layout.iconShape != IconShape.CIRCLE,
                        inheritedFrom = "circle",
                        onClick = onPickIconShape,
                        onReset = { onLayout(layout.resetIconShape()) },
                        iconStyle = layout.iconStyle,
                        iconColor = layout.iconColor?.let { Color(it) },
                    )
                    val shapeOverrides = layout.iconShapes.size
                    if (shapeOverrides > 0) {
                        TextButton(onClick = { onLayout(layout.resetEntryIconShapes()) }) {
                            Text(
                                if (shapeOverrides == 1) {
                                    "1 tile has its own shape — clear it"
                                } else {
                                    "$shapeOverrides tiles have their own shape — clear them"
                                }
                            )
                        }
                    }

                    SectionLabel("Icon position")
                    ChipOptions(
                        options = IconPosition.values().toList(),
                        selected = layout.iconPosition,
                        labelOf = { it.displayName() },
                        onSelect = { onLayout(layout.withIconPosition(it)) },
                    )
                    val positionOverrides = layout.iconPositions.size
                    if (positionOverrides > 0) {
                        TextButton(onClick = { onLayout(layout.resetEntryIconPositions()) }) {
                            Text(
                                if (positionOverrides == 1) {
                                    "1 tile has its own position — clear it"
                                } else {
                                    "$positionOverrides tiles have their own position — clear them"
                                }
                            )
                        }
                    }

                    SectionLabel("Chevron")
                    ChipOptions(
                        options = ChevronStyle.values().toList(),
                        selected = layout.chevronStyle,
                        labelOf = { it.displayName() },
                        onSelect = { onLayout(layout.copy(chevronStyle = it)) },
                    )

                    SectionLabel("Corners")
                    ChipOptions(
                        options = CornerStyle.values().toList(),
                        selected = layout.cornerStyle,
                        labelOf = { it.displayName() },
                        onSelect = { onLayout(layout.copy(cornerStyle = it)) },
                    )

                    SectionLabel("Dividers")
                    ChipOptions(
                        options = DividerStyle.values().toList(),
                        selected = layout.dividerStyle,
                        labelOf = { it.displayName() },
                        onSelect = { onLayout(layout.copy(dividerStyle = it)) },
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onAddBreak) {
                            Icon(Icons.Outlined.Add, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Card break")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = onResetAll) {
                            Icon(
                                Icons.Outlined.RestartAlt,
                                contentDescription = null,
                                Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Reset all")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HiddenTray(
    items: List<HomepageItem>,
    corner: Dp,
    onRestore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(corner),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Hidden",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            if (items.isEmpty()) {
                Text(
                    text = "Nothing hidden. Use a tile's options to hide it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape,
                                )
                                .clickable { onRestore(item.key) }
                                .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Restore ${item.title}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ResetConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset homepage?") },
        text = {
            Text(
                "Order, sizes, labels, icon colours and shapes, hidden preferences and page style " +
                    "all go back to default."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Reset") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipOptions(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(labelOf(option)) },
                leadingIcon = if (option == selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        HomepageSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

internal fun TileSize.displayName(): String = when (this) {
    TileSize.SMALL -> "Small"
    TileSize.MEDIUM -> "Medium"
    TileSize.ROW -> "List row"
    TileSize.LARGE -> "Large"
}

internal fun SearchBarPosition.displayName(): String = when (this) {
    SearchBarPosition.TOP -> "Top"
    SearchBarPosition.BOTTOM -> "Bottom"
    SearchBarPosition.INLINE -> "In page"
    SearchBarPosition.HIDDEN -> "Hidden"
}

internal fun IconStyle.displayName(): String = when (this) {
    IconStyle.ACCENT -> "Accent"
    IconStyle.PLAIN -> "Plain"
    IconStyle.FILLED -> "Filled"
}

internal fun IconShape.displayName(): String = when (this) {
    IconShape.CIRCLE -> "Circle"
    IconShape.ARROW -> "Arrow"
    IconShape.PILL -> "Pill"
    IconShape.TRIANGLE -> "Triangle"
    IconShape.GEM -> "Gem"
    IconShape.COOKIE_4 -> "Cookie 4"
    IconShape.COOKIE_6 -> "Cookie 6"
    IconShape.CLOVER_4 -> "Clover"
}

internal fun IconPosition.displayName(): String = when (this) {
    IconPosition.START -> "Icon at start"
    IconPosition.END -> "Icon at end"
}

internal fun ChevronStyle.displayName(): String = when (this) {
    ChevronStyle.PLAIN -> "Plain"
    ChevronStyle.CONTAINED -> "On a circle"
    ChevronStyle.HIDDEN -> "Hidden"
}

internal fun CornerStyle.displayName(): String = when (this) {
    CornerStyle.SOFT -> "Soft"
    CornerStyle.ROUNDED -> "Rounded"
    CornerStyle.PILL -> "Pill"
}

internal fun DividerStyle.displayName(): String = when (this) {
    DividerStyle.NONE -> "None"
    DividerStyle.GAP -> "Gap"
    DividerStyle.INSET -> "Inset"
    DividerStyle.FULL -> "Full"
    DividerStyle.THICK -> "Thick"
    DividerStyle.SPLIT -> "Split cards"
}

internal fun HeaderSize.displayName(): String = when (this) {
    HeaderSize.LARGE -> "Large header"
    HeaderSize.COMPACT -> "Compact header"
}

internal fun HomepageLayout.inheritedColorLabel(): String =
    if (iconColor != null) "page colour" else "theme accent"
