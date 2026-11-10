package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.height
import com.example.data.cam.ProjectExportManager
import com.example.data.cam.StandardAluminumBarPreset
import com.example.data.cam.StandardMelamineSheetPreset
import com.example.data.model.CameraPreset3D
import com.example.data.model.KitchenCatalogData
import com.example.data.model.KitchenCatalogUnit
import com.example.data.model.KitchenItemsSerializer
import com.example.data.model.KitchenProject
import com.example.data.model.KitchenShape
import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.ui.components.CatalogDragSession
import com.example.ui.components.DraggableCatalogUnitCard
import com.example.ui.components.DraggableMaterialFinishChip
import com.example.ui.components.FloatingCatalogDragGhostOverlay
import com.example.ui.components.FloatingCollapsibleWindow
import com.example.ui.components.FloatingMenuToggleItem
import com.example.ui.components.FloatingMenusMasterDockBar
import com.example.ui.components.KitchenInteractiveCanvas
import com.example.ui.components.Simple3DKitchenMiniPreviewCanvas
import com.example.ui.components.VrCameraSensorNavigationOverlay
import com.example.ui.components.screenOffsetToRoomMillimeters
import com.example.ui.viewmodel.KitchenPlannerViewModel
import com.example.ui.viewmodel.LightingMode
import com.example.ui.viewmodel.PlannerStudioState
import com.example.ui.viewmodel.StudioViewMode
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@Composable
fun StudioTabScreen(
    state: PlannerStudioState,
    catalogUnitsState: UiState<List<KitchenCatalogUnit>>,
    projectsState: UiState<List<KitchenProject>>,
    viewModel: KitchenPlannerViewModel,
    onNavigateToQuote: () -> Unit,
    onNavigateToProjects: () -> Unit = {}
) {
    val context = LocalContext.current
    var showRoomConfigDialog by remember { mutableStateOf(false) }
    var showSaveLayoutDialog by remember { mutableStateOf(false) }
    var showRetrieveProjectDialog by remember { mutableStateOf(false) }
    var showAutoFillerDialog by remember { mutableStateOf(false) }
    var showMmPrecisionDialog by remember { mutableStateOf(false) }
    var showSnapshotsDialog by remember { mutableStateOf(false) }
    var showCameraToJsonDialog by remember { mutableStateOf(false) }
    var showUniversalJsonDialog by remember { mutableStateOf(false) }
    var showAutoCadDxfDialog by remember { mutableStateOf(false) }
    var showGeminiAiDialog by remember { mutableStateOf(false) }
    var showLightingSimulationDialog by remember { mutableStateOf(false) }
    var showComplianceAlertsDialog by remember { mutableStateOf(false) }
    var showSmartAccessoriesDialog by remember { mutableStateOf(false) }
    var showShareBlueprintDialog by remember { mutableStateOf(false) }
    var showWallFloorHarmonyDialog by remember { mutableStateOf(false) }
    var showCustomCabinetPhotoDialog by remember { mutableStateOf(false) }
    var showMaterialStandardsDialog by remember { mutableStateOf(false) }

    // Collapsible / Hideable Floating Menus State
    var allMenusHidden by remember { mutableStateOf(false) }
    var showToolsFloatingMenu by remember { mutableStateOf(true) }
    var showCatalogFloatingMenu by remember { mutableStateOf(true) }
    var showMaterialsFloatingMenu by remember { mutableStateOf(true) }
    var showInspectorFloatingMenu by remember { mutableStateOf(true) }
    var showCostFloatingMenu by remember { mutableStateOf(true) }

    var selectedCatalogCategoryFilter by remember { mutableStateOf("ALL") }

    // Drag-and-Drop Session State from Floating Menus to 2D/3D Canvas
    var rootBoxPosition by remember { mutableStateOf(Offset.Zero) }
    var canvasRootRect by remember { mutableStateOf(Rect.Zero) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    var activeDragSession by remember { mutableStateOf<CatalogDragSession?>(null) }

    val localSnapshots by viewModel.localSnapshotsState.collectAsStateWithLifecycle()
    val selectedItem = state.placedItems.firstOrNull { it.uid == state.selectedItemUid }

    val catalogUnits = when (catalogUnitsState) {
        is UiState.Success -> catalogUnitsState.data
        else -> ModuleType.entries.map { it.toDefaultCatalogUnit() }
    }

    val filteredCatalogUnits = remember(catalogUnits, selectedCatalogCategoryFilter) {
        when (selectedCatalogCategoryFilter) {
            "ALL" -> catalogUnits
            "CORNER" -> catalogUnits.filter { it.isCornerUnit }
            else -> catalogUnits.filter { it.category == selectedCatalogCategoryFilter }
        }
    }

    val savedProjects = when (projectsState) {
        is UiState.Success -> projectsState.data
        else -> emptyList()
    }

    fun updateUnitDragPosition(unit: KitchenCatalogUnit, pointerRoot: Offset) {
        val isInsideCanvas = canvasRootRect.width > 0f &&
            pointerRoot.x in (canvasRootRect.left - 36f)..(canvasRootRect.right + 36f) &&
            pointerRoot.y in (canvasRootRect.top - 36f)..(canvasRootRect.bottom + 36f)

        val previewItem = if (isInsideCanvas) {
            val localOffset = Offset(
                (pointerRoot.x - canvasRootRect.left).coerceIn(0f, canvasRootRect.width),
                (pointerRoot.y - canvasRootRect.top).coerceIn(0f, canvasRootRect.height)
            )
            val resolvedType = unit.resolvedModuleType
            val (rawXMm, rawYMm) = screenOffsetToRoomMillimeters(
                canvasOffset = localOffset,
                canvasWidth = canvasSizePx.width.toFloat(),
                canvasHeight = canvasSizePx.height.toFloat(),
                state = state,
                unitWidthMm = (unit.widthCm.toInt() * 10).coerceAtLeast(100),
                unitDepthMm = (unit.depthCm.toInt() * 10).coerceAtLeast(250),
                unitElevationMm = resolvedType.elevationCm * 10,
                unitHeightMm = resolvedType.heightCm * 10,
                isWallCabinet = resolvedType.category == ModuleCategory.WALL_CABINET
            )
            viewModel.computeSnappedDropPreview(unit, rawXMm, rawYMm)
        } else {
            null
        }

        activeDragSession = CatalogDragSession(
            unit = unit,
            pointerRootOffset = pointerRoot,
            isOverCanvas = isInsideCanvas,
            previewPlacedItem = previewItem
        )
    }

    fun completeCatalogDragSession() {
        val session = activeDragSession
        activeDragSession = null
        if (session != null && session.isOverCanvas) {
            val unit = session.unit
            val preview = session.previewPlacedItem
            if (unit != null && preview != null) {
                viewModel.dropCatalogUnitAtPositionMm(unit, preview.xMm, preview.yMm)
            } else if (session.cabinetFinishNameAr != null) {
                viewModel.updateFinishes(cabinetFinish = session.cabinetFinishNameAr)
            } else if (session.countertopMaterialNameAr != null) {
                viewModel.updateFinishes(countertopMaterial = session.countertopMaterialNameAr)
            }
        }
    }

    val isDraggingAnything = activeDragSession != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootBoxPosition = coords.positionInRoot()
            }
    ) {
        // =========================================================================
        // 1. FULL-SCREEN INTERACTIVE 2D / 3D / WALKTHROUGH DESIGN VIEWPORT
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    canvasSizePx = coords.size
                    canvasRootRect = Rect(
                        offset = pos,
                        size = androidx.compose.ui.geometry.Size(
                            coords.size.width.toFloat(),
                            coords.size.height.toFloat()
                        )
                    )
                }
        ) {
            KitchenInteractiveCanvas(
                state = state,
                onSelectItem = { viewModel.selectItem(it) },
                onMoveItemDirect = { uid, x, y -> viewModel.updateItemPositionDirect(uid, x, y) },
                onMoveItemDirectMm = { uid, xMm, yMm -> viewModel.updateItemPositionDirectMm(uid, xMm, yMm) },
                onResizeItemDirectMm = { uid, wMm, dMm -> viewModel.resizeItemDirectMm(uid, wMm, dMm) },
                onBeginItemDrag = { viewModel.beginItemDragUndoCheckpoint() },
                onOrbitCamera = { deltaYaw, deltaPitch ->
                    viewModel.adjustCameraOrbit(deltaYaw, deltaPitch)
                },
                previewDragItem = activeDragSession?.previewPlacedItem,
                modifier = Modifier.fillMaxSize()
            )

            // VR Camera Sensor Navigation Overlay (PiP Optical-Flow Radar + Walk/Pan/Tilt Pad)
            if (state.viewMode == StudioViewMode.VR_CAMERA_WALKTHROUGH) {
                VrCameraSensorNavigationOverlay(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Live Status & Telemetry Pill (Bottom-Start of Canvas)
            if (state.viewMode != StudioViewMode.VR_CAMERA_WALKTHROUGH) {
                Surface(
                    color = Color(0xFF141C1E).copy(alpha = 0.78f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = when {
                            isDraggingAnything && activeDragSession?.previewPlacedItem != null -> {
                                val p = activeDragSession!!.previewPlacedItem!!
                                "أفلت العنصر الآن لوضعه عند (${state.unitSystem.formatMillimeters(p.xMm)}، ${state.unitSystem.formatMillimeters(p.yMm)}) • التصاق مغناطيسي نشط"
                            }
                            state.viewMode == StudioViewMode.RENDER_3D ->
                                "منظور 3D حر 360° (${state.cameraYawDeg.roundToInt()}°) • إضاءة ${state.lightingSimulation.activePreset.titleAr} (${state.lightingSimulation.colorTemperatureKelvin}K)"
                            state.viewMode == StudioViewMode.WALKTHROUGH_FP ->
                                "تجول داخلي من منظور العين (${state.cameraYawDeg.roundToInt()}°) • اسحب للالتفات أو أفلت وحدات 3D"
                            else ->
                                "لوحة 2D التفاعلية (${state.unitSystem.badgeAr}) • اسحب الوحدة لتحريكها أو اسحب مقابض (↔ ↕ ⤡) لتغيير أبعادها باللمس"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Floating Camera & Undo/Redo Quick HUD (Bottom-End of Canvas)
            Surface(
                color = Color(0xFF141C1E).copy(alpha = 0.84f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.undoLastItemChange() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "تراجع",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redoLastItemChange() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "إعادة",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.adjustCameraZoom(0.15f) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "تكبير",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.adjustCameraZoom(-0.15f) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "تصغير",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (state.viewMode != StudioViewMode.PLAN_2D) {
                        IconButton(
                            onClick = { viewModel.adjustCameraYaw(30f) },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("rotate_camera_3d_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "تدوير زاوية الرؤية",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (state.viewMode == StudioViewMode.VR_CAMERA_WALKTHROUGH) {
                                viewModel.setViewMode(StudioViewMode.RENDER_3D)
                            } else {
                                viewModel.setViewMode(StudioViewMode.VR_CAMERA_WALKTHROUGH)
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("quick_vr_walkthrough_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "تجول الواقع الافتراضي بالكاميرا VR",
                            tint = if (state.viewMode == StudioViewMode.VR_CAMERA_WALKTHROUGH) {
                                Color(0xFF80CBC4)
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 2. FLOATING COLLAPSIBLE / HIDEABLE MENUS OVERLAY
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Master Floating Dock Bar (Always accessible to show/hide any or all floating menus)
            FloatingMenusMasterDockBar(
                allMenusHidden = allMenusHidden,
                onToggleAllMenus = { allMenusHidden = !allMenusHidden },
                menuItems = listOf(
                    FloatingMenuToggleItem(
                        id = "tools",
                        titleAr = "أدوات التصميم والمشروع",
                        icon = Icons.Default.Tune,
                        isVisible = showToolsFloatingMenu,
                        testTag = "dock_toggle_tools_menu",
                        onToggle = { showToolsFloatingMenu = !showToolsFloatingMenu }
                    ),
                    FloatingMenuToggleItem(
                        id = "catalog",
                        titleAr = "كتالوج السحب والإفلات",
                        icon = Icons.Default.DashboardCustomize,
                        isVisible = showCatalogFloatingMenu,
                        badgeText = "${catalogUnits.size}",
                        testTag = "dock_toggle_catalog_menu",
                        onToggle = { showCatalogFloatingMenu = !showCatalogFloatingMenu }
                    ),
                    FloatingMenuToggleItem(
                        id = "materials",
                        titleAr = "خامات PBR والألوان",
                        icon = Icons.Default.Palette,
                        isVisible = showMaterialsFloatingMenu,
                        testTag = "dock_toggle_materials_menu",
                        onToggle = { showMaterialsFloatingMenu = !showMaterialsFloatingMenu }
                    ),
                    FloatingMenuToggleItem(
                        id = "inspector",
                        titleAr = "فحص الوحدة 1mm",
                        icon = Icons.Default.Straighten,
                        isVisible = showInspectorFloatingMenu,
                        badgeText = selectedItem?.widthMm?.let { "${it}mm" },
                        testTag = "dock_toggle_inspector_menu",
                        onToggle = { showInspectorFloatingMenu = !showInspectorFloatingMenu }
                    ),
                    FloatingMenuToggleItem(
                        id = "cost",
                        titleAr = "مثلث العمل والتكلفة",
                        icon = Icons.Default.RequestQuote,
                        isVisible = showCostFloatingMenu,
                        badgeText = "${state.costBreakdown.totalSar} ر.س",
                        testTag = "dock_toggle_cost_menu",
                        onToggle = { showCostFloatingMenu = !showCostFloatingMenu }
                    )
                )
            )

            // Scrollable overlay container for open floating panels so they never clip on compact screens
            if (!allMenusHidden) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 540.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // -----------------------------------------------------------------
                    // FLOATING MENU 1: Project, Cloud Save, CAD Tools & 2D/3D View Modes
                    // -----------------------------------------------------------------
                    FloatingCollapsibleWindow(
                        titleAr = state.projectName,
                        subtitleAr = "${state.shape.titleAr} • ${state.unitSystem.formatMillimeters(state.roomWidthMm)} × ${state.unitSystem.formatMillimeters(state.roomDepthMm)} • ${state.placedItems.size} وحدة",
                        icon = Icons.Default.Tune,
                        visible = showToolsFloatingMenu,
                        onHide = { showToolsFloatingMenu = false },
                        fadeWhenDraggingCatalogItem = isDraggingAnything,
                        testTag = "floating_tools_menu",
                        headerActions = {
                            Surface(
                                color = when {
                                    state.hasUnsavedChanges -> MaterialTheme.colorScheme.tertiaryContainer
                                    state.activeProjectId.isNotBlank() -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = when {
                                        state.hasUnsavedChanges -> "غير محفوظ"
                                        state.activeProjectId.isNotBlank() -> "سحابي"
                                        else -> "جديد"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    ) {
                        // Cloud Save / Load / Room Auto-Save / Dimensions Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showRoomConfigDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("room_dimensions_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "أبعاد الغرفة", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("أبعاد وشكل الغرفة", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = { showSaveLayoutDialog = true },
                                enabled = !state.isSaving,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("studio_open_save_dialog_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.activeProjectId.isBlank()) {
                                        "حفظ التصميم 3D في Firestore"
                                    } else {
                                        "حفظ التعديلات / نسخة جديدة"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            FilledTonalButton(
                                onClick = { showRetrieveProjectDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("studio_open_load_dialog_button")
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استرجاع مشروع (${savedProjects.size})", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = { showSnapshotsDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_snapshots_recovery_button")
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "حفظ تلقائي Room كل 30ث (${state.autoSaveCountdownSeconds}ث • ${localSnapshots.size})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Button(
                                onClick = { showGeminiAiDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("studio_gemini_ai_suggest_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اقتراح تصميم كامل عبر Gemini AI", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = { showShareBlueprintDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_share_blueprint_hub_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة المخطط مع الورش والأصدقاء (تواصل / بريد)", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = { showWallFloorHarmonyDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_wall_floor_harmony_button")
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ألوان الحوائط والأرضيات والتناسق 3D", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = { showCustomCabinetPhotoDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_custom_cabinet_photo_designer_button")
                            ) {
                                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصميم خزانة من صور أمامية وجانبية (6 جهات)", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = {
                                    ProjectExportManager.generateEngineeringPdfReport(
                                        context = context,
                                        state = state,
                                        nesting = state.sheetNesting,
                                        aluminum = state.aluminumNesting,
                                        shareViaMessagingApps = true
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("studio_export_pdf_report_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير ومشاركة PDF احترافي (رندر + BOM + مخطط)", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = { showAutoCadDxfDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("studio_dxf_import_export_button")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("أوتوكاد DXF (استيراد / تصدير للورش)", style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { viewModel.startNewBlankProject() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("studio_new_project_button")
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مخطط جديد", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                        // CAD Engineering Tools Row (Unit System, Virtual Ruler, Compliance Validator, Smart Accessories, Lighting Sim)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = state.unitSystem.code == "IMPERIAL",
                                onClick = { viewModel.toggleMeasurementUnitSystem() },
                                label = {
                                    Text("نظام القياس: ${state.unitSystem.badgeAr}")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("toggle_unit_system_chip")
                            )

                            FilterChip(
                                selected = state.showVirtualRuler,
                                onClick = { viewModel.toggleVirtualRuler() },
                                label = {
                                    Text(
                                        if (state.showVirtualRuler) {
                                            "المسطرة الافتراضية والليزر (نشطة)"
                                        } else {
                                            "المسطرة الافتراضية (متوقفة)"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("toggle_virtual_ruler_chip")
                            )

                            FilledTonalButton(
                                onClick = { showLightingSimulationDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_lighting_simulation_button")
                            ) {
                                Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "محرك الإضاءة 3D (${state.lightingSimulation.activePreset.titleAr})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            FilledTonalButton(
                                onClick = { showComplianceAlertsDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_compliance_alerts_button")
                            ) {
                                Icon(
                                    imageVector = if (state.complianceReport.alerts.isEmpty()) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.WarningAmber
                                    },
                                    contentDescription = null,
                                    tint = if (state.complianceReport.alerts.isEmpty()) {
                                        Color(0xFF00C853)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (state.complianceReport.alerts.isEmpty()) {
                                        "التحقق الهندسي (100% مطابق)"
                                    } else {
                                        "تنبيهات التركيب (${state.complianceReport.alerts.size} تعارض)"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            if (state.complianceReport.alerts.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.autoFixEngineeringComplianceViolations() },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("quick_auto_fix_compliance_button")
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إصلاح هندسي تلقائي", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            FilledTonalButton(
                                onClick = { showSmartAccessoriesDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_smart_accessories_button")
                            ) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "إكسسوارات داخلية ذكية (${state.costBreakdown.linkedSmartAccessoriesCount} مرتبط بـ BOM)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            FilledTonalButton(
                                onClick = { showMaterialStandardsDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_standard_dimensions_settings_button")
                            ) {
                                Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "الأبعاد القياسية للألواح والبروفايلات (${state.camMaterialStandards.sheetWidthMm}×${state.camMaterialStandards.sheetHeightMm} ملم)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            FilterChip(
                                selected = state.magneticSnapEnabled,
                                onClick = { viewModel.toggleMagneticSnap() },
                                label = {
                                    Text(
                                        if (state.magneticSnapEnabled) {
                                            "التصاق مغناطيسي ومنع التداخل (مفعّل)"
                                        } else {
                                            "التصاق مغناطيسي (متوقف)"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("toggle_magnetic_snap_chip")
                            )

                            FilledTonalButton(
                                onClick = { viewModel.pairUpperWallCabinetsWithBase() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("pair_upper_cabinets_button")
                            ) {
                                Icon(Icons.Default.VerticalAlignCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ربط الخزائن العلوية (أمان 600 ملم)", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = { showAutoFillerDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_auto_filler_button")
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("أداة الفيلر التلقائية 1 ملم", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = { showCameraToJsonDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("studio_camera_to_json_button")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد بالكاميرا كـ JSON", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = { showUniversalJsonDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("studio_import_any_json_button")
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد أي ملف JSON", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // 2D / 3D / Walkthrough & Lighting Mode Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = state.viewMode == StudioViewMode.RENDER_3D,
                                onClick = { viewModel.setViewMode(StudioViewMode.RENDER_3D) },
                                label = { Text("منظور مداري 3D Orbit") },
                                leadingIcon = {
                                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("mode_3d_chip")
                            )

                            FilterChip(
                                selected = state.viewMode == StudioViewMode.PLAN_2D,
                                onClick = { viewModel.setViewMode(StudioViewMode.PLAN_2D) },
                                label = { Text("مسقط أفقي 2D (دقة 1 ملم)") },
                                leadingIcon = {
                                    Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("mode_2d_chip")
                            )

                            FilterChip(
                                selected = state.viewMode == StudioViewMode.WALKTHROUGH_FP,
                                onClick = { viewModel.setViewMode(StudioViewMode.WALKTHROUGH_FP) },
                                label = { Text("تجول داخلي (الشخص الأول)") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("mode_fp_walkthrough_chip")
                            )

                            FilterChip(
                                selected = state.viewMode == StudioViewMode.VR_CAMERA_WALKTHROUGH,
                                onClick = { viewModel.setViewMode(StudioViewMode.VR_CAMERA_WALKTHROUGH) },
                                label = { Text("تجول واقع افتراضي بالحساس والكاميرا (VR)") },
                                leadingIcon = {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("mode_vr_walkthrough_chip")
                            )

                            FilterChip(
                                selected = state.doorsOpenPreview,
                                onClick = { viewModel.toggleDoorsOpenPreview() },
                                label = { Text("فتح الأدراج 3D") },
                                leadingIcon = {
                                    Icon(Icons.Default.DoorSliding, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("toggle_doors_open_chip")
                            )

                            FilterChip(
                                selected = state.showWorkTriangle,
                                onClick = { viewModel.toggleWorkTriangle() },
                                label = { Text("مثلث العمل (${state.workTriangle.score}%)") },
                                leadingIcon = {
                                    Icon(Icons.Default.ChangeHistory, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("toggle_work_triangle_chip")
                            )

                            LightingMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = state.lightingMode == mode,
                                    onClick = { viewModel.setLightingMode(mode) },
                                    label = { Text(mode.titleAr) },
                                    leadingIcon = {
                                        Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }

                        if (state.viewMode != StudioViewMode.PLAN_2D) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "زوايا الكاميرا",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "زوايا الرؤية 3D:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                CameraPreset3D.entries.forEach { preset ->
                                    FilterChip(
                                        selected = state.activeCameraPreset == preset,
                                        onClick = { viewModel.applyCameraPreset(preset) },
                                        label = { Text(preset.titleAr) },
                                        modifier = Modifier.testTag("camera_preset_${preset.name}")
                                    )
                                }
                            }
                        }
                    }

                    // -----------------------------------------------------------------
                    // FLOATING MENU 2: Drag-and-Drop Units & Appliances Catalog
                    // -----------------------------------------------------------------
                    FloatingCollapsibleWindow(
                        titleAr = "قائمة الكتالوج العائمة (اسحب وأفلت داخل التصميم)",
                        subtitleAr = "اسحب أي خزانة أو جهاز بإصبعك وأفلته في المكان المطلوب أو اضغط للإضافة الفورية",
                        icon = Icons.Default.DashboardCustomize,
                        visible = showCatalogFloatingMenu,
                        onHide = { showCatalogFloatingMenu = false },
                        fadeWhenDraggingCatalogItem = false,
                        testTag = "floating_catalog_menu"
                    ) {
                        // Category Filter Strip + Corner Cabinets + Custom Cabinet from Front & Side Photos Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { showCustomCabinetPhotoDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("catalog_menu_custom_photo_cabinet_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصميم خزانة من صور أمامية وجانبية", style = MaterialTheme.typography.labelSmall)
                            }

                            val cornerCount = catalogUnits.count { it.isCornerUnit }
                            listOf(
                                "ALL" to "الكل (${catalogUnits.size})",
                                "CORNER" to "كورنرات علوية وسفلية ($cornerCount)",
                                "BASE" to "خزائن سفلية",
                                "WALL" to "خزائن علوية وفيترينات",
                                "TALL" to "دواليب وأبراج",
                                "APPLIANCE" to "أجهزة مدمجة",
                                "ISLAND" to "جزر وسطية"
                            ).forEach { (catCode, catLabel) ->
                                FilterChip(
                                    selected = selectedCatalogCategoryFilter == catCode,
                                    onClick = { selectedCatalogCategoryFilter = catCode },
                                    label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("catalog_filter_chip_$catCode")
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(filteredCatalogUnits, key = { it.id.ifBlank { it.code } }) { unit ->
                                DraggableCatalogUnitCard(
                                    unit = unit,
                                    onQuickTapAdd = { viewModel.addCatalogUnitToLayout(it) },
                                    onDragStartUnit = { draggedUnit, startRoot ->
                                        updateUnitDragPosition(draggedUnit, startRoot)
                                    },
                                    onDragMoveUnit = { moveRoot ->
                                        val activeUnit = activeDragSession?.unit ?: unit
                                        updateUnitDragPosition(activeUnit, moveRoot)
                                    },
                                    onDragEndUnit = {
                                        completeCatalogDragSession()
                                    },
                                    onDragCancelUnit = {
                                        activeDragSession = null
                                    },
                                    testTag = "quick_add_${unit.code}"
                                )
                            }
                        }
                    }

                    // -----------------------------------------------------------------
                    // FLOATING MENU 3: PBR Surface Materials & Finishes (Draggable & Clickable)
                    // -----------------------------------------------------------------
                    FloatingCollapsibleWindow(
                        titleAr = "قائمة الخامات والألوان الفيزيائية (PBR)",
                        subtitleAr = "اسحب الخامة وأفلتها على المطبخ أو اضغط للتغيير الفوري",
                        icon = Icons.Default.Palette,
                        visible = showMaterialsFloatingMenu,
                        onHide = { showMaterialsFloatingMenu = false },
                        fadeWhenDraggingCatalogItem = isDraggingAnything,
                        testTag = "floating_materials_menu"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KitchenCatalogData.cabinetFinishes.forEach { finish ->
                                val isSelected = state.cabinetFinish == finish.nameAr
                                DraggableMaterialFinishChip(
                                    labelAr = finish.nameAr,
                                    colorHex = finish.colorHex,
                                    isSelected = isSelected,
                                    onTapSelect = {
                                        viewModel.updateFinishes(cabinetFinish = finish.nameAr)
                                    },
                                    onDragStartSwatch = { startRoot ->
                                        activeDragSession = CatalogDragSession(
                                            cabinetFinishNameAr = finish.nameAr,
                                            pointerRootOffset = startRoot,
                                            isOverCanvas = true
                                        )
                                    },
                                    onDragMoveSwatch = { moveRoot ->
                                        val over = canvasRootRect.width > 0f &&
                                            moveRoot.x in (canvasRootRect.left - 30f)..(canvasRootRect.right + 30f) &&
                                            moveRoot.y in (canvasRootRect.top - 30f)..(canvasRootRect.bottom + 30f)
                                        activeDragSession = activeDragSession?.copy(
                                            pointerRootOffset = moveRoot,
                                            isOverCanvas = over
                                        )
                                    },
                                    onDragEndSwatch = { completeCatalogDragSession() },
                                    onDragCancelSwatch = { activeDragSession = null },
                                    textureResId = finish.textureResId
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KitchenCatalogData.countertopMaterials.forEach { surface ->
                                val isSelected = state.countertopMaterial == surface.nameAr
                                DraggableMaterialFinishChip(
                                    labelAr = "سطح: ${surface.nameAr}",
                                    colorHex = surface.colorHex,
                                    isSelected = isSelected,
                                    onTapSelect = {
                                        viewModel.updateFinishes(countertopMaterial = surface.nameAr)
                                    },
                                    onDragStartSwatch = { startRoot ->
                                        activeDragSession = CatalogDragSession(
                                            countertopMaterialNameAr = surface.nameAr,
                                            pointerRootOffset = startRoot,
                                            isOverCanvas = true
                                        )
                                    },
                                    onDragMoveSwatch = { moveRoot ->
                                        val over = canvasRootRect.width > 0f &&
                                            moveRoot.x in (canvasRootRect.left - 30f)..(canvasRootRect.right + 30f) &&
                                            moveRoot.y in (canvasRootRect.top - 30f)..(canvasRootRect.bottom + 30f)
                                        activeDragSession = activeDragSession?.copy(
                                            pointerRootOffset = moveRoot,
                                            isOverCanvas = over
                                        )
                                    },
                                    onDragEndSwatch = { completeCatalogDragSession() },
                                    onDragCancelSwatch = { activeDragSession = null },
                                    textureResId = surface.textureResId
                                )
                            }
                        }

                        // Wall Color & Floor Material 3D Harmony Quick Strip inside Materials Menu
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { showWallFloorHarmonyDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("materials_menu_wall_floor_harmony_button")
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("أداة ألوان الحوائط والأرضيات 3D", style = MaterialTheme.typography.labelSmall)
                            }

                            KitchenCatalogData.wallColors.take(6).forEachIndexed { idx, wallOpt ->
                                FilterChip(
                                    selected = state.wallColorHex.equals(wallOpt.key, ignoreCase = true),
                                    onClick = { viewModel.updateFinishes(wallColorHex = wallOpt.key) },
                                    label = { Text("حائط: ${wallOpt.nameAr}", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("quick_wall_color_chip_$idx")
                                )
                            }

                            KitchenCatalogData.floorMaterials.take(5).forEachIndexed { idx, floorOpt ->
                                FilterChip(
                                    selected = state.floorMaterial == floorOpt.nameAr,
                                    onClick = { viewModel.updateFinishes(floorMaterial = floorOpt.nameAr) },
                                    label = { Text("أرضية: ${floorOpt.nameAr}", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("quick_floor_material_chip_$idx")
                                )
                            }
                        }
                    }

                    // -----------------------------------------------------------------
                    // FLOATING MENU 4: Selected Kitchen Unit 1mm Inspector
                    // -----------------------------------------------------------------
                    if (selectedItem != null) {
                        val clearances = state.selectedUnitClearanceDistances
                        FloatingCollapsibleWindow(
                            titleAr = "فحص وتحريك الوحدة: ${selectedItem.customLabel}",
                            subtitleAr = "الموقع (${state.unitSystem.formatMillimeters(selectedItem.xMm)}، ${state.unitSystem.formatMillimeters(selectedItem.yMm)}) • ${state.unitSystem.formatDimensionsBox(selectedItem.widthMm, selectedItem.depthMm, selectedItem.heightMm)}",
                            icon = Icons.Default.Straighten,
                            visible = showInspectorFloatingMenu,
                            onHide = { showInspectorFloatingMenu = false },
                            fadeWhenDraggingCatalogItem = isDraggingAnything,
                            testTag = "floating_inspector_menu",
                            headerActions = {
                                OutlinedButton(
                                    onClick = { showMmPrecisionDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("open_mm_precision_dialog_button")
                                ) {
                                    Text("إدخال 1mm", style = MaterialTheme.typography.labelSmall)
                                }
                                FilledTonalIconButton(
                                    onClick = { viewModel.rotateSelectedItem() },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("rotate_module_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                        contentDescription = "تدوير الوحدة 90 درجة",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { viewModel.deleteSelectedItem() },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("delete_module_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف الوحدة",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        ) {
                            if (clearances != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("virtual_ruler_inspector_banner")
                                ) {
                                    Text(
                                        text = "المسطرة الافتراضية: ${clearances.summaryAr(state.unitSystem)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تحريك دقيق 1 ملم:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                OutlinedButton(
                                    onClick = { viewModel.moveSelectedItemByMm(-1, 0) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("nudge_x_minus_1mm")
                                ) { Text("X -1 ملم", style = MaterialTheme.typography.labelSmall) }
                                OutlinedButton(
                                    onClick = { viewModel.moveSelectedItemByMm(1, 0) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("nudge_x_plus_1mm")
                                ) { Text("X +1 ملم", style = MaterialTheme.typography.labelSmall) }
                                OutlinedButton(
                                    onClick = { viewModel.moveSelectedItemByMm(0, -1) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("nudge_y_minus_1mm")
                                ) { Text("Y -1 ملم", style = MaterialTheme.typography.labelSmall) }
                                OutlinedButton(
                                    onClick = { viewModel.moveSelectedItemByMm(0, 1) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("nudge_y_plus_1mm")
                                ) { Text("Y +1 ملم", style = MaterialTheme.typography.labelSmall) }
                                OutlinedButton(
                                    onClick = { viewModel.resizeSelectedItemWidthMm(-1) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("resize_w_minus_1mm")
                                ) { Text("عرض -1 ملم", style = MaterialTheme.typography.labelSmall) }
                                OutlinedButton(
                                    onClick = { viewModel.resizeSelectedItemWidthMm(1) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("resize_w_plus_1mm")
                                ) { Text("عرض +1 ملم", style = MaterialTheme.typography.labelSmall) }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تحريك:", style = MaterialTheme.typography.labelMedium)
                                    FilledTonalIconButton(
                                        onClick = { viewModel.moveSelectedItemBy(20, 0) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "يمين")
                                    }
                                    FilledTonalIconButton(
                                        onClick = { viewModel.moveSelectedItemBy(-20, 0) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "يسار")
                                    }
                                    FilledTonalIconButton(
                                        onClick = { viewModel.moveSelectedItemBy(0, -20) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "أعلى")
                                    }
                                    FilledTonalIconButton(
                                        onClick = { viewModel.moveSelectedItemBy(0, 20) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "أسفل")
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("العرض:", style = MaterialTheme.typography.labelMedium)
                                    FilledTonalIconButton(
                                        onClick = { viewModel.resizeSelectedItemWidth(-10) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "تقليل العرض")
                                    }
                                    Text(
                                        text = state.unitSystem.formatMillimeters(selectedItem.widthMm),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FilledTonalIconButton(
                                        onClick = { viewModel.resizeSelectedItemWidth(10) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة العرض")
                                    }
                                }
                            }
                        }
                    }

                    // -----------------------------------------------------------------
                    // FLOATING MENU 5: Live Firestore Automatic Cost Estimator & NKBA Work Triangle
                    // -----------------------------------------------------------------
                    val triangle = state.workTriangle
                    val cost = state.costBreakdown
                    FloatingCollapsibleWindow(
                        titleAr = "حساب التكلفة التلقائي اللحظي (Firestore) ومثلث العمل (${triangle.score}%)",
                        subtitleAr = "${triangle.statusTitleAr} • الإجمالي المحدث لحظياً: ${cost.totalSar} ر.س (${state.unitSystem.badgeAr})",
                        icon = Icons.Default.RequestQuote,
                        visible = showCostFloatingMenu,
                        onHide = { showCostFloatingMenu = false },
                        fadeWhenDraggingCatalogItem = isDraggingAnything,
                        testTag = "floating_cost_menu"
                    ) {
                        // Real-time itemized breakdown from Firestore catalog (Cabinets, Surfaces, Accessories, Appliances)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "الخزائن: ${cost.cabinetsCostSar} ر.س",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "الأسطح (${state.unitSystem.formatLinearCountertop(cost.countertopLinearMeters)}): ${cost.countertopCostSar} ر.س",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "الإكسسوارات (${cost.linkedSmartAccessoriesCount} ذكي + Blum/Gola): ${cost.accessoriesCostSar} ر.س",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "الأجهزة: ${cost.appliancesCostSar} ر.س",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${triangle.recommendationAr} • متزامن مع أسعار كتالوج Firestore (${cost.firestoreSyncedUnitsCount} وحدة)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "الإجمالي التقديري المحدث لحظياً (${state.placedItems.size} وحدة): ${cost.totalSar} ر.س",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("live_firestore_total_cost_text")
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showMaterialStandardsDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("cost_menu_material_standards_button")
                                ) {
                                    Text("أبعاد المواد", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = {
                                        ProjectExportManager.generateEngineeringPdfReport(
                                            context = context,
                                            state = state,
                                            nesting = state.sheetNesting,
                                            aluminum = state.aluminumNesting,
                                            shareViaMessagingApps = true
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("cost_menu_pdf_share_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("عرض سعر PDF", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { showAutoCadDxfDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("cost_menu_dxf_button")
                                ) {
                                    Text("DXF للورشة", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.saveCurrentProjectToCloud() },
                                    enabled = !state.isSaving,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("studio_save_cloud_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (state.activeProjectId.isBlank()) "حفظ" else "تحديث")
                                }
                                Button(
                                    onClick = onNavigateToQuote,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("studio_request_quote_button")
                                ) {
                                    Text("طلب تسعير وتنفيذ")
                                }
                            }
                        }

                        // Live 3D Mini-Preview Driven by Current Unit Coordinates + Graphical Cost Distribution Chart
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier
                                    .width(176.dp)
                                    .clickable { viewModel.setViewMode(StudioViewMode.RENDER_3D) }
                                    .testTag("live_3d_mini_preview_card"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "معاينة فورية 3D (${state.placedItems.size} وحدة)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Simple3DKitchenMiniPreviewCanvas(
                                        state = state,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(92.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "توزيع التكاليف (المواد: ${cost.materialsTotalSar} • العمالة: ${cost.laborAndCncSar} • الإكسسوارات: ${cost.accessoriesCostSar} ر.س)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (state.complianceReport.violatingUnitUids.isEmpty()) {
                                        "✓ المسافات مطابقة (0 تقاطع)"
                                    } else {
                                        "⚠️ تنبيه: ${state.complianceReport.violatingUnitUids.size} خزانة محاطة بمربع أحمر لتقاطع أو انتهاك المسافة"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.complianceReport.violatingUnitUids.isEmpty()) {
                                        Color(0xFF00C853)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. FLOATING DRAG-AND-DROP GHOST CARD FOLLOWING POINTER
        // =========================================================================
        FloatingCatalogDragGhostOverlay(
            dragSession = activeDragSession,
            containerRootOffset = rootBoxPosition
        )
    }

    if (showRoomConfigDialog) {
        RoomConfigurationDialog(
            state = state,
            onDismiss = { showRoomConfigDialog = false },
            onApply = { name, notes, shape, w, d, h ->
                viewModel.updateProjectMetadata(name, notes)
                viewModel.updateRoomDimensions(w, d, h)
                if (shape != state.shape) {
                    viewModel.applyKitchenShape(shape)
                }
                showRoomConfigDialog = false
            }
        )
    }

    if (showSaveLayoutDialog) {
        Save3DLayoutDialog(
            state = state,
            onDismiss = { showSaveLayoutDialog = false },
            onSaveCurrentOrCreate = { name, notes ->
                viewModel.saveCurrentProjectToCloud(
                    customName = name,
                    customNotes = notes,
                    saveAsNew = false
                )
                showSaveLayoutDialog = false
            },
            onSaveAsNewCopy = { name, notes ->
                viewModel.saveCurrentProjectToCloud(
                    customName = name,
                    customNotes = notes,
                    saveAsNew = true
                )
                showSaveLayoutDialog = false
            }
        )
    }

    if (showRetrieveProjectDialog) {
        RetrieveSavedProjectDialog(
            activeProjectId = state.activeProjectId,
            savedProjects = savedProjects,
            onDismiss = { showRetrieveProjectDialog = false },
            onSelectProject = { project ->
                viewModel.loadSavedProject(project)
                showRetrieveProjectDialog = false
            },
            onOpenAllProjectsTab = {
                showRetrieveProjectDialog = false
                onNavigateToProjects()
            }
        )
    }

    if (showAutoFillerDialog) {
        AutoFillerToolDialog(
            state = state,
            onDismiss = { showAutoFillerDialog = false },
            onApplyFiller = { wallMm, mode, includeUpper ->
                viewModel.applyAutoFillerStrips(
                    mode = mode,
                    customWallLengthMm = wallMm,
                    includeUpperWall = includeUpper
                )
                showAutoFillerDialog = false
            }
        )
    }

    if (showMmPrecisionDialog && selectedItem != null) {
        MillimeterPrecisionItemDialog(
            item = selectedItem,
            onDismiss = { showMmPrecisionDialog = false },
            onApplyMm = { xMm, yMm, wMm, dMm, hMm, elevMm ->
                viewModel.updateSelectedItemExactMillimeters(
                    xMm = xMm,
                    yMm = yMm,
                    widthMm = wMm,
                    depthMm = dMm,
                    heightMm = hMm,
                    elevationMm = elevMm
                )
                showMmPrecisionDialog = false
            }
        )
    }

    if (showSnapshotsDialog) {
        RoomSnapshotsRecoveryDialog(
            snapshots = localSnapshots,
            autoSaveCountdownSeconds = state.autoSaveCountdownSeconds,
            autoSaveCount = state.autoSaveCount,
            onDismiss = { showSnapshotsDialog = false },
            onCreateManualSnapshot = { label ->
                viewModel.saveManualOfflineSnapshot(label = label)
            },
            onRestoreSnapshot = { snap ->
                viewModel.restoreFromOfflineSnapshot(snap)
                showSnapshotsDialog = false
            },
            onDeleteSnapshot = { id ->
                viewModel.deleteOfflineSnapshot(id)
            }
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

    if (showAutoCadDxfDialog) {
        AutoCadDxfStudioDialog(
            state = state,
            onDismiss = { showAutoCadDxfDialog = false },
            onImportDxf = { fileName, rawDxf ->
                viewModel.importAutoCadDxfFileOrContent(fileName, rawDxf)
            }
        )
    }

    if (showGeminiAiDialog) {
        GeminiAiKitchenDesignerDialog(
            state = state,
            onDismiss = { showGeminiAiDialog = false },
            onGenerateWithGemini = { w, d, h, shape, styleAr, prioritiesAr ->
                viewModel.requestGeminiKitchenDesignSuggestion(
                    roomWidthCm = w,
                    roomDepthCm = d,
                    roomHeightCm = h,
                    preferredShape = shape,
                    stylePreferenceAr = styleAr,
                    customPrioritiesAr = prioritiesAr
                )
            }
        )
    }

    if (showLightingSimulationDialog) {
        LightingSimulationStudioDialog(
            state = state,
            onDismiss = { showLightingSimulationDialog = false },
            onSelectPreset = { preset ->
                viewModel.applyLightingSimulationPreset(preset)
            },
            onUpdateParams = { azimuth, nat, spot, underLed, kelvin, shadow, specular ->
                viewModel.updateLightingSimulationParams(
                    sunAzimuthDeg = azimuth,
                    naturalLightIntensity = nat,
                    artificialSpotIntensity = spot,
                    underCabinetLedIntensity = underLed,
                    colorTemperatureKelvin = kelvin,
                    shadowSoftnessAndDepth = shadow,
                    surfaceSpecularReflection = specular
                )
            }
        )
    }

    if (showComplianceAlertsDialog) {
        EngineeringComplianceAlertsDialog(
            state = state,
            onDismiss = { showComplianceAlertsDialog = false },
            onAutoFixAll = {
                viewModel.autoFixEngineeringComplianceViolations()
            },
            onSelectUnit = { uid ->
                viewModel.selectItem(uid)
            }
        )
    }

    if (showSmartAccessoriesDialog) {
        SmartInternalAccessoriesDialog(
            state = state,
            onDismiss = { showSmartAccessoriesDialog = false },
            onToggleAccessory = { code ->
                viewModel.toggleSmartInternalAccessory(code)
            }
        )
    }

    if (showShareBlueprintDialog) {
        ShareBlueprintWithWorkshopAndFriendsDialog(
            state = state,
            onDismiss = { showShareBlueprintDialog = false }
        )
    }

    if (showWallFloorHarmonyDialog) {
        WallAndFloorHarmonyStudioDialog(
            state = state,
            onDismiss = { showWallFloorHarmonyDialog = false },
            onUpdateWallAndFloor = { wallHex, floorMat, cabFinish, counterMat ->
                viewModel.updateFinishes(
                    cabinetFinish = cabFinish,
                    countertopMaterial = counterMat,
                    floorMaterial = floorMat,
                    wallColorHex = wallHex
                )
            },
            onApplyHarmonyPalette = { palette, includeCabinets ->
                viewModel.applyWallAndFloorHarmonyPalette(
                    palette = palette,
                    alsoApplyCabinetAndCountertop = includeCabinets
                )
            }
        )
    }

    if (showCustomCabinetPhotoDialog) {
        CustomCabinetPhotoDesignerDialog(
            state = state,
            onDismiss = { showCustomCabinetPhotoDialog = false },
            onCreateCustomCabinet = { labelAr, baseType, wMm, dMm, hMm, doors, drawers, shelves, fullBack, backThick, fullSides, boardThick, finishName, frontUri, sideUri, saveToCatalog ->
                viewModel.createCustomCabinetFromFrontAndSidePhotos(
                    labelAr = labelAr,
                    baseType = baseType,
                    widthMm = wMm,
                    depthMm = dMm,
                    heightMm = hMm,
                    doorsCount = doors,
                    drawersCount = drawers,
                    shelvesCount = shelves,
                    hasFullBackPanel = fullBack,
                    backPanelThicknessMm = backThick,
                    hasFullSidePanels = fullSides,
                    boardThicknessMm = boardThick,
                    customFinishName = finishName,
                    frontPhotoUriString = frontUri,
                    sidePhotoUriString = sideUri,
                    saveToFirestoreCatalog = saveToCatalog
                )
            }
        )
    }

    if (showMaterialStandardsDialog) {
        StandardMaterialDimensionsSettingsDialog(
            state = state,
            onDismiss = { showMaterialStandardsDialog = false },
            onApplyStandards = { sheetW, sheetH, panelD, boardThick, alumLen, alumW, alumD ->
                viewModel.updateCamMaterialStandards(
                    sheetWidthMm = sheetW,
                    sheetHeightMm = sheetH,
                    standardPanelDepthMm = panelD,
                    boardThicknessMm = boardThick,
                    aluminumBarLengthMm = alumLen,
                    aluminumProfileWidthMm = alumW,
                    aluminumProfileDepthMm = alumD
                )
                showMaterialStandardsDialog = false
            },
            onResetDefaults = {
                viewModel.resetCamMaterialStandardsToDefault()
                showMaterialStandardsDialog = false
            }
        )
    }
}

@Composable
private fun StandardMaterialDimensionsSettingsDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onApplyStandards: (Int, Int, Int, Int, Int, Int, Int) -> Unit,
    onResetDefaults: () -> Unit
) {
    val std = state.camMaterialStandards
    var sheetLengthText by remember(std.sheetWidthMm) { mutableStateOf(std.sheetWidthMm.toString()) }
    var sheetWidthText by remember(std.sheetHeightMm) { mutableStateOf(std.sheetHeightMm.toString()) }
    var panelDepthText by remember(std.standardPanelDepthMm) { mutableStateOf(std.standardPanelDepthMm.toString()) }
    var boardThickText by remember(std.boardThicknessMm) { mutableStateOf(std.boardThicknessMm.toString()) }
    var alumLengthText by remember(std.aluminumBarLengthMm) { mutableStateOf(std.aluminumBarLengthMm.toString()) }
    var alumWidthText by remember(std.aluminumProfileWidthMm) { mutableStateOf(std.aluminumProfileWidthMm.toString()) }
    var alumDepthText by remember(std.aluminumProfileDepthMm) { mutableStateOf(std.aluminumProfileDepthMm.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعدادات الأبعاد القياسية للألواح والبروفايلات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تُعتمد هذه الأبعاد افتراضياً في محرك حساب التكاليف وقائمة القص (Cut-List):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "1. الألواح الخشبية والميلامين (الطول • العرض • العمق • السماكة):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StandardMelamineSheetPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = sheetLengthText == preset.widthMm.toString() && sheetWidthText == preset.heightMm.toString(),
                            onClick = {
                                sheetLengthText = preset.widthMm.toString()
                                sheetWidthText = preset.heightMm.toString()
                            },
                            label = { Text(preset.titleAr, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sheetLengthText,
                        onValueChange = { sheetLengthText = it },
                        label = { Text("طول اللوح (ملم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sheetWidthText,
                        onValueChange = { sheetWidthText = it },
                        label = { Text("عرض اللوح (ملم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = panelDepthText,
                        onValueChange = { panelDepthText = it },
                        label = { Text("العمق القياسي (ملم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = boardThickText,
                        onValueChange = { boardThickText = it },
                        label = { Text("السماكة (ملم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                Text(
                    text = "2. بروفايلات وقضبان الألمنيوم (الطول • العرض • العمق):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StandardAluminumBarPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = alumLengthText == preset.lengthMm.toString(),
                            onClick = { alumLengthText = preset.lengthMm.toString() },
                            label = { Text(preset.titleAr, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alumLengthText,
                        onValueChange = { alumLengthText = it },
                        label = { Text("الطول (ملم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = alumWidthText,
                        onValueChange = { alumWidthText = it },
                        label = { Text("العرض (ملم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = alumDepthText,
                        onValueChange = { alumDepthText = it },
                        label = { Text("العمق (ملم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyStandards(
                        sheetLengthText.toIntOrNull() ?: std.sheetWidthMm,
                        sheetWidthText.toIntOrNull() ?: std.sheetHeightMm,
                        panelDepthText.toIntOrNull() ?: std.standardPanelDepthMm,
                        boardThickText.toIntOrNull() ?: std.boardThicknessMm,
                        alumLengthText.toIntOrNull() ?: std.aluminumBarLengthMm,
                        alumWidthText.toIntOrNull() ?: std.aluminumProfileWidthMm,
                        alumDepthText.toIntOrNull() ?: std.aluminumProfileDepthMm
                    )
                },
                modifier = Modifier.testTag("confirm_standard_dimensions_dialog_button")
            ) {
                Text("اعتماد الأبعاد في التكلفة والقص")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onResetDefaults) {
                    Text("استعادة الافتراضي")
                }
                TextButton(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        }
    )
}

@Composable
private fun Save3DLayoutDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onSaveCurrentOrCreate: (String, String) -> Unit,
    onSaveAsNewCopy: (String, String) -> Unit
) {
    var projectName by remember { mutableStateOf(state.projectName) }
    var projectNotes by remember { mutableStateOf(state.notes) }
    val isUpdatingExisting = state.activeProjectId.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isUpdatingExisting) {
                    "حفظ أو نسخ تصميم المطبخ 3D في Firestore"
                } else {
                    "حفظ تصميم المطبخ 3D في Firestore"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ملخص التصميم ثلاثي الأبعاد الذي سيتم حفظه:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• التخطيط والأبعاد: ${state.shape.titleAr} (${state.roomWidthCm}×${state.roomDepthCm}×${state.roomHeightCm} سم)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• الوحدات والأجهزة الموزعة: ${state.placedItems.size} وحدة ثلاثية الأبعاد",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• الخامات: خزائن ${state.cabinetFinish} | سطح ${state.countertopMaterial}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• زاوية الكاميرا 3D المحفوظة: ${state.activeCameraPreset.titleAr} (${state.cameraYawDeg.roundToInt()}°)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("اسم المشروع في السحابة") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_dialog_project_name_input")
                )

                OutlinedTextField(
                    value = projectNotes,
                    onValueChange = { projectNotes = it },
                    label = { Text("ملاحظات التصميم والتعديلات") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_dialog_project_notes_input")
                )

                if (isUpdatingExisting) {
                    OutlinedButton(
                        onClick = {
                            val copyName = if (projectName == state.projectName) {
                                "$projectName (نسخة محدثة)"
                            } else {
                                projectName
                            }
                            onSaveAsNewCopy(copyName, projectNotes)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_as_new_project_button")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ كنسخة مشروع جديدة ومستقلة في Firestore")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveCurrentOrCreate(projectName, projectNotes) },
                modifier = Modifier.testTag("confirm_save_project_button")
            ) {
                Text(if (isUpdatingExisting) "حفظ التعديلات على المشروع" else "حفظ المشروع في السحابة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun RetrieveSavedProjectDialog(
    activeProjectId: String,
    savedProjects: List<KitchenProject>,
    onDismiss: () -> Unit,
    onSelectProject: (KitchenProject) -> Unit,
    onOpenAllProjectsTab: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "استرجاع وتعديل مشروع مطبخ سابق من Firestore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (savedProjects.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "لا توجد مشاريع محفوظة في Firestore حتى الآن",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "قم بتصميم مطبخك ثلاثي الأبعاد ثم اضغط 'حفظ التصميم 3D في Firestore' لتجده هنا في أي وقت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    savedProjects.forEach { project ->
                        val isCurrent = project.id == activeProjectId
                        val unitCount = KitchenItemsSerializer.decode(project.itemsData).size
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProject(project) }
                                .testTag("quick_retrieve_project_${project.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrent) {
                                        Text(
                                            text = "مفتوح حالياً",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "${KitchenShape.fromCode(project.shape).titleAr} • ${project.roomWidthCm}×${project.roomDepthCm} سم • $unitCount وحدة 3D",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${project.cabinetFinish} • ${project.countertopMaterial} • ${project.totalPriceSar} ر.س",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onOpenAllProjectsTab) {
                Text("إدارة كافة المشاريع والقوالب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun RoomConfigurationDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onApply: (String, String, KitchenShape, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(state.projectName) }
    var notes by remember { mutableStateOf(state.notes) }
    var selectedShape by remember { mutableStateOf(state.shape) }
    var widthSlider by remember { mutableFloatStateOf(state.roomWidthCm.toFloat()) }
    var depthSlider by remember { mutableFloatStateOf(state.roomDepthCm.toFloat()) }
    var heightSlider by remember { mutableFloatStateOf(state.roomHeightCm.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعدادات الغرفة وشكل المطبخ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم مشروع المطبخ") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )

                Text(
                    text = "اختر التخطيط الهندسي للمطبخ:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                KitchenShape.entries.forEach { shape ->
                    val isSelected = selectedShape == shape
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedShape = shape }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = shape.titleAr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = shape.subtitleAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "عرض المطبخ: ${widthSlider.roundToInt()} سم (${widthSlider.roundToInt() * 10} ملم)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = widthSlider,
                        onValueChange = { widthSlider = it },
                        valueRange = 220f..800f
                    )
                }

                Column {
                    Text(
                        text = "عمق المطبخ: ${depthSlider.roundToInt()} سم (${depthSlider.roundToInt() * 10} ملم)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = depthSlider,
                        onValueChange = { depthSlider = it },
                        valueRange = 200f..700f
                    )
                }

                Column {
                    Text(
                        text = "ارتفاع السقف: ${heightSlider.roundToInt()} سم (${heightSlider.roundToInt() * 10} ملم)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = heightSlider,
                        onValueChange = { heightSlider = it },
                        valueRange = 240f..380f
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات ومواصفات خاصة") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        name,
                        notes,
                        selectedShape,
                        widthSlider.roundToInt(),
                        depthSlider.roundToInt(),
                        heightSlider.roundToInt()
                    )
                },
                modifier = Modifier.testTag("apply_room_config_button")
            ) {
                Text("تطبيق التعديلات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
