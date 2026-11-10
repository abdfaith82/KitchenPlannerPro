package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.cad.EngineeringComplianceAnalyzer
import com.example.data.model.KitchenCatalogData
import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import com.example.ui.viewmodel.LightingMode
import com.example.ui.viewmodel.PlannerStudioState
import com.example.ui.viewmodel.StudioViewMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Converts a local canvas pointer Offset into exact 1mm room coordinates (desiredXMm, desiredYMm)
 * for both 2D Blueprint and 3D Isometric / Walkthrough camera modes.
 */
fun screenOffsetToRoomMillimeters(
    canvasOffset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    state: PlannerStudioState,
    unitWidthMm: Int,
    unitDepthMm: Int,
    unitElevationMm: Int = 0,
    unitHeightMm: Int = 850,
    isWallCabinet: Boolean = false
): Pair<Int, Int> {
    val safeW = canvasWidth.coerceAtLeast(100f)
    val safeH = canvasHeight.coerceAtLeast(100f)

    if (state.viewMode == StudioViewMode.PLAN_2D) {
        val paddingPx = 46f
        val availW = (safeW - paddingPx * 2).coerceAtLeast(100f)
        val availH = (safeH - paddingPx * 2).coerceAtLeast(100f)
        val scale = (min(
            availW / state.roomWidthCm.toFloat(),
            availH / state.roomDepthCm.toFloat()
        ) * state.cameraZoom).coerceAtLeast(0.05f)
        val roomPxW = state.roomWidthCm * scale
        val roomPxH = state.roomDepthCm * scale
        val originX = (safeW - roomPxW) / 2f
        val originY = (safeH - roomPxH) / 2f

        val pointerXMm = (((canvasOffset.x - originX) / scale) * 10f).roundToInt()
        val pointerYMm = (((canvasOffset.y - originY) / scale) * 10f).roundToInt()

        val maxX = (state.roomWidthMm - unitWidthMm).coerceAtLeast(0)
        val maxY = (state.roomDepthMm - unitDepthMm).coerceAtLeast(0)
        val desiredXMm = (pointerXMm - unitWidthMm / 2).coerceIn(0, maxX)
        val desiredYMm = if (isWallCabinet && pointerYMm < 900) {
            0
        } else {
            (pointerYMm - unitDepthMm / 2).coerceIn(0, maxY)
        }
        return desiredXMm to desiredYMm
    } else {
        val roomW = state.roomWidthCm.toFloat()
        val roomD = state.roomDepthCm.toFloat()
        val isFp = state.viewMode == StudioViewMode.WALKTHROUGH_FP ||
            state.viewMode == StudioViewMode.VR_CAMERA_WALKTHROUGH
        val baseScale = (min(safeW / (roomW * 1.45f), safeH / (roomD * 1.35f)) * state.cameraZoom)
            .coerceAtLeast(0.05f)
        val centerX = safeW / 2f
        val pitch = state.cameraPitchFactor.coerceIn(0.14f, 0.88f)
        val centerY = if (isFp) {
            safeH * (0.38f + pitch * 0.52f).coerceIn(0.32f, 0.74f)
        } else {
            safeH * 0.60f
        }

        val zMidCm = (unitElevationMm + unitHeightMm * 0.35f) / 10f
        val eyeRefCm = if (isFp) (state.vrEyeHeightCm - 40f).coerceIn(75f, 155f) else 0f
        val verticalScale = if (isFp) {
            0.88f
        } else {
            (1.05f - pitch * 0.45f).coerceIn(0.45f, 0.92f)
        }
        val zOffsetPx = if (isFp) {
            (zMidCm - eyeRefCm) * verticalScale * baseScale
        } else {
            zMidCm * verticalScale * baseScale
        }

        val rx = (canvasOffset.x - centerX) / baseScale + (if (isFp) state.vrStrafeLateralCm else 0f)
        val ry = (canvasOffset.y - centerY + zOffsetPx) / (pitch * baseScale).coerceAtLeast(0.05f) -
            (if (isFp) state.firstPersonStepForwardCm else 0f)

        val rad = Math.toRadians(state.cameraYawDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        val cx = rx * cosA + ry * sinA
        val cy = -rx * sinA + ry * cosA

        val pointerXMm = ((cx + roomW / 2f) * 10f).roundToInt()
        val pointerYMm = ((cy + roomD / 2f) * 10f).roundToInt()

        val maxX = (state.roomWidthMm - unitWidthMm).coerceAtLeast(0)
        val maxY = (state.roomDepthMm - unitDepthMm).coerceAtLeast(0)
        val desiredXMm = (pointerXMm - unitWidthMm / 2).coerceIn(0, maxX)
        val desiredYMm = if (isWallCabinet && pointerYMm < 1100) {
            0
        } else {
            (pointerYMm - unitDepthMm / 2).coerceIn(0, maxY)
        }
        return desiredXMm to desiredYMm
    }
}

@Composable
fun KitchenInteractiveCanvas(
    state: PlannerStudioState,
    onSelectItem: (String?) -> Unit,
    onMoveItemDirect: (String, Int, Int) -> Unit,
    onOrbitCamera: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    onMoveItemDirectMm: (String, Int, Int) -> Unit = { uid, xMm, yMm ->
        onMoveItemDirect(uid, xMm / 10, yMm / 10)
    },
    onResizeItemDirectMm: (String, Int, Int) -> Unit = { _, _, _ -> },
    onBeginItemDrag: () -> Unit = {},
    previewDragItem: PlacedKitchenItem? = null
) {
    // Smoothly interpolate scene camera rotation, pitch, and zoom for fluid scene transitions
    val smoothYawDeg by animateFloatAsState(
        targetValue = state.cameraYawDeg,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f),
        label = "smoothYawDeg"
    )
    val smoothPitchFactor by animateFloatAsState(
        targetValue = state.cameraPitchFactor,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f),
        label = "smoothPitchFactor"
    )
    val smoothCameraZoom by animateFloatAsState(
        targetValue = state.cameraZoom,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 460f),
        label = "smoothCameraZoom"
    )
    val smoothWalkForwardCm by animateFloatAsState(
        targetValue = state.firstPersonStepForwardCm,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "smoothWalkForwardCm"
    )
    val smoothWalkStrafeCm by animateFloatAsState(
        targetValue = state.vrStrafeLateralCm,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "smoothWalkStrafeCm"
    )
    val smoothEyeHeightCm by animateFloatAsState(
        targetValue = state.vrEyeHeightCm,
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 420f),
        label = "smoothEyeHeightCm"
    )

    val effectiveRenderState = remember(
        state,
        smoothYawDeg,
        smoothPitchFactor,
        smoothCameraZoom,
        smoothWalkForwardCm,
        smoothWalkStrafeCm,
        smoothEyeHeightCm
    ) {
        state.copy(
            cameraYawDeg = smoothYawDeg,
            cameraPitchFactor = smoothPitchFactor,
            cameraZoom = smoothCameraZoom,
            firstPersonStepForwardCm = smoothWalkForwardCm,
            vrStrafeLateralCm = smoothWalkStrafeCm,
            vrEyeHeightCm = smoothEyeHeightCm
        )
    }
    val currentState by rememberUpdatedState(effectiveRenderState)

    // Continuous drag tracking from gesture start to prevent magnetic-snap sticking or jumping
    var dragTotalDeltaXMm by remember { mutableFloatStateOf(0f) }
    var dragTotalDeltaYMm by remember { mutableFloatStateOf(0f) }
    var dragStartItemXMm by remember { mutableIntStateOf(0) }
    var dragStartItemYMm by remember { mutableIntStateOf(0) }
    var dragStartWidthMm by remember { mutableIntStateOf(600) }
    var dragStartDepthMm by remember { mutableIntStateOf(600) }
    var activeDraggedItemUid by remember { mutableStateOf<String?>(null) }
    var isDragging3DItemDirectly by remember { mutableStateOf(false) }
    var grabOffset3DXMm by remember { mutableIntStateOf(0) }
    var grabOffset3DYMm by remember { mutableIntStateOf(0) }
    // 2D Touch Resize Mode: "NONE", "WIDTH_RIGHT", "DEPTH_BOTTOM", "BOTH_CORNER"
    var active2DResizeHandle by remember { mutableStateOf("NONE") }

    val sim = state.lightingSimulation
    val bgBrush = when {
        state.lightingMode == LightingMode.BLUEPRINT_WIRE -> Brush.verticalGradient(
            listOf(Color(0xFF12283A), Color(0xFF0C1B28))
        )
        state.lightingMode == LightingMode.WARM_LED || sim.naturalLightIntensity < 0.28f -> Brush.verticalGradient(
            listOf(Color(0xFF0B1013), Color(0xFF162025), Color(0xFF0D1316))
        )
        sim.colorTemperatureKelvin <= 3300 -> Brush.verticalGradient(
            listOf(Color(0xFFE6D5B8), Color(0xFFF7EFE0), Color(0xFFE5D2B5))
        )
        else -> Brush.verticalGradient(
            listOf(Color(0xFFDDD6C7), Color(0xFFF3EFE6), Color(0xFFE0D7C3))
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bgBrush)
            .testTag("kitchen_interactive_canvas")
            .pointerInput(state.viewMode) {
                detectTapGestures { tapOffset ->
                    val s = currentState
                    if (s.viewMode == StudioViewMode.PLAN_2D) {
                        val paddingPx = 46f
                        val availW = (size.width - paddingPx * 2).coerceAtLeast(100f)
                        val availH = (size.height - paddingPx * 2).coerceAtLeast(100f)
                        val scale = min(
                            availW / s.roomWidthCm.toFloat(),
                            availH / s.roomDepthCm.toFloat()
                        ) * s.cameraZoom
                        val originX = (size.width - s.roomWidthCm * scale) / 2f
                        val originY = (size.height - s.roomDepthCm * scale) / 2f

                        val tapXCm = (tapOffset.x - originX) / scale
                        val tapYCm = (tapOffset.y - originY) / scale

                        val hit = s.placedItems.lastOrNull { item ->
                            tapXCm >= item.xMm / 10f &&
                                tapXCm <= (item.xMm + item.effectiveWidthMm) / 10f &&
                                tapYCm >= item.yMm / 10f &&
                                tapYCm <= (item.yMm + item.effectiveDepthMm) / 10f
                        }
                        onSelectItem(hit?.uid)
                    } else {
                        val hit = findClosest3DItemWithinRadius(
                            tapOffset = tapOffset,
                            canvasWidth = size.width.toFloat(),
                            canvasHeight = size.height.toFloat(),
                            state = s,
                            maxRadiusPx = 110f
                        ) ?: findClosest3DItem(
                            tapOffset = tapOffset,
                            canvasWidth = size.width.toFloat(),
                            canvasHeight = size.height.toFloat(),
                            state = s
                        )
                        if (hit != null) {
                            onSelectItem(hit.uid)
                        }
                    }
                }
            }
            .pointerInput(state.viewMode) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        dragTotalDeltaXMm = 0f
                        dragTotalDeltaYMm = 0f
                        activeDraggedItemUid = null
                        isDragging3DItemDirectly = false
                        active2DResizeHandle = "NONE"
                        val s = currentState
                        if (s.viewMode == StudioViewMode.PLAN_2D) {
                            val paddingPx = 46f
                            val availW = (size.width - paddingPx * 2).coerceAtLeast(100f)
                            val availH = (size.height - paddingPx * 2).coerceAtLeast(100f)
                            val scale = min(
                                availW / s.roomWidthCm.toFloat(),
                                availH / s.roomDepthCm.toFloat()
                            ) * s.cameraZoom
                            val originX = (size.width - s.roomWidthCm * scale) / 2f
                            val originY = (size.height - s.roomDepthCm * scale) / 2f

                            // 1. Check if user grabbed one of the 3 Touch Resize Handles on the selected item
                            val currentlySelected = s.placedItems.firstOrNull { it.uid == s.selectedItemUid }
                            if (currentlySelected != null) {
                                val leftPx = originX + (currentlySelected.xMm / 10f) * scale
                                val topPx = originY + (currentlySelected.yMm / 10f) * scale
                                val wPx = (currentlySelected.effectiveWidthMm / 10f) * scale
                                val hPx = (currentlySelected.effectiveDepthMm / 10f) * scale

                                val rightHandleCenter = Offset(leftPx + wPx, topPx + hPx / 2f)
                                val bottomHandleCenter = Offset(leftPx + wPx / 2f, topPx + hPx)
                                val cornerHandleCenter = Offset(leftPx + wPx, topPx + hPx)
                                val handleHitRadius = 34f

                                when {
                                    hypot(startOffset.x - cornerHandleCenter.x, startOffset.y - cornerHandleCenter.y) <= handleHitRadius -> {
                                        active2DResizeHandle = "BOTH_CORNER"
                                        activeDraggedItemUid = currentlySelected.uid
                                        dragStartWidthMm = currentlySelected.widthMm
                                        dragStartDepthMm = currentlySelected.depthMm
                                        onBeginItemDrag()
                                        return@detectDragGestures
                                    }
                                    hypot(startOffset.x - rightHandleCenter.x, startOffset.y - rightHandleCenter.y) <= handleHitRadius -> {
                                        active2DResizeHandle = "WIDTH_RIGHT"
                                        activeDraggedItemUid = currentlySelected.uid
                                        dragStartWidthMm = currentlySelected.widthMm
                                        dragStartDepthMm = currentlySelected.depthMm
                                        onBeginItemDrag()
                                        return@detectDragGestures
                                    }
                                    hypot(startOffset.x - bottomHandleCenter.x, startOffset.y - bottomHandleCenter.y) <= handleHitRadius -> {
                                        active2DResizeHandle = "DEPTH_BOTTOM"
                                        activeDraggedItemUid = currentlySelected.uid
                                        dragStartWidthMm = currentlySelected.widthMm
                                        dragStartDepthMm = currentlySelected.depthMm
                                        onBeginItemDrag()
                                        return@detectDragGestures
                                    }
                                }
                            }

                            val tapXCm = (startOffset.x - originX) / scale
                            val tapYCm = (startOffset.y - originY) / scale
                            val hit = s.placedItems.lastOrNull { item ->
                                tapXCm >= item.xMm / 10f &&
                                    tapXCm <= (item.xMm + item.effectiveWidthMm) / 10f &&
                                    tapYCm >= item.yMm / 10f &&
                                    tapYCm <= (item.yMm + item.effectiveDepthMm) / 10f
                            } ?: currentlySelected

                            if (hit != null) {
                                onSelectItem(hit.uid)
                                activeDraggedItemUid = hit.uid
                                dragStartItemXMm = hit.xMm
                                dragStartItemYMm = hit.yMm
                                dragStartWidthMm = hit.widthMm
                                dragStartDepthMm = hit.depthMm
                                onBeginItemDrag()
                            }
                        } else {
                            // In 3D / Walkthrough: allow touching ANY 3D module within radius to immediately select and drag smoothly
                            val hit3D = findClosest3DItemWithinRadius(
                                tapOffset = startOffset,
                                canvasWidth = size.width.toFloat(),
                                canvasHeight = size.height.toFloat(),
                                state = s,
                                maxRadiusPx = 78f
                            )
                            if (hit3D != null) {
                                onSelectItem(hit3D.uid)
                                activeDraggedItemUid = hit3D.uid
                                isDragging3DItemDirectly = true
                                onBeginItemDrag()
                                val (pointerXMm, pointerYMm) = screenOffsetToRoomMillimeters(
                                    canvasOffset = startOffset,
                                    canvasWidth = size.width.toFloat(),
                                    canvasHeight = size.height.toFloat(),
                                    state = s,
                                    unitWidthMm = hit3D.effectiveWidthMm,
                                    unitDepthMm = hit3D.effectiveDepthMm,
                                    unitElevationMm = hit3D.elevationMm,
                                    unitHeightMm = hit3D.heightMm,
                                    isWallCabinet = hit3D.type.category == ModuleCategory.WALL_CABINET
                                )
                                grabOffset3DXMm = (pointerXMm - hit3D.xMm).coerceIn(-600, 600)
                                grabOffset3DYMm = (pointerYMm - hit3D.yMm).coerceIn(-600, 600)
                            }
                        }
                    },
                    onDragEnd = {
                        activeDraggedItemUid = null
                        isDragging3DItemDirectly = false
                        active2DResizeHandle = "NONE"
                    },
                    onDragCancel = {
                        activeDraggedItemUid = null
                        isDragging3DItemDirectly = false
                        active2DResizeHandle = "NONE"
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val s = currentState
                        if (s.viewMode == StudioViewMode.PLAN_2D) {
                            val selectedId = activeDraggedItemUid ?: s.selectedItemUid ?: return@detectDragGestures
                            val selectedItem = s.placedItems.firstOrNull { it.uid == selectedId }
                                ?: return@detectDragGestures
                            val paddingPx = 46f
                            val availW = (size.width - paddingPx * 2).coerceAtLeast(100f)
                            val availH = (size.height - paddingPx * 2).coerceAtLeast(100f)
                            val scaleMm = (min(
                                availW / s.roomWidthCm.toFloat(),
                                availH / s.roomDepthCm.toFloat()
                            ) * s.cameraZoom) / 10f

                            dragTotalDeltaXMm += dragAmount.x / scaleMm.coerceAtLeast(0.01f)
                            dragTotalDeltaYMm += dragAmount.y / scaleMm.coerceAtLeast(0.01f)

                            val totalStepXMm = dragTotalDeltaXMm.roundToInt()
                            val totalStepYMm = dragTotalDeltaYMm.roundToInt()

                            when (active2DResizeHandle) {
                                "WIDTH_RIGHT" -> {
                                    val isRotated = selectedItem.rotationDeg % 180 != 0
                                    val targetW = if (isRotated) dragStartWidthMm else (dragStartWidthMm + totalStepXMm)
                                    val targetD = if (isRotated) (dragStartDepthMm + totalStepXMm) else dragStartDepthMm
                                    onResizeItemDirectMm(selectedId, targetW, targetD)
                                }
                                "DEPTH_BOTTOM" -> {
                                    val isRotated = selectedItem.rotationDeg % 180 != 0
                                    val targetW = if (isRotated) (dragStartWidthMm + totalStepYMm) else dragStartWidthMm
                                    val targetD = if (isRotated) dragStartDepthMm else (dragStartDepthMm + totalStepYMm)
                                    onResizeItemDirectMm(selectedId, targetW, targetD)
                                }
                                "BOTH_CORNER" -> {
                                    val isRotated = selectedItem.rotationDeg % 180 != 0
                                    val targetW = if (isRotated) (dragStartWidthMm + totalStepYMm) else (dragStartWidthMm + totalStepXMm)
                                    val targetD = if (isRotated) (dragStartDepthMm + totalStepXMm) else (dragStartDepthMm + totalStepYMm)
                                    onResizeItemDirectMm(selectedId, targetW, targetD)
                                }
                                else -> {
                                    onMoveItemDirectMm(
                                        selectedId,
                                        dragStartItemXMm + totalStepXMm,
                                        dragStartItemYMm + totalStepYMm
                                    )
                                }
                            }
                        } else if (isDragging3DItemDirectly && (activeDraggedItemUid != null || s.selectedItemUid != null)) {
                            val targetUid = activeDraggedItemUid ?: s.selectedItemUid
                            val selectedItem = s.placedItems.firstOrNull { it.uid == targetUid }
                            if (selectedItem != null) {
                                val (rawTargetXMm, rawTargetYMm) = screenOffsetToRoomMillimeters(
                                    canvasOffset = change.position,
                                    canvasWidth = size.width.toFloat(),
                                    canvasHeight = size.height.toFloat(),
                                    state = s,
                                    unitWidthMm = selectedItem.effectiveWidthMm,
                                    unitDepthMm = selectedItem.effectiveDepthMm,
                                    unitElevationMm = selectedItem.elevationMm,
                                    unitHeightMm = selectedItem.heightMm,
                                    isWallCabinet = selectedItem.type.category == ModuleCategory.WALL_CABINET
                                )
                                val maxX = (s.roomWidthMm - selectedItem.effectiveWidthMm).coerceAtLeast(0)
                                val maxY = (s.roomDepthMm - selectedItem.effectiveDepthMm).coerceAtLeast(0)
                                val smoothTargetX = (rawTargetXMm - grabOffset3DXMm).coerceIn(0, maxX)
                                val smoothTargetY = (rawTargetYMm - grabOffset3DYMm).coerceIn(0, maxY)
                                onMoveItemDirectMm(selectedItem.uid, smoothTargetX, smoothTargetY)
                            }
                        } else {
                            // Smooth proportional 3D scene camera orbit & pitch
                            onOrbitCamera(dragAmount.x * 0.30f, dragAmount.y * 0.0016f)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (effectiveRenderState.viewMode) {
                StudioViewMode.PLAN_2D -> draw2DBlueprint(effectiveRenderState, previewDragItem)
                StudioViewMode.RENDER_3D, StudioViewMode.WALKTHROUGH_FP ->
                    draw3DPhotorealisticScene(effectiveRenderState, previewDragItem)
                StudioViewMode.VR_CAMERA_WALKTHROUGH -> {
                    if (effectiveRenderState.vrStereoscopicSplitEnabled) {
                        val halfW = size.width / 2f
                        // Left Eye VR Viewport (-3.4cm interpupillary offset)
                        clipRect(left = 0f, top = 0f, right = halfW - 2f, bottom = size.height) {
                            draw3DPhotorealisticScene(
                                state = effectiveRenderState.copy(
                                    vrStrafeLateralCm = effectiveRenderState.vrStrafeLateralCm - 3.4f,
                                    cameraZoom = effectiveRenderState.cameraZoom * 0.86f
                                ),
                                previewDragItem = previewDragItem,
                                customCenterX = halfW * 0.5f
                            )
                        }
                        // Right Eye VR Viewport (+3.4cm interpupillary offset)
                        clipRect(left = halfW + 2f, top = 0f, right = size.width, bottom = size.height) {
                            draw3DPhotorealisticScene(
                                state = effectiveRenderState.copy(
                                    vrStrafeLateralCm = effectiveRenderState.vrStrafeLateralCm + 3.4f,
                                    cameraZoom = effectiveRenderState.cameraZoom * 0.86f
                                ),
                                previewDragItem = previewDragItem,
                                customCenterX = halfW * 1.5f
                            )
                        }
                        drawVrNavigationHudOverlay(effectiveRenderState, isStereoscopic = true)
                    } else {
                        draw3DPhotorealisticScene(effectiveRenderState, previewDragItem)
                        drawVrNavigationHudOverlay(effectiveRenderState, isStereoscopic = false)
                    }
                }
            }
        }
    }
}

