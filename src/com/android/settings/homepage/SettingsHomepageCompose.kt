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

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.axion.compose.theme.AxionTheme
import com.android.settings.R
import java.util.function.Consumer

class HomepageEditController {

    var isEditing: Boolean by mutableStateOf(false)
        internal set

    internal var discard: (() -> Unit)? = null

    fun discardEdits(): Boolean {
        if (!isEditing) return false
        discard?.invoke()
        return true
    }
}

private sealed class HomepageCell(val key: String, val span: Int) {

    class Chrome(val kind: ChromeKind) : HomepageCell("chrome:${kind.name}", 2)

    class Entry(val item: HomepageItem, val size: TileSize, val wallpaper: Boolean) :
        HomepageCell(reorderCellKey(item.key), size.span)

    class Group(val entries: List<HomepageItem>, val wallpaper: Boolean) :
        HomepageCell("card:${entries.first().key}", 2)

    class Break(val layoutKey: String) : HomepageCell(reorderCellKey(layoutKey), 2)
}

private enum class ChromeKind { HEADER, STYLE_PANEL, TOP_SEARCH, SEARCH_BREAK, HIDDEN_TRAY }

private sealed class ColorTarget {

    object Page : ColorTarget()

    class Entry(val key: String) : ColorTarget()
}

private sealed class ShapeTarget {

    object Page : ShapeTarget()

    class Entry(val key: String) : ShapeTarget()

    object Avatar : ShapeTarget()
}

private fun buildCells(
    layout: HomepageLayout,
    catalog: HomepageCatalog,
    editing: Boolean,
): List<HomepageCell> {
    val cells = mutableListOf<HomepageCell>()
    cells += HomepageCell.Chrome(ChromeKind.HEADER)
    if (layout.searchBar == SearchBarPosition.TOP) {
        cells += HomepageCell.Chrome(ChromeKind.TOP_SEARCH)
        if (layout.searchBarBreak) {
            cells += HomepageCell.Chrome(ChromeKind.SEARCH_BREAK)
        }
    }
    if (editing) {
        cells += HomepageCell.Chrome(ChromeKind.STYLE_PANEL)
    }

    val pendingRows = mutableListOf<HomepageItem>()
    var pendingWallpaper = false
    fun flushRows() {
        if (pendingRows.isNotEmpty()) {
            cells += HomepageCell.Group(pendingRows.toList(), pendingWallpaper)
            pendingRows.clear()
        }
    }

    val seen = mutableSetOf<String>()
    for (key in layout.order) {
        if (!seen.add(key)) continue
        if (layout.isHidden(key)) continue
        if (isSeparatorKey(key)) {
            flushRows()
            if (editing || cells.lastOrNull().isContent()) {
                cells += HomepageCell.Break(key)
            }
            continue
        }
        val item = catalog.find(key) ?: continue
        if (item.kind == ItemKind.SEARCH_BAR && layout.searchBar != SearchBarPosition.INLINE) {
            continue
        }
        val resolved = layout.resolve(item)
        val size = layout.sizeOf(key, item.defaultSize)
        val wallpaper = layout.usesWallpaper(key)
        val mergeable = !editing && size == TileSize.ROW && item.kind != ItemKind.SEARCH_BAR
        if (mergeable) {
            if (pendingRows.isNotEmpty() && wallpaper != pendingWallpaper) flushRows()
            pendingWallpaper = wallpaper
            pendingRows += resolved
        } else {
            flushRows()
            cells += HomepageCell.Entry(resolved, size, wallpaper)
        }
    }
    flushRows()

    if (editing) {
        cells += HomepageCell.Chrome(ChromeKind.HIDDEN_TRAY)
    } else {
        while (cells.lastOrNull() is HomepageCell.Break) cells.removeAt(cells.lastIndex)
    }
    return cells
}

