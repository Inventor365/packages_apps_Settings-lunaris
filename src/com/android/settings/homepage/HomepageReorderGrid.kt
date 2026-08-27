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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

const val REORDER_KEY_PREFIX = "item:"

fun reorderCellKey(layoutKey: String): String = REORDER_KEY_PREFIX + layoutKey

private fun layoutKeyOf(cellKey: String): String = cellKey.removePrefix(REORDER_KEY_PREFIX)

private const val AUTOSCROLL_FACTOR = 0.18f
private const val MAX_AUTOSCROLL_PER_FRAME = 36f
private const val DRAG_SCALE = 1.03f
private val AUTOSCROLL_EDGE_MARGIN = 72.dp

class GridReorderState internal constructor(
    private val gridState: LazyGridState,
    private val edgeMarginPx: Float,
    private val onMove: (from: String, to: String) -> Unit,
) {

    var draggingKey: String? by mutableStateOf(null)
        private set

    private var initialOffset by mutableStateOf(Offset.Zero)
    private var dragDelta by mutableStateOf(Offset.Zero)
    private var draggingSize by mutableStateOf(IntSize.Zero)

    private var suppressed = false

    val isDragging: Boolean get() = draggingKey != null

    fun setSuppressed(value: Boolean) {
        suppressed = value
    }

    fun onDragStart(offset: Offset): Boolean {
        if (suppressed) return false
        val point = toItemSpace(offset)
        val item = reorderableItems().firstOrNull { it.containsPoint(point) } ?: return false
        val key = item.key as? String ?: return false
        draggingKey = key
        initialOffset = Offset(item.offset.x.toFloat(), item.offset.y.toFloat())
        draggingSize = item.size
        dragDelta = Offset.Zero
        return true
    }

    fun onDrag(delta: Offset) {
        if (draggingKey == null) return
        dragDelta += delta
        refreshTarget()
    }

    fun onDragEnd() {
        draggingKey = null
        dragDelta = Offset.Zero
        draggingSize = IntSize.Zero
    }

    fun refreshTarget() {
        val key = draggingKey ?: return
        val centre = Offset(
            initialOffset.x + dragDelta.x + draggingSize.width / 2f,
            initialOffset.y + dragDelta.y + draggingSize.height / 2f,
        )
        val target = reorderableItems()
            .firstOrNull { it.key != key && it.containsPoint(centre) } ?: return
        val targetKey = target.key as? String ?: return
        onMove(layoutKeyOf(key), layoutKeyOf(targetKey))
    }

    fun offsetFor(cellKey: String): Offset {
        if (cellKey != draggingKey) return Offset.Zero
        val live = gridState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == cellKey } ?: return Offset.Zero
        return Offset(
            initialOffset.x + dragDelta.x - live.offset.x,
            initialOffset.y + dragDelta.y - live.offset.y,
        )
    }

    fun autoScrollAmount(): Float {
        if (draggingKey == null) return 0f
        val info = gridState.layoutInfo
        val top = initialOffset.y + dragDelta.y
        val bottom = top + draggingSize.height
        val scrollStart = info.viewportStartOffset + edgeMarginPx
        val scrollEnd = info.viewportEndOffset - edgeMarginPx
        val overflow = when {
            bottom > scrollEnd -> bottom - scrollEnd
            top < scrollStart -> top - scrollStart
            else -> return 0f
        }
        return (overflow * AUTOSCROLL_FACTOR)
            .coerceIn(-MAX_AUTOSCROLL_PER_FRAME, MAX_AUTOSCROLL_PER_FRAME)
    }

    private fun toItemSpace(nodeOffset: Offset): Offset =
        Offset(nodeOffset.x, nodeOffset.y + gridState.layoutInfo.viewportStartOffset)

    private fun reorderableItems(): List<LazyGridItemInfo> =
        gridState.layoutInfo.visibleItemsInfo.filter {
            (it.key as? String)?.startsWith(REORDER_KEY_PREFIX) == true
        }
}

@Composable
fun rememberGridReorderState(
    gridState: LazyGridState,
    onMove: (from: String, to: String) -> Unit,
): GridReorderState {
    val moveHandler = rememberUpdatedState(onMove)
    val edgeMarginPx = with(LocalDensity.current) { AUTOSCROLL_EDGE_MARGIN.toPx() }
    val state = remember(gridState, edgeMarginPx) {
        GridReorderState(gridState, edgeMarginPx) { from, to -> moveHandler.value(from, to) }
    }
    val dragging = state.isDragging
    LaunchedEffect(state, dragging) {
        if (!dragging) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            val amount = state.autoScrollAmount()
            if (amount != 0f) {
                gridState.scrollBy(amount)
                state.refreshTarget()
            }
        }
    }
    return state
}

fun Modifier.homepageDragContainer(
    state: GridReorderState,
    onDragStarted: () -> Unit,
): Modifier = pointerInput(state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset -> if (state.onDragStart(offset)) onDragStarted() },
        onDrag = { change, amount ->
            if (state.isDragging) {
                change.consume()
                state.onDrag(amount)
            }
        },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() },
    )
}

fun Modifier.draggingItem(state: GridReorderState, cellKey: String): Modifier =
    if (state.draggingKey != cellKey) {
        this
    } else {
        this
            .zIndex(1f)
            .graphicsLayer {
                val offset = state.offsetFor(cellKey)
                translationX = offset.x
                translationY = offset.y
                scaleX = DRAG_SCALE
                scaleY = DRAG_SCALE
            }
    }

private fun LazyGridItemInfo.containsPoint(point: Offset): Boolean =
    point.x >= offset.x && point.x <= offset.x + size.width &&
        point.y >= offset.y && point.y <= offset.y + size.height

fun Modifier.tileResizeGesture(
    reorderState: GridReorderState,
    size: () -> TileSize,
    onResize: (TileSize) -> Unit,
    stepPx: Float,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        reorderState.setSuppressed(true)
        try {
            val startSize = size()
            var travel = Offset.Zero
            val slopChange = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                change.consume()
                travel += overSlop
            }
            if (slopChange == null) {
                onResize(startSize.next())
                return@awaitEachGesture
            }
            var applied = startSize
            drag(down.id) { change ->
                change.consume()
                travel += change.positionChange()
                val target = TileSize.of(
                    when {
                        travel.x > stepPx -> TileWidth.FULL
                        travel.x < -stepPx -> TileWidth.HALF
                        else -> startSize.width
                    },
                    when {
                        travel.y > stepPx -> TileHeight.TALL
                        travel.y < -stepPx -> TileHeight.SHORT
                        else -> startSize.height
                    },
                )
                if (target != applied) {
                    applied = target
                    onResize(target)
                }
            }
        } finally {
            reorderState.setSuppressed(false)
        }
    }
}