private fun DrawScope.draw2DBlueprint(
    state: PlannerStudioState,
    previewDragItem: PlacedKitchenItem? = null
) {
    val isDarkWire = state.lightingMode != LightingMode.DAYLIGHT
    val paddingPx = 46f
    val availW = (size.width - paddingPx * 2).coerceAtLeast(100f)
    val availH = (size.height - paddingPx * 2).coerceAtLeast(100f)
    val scale = min(
        availW / state.roomWidthCm.toFloat(),
        availH / state.roomDepthCm.toFloat()
    ) * state.cameraZoom

    val roomPxW = state.roomWidthCm * scale
    val roomPxH = state.roomDepthCm * scale
    val originX = (size.width - roomPxW) / 2f
    val originY = (size.height - roomPxH) / 2f

    val floorColor = if (state.lightingMode == LightingMode.BLUEPRINT_WIRE) {
        Color(0xFF16324A)
    } else if (isDarkWire) {
        Color(0xFF222C2F)
    } else {
        Color(0xFFFAF7F0)
    }
    drawRect(
        color = floorColor,
        topLeft = Offset(originX, originY),
        size = Size(roomPxW, roomPxH)
    )

    val gridMinorColor = if (isDarkWire) Color.White.copy(alpha = 0.07f) else Color(0xFF1E292B).copy(alpha = 0.07f)
    val gridMajorColor = if (isDarkWire) Color.White.copy(alpha = 0.16f) else Color(0xFF1E292B).copy(alpha = 0.16f)

    var xCm = 0
    while (xCm <= state.roomWidthCm) {
        val xPx = originX + xCm * scale
        val isMajor = xCm % 100 == 0
        drawLine(
            color = if (isMajor) gridMajorColor else gridMinorColor,
            start = Offset(xPx, originY),
            end = Offset(xPx, originY + roomPxH),
            strokeWidth = if (isMajor) 1.8f else 0.9f
        )
        xCm += 20
    }

    var yCm = 0
    while (yCm <= state.roomDepthCm) {
        val yPx = originY + yCm * scale
        val isMajor = yCm % 100 == 0
        drawLine(
            color = if (isMajor) gridMajorColor else gridMinorColor,
            start = Offset(originX, yPx),
            end = Offset(originX + roomPxW, yPx),
            strokeWidth = if (isMajor) 1.8f else 0.9f
        )
        yCm += 20
    }

    val wallStrokeColor = if (isDarkWire) Color(0xFF8AC6D0) else Color(0xFF1E292B)
    drawRect(
        color = wallStrokeColor,
        topLeft = Offset(originX, originY),
        size = Size(roomPxW, roomPxH),
        style = Stroke(width = 7f)
    )

    val labelPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = if (isDarkWire) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1E292B")
    }

    if (state.showDimensions) {
        val unitSys = state.unitSystem
        drawContext.canvas.nativeCanvas.drawText(
            "العرض: ${unitSys.formatMillimetersVerboseAr(state.roomWidthMm)}",
            originX + roomPxW / 2f,
            (originY - 14f).coerceAtLeast(24f),
            labelPaint
        )
        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(
            -90f,
            (originX - 16f).coerceAtLeast(18f),
            originY + roomPxH / 2f
        )
        drawContext.canvas.nativeCanvas.drawText(
            "العمق: ${unitSys.formatMillimetersVerboseAr(state.roomDepthMm)}",
            (originX - 16f).coerceAtLeast(18f),
            originY + roomPxH / 2f,
            labelPaint
        )
        drawContext.canvas.nativeCanvas.restore()
    }

    val violatingUids = state.complianceReport.violatingUnitUids

    val sortedItems = state.placedItems.sortedBy {
        if (it.type.category == ModuleCategory.WALL_CABINET) 1 else 0
    }

    sortedItems.forEach { item ->
        val left = originX + (item.xMm / 10f) * scale
        val top = originY + (item.yMm / 10f) * scale
        val w = (item.effectiveWidthMm / 10f) * scale
        val h = (item.effectiveDepthMm / 10f) * scale
        val isSelected = item.uid == state.selectedItemUid
        val isWallCab = item.type.category == ModuleCategory.WALL_CABINET
        val unitAlerts = state.complianceReport.alerts.filter {
            it.affectedUnitUid == item.uid || it.secondaryUnitUid == item.uid
        }
        val hasCriticalAlert = unitAlerts.any { it.isCritical }
        val hasWarningAlert = item.uid in violatingUids || unitAlerts.isNotEmpty()

        // 0. Draw Door Swing / Drawer Pull-Out Clearance Zone in 2D Plan for non-wall units
        if (!isWallCab && item.type != ModuleType.FILLER_STRIP && (state.doorsOpenPreview || isSelected || hasWarningAlert)) {
            val swingDepthCm = when (item.type) {
                ModuleType.DISHWASHER, ModuleType.STOVE_HOB, ModuleType.TALL_OVEN_TOWER -> 62f
                ModuleType.FRIDGE_SMART -> 68f
                else -> 52f
            }
            val swingPx = swingDepthCm * scale
            val swingColor = when {
                hasCriticalAlert -> Color(0xFFFF1744)
                hasWarningAlert -> Color(0xFFFF9100)
                else -> Color(0xFF00B0FF)
            }
            drawRoundRect(
                color = swingColor.copy(alpha = if (hasWarningAlert) 0.16f else 0.08f),
                topLeft = Offset(left + 2f, top + h),
                size = Size((w - 4f).coerceAtLeast(8f), swingPx.coerceAtMost(originY + roomPxH - (top + h)).coerceAtLeast(8f)),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = swingColor.copy(alpha = 0.65f),
                topLeft = Offset(left + 2f, top + h),
                size = Size((w - 4f).coerceAtLeast(8f), swingPx.coerceAtMost(originY + roomPxH - (top + h)).coerceAtLeast(8f)),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(
                    width = if (hasWarningAlert) 2.5f else 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
            )
        }

        val fillColor = when (state.lightingMode) {
            LightingMode.BLUEPRINT_WIRE -> Color(0xFF1E496D).copy(alpha = if (isWallCab) 0.55f else 0.85f)
            else -> Color(item.type.colorAccentHex).copy(alpha = if (isWallCab) 0.68f else 0.92f)
        }

        drawRoundRect(
            color = fillColor,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(6f, 6f)
        )

        when (item.type) {
            ModuleType.SINK_DOUBLE -> {
                val pad = min(w, h) * 0.16f
                val basinW = (w - pad * 3) / 2f
                val basinH = (h - pad * 2).coerceAtLeast(6f)
                if (basinW > 4f) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(left + pad, top + pad),
                        size = Size(basinW, basinH),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 2.2f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(left + pad * 2 + basinW, top + pad),
                        size = Size(basinW, basinH),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 2.2f)
                    )
                }
            }
            ModuleType.STOVE_HOB -> {
                val r = min(w, h) * 0.14f
                val cx1 = left + w * 0.32f
                val cx2 = left + w * 0.68f
                val cy1 = top + h * 0.33f
                val cy2 = top + h * 0.67f
                listOf(
                    Offset(cx1, cy1),
                    Offset(cx2, cy1),
                    Offset(cx1, cy2),
                    Offset(cx2, cy2)
                ).forEach { center ->
                    drawCircle(
                        color = Color(0xFFFFB74D),
                        radius = r,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                }
            }
            ModuleType.FRIDGE_SMART -> {
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(left + 4f, top + h * 0.82f),
                    end = Offset(left + w - 4f, top + h * 0.82f),
                    strokeWidth = 2.5f
                )
            }
            ModuleType.CORNER_MAGIC,
            ModuleType.CORNER_BASE_CAROUSEL_90,
            ModuleType.CORNER_BASE_DIAGONAL_105,
            ModuleType.CORNER_BASE_BLIND_110,
            ModuleType.CORNER_WALL_L_65,
            ModuleType.CORNER_WALL_DIAGONAL_60 -> {
                // Draw L-corner / diagonal turntable symbol in 2D blueprint
                val cRadius = min(w, h) * 0.32f
                drawCircle(
                    color = Color.White.copy(alpha = 0.78f),
                    radius = cRadius,
                    center = Offset(left + w / 2f, top + h / 2f),
                    style = Stroke(
                        width = 2.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                    )
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.75f),
                    start = Offset(left + w * 0.15f, top + h * 0.85f),
                    end = Offset(left + w * 0.85f, top + h * 0.15f),
                    strokeWidth = 2.0f
                )
            }
            else -> {}
        }

        // Draw 18mm Left/Right Side Panels & Full Back Panel indicators in 2D CAD Blueprint
        if (!isWallCab || item.hasFullBackPanel) {
            val panelThickPx = (1.8f * scale).coerceIn(2f, 6f)
            if (item.hasFullBackPanel && item.type != ModuleType.FILLER_STRIP) {
                drawRect(
                    color = Color.White.copy(alpha = 0.48f),
                    topLeft = Offset(left + 1f, top + 1f),
                    size = Size((w - 2f).coerceAtLeast(4f), panelThickPx)
                )
            }
            if (item.hasFullSidePanels && item.type != ModuleType.FILLER_STRIP) {
                drawRect(
                    color = Color.White.copy(alpha = 0.42f),
                    topLeft = Offset(left + 1f, top + 1f),
                    size = Size(panelThickPx, (h - 2f).coerceAtLeast(4f))
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.42f),
                    topLeft = Offset((left + w - panelThickPx - 1f).coerceAtLeast(left), top + 1f),
                    size = Size(panelThickPx, (h - 2f).coerceAtLeast(4f))
                )
            }
        }

        val borderStroke = if (hasWarningAlert) {
            Stroke(width = 5.5f)
        } else if (isSelected) {
            Stroke(width = 5f)
        } else if (isWallCab) {
            Stroke(width = 2.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
        } else {
            Stroke(width = 2f)
        }

        val borderColor = when {
            hasWarningAlert -> Color(0xFFFF1744)
            isSelected -> Color(0xFFFFD54F)
            else -> Color.White.copy(alpha = 0.85f)
        }

        drawRoundRect(
            color = borderColor,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(6f, 6f),
            style = borderStroke
        )

        // Real-Time Red Bounding Box Feedback around any cabinet violating intersection or minimum distance rules
        if (hasWarningAlert) {
            // Translucent Red Warning Overlay inside the cabinet
            drawRoundRect(
                color = Color(0xFFFF1744).copy(alpha = 0.24f),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Primary Solid Red Bounding Box surrounding the cabinet
            drawRect(
                color = Color(0xFFFF1744),
                topLeft = Offset(left - 6f, top - 6f),
                size = Size(w + 12f, h + 12f),
                style = Stroke(width = 4.2f)
            )
            // Outer Dashed Red Safety Halo Box
            drawRect(
                color = Color(0xFFD50000).copy(alpha = 0.85f),
                topLeft = Offset(left - 11f, top - 11f),
                size = Size(w + 22f, h + 22f),
                style = Stroke(
                    width = 2.4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            )
            val badgeCenter = Offset(left + w - 10f, top + 10f)
            drawCircle(
                color = Color(0xFFD50000),
                radius = 12f,
                center = badgeCenter
            )
            val alertPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.WHITE
            }
            drawContext.canvas.nativeCanvas.drawText("!", badgeCenter.x, badgeCenter.y + 6f, alertPaint)
        }

        val itemPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = (min(w, h) * 0.24f).coerceIn(14f, 22f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.WHITE
        }
        val shortLabel = item.customLabel.split(" ").take(2).joinToString(" ")
        drawContext.canvas.nativeCanvas.drawText(
            shortLabel,
            left + w / 2f,
            top + h / 2f,
            itemPaint
        )
        if (state.showDimensions && h > 30f) {
            itemPaint.textSize = (min(w, h) * 0.19f).coerceIn(12f, 18f)
            itemPaint.color = android.graphics.Color.parseColor("#FFE082")
            val dimLabel = "${state.unitSystem.formatMillimeters(item.widthMm)} × ${state.unitSystem.formatMillimeters(item.depthMm)}"
            drawContext.canvas.nativeCanvas.drawText(
                dimLabel,
                left + w / 2f,
                top + h / 2f + 16f,
                itemPaint
            )
        }

        // Draw Interactive Touch Resize Handles on Selected Item in 2D Canvas
        if (isSelected) {
            val rightHandle = Offset(left + w, top + h / 2f)
            val bottomHandle = Offset(left + w / 2f, top + h)
            val cornerHandle = Offset(left + w, top + h)
            val handlePaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#00251A")
            }
            listOf(
                rightHandle to "↔",
                bottomHandle to "↕",
                cornerHandle to "⤡"
            ).forEach { (pt, symbol) ->
                drawCircle(color = Color(0xFF00E5FF), radius = 13f, center = pt)
                drawCircle(color = Color.White, radius = 13f, center = pt, style = Stroke(width = 2.5f))
                drawContext.canvas.nativeCanvas.drawText(symbol, pt.x, pt.y + 5f, handlePaint)
            }
        }
    }

    // Interactive Virtual Ruler & Laser Wall/Neighbor Distance Callouts for Selected Item
    val selectedClearances = state.selectedUnitClearanceDistances
    val selectedUnit = state.placedItems.firstOrNull { it.uid == state.selectedItemUid }
    if (selectedUnit != null && selectedClearances != null && (state.showVirtualRuler || state.showDimensions)) {
        val left = originX + (selectedUnit.xMm / 10f) * scale
        val top = originY + (selectedUnit.yMm / 10f) * scale
        val w = (selectedUnit.effectiveWidthMm / 10f) * scale
        val h = (selectedUnit.effectiveDepthMm / 10f) * scale
        val midX = left + w / 2f
        val midY = top + h / 2f
        val unitSys = state.unitSystem

        val rulerPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.parseColor("#004D40")
        }

        // 1. Left Wall / Left Neighbor Distance Line
        if (selectedClearances.leftWallMm > 0) {
            drawLine(
                color = Color(0xFF00E676),
                start = Offset(originX, midY),
                end = Offset(left, midY),
                strokeWidth = 2.6f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
            )
            val leftNeighborGapMm = selectedClearances.nearestLeftUnitGapMm
            val leftLabel = if (leftNeighborGapMm != null &&
                leftNeighborGapMm < selectedClearances.leftWallMm
            ) {
                "جار يسار: ${unitSys.formatMillimeters(leftNeighborGapMm)}"
            } else {
                "جدار يسار: ${unitSys.formatMillimeters(selectedClearances.leftWallMm)}"
            }
            drawDistanceBadge(
                center = Offset((originX + left) / 2f, (midY - 18f).coerceAtLeast(originY + 18f)),
                text = leftLabel,
                paint = rulerPaint
            )
        }

        // 2. Right Wall / Right Neighbor Distance Line
        if (selectedClearances.rightWallMm > 0) {
            drawLine(
                color = Color(0xFF00E676),
                start = Offset(left + w, midY),
                end = Offset(originX + roomPxW, midY),
                strokeWidth = 2.6f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
            )
            val rightNeighborGapMm = selectedClearances.nearestRightUnitGapMm
            val rightLabel = if (rightNeighborGapMm != null &&
                rightNeighborGapMm < selectedClearances.rightWallMm
            ) {
                "جار يمين: ${unitSys.formatMillimeters(rightNeighborGapMm)}"
            } else {
                "جدار يمين: ${unitSys.formatMillimeters(selectedClearances.rightWallMm)}"
            }
            drawDistanceBadge(
                center = Offset((left + w + originX + roomPxW) / 2f, (midY + 18f).coerceAtMost(originY + roomPxH - 18f)),
                text = rightLabel,
                paint = rulerPaint
            )
        }

        // 3. Back Wall & Front Wall Virtual Ruler Laser Lines (when Virtual Ruler is active)
        if (state.showVirtualRuler) {
            if (selectedClearances.backWallMm > 0) {
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(midX, originY),
                    end = Offset(midX, top),
                    strokeWidth = 2.4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                )
                drawDistanceBadge(
                    center = Offset(midX, (originY + top) / 2f),
                    text = "خلف: ${unitSys.formatMillimeters(selectedClearances.backWallMm)}",
                    paint = rulerPaint
                )
            }
            if (selectedClearances.frontWallMm > 0) {
                drawLine(
                    color = Color(0xFF00E5FF),
                    start = Offset(midX, top + h),
                    end = Offset(midX, originY + roomPxH),
                    strokeWidth = 2.4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                )
                drawDistanceBadge(
                    center = Offset(midX, (top + h + originY + roomPxH) / 2f),
                    text = "ممر أمامي: ${unitSys.formatMillimeters(selectedClearances.frontWallMm)}",
                    paint = rulerPaint
                )
            }
        }
    }

    if (state.showWorkTriangle) {
        val sink = state.placedItems.firstOrNull { it.type == ModuleType.SINK_DOUBLE }
        val stove = state.placedItems.firstOrNull {
            it.type == ModuleType.STOVE_HOB || it.type == ModuleType.TALL_OVEN_TOWER
        }
        val fridge = state.placedItems.firstOrNull { it.type == ModuleType.FRIDGE_SMART }

        if (sink != null && stove != null && fridge != null) {
            val pSink = Offset(originX + sink.centerXCm * scale, originY + sink.centerYCm * scale)
            val pStove = Offset(originX + stove.centerXCm * scale, originY + stove.centerYCm * scale)
            val pFridge = Offset(originX + fridge.centerXCm * scale, originY + fridge.centerYCm * scale)

            val triPath = Path().apply {
                moveTo(pSink.x, pSink.y)
                lineTo(pStove.x, pStove.y)
                lineTo(pFridge.x, pFridge.y)
                close()
            }
            drawPath(
                path = triPath,
                color = Color(0xFF00E676).copy(alpha = 0.14f)
            )
            drawPath(
                path = triPath,
                color = Color(0xFF00C853),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f))
                )
            )

            listOf(pSink, pStove, pFridge).forEach { pt ->
                drawCircle(color = Color(0xFF00E676), radius = 9f, center = pt)
                drawCircle(color = Color(0xFF1E292B), radius = 4f, center = pt)
            }

            val distPaint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#004D40")
            }
            val analysis = state.workTriangle
            val unitSys = state.unitSystem
            drawDistanceBadge((pSink + pStove) / 2f, unitSys.formatMillimeters(analysis.sinkToStoveCm * 10), distPaint)
            drawDistanceBadge((pStove + pFridge) / 2f, unitSys.formatMillimeters(analysis.stoveToFridgeCm * 10), distPaint)
            drawDistanceBadge((pFridge + pSink) / 2f, unitSys.formatMillimeters(analysis.fridgeToSinkCm * 10), distPaint)
        }
    }

    // Draw Red Dashed Conflict Lines connecting pairs of intersecting or distance-violating cabinets
    state.complianceReport.alerts.forEach { alert ->
        val u1 = state.placedItems.firstOrNull { it.uid == alert.affectedUnitUid }
        val u2 = state.placedItems.firstOrNull { it.uid == alert.secondaryUnitUid }
        if (u1 != null && u2 != null) {
            val p1 = Offset(originX + u1.centerXCm * scale, originY + u1.centerYCm * scale)
            val p2 = Offset(originX + u2.centerXCm * scale, originY + u2.centerYCm * scale)
            drawLine(
                color = Color(0xFFFF1744),
                start = p1,
                end = p2,
                strokeWidth = 3.8f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
            )
        }
    }

    // Live Drag-and-Drop Ghost Preview in 2D Blueprint Mode (with Real-Time Red Box if violating)
    if (previewDragItem != null) {
        val left = originX + (previewDragItem.xMm / 10f) * scale
        val top = originY + (previewDragItem.yMm / 10f) * scale
        val w = (previewDragItem.effectiveWidthMm / 10f) * scale
        val h = (previewDragItem.effectiveDepthMm / 10f) * scale

        val previewReport = EngineeringComplianceAnalyzer.validateLayout(
            items = state.placedItems.filter { it.uid != previewDragItem.uid } + previewDragItem,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm
        )
        val isPreviewViolating = previewDragItem.uid in previewReport.violatingUnitUids
        val ghostFillColor = if (isPreviewViolating) Color(0xFFFF1744).copy(alpha = 0.36f) else Color(0xFF00E5FF).copy(alpha = 0.32f)
        val ghostStrokeColor = if (isPreviewViolating) Color(0xFFFF1744) else Color(0xFF00E676)

        drawRoundRect(
            color = ghostFillColor,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = ghostStrokeColor,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(
                width = 4.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
            )
        )
        if (isPreviewViolating) {
            drawRect(
                color = Color(0xFFFF1744),
                topLeft = Offset(left - 6f, top - 6f),
                size = Size(w + 12f, h + 12f),
                style = Stroke(width = 4.2f)
            )
        }
        val ghostPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (isPreviewViolating) android.graphics.Color.parseColor("#B71C1C") else android.graphics.Color.parseColor("#004D40")
        }
        drawDistanceBadge(
            center = Offset(left + w / 2f, (top - 18f).coerceAtLeast(originY + 18f)),
            text = if (isPreviewViolating) "⚠️ تقاطع / مسافة غير كافية!" else "إفلات: ${previewDragItem.xMm},${previewDragItem.yMm}mm",
            paint = ghostPaint
        )
    }
}