private fun HomepageCell?.isContent(): Boolean =
    this is HomepageCell.Entry || this is HomepageCell.Group

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    isWifiEnabled: Boolean = true,
    wifiSummary: String? = null,
    onWifiToggle: (Boolean) -> Unit = {},
    isAirplaneModeOn: Boolean = false,
    airplaneSummary: String? = null,
    onAirplaneToggle: (Boolean) -> Unit = {},
    editController: HomepageEditController = remember { HomepageEditController() },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val defaultTitle = stringResource(R.string.settings_label)

    val catalog = rememberHomepageCatalog(
        isCommunalAvailable = isCommunalAvailable,
        isSafetyCenterAvailable = isSafetyCenterAvailable,
        isEmergencyAvailable = isEmergencyAvailable,
        isSupportAvailable = isSupportAvailable,
        isWellbeingAvailable = isWellbeingAvailable,
        isGoogleAvailable = isGoogleAvailable,
        wifiSummary = wifiSummary,
        airplaneSummary = airplaneSummary,
    )
    val defaultOrder = catalog.defaultOrder
    val defaultLayout = remember(defaultOrder, catalog.defaultHidden) {
        HomepageLayout(order = defaultOrder, hidden = catalog.defaultHidden)
    }
    val store = remember(context) { HomepageLayoutStore(context) }

    var layout by remember(store, defaultLayout) { mutableStateOf(store.load(defaultLayout)) }
    var snapshot by remember { mutableStateOf<HomepageLayout?>(null) }
    var optionsKey by remember { mutableStateOf<String?>(null) }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }
    var shapeTarget by remember { mutableStateOf<ShapeTarget?>(null) }
    var confirmingReset by remember { mutableStateOf(false) }

    val editing = snapshot != null
    val style = layout.tileStyle()
    val searchHint = remember { SEARCH_HINTS.random() }
    val wallpaperBackdrop = rememberWallpaperBackdrop(layout.wallpaperCards.isNotEmpty())

    val checkedOf: (HomepageItem) -> Boolean = { item ->
        when (item.key) {
            KEY_WIFI_TOGGLE -> isWifiEnabled
            KEY_AIRPLANE_TOGGLE -> isAirplaneModeOn
            else -> false
        }
    }
    val onToggle: (HomepageItem, Boolean) -> Unit = { item, enabled ->
        when (item.key) {
            KEY_WIFI_TOGGLE -> onWifiToggle(enabled)
            KEY_AIRPLANE_TOGGLE -> onAirplaneToggle(enabled)
        }
    }

    val leaveEditMode: () -> Unit = {
        optionsKey = null
        colorTarget = null
        shapeTarget = null
        confirmingReset = false
        snapshot = null
    }
    val enterEditMode: () -> Unit = { snapshot = layout }
    val discardEdits: () -> Unit = {
        snapshot?.let { layout = it }
        leaveEditMode()
    }
    val saveEdits: () -> Unit = {
        store.save(layout)
        leaveEditMode()
    }

    SideEffect {
        editController.isEditing = editing
        editController.discard = discardEdits
    }
    DisposableEffect(editController) {
        onDispose {
            editController.discard = null
            editController.isEditing = false
        }
    }

    val gridState = rememberLazyGridState()
    val glassBackdrop = rememberHomepageGlassBackdrop()
    val glassEnabled = layout.bottomBarGlass &&
        (editing || layout.searchBar == SearchBarPosition.BOTTOM)
    val reorderState = rememberGridReorderState(gridState) { from, to ->
        layout = layout.move(from, to)
    }

    val cells = buildCells(layout, catalog, editing)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AxionTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (glassEnabled) {
                                Modifier
                                    .homepageGlassSource(glassBackdrop)
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            } else {
                                Modifier
                            }
                        )
                        .then(
                            if (editing) {
                                Modifier.homepageDragContainer(reorderState) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            } else {
                                Modifier
                            }
                        ),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = statusBarPadding,
                        bottom = SEARCH_BAR_HEIGHT + SEARCH_BAR_BOTTOM_PADDING * 2 +
                            navBarPadding + 8.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(GRID_TILE_SPACING),
                    verticalArrangement = Arrangement.spacedBy(GRID_TILE_SPACING),
                ) {
                    cells.forEach { cell ->
                        item(key = cell.key, span = { GridItemSpan(cell.span) }) {
                            val dragged = reorderState.draggingKey == cell.key
                            val cellModifier = when {
                                dragged -> Modifier.draggingItem(reorderState, cell.key)
                                editing -> Modifier.animateItem()
                                else -> Modifier
                            }
                            when (cell) {
                                is HomepageCell.Chrome -> ChromeContent(
                                    kind = cell.kind,
                                    layout = layout,
                                    catalog = catalog,
                                    defaultTitle = defaultTitle,
                                    userName = userName,
                                    avatarBitmap = avatarBitmap,
                                    searchHint = searchHint,
                                    editing = editing,
                                    onAvatarClick = onAvatarClick,
                                    onSearchClick = onSearchClick,
                                    onLayout = { layout = it },
                                    onPickIconColor = { colorTarget = ColorTarget.Page },
                                    onPickIconShape = { shapeTarget = ShapeTarget.Page },
                                    onPickAvatarShape = { shapeTarget = ShapeTarget.Avatar },
                                    onAddBreak = { layout = layout.appendSeparator() },
                                    onResetAll = { confirmingReset = true },
                                    modifier = cellModifier,
                                )

                                is HomepageCell.Group -> HomepageCard(
                                    entries = cell.entries,
                                    style = style,
                                    onEntryClick = { onPreferenceClick(it.navKey) },
                                    checkedOf = checkedOf,
                                    onToggle = onToggle,
                                    searchHint = searchHint,
                                    modifier = cellModifier,
                                    backdrop = wallpaperBackdrop.takeIf { cell.wallpaper },
                                )

                                is HomepageCell.Break -> CardBreakCell(
                                    editing = editing,
                                    onDelete = { layout = layout.removeEntry(cell.layoutKey) },
                                    modifier = cellModifier,
                                )

                                is HomepageCell.Entry -> Box(modifier = cellModifier) {
                                    val backdrop = wallpaperBackdrop.takeIf { cell.wallpaper }
                                    if (cell.item.kind == ItemKind.SEARCH_BAR) {
                                        InlineSearchBarTile(
                                            size = cell.size,
                                            hint = searchHint,
                                            onClick = onSearchClick,
                                            clickable = !editing,
                                            backdrop = backdrop,
                                        )
                                    } else {
                                        HomepageTile(
                                            item = cell.item,
                                            size = cell.size,
                                            style = style,
                                            onClick = { onPreferenceClick(cell.item.navKey) },
                                            clickable = !editing,
                                            checked = checkedOf(cell.item),
                                            onCheckedChange = { onToggle(cell.item, it) },
                                            searchHint = searchHint,
                                            backdrop = backdrop,
                                        )
                                    }
                                    if (editing) {
                                        TileEditControls(
                                            size = cell.size,
                                            reorderState = reorderState,
                                            onOpenOptions = { optionsKey = cell.item.key },
                                            onResize = {
                                                layout = layout.withSize(cell.item.key, it)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                BottomChrome(
                    editing = editing,
                    searchBar = layout.searchBar,
                    dirty = editing && snapshot != layout,
                    searchHint = searchHint,
                    onSearchClick = onSearchClick,
                    onEnterEditMode = enterEditMode,
                    onDiscard = discardEdits,
                    onReset = { confirmingReset = true },
                    onSave = saveEdits,
                    glass = glassBackdrop.takeIf { glassEnabled },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        val optionsItem = optionsKey?.let { catalog.find(it) }
        if (optionsItem != null) {
            val key = optionsItem.key
            TileOptionsSheet(
                item = optionsItem,
                size = layout.sizeOf(key, optionsItem.defaultSize),
                title = layout.titleDraft(key, optionsItem.title),
                summary = layout.summaryDraft(key, optionsItem.summary),
                hidden = layout.isHidden(key),
                hasSizeOverride = key in layout.sizes,
                hasLabelOverride = layout.hasLabelOverride(key),
                themedIcon = layout.isThemedIcon(key),
                wallpaperBackground = layout.usesWallpaper(key),
                iconColor = layout.iconColorOf(key)?.let { Color(it) }
                    ?: MaterialTheme.colorScheme.primary,
                hasIconColorOverride = layout.hasIconColorOverride(key),
                inheritedColorLabel = layout.inheritedColorLabel(),
                iconShape = layout.iconShapeOf(key),
                hasIconShapeOverride = layout.hasIconShapeOverride(key),
                iconPosition = layout.iconPositionOf(key),
                hasIconPositionOverride = layout.hasIconPositionOverride(key),
                iconStyle = layout.iconStyle,
                onTitle = { layout = layout.withTitle(key, it) },
                onSummary = { layout = layout.withSummary(key, it) },
                onResetLabel = { layout = layout.resetLabel(key) },
                onSize = { layout = layout.withSize(key, it) },
                onResetSize = {
                    layout = layout.resetSize(key)
                    optionsKey = null
                },
                onHidden = {
                    layout = layout.setHidden(key, it)
                    optionsKey = null
                },
                onThemedIcon = {
                    layout = layout.setThemedIcon(key, it)
                    optionsKey = null
                },
                onWallpaperBackground = {
                    layout = layout.setWallpaperCard(key, it)
                    optionsKey = null
                },
                onPickIconColor = {
                    optionsKey = null
                    colorTarget = ColorTarget.Entry(key)
                },
                onResetIconColor = { layout = layout.resetIconColor(key) },
                onPickIconShape = {
                    optionsKey = null
                    shapeTarget = ShapeTarget.Entry(key)
                },
                onResetIconShape = { layout = layout.resetIconShape(key) },
                onIconPosition = { layout = layout.withIconPosition(key, it) },
                onResetIconPosition = { layout = layout.resetIconPosition(key) },
                onMoveToTop = {
                    layout = layout.moveToTop(key)
                    optionsKey = null
                },
                onAddBreakAfter = {
                    layout = layout.insertSeparatorAfter(key)
                    optionsKey = null
                },
                onDismiss = { optionsKey = null },
            )
        }

        val colorEntry = (colorTarget as? ColorTarget.Entry)?.let { catalog.find(it.key) }
        when {
            colorTarget is ColorTarget.Page -> ColorPickerSheet(
                title = "All tile icons",
                initial = layout.iconColor?.let { Color(it) },
                themeColor = MaterialTheme.colorScheme.primary,
                onPick = { layout = layout.withIconColor(it.toArgb()) },
                onReset = {
                    layout = layout.resetIconColor()
                    colorTarget = null
                },
                onDismiss = { colorTarget = null },
            )

            colorEntry != null -> {
                val key = colorEntry.key
                key(key) {
                    ColorPickerSheet(
                        title = layout.titleOf(key, colorEntry.title),
                        initial = layout.iconColors[key]?.let { Color(it) },
                        themeColor = layout.iconColor?.let { Color(it) }
                            ?: MaterialTheme.colorScheme.primary,
                        onPick = { layout = layout.withIconColor(key, it.toArgb()) },
                        onReset = {
                            layout = layout.resetIconColor(key)
                            colorTarget = null
                        },
                        onDismiss = { colorTarget = null },
                        followLabel = layout.inheritedColorLabel(),
                    )
                }
            }
        }

        val shapeEntry = (shapeTarget as? ShapeTarget.Entry)?.let { catalog.find(it.key) }
        when {
            shapeTarget is ShapeTarget.Page -> ShapePickerDialog(
                title = "All tile icons",
                selected = layout.iconShape,
                iconStyle = layout.iconStyle,
                iconColor = layout.iconColor?.let { Color(it) },
                onPick = { layout = layout.withIconShape(it) },
                onReset = {
                    layout = layout.resetIconShape()
                    shapeTarget = null
                },
                onDismiss = { shapeTarget = null },
            )

            shapeTarget is ShapeTarget.Avatar -> ShapePickerDialog(
                title = "Avatar",
                selected = layout.header.avatarShape,
                previewIcon = Icons.Outlined.AccountCircle,
                iconStyle = IconStyle.FILLED,
                iconColor = MaterialTheme.colorScheme.surfaceBright,
                onPick = {
                    layout = layout.copy(header = layout.header.copy(avatarShape = it))
                },
                onReset = {
                    layout = layout.copy(
                        header = layout.header.copy(avatarShape = HeaderConfig().avatarShape),
                    )
                    shapeTarget = null
                },
                onDismiss = { shapeTarget = null },
            )

            shapeEntry != null -> {
                val key = shapeEntry.key
                val resolved = layout.resolve(shapeEntry)
                ShapePickerDialog(
                    title = resolved.title,
                    selected = layout.iconShapeOf(key),
                    previewIcon = resolved.icon,
                    previewIconRes = resolved.iconRes,
                    previewTinted = resolved.tintIcon,
                    iconStyle = layout.iconStyle,
                    iconColor = resolved.iconColor ?: layout.iconColor?.let { Color(it) },
                    onPick = { layout = layout.withIconShape(key, it) },
                    onReset = if (layout.hasIconShapeOverride(key)) {
                        {
                            layout = layout.resetIconShape(key)
                            shapeTarget = null
                        }
                    } else {
                        null
                    },
                    onDismiss = { shapeTarget = null },
                    followLabel = "all tile icons",
                )
            }
        }

        if (confirmingReset) {
            ResetConfirmDialog(
                onConfirm = {
                    layout = defaultLayout
                    confirmingReset = false
                },
                onDismiss = { confirmingReset = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChromeContent(
    kind: ChromeKind,
    layout: HomepageLayout,
    catalog: HomepageCatalog,
    defaultTitle: String,
    userName: String?,
    avatarBitmap: Bitmap?,
    searchHint: String,
    editing: Boolean,
    onAvatarClick: () -> Unit,
    onSearchClick: () -> Unit,
    onLayout: (HomepageLayout) -> Unit,
    onPickIconColor: () -> Unit,
    onPickIconShape: () -> Unit,
    onPickAvatarShape: () -> Unit,
    onAddBreak: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (kind) {
        ChromeKind.HEADER -> HomepageHeader(
            config = layout.header,
            title = layout.header.customTitle ?: defaultTitle,
            greeting = userName?.takeIf { layout.header.showGreeting }?.let { "Hi, $it" },
            avatarBitmap = avatarBitmap,
            onAvatarClick = { if (!editing) onAvatarClick() },
            modifier = modifier,
        )

        ChromeKind.STYLE_PANEL -> AppearancePanel(
            layout = layout,
            defaultTitle = defaultTitle,
            onLayout = onLayout,
            onPickIconColor = onPickIconColor,
            onPickIconShape = onPickIconShape,
            onPickAvatarShape = onPickAvatarShape,
            onAddBreak = onAddBreak,
            onResetAll = onResetAll,
            modifier = modifier,
        )

        ChromeKind.TOP_SEARCH -> PinnedSearchBar(
            hint = searchHint,
            onClick = { if (!editing) onSearchClick() },
            modifier = modifier.fillMaxWidth(),
            elevated = false,
        )

        ChromeKind.SEARCH_BREAK -> CardBreakCell(
            editing = editing,
            onDelete = { onLayout(layout.copy(searchBarBreak = false)) },
            modifier = modifier,
            label = "Below search bar",
        )

        ChromeKind.HIDDEN_TRAY -> HiddenTray(
            items = layout.hiddenOrder().mapNotNull { key ->
                catalog.find(key)?.let { layout.resolve(it) }
            },
            corner = layout.cornerStyle.radius,
            onRestore = { onLayout(layout.setHidden(it, false)) },
            modifier = modifier,
        )
    }
}

@Composable
private fun BottomChrome(
    editing: Boolean,
    searchBar: SearchBarPosition,
    dirty: Boolean,
    searchHint: String,
    onSearchClick: () -> Unit,
    onEnterEditMode: () -> Unit,
    onDiscard: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    glass: HomepageGlassBackdrop? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.surfaceContainer),
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SEARCH_BAR_HORIZONTAL_PADDING,
                    vertical = SEARCH_BAR_BOTTOM_PADDING,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                editing -> EditActionBar(
                    dirty = dirty,
                    onCancel = onDiscard,
                    onReset = onReset,
                    onSave = onSave,
                    glass = glass,
                    modifier = Modifier.weight(1f),
                )

                searchBar == SearchBarPosition.BOTTOM -> {
                    PinnedSearchBar(
                        hint = searchHint,
                        onClick = onSearchClick,
                        glass = glass,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    EditModeButton(onClick = onEnterEditMode, glass = glass)
                }

                else -> {
                    Spacer(Modifier.weight(1f))
                    EditModeButton(onClick = onEnterEditMode)
                }
            }
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

object SettingsHomepageComposeInterop {
    private var userNameState: MutableState<String?>? = null
    private var avatarBitmapState: MutableState<Bitmap?>? = null
    private var wifiEnabledState: MutableState<Boolean>? = null
    private var wifiSummaryState: MutableState<String?>? = null
    private var airplaneModeState: MutableState<Boolean>? = null
    private var airplaneSummaryState: MutableState<String?>? = null
    private var editController: HomepageEditController? = null

    @JvmStatic
    fun setContent(
        view: ComposeView,
        onSearchClick: Runnable,
        onAvatarClick: Runnable,
        onPreferenceClick: Consumer<String>,
        onWifiToggle: Consumer<Boolean>,
        onAirplaneToggle: Consumer<Boolean>,
        onEditModeChanged: Consumer<Boolean>,
        userName: String?,
        avatarBitmap: Bitmap?,
        isWifiEnabled: Boolean,
        wifiSummary: String?,
        isAirplaneModeOn: Boolean,
        airplaneSummary: String?,
        isCommunalAvailable: Boolean,
        isSafetyCenterAvailable: Boolean,
        isEmergencyAvailable: Boolean,
        isSupportAvailable: Boolean,
        isWellbeingAvailable: Boolean,
        isGoogleAvailable: Boolean,
    ) {
        val controller = HomepageEditController()
        editController = controller
        val wifiEnabledMutableState = mutableStateOf(isWifiEnabled)
        val wifiSummaryMutableState = mutableStateOf(wifiSummary)
        wifiEnabledState = wifiEnabledMutableState
        wifiSummaryState = wifiSummaryMutableState
        view.setContent {
            val userNameMutableState = remember { mutableStateOf(userName) }
            val avatarBitmapMutableState = remember { mutableStateOf(avatarBitmap) }
            val airplaneModeMutableState = remember { mutableStateOf(isAirplaneModeOn) }
            val airplaneSummaryMutableState = remember { mutableStateOf(airplaneSummary) }
            userNameState = userNameMutableState
            avatarBitmapState = avatarBitmapMutableState
            airplaneModeState = airplaneModeMutableState
            airplaneSummaryState = airplaneSummaryMutableState

            val isEditing = controller.isEditing
            DisposableEffect(isEditing) {
                onEditModeChanged.accept(isEditing)
                onDispose { }
            }

            AxionTheme {
                SettingsHomepageScreen(
                    onSearchClick = { onSearchClick.run() },
                    onAvatarClick = { onAvatarClick.run() },
                    onPreferenceClick = { onPreferenceClick.accept(it) },
                    userName = userNameMutableState.value,
                    avatarBitmap = avatarBitmapMutableState.value,
                    isWifiEnabled = wifiEnabledMutableState.value,
                    wifiSummary = wifiSummaryMutableState.value,
                    onWifiToggle = { enabled ->
                        onWifiToggle.accept(enabled)
                    },
                    isAirplaneModeOn = airplaneModeMutableState.value,
                    airplaneSummary = airplaneSummaryMutableState.value,
                    onAirplaneToggle = { enabled ->
                        airplaneModeMutableState.value = enabled
                        onAirplaneToggle.accept(enabled)
                    },
                    isCommunalAvailable = isCommunalAvailable,
                    isSafetyCenterAvailable = isSafetyCenterAvailable,
                    isEmergencyAvailable = isEmergencyAvailable,
                    isSupportAvailable = isSupportAvailable,
                    isWellbeingAvailable = isWellbeingAvailable,
                    isGoogleAvailable = isGoogleAvailable,
                    editController = controller,
                )
            }
        }
    }

    @JvmStatic
    fun updateUserInfo(userName: String?, avatarBitmap: Bitmap?) {
        userNameState?.value = userName
        avatarBitmapState?.value = avatarBitmap
    }

    @JvmStatic
    fun updateWifiState(isWifiEnabled: Boolean, wifiSummary: String?) {
        wifiEnabledState?.value = isWifiEnabled
        wifiSummaryState?.value = wifiSummary
    }

    @JvmStatic
    fun updateAirplaneMode(isAirplaneModeOn: Boolean, airplaneSummary: String?) {
        airplaneModeState?.value = isAirplaneModeOn
        airplaneSummaryState?.value = airplaneSummary
    }

    @JvmStatic
    fun exitEditMode(): Boolean = editController?.discardEdits() ?: false
}
