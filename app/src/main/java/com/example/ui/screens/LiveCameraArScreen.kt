package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.cam.JsonCadEngine
import com.example.data.cam.ProjectExportManager
import com.example.data.model.KitchenCatalogData
import com.example.data.model.KitchenCatalogUnit
import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.ui.components.CatalogDragSession
import com.example.ui.components.DraggableCatalogUnitCard
import com.example.ui.components.FloatingCatalogDragGhostOverlay
import com.example.ui.components.FloatingCollapsibleWindow
import com.example.ui.components.FloatingMenuToggleItem
import com.example.ui.components.FloatingMenusMasterDockBar
import com.example.ui.viewmodel.KitchenPlannerViewModel
import com.example.ui.viewmodel.PlannerStudioState
import com.example.ui.viewmodel.UiState
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ArInteractionTool(val titleAr: String) {
    PLACE_AND_MOVE_3D("تحريك خزائن 3D باللمس"),
    AR_MATERIAL_EDITOR("تعديل الخامة بالواقع المعزز + JSON"),
    CALIBRATE_4_PINS("معايرة المنظور 4-Pin"),
    WALL_INPAINTING("مسح الحائط الذكي Inpainting")
}

@Composable
fun LiveCameraArScreen(
    state: PlannerStudioState,
    catalogUnitsState: UiState<List<KitchenCatalogUnit>>,
    viewModel: KitchenPlannerViewModel,
    onOpenJsonTab: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var activeTool by remember { mutableStateOf(ArInteractionTool.PLACE_AND_MOVE_3D) }
    var showCameraToJsonDialog by remember { mutableStateOf(false) }
    var showUniversalJsonDialog by remember { mutableStateOf(false) }

    // Floating Collapsible Menus State for AR
    var allArMenusHidden by remember { mutableStateOf(false) }
    var showArToolsFloatingMenu by remember { mutableStateOf(true) }
    var showArMaterialJsonFloatingMenu by remember { mutableStateOf(true) }
    var showArHorizonFloatingMenu by remember { mutableStateOf(false) }
    var showArCatalogFloatingMenu by remember { mutableStateOf(true) }

    var pinTopLeft by remember { mutableStateOf(Offset(0.16f, 0.56f)) }
    var pinTopRight by remember { mutableStateOf(Offset(0.84f, 0.56f)) }
    var pinBottomRight by remember { mutableStateOf(Offset(0.96f, 0.92f)) }
    var pinBottomLeft by remember { mutableStateOf(Offset(0.04f, 0.92f)) }
    var horizonRatioY by remember { mutableFloatStateOf(0.56f) }

    val inpaintedWallPatches = remember {
        mutableStateListOf(
            Rect(0.22f, 0.25f, 0.58f, 0.54f)
        )
    }
    var dragInpaintStart by remember { mutableStateOf<Offset?>(null) }
    var dragInpaintCurrent by remember { mutableStateOf<Offset?>(null) }

    // Drag-and-Drop Session State for AR Viewport
    var rootBoxPosition by remember { mutableStateOf(Offset.Zero) }
    var arViewportRootRect by remember { mutableStateOf(Rect.Zero) }
    var arViewportSizePx by remember { mutableStateOf(IntSize.Zero) }
    var activeArDragSession by remember { mutableStateOf<CatalogDragSession?>(null) }

    val catalogUnits = when (catalogUnitsState) {
        is UiState.Success -> catalogUnitsState.data
        else -> ModuleType.entries.map { it.toDefaultCatalogUnit() }
    }

    val currentState by rememberUpdatedState(state)

    fun updateArUnitDragPosition(unit: KitchenCatalogUnit, pointerRoot: Offset) {
        val isInsideAr = arViewportRootRect.width > 0f &&
            pointerRoot.x in (arViewportRootRect.left - 30f)..(arViewportRootRect.right + 30f) &&
            pointerRoot.y in (arViewportRootRect.top - 30f)..(arViewportRootRect.bottom + 30f)

        val previewItem = if (isInsideAr) {
            val localX = (pointerRoot.x - arViewportRootRect.left).coerceIn(0f, arViewportRootRect.width)
            val localY = (pointerRoot.y - arViewportRootRect.top).coerceIn(0f, arViewportRootRect.height)
            val w = arViewportSizePx.width.toFloat().coerceAtLeast(100f)
            val h = arViewportSizePx.height.toFloat().coerceAtLeast(100f)
            val wallLeftPx = pinTopLeft.x * w
            val wallRightPx = pinTopRight.x * w
            val wallSpanPx = (wallRightPx - wallLeftPx).coerceAtLeast(120f)
            val scaleMmToPx = wallSpanPx / state.roomWidthMm.toFloat()

            val unitW = (unit.widthCm.toInt() * 10).coerceAtLeast(100)
            val unitD = (unit.depthCm.toInt() * 10).coerceAtLeast(250)
            val rawXMm = (((localX - wallLeftPx) / scaleMmToPx) - unitW / 2f)
                .roundToInt()
                .coerceIn(0, (state.roomWidthMm - unitW).coerceAtLeast(0))

            val horizonY = h * horizonRatioY
            val floorBottomY = pinBottomLeft.y * h
            val rawYMm = if (unit.resolvedModuleType.category == ModuleCategory.WALL_CABINET || localY <= horizonY + 24f) {
                0
            } else {
                val ratio = ((localY - horizonY) / (floorBottomY - horizonY).coerceAtLeast(40f)).coerceIn(0f, 1f)
                (ratio * (state.roomDepthMm - unitD)).roundToInt()
            }
            viewModel.computeSnappedDropPreview(unit, rawXMm, rawYMm)
        } else {
            null
        }

        activeArDragSession = CatalogDragSession(
            unit = unit,
            pointerRootOffset = pointerRoot,
            isOverCanvas = isInsideAr,
            previewPlacedItem = previewItem
        )
    }

    fun completeArCatalogDragSession() {
        val session = activeArDragSession
        activeArDragSession = null
        if (session != null && session.isOverCanvas && session.unit != null && session.previewPlacedItem != null) {
            viewModel.dropCatalogUnitAtPositionMm(
                session.unit,
                session.previewPlacedItem.xMm,
                session.previewPlacedItem.yMm
            )
        }
    }

    val isDraggingInAr = activeArDragSession != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootBoxPosition = coords.positionInRoot()
            }
    ) {
        // =========================================================================
        // 1. FULL-SCREEN LIVE CAMERA PREVIEW + INTERACTIVE AR 3D OVERLAY VIEWPORT
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2B3236), Color(0xFF3E464B), Color(0xFF1F2427))
                    )
                )
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    arViewportSizePx = coords.size
                    arViewportRootRect = Rect(
                        offset = pos,
                        size = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                    )
                }
                .testTag("live_camera_ar_viewport")
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                runCatching {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = surfaceProvider
                                    }
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview
                                    )
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // AR Canvas Layer: Wall Inpainting Patches + 4-Pin Floor Calibration Grid + Realistic PBR 3D Cabinets
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeTool) {
                        detectTapGestures { tap ->
                            if (activeTool == ArInteractionTool.PLACE_AND_MOVE_3D ||
                                activeTool == ArInteractionTool.AR_MATERIAL_EDITOR
                            ) {
                                val s = currentState
                                val wallLeft = size.width * pinTopLeft.x
                                val wallRight = size.width * pinTopRight.x
                                val wallWidthPx = (wallRight - wallLeft).coerceAtLeast(120f)
                                val scaleMmToPx = wallWidthPx / s.roomWidthMm.toFloat()
                                val floorHorizonY = size.height * horizonRatioY

                                val hit = s.placedItems.lastOrNull { item ->
                                    val leftPx = wallLeft + item.xMm * scaleMmToPx
                                    val rightPx = leftPx + item.effectiveWidthMm * scaleMmToPx
                                    val bottomPx = floorHorizonY - item.elevationMm * scaleMmToPx +
                                        (item.yMm / s.roomDepthMm.toFloat()) * (size.height * 0.25f)
                                    val topPx = bottomPx - item.heightMm * scaleMmToPx
                                    tap.x in leftPx..rightPx && tap.y in topPx..bottomPx
                                }
                                if (hit != null) {
                                    viewModel.selectItem(hit.uid)
                                    if (activeTool == ArInteractionTool.AR_MATERIAL_EDITOR) {
                                        showArMaterialJsonFloatingMenu = true
                                    }
                                } else if (activeTool == ArInteractionTool.PLACE_AND_MOVE_3D) {
                                    viewModel.selectItem(null)
                                }
                            }
                        }
                    }
                    .pointerInput(activeTool) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val norm = Offset(
                                    (startOffset.x / size.width.coerceAtLeast(1)).coerceIn(0.02f, 0.98f),
                                    (startOffset.y / size.height.coerceAtLeast(1)).coerceIn(0.05f, 0.96f)
                                )
                                if (activeTool == ArInteractionTool.WALL_INPAINTING) {
                                    dragInpaintStart = norm
                                    dragInpaintCurrent = norm
                                }
                            },
                            onDragEnd = {
                                if (activeTool == ArInteractionTool.WALL_INPAINTING) {
                                    val s = dragInpaintStart
                                    val e = dragInpaintCurrent
                                    if (s != null && e != null) {
                                        val rect = Rect(
                                            left = min(s.x, e.x),
                                            top = min(s.y, e.y),
                                            right = max(s.x, e.x).coerceAtLeast(min(s.x, e.x) + 0.08f),
                                            bottom = max(s.y, e.y).coerceAtLeast(min(s.y, e.y) + 0.08f)
                                        )
                                        inpaintedWallPatches.add(rect)
                                    }
                                    dragInpaintStart = null
                                    dragInpaintCurrent = null
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val posNorm = Offset(
                                    (change.position.x / w).coerceIn(0.02f, 0.98f),
                                    (change.position.y / h).coerceIn(0.05f, 0.96f)
                                )

                                when (activeTool) {
                                    ArInteractionTool.CALIBRATE_4_PINS -> {
                                        val pins = listOf(pinTopLeft, pinTopRight, pinBottomRight, pinBottomLeft)
                                        val closestIdx = pins.indices.minByOrNull { idx ->
                                            hypot(pins[idx].x - posNorm.x, pins[idx].y - posNorm.y)
                                        } ?: 0
                                        when (closestIdx) {
                                            0 -> {
                                                pinTopLeft = posNorm
                                                horizonRatioY = posNorm.y.coerceIn(0.30f, 0.78f)
                                            }
                                            1 -> {
                                                pinTopRight = posNorm
                                                horizonRatioY = posNorm.y.coerceIn(0.30f, 0.78f)
                                            }
                                            2 -> pinBottomRight = posNorm
                                            3 -> pinBottomLeft = posNorm
                                        }
                                    }

                                    ArInteractionTool.WALL_INPAINTING -> {
                                        dragInpaintCurrent = posNorm
                                    }

                                    ArInteractionTool.PLACE_AND_MOVE_3D,
                                    ArInteractionTool.AR_MATERIAL_EDITOR -> {
                                        if (activeTool == ArInteractionTool.PLACE_AND_MOVE_3D) {
                                            val s = currentState
                                            val selectedId = s.selectedItemUid ?: return@detectDragGestures
                                            val selected = s.placedItems.firstOrNull { it.uid == selectedId }
                                                ?: return@detectDragGestures
                                            val wallWidthPx = (w * (pinTopRight.x - pinTopLeft.x)).coerceAtLeast(120f)
                                            val mmPerPx = s.roomWidthMm / wallWidthPx
                                            val dxMm = (dragAmount.x * mmPerPx).roundToInt()
                                            val dyMm = (dragAmount.y * mmPerPx * 0.65f).roundToInt()
                                            if (abs(dxMm) >= 1 || abs(dyMm) >= 1) {
                                                viewModel.updateItemPositionDirectMm(
                                                    selectedId,
                                                    selected.xMm + dxMm,
                                                    selected.yMm + dyMm
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val horizonY = h * horizonRatioY

                if (!hasCameraPermission) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFD7D2C8), Color(0xFFC5BEB2)),
                            startY = 0f,
                            endY = horizonY
                        ),
                        topLeft = Offset.Zero,
                        size = Size(w, horizonY)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF8D7B68), Color(0xFF5D4E3E)),
                            startY = horizonY,
                            endY = h
                        ),
                        topLeft = Offset(0f, horizonY),
                        size = Size(w, h - horizonY)
                    )
                }

                // 1. Smart Wall Inpainting Cleaned Patches
                val cleanWallColor = Color(KitchenCatalogData.resolveWallColorHex(state.wallColorHex))
                inpaintedWallPatches.forEach { normRect ->
                    val rLeft = normRect.left * w
                    val rTop = normRect.top * h
                    val rW = (normRect.right - normRect.left) * w
                    val rH = (normRect.bottom - normRect.top) * h
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(cleanWallColor, cleanWallColor.copy(alpha = 0.95f))
                        ),
                        topLeft = Offset(rLeft, rTop),
                        size = Size(rW, rH),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    if (activeTool == ArInteractionTool.WALL_INPAINTING) {
                        drawRoundRect(
                            color = Color(0xFF00E676),
                            topLeft = Offset(rLeft, rTop),
                            size = Size(rW, rH),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                            )
                        )
                    }
                }

                val activeStart = dragInpaintStart
                val activeEnd = dragInpaintCurrent
                if (activeStart != null && activeEnd != null) {
                    val left = min(activeStart.x, activeEnd.x) * w
                    val top = min(activeStart.y, activeEnd.y) * h
                    val rw = abs(activeEnd.x - activeStart.x) * w
                    val rh = abs(activeEnd.y - activeStart.y) * h
                    drawRect(
                        color = Color(0xFF00E676).copy(alpha = 0.35f),
                        topLeft = Offset(left, top),
                        size = Size(rw, rh)
                    )
                }

                // 2. 4-Pin Floor Calibration Perspective Quadrilateral & Horizon Line
                val pTL = Offset(pinTopLeft.x * w, horizonY)
                val pTR = Offset(pinTopRight.x * w, horizonY)
                val pBR = Offset(pinBottomRight.x * w, pinBottomRight.y * h)
                val pBL = Offset(pinBottomLeft.x * w, pinBottomLeft.y * h)

                val floorQuad = Path().apply {
                    moveTo(pTL.x, pTL.y)
                    lineTo(pTR.x, pTR.y)
                    lineTo(pBR.x, pBR.y)
                    lineTo(pBL.x, pBL.y)
                    close()
                }
                drawPath(
                    path = floorQuad,
                    color = Color(0xFF00E5FF).copy(alpha = 0.14f)
                )
                drawPath(
                    path = floorQuad,
                    color = Color(0xFF00E5FF),
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                )

                listOf(pTL, pTR, pBR, pBL).forEach { pt ->
                    drawCircle(color = Color(0xFF00E5FF), radius = 11f, center = pt)
                    drawCircle(color = Color(0xFF102027), radius = 5f, center = pt)
                }

                // 3. Project Photorealistic 3D Kitchen Units onto Calibrated Live Camera Floor & Wall
                val wallSpanPx = (pTR.x - pTL.x).coerceAtLeast(120f)
                val scaleMmToPx = wallSpanPx / state.roomWidthMm.toFloat()

                val dimPaint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#FFE082")
                }

                val finishBadgePaint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    textSize = 16f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#80DEEA")
                }

                val itemsWithPreview = buildList {
                    addAll(state.placedItems)
                    activeArDragSession?.previewPlacedItem?.let { add(it) }
                }

                itemsWithPreview
                    .sortedWith(compareBy({ it.yMm }, { it.elevationMm }))
                    .forEach { item ->
                        val isGhost = item.uid == "drag_preview_ghost"
                        val itemFinishAr = item.customCabinetFinish ?: state.cabinetFinish
                        val itemCountertopAr = item.customCountertopMaterial ?: state.countertopMaterial
                        val cabColor = Color(KitchenCatalogData.resolveCabinetColorHex(itemFinishAr))
                        val topColor = Color(KitchenCatalogData.resolveCountertopColorHex(itemCountertopAr))
                        val isSmokedGlass = itemFinishAr.contains("زجاج")

                        val depthRatio = (item.yMm / state.roomDepthMm.toFloat()).coerceIn(0f, 1f)
                        val perspectiveScale = 1f + depthRatio * 0.22f
                        val leftPx = pTL.x + item.xMm * scaleMmToPx
                        val unitWidthPx = item.effectiveWidthMm * scaleMmToPx * perspectiveScale
                        val unitHeightPx = item.heightMm * scaleMmToPx * perspectiveScale
                        val baseFloorY = horizonY + depthRatio * (pBL.y - horizonY)
                        val bottomPx = baseFloorY - item.elevationMm * scaleMmToPx
                        val topPx = bottomPx - unitHeightPx
                        val depthOffsetPx = (item.effectiveDepthMm * scaleMmToPx * 0.28f)

                        val isSelected = item.uid == state.selectedItemUid
                        val isWall = item.type.category == ModuleCategory.WALL_CABINET

                        // AR Soft Ground Contact Shadow
                        if (!isWall) {
                            drawRoundRect(
                                color = if (isGhost) {
                                    Color(0xFF00E676).copy(alpha = 0.42f)
                                } else {
                                    Color.Black.copy(alpha = 0.36f)
                                },
                                topLeft = Offset(leftPx + 3f, bottomPx - 4f),
                                size = Size(unitWidthPx + depthOffsetPx * 0.7f, 11f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }

                        val unitFill = when {
                            isGhost -> Color(0xFF00E5FF).copy(alpha = 0.48f)
                            item.type == ModuleType.FILLER_STRIP -> Color(0xFFD84315).copy(alpha = 0.92f)
                            (isWall && isSmokedGlass) || item.type == ModuleType.WALL_GLASS_80 ->
                                Color(0xFF1A2228).copy(alpha = 0.86f)
                            item.type.category == ModuleCategory.APPLIANCE -> Color(0xFF37474F).copy(alpha = 0.94f)
                            else -> cabColor.copy(alpha = 0.94f)
                        }

                        // 3D Side Extrusion Face
                        val sidePath = Path().apply {
                            moveTo(leftPx + unitWidthPx, topPx)
                            lineTo(leftPx + unitWidthPx + depthOffsetPx, topPx - depthOffsetPx * 0.45f)
                            lineTo(leftPx + unitWidthPx + depthOffsetPx, bottomPx - depthOffsetPx * 0.45f)
                            lineTo(leftPx + unitWidthPx, bottomPx)
                            close()
                        }
                        drawPath(sidePath, unitFill.copy(alpha = 0.76f))

                        // 3D Top Face / Marble Countertop Slab
                        val topPath = Path().apply {
                            moveTo(leftPx, topPx)
                            lineTo(leftPx + depthOffsetPx, topPx - depthOffsetPx * 0.45f)
                            lineTo(leftPx + unitWidthPx + depthOffsetPx, topPx - depthOffsetPx * 0.45f)
                            lineTo(leftPx + unitWidthPx, topPx)
                            close()
                        }
                        drawPath(
                            topPath,
                            if (item.type.countertopIncluded && !isGhost) topColor else unitFill.copy(alpha = 0.86f)
                        )

                        // Main Front Face with PBR Vertical Gradient
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    unitFill,
                                    unitFill.copy(
                                        red = (unitFill.red * 0.86f).coerceIn(0f, 1f),
                                        green = (unitFill.green * 0.86f).coerceIn(0f, 1f),
                                        blue = (unitFill.blue * 0.86f).coerceIn(0f, 1f)
                                    )
                                ),
                                startY = topPx,
                                endY = bottomPx
                            ),
                            topLeft = Offset(leftPx, topPx),
                            size = Size(unitWidthPx, unitHeightPx),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Recessed 10cm Plinth for Base Units
                        if (!isWall && !isGhost && item.elevationMm == 0 && item.type != ModuleType.FILLER_STRIP) {
                            val plinthH = (unitHeightPx * 0.11f).coerceIn(6f, 16f)
                            drawRect(
                                color = Color(0xFF141A1D),
                                topLeft = Offset(leftPx + 2f, bottomPx - plinthH),
                                size = Size((unitWidthPx - 4f).coerceAtLeast(4f), plinthH)
                            )
                        }

                        // Warm 3000K Vitrine LED & Shelves for Wall Cabinets
                        if (isWall && !isGhost) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFCA28).copy(alpha = 0.36f),
                                        Color(0xFFFF8F00).copy(alpha = 0.14f)
                                    ),
                                    startY = topPx,
                                    endY = bottomPx
                                ),
                                topLeft = Offset(leftPx + 5f, topPx + 5f),
                                size = Size(
                                    (unitWidthPx - 10f).coerceAtLeast(4f),
                                    (unitHeightPx - 10f).coerceAtLeast(4f)
                                ),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                            val midShelfY = topPx + unitHeightPx * 0.5f
                            drawLine(
                                color = Color(0xFFFFE082).copy(alpha = 0.7f),
                                start = Offset(leftPx + 6f, midShelfY),
                                end = Offset(leftPx + unitWidthPx - 6f, midShelfY),
                                strokeWidth = 1.6f
                            )
                        }

                        drawRoundRect(
                            color = when {
                                isGhost -> Color(0xFF00E676)
                                isSelected && activeTool == ArInteractionTool.AR_MATERIAL_EDITOR -> Color(0xFF00E5FF)
                                isSelected -> Color(0xFFFFD54F)
                                else -> Color.White.copy(alpha = 0.82f)
                            },
                            topLeft = Offset(leftPx, topPx),
                            size = Size(unitWidthPx, unitHeightPx),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(
                                width = if (isSelected || isGhost) 4f else 1.8f,
                                pathEffect = if (isGhost) {
                                    PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                                } else {
                                    null
                                }
                            )
                        )

                        // AR Targeting Reticle when selected for AR Material Customization
                        if (isSelected && !isGhost) {
                            val cx = leftPx + unitWidthPx / 2f
                            val cy = topPx + unitHeightPx / 2f
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.85f),
                                radius = 14f,
                                center = Offset(cx, cy - 16f),
                                style = Stroke(width = 2.5f)
                            )
                            drawCircle(
                                color = Color(0xFFFFD54F),
                                radius = 4f,
                                center = Offset(cx, cy - 16f)
                            )
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            if (isGhost) {
                                "إفلات: ${item.xMm},${item.yMm}mm"
                            } else {
                                "${item.widthMm}×${item.heightMm}mm"
                            },
                            leftPx + unitWidthPx / 2f,
                            topPx + unitHeightPx / 2f + 6f,
                            dimPaint
                        )

                        if (!isGhost && (item.customCabinetFinish != null || item.customCountertopMaterial != null || isSelected)) {
                            drawContext.canvas.nativeCanvas.drawText(
                                itemFinishAr.take(16),
                                leftPx + unitWidthPx / 2f,
                                topPx + unitHeightPx / 2f + 24f,
                                finishBadgePaint
                            )
                        }
                    }
            }

            Surface(
                color = Color(0xFF10181B).copy(alpha = 0.80f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = when {
                        isDraggingInAr && activeArDragSession?.previewPlacedItem != null -> {
                            val p = activeArDragSession!!.previewPlacedItem!!
                            "أفلت الآن لإسقاط الوحدة في الواقع المعزز عند (${p.xMm}، ${p.yMm} ملم)"
                        }
                        activeTool == ArInteractionTool.AR_MATERIAL_EDITOR ->
                            "تعديل الخامة بالواقع المعزز: وجّه الكاميرا أو اضغط على أي خزانة لتغيير الخشب أو الرخام ومزامنة JSON فوراً"
                        activeTool == ArInteractionTool.PLACE_AND_MOVE_3D ->
                            "AR مباشر: اسحب الخزائن من القائمة العائمة وأفلتها فوق الكاميرا الحية بدقة 1 ملم"
                        activeTool == ArInteractionTool.CALIBRATE_4_PINS ->
                            "معايرة 4-Pin: اسحب النقاط الأربع الزرقاء لمطابقة أرضية الغرفة وخط الأفق"
                        else ->
                            "مسح الحائط الذكي: اسحب بإصبعك فوق الخزائن القديمة لإخفائها وتنظيف الجدار"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // =========================================================================
        // 2. FLOATING COLLAPSIBLE / HIDEABLE AR MENUS OVERLAY
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingMenusMasterDockBar(
                allMenusHidden = allArMenusHidden,
                onToggleAllMenus = { allArMenusHidden = !allArMenusHidden },
                dockTitleAr = "قوائم AR العائمة",
                menuItems = listOf(
                    FloatingMenuToggleItem(
                        id = "ar_tools",
                        titleAr = "أدوات الكاميرا وJSON",
                        icon = Icons.Default.CameraAlt,
                        isVisible = showArToolsFloatingMenu,
                        testTag = "ar_dock_toggle_tools",
                        onToggle = { showArToolsFloatingMenu = !showArToolsFloatingMenu }
                    ),
                    FloatingMenuToggleItem(
                        id = "ar_material_json",
                        titleAr = "تعديل الخامة AR ➔ JSON",
                        icon = Icons.Default.Palette,
                        isVisible = showArMaterialJsonFloatingMenu,
                        badgeText = "PBR",
                        testTag = "ar_dock_toggle_material_json",
                        onToggle = { showArMaterialJsonFloatingMenu = !showArMaterialJsonFloatingMenu }
                    ),
                    FloatingMenuToggleItem(
                        id = "ar_horizon",
                        titleAr = "معايرة الأفق 4-Pin",
                        icon = Icons.Default.Straighten,
                        isVisible = showArHorizonFloatingMenu,
                        badgeText = "${(horizonRatioY * 100).roundToInt()}%",
                        testTag = "ar_dock_toggle_horizon",
                        onToggle = { showArHorizonFloatingMenu = !showArHorizonFloatingMenu }
                    ),
                    FloatingMenuToggleItem(
                        id = "ar_catalog",
                        titleAr = "كتالوج السحب والإفلات AR",
                        icon = Icons.Default.DashboardCustomize,
                        isVisible = showArCatalogFloatingMenu,
                        badgeText = "${catalogUnits.size}",
                        testTag = "ar_dock_toggle_catalog",
                        onToggle = { showArCatalogFloatingMenu = !showArCatalogFloatingMenu }
                    )
                )
            )

            if (!allArMenusHidden) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Floating AR Menu 1: AR Tools, Permission & Camera ➔ JSON v2.0
                    FloatingCollapsibleWindow(
                        titleAr = "التصميم المباشر بالكاميرا الحية (AR Live)",
                        subtitleAr = "إسقاط خزائن 3D واقعية • تعديل الخامة بالواقع المعزز • كاميرا ➔ JSON",
                        icon = Icons.Default.CameraAlt,
                        visible = showArToolsFloatingMenu,
                        onHide = { showArToolsFloatingMenu = false },
                        fadeWhenDraggingCatalogItem = isDraggingInAr,
                        testTag = "ar_floating_tools_menu",
                        headerActions = {
                            if (!hasCameraPermission) {
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("request_camera_permission_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تفعيل الكاميرا", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ArInteractionTool.entries.forEach { tool ->
                                FilterChip(
                                    selected = activeTool == tool,
                                    onClick = {
                                        activeTool = tool
                                        if (tool == ArInteractionTool.AR_MATERIAL_EDITOR) {
                                            showArMaterialJsonFloatingMenu = true
                                        }
                                    },
                                    label = { Text(tool.titleAr) },
                                    leadingIcon = {
                                        val icon = when (tool) {
                                            ArInteractionTool.PLACE_AND_MOVE_3D -> Icons.Default.AddPhotoAlternate
                                            ArInteractionTool.AR_MATERIAL_EDITOR -> Icons.Default.Palette
                                            ArInteractionTool.CALIBRATE_4_PINS -> Icons.Default.CropFree
                                            ArInteractionTool.WALL_INPAINTING -> Icons.Default.AutoFixNormal
                                        }
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.testTag("ar_tool_${tool.name}")
                                )
                            }

                            FilledTonalButton(
                                onClick = { showCameraToJsonDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("camera_to_json_scanner_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استيراد كائن بالكاميرا ➔ JSON v2.0")
                            }

                            OutlinedButton(
                                onClick = { showUniversalJsonDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("ar_import_any_json_button")
                            ) {
                                Text("استيراد أي ملف JSON")
                            }

                            if (inpaintedWallPatches.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { inpaintedWallPatches.clear() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("clear_inpainting_patches_button")
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إلغاء مسح الحائط (${inpaintedWallPatches.size})")
                                }
                            }
                        }
                    }

                    // Floating AR Menu 1B: Live AR Material Customizer with Immediate JSON v2.0 Sync
                    val targetedArItem = state.placedItems.firstOrNull { it.uid == state.selectedItemUid }
                        ?: state.placedItems.firstOrNull()
                    FloatingCollapsibleWindow(
                        titleAr = "تعديل الخامة بالواقع المعزز ➔ مزامنة JSON فورية",
                        subtitleAr = targetedArItem?.let {
                            "الخزانة المستهدفة: ${it.displayTitle} (${it.widthMm}×${it.heightMm} ملم) • اضغط لتغيير الخشب أو الرخام فوراً"
                        } ?: "وجّه الكاميرا نحو أي خزانة مصممة لتغيير خامتها وتحديث ملف JSON الخاص بها فوراً",
                        icon = Icons.Default.Palette,
                        visible = showArMaterialJsonFloatingMenu,
                        onHide = { showArMaterialJsonFloatingMenu = false },
                        fadeWhenDraggingCatalogItem = isDraggingInAr,
                        testTag = "ar_floating_material_json_menu",
                        headerActions = {
                            if (targetedArItem != null) {
                                TextButton(
                                    onClick = {
                                        ProjectExportManager.exportSingleUnitJsonV2File(
                                            context = context,
                                            item = targetedArItem,
                                            cabinetFinish = state.cabinetFinish,
                                            countertopMaterial = state.countertopMaterial
                                        )
                                    },
                                    modifier = Modifier.testTag("ar_export_unit_json_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاركة JSON الخزانة", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    ) {
                        if (targetedArItem == null) {
                            Text(
                                text = "لا توجد خزائن في المشهد حالياً. أضف خزانة من شريط الكتالوج لتجربة تعديل الخامة بالواقع المعزز.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val activeUnitFinish = targetedArItem.customCabinetFinish ?: state.cabinetFinish
                            val activeUnitCounter = targetedArItem.customCountertopMaterial ?: state.countertopMaterial
                            val liveUnitJson = remember(targetedArItem, state.cabinetFinish, state.countertopMaterial) {
                                JsonCadEngine.itemToJsonV2(
                                    item = targetedArItem,
                                    finishName = state.cabinetFinish,
                                    countertopName = state.countertopMaterial,
                                    captureSource = "AR_LIVE_MATERIAL_EDITOR"
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // 1. Target Cabinet Selector Row
                                Text(
                                    text = "1. اختر الخزانة المصممة أمام الكاميرا:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.placedItems.forEach { unit ->
                                        FilterChip(
                                            selected = unit.uid == targetedArItem.uid,
                                            onClick = {
                                                viewModel.selectItem(unit.uid)
                                                activeTool = ArInteractionTool.AR_MATERIAL_EDITOR
                                            },
                                            label = { Text("${unit.displayTitle} (${unit.widthMm}mm)") },
                                            modifier = Modifier.testTag("ar_target_unit_${unit.uid}")
                                        )
                                    }
                                }

                                // 2. Live Wood / Lacquer Finish Selector
                                Text(
                                    text = "2. نوع الخشب / تشطيب الخزانة (الحالي: $activeUnitFinish):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    KitchenCatalogData.cabinetFinishes.forEach { finish ->
                                        FilterChip(
                                            selected = activeUnitFinish == finish.nameAr,
                                            onClick = {
                                                activeTool = ArInteractionTool.AR_MATERIAL_EDITOR
                                                viewModel.applyArMaterialToUnitAndSyncJson(
                                                    uid = targetedArItem.uid,
                                                    newCabinetFinish = finish.nameAr,
                                                    newCountertopMaterial = null
                                                )
                                            },
                                            label = { Text(finish.nameAr) },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(finish.colorHex))
                                                )
                                            },
                                            modifier = Modifier.testTag("ar_material_finish_${finish.nameAr}")
                                        )
                                    }
                                }

                                // 3. Live Marble / Countertop Selector
                                Text(
                                    text = "3. نوع الرخام / الكوارتز لسطح العمل (الحالي: $activeUnitCounter):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    KitchenCatalogData.countertopMaterials.forEach { surface ->
                                        FilterChip(
                                            selected = activeUnitCounter == surface.nameAr,
                                            onClick = {
                                                activeTool = ArInteractionTool.AR_MATERIAL_EDITOR
                                                viewModel.applyArMaterialToUnitAndSyncJson(
                                                    uid = targetedArItem.uid,
                                                    newCabinetFinish = null,
                                                    newCountertopMaterial = surface.nameAr
                                                )
                                            },
                                            label = { Text(surface.nameAr) },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(surface.colorHex))
                                                )
                                            },
                                            modifier = Modifier.testTag("ar_countertop_${surface.nameAr}")
                                        )
                                    }
                                }

                                // 4. Live Synced JSON v2.0 Preview of the Targeted Unit
                                Surface(
                                    color = Color(0xFF141B1F),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ar_synced_unit_json_preview")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "ملف JSON v2.0 المحدث لحظياً للخزانة (${targetedArItem.uid}):",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF80DEEA),
                                                fontWeight = FontWeight.Bold
                                            )
                                            TextButton(
                                                onClick = onOpenJsonTab,
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier.testTag("ar_open_full_json_editor_button")
                                            ) {
                                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD54F))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("فتح محرر JSON الكامل", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD54F))
                                            }
                                        }
                                        Text(
                                            text = liveUnitJson,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE0F7FA),
                                            maxLines = 6
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating AR Menu 2: Horizon & 4-Pin Calibration Slider
                    FloatingCollapsibleWindow(
                        titleAr = "معايرة خط الأفق ومستوى الأرضية (${(horizonRatioY * 100).roundToInt()}%)",
                        subtitleAr = "اضبط مستوى خط الأفق أو أعد ضبط نقاط المعايرة الأربع",
                        icon = Icons.Default.Straighten,
                        visible = showArHorizonFloatingMenu,
                        onHide = { showArHorizonFloatingMenu = false },
                        fadeWhenDraggingCatalogItem = isDraggingInAr,
                        testTag = "ar_floating_horizon_menu",
                        headerActions = {
                            TextButton(
                                onClick = {
                                    pinTopLeft = Offset(0.16f, 0.56f)
                                    pinTopRight = Offset(0.84f, 0.56f)
                                    pinBottomRight = Offset(0.96f, 0.92f)
                                    pinBottomLeft = Offset(0.04f, 0.92f)
                                    horizonRatioY = 0.56f
                                }
                            ) {
                                Text("إعادة ضبط 4-Pin", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    ) {
                        Slider(
                            value = horizonRatioY,
                            onValueChange = {
                                horizonRatioY = it
                                pinTopLeft = pinTopLeft.copy(y = it)
                                pinTopRight = pinTopRight.copy(y = it)
                            },
                            valueRange = 0.35f..0.75f
                        )
                    }

                    // Floating AR Menu 3: Live Visual Drag-and-Drop AR Catalog
                    FloatingCollapsibleWindow(
                        titleAr = "شريط الكتالوج العائم فوق الكاميرا (سحب وإفلات مباشر)",
                        subtitleAr = "اسحب أي وحدة وأفلتها مباشرة فوق الكاميرا الحية أو اضغط للإضافة",
                        icon = Icons.Default.DashboardCustomize,
                        visible = showArCatalogFloatingMenu,
                        onHide = { showArCatalogFloatingMenu = false },
                        fadeWhenDraggingCatalogItem = false,
                        testTag = "ar_floating_catalog_menu"
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(catalogUnits, key = { "ar_${it.id.ifBlank { it.code }}" }) { unit ->
                                DraggableCatalogUnitCard(
                                    unit = unit,
                                    onQuickTapAdd = { viewModel.addCatalogUnitToLayout(it) },
                                    onDragStartUnit = { draggedUnit, startRoot ->
                                        updateArUnitDragPosition(draggedUnit, startRoot)
                                    },
                                    onDragMoveUnit = { moveRoot ->
                                        val activeUnit = activeArDragSession?.unit ?: unit
                                        updateArUnitDragPosition(activeUnit, moveRoot)
                                    },
                                    onDragEndUnit = {
                                        completeArCatalogDragSession()
                                    },
                                    onDragCancelUnit = {
                                        activeArDragSession = null
                                    },
                                    actionLabelAr = "اسحب للكاميرا أو +",
                                    testTag = "ar_catalog_add_${unit.code}"
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. FLOATING DRAG-AND-DROP GHOST CARD FOR AR VIEWPORT
        // =========================================================================
        FloatingCatalogDragGhostOverlay(
            dragSession = activeArDragSession,
            containerRootOffset = rootBoxPosition
        )
    }

    if (showCameraToJsonDialog) {
        CameraToJsonV2ScannerDialog(
            state = state,
            onDismiss = { showCameraToJsonDialog = false },
            onScanAndGenerate = { label, type, wMm, dMm, hMm, sampledHex ->
                viewModel.createUnitFromCameraScanToJsonV2(
                    labelAr = label,
                    baseType = type,
                    widthMm = wMm,
                    depthMm = dMm,
                    heightMm = hMm,
                    sampledColorHex = sampledHex
                )
                showCameraToJsonDialog = false
                onOpenJsonTab()
            }
        )
    }

    if (showUniversalJsonDialog) {
        UniversalJsonImporterDialog(
            onDismiss = { showUniversalJsonDialog = false },
            onImportJson = { fileName, rawJson ->
                viewModel.importAnyJsonFileOrContent(fileName, rawJson)
            }
        )
    }
}