private fun DrawScope.drawDistanceBadge(center: Offset, text: String, paint: Paint) {
    val badgeW = 140f
    val badgeH = 30f
    drawRoundRect(
        color = Color(0xFFA7FFEB),
        topLeft = Offset(center.x - badgeW / 2f, center.y - badgeH / 2f),
        size = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = Color(0xFF00796B),
        topLeft = Offset(center.x - badgeW / 2f, center.y - badgeH / 2f),
        size = Size(badgeW, badgeH),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 2f)
    )
    drawContext.canvas.nativeCanvas.drawText(
        text,
        center.x,
        center.y + 8f,
        paint
    )
}

private fun projectIsoPoint(
    xCm: Float,
    yCm: Float,
    zCm: Float,
    roomW: Float,
    roomD: Float,
    yawDeg: Float,
    pitchFactor: Float,
    scale: Float,
    centerX: Float,
    centerY: Float,
    isFirstPerson: Boolean = false,
    walkForwardCm: Float = 0f,
    walkStrafeCm: Float = 0f,
    eyeHeightCm: Float = 155f
): Offset {
    val rad = Math.toRadians(yawDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    val cx = xCm - roomW / 2f
    val cy = yCm - roomD / 2f

    val rx = cx * cosA - cy * sinA
    val ry = cx * sinA + cy * cosA

    if (isFirstPerson) {
        val localRx = rx - walkStrafeCm
        val localRy = ry + walkForwardCm
        val depthPerspective = (1.18f + (localRy / roomD.coerceAtLeast(200f)) * 0.38f).coerceIn(0.72f, 1.68f)
        val verticalScale = 0.88f * depthPerspective
        val dynamicPitch = pitchFactor.coerceIn(0.14f, 0.72f)
        val eyeRefCm = (eyeHeightCm - 40f).coerceIn(75f, 155f)
        val screenX = centerX + localRx * scale * depthPerspective
        val screenY = centerY + localRy * dynamicPitch * scale - (zCm - eyeRefCm) * verticalScale * scale
        return Offset(screenX, screenY)
    }

    val verticalScale = (1.05f - pitchFactor * 0.45f).coerceIn(0.45f, 0.92f)
    val screenX = centerX + rx * scale
    val screenY = centerY + ry * pitchFactor * scale - zCm * verticalScale * scale
    return Offset(screenX, screenY)
}

private fun findClosest3DItem(
    tapOffset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    state: PlannerStudioState
): PlacedKitchenItem? {
    val roomW = state.roomWidthCm.toFloat()
    val roomD = state.roomDepthCm.toFloat()
    val isFp = state.viewMode == StudioViewMode.WALKTHROUGH_FP ||
        state.viewMode == StudioViewMode.VR_CAMERA_WALKTHROUGH
    val baseScale = min(canvasWidth / (roomW * 1.45f), canvasHeight / (roomD * 1.35f)) * state.cameraZoom
    val centerX = canvasWidth / 2f
    val pitch = state.cameraPitchFactor.coerceIn(0.14f, 0.88f)
    val centerY = if (isFp) canvasHeight * (0.38f + pitch * 0.52f).coerceIn(0.32f, 0.74f) else canvasHeight * 0.60f

    return state.placedItems.minByOrNull { item ->
        val zMid = (item.elevationMm + item.heightMm / 2f) / 10f
        val proj = projectIsoPoint(
            xCm = item.centerXCm,
            yCm = item.centerYCm,
            zCm = zMid,
            roomW = roomW,
            roomD = roomD,
            yawDeg = state.cameraYawDeg,
            pitchFactor = state.cameraPitchFactor,
            scale = baseScale,
            centerX = centerX,
            centerY = centerY,
            isFirstPerson = isFp,
            walkForwardCm = state.firstPersonStepForwardCm,
            walkStrafeCm = state.vrStrafeLateralCm,
            eyeHeightCm = state.vrEyeHeightCm
        )
        val dx = proj.x - tapOffset.x
        val dy = proj.y - tapOffset.y
        dx * dx + dy * dy
    }
}

private fun findClosest3DItemWithinRadius(
    tapOffset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    state: PlannerStudioState,
    maxRadiusPx: Float
): PlacedKitchenItem? {
    val roomW = state.roomWidthCm.toFloat()
    val roomD = state.roomDepthCm.toFloat()
    val isFp = state.viewMode == StudioViewMode.WALKTHROUGH_FP ||
        state.viewMode == StudioViewMode.VR_CAMERA_WALKTHROUGH
    val baseScale = min(canvasWidth / (roomW * 1.45f), canvasHeight / (roomD * 1.35f)) * state.cameraZoom
    val centerX = canvasWidth / 2f
    val pitch = state.cameraPitchFactor.coerceIn(0.14f, 0.88f)
    val centerY = if (isFp) canvasHeight * (0.38f + pitch * 0.52f).coerceIn(0.32f, 0.74f) else canvasHeight * 0.60f

    return state.placedItems
        .map { item ->
            val zMid = (item.elevationMm + item.heightMm / 2f) / 10f
            val proj = projectIsoPoint(
                xCm = item.centerXCm,
                yCm = item.centerYCm,
                zCm = zMid,
                roomW = roomW,
                roomD = roomD,
                yawDeg = state.cameraYawDeg,
                pitchFactor = state.cameraPitchFactor,
                scale = baseScale,
                centerX = centerX,
                centerY = centerY,
                isFirstPerson = isFp,
                walkForwardCm = state.firstPersonStepForwardCm,
                walkStrafeCm = state.vrStrafeLateralCm,
                eyeHeightCm = state.vrEyeHeightCm
            )
            item to hypot(proj.x - tapOffset.x, proj.y - tapOffset.y)
        }
        .filter { (_, dist) -> dist <= maxRadiusPx }
        .minByOrNull { (_, dist) -> dist }
        ?.first
}

/**
 * Real-Time Photorealistic 3D PBR Renderer:
 * Features directional sunlight, floor cast shadows, contact ambient occlusion (AO),
 * recessed 10cm toe-kick plinths, marble backsplash veins, architectural window with sunbeams,
 * ceiling recessed spotlights, CNC 2mm door reveals, Gola aluminum channels, and 3000K vitrine glass reflections.
 */
private fun DrawScope.draw3DPhotorealisticScene(
    state: PlannerStudioState,
    previewDragItem: PlacedKitchenItem? = null,
    customCenterX: Float? = null
) {
    val roomW = state.roomWidthCm.toFloat()
    val roomD = state.roomDepthCm.toFloat()
    val roomH = state.roomHeightCm.toFloat()
    val isFp = state.viewMode == StudioViewMode.WALKTHROUGH_FP ||
        state.viewMode == StudioViewMode.VR_CAMERA_WALKTHROUGH

    val baseScale = min(size.width / (roomW * 1.45f), size.height / (roomD * 1.35f)) * state.cameraZoom
    val centerX = customCenterX ?: (size.width / 2f)
    val pitch = state.cameraPitchFactor.coerceIn(0.14f, 0.88f)
    val centerY = if (isFp) size.height * (0.38f + pitch * 0.52f).coerceIn(0.32f, 0.74f) else size.height * 0.60f

    fun proj(x: Float, y: Float, z: Float): Offset = projectIsoPoint(
        xCm = x,
        yCm = y,
        zCm = z,
        roomW = roomW,
        roomD = roomD,
        yawDeg = state.cameraYawDeg,
        pitchFactor = state.cameraPitchFactor,
        scale = baseScale,
        centerX = centerX,
        centerY = centerY,
        isFirstPerson = isFp,
        walkForwardCm = state.firstPersonStepForwardCm,
        walkStrafeCm = state.vrStrafeLateralCm,
        eyeHeightCm = state.vrEyeHeightCm
    )

    val isWire = state.lightingMode == LightingMode.BLUEPRINT_WIRE
    val lightSim = state.lightingSimulation
    val isNightLed = state.lightingMode == LightingMode.WARM_LED || lightSim.naturalLightIntensity < 0.28f

    // Compute dynamic Kelvin light color (2700K warm gold -> 6500K cool daylight)
    val kelvinColor = when {
        lightSim.colorTemperatureKelvin <= 3200 -> Color(0xFFFFCA28)
        lightSim.colorTemperatureKelvin <= 4200 -> Color(0xFFFFE082)
        lightSim.colorTemperatureKelvin <= 5200 -> Color(0xFFFFF9C4)
        else -> Color(0xFFE1F5FE)
    }
    val sunRad = Math.toRadians(lightSim.sunAzimuthDeg.toDouble())
    val sunDirX = cos(sunRad).toFloat()
    val sunDirY = sin(sunRad).toFloat().coerceAtLeast(0.15f)
    val shadowDxCm = (sunDirX * 14f * lightSim.shadowSoftnessAndDepth).coerceIn(-18f, 20f)
    val shadowDyCm = (sunDirY * 15f * lightSim.shadowSoftnessAndDepth).coerceIn(4f, 22f)
    val complianceAlertsByUnit3D = state.complianceReport.alerts.groupBy { it.affectedUnitUid }

    // 1. Draw 3D Room Floor Slab with PBR Directional Gradient & Specular Sheen
    val floorBaseHex = KitchenCatalogData.resolveFloorColorHex(state.floorMaterial)
    val floorDarkenFactor = ((1f - lightSim.naturalLightIntensity * 0.7f - lightSim.artificialSpotIntensity * 0.3f) * 0.52f).coerceIn(0f, 0.55f)
    val floorColor = when {
        isWire -> Color(0xFF173753)
        isNightLed -> Color(floorBaseHex).darken(0.45f)
        else -> Color(floorBaseHex).darken(floorDarkenFactor)
    }

    val f0 = proj(0f, 0f, 0f)
    val f1 = proj(roomW, 0f, 0f)
    val f2 = proj(roomW, roomD, 0f)
    val f3 = proj(0f, roomD, 0f)

    val floorPath = Path().apply {
        moveTo(f0.x, f0.y)
        lineTo(f1.x, f1.y)
        lineTo(f2.x, f2.y)
        lineTo(f3.x, f3.y)
        close()
    }
    if (isWire) {
        drawPath(floorPath, color = floorColor)
    } else {
        drawPath(
            path = floorPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    floorColor.darken(0.14f),
                    floorColor,
                    floorColor.lighten((0.08f + lightSim.surfaceSpecularReflection * 0.10f).coerceAtMost(0.24f)),
                    floorColor.darken(0.08f)
                ),
                start = f0,
                end = f2
            )
        )
    }
    drawPath(floorPath, color = Color(0xFFC88A4B).copy(alpha = 0.55f), style = Stroke(width = 2.8f))

    // Parquet / Porcelain Floor Tile Planks & Subtle Specular Grout Lines
    val gridLineColor = if (isWire || isNightLed) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.07f)
    }
    var gx = 30f
    while (gx < roomW) {
        val pStart = proj(gx, 0f, 0f)
        val pEnd = proj(gx, roomD, 0f)
        drawLine(gridLineColor, pStart, pEnd, strokeWidth = 1.1f)
        gx += 30f
    }
    var gy = 45f
    while (gy < roomD) {
        val pStart = proj(0f, gy, 0f)
        val pEnd = proj(roomW, gy, 0f)
        drawLine(gridLineColor, pStart, pEnd, strokeWidth = 1.1f)
        gy += 45f
    }

    // Floor Sunlight Patch (cast from side architectural window, steered by sunAzimuthDeg & naturalLightIntensity)
    if (!isWire && lightSim.naturalLightIntensity > 0.08f) {
        val shiftX = sunDirX * 38f
        val shiftY = sunDirY * 26f
        val sunPatch = Path().apply {
            val sp0 = proj((25f + shiftX * 0.3f).coerceIn(8f, roomW - 20f), 80f, 0.2f)
            val sp1 = proj((195f + shiftX).coerceIn(40f, roomW - 10f), (95f + shiftY * 0.4f).coerceIn(40f, roomD - 10f), 0.2f)
            val sp2 = proj((235f + shiftX * 1.2f).coerceIn(50f, roomW - 8f), (245f + shiftY).coerceIn(60f, roomD - 8f), 0.2f)
            val sp3 = proj((35f + shiftX * 0.4f).coerceIn(8f, roomW - 20f), (225f + shiftY * 0.6f).coerceIn(50f, roomD - 10f), 0.2f)
            moveTo(sp0.x, sp0.y)
            lineTo(sp1.x, sp1.y)
            lineTo(sp2.x, sp2.y)
            lineTo(sp3.x, sp3.y)
            close()
        }
        drawPath(
            path = sunPatch,
            color = kelvinColor.copy(alpha = (lightSim.naturalLightIntensity * 0.28f).coerceIn(0.04f, 0.32f))
        )
    }

    // 2. Draw Back & Side Architectural Walls with PBR Gradient & Corner Ambient Occlusion
    val wallBaseColor = Color(KitchenCatalogData.resolveWallColorHex(state.wallColorHex))
    val wallColorBack = when {
        isWire -> Color(0xFF1D4668).copy(alpha = 0.55f)
        isNightLed -> wallBaseColor.darken(0.62f)
        else -> wallBaseColor.darken(floorDarkenFactor * 0.8f)
    }
    val wallColorSide = when {
        isWire -> Color(0xFF163854).copy(alpha = 0.55f)
        isNightLed -> wallBaseColor.darken(0.70f)
        else -> wallBaseColor.darken((0.09f + floorDarkenFactor * 0.8f).coerceAtMost(0.68f))
    }

    val wBack0 = proj(0f, 0f, 0f)
    val wBack1 = proj(roomW, 0f, 0f)
    val wBack2 = proj(roomW, 0f, roomH)
    val wBack3 = proj(0f, 0f, roomH)
    val backWallPath = Path().apply {
        moveTo(wBack0.x, wBack0.y)
        lineTo(wBack1.x, wBack1.y)
        lineTo(wBack2.x, wBack2.y)
        lineTo(wBack3.x, wBack3.y)
        close()
    }
    if (isWire) {
        drawPath(backWallPath, wallColorBack)
    } else {
        drawPath(
            path = backWallPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    wallColorBack.lighten(0.06f),
                    wallColorBack,
                    wallColorBack.darken(0.12f)
                ),
                startY = min(wBack2.y, wBack3.y),
                endY = maxOf(wBack0.y, wBack1.y)
            )
        )
    }
    drawPath(backWallPath, Color.White.copy(alpha = 0.22f), style = Stroke(width = 2f))

    val wSide0 = proj(0f, 0f, 0f)
    val wSide1 = proj(0f, roomD, 0f)
    val wSide2 = proj(0f, roomD, roomH)
    val wSide3 = proj(0f, 0f, roomH)
    val sideWallPath = Path().apply {
        moveTo(wSide0.x, wSide0.y)
        lineTo(wSide1.x, wSide1.y)
        lineTo(wSide2.x, wSide2.y)
        lineTo(wSide3.x, wSide3.y)
        close()
    }
    if (isWire) {
        drawPath(sideWallPath, wallColorSide)
    } else {
        drawPath(
            path = sideWallPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    wallColorSide.darken(0.14f),
                    wallColorSide,
                    wallColorSide.lighten(0.05f)
                ),
                start = wSide0,
                end = wSide2
            )
        )
    }
    drawPath(sideWallPath, Color.White.copy(alpha = 0.22f), style = Stroke(width = 2f))

    // Room Corner & Baseboard Ambient Occlusion (AO) Shadow Creases
    if (!isWire) {
        val aoCornerAlpha = (0.18f + lightSim.shadowSoftnessAndDepth * 0.16f).coerceIn(0.10f, 0.38f)
        drawLine(
            color = Color.Black.copy(alpha = aoCornerAlpha),
            start = wBack0,
            end = wBack3,
            strokeWidth = 6f
        )
        drawLine(
            color = Color.Black.copy(alpha = aoCornerAlpha * 0.85f),
            start = wBack0,
            end = wBack1,
            strokeWidth = 5f
        )
        drawLine(
            color = Color.Black.copy(alpha = aoCornerAlpha * 0.85f),
            start = wSide0,
            end = wSide1,
            strokeWidth = 5f
        )
    }

    // 2B. Architectural Framed Window on Side Wall + Daylight / Evening Sky
    if (!isWire && roomD >= 240f) {
        val winY0 = (roomD * 0.28f).coerceAtLeast(65f)
        val winY1 = (roomD * 0.66f).coerceAtMost(roomD - 45f)
        val winZ0 = 102f
        val winZ1 = (roomH - 48f).coerceAtLeast(195f)

        val wp0 = proj(0.6f, winY0, winZ0)
        val wp1 = proj(0.6f, winY1, winZ0)
        val wp2 = proj(0.6f, winY1, winZ1)
        val wp3 = proj(0.6f, winY0, winZ1)
        val winPath = Path().apply {
            moveTo(wp0.x, wp0.y)
            lineTo(wp1.x, wp1.y)
            lineTo(wp2.x, wp2.y)
            lineTo(wp3.x, wp3.y)
            close()
        }
        val skyColors = when {
            isNightLed -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            lightSim.colorTemperatureKelvin <= 3600 -> listOf(Color(0xFFFF8A65), Color(0xFFFFCC80), Color(0xFFFFF59D))
            else -> listOf(Color(0xFF90CAF9), Color(0xFFE3F2FD), Color(0xFFFFF9C4))
        }
        drawPath(
            path = winPath,
            brush = Brush.verticalGradient(
                colors = skyColors,
                startY = min(wp2.y, wp3.y),
                endY = maxOf(wp0.y, wp1.y)
            )
        )
        // Window Anodized Aluminum Frame & Mullion
        drawPath(winPath, color = Color(0xFF263238), style = Stroke(width = 4.5f))
        val midWinBottom = proj(0.6f, (winY0 + winY1) / 2f, winZ0)
        val midWinTop = proj(0.6f, (winY0 + winY1) / 2f, winZ1)
        drawLine(Color(0xFF263238), midWinBottom, midWinTop, strokeWidth = 3f)
        // Diagonal glass reflection streak
        val refA = proj(0.7f, winY0 + 12f, winZ0 + 18f)
        val refB = proj(0.7f, winY0 + 38f, winZ1 - 16f)
        drawLine(
            Color.White.copy(alpha = (0.25f + lightSim.surfaceSpecularReflection * 0.35f).coerceIn(0.15f, 0.65f)),
            refA,
            refB,
            strokeWidth = 3.5f
        )
    }

    // 2C. Full-Width Marble/Porcelain Backsplash Slab on Back Wall (z = 85cm to z = 145cm)
    val countertopBaseHex = KitchenCatalogData.resolveCountertopColorHex(state.countertopMaterial)
    if (!isWire) {
        val bs0 = proj(0f, 0.8f, 85f)
        val bs1 = proj(roomW, 0.8f, 85f)
        val bs2 = proj(roomW, 0.8f, 145f)
        val bs3 = proj(0f, 0.8f, 145f)
        val backsplashPath = Path().apply {
            moveTo(bs0.x, bs0.y)
            lineTo(bs1.x, bs1.y)
            lineTo(bs2.x, bs2.y)
            lineTo(bs3.x, bs3.y)
            close()
        }
        val bsBase = Color(countertopBaseHex)
        val bsTinted = if (isNightLed) bsBase.darken(0.32f) else bsBase.darken(0.04f)
        drawPath(
            path = backsplashPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    bsTinted.lighten(0.08f),
                    bsTinted,
                    bsTinted.darken(0.10f)
                ),
                start = bs3,
                end = bs1
            )
        )
        // Subtle marble veining across backsplash
        var vx = 45f
        while (vx < roomW - 35f) {
            val vStart = proj(vx, 1.0f, 88f)
            val vMid = proj(vx + 28f, 1.0f, 118f)
            val vEnd = proj(vx + 46f, 1.0f, 142f)
            drawLine(Color(0xFF90A4AE).copy(alpha = 0.28f), vStart, vMid, strokeWidth = 1.4f)
            drawLine(Color(0xFFB0BEC5).copy(alpha = 0.22f), vMid, vEnd, strokeWidth = 1.1f)
            vx += 85f
        }
    }

    // 2D. Ceiling Architectural Recessed Downlights along Top of Back Wall (controlled by artificialSpotIntensity)
    if (!isWire && lightSim.artificialSpotIntensity > 0.05f) {
        val spotGlowAlpha = (lightSim.artificialSpotIntensity * (if (isNightLed) 0.48f else 0.30f)).coerceIn(0.04f, 0.55f)
        val spotRadius = (36f + lightSim.artificialSpotIntensity * 26f) * state.cameraZoom
        var spotX = 70f
        while (spotX <= roomW - 60f) {
            val spotCenter = proj(spotX, 18f, roomH - 6f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        kelvinColor.copy(alpha = spotGlowAlpha),
                        Color.Transparent
                    ),
                    center = spotCenter,
                    radius = spotRadius
                ),
                radius = spotRadius,
                center = spotCenter
            )
            drawCircle(
                color = Color(0xFFFFF8E1),
                radius = 4.2f * state.cameraZoom,
                center = spotCenter
            )
            spotX += 110f
        }
    }

    // Draw 600mm (60cm) Safety Clearance Zone Laser Line on back wall (from z=85cm to z=145cm)
    if (state.showDimensions) {
        val safeBotLeft = proj(0f, 2f, 85f)
        val safeBotRight = proj(roomW, 2f, 85f)
        val safeTopLeft = proj(0f, 2f, 145f)
        val safeTopRight = proj(roomW, 2f, 145f)
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.42f),
            start = safeBotLeft,
            end = safeBotRight,
            strokeWidth = 1.8f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
        )
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.52f),
            start = safeTopLeft,
            end = safeTopRight,
            strokeWidth = 2.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
        )
    }

    // 3. Depth-sort placed 3D modules using painter's algorithm
    val rad = Math.toRadians(state.cameraYawDeg.toDouble())
    val sinA = sin(rad).toFloat()
    val cosA = cos(rad).toFloat()

    val sorted3DItems = state.placedItems.sortedWith(
        compareBy<PlacedKitchenItem> { item ->
            item.centerXCm * sinA + item.centerYCm * cosA
        }.thenBy { it.elevationMm }
    )

    // 3A. First Pass: Real-Time Soft Cast Floor Shadows & Contact Ambient Occlusion (steered by LightingSimulationConfig)
    if (!isWire) {
        val castShadowAlpha = ((0.12f + lightSim.shadowSoftnessAndDepth * 0.26f) *
            (0.45f + lightSim.naturalLightIntensity * 0.55f)).coerceIn(0.06f, 0.45f)
        val contactAoAlpha = (0.20f + lightSim.shadowSoftnessAndDepth * 0.25f).coerceIn(0.10f, 0.46f)
        val underCabAlpha = (lightSim.underCabinetLedIntensity * (if (isNightLed) 0.46f else 0.28f)).coerceIn(0.0f, 0.52f)

        sorted3DItems.forEach { item ->
            if (item.elevationMm <= 200) {
                val x0 = item.xMm / 10f
                val y0 = item.yMm / 10f
                val x1 = x0 + item.effectiveWidthMm / 10f
                val y1 = y0 + item.effectiveDepthMm / 10f

                // Directional sun cast shadow steered by sunAzimuthDeg & shadowSoftnessAndDepth
                val sh0 = proj(x0 + shadowDxCm * 0.35f, y0 + shadowDyCm * 0.35f, 0.2f)
                val sh1 = proj(x1 + shadowDxCm, y0 + shadowDyCm * 0.35f, 0.2f)
                val sh2 = proj(x1 + shadowDxCm * 1.15f, y1 + shadowDyCm, 0.2f)
                val sh3 = proj(x0 + shadowDxCm * 0.45f, y1 + shadowDyCm, 0.2f)
                val castShadowPath = Path().apply {
                    moveTo(sh0.x, sh0.y)
                    lineTo(sh1.x, sh1.y)
                    lineTo(sh2.x, sh2.y)
                    lineTo(sh3.x, sh3.y)
                    close()
                }
                drawPath(castShadowPath, Color.Black.copy(alpha = castShadowAlpha))

                // Tight Contact Ambient Occlusion (AO) directly under unit footprint
                val ao0 = proj(x0 - 1f, y0 - 1f, 0.4f)
                val ao1 = proj(x1 + 1f, y0 - 1f, 0.4f)
                val ao2 = proj(x1 + 2f, y1 + 2.5f, 0.4f)
                val ao3 = proj(x0 - 1f, y1 + 2.5f, 0.4f)
                val aoPath = Path().apply {
                    moveTo(ao0.x, ao0.y)
                    lineTo(ao1.x, ao1.y)
                    lineTo(ao2.x, ao2.y)
                    lineTo(ao3.x, ao3.y)
                    close()
                }
                drawPath(aoPath, Color.Black.copy(alpha = contactAoAlpha))
            } else {
                // Upper wall cabinet cast shadow onto backsplash + Kelvin under-cabinet LED wash
                val x0 = item.xMm / 10f
                val x1 = x0 + item.effectiveWidthMm / 10f
                val z0 = item.elevationMm / 10f
                val ws0 = proj(x0 + shadowDxCm * 0.25f, 1.2f, (z0 - 16f).coerceAtLeast(86f))
                val ws1 = proj(x1 + shadowDxCm * 0.45f, 1.2f, (z0 - 16f).coerceAtLeast(86f))
                val ws2 = proj(x1 + shadowDxCm * 0.45f, 1.2f, z0)
                val ws3 = proj(x0 + shadowDxCm * 0.25f, 1.2f, z0)
                val wallShadowPath = Path().apply {
                    moveTo(ws0.x, ws0.y)
                    lineTo(ws1.x, ws1.y)
                    lineTo(ws2.x, ws2.y)
                    lineTo(ws3.x, ws3.y)
                    close()
                }
                drawPath(wallShadowPath, Color.Black.copy(alpha = castShadowAlpha))

                if (underCabAlpha > 0.02f) {
                    val lg0 = proj(x0 + 4f, 1.4f, 92f)
                    val lg1 = proj(x1 - 4f, 1.4f, 92f)
                    val lg2 = proj(x1 - 2f, 1.4f, z0 - 1f)
                    val lg3 = proj(x0 + 2f, 1.4f, z0 - 1f)
                    val ledWashPath = Path().apply {
                        moveTo(lg0.x, lg0.y)
                        lineTo(lg1.x, lg1.y)
                        lineTo(lg2.x, lg2.y)
                        lineTo(lg3.x, lg3.y)
                        close()
                    }
                    drawPath(
                        path = ledWashPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                kelvinColor.copy(alpha = underCabAlpha),
                                Color.Transparent
                            ),
                            startY = min(lg2.y, lg3.y),
                            endY = maxOf(lg0.y, lg1.y)
                        )
                    )
                }
            }
        }
    }

    val cabinetBaseHex = KitchenCatalogData.resolveCabinetColorHex(state.cabinetFinish)
    val isSmokedGlassFinish = state.cabinetFinish.contains("زجاج")
    val isWoodFinish = state.cabinetFinish.contains("جوز") || state.cabinetFinish.contains("بلوط") || state.cabinetFinish.contains("خشب")
    val handleColor = when {
        state.handleStyle.contains("نحاس") -> Color(0xFFD4AF37)
        state.handleStyle.contains("أسود") -> Color(0xFF1A1D1F)
        state.handleStyle.contains("ستانلس") -> Color(0xFFCFD8DC)
        state.handleStyle.contains("Gola") -> Color(0xFFB0BEC5)
        else -> Color.Transparent
    }

    // 3B. Second Pass: Draw Detailed 3D Cabinets, Plinths, Countertops & Appliances
    sorted3DItems.forEach { item ->
        val x0 = item.xMm / 10f
        val y0 = item.yMm / 10f
        val x1 = x0 + item.effectiveWidthMm / 10f
        val y1 = y0 + item.effectiveDepthMm / 10f
        val z0 = item.elevationMm / 10f
        val z1 = z0 + item.heightMm / 10f

        val isSelected = item.uid == state.selectedItemUid
        val isAppliance = item.type.category == ModuleCategory.APPLIANCE
        val isWallCab = item.type.category == ModuleCategory.WALL_CABINET
        val isFiller = item.type == ModuleType.FILLER_STRIP
        val isIsland = item.type.category == ModuleCategory.ISLAND_UNIT
        val hasCountertop = item.type.countertopIncluded
        val hasToeKickPlinth = !isWallCab && !isFiller && !isAppliance && z0 == 0f

        val itemFinishName = item.customCabinetFinish?.takeIf { it.isNotBlank() } ?: state.cabinetFinish
        val itemCountertopName = item.customCountertopMaterial?.takeIf { it.isNotBlank() } ?: state.countertopMaterial
        val itemCabinetBaseHex = KitchenCatalogData.resolveCabinetColorHex(itemFinishName)
        val itemCountertopBaseHex = KitchenCatalogData.resolveCountertopColorHex(itemCountertopName)
        val itemIsSmokedGlass = itemFinishName.contains("زجاج")
        val itemIsWoodFinish = itemFinishName.contains("جوز") || itemFinishName.contains("بلوط") || itemFinishName.contains("خشب")

        val bodyBaseColor = when {
            isWire -> Color(0xFF235784)
            isFiller -> Color(0xFFD84315)
            isAppliance && item.type != ModuleType.STOVE_HOB -> Color(0xFF343F46)
            (isWallCab && itemIsSmokedGlass) || item.type == ModuleType.WALL_GLASS_80 ->
                Color(0xFF1A2229).copy(alpha = 0.86f)
            else -> Color(itemCabinetBaseHex)
        }
        val bodyColor = if (isNightLed && !isWire) bodyBaseColor.darken(0.24f) else bodyBaseColor

        // 1. Recessed 10cm Anodized Toe-Kick Plinth (نعلات سفلية غاطسة) + Plinth LED
        val carcassZ0 = if (hasToeKickPlinth) 10f else z0
        if (hasToeKickPlinth && !isWire) {
            drawPbrIsoBox(
                x0 = x0 + 1.5f,
                y0 = y0 + 1.5f,
                x1 = x1 - 1.5f,
                y1 = (y1 - 4.5f).coerceAtLeast(y0 + 10f),
                z0 = 0f,
                z1 = 10f,
                frontColor = Color(0xFF181E22),
                sideColor = Color(0xFF101417),
                topColor = Color(0xFF222A30),
                strokeColor = Color.Black.copy(alpha = 0.5f),
                strokeWidth = 1.1f,
                wireframeOnly = false,
                proj = ::proj
            )
            // Warm LED Plinth Strip under cabinet overhang
            val pl0 = proj(x0 + 3f, y1 - 3f, 1.5f)
            val pl1 = proj(x1 - 3f, y1 - 3f, 1.5f)
            drawLine(
                color = Color(0xFFFFD54F).copy(alpha = if (isNightLed) 0.65f else 0.28f),
                start = pl0,
                end = pl1,
                strokeWidth = if (isNightLed) 4.2f else 2.4f
            )
        }

        // 2. Complete 6-Sided Realistic 3D Cabinet Carcass (with Full Back Panel, Full Left/Right 18mm Side Gable Panels & Internal Shelves)
        val unitAlerts3D = complianceAlertsByUnit3D[item.uid].orEmpty()
        val has3DAlert = unitAlerts3D.isNotEmpty()
        val carcassStrokeColor = when {
            unitAlerts3D.any { it.isCritical } -> Color(0xFFFF1744)
            has3DAlert -> Color(0xFFFF9100)
            isSelected -> Color(0xFFFFD54F)
            else -> Color.Black.copy(alpha = 0.38f)
        }
        val carcassZ1 = if (hasCountertop) z1 - 5f else z1
        val sideGableThickCm = (item.boardThicknessMm / 10f).coerceIn(1.4f, 2.4f)
        val backPanelThickCm = (item.backPanelThicknessMm / 10f).coerceIn(0.8f, 2.2f)

        // Draw the main 6-sided enclosed 3D box (depth-sorted across all 6 faces by cameraYawDeg)
        drawPbrIsoBox(
            x0 = x0,
            y0 = y0,
            x1 = x1,
            y1 = y1,
            z0 = carcassZ0,
            z1 = carcassZ1,
            frontColor = bodyColor,
            sideColor = bodyColor.darken(0.18f),
            topColor = bodyColor.lighten(0.12f),
            strokeColor = carcassStrokeColor,
            strokeWidth = if (isSelected || has3DAlert) 4.2f else 1.5f,
            wireframeOnly = isWire,
            cameraYawDeg = state.cameraYawDeg,
            proj = ::proj
        )

        // Render explicit Full Back Panel (ظهر كامل 18/8 ملم) & Full Left/Right 18mm Side Gable Panels (أجناب كاملة)
        if (!isFiller && !isAppliance) {
            val gableSideColor = bodyColor.darken(0.12f)
            val backBoardColor = bodyColor.darken(0.24f)

            // Full Back Panel Slab at rear (y0 .. y0 + backPanelThickCm)
            if (item.hasFullBackPanel) {
                drawPbrIsoBox(
                    x0 = x0 + 0.2f,
                    y0 = y0,
                    x1 = x1 - 0.2f,
                    y1 = (y0 + backPanelThickCm).coerceAtMost(y1 - 2f),
                    z0 = carcassZ0 + 0.2f,
                    z1 = carcassZ1 - 0.2f,
                    frontColor = backBoardColor,
                    sideColor = backBoardColor.darken(0.15f),
                    topColor = backBoardColor.lighten(0.08f),
                    strokeColor = Color.Black.copy(alpha = 0.42f),
                    strokeWidth = 1.1f,
                    wireframeOnly = isWire,
                    cameraYawDeg = state.cameraYawDeg,
                    proj = ::proj
                )
            }

            // Full Left Side Panel (جنب أيسر كامل 18 ملم) & Full Right Side Panel (جنب أيمن كامل 18 ملم)
            if (item.hasFullSidePanels && (x1 - x0) > sideGableThickCm * 3f) {
                // Left Gable
                drawPbrIsoBox(
                    x0 = x0,
                    y0 = y0,
                    x1 = x0 + sideGableThickCm,
                    y1 = y1,
                    z0 = carcassZ0,
                    z1 = carcassZ1,
                    frontColor = gableSideColor.lighten(0.05f),
                    sideColor = gableSideColor,
                    topColor = gableSideColor.lighten(0.12f),
                    strokeColor = Color.Black.copy(alpha = 0.35f),
                    strokeWidth = 1.0f,
                    wireframeOnly = isWire,
                    cameraYawDeg = state.cameraYawDeg,
                    proj = ::proj
                )
                // Right Gable
                drawPbrIsoBox(
                    x0 = x1 - sideGableThickCm,
                    y0 = y0,
                    x1 = x1,
                    y1 = y1,
                    z0 = carcassZ0,
                    z1 = carcassZ1,
                    frontColor = gableSideColor.lighten(0.05f),
                    sideColor = gableSideColor,
                    topColor = gableSideColor.lighten(0.12f),
                    strokeColor = Color.Black.copy(alpha = 0.35f),
                    strokeWidth = 1.0f,
                    wireframeOnly = isWire,
                    cameraYawDeg = state.cameraYawDeg,
                    proj = ::proj
                )
            }

            // Internal Structural Shelves (أرفف داخلية 18 ملم) visible inside open/glass/preview cabinets
            val shelvesToDraw = item.shelvesCount.coerceIn(1, 5)
            if (isWire || item.type == ModuleType.WALL_OPEN_SHELVES_60 ||
                item.type == ModuleType.WALL_GLASS_80 ||
                item.type == ModuleType.TALL_GLASS_DISPLAY_80 ||
                item.type == ModuleType.TALL_COFFEE_STATION_90 ||
                state.doorsOpenPreview
            ) {
                for (sIdx in 1..shelvesToDraw) {
                    val ratio = sIdx.toFloat() / (shelvesToDraw + 1).toFloat()
                    val shelfZ = carcassZ0 + (carcassZ1 - carcassZ0) * ratio
                    val sLeft = proj(x0 + sideGableThickCm, y1 - 1.5f, shelfZ)
                    val sRight = proj(x1 - sideGableThickCm, y1 - 1.5f, shelfZ)
                    val sBackL = proj(x0 + sideGableThickCm, y0 + backPanelThickCm, shelfZ)
                    val sBackR = proj(x1 - sideGableThickCm, y0 + backPanelThickCm, shelfZ)
                    val shelfPath = Path().apply {
                        moveTo(sBackL.x, sBackL.y)
                        lineTo(sBackR.x, sBackR.y)
                        lineTo(sRight.x, sRight.y)
                        lineTo(sLeft.x, sLeft.y)
                        close()
                    }
                    drawPath(shelfPath, bodyColor.lighten(0.10f).copy(alpha = 0.78f))
                    drawLine(Color.Black.copy(alpha = 0.45f), sLeft, sRight, strokeWidth = 2.0f)
                }
            }

            // Special L-Corner & Diagonal 45° Corner 3D Detailing for Upper & Lower Corner Cabinets
            if (item.type.isCornerCabinet && !isWire) {
                val cornerDiagStart = proj(x0 + sideGableThickCm, y1 - 0.3f, carcassZ0 + 3f)
                val cornerDiagMid = proj((x0 + x1) / 2f, (y0 + y1) / 2f + 6f, carcassZ0 + 3f)
                val cornerDiagTop = proj((x0 + x1) / 2f, (y0 + y1) / 2f + 6f, carcassZ1 - 3f)
                drawLine(Color(0xFF10181E).copy(alpha = 0.72f), cornerDiagMid, cornerDiagTop, strokeWidth = 2.6f)
                drawLine(Color(0xFFB0BEC5).copy(alpha = 0.65f), cornerDiagStart, cornerDiagMid, strokeWidth = 1.8f)

                // Carousel turntable ring inside corner cabinet
                val turntableCenter = proj((x0 + x1) / 2f, (y0 + y1) / 2f, (carcassZ0 + carcassZ1) / 2f)
                drawCircle(
                    color = Color(0xFF80DEEA).copy(alpha = 0.38f),
                    radius = 12f * state.cameraZoom,
                    center = turntableCenter,
                    style = Stroke(width = 2.0f)
                )
            }
        }

        // 3. Wood Grain Texture Lines on Front & Side Faces (when Walnut / Oak finish is selected)
        if (!isWire && itemIsWoodFinish && !isAppliance && item.type != ModuleType.WALL_GLASS_80) {
            val grainColor = bodyColor.darken(0.14f).copy(alpha = 0.35f)
            val g1Start = proj(x0 + (x1 - x0) * 0.28f, y1 + 0.2f, carcassZ0 + 6f)
            val g1End = proj(x0 + (x1 - x0) * 0.28f, y1 + 0.2f, carcassZ1 - 6f)
            val g2Start = proj(x0 + (x1 - x0) * 0.72f, y1 + 0.2f, carcassZ0 + 6f)
            val g2End = proj(x0 + (x1 - x0) * 0.72f, y1 + 0.2f, carcassZ1 - 6f)
            drawLine(grainColor, g1Start, g1End, strokeWidth = 1.2f)
            drawLine(grainColor, g2Start, g2End, strokeWidth = 1.2f)
        }

        // 4. CNC 2mm Door Reveals, Drawer Stack Splits & Gola Recessed Channels
        if (!isWire && !isFiller && !isAppliance) {
            val revealColor = Color(0xFF111619).copy(alpha = 0.68f)
            when (item.type) {
                ModuleType.BASE_DRAWERS_80 -> {
                    // 3 horizontal drawer splits + recessed Gola J/C aluminum shadow channels
                    val d1Z = carcassZ0 + (carcassZ1 - carcassZ0) * 0.38f
                    val d2Z = carcassZ0 + (carcassZ1 - carcassZ0) * 0.72f
                    val topGolaZ = carcassZ1 - 2.5f
                    listOf(d1Z, d2Z, topGolaZ).forEach { gz ->
                        val pLeft = proj(x0 + 2f, y1 + 0.4f, gz)
                        val pRight = proj(x1 - 2f, y1 + 0.4f, gz)
                        drawLine(revealColor, pLeft, pRight, strokeWidth = 3.0f)
                        drawLine(Color(0xFFB0BEC5).copy(alpha = 0.55f), pLeft, pRight, strokeWidth = 1.2f)
                    }
                }
                ModuleType.BASE_POT_DRAWERS_90 -> {
                    // 2 deep heavy-duty pot drawer splits
                    val midZ = (carcassZ0 + carcassZ1) / 2f
                    val topGolaZ = carcassZ1 - 2.5f
                    listOf(midZ, topGolaZ).forEach { gz ->
                        val pLeft = proj(x0 + 2f, y1 + 0.4f, gz)
                        val pRight = proj(x1 - 2f, y1 + 0.4f, gz)
                        drawLine(revealColor, pLeft, pRight, strokeWidth = 3.2f)
                        drawLine(Color(0xFFB0BEC5).copy(alpha = 0.6f), pLeft, pRight, strokeWidth = 1.4f)
                    }
                }
                ModuleType.BASE_SPICE_PULLOUT_30 -> {
                    // Narrow pull-out spice unit with dual chrome rail lines
                    val r1 = carcassZ0 + (carcassZ1 - carcassZ0) * 0.35f
                    val r2 = carcassZ0 + (carcassZ1 - carcassZ0) * 0.70f
                    listOf(r1, r2).forEach { rz ->
                        val pLeft = proj(x0 + 3f, y1 + 0.4f, rz)
                        val pRight = proj(x1 - 3f, y1 + 0.4f, rz)
                        drawLine(Color(0xFFCFD8DC), pLeft, pRight, strokeWidth = 2.0f)
                    }
                }
                ModuleType.WALL_LIFT_60 -> {
                    // Bi-fold lift-up horizontal split in middle
                    val midZ = (carcassZ0 + carcassZ1) / 2f
                    val pLeft = proj(x0 + 2f, y1 + 0.4f, midZ)
                    val pRight = proj(x1 - 2f, y1 + 0.4f, midZ)
                    drawLine(revealColor, pLeft, pRight, strokeWidth = 2.4f)
                }
                ModuleType.TALL_COFFEE_STATION_90 -> {
                    // Tall coffee bar niche in middle + lower drawers + upper bi-fold door
                    val nicheZ0 = carcassZ0 + (carcassZ1 - carcassZ0) * 0.42f
                    val nicheZ1 = carcassZ0 + (carcassZ1 - carcassZ0) * 0.68f
                    val n0 = proj(x0 + 4f, y1 + 0.4f, nicheZ0)
                    val n1 = proj(x1 - 4f, y1 + 0.4f, nicheZ0)
                    val n2 = proj(x1 - 4f, y1 + 0.4f, nicheZ1)
                    val n3 = proj(x0 + 4f, y1 + 0.4f, nicheZ1)
                    val nichePath = Path().apply {
                        moveTo(n0.x, n0.y); lineTo(n1.x, n1.y); lineTo(n2.x, n2.y); lineTo(n3.x, n3.y); close()
                    }
                    drawPath(nichePath, Color(0xFF1A1412))
                    drawLine(Color(0xFFFFD54F).copy(alpha = 0.75f), n3, n2, strokeWidth = 2.8f)
                }
                else -> {
                    // Custom drawer splits if user configured drawersCount > 0 in Custom Cabinet Designer
                    if (item.drawersCount > 0) {
                        for (dIdx in 1..item.drawersCount) {
                            val gz = carcassZ0 + (carcassZ1 - carcassZ0) * (dIdx.toFloat() / item.drawersCount.toFloat())
                            val pLeft = proj(x0 + 2f, y1 + 0.4f, gz)
                            val pRight = proj(x1 - 2f, y1 + 0.4f, gz)
                            drawLine(revealColor, pLeft, pRight, strokeWidth = 2.6f)
                        }
                    } else if (item.doorsCount >= 2 || item.widthMm >= 700) {
                        val doorCount = item.doorsCount.coerceAtLeast(2)
                        for (dIdx in 1 until doorCount) {
                            val splitX = x0 + (x1 - x0) * (dIdx.toFloat() / doorCount.toFloat())
                            val pBot = proj(splitX, y1 + 0.4f, carcassZ0 + 3f)
                            val pTop = proj(splitX, y1 + 0.4f, carcassZ1 - 3f)
                            drawLine(revealColor, pBot, pTop, strokeWidth = 2.2f)
                        }
                    }
                }
            }
        }

        // 5. Smoked Glass Vitrine / Tall Glass Display Kelvin Warm Interior LED Glow, Shelves & Specular Glass Reflections
        if ((isWallCab || item.type == ModuleType.WALL_GLASS_80 || item.type == ModuleType.TALL_GLASS_DISPLAY_80) && !isWire &&
            (itemIsSmokedGlass || item.type == ModuleType.WALL_GLASS_80 || item.type == ModuleType.TALL_GLASS_DISPLAY_80 || isNightLed || lightSim.underCabinetLedIntensity > 0.45f)
        ) {
            val v0 = proj(x0 + 3.5f, y1 + 0.4f, carcassZ0 + 4f)
            val v1 = proj(x1 - 3.5f, y1 + 0.4f, carcassZ0 + 4f)
            val v2 = proj(x1 - 3.5f, y1 + 0.4f, carcassZ1 - 4f)
            val v3 = proj(x0 + 3.5f, y1 + 0.4f, carcassZ1 - 4f)
            val vitrinePath = Path().apply {
                moveTo(v0.x, v0.y)
                lineTo(v1.x, v1.y)
                lineTo(v2.x, v2.y)
                lineTo(v3.x, v3.y)
                close()
            }
            val vitrineGlowAlpha = (lightSim.underCabinetLedIntensity * (if (isNightLed) 0.56f else 0.36f)).coerceIn(0.10f, 0.62f)
            drawPath(
                path = vitrinePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        kelvinColor.copy(alpha = vitrineGlowAlpha),
                        Color(0xFFFF8F00).copy(alpha = vitrineGlowAlpha * 0.45f),
                        Color(0xFF10181E).copy(alpha = 0.45f)
                    ),
                    startY = min(v2.y, v3.y),
                    endY = maxOf(v0.y, v1.y)
                )
            )
            // 2 Illuminated Internal Glass Shelves inside Vitrine
            val shZ1 = carcassZ0 + (carcassZ1 - carcassZ0) * 0.35f
            val shZ2 = carcassZ0 + (carcassZ1 - carcassZ0) * 0.68f
            listOf(shZ1, shZ2).forEach { sz ->
                val sL = proj(x0 + 5f, y1 + 0.5f, sz)
                val sR = proj(x1 - 5f, y1 + 0.5f, sz)
                drawLine(kelvinColor.copy(alpha = 0.78f), sL, sR, strokeWidth = 2.0f)
            }
            // Diagonal Specular Glass Reflection Streak
            val refl1 = proj(x0 + (x1 - x0) * 0.18f, y1 + 0.6f, carcassZ0 + 10f)
            val refl2 = proj(x0 + (x1 - x0) * 0.44f, y1 + 0.6f, carcassZ1 - 8f)
            drawLine(
                Color.White.copy(alpha = (0.20f + lightSim.surfaceSpecularReflection * 0.45f).coerceIn(0.10f, 0.68f)),
                refl1,
                refl2,
                strokeWidth = 3.0f
            )
            // Anodized Aluminum Frame
            drawPath(vitrinePath, Color(0xFF90A4AE), style = Stroke(width = 2.0f))
        }

        // 6. Draw Open Drawer / Cabinet Door Preview if enabled
        if (state.doorsOpenPreview && !isWire && !isAppliance && !isFiller) {
            val pullOutDepth = 18f
            val drawerZ0 = carcassZ0 + (carcassZ1 - carcassZ0) * 0.35f
            val drawerZ1 = carcassZ0 + (carcassZ1 - carcassZ0) * 0.78f
            drawPbrIsoBox(
                x0 = x0 + 4f,
                y0 = y1,
                x1 = x1 - 4f,
                y1 = y1 + pullOutDepth,
                z0 = drawerZ0,
                z1 = drawerZ1,
                frontColor = bodyColor.lighten(0.08f),
                sideColor = Color(0xFFD7CCC8),
                topColor = Color(0xFFEFEBE9),
                strokeColor = Color.Black.copy(alpha = 0.35f),
                strokeWidth = 1.3f,
                wireframeOnly = false,
                proj = ::proj
            )
        }

        // 7. Draw Cabinet Handles / Gola Aluminum Profile on front face
        if (handleColor != Color.Transparent && !isWire && !isFiller) {
            val hZ = if (isWallCab) carcassZ0 + 6f else carcassZ0 + (carcassZ1 - carcassZ0) * 0.84f
            val hLeft = proj(x0 + (x1 - x0) * 0.25f, y1 + 0.6f, hZ)
            val hRight = proj(x0 + (x1 - x0) * 0.75f, y1 + 0.6f, hZ)
            drawLine(handleColor, hLeft, hRight, strokeWidth = 3.2f)
        }

        // 8. Draw 5cm Countertop Slab (+ Waterfall Side Slab for Islands!) with Marble Veining & Specular Gloss
        if (hasCountertop) {
            val slabColor = if (isWire) {
                Color(0xFF4FC3F7)
            } else if (isNightLed) {
                Color(itemCountertopBaseHex).darken(0.16f)
            } else {
                Color(itemCountertopBaseHex).lighten(lightSim.surfaceSpecularReflection * 0.08f)
            }
            drawPbrIsoBox(
                x0 = x0 - 1.5f,
                y0 = y0 - 1.5f,
                x1 = x1 + 1.5f,
                y1 = y1 + 1.5f,
                z0 = z1 - 5f,
                z1 = z1,
                frontColor = slabColor.darken(0.08f),
                sideColor = slabColor.darken(0.15f),
                topColor = slabColor,
                strokeColor = if (isSelected) Color(0xFFFFD54F) else Color.Black.copy(alpha = 0.25f),
                strokeWidth = if (isSelected) 3.5f else 1.2f,
                wireframeOnly = isWire,
                proj = ::proj
            )

            // Waterfall Marble Side Leg for Island Unit
            if (isIsland && !isWire) {
                drawPbrIsoBox(
                    x0 = x1 - 4.5f,
                    y0 = y0 - 1.5f,
                    x1 = x1 + 1.5f,
                    y1 = y1 + 1.5f,
                    z0 = 0f,
                    z1 = z1 - 5f,
                    frontColor = slabColor.darken(0.06f),
                    sideColor = slabColor.darken(0.12f),
                    topColor = slabColor,
                    strokeColor = Color.Black.copy(alpha = 0.20f),
                    strokeWidth = 1.1f,
                    wireframeOnly = false,
                    proj = ::proj
                )
            }

            // Realistic Multi-Branch Carrara / Calacatta Marble Veins + Specular Gloss
            if (!isWire && (itemCountertopName.contains("رخام") || itemCountertopName.contains("كوارتز"))) {
                val mv1 = proj(x0 + (x1 - x0) * 0.16f, y0 + 6f, z1 + 0.3f)
                val mv2 = proj(x0 + (x1 - x0) * 0.52f, (y0 + y1) / 2f, z1 + 0.3f)
                val mv3 = proj(x0 + (x1 - x0) * 0.84f, y1 - 6f, z1 + 0.3f)
                val mvBranch = proj(x0 + (x1 - x0) * 0.68f, y0 + 10f, z1 + 0.3f)
                drawLine(Color(0xFF90A4AE).copy(alpha = 0.48f), mv1, mv2, strokeWidth = 1.8f)
                drawLine(Color(0xFF90A4AE).copy(alpha = 0.42f), mv2, mv3, strokeWidth = 1.4f)
                drawLine(Color(0xFFD4AF37).copy(alpha = 0.28f), mv2, mvBranch, strokeWidth = 1.2f)
                // Glossy Specular Highlight Streak on Countertop controlled by surfaceSpecularReflection
                if (lightSim.surfaceSpecularReflection > 0.10f) {
                    val spec1 = proj(x0 + 8f, y0 + 8f, z1 + 0.4f)
                    val spec2 = proj(x0 + (x1 - x0) * 0.45f, y1 - 8f, z1 + 0.4f)
                    drawLine(
                        Color.White.copy(alpha = (lightSim.surfaceSpecularReflection * 0.52f).coerceIn(0.08f, 0.62f)),
                        spec1,
                        spec2,
                        strokeWidth = 2.8f
                    )
                }
            }
        }

        // 9. Detailed 3D Double Sink Basin, Drain Strainers & High-Arc Mixer Faucet
        if (item.type == ModuleType.SINK_DOUBLE && !isWire) {
            val midX = (x0 + x1) / 2f
            // Left Basin
            val lb0 = proj(x0 + 10f, y0 + 10f, z1 + 0.5f)
            val lb1 = proj(midX - 3f, y0 + 10f, z1 + 0.5f)
            val lb2 = proj(midX - 3f, y1 - 12f, z1 + 0.5f)
            val lb3 = proj(x0 + 10f, y1 - 12f, z1 + 0.5f)
            val leftBasin = Path().apply {
                moveTo(lb0.x, lb0.y); lineTo(lb1.x, lb1.y); lineTo(lb2.x, lb2.y); lineTo(lb3.x, lb3.y); close()
            }
            // Right Basin
            val rb0 = proj(midX + 3f, y0 + 10f, z1 + 0.5f)
            val rb1 = proj(x1 - 10f, y0 + 10f, z1 + 0.5f)
            val rb2 = proj(x1 - 10f, y1 - 12f, z1 + 0.5f)
            val rb3 = proj(midX + 3f, y1 - 12f, z1 + 0.5f)
            val rightBasin = Path().apply {
                moveTo(rb0.x, rb0.y); lineTo(rb1.x, rb1.y); lineTo(rb2.x, rb2.y); lineTo(rb3.x, rb3.y); close()
            }
            drawPath(leftBasin, Color(0xFF263238))
            drawPath(leftBasin, Color(0xFFB0BEC5), style = Stroke(width = 1.6f))
            drawPath(rightBasin, Color(0xFF263238))
            drawPath(rightBasin, Color(0xFFB0BEC5), style = Stroke(width = 1.6f))

            // Drain circles
            val drainL = proj((x0 + 10f + midX - 3f) / 2f, (y0 + y1) / 2f, z1 + 0.6f)
            val drainR = proj((midX + 3f + x1 - 10f) / 2f, (y0 + y1) / 2f, z1 + 0.6f)
            drawCircle(Color(0xFF90A4AE), radius = 3.2f * state.cameraZoom, center = drainL)
            drawCircle(Color(0xFF90A4AE), radius = 3.2f * state.cameraZoom, center = drainR)

            // High-arc Brushed Gold/Chrome Mixer Faucet
            val faucetBase = proj(midX, y0 + 7f, z1)
            val faucetTop = proj(midX, y0 + 7f, z1 + 28f)
            val faucetSpout = proj(midX, y0 + 23f, z1 + 23f)
            drawLine(Color(0xFFD4AF37), faucetBase, faucetTop, strokeWidth = 4.8f)
            drawLine(Color(0xFFD4AF37), faucetTop, faucetSpout, strokeWidth = 4.4f)
        }

        // 10. Detailed 3D Induction Cooktop + Built-in Oven Window + Architectural Chimney Range Hood
        if (item.type == ModuleType.STOVE_HOB && !isWire) {
            val c0 = proj(x0 + 9f, y0 + 9f, z1 + 0.5f)
            val c1 = proj(x1 - 9f, y0 + 9f, z1 + 0.5f)
            val c2 = proj(x1 - 9f, y1 - 9f, z1 + 0.5f)
            val c3 = proj(x0 + 9f, y1 - 9f, z1 + 0.5f)
            val glassPath = Path().apply {
                moveTo(c0.x, c0.y); lineTo(c1.x, c1.y); lineTo(c2.x, c2.y); lineTo(c3.x, c3.y); close()
            }
            drawPath(glassPath, Color(0xFF14181B))
            drawPath(glassPath, Color(0xFF546E7A), style = Stroke(width = 1.4f))

            val b1 = proj(x0 + 24f, y0 + 20f, z1 + 1f)
            val b2 = proj(x1 - 24f, y0 + 20f, z1 + 1f)
            val b3 = proj(x0 + 24f, y1 - 20f, z1 + 1f)
            val b4 = proj(x1 - 24f, y1 - 20f, z1 + 1f)
            listOf(b1, b2, b3, b4).forEach { center ->
                drawCircle(Color(0xFFFF5722), radius = 6.5f * state.cameraZoom, center = center, style = Stroke(width = 2.4f))
                drawCircle(Color(0xFFFFAB91), radius = 3.0f * state.cameraZoom, center = center, style = Stroke(width = 1.4f))
            }

            // Properly ordered 4-corner Front Oven Glass Window + Warm Interior Baking Glow
            val ov0 = proj(x0 + 11f, y1 + 0.6f, z0 + 16f)
            val ov1 = proj(x1 - 11f, y1 + 0.6f, z0 + 16f)
            val ov2 = proj(x1 - 11f, y1 + 0.6f, z1 - 16f)
            val ov3 = proj(x0 + 11f, y1 + 0.6f, z1 - 16f)
            val ovenWin = Path().apply {
                moveTo(ov0.x, ov0.y)
                lineTo(ov1.x, ov1.y)
                lineTo(ov2.x, ov2.y)
                lineTo(ov3.x, ov3.y)
                close()
            }
            drawPath(ovenWin, Color(0xFF101417))
            drawPath(ovenWin, Color(0xFF90A4AE), style = Stroke(width = 1.8f))
            // Glowing interior oven rack line
            val rackL = proj(x0 + 16f, y1 + 0.7f, (z0 + z1) / 2f - 4f)
            val rackR = proj(x1 - 16f, y1 + 0.7f, (z0 + z1) / 2f - 4f)
            drawLine(Color(0xFFFFB74D).copy(alpha = 0.65f), rackL, rackR, strokeWidth = 2.2f)

            // Sleek Architectural Chimney Range Hood above Stove (at z = 152cm..235cm)
            val hoodZ0 = 152f
            val hoodZ1 = 166f
            drawPbrIsoBox(
                x0 = x0 + 4f,
                y0 = y0,
                x1 = x1 - 4f,
                y1 = (y0 + 46f).coerceAtMost(y1),
                z0 = hoodZ0,
                z1 = hoodZ1,
                frontColor = Color(0xFF263238),
                sideColor = Color(0xFF1C262B),
                topColor = Color(0xFF37474F),
                strokeColor = Color(0xFF78909C),
                strokeWidth = 1.3f,
                wireframeOnly = false,
                proj = ::proj
            )
            // Hood Chimney Flue up to ceiling
            val midStoveX = (x0 + x1) / 2f
            drawPbrIsoBox(
                x0 = midStoveX - 14f,
                y0 = y0,
                x1 = midStoveX + 14f,
                y1 = y0 + 26f,
                z0 = hoodZ1,
                z1 = (roomH - 15f).coerceAtLeast(hoodZ1 + 35f),
                frontColor = Color(0xFF37474F),
                sideColor = Color(0xFF263238),
                topColor = Color(0xFF455A64),
                strokeColor = Color.Black.copy(alpha = 0.35f),
                strokeWidth = 1.1f,
                wireframeOnly = false,
                proj = ::proj
            )
        }

        // 11. Built-in Oven & Microwave Tower (TALL_OVEN_TOWER)
        if (item.type == ModuleType.TALL_OVEN_TOWER && !isWire) {
            // Lower Oven Window (z = 75..130cm) & Upper Microwave Window (z = 135..172cm)
            listOf(75f to 130f, 136f to 174f).forEach { (oz0, oz1) ->
                val p0 = proj(x0 + 8f, y1 + 0.6f, oz0)
                val p1 = proj(x1 - 8f, y1 + 0.6f, oz0)
                val p2 = proj(x1 - 8f, y1 + 0.6f, oz1)
                val p3 = proj(x0 + 8f, y1 + 0.6f, oz1)
                val path = Path().apply {
                    moveTo(p0.x, p0.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
                }
                drawPath(path, Color(0xFF101417))
                drawPath(path, Color(0xFFB0BEC5), style = Stroke(width = 1.8f))
            }
        }

        // 12. Smart Refrigerator French Doors, Dispenser Niche & Glowing Smart Screen
        if (item.type == ModuleType.FRIDGE_SMART && !isWire) {
            val midX = (x0 + x1) / 2f
            val splitBottom = proj(midX, y1 + 0.6f, z0 + 12f)
            val splitTop = proj(midX, y1 + 0.6f, z1 - 6f)
            val freezerSplitL = proj(x0 + 3f, y1 + 0.6f, z0 + 68f)
            val freezerSplitR = proj(x1 - 3f, y1 + 0.6f, z0 + 68f)
            drawLine(Color(0xFF90A4AE), splitBottom, splitTop, strokeWidth = 2.4f)
            drawLine(Color(0xFF90A4AE), freezerSplitL, freezerSplitR, strokeWidth = 2.2f)

            // Smart Touchscreen / Ice Dispenser Panel on Right Door
            val sc0 = proj(midX + 8f, y1 + 0.8f, z0 + 105f)
            val sc1 = proj(x1 - 10f, y1 + 0.8f, z0 + 105f)
            val sc2 = proj(x1 - 10f, y1 + 0.8f, z0 + 152f)
            val sc3 = proj(midX + 8f, y1 + 0.8f, z0 + 152f)
            val screenPath = Path().apply {
                moveTo(sc0.x, sc0.y); lineTo(sc1.x, sc1.y); lineTo(sc2.x, sc2.y); lineTo(sc3.x, sc3.y); close()
            }
            drawPath(screenPath, Color(0xFF0D1B2A))
            drawPath(screenPath, Color(0xFF00E5FF).copy(alpha = 0.65f), style = Stroke(width = 1.5f))
        }

        // 13. Real-Time 3D Red Bounding Box around any cabinet violating intersection or minimum distance rules
        if (item.uid in state.complianceReport.violatingUnitUids) {
            val rf0 = proj(x0 - 2f, y0 - 2f, 0.6f)
            val rf1 = proj(x1 + 2f, y0 - 2f, 0.6f)
            val rf2 = proj(x1 + 2f, y1 + 2f, 0.6f)
            val rf3 = proj(x0 - 2f, y1 + 2f, 0.6f)
            val redFloorBox = Path().apply {
                moveTo(rf0.x, rf0.y); lineTo(rf1.x, rf1.y); lineTo(rf2.x, rf2.y); lineTo(rf3.x, rf3.y); close()
            }
            drawPath(redFloorBox, Color(0xFFFF1744).copy(alpha = 0.25f))
            drawPath(redFloorBox, Color(0xFFFF1744), style = Stroke(width = 3.8f))

            drawPbrIsoBox(
                x0 = x0 - 1.5f,
                y0 = y0 - 1.5f,
                x1 = x1 + 1.5f,
                y1 = y1 + 1.5f,
                z0 = z0,
                z1 = z1 + 1.5f,
                frontColor = Color(0xFFFF1744).copy(alpha = 0.22f),
                sideColor = Color(0xFFD50000).copy(alpha = 0.22f),
                topColor = Color(0xFFFF5252).copy(alpha = 0.28f),
                strokeColor = Color(0xFFFF1744),
                strokeWidth = 4.0f,
                wireframeOnly = true,
                cameraYawDeg = state.cameraYawDeg,
                proj = ::proj
            )
        }
    }

    // 4. Draw 3D Work Triangle floating laser line at counter height (z = 94cm)
    if (state.showWorkTriangle) {
        val sink = state.placedItems.firstOrNull { it.type == ModuleType.SINK_DOUBLE }
        val stove = state.placedItems.firstOrNull {
            it.type == ModuleType.STOVE_HOB || it.type == ModuleType.TALL_OVEN_TOWER
        }
        val fridge = state.placedItems.firstOrNull { it.type == ModuleType.FRIDGE_SMART }

        if (sink != null && stove != null && fridge != null) {
            val pSink = proj(sink.centerXCm, sink.centerYCm, 94f)
            val pStove = proj(stove.centerXCm, stove.centerYCm, 94f)
            val pFridge = proj(fridge.centerXCm, fridge.centerYCm, 94f)

            val triPath = Path().apply {
                moveTo(pSink.x, pSink.y)
                lineTo(pStove.x, pStove.y)
                lineTo(pFridge.x, pFridge.y)
                close()
            }
            drawPath(triPath, Color(0xFF00E676).copy(alpha = 0.15f))
            drawPath(
                triPath,
                Color(0xFF00E676),
                style = Stroke(
                    width = 3.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            )
            listOf(pSink, pStove, pFridge).forEach { pt ->
                drawCircle(Color(0xFF00E676), radius = 8f, center = pt)
            }
        }
    }

    // 5. Live 3D Drag-and-Drop Ghost Preview with Floor Target Footprint & Real-Time Red Box on Violation
    if (previewDragItem != null) {
        val gx0 = previewDragItem.xMm / 10f
        val gy0 = previewDragItem.yMm / 10f
        val gx1 = gx0 + previewDragItem.effectiveWidthMm / 10f
        val gy1 = gy0 + previewDragItem.effectiveDepthMm / 10f
        val gz0 = previewDragItem.elevationMm / 10f
        val gz1 = gz0 + previewDragItem.heightMm / 10f

        val previewReport3D = EngineeringComplianceAnalyzer.validateLayout(
            items = state.placedItems.filter { it.uid != previewDragItem.uid } + previewDragItem,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm
        )
        val isPreviewViolating3D = previewDragItem.uid in previewReport3D.violatingUnitUids
        val footprintStrokeColor = if (isPreviewViolating3D) Color(0xFFFF1744) else Color(0xFF00E676)

        // Glowing Floor Drop Target Footprint
        val fp0 = proj(gx0, gy0, 0.5f)
        val fp1 = proj(gx1, gy0, 0.5f)
        val fp2 = proj(gx1, gy1, 0.5f)
        val fp3 = proj(gx0, gy1, 0.5f)
        val footprintPath = Path().apply {
            moveTo(fp0.x, fp0.y)
            lineTo(fp1.x, fp1.y)
            lineTo(fp2.x, fp2.y)
            lineTo(fp3.x, fp3.y)
            close()
        }
        drawPath(footprintPath, footprintStrokeColor.copy(alpha = 0.28f))
        drawPath(
            footprintPath,
            footprintStrokeColor,
            style = Stroke(
                width = 3.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
            )
        )

        // Translucent 3D Ghost Box (Red Box if violating)
        drawPbrIsoBox(
            x0 = gx0,
            y0 = gy0,
            x1 = gx1,
            y1 = gy1,
            z0 = gz0,
            z1 = gz1,
            frontColor = if (isPreviewViolating3D) Color(0xFFFF1744).copy(alpha = 0.55f) else Color(0xFF00E5FF).copy(alpha = 0.55f),
            sideColor = if (isPreviewViolating3D) Color(0xFFD50000).copy(alpha = 0.50f) else Color(0xFF00B8D4).copy(alpha = 0.50f),
            topColor = if (isPreviewViolating3D) Color(0xFFFF5252).copy(alpha = 0.65f) else Color(0xFF69F0AE).copy(alpha = 0.65f),
            strokeColor = footprintStrokeColor,
            strokeWidth = if (isPreviewViolating3D) 4.4f else 3.5f,
            wireframeOnly = true,
            proj = ::proj
        )

        val labelPos = proj((gx0 + gx1) / 2f, (gy0 + gy1) / 2f, gz1 + 12f)
        val ghostPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (isPreviewViolating3D) android.graphics.Color.parseColor("#B71C1C") else android.graphics.Color.parseColor("#004D40")
        }
        drawDistanceBadge(
            center = labelPos,
            text = if (isPreviewViolating3D) "⚠️ تقاطع / مسافة غير كافية!" else "${previewDragItem.customLabel.take(12)} (${previewDragItem.xMm},${previewDragItem.yMm}mm)",
            paint = ghostPaint
        )
    }
}

/**
 * Simple Live 3D Mini-Preview Canvas driven directly by the current unit coordinates in the workspace.
 * Allows users working in 2D Blueprint mode (or inspecting the floating workspace) to continuously
 * see the live 3D isometric kitchen layout alongside any real-time red collision boxes.
 */
@Composable
fun Simple3DKitchenMiniPreviewCanvas(
    state: PlannerStudioState,
    modifier: Modifier = Modifier
) {
    val mini3DState = remember(state) {
        state.copy(
            viewMode = StudioViewMode.RENDER_3D,
            cameraZoom = 0.92f
        )
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF162228), Color(0xFF22333B))
                )
            )
            .testTag("simple_3d_mini_preview_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            draw3DPhotorealisticScene(mini3DState, null)
        }
    }
}

/**
 * Complete 6-Sided 3D Isometric / Perspective Box Renderer:
 * Draws all 6 faces (`Bottom`, `Back`, `Left Side`, `Right Side`, `Front`, `Top`)
 * dynamically depth-sorted based on `cameraYawDeg` so rotating the 3D scene 0°..360°
 * always shows a solid, enclosed cabinet with full back and full left/right side panels.
 */
private fun DrawScope.drawPbrIsoBox(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    z0: Float,
    z1: Float,
    frontColor: Color,
    sideColor: Color,
    topColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
    wireframeOnly: Boolean,
    cameraYawDeg: Float = -34f,
    proj: (Float, Float, Float) -> Offset
) {
    val b0 = proj(x0, y0, z0)
    val b1 = proj(x1, y0, z0)
    val b2 = proj(x1, y1, z0)
    val b3 = proj(x0, y1, z0)

    val t0 = proj(x0, y0, z1)
    val t1 = proj(x1, y0, z1)
    val t2 = proj(x1, y1, z1)
    val t3 = proj(x0, y1, z1)

    val faceBottom = Path().apply {
        moveTo(b0.x, b0.y); lineTo(b1.x, b1.y); lineTo(b2.x, b2.y); lineTo(b3.x, b3.y); close()
    }
    val faceBack = Path().apply {
        moveTo(b0.x, b0.y); lineTo(b1.x, b1.y); lineTo(t1.x, t1.y); lineTo(t0.x, t0.y); close()
    }
    val faceLeft = Path().apply {
        moveTo(b0.x, b0.y); lineTo(b3.x, b3.y); lineTo(t3.x, t3.y); lineTo(t0.x, t0.y); close()
    }
    val faceRight = Path().apply {
        moveTo(b1.x, b1.y); lineTo(b2.x, b2.y); lineTo(t2.x, t2.y); lineTo(t1.x, t1.y); close()
    }
    val faceFront = Path().apply {
        moveTo(b3.x, b3.y); lineTo(b2.x, b2.y); lineTo(t2.x, t2.y); lineTo(t3.x, t3.y); close()
    }
    val faceTop = Path().apply {
        moveTo(t0.x, t0.y); lineTo(t1.x, t1.y); lineTo(t2.x, t2.y); lineTo(t3.x, t3.y); close()
    }

    // Painter's depth sorting of the 4 vertical faces using camera yaw
    val rad = Math.toRadians(cameraYawDeg.toDouble())
    val sinA = sin(rad).toFloat()
    val cosA = cos(rad).toFloat()
    val midX = (x0 + x1) / 2f
    val midY = (y0 + y1) / 2f

    data class VerticalFaceEntry(
        val path: Path,
        val depthScore: Float,
        val baseColor: Color,
        val gradStart: Offset,
        val gradEnd: Offset
    )

    val verticalFaces = listOf(
        // Back face (y = y0)
        VerticalFaceEntry(
            path = faceBack,
            depthScore = midX * sinA + y0 * cosA,
            baseColor = sideColor.darken(0.14f),
            gradStart = t0,
            gradEnd = b1
        ),
        // Left side panel (x = x0)
        VerticalFaceEntry(
            path = faceLeft,
            depthScore = x0 * sinA + midY * cosA,
            baseColor = sideColor.darken(0.06f),
            gradStart = t0,
            gradEnd = b3
        ),
        // Right side panel (x = x1)
        VerticalFaceEntry(
            path = faceRight,
            depthScore = x1 * sinA + midY * cosA,
            baseColor = sideColor,
            gradStart = t1,
            gradEnd = b2
        ),
        // Front face (y = y1)
        VerticalFaceEntry(
            path = faceFront,
            depthScore = midX * sinA + y1 * cosA,
            baseColor = frontColor,
            gradStart = t3,
            gradEnd = b2
        )
    ).sortedBy { it.depthScore }

    val stroke = Stroke(width = strokeWidth)

    if (!wireframeOnly) {
        // 1. Bottom base panel
        drawPath(faceBottom, sideColor.darken(0.22f))
        drawPath(faceBottom, strokeColor.copy(alpha = 0.45f), style = stroke)

        // 2. All 4 vertical faces (Back, Left Side, Right Side, Front) ordered back-to-front
        verticalFaces.forEach { vf ->
            drawPath(
                path = vf.path,
                brush = Brush.linearGradient(
                    colors = listOf(vf.baseColor.lighten(0.08f), vf.baseColor, vf.baseColor.darken(0.12f)),
                    start = vf.gradStart,
                    end = vf.gradEnd
                )
            )
            drawPath(vf.path, strokeColor, style = stroke)
        }

        // 3. Top panel
        drawPath(
            path = faceTop,
            brush = Brush.linearGradient(
                colors = listOf(topColor.lighten(0.14f), topColor, topColor.darken(0.06f)),
                start = t0,
                end = t2
            )
        )
        drawPath(faceTop, strokeColor, style = stroke)

        // Subtle Fresnel top bevel specular highlight on front-top edge
        drawLine(
            color = Color.White.copy(alpha = 0.28f),
            start = t3,
            end = t2,
            strokeWidth = 1.6f
        )
    } else {
        drawPath(faceBottom, sideColor.copy(alpha = 0.18f))
        verticalFaces.forEach { vf ->
            drawPath(vf.path, vf.baseColor.copy(alpha = 0.28f))
            drawPath(vf.path, strokeColor, style = stroke)
        }
        drawPath(faceTop, topColor.copy(alpha = 0.45f))
        drawPath(faceTop, strokeColor, style = stroke)
    }
}

private fun DrawScope.drawVrNavigationHudOverlay(
    state: PlannerStudioState,
    isStereoscopic: Boolean
) {
    val cyanHud = Color(0xFF00E5FF)
    val greenHud = Color(0xFF00E676)

    if (isStereoscopic) {
        val halfW = size.width / 2f
        // Center Stereoscopic Bridge Line
        drawLine(
            color = cyanHud.copy(alpha = 0.75f),
            start = Offset(halfW, 0f),
            end = Offset(halfW, size.height),
            strokeWidth = 4f
        )
        // Left & Right VR Lens Framing Borders
        drawRoundRect(
            color = cyanHud.copy(alpha = 0.55f),
            topLeft = Offset(10f, 12f),
            size = Size(halfW - 18f, size.height - 24f),
            cornerRadius = CornerRadius(42f, 42f),
            style = Stroke(width = 3f)
        )
        drawRoundRect(
            color = cyanHud.copy(alpha = 0.55f),
            topLeft = Offset(halfW + 8f, 12f),
            size = Size(halfW - 18f, size.height - 24f),
            cornerRadius = CornerRadius(42f, 42f),
            style = Stroke(width = 3f)
        )
        // Left & Right Aiming Reticles
        listOf(halfW * 0.5f, halfW * 1.5f).forEach { rx ->
            val ry = size.height * 0.5f
            drawCircle(
                color = cyanHud.copy(alpha = 0.75f),
                radius = 16f,
                center = Offset(rx, ry),
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = greenHud,
                radius = 3.5f,
                center = Offset(rx, ry)
            )
        }
    } else {
        // Single-Screen VR Reticle + Horizon Tilt Bar + Top Compass Ribbon
        val cx = size.width / 2f
        val cy = size.height * 0.5f

        // Center Gaze Crosshair Reticle
        drawCircle(
            color = cyanHud.copy(alpha = 0.72f),
            radius = 22f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )
        drawCircle(
            color = greenHud,
            radius = 4f,
            center = Offset(cx, cy)
        )
        drawLine(cyanHud.copy(alpha = 0.65f), Offset(cx - 34f, cy), Offset(cx - 12f, cy), strokeWidth = 2f)
        drawLine(cyanHud.copy(alpha = 0.65f), Offset(cx + 12f, cy), Offset(cx + 34f, cy), strokeWidth = 2f)
        drawLine(cyanHud.copy(alpha = 0.65f), Offset(cx, cy - 34f), Offset(cx, cy - 12f), strokeWidth = 2f)
        drawLine(cyanHud.copy(alpha = 0.65f), Offset(cx, cy + 12f), Offset(cx, cy + 34f), strokeWidth = 2f)

        // Optical Flow Velocity Vector Indicator from Center Reticle
        val flowEndX = (cx + state.vrSensorDeltaFlowX * 38f).coerceIn(cx - 90f, cx + 90f)
        val flowEndY = (cy + state.vrSensorDeltaFlowY * 38f).coerceIn(cy - 90f, cy + 90f)
        if (hypot(flowEndX - cx, flowEndY - cy) > 3f) {
            drawLine(
                color = Color(0xFFFFD54F),
                start = Offset(cx, cy),
                end = Offset(flowEndX, flowEndY),
                strokeWidth = 3.5f
            )
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 5f,
                center = Offset(flowEndX, flowEndY)
            )
        }
    }
}

private fun Color.darken(factor: Float): Color {
    val f = (1f - factor).coerceIn(0f, 1f)
    return Color(red = red * f, green = green * f, blue = blue * f, alpha = alpha)
}

private fun Color.lighten(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red = (red + (1f - red) * f).coerceIn(0f, 1f),
        green = (green + (1f - green) * f).coerceIn(0f, 1f),
        blue = (blue + (1f - blue) * f).coerceIn(0f, 1f),
        alpha = alpha
    )
}
