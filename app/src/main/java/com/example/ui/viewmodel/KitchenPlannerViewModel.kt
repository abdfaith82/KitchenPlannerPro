package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiKitchenDesignService
import com.example.ai.GeminiSuggestedKitchenDesign
import com.example.data.cad.AutoFillerEngine
import com.example.data.cad.CadPhysicsEngine
import com.example.data.cad.EngineeringComplianceAnalyzer
import com.example.data.cad.EngineeringComplianceReport
import com.example.data.cad.FillerCalculationReport
import com.example.data.cad.FillerPlacementMode
import com.example.data.cad.LightingSimulationConfig
import com.example.data.cad.LinkedAccessoryBomItem
import com.example.data.cad.MeasurementUnitSystem
import com.example.data.cad.NaturalDaylightPreset
import com.example.data.cad.SmartAccessoriesCatalog
import com.example.data.cad.UnitClearanceDistancesMm
import com.example.data.cad.VirtualRulerEngine
import com.example.data.cam.AluminumNestingSummary
import com.example.data.cam.CamMaterialStandardsConfig
import com.example.data.cam.CamNestingEngine
import com.example.data.cam.JsonCadEngine
import com.example.data.cam.SheetNestingSummary
import com.example.data.cam.StandardAluminumBarPreset
import com.example.data.cam.StandardMelamineSheetPreset
import com.example.data.local.LocalProjectSnapshotEntity
import com.example.data.local.LocalSnapshotRepository
import com.example.data.model.CameraPreset3D
import com.example.data.model.CertifiedShowroom
import com.example.data.model.ErgonomicAnalyzer
import com.example.data.model.KitchenCatalogData
import com.example.data.model.KitchenCatalogUnit
import com.example.data.model.KitchenItemsSerializer
import com.example.data.model.KitchenProject
import com.example.data.model.KitchenShape
import com.example.data.model.KitchenStylePreset
import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import com.example.data.model.QuoteRequest
import com.example.data.model.Saved3DViewportSnapshot
import com.example.data.model.WorkTriangleAnalysis
import com.example.data.repository.KitchenPlannerRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StudioViewMode(val titleAr: String) {
    PLAN_2D("مسقط هندسي 2D (1mm)"),
    RENDER_3D("منظور مداري 3D Orbit"),
    WALKTHROUGH_FP("تجول داخلي (شخص أول)"),
    VR_CAMERA_WALKTHROUGH("تجول واقع افتراضي بالحساس والكاميرا (VR)")
}

enum class LightingMode(val titleAr: String) {
    DAYLIGHT("إضاءة نهارية PBR"),
    WARM_LED("إضاءة LED 3000K للفيترينات"),
    BLUEPRINT_WIRE("مخطط هندسي CAD");

    companion object {
        fun fromNameOrDefault(name: String): LightingMode =
            entries.firstOrNull { it.name == name } ?: DAYLIGHT
    }
}

data class CostBreakdown(
    val cabinetsCostSar: Int,
    val appliancesCostSar: Int,
    val countertopLinearMeters: Float,
    val countertopLinearFeet: Float = (countertopLinearMeters * 3.28084f * 10f).roundToInt() / 10f,
    val countertopCostSar: Int,
    val accessoriesCostSar: Int = 0,
    val smartInternalAccessoriesCostSar: Int = 0,
    val linkedSmartAccessoriesCount: Int = 0,
    val installationAndDeliverySar: Int,
    val totalSar: Int,
    val firestoreSyncedUnitsCount: Int = 0,
    val sheetsMaterialCostSar: Int = 0,
    val aluminumProfilesCostSar: Int = 0,
    val materialsTotalSar: Int = cabinetsCostSar + countertopCostSar,
    val laborAndCncSar: Int = installationAndDeliverySar
)

data class PlannerStudioState(
    val activeProjectId: String = "",
    val projectName: String = "مطبخ فيلا الياسمين الحديث",
    val shape: KitchenShape = KitchenShape.ISLAND,
    val styleId: String = "MODERN_WALNUT",
    val roomWidthCm: Int = 440,
    val roomDepthCm: Int = 380,
    val roomHeightCm: Int = 285,
    val fineWallLengthMmOverride: Int? = null,
    val unitSystem: MeasurementUnitSystem = MeasurementUnitSystem.METRIC,
    val cabinetFinish: String = "جوز أمريكي طبيعي",
    val countertopMaterial: String = "رخام كالكاتا إيطالي",
    val handleStyle: String = "مقابض مدمجة مخفية (Gola)",
    val wallColorHex: String = "#F5F2EB",
    val floorMaterial: String = "باركيه بلوط دافئ",
    val notes: String = "تصميم 18mm CNC مع جزيرة وسطية وإضاءة LED 3000K",
    val placedItems: List<PlacedKitchenItem> = KitchenCatalogData.buildDefaultLayoutForShape(
        KitchenShape.ISLAND,
        440,
        380
    ),
    val firestoreCatalogUnits: List<KitchenCatalogUnit> = emptyList(),
    val selectedItemUid: String? = "item_island",
    val viewMode: StudioViewMode = StudioViewMode.RENDER_3D,
    val lightingMode: LightingMode = LightingMode.DAYLIGHT,
    val lightingSimulation: LightingSimulationConfig = LightingSimulationConfig(),
    val enabledAccessoryCodes: Set<String> = SmartAccessoriesCatalog.defaultEnabledCodes,
    val activeCameraPreset: CameraPreset3D = CameraPreset3D.ISOMETRIC_RIGHT,
    val showWorkTriangle: Boolean = true,
    val showDimensions: Boolean = true,
    val showVirtualRuler: Boolean = true,
    val virtualRulerPinAMm: Pair<Int, Int>? = null,
    val virtualRulerPinBMm: Pair<Int, Int>? = null,
    val doorsOpenPreview: Boolean = false,
    val magneticSnapEnabled: Boolean = true,
    val antiOverlapEnabled: Boolean = true,
    val upperSafetyClearanceMm: Int = 600,
    val snapTelemetryAr: String = "الالتصاق المغناطيسي ومنع التداخل مفعّل (دقة 1 ملم)",
    val cameraYawDeg: Float = 38f,
    val cameraPitchFactor: Float = 0.54f,
    val cameraZoom: Float = 1.0f,
    val firstPersonStepForwardCm: Float = 0f,
    val vrStrafeLateralCm: Float = 0f,
    val vrEyeHeightCm: Float = 155f,
    val vrStereoscopicSplitEnabled: Boolean = false,
    val vrCameraSensorTrackingEnabled: Boolean = true,
    val vrAutoWalkEnabled: Boolean = false,
    val vrCameraPipVisible: Boolean = true,
    val vrSensorDeltaFlowX: Float = 0f,
    val vrSensorDeltaFlowY: Float = 0f,
    val vrSensorStepCount: Int = 0,
    val camMaterialStandards: CamMaterialStandardsConfig = CamMaterialStandardsConfig(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val autoSaveCount: Int = 0,
    val autoSaveIntervalSeconds: Int = 30,
    val autoSaveCountdownSeconds: Int = 30,
    val lastAutoSaveTimeText: String = "حفظ تلقائي كل 30 ثانية (Room DB)",
    val latestRecoverableSnapshot: LocalProjectSnapshotEntity? = null,
    val lastGeneratedJsonV2: String? = null,
    val isGeneratingGeminiDesign: Boolean = false,
    val lastGeminiSuggestion: GeminiSuggestedKitchenDesign? = null,
    val geminiSuggestionError: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val statusBannerMessage: String? = null,
    val errorBannerMessage: String? = null
) {
    val roomWidthMm: Int
        get() = fineWallLengthMmOverride ?: (roomWidthCm * 10)

    val roomDepthMm: Int
        get() = roomDepthCm * 10

    val roomHeightMm: Int
        get() = roomHeightCm * 10

    val workTriangle: WorkTriangleAnalysis
        get() = ErgonomicAnalyzer.analyze(placedItems)

    val complianceReport: EngineeringComplianceReport
        get() = EngineeringComplianceAnalyzer.validateLayout(placedItems, roomWidthMm, roomDepthMm)

    val linkedAccessoriesBom: List<LinkedAccessoryBomItem>
        get() = SmartAccessoriesCatalog.computeAutoLinkedAccessoriesBom(placedItems, enabledAccessoryCodes)

    val selectedUnitClearanceDistances: UnitClearanceDistancesMm?
        get() {
            val target = placedItems.firstOrNull { it.uid == selectedItemUid }
                ?: placedItems.firstOrNull()
                ?: return null
            return VirtualRulerEngine.measureUnitDistances(target, placedItems, roomWidthMm, roomDepthMm)
        }

    val fillerReport: FillerCalculationReport
        get() = AutoFillerEngine.analyzeWallGaps(placedItems, roomWidthMm)

    val sheetNesting: SheetNestingSummary
        get() = CamNestingEngine.computeSheetNesting(
            items = placedItems,
            cabinetFinish = cabinetFinish,
            standardSheetWidthMm = camMaterialStandards.sheetWidthMm,
            standardSheetHeightMm = camMaterialStandards.sheetHeightMm,
            boardThicknessMm = camMaterialStandards.boardThicknessMm,
            standardPanelDepthMm = camMaterialStandards.standardPanelDepthMm,
            sawKerfMm = camMaterialStandards.sawKerfMm,
            edgeTrimMm = camMaterialStandards.sheetEdgeTrimMm
        )

    val aluminumNesting: AluminumNestingSummary
        get() = CamNestingEngine.computeAluminumBarNesting(
            items = placedItems,
            standardBarLengthMm = camMaterialStandards.aluminumBarLengthMm,
            profileWidthMm = camMaterialStandards.aluminumProfileWidthMm,
            profileDepthMm = camMaterialStandards.aluminumProfileDepthMm,
            sawKerfMm = camMaterialStandards.aluminumSawKerfMm
        )

    val costBreakdown: CostBreakdown
        get() {
            val finishMult = KitchenCatalogData.cabinetFinishes
                .firstOrNull { it.nameAr == cabinetFinish }?.priceMultiplierPercent ?: 0
            val counterMult = KitchenCatalogData.countertopMaterials
                .firstOrNull { it.nameAr == countertopMaterial }?.priceMultiplierPercent ?: 0

            val catalogByCode = firestoreCatalogUnits.associateBy { it.code }
            var syncedCount = 0

            fun resolveFirestoreSyncedItemPrice(item: PlacedKitchenItem): Int {
                val catalogMatch = catalogByCode[item.type.code]
                return if (catalogMatch != null && catalogMatch.priceSar > 0L) {
                    syncedCount++
                    val catalogWidthMm = (catalogMatch.widthCm.toInt() * 10).coerceAtLeast(100)
                    val widthRatio = item.widthMm.toFloat() / catalogWidthMm.toFloat()
                    // Blend live Firestore catalog base price with 1mm resized width ratio
                    (catalogMatch.priceSar.toFloat() * widthRatio).roundToInt().coerceAtLeast(150)
                } else {
                    item.priceSar
                }
            }

            // Standard Sheet & Panel Dimension Factor (Length x Width x Depth/Thickness)
            val currentSheets = sheetNesting
            val currentAluminum = aluminumNesting
            val thicknessFactor = camMaterialStandards.boardThicknessMm.toFloat() / 18f
            val depthFactor = camMaterialStandards.standardPanelDepthMm.toFloat() / 600f
            val sheetsRawCostSar = (currentSheets.totalSheets18Mm * camMaterialStandards.sheetUnitPriceSar * thicknessFactor).roundToInt()

            val profileSectionFactor = ((camMaterialStandards.aluminumProfileWidthMm * camMaterialStandards.aluminumProfileDepthMm).toFloat() / (45f * 22f)).coerceIn(0.6f, 2.2f)
            val aluminumBarsCostSar = (currentAluminum.totalBarsCount * camMaterialStandards.aluminumBarUnitPriceSar * profileSectionFactor).roundToInt()

            val baseCabinetsSum = placedItems
                .filter { it.type.category.code != "APPLIANCE" }
                .sumOf { item ->
                    val basePrice = resolveFirestoreSyncedItemPrice(item)
                    val unitFinishName = item.customCabinetFinish ?: cabinetFinish
                    val unitFinishMult = KitchenCatalogData.cabinetFinishes
                        .firstOrNull { it.nameAr == unitFinishName }?.priceMultiplierPercent ?: finishMult
                    (basePrice * (100 + unitFinishMult) / 100.0).roundToInt()
                }

            val standardMaterialDeltaSar = ((sheetsRawCostSar - (currentSheets.totalSheets18Mm * 245)) +
                ((depthFactor - 1f) * baseCabinetsSum * 0.22f)).roundToInt()
            val adjustedCabinets = (baseCabinetsSum + standardMaterialDeltaSar).coerceAtLeast(0)

            val appliancesCost = placedItems
                .filter { it.type.category.code == "APPLIANCE" }
                .sumOf { resolveFirestoreSyncedItemPrice(it) }

            val counterItems = placedItems.filter {
                it.type.category.code == "BASE" ||
                    it.type.category.code == "ISLAND" ||
                    it.type.category.code == "SINK"
            }
            val baseAndIslandWidthMm = counterItems.sumOf { it.widthMm }
            val linearMeters = (baseAndIslandWidthMm / 1000f).coerceAtLeast(1.5f)
            val baseCounterRatePerMeter = 1150f * depthFactor.coerceIn(0.75f, 1.45f)
            val countertopCost = if (counterItems.isEmpty()) {
                (linearMeters * baseCounterRatePerMeter * (100 + counterMult) / 100f).roundToInt()
            } else {
                counterItems.sumOf { item ->
                    val itemMeters = (item.widthMm / 1000f).coerceAtLeast(0.3f)
                    val itemCounterName = item.customCountertopMaterial ?: countertopMaterial
                    val itemCounterMult = KitchenCatalogData.countertopMaterials
                        .firstOrNull { it.nameAr == itemCounterName }?.priceMultiplierPercent ?: counterMult
                    (itemMeters * baseCounterRatePerMeter * (100 + itemCounterMult) / 100f).roundToInt()
                }
            }

            // Automatic Accessories Cost (Blum soft-close hinges, drawer runners, Gola profiles, LED 3000K, PVC edge)
            val cabinetUnits = placedItems.filter { it.type.category.code != "APPLIANCE" }
            val hingesAndRunnersCost = cabinetUnits.sumOf { unit ->
                when (unit.type.category.code) {
                    "BASE" -> 185
                    "WALL" -> 140
                    "TALL" -> 290
                    "ISLAND" -> 340
                    "SINK" -> 210
                    else -> 95
                }
            }
            val golaAndLightingCost = aluminumBarsCostSar +
                placedItems.count { it.type == ModuleType.WALL_GLASS_80 } * 180
            val smartInternalItems = linkedAccessoriesBom
            val smartAccessoriesCost = smartInternalItems.sumOf { it.totalCostSar }
            val smartAccessoriesCount = smartInternalItems.sumOf { it.matchedUnitsCount }
            val accessoriesCost = hingesAndRunnersCost + golaAndLightingCost + smartAccessoriesCost

            val subtotal = adjustedCabinets + appliancesCost + countertopCost + accessoriesCost
            val cncLaborCost = (currentSheets.totalSheets18Mm * 85 + currentAluminum.totalBarsCount * 35)
            val installCost = (subtotal * 0.07f).roundToInt() + cncLaborCost
            val total = subtotal + installCost

            return CostBreakdown(
                cabinetsCostSar = adjustedCabinets,
                appliancesCostSar = appliancesCost,
                countertopLinearMeters = (linearMeters * 10f).roundToInt() / 10f,
                countertopLinearFeet = (linearMeters * 3.28084f * 10f).roundToInt() / 10f,
                countertopCostSar = countertopCost,
                accessoriesCostSar = accessoriesCost,
                smartInternalAccessoriesCostSar = smartAccessoriesCost,
                linkedSmartAccessoriesCount = smartAccessoriesCount,
                installationAndDeliverySar = installCost,
                totalSar = total,
                firestoreSyncedUnitsCount = syncedCount,
                sheetsMaterialCostSar = sheetsRawCostSar,
                aluminumProfilesCostSar = aluminumBarsCostSar,
                materialsTotalSar = adjustedCabinets + countertopCost,
                laborAndCncSar = installCost
            )
        }
}

class KitchenPlannerViewModel(
    private val repository: KitchenPlannerRepository,
    private val currentUserId: String,
    private val localSnapshotRepository: LocalSnapshotRepository? = null
) : ViewModel() {

    private val _studioState = MutableStateFlow(PlannerStudioState())
    val studioState: StateFlow<PlannerStudioState> = _studioState.asStateFlow()

    private val undoStack = ArrayDeque<List<PlacedKitchenItem>>()
    private val redoStack = ArrayDeque<List<PlacedKitchenItem>>()

    val localSnapshotsState: StateFlow<List<LocalProjectSnapshotEntity>> =
        (localSnapshotRepository?.recentSnapshots ?: emptyFlow())
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val catalogUnitsState: StateFlow<UiState<List<KitchenCatalogUnit>>> =
        repository.observeCatalogUnits(currentUserId)
            .map<List<KitchenCatalogUnit>, UiState<List<KitchenCatalogUnit>>> { list ->
                val displayList = if (list.isEmpty()) {
                    KitchenCatalogData.defaultCatalogUnitsForUser(currentUserId)
                } else {
                    list.sortedBy { it.category }
                }
                // Sync live Firestore catalog prices directly into PlannerStudioState for real-time cost calculation
                _studioState.update { current ->
                    current.copy(firestoreCatalogUnits = displayList)
                }
                UiState.Success(displayList)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing kitchen units catalog", rootError)
                emit(UiState.Error(rootError.message ?: "تعذر تحميل كتالوج وحدات المطبخ"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val userProjectsState: StateFlow<UiState<List<KitchenProject>>> =
        repository.observeUserProjects(currentUserId)
            .map<List<KitchenProject>, UiState<List<KitchenProject>>> { list ->
                val sorted = list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                UiState.Success(sorted)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user kitchen projects", rootError)
                emit(UiState.Error(rootError.message ?: "تعذر تحميل مشاريع المطابخ المحفوظة"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val userQuotesState: StateFlow<UiState<List<QuoteRequest>>> =
        repository.observeUserQuoteRequests(currentUserId)
            .map<List<QuoteRequest>, UiState<List<QuoteRequest>>> { list ->
                val sorted = list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                UiState.Success(sorted)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing quote requests", rootError)
                emit(UiState.Error(rootError.message ?: "تعذر تحميل طلبات عروض الأسعار"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    init {
        // Initialize default Firestore catalog units in state immediately, then sync live from Firestore
        _studioState.update {
            it.copy(firestoreCatalogUnits = KitchenCatalogData.defaultCatalogUnitsForUser(currentUserId))
        }
        viewModelScope.launch {
            repository.seedDefaultCatalogIfEmpty(currentUserId)
        }
        // Start Periodic Local Room Auto-Save coroutine every 30 seconds (30_000 ms)
        if (localSnapshotRepository != null) {
            viewModelScope.launch {
                val previousLatest = runCatching { localSnapshotRepository.getLatestSnapshot() }.getOrNull()
                if (previousLatest != null) {
                    _studioState.update { it.copy(latestRecoverableSnapshot = previousLatest) }
                }
                recordLocalRoomSnapshot(reason = "لقطة بداية الجلسة (حفظ تلقائي Room)", isAutoSave = true)
                while (true) {
                    for (remainingSec in AUTO_SAVE_INTERVAL_SECONDS downTo 1) {
                        _studioState.update { it.copy(autoSaveCountdownSeconds = remainingSec) }
                        delay(1_000L)
                    }
                    recordLocalRoomSnapshot(
                        reason = "حفظ تلقائي دوري كل 30 ثانية (Room DB)",
                        isAutoSave = true
                    )
                }
            }
        }
    }

    private fun pushUndoSnapshot(items: List<PlacedKitchenItem>) {
        if (undoStack.size >= 25) {
            undoStack.removeFirst()
        }
        undoStack.addLast(items)
        redoStack.clear()
    }

    fun undoLastItemChange() {
        val previous = undoStack.removeLastOrNull() ?: return
        val current = _studioState.value.placedItems
        redoStack.addLast(current)
        _studioState.update {
            it.copy(
                placedItems = previous,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                statusBannerMessage = "تم التراجع عن التعديل الأخير (Undo)"
            )
        }
    }

    fun redoLastItemChange() {
        val next = redoStack.removeLastOrNull() ?: return
        val current = _studioState.value.placedItems
        undoStack.addLast(current)
        _studioState.update {
            it.copy(
                placedItems = next,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                statusBannerMessage = "تمت إعادة التعديل (Redo)"
            )
        }
    }

    fun saveManualOfflineSnapshot(label: String = "نسخة حفظ يدوية") {
        viewModelScope.launch {
            recordLocalRoomSnapshot(reason = label, isAutoSave = false)
            _studioState.update {
                it.copy(statusBannerMessage = "تم حفظ لقطة استعادة فورية في قاعدة بيانات Room المحلية")
            }
        }
    }

    /**
     * Triggered immediately when the app goes to background / closes suddenly or connection drops,
     * ensuring zero data loss on top of the 30-second periodic Room auto-save loop.
     */
    fun triggerImmediateAutoSaveToRoom(
        reason: String = "حفظ تلقائي وقائي عند إغلاق التطبيق أو انقطاع الاتصال"
    ) {
        viewModelScope.launch {
            recordLocalRoomSnapshot(reason = reason, isAutoSave = true)
        }
    }

    private suspend fun recordLocalRoomSnapshot(reason: String, isAutoSave: Boolean) {
        val localRepo = localSnapshotRepository ?: return
        val s = _studioState.value
        val viewport = Saved3DViewportSnapshot(
            cameraYawDeg = s.cameraYawDeg,
            cameraPitchFactor = s.cameraPitchFactor,
            cameraZoom = s.cameraZoom,
            cameraPreset = s.activeCameraPreset,
            lightingModeName = s.lightingMode.name,
            doorsOpenPreview = s.doorsOpenPreview
        )
        val encoded = KitchenItemsSerializer.encodeWithViewport(s.placedItems, viewport)
        val entity = LocalProjectSnapshotEntity(
            projectId = s.activeProjectId.ifBlank { "local_draft" },
            projectName = s.projectName,
            shapeCode = s.shape.code,
            styleId = s.styleId,
            roomWidthMm = s.roomWidthMm,
            roomDepthMm = s.roomDepthMm,
            roomHeightMm = s.roomHeightMm,
            cabinetFinish = s.cabinetFinish,
            countertopMaterial = s.countertopMaterial,
            handleStyle = s.handleStyle,
            wallColorHex = s.wallColorHex,
            floorMaterial = s.floorMaterial,
            itemsData = encoded,
            totalPriceSar = s.costBreakdown.totalSar,
            ergonomicScore = s.workTriangle.score,
            snapshotReason = reason,
            isAutoSave = isAutoSave
        )
        runCatching {
            localRepo.saveSnapshot(entity)
            _studioState.update {
                it.copy(
                    autoSaveCount = it.autoSaveCount + 1,
                    autoSaveCountdownSeconds = AUTO_SAVE_INTERVAL_SECONDS,
                    latestRecoverableSnapshot = entity,
                    lastAutoSaveTimeText = "حفظ تلقائي كل 30ث (#${it.autoSaveCount + 1})"
                )
            }
        }
    }

    fun restoreFromOfflineSnapshot(snapshot: LocalProjectSnapshotEntity) {
        pushUndoSnapshot(_studioState.value.placedItems)
        val decoded = KitchenItemsSerializer.decodeWithViewport(snapshot.itemsData)
        val vp = decoded.viewport
        _studioState.update { current ->
            current.copy(
                projectName = snapshot.projectName,
                shape = KitchenShape.fromCode(snapshot.shapeCode),
                styleId = snapshot.styleId,
                roomWidthCm = (snapshot.roomWidthMm / 10).coerceAtLeast(180),
                roomDepthCm = (snapshot.roomDepthMm / 10).coerceAtLeast(180),
                roomHeightCm = (snapshot.roomHeightMm / 10).coerceAtLeast(220),
                fineWallLengthMmOverride = snapshot.roomWidthMm,
                cabinetFinish = snapshot.cabinetFinish,
                countertopMaterial = snapshot.countertopMaterial,
                handleStyle = snapshot.handleStyle,
                wallColorHex = snapshot.wallColorHex,
                floorMaterial = snapshot.floorMaterial,
                placedItems = decoded.items.ifEmpty { current.placedItems },
                selectedItemUid = decoded.items.firstOrNull()?.uid,
                cameraYawDeg = vp?.cameraYawDeg ?: current.cameraYawDeg,
                cameraPitchFactor = vp?.cameraPitchFactor ?: current.cameraPitchFactor,
                cameraZoom = vp?.cameraZoom ?: current.cameraZoom,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusBannerMessage = "تمت استعادة لقطة Room: ${snapshot.snapshotReason}"
            )
        }
    }

    fun deleteOfflineSnapshot(id: Long) {
        val localRepo = localSnapshotRepository ?: return
        viewModelScope.launch {
            localRepo.deleteSnapshot(id)
        }
    }

    fun setViewMode(mode: StudioViewMode) {
        _studioState.update { current ->
            when (mode) {
                StudioViewMode.WALKTHROUGH_FP -> current.copy(
                    viewMode = mode,
                    activeCameraPreset = CameraPreset3D.FIRST_PERSON_EYE,
                    cameraYawDeg = 12f,
                    cameraPitchFactor = 0.25f,
                    cameraZoom = 1.42f,
                    firstPersonStepForwardCm = 20f
                )
                StudioViewMode.VR_CAMERA_WALKTHROUGH -> current.copy(
                    viewMode = mode,
                    activeCameraPreset = CameraPreset3D.FIRST_PERSON_EYE,
                    cameraYawDeg = 10f,
                    cameraPitchFactor = 0.28f,
                    cameraZoom = 1.48f,
                    firstPersonStepForwardCm = 28f,
                    vrStrafeLateralCm = 0f,
                    vrEyeHeightCm = 155f,
                    snapTelemetryAr = "وضع التجول الافتراضي VR نشط: حرك الكاميرا أو الجهاز للالتفات والإمالة والمشي داخل المطبخ 3D",
                    statusBannerMessage = "تم تفعيل وضع التجول الافتراضي بالحساس والكاميرا (VR Navigation Mode)"
                )
                else -> current.copy(viewMode = mode)
            }
        }
    }

    fun stepFirstPersonWalkthrough(deltaCm: Float) {
        _studioState.update { current ->
            val nextStep = (current.firstPersonStepForwardCm + deltaCm).coerceIn(-120f, 180f)
            val nextZoom = (1.35f + nextStep * 0.0025f).coerceIn(0.95f, 1.95f)
            current.copy(
                firstPersonStepForwardCm = nextStep,
                cameraZoom = nextZoom
            )
        }
    }

    fun applyVrCameraSensorMotion(
        panYawDeltaDeg: Float,
        tiltPitchDelta: Float,
        walkForwardDeltaCm: Float,
        walkStrafeDeltaCm: Float = 0f,
        flowX: Float = 0f,
        flowY: Float = 0f,
        detectedStep: Boolean = false
    ) {
        _studioState.update { current ->
            if (current.viewMode != StudioViewMode.VR_CAMERA_WALKTHROUGH && current.viewMode != StudioViewMode.WALKTHROUGH_FP) {
                return@update current
            }
            var nextYaw = (current.cameraYawDeg + panYawDeltaDeg) % 360f
            if (nextYaw > 180f) nextYaw -= 360f
            if (nextYaw < -180f) nextYaw += 360f
            val nextPitch = (current.cameraPitchFactor + tiltPitchDelta).coerceIn(0.14f, 0.72f)
            val nextForward = (current.firstPersonStepForwardCm + walkForwardDeltaCm).coerceIn(-140f, 200f)
            val nextStrafe = (current.vrStrafeLateralCm + walkStrafeDeltaCm).coerceIn(-160f, 160f)
            val nextZoom = (1.38f + nextForward * 0.0022f).coerceIn(0.92f, 2.05f)
            val nextSteps = if (detectedStep) current.vrSensorStepCount + 1 else current.vrSensorStepCount

            current.copy(
                cameraYawDeg = nextYaw,
                cameraPitchFactor = nextPitch,
                firstPersonStepForwardCm = nextForward,
                vrStrafeLateralCm = nextStrafe,
                cameraZoom = nextZoom,
                vrSensorDeltaFlowX = flowX,
                vrSensorDeltaFlowY = flowY,
                vrSensorStepCount = nextSteps
            )
        }
    }

    fun moveVrWalkthroughPosition(
        forwardDeltaCm: Float,
        strafeDeltaCm: Float = 0f,
        eyeHeightDeltaCm: Float = 0f
    ) {
        _studioState.update { current ->
            val nextForward = (current.firstPersonStepForwardCm + forwardDeltaCm).coerceIn(-140f, 200f)
            val nextStrafe = (current.vrStrafeLateralCm + strafeDeltaCm).coerceIn(-160f, 160f)
            val nextEyeH = (current.vrEyeHeightCm + eyeHeightDeltaCm).coerceIn(105f, 195f)
            val nextZoom = (1.38f + nextForward * 0.0022f).coerceIn(0.92f, 2.05f)
            val nextSteps = if (kotlin.math.abs(forwardDeltaCm) >= 8f || kotlin.math.abs(strafeDeltaCm) >= 8f) {
                current.vrSensorStepCount + 1
            } else {
                current.vrSensorStepCount
            }
            current.copy(
                firstPersonStepForwardCm = nextForward,
                vrStrafeLateralCm = nextStrafe,
                vrEyeHeightCm = nextEyeH,
                cameraZoom = nextZoom,
                vrSensorStepCount = nextSteps,
                snapTelemetryAr = "موقع التجول VR: تقدم ${nextForward.roundToInt()} سم • إزاحة جانبية ${nextStrafe.roundToInt()} سم • ارتفاع العين ${nextEyeH.roundToInt()} سم"
            )
        }
    }

    fun resetVrWalkthroughPosition() {
        _studioState.update { current ->
            current.copy(
                cameraYawDeg = 10f,
                cameraPitchFactor = 0.28f,
                cameraZoom = 1.48f,
                firstPersonStepForwardCm = 24f,
                vrStrafeLateralCm = 0f,
                vrEyeHeightCm = 155f,
                vrSensorDeltaFlowX = 0f,
                vrSensorDeltaFlowY = 0f,
                snapTelemetryAr = "تمت إعادة ضبط نقطة التجول الافتراضي VR لمركز المطبخ"
            )
        }
    }

    fun toggleVrStereoscopicSplit() {
        _studioState.update { current ->
            val next = !current.vrStereoscopicSplitEnabled
            current.copy(
                vrStereoscopicSplitEnabled = next,
                statusBannerMessage = if (next) {
                    "تم تفعيل عرض النظارة الافتراضية المزدوج (Stereoscopic Dual-Eye VR)"
                } else {
                    "تم التبديل إلى عرض التجول الافتراضي بشاشة كاملة (Full-Screen VR)"
                }
            )
        }
    }

    fun toggleVrCameraSensorTracking() {
        _studioState.update { current ->
            val next = !current.vrCameraSensorTrackingEnabled
            current.copy(
                vrCameraSensorTrackingEnabled = next,
                snapTelemetryAr = if (next) {
                    "تتبع حساس الكاميرا والمستشعرات في الوقت الفعلي مفعّل (Optical Flow + Gyro)"
                } else {
                    "تم إيقاف تتبع الحساس التلقائي — يمكنك التحكم اليدوي باللمس وأزرار المشي"
                }
            )
        }
    }

    fun toggleVrAutoWalk() {
        _studioState.update { current ->
            val next = !current.vrAutoWalkEnabled
            current.copy(
                vrAutoWalkEnabled = next,
                snapTelemetryAr = if (next) {
                    "المشي التلقائي المستمر داخل المطبخ 3D مفعّل"
                } else {
                    "تم إيقاف المشي التلقائي المستمر"
                }
            )
        }
    }

    fun toggleVrCameraPipVisible() {
        _studioState.update { current ->
            current.copy(vrCameraPipVisible = !current.vrCameraPipVisible)
        }
    }

    fun updateCamMaterialStandards(
        sheetWidthMm: Int = _studioState.value.camMaterialStandards.sheetWidthMm,
        sheetHeightMm: Int = _studioState.value.camMaterialStandards.sheetHeightMm,
        boardThicknessMm: Int = _studioState.value.camMaterialStandards.boardThicknessMm,
        standardPanelDepthMm: Int = _studioState.value.camMaterialStandards.standardPanelDepthMm,
        sawKerfMm: Int = _studioState.value.camMaterialStandards.sawKerfMm,
        sheetEdgeTrimMm: Int = _studioState.value.camMaterialStandards.sheetEdgeTrimMm,
        aluminumBarLengthMm: Int = _studioState.value.camMaterialStandards.aluminumBarLengthMm,
        aluminumProfileWidthMm: Int = _studioState.value.camMaterialStandards.aluminumProfileWidthMm,
        aluminumProfileDepthMm: Int = _studioState.value.camMaterialStandards.aluminumProfileDepthMm,
        aluminumSawKerfMm: Int = _studioState.value.camMaterialStandards.aluminumSawKerfMm,
        sheetUnitPriceSar: Int = _studioState.value.camMaterialStandards.sheetUnitPriceSar,
        aluminumBarUnitPriceSar: Int = _studioState.value.camMaterialStandards.aluminumBarUnitPriceSar
    ) {
        val safeConfig = CamMaterialStandardsConfig(
            sheetWidthMm = sheetWidthMm.coerceIn(1000, 4200),
            sheetHeightMm = sheetHeightMm.coerceIn(600, 2800),
            boardThicknessMm = boardThicknessMm.coerceIn(12, 25),
            standardPanelDepthMm = standardPanelDepthMm.coerceIn(300, 950),
            backPanelThicknessMm = boardThicknessMm.coerceIn(12, 25),
            sawKerfMm = sawKerfMm.coerceIn(1, 15),
            sheetEdgeTrimMm = sheetEdgeTrimMm.coerceIn(0, 40),
            aluminumBarLengthMm = aluminumBarLengthMm.coerceIn(1500, 8000),
            aluminumProfileWidthMm = aluminumProfileWidthMm.coerceIn(15, 120),
            aluminumProfileDepthMm = aluminumProfileDepthMm.coerceIn(10, 90),
            aluminumSawKerfMm = aluminumSawKerfMm.coerceIn(1, 15),
            sheetUnitPriceSar = sheetUnitPriceSar.coerceIn(80, 1500),
            aluminumBarUnitPriceSar = aluminumBarUnitPriceSar.coerceIn(40, 900)
        )
        _studioState.update { current ->
            val updatedState = current.copy(
                camMaterialStandards = safeConfig,
                hasUnsavedChanges = true
            )
            val newSheets = updatedState.sheetNesting.totalSheets18Mm
            val newBars = updatedState.aluminumNesting.totalBarsCount
            val newTotalCost = updatedState.costBreakdown.totalSar
            updatedState.copy(
                snapTelemetryAr = "تم تحديث الأبعاد القياسية للمواد: لوح ${safeConfig.sheetWidthMm}×${safeConfig.sheetHeightMm}×${safeConfig.boardThicknessMm}mm (عمق ${safeConfig.standardPanelDepthMm}mm • $newSheets لوح) • بروفايل ${safeConfig.aluminumBarLengthMm}×${safeConfig.aluminumProfileWidthMm}×${safeConfig.aluminumProfileDepthMm}mm ($newBars قضيب) • التكلفة: $newTotalCost ر.س",
                statusBannerMessage = "تم اعتماد الأبعاد القياسية للألواح (${safeConfig.sheetWidthMm}×${safeConfig.sheetHeightMm}×${safeConfig.standardPanelDepthMm} ملم) والبروفايلات (${safeConfig.aluminumBarLengthMm}×${safeConfig.aluminumProfileWidthMm}×${safeConfig.aluminumProfileDepthMm} ملم) في حساب التكاليف وقائمة القص"
            )
        }
    }

    fun applyMelamineSheetPreset(preset: StandardMelamineSheetPreset) {
        val current = _studioState.value.camMaterialStandards
        updateCamMaterialStandards(
            sheetWidthMm = preset.widthMm,
            sheetHeightMm = preset.heightMm,
            boardThicknessMm = current.boardThicknessMm,
            standardPanelDepthMm = current.standardPanelDepthMm,
            sawKerfMm = current.sawKerfMm,
            sheetEdgeTrimMm = current.sheetEdgeTrimMm,
            aluminumBarLengthMm = current.aluminumBarLengthMm,
            aluminumProfileWidthMm = current.aluminumProfileWidthMm,
            aluminumProfileDepthMm = current.aluminumProfileDepthMm,
            aluminumSawKerfMm = current.aluminumSawKerfMm,
            sheetUnitPriceSar = current.sheetUnitPriceSar,
            aluminumBarUnitPriceSar = current.aluminumBarUnitPriceSar
        )
    }

    fun applyAluminumBarPreset(preset: StandardAluminumBarPreset) {
        val current = _studioState.value.camMaterialStandards
        updateCamMaterialStandards(
            sheetWidthMm = current.sheetWidthMm,
            sheetHeightMm = current.sheetHeightMm,
            boardThicknessMm = current.boardThicknessMm,
            standardPanelDepthMm = current.standardPanelDepthMm,
            sawKerfMm = current.sawKerfMm,
            sheetEdgeTrimMm = current.sheetEdgeTrimMm,
            aluminumBarLengthMm = preset.lengthMm,
            aluminumProfileWidthMm = current.aluminumProfileWidthMm,
            aluminumProfileDepthMm = current.aluminumProfileDepthMm,
            aluminumSawKerfMm = current.aluminumSawKerfMm,
            sheetUnitPriceSar = current.sheetUnitPriceSar,
            aluminumBarUnitPriceSar = current.aluminumBarUnitPriceSar
        )
    }

    fun resetCamMaterialStandardsToDefault() {
        updateCamMaterialStandards(
            sheetWidthMm = 2440,
            sheetHeightMm = 1220,
            boardThicknessMm = 18,
            standardPanelDepthMm = 600,
            sawKerfMm = 4,
            sheetEdgeTrimMm = 10,
            aluminumBarLengthMm = 6000,
            aluminumProfileWidthMm = 45,
            aluminumProfileDepthMm = 22,
            aluminumSawKerfMm = 4,
            sheetUnitPriceSar = 245,
            aluminumBarUnitPriceSar = 140
        )
    }

    fun setLightingMode(mode: LightingMode) {
        _studioState.update { it.copy(lightingMode = mode, hasUnsavedChanges = true) }
    }

    fun toggleMagneticSnap() {
        _studioState.update {
            val next = !it.magneticSnapEnabled
            it.copy(
                magneticSnapEnabled = next,
                snapTelemetryAr = if (next) "الالتصاق المغناطيسي ومنع التداخل مفعّل (1 ملم)" else "التحريك الحر بدون التصاق مغناطيسي"
            )
        }
    }

    fun pairUpperWallCabinetsWithBase() {
        val state = _studioState.value
        pushUndoSnapshot(state.placedItems)
        val paired = CadPhysicsEngine.pairAndAlignUpperWallCabinets(
            items = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            safetyClearanceMm = state.upperSafetyClearanceMm
        )
        _studioState.update {
            it.copy(
                placedItems = paired,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = "تم ربط الخزائن العلوية بالسفلية مع خلوص أمان ${state.upperSafetyClearanceMm} ملم (60 سم)",
                statusBannerMessage = "تمت المحاذاة الرأسية للخزائن العلوية مع خلوص أمان 600 ملم فوق الرخام"
            )
        }
        viewModelScope.launch {
            recordLocalRoomSnapshot("ربط رأسي للخزائن العلوية (600 ملم)", isAutoSave = true)
        }
    }

    fun applyAutoFillerStrips(
        mode: FillerPlacementMode,
        customWallLengthMm: Int = _studioState.value.roomWidthMm,
        includeUpperWall: Boolean = true
    ) {
        val state = _studioState.value
        pushUndoSnapshot(state.placedItems)
        val safeWallMm = customWallLengthMm.coerceIn(1800, 10000)
        val (newItems, message) = AutoFillerEngine.generateAutoFillersForWall(
            items = state.placedItems,
            targetWallLengthMm = safeWallMm,
            mode = mode,
            includeUpperWallFillers = includeUpperWall
        )
        _studioState.update {
            it.copy(
                roomWidthCm = (safeWallMm / 10).coerceAtLeast(180),
                fineWallLengthMmOverride = safeWallMm,
                placedItems = newItems,
                selectedItemUid = newItems.lastOrNull()?.uid,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = message,
                statusBannerMessage = message
            )
        }
        viewModelScope.launch {
            recordLocalRoomSnapshot("توليد شرائح فيلر تلقائية (1 ملم)", isAutoSave = true)
        }
    }

    fun applyCameraPreset(preset: CameraPreset3D) {
        _studioState.update {
            it.copy(
                viewMode = if (preset == CameraPreset3D.FIRST_PERSON_EYE) {
                    StudioViewMode.WALKTHROUGH_FP
                } else {
                    StudioViewMode.RENDER_3D
                },
                activeCameraPreset = preset,
                cameraYawDeg = preset.yawDeg,
                cameraPitchFactor = preset.pitchFactor,
                cameraZoom = preset.zoom
            )
        }
    }

    fun toggleWorkTriangle() {
        _studioState.update { it.copy(showWorkTriangle = !it.showWorkTriangle) }
    }

    fun toggleDoorsOpenPreview() {
        _studioState.update { it.copy(doorsOpenPreview = !it.doorsOpenPreview) }
    }

    fun toggleDimensions() {
        _studioState.update { it.copy(showDimensions = !it.showDimensions) }
    }

    fun adjustCameraYaw(deltaDeg: Float) {
        _studioState.update { state ->
            val nextYaw = (state.cameraYawDeg + deltaDeg) % 360f
            state.copy(cameraYawDeg = if (nextYaw < 0f) nextYaw + 360f else nextYaw)
        }
    }

    fun adjustCameraOrbit(deltaYawDeg: Float, deltaPitchFactor: Float) {
        _studioState.update { state ->
            val nextYaw = (state.cameraYawDeg + deltaYawDeg) % 360f
            val normalizedYaw = if (nextYaw < 0f) nextYaw + 360f else nextYaw
            val nextPitch = (state.cameraPitchFactor + deltaPitchFactor).coerceIn(0.20f, 0.88f)
            state.copy(
                cameraYawDeg = normalizedYaw,
                cameraPitchFactor = nextPitch
            )
        }
    }

    fun adjustCameraZoom(deltaZoom: Float) {
        _studioState.update { state ->
            state.copy(cameraZoom = (state.cameraZoom + deltaZoom).coerceIn(0.65f, 1.85f))
        }
    }

    fun updateProjectMetadata(name: String, notes: String) {
        _studioState.update { it.copy(projectName = name, notes = notes, hasUnsavedChanges = true) }
    }

    fun updateRoomDimensions(widthCm: Int, depthCm: Int, heightCm: Int, exactWidthMm: Int? = null) {
        val safeW = widthCm.coerceIn(180, 1000)
        val safeD = depthCm.coerceIn(180, 1000)
        val safeH = heightCm.coerceIn(220, 420)
        val resolvedWidthMm = exactWidthMm?.coerceIn(1800, 10000) ?: (safeW * 10)
        _studioState.update { state ->
            val clampedItems = state.placedItems.map { item ->
                val maxX = (resolvedWidthMm - item.effectiveWidthMm).coerceAtLeast(0)
                val maxY = (safeD * 10 - item.effectiveDepthMm).coerceAtLeast(0)
                item.withPositionMm(
                    item.xMm.coerceIn(0, maxX),
                    item.yMm.coerceIn(0, maxY)
                )
            }
            state.copy(
                roomWidthCm = resolvedWidthMm / 10,
                roomDepthCm = safeD,
                roomHeightCm = safeH,
                fineWallLengthMmOverride = resolvedWidthMm,
                placedItems = clampedItems,
                hasUnsavedChanges = true
            )
        }
    }

    fun applyKitchenShape(shape: KitchenShape) {
        val current = _studioState.value
        pushUndoSnapshot(current.placedItems)
        val generated = KitchenCatalogData.buildDefaultLayoutForShape(
            shape,
            current.roomWidthCm,
            current.roomDepthCm
        )
        _studioState.update {
            it.copy(
                shape = shape,
                placedItems = generated,
                selectedItemUid = generated.firstOrNull()?.uid,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                statusBannerMessage = "تم تطبيق تخطيط ${shape.titleAr} تلقائياً"
            )
        }
    }

    fun applyStylePreset(preset: KitchenStylePreset) {
        pushUndoSnapshot(_studioState.value.placedItems)
        _studioState.update {
            it.copy(
                activeProjectId = "",
                projectName = preset.titleAr,
                shape = preset.defaultShape,
                styleId = preset.id,
                roomWidthCm = preset.defaultWidthCm,
                roomDepthCm = preset.defaultDepthCm,
                roomHeightCm = preset.defaultHeightCm,
                fineWallLengthMmOverride = preset.defaultWidthCm * 10,
                cabinetFinish = preset.cabinetFinish,
                countertopMaterial = preset.countertopMaterial,
                handleStyle = preset.handleStyle,
                wallColorHex = preset.wallColorHex,
                floorMaterial = preset.floorMaterial,
                placedItems = preset.presetItems,
                selectedItemUid = preset.presetItems.firstOrNull()?.uid,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                statusBannerMessage = "تم تحميل قالب: ${preset.titleAr} — يمكنك تعديله وحفظه في السحابة"
            )
        }
    }

    fun loadSavedProject(project: KitchenProject) {
        val decoded = KitchenItemsSerializer.decodeWithViewport(project.itemsData)
        val decodedItems = decoded.items.ifEmpty {
            KitchenCatalogData.buildDefaultLayoutForShape(
                KitchenShape.fromCode(project.shape),
                project.roomWidthCm.toInt(),
                project.roomDepthCm.toInt()
            )
        }
        val viewport = decoded.viewport
        _studioState.update { current ->
            current.copy(
                activeProjectId = project.id,
                projectName = project.name,
                shape = KitchenShape.fromCode(project.shape),
                styleId = project.styleId,
                roomWidthCm = project.roomWidthCm.toInt(),
                roomDepthCm = project.roomDepthCm.toInt(),
                roomHeightCm = project.roomHeightCm.toInt(),
                fineWallLengthMmOverride = project.roomWidthCm.toInt() * 10,
                cabinetFinish = project.cabinetFinish,
                countertopMaterial = project.countertopMaterial,
                handleStyle = project.handleStyle,
                wallColorHex = project.wallColorHex,
                floorMaterial = project.floorMaterial,
                notes = project.notes,
                placedItems = decodedItems,
                selectedItemUid = decodedItems.firstOrNull()?.uid,
                viewMode = StudioViewMode.RENDER_3D,
                activeCameraPreset = viewport?.cameraPreset ?: current.activeCameraPreset,
                cameraYawDeg = viewport?.cameraYawDeg ?: current.cameraYawDeg,
                cameraPitchFactor = viewport?.cameraPitchFactor ?: current.cameraPitchFactor,
                cameraZoom = viewport?.cameraZoom ?: current.cameraZoom,
                lightingMode = viewport?.let { LightingMode.fromNameOrDefault(it.lightingModeName) }
                    ?: current.lightingMode,
                doorsOpenPreview = viewport?.doorsOpenPreview ?: current.doorsOpenPreview,
                hasUnsavedChanges = false,
                statusBannerMessage = "تم استرجاع التصميم ثلاثي الأبعاد: ${project.name} — جاهز للتعديل"
            )
        }
    }

    fun loadSavedProjectById(projectId: String, onLoaded: () -> Unit = {}) {
        viewModelScope.launch {
            repository.getProjectById(projectId)
                .onSuccess { project ->
                    if (project != null) {
                        loadSavedProject(project)
                        onLoaded()
                    } else {
                        _studioState.update {
                            it.copy(errorBannerMessage = "لم يتم العثور على المشروع المطلوب في السحابة")
                        }
                    }
                }
                .onFailure { err ->
                    _studioState.update {
                        it.copy(errorBannerMessage = err.localizedMessage ?: "تعذر استرجاع المشروع من السحابة")
                    }
                }
        }
    }

    fun startNewBlankProject() {
        pushUndoSnapshot(_studioState.value.placedItems)
        val defaultItems = KitchenCatalogData.buildDefaultLayoutForShape(KitchenShape.L_SHAPE, 400, 340)
        _studioState.update {
            PlannerStudioState(
                activeProjectId = "",
                projectName = "مشروع مطبخ مخصص جديد",
                shape = KitchenShape.L_SHAPE,
                roomWidthCm = 400,
                roomDepthCm = 340,
                roomHeightCm = 280,
                placedItems = defaultItems,
                selectedItemUid = defaultItems.firstOrNull()?.uid,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = false,
                statusBannerMessage = "تم إنشاء مخطط مطبخ ثلاثي الأبعاد جديد"
            )
        }
    }

    fun updateFinishes(
        cabinetFinish: String = _studioState.value.cabinetFinish,
        countertopMaterial: String = _studioState.value.countertopMaterial,
        handleStyle: String = _studioState.value.handleStyle,
        floorMaterial: String = _studioState.value.floorMaterial,
        wallColorHex: String = _studioState.value.wallColorHex
    ) {
        _studioState.update {
            it.copy(
                cabinetFinish = cabinetFinish,
                countertopMaterial = countertopMaterial,
                handleStyle = handleStyle,
                floorMaterial = floorMaterial,
                wallColorHex = wallColorHex,
                hasUnsavedChanges = true
            )
        }
    }

    fun selectItem(uid: String?) {
        _studioState.update { it.copy(selectedItemUid = uid) }
    }

    fun addModuleToLayout(type: ModuleType) {
        val state = _studioState.value
        pushUndoSnapshot(state.placedItems)
        val newUid = "mod_${UUID.randomUUID().toString().take(8)}"
        val rawItem = PlacedKitchenItem(
            uid = newUid,
            type = type,
            xCm = 60,
            yCm = if (type == ModuleType.ISLAND_WATERFALL) (state.roomDepthCm / 2).coerceAtLeast(120) else 0
        )
        val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = rawItem,
            desiredXMm = rawItem.xMm,
            desiredYMm = rawItem.yMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = state.magneticSnapEnabled
        )
        _studioState.update {
            it.copy(
                placedItems = it.placedItems + solved.updatedItem,
                selectedItemUid = newUid,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = solved.telemetryMessageAr,
                statusBannerMessage = "تمت إضافة: ${type.titleAr} (${solved.updatedItem.widthMm}×${solved.updatedItem.depthMm} ملم)"
            )
        }
    }

    fun addCatalogUnitToLayout(unit: KitchenCatalogUnit) {
        val state = _studioState.value
        val defaultYMm = if (unit.resolvedModuleType == ModuleType.ISLAND_WATERFALL) {
            (state.roomDepthMm / 2).coerceAtLeast(1200)
        } else {
            0
        }
        dropCatalogUnitAtPositionMm(unit, desiredXMm = 500, desiredYMm = defaultYMm)
    }

    /**
     * Computes live 1mm snapped & anti-overlap preview item while dragging a catalog unit over the 2D/3D/AR canvas.
     */
    fun computeSnappedDropPreview(
        unit: KitchenCatalogUnit,
        desiredXMm: Int,
        desiredYMm: Int
    ): PlacedKitchenItem {
        val state = _studioState.value
        val type = unit.resolvedModuleType
        val wCm = unit.widthCm.toInt().coerceIn(10, 300)
        val dCm = unit.depthCm.toInt().coerceIn(25, 150)
        val rawItem = PlacedKitchenItem(
            uid = "drag_preview_ghost",
            type = type,
            xCm = (desiredXMm / 10).coerceAtLeast(0),
            yCm = (desiredYMm / 10).coerceAtLeast(0),
            widthCm = wCm,
            depthCm = dCm,
            rotationDeg = 0,
            customLabel = unit.nameAr.ifBlank { type.titleAr },
            priceSar = unit.priceSar.toInt()
        ).withPositionMm(desiredXMm.coerceAtLeast(0), desiredYMm.coerceAtLeast(0))

        return CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = rawItem,
            desiredXMm = desiredXMm,
            desiredYMm = desiredYMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = state.magneticSnapEnabled
        ).updatedItem
    }

    /**
     * Drops a dragged KitchenCatalogUnit at the exact target (desiredXMm, desiredYMm) in the 2D/3D/AR design.
     */
    fun dropCatalogUnitAtPositionMm(
        unit: KitchenCatalogUnit,
        desiredXMm: Int,
        desiredYMm: Int
    ) {
        val state = _studioState.value
        pushUndoSnapshot(state.placedItems)
        val type = unit.resolvedModuleType
        val newUid = "mod_${UUID.randomUUID().toString().take(8)}"
        val wCm = unit.widthCm.toInt().coerceIn(10, 300)
        val dCm = unit.depthCm.toInt().coerceIn(25, 150)
        val rawItem = PlacedKitchenItem(
            uid = newUid,
            type = type,
            xCm = (desiredXMm / 10).coerceAtLeast(0),
            yCm = (desiredYMm / 10).coerceAtLeast(0),
            widthCm = wCm,
            depthCm = dCm,
            rotationDeg = 0,
            customLabel = unit.nameAr.ifBlank { type.titleAr },
            priceSar = unit.priceSar.toInt()
        ).withPositionMm(desiredXMm.coerceAtLeast(0), desiredYMm.coerceAtLeast(0))

        val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = rawItem,
            desiredXMm = desiredXMm,
            desiredYMm = desiredYMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = state.magneticSnapEnabled
        )
        _studioState.update {
            it.copy(
                placedItems = it.placedItems + solved.updatedItem,
                selectedItemUid = newUid,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = solved.telemetryMessageAr,
                statusBannerMessage = "تم إفلات '${unit.nameAr}' في التصميم بدقة 1 ملم (${solved.updatedItem.xMm}، ${solved.updatedItem.yMm} ملم)"
            )
        }
    }

    fun addCustomCatalogUnitToFirestore(
        nameAr: String,
        baseType: ModuleType,
        widthCm: Int,
        depthCm: Int,
        priceSar: Int,
        descriptionAr: String
    ) {
        val newUnit = KitchenCatalogUnit(
            userId = currentUserId,
            code = baseType.code,
            nameAr = nameAr.ifBlank { baseType.titleAr },
            category = baseType.category.code,
            subType = baseType.subType,
            widthCm = widthCm.toLong().coerceIn(30L, 300L),
            depthCm = depthCm.toLong().coerceIn(25L, 150L),
            heightCm = baseType.heightCm.toLong(),
            elevationCm = baseType.elevationCm.toLong(),
            priceSar = priceSar.toLong().coerceIn(0L, 500000L),
            colorHex = baseType.colorAccentHex,
            materialFinish = _studioState.value.cabinetFinish,
            countertopIncluded = baseType.countertopIncluded,
            descriptionAr = descriptionAr.ifBlank { baseType.descriptionAr }
        )
        viewModelScope.launch {
            repository.createCatalogUnit(newUnit)
                .onSuccess {
                    _studioState.update {
                        it.copy(statusBannerMessage = "تم حفظ الوحدة '${newUnit.nameAr}' في كتالوج Firestore")
                    }
                }
                .onFailure { err ->
                    _studioState.update {
                        it.copy(errorBannerMessage = err.localizedMessage ?: "تعذر إضافة الوحدة للكتالوج")
                    }
                }
        }
    }

    fun deleteCatalogUnitFromFirestore(unitId: String) {
        viewModelScope.launch {
            repository.deleteCatalogUnit(unitId)
                .onSuccess {
                    _studioState.update {
                        it.copy(statusBannerMessage = "تم حذف الوحدة من الكتالوج السحابي")
                    }
                }
                .onFailure { err ->
                    _studioState.update {
                        it.copy(errorBannerMessage = err.localizedMessage ?: "تعذر حذف الوحدة")
                    }
                }
        }
    }

    /**
     * 1mm Precision Movement with optional Magnetic Snapping & Anti-Overlap Physics.
     */
    fun moveSelectedItemByMm(deltaXMm: Int, deltaYMm: Int) {
        val state = _studioState.value
        val selectedId = state.selectedItemUid ?: return
        val target = state.placedItems.firstOrNull { it.uid == selectedId } ?: return
        pushUndoSnapshot(state.placedItems)

        val desiredXMm = (target.xMm + deltaXMm).coerceAtLeast(0)
        val desiredYMm = (target.yMm + deltaYMm).coerceAtLeast(0)

        // For fine 1mm / 10mm nudge buttons, disable magnetic snap jump so exact 1mm step is preserved,
        // while keeping strict anti-overlap & boundary clamping!
        val isFineMillimeterStep = kotlin.math.abs(deltaXMm) in 1..25 || kotlin.math.abs(deltaYMm) in 1..25
        val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = target,
            desiredXMm = desiredXMm,
            desiredYMm = desiredYMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = state.magneticSnapEnabled && !isFineMillimeterStep
        )

        _studioState.update { current ->
            val updatedList = current.placedItems.map { if (it.uid == selectedId) solved.updatedItem else it }
            current.copy(
                placedItems = updatedList,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = solved.telemetryMessageAr
            )
        }
    }

    fun moveSelectedItemBy(deltaXCm: Int, deltaYCm: Int) {
        moveSelectedItemByMm(deltaXCm * 10, deltaYCm * 10)
    }

    fun updateItemPositionDirectMm(uid: String, newXMm: Int, newYMm: Int) {
        val state = _studioState.value
        val target = state.placedItems.firstOrNull { it.uid == uid } ?: return
        val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = target,
            desiredXMm = newXMm,
            desiredYMm = newYMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = state.magneticSnapEnabled
        )
        _studioState.update { current ->
            val updated = current.placedItems.map { if (it.uid == uid) solved.updatedItem else it }
            current.copy(
                placedItems = updated,
                selectedItemUid = uid,
                hasUnsavedChanges = true,
                snapTelemetryAr = solved.telemetryMessageAr
            )
        }
    }

    fun updateItemPositionDirect(uid: String, newXCm: Int, newYCm: Int) {
        updateItemPositionDirectMm(uid, newXCm * 10, newYCm * 10)
    }

    fun rotateSelectedItem() {
        val state = _studioState.value
        val selectedId = state.selectedItemUid ?: return
        val target = state.placedItems.firstOrNull { it.uid == selectedId } ?: return
        pushUndoSnapshot(state.placedItems)

        val nextRot = (target.rotationDeg + 90) % 360
        val rotated = target.copy(rotationDeg = nextRot)
        val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = rotated,
            desiredXMm = rotated.xMm,
            desiredYMm = rotated.yMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = state.magneticSnapEnabled
        )
        _studioState.update { current ->
            val updated = current.placedItems.map { if (it.uid == selectedId) solved.updatedItem else it }
            current.copy(
                placedItems = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = solved.telemetryMessageAr
            )
        }
    }

    /**
     * 1mm Precision Width / Depth / Elevation Resizing.
     */
    fun resizeSelectedItemWidthMm(deltaWidthMm: Int) {
        val state = _studioState.value
        val selectedId = state.selectedItemUid ?: return
        pushUndoSnapshot(state.placedItems)
        _studioState.update { current ->
            val updated = current.placedItems.map { item ->
                if (item.uid == selectedId) {
                    val newWidthMm = (item.widthMm + deltaWidthMm).coerceIn(20, 3000)
                    val defaultWMm = (item.type.defaultWidthCm * 10).coerceAtLeast(100)
                    val priceRatio = newWidthMm.toFloat() / defaultWMm.toFloat()
                    val newPrice = (item.type.defaultPriceSar * priceRatio).roundToInt().coerceAtLeast(150)
                    item.withDimensionsMm(newWidthMm, item.depthMm).copy(priceSar = newPrice)
                } else {
                    item
                }
            }
            current.copy(
                placedItems = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true
            )
        }
    }

    fun resizeSelectedItemWidth(deltaWidthCm: Int) {
        resizeSelectedItemWidthMm(deltaWidthCm * 10)
    }

    fun updateSelectedItemExactMillimeters(
        xMm: Int,
        yMm: Int,
        widthMm: Int,
        depthMm: Int,
        heightMm: Int,
        elevationMm: Int
    ) {
        val state = _studioState.value
        val selectedId = state.selectedItemUid ?: return
        val target = state.placedItems.firstOrNull { it.uid == selectedId } ?: return
        pushUndoSnapshot(state.placedItems)

        val minElev = if (target.type.category == ModuleCategory.WALL_CABINET) {
            CadPhysicsEngine.MIN_WALL_CABINET_ELEVATION_MM
        } else {
            0
        }
        val resized = target
            .withDimensionsMm(widthMm.coerceIn(20, 3200), depthMm.coerceIn(100, 1500))
            .copy(
                customHeightMm = heightMm.coerceIn(200, 2600),
                customElevationMm = elevationMm.coerceIn(minElev, 2200)
            )
        val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = resized,
            desiredXMm = xMm,
            desiredYMm = yMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = false
        )
        _studioState.update { current ->
            val updated = current.placedItems.map { if (it.uid == selectedId) solved.updatedItem else it }
            current.copy(
                placedItems = updated,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = "تم تحديث الأبعاد بدقة 1 ملم: ${solved.updatedItem.widthMm}×${solved.updatedItem.depthMm}×${solved.updatedItem.customHeightMm} ملم",
                statusBannerMessage = "تم تطبيق القياسات المليمترية (1mm CAD) بنجاح"
            )
        }
    }

    fun applyJsonV2ToSelectedOrScene(jsonText: String) {
        importAnyJsonFileOrContent(
            fileName = "live_editor.json",
            rawJsonContent = jsonText,
            preferUpdateSelectedIfSingle = true
        )
    }

    fun importAnyJsonFileOrContent(
        fileName: String,
        rawJsonContent: String,
        preferUpdateSelectedIfSingle: Boolean = false
    ) {
        val state = _studioState.value
        JsonCadEngine.parseUniversalJsonContent(
            rawJsonText = rawJsonContent,
            fileName = fileName
        ).onSuccess { result ->
            pushUndoSnapshot(state.placedItems)
            val targetRoomW = result.roomWidthMm ?: state.roomWidthMm
            val targetRoomD = result.roomDepthMm ?: state.roomDepthMm
            val targetRoomH = result.roomHeightMm ?: state.roomHeightMm

            val nextItems = if (result.replaceEntireScene && result.items.size > 1) {
                result.items
            } else if (result.items.size == 1) {
                val single = result.items.first()
                val existingUid = when {
                    state.placedItems.any { it.uid == single.uid } -> single.uid
                    preferUpdateSelectedIfSingle && state.selectedItemUid != null &&
                        !rawJsonContent.contains("\"uid\"") -> state.selectedItemUid
                    else -> null
                }
                if (existingUid != null) {
                    val updatedTarget = single.copy(uid = existingUid)
                    state.placedItems.map { if (it.uid == existingUid) updatedTarget else it }
                } else {
                    val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
                        targetItem = single,
                        desiredXMm = single.xMm,
                        desiredYMm = single.yMm,
                        allItems = state.placedItems,
                        roomWidthMm = targetRoomW,
                        roomDepthMm = targetRoomD,
                        enableMagneticSnap = state.magneticSnapEnabled
                    )
                    state.placedItems + solved.updatedItem
                }
            } else {
                val working = state.placedItems.toMutableList()
                result.items.forEach { item ->
                    val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
                        targetItem = item,
                        desiredXMm = item.xMm,
                        desiredYMm = item.yMm,
                        allItems = working,
                        roomWidthMm = targetRoomW,
                        roomDepthMm = targetRoomD,
                        enableMagneticSnap = state.magneticSnapEnabled
                    )
                    working.add(solved.updatedItem)
                }
                working
            }

            _studioState.update { current ->
                current.copy(
                    projectName = result.projectName ?: current.projectName,
                    roomWidthCm = (targetRoomW / 10).coerceAtLeast(180),
                    roomDepthCm = (targetRoomD / 10).coerceAtLeast(180),
                    roomHeightCm = (targetRoomH / 10).coerceAtLeast(220),
                    fineWallLengthMmOverride = targetRoomW,
                    cabinetFinish = result.cabinetFinish ?: current.cabinetFinish,
                    countertopMaterial = result.countertopMaterial ?: current.countertopMaterial,
                    placedItems = nextItems,
                    selectedItemUid = nextItems.lastOrNull()?.uid ?: current.selectedItemUid,
                    lastGeneratedJsonV2 = result.normalizedJsonPreview,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    hasUnsavedChanges = true,
                    statusBannerMessage = result.summaryMessageAr,
                    errorBannerMessage = null
                )
            }
        }.onFailure { err ->
            _studioState.update {
                it.copy(errorBannerMessage = "صيغة JSON غير صالحة (${fileName}): ${err.localizedMessage}")
            }
        }
    }

    fun importAutoCadDxfFileOrContent(
        fileName: String,
        rawDxfContent: String
    ) {
        val state = _studioState.value
        JsonCadEngine.parseAutoCadDxfContent(
            rawDxfText = rawDxfContent,
            fileName = fileName
        ).onSuccess { result ->
            pushUndoSnapshot(state.placedItems)
            val targetRoomW = result.roomWidthMm ?: state.roomWidthMm
            val targetRoomD = result.roomDepthMm ?: state.roomDepthMm
            val targetRoomH = result.roomHeightMm ?: state.roomHeightMm

            val nextItems = if (result.replaceEntireScene) {
                result.items
            } else {
                val working = state.placedItems.toMutableList()
                result.items.forEach { item ->
                    val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
                        targetItem = item,
                        desiredXMm = item.xMm,
                        desiredYMm = item.yMm,
                        allItems = working,
                        roomWidthMm = targetRoomW,
                        roomDepthMm = targetRoomD,
                        enableMagneticSnap = state.magneticSnapEnabled
                    )
                    working.add(solved.updatedItem)
                }
                working
            }

            _studioState.update { current ->
                current.copy(
                    projectName = result.projectName ?: current.projectName,
                    roomWidthCm = (targetRoomW / 10).coerceAtLeast(180),
                    roomDepthCm = (targetRoomD / 10).coerceAtLeast(180),
                    roomHeightCm = (targetRoomH / 10).coerceAtLeast(220),
                    fineWallLengthMmOverride = targetRoomW,
                    cabinetFinish = result.cabinetFinish ?: current.cabinetFinish,
                    countertopMaterial = result.countertopMaterial ?: current.countertopMaterial,
                    placedItems = nextItems,
                    selectedItemUid = nextItems.lastOrNull()?.uid ?: current.selectedItemUid,
                    lastGeneratedJsonV2 = result.normalizedJsonPreview,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    hasUnsavedChanges = true,
                    statusBannerMessage = result.summaryMessageAr,
                    errorBannerMessage = null
                )
            }
            viewModelScope.launch {
                recordLocalRoomSnapshot(reason = "استيراد مخطط أوتوكاد DXF ($fileName)", isAutoSave = true)
            }
        }.onFailure { err ->
            _studioState.update {
                it.copy(errorBannerMessage = "تعذر استيراد ملف الأوتوكاد DXF ($fileName): ${err.localizedMessage}")
            }
        }
    }

    fun importExternal3DOrCadModel(fileName: String, rawContent: String) {
        val lower = fileName.lowercase()
        val trimmed = rawContent.trimStart()
        if (lower.endsWith(".json") || trimmed.startsWith("{") || trimmed.startsWith("[")) {
            importAnyJsonFileOrContent(fileName = fileName, rawJsonContent = rawContent)
            return
        }
        if (lower.endsWith(".dxf") || trimmed.contains("SECTION") && trimmed.contains("ENTITIES")) {
            importAutoCadDxfFileOrContent(fileName = fileName, rawDxfContent = rawContent)
            return
        }
        val state = _studioState.value
        JsonCadEngine.parseExternal3DOrCadContent(fileName, rawContent)
            .onSuccess { (importedItem, msg) ->
                pushUndoSnapshot(state.placedItems)
                val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
                    targetItem = importedItem,
                    desiredXMm = importedItem.xMm,
                    desiredYMm = importedItem.yMm,
                    allItems = state.placedItems,
                    roomWidthMm = state.roomWidthMm,
                    roomDepthMm = state.roomDepthMm,
                    enableMagneticSnap = state.magneticSnapEnabled
                )
                val generatedJson = JsonCadEngine.itemToJsonV2(
                    item = solved.updatedItem,
                    finishName = state.cabinetFinish,
                    countertopName = state.countertopMaterial,
                    captureSource = "3D_CAD_MODEL_IMPORT"
                )
                _studioState.update {
                    it.copy(
                        placedItems = it.placedItems + solved.updatedItem,
                        selectedItemUid = solved.updatedItem.uid,
                        lastGeneratedJsonV2 = generatedJson,
                        canUndo = undoStack.isNotEmpty(),
                        canRedo = redoStack.isNotEmpty(),
                        hasUnsavedChanges = true,
                        statusBannerMessage = msg,
                        errorBannerMessage = null
                    )
                }
            }
            .onFailure { err ->
                _studioState.update {
                    it.copy(errorBannerMessage = "تعذر استيراد النموذج ثلاثي الأبعاد: ${err.localizedMessage}")
                }
            }
    }

    fun createUnitFromCameraScanToJsonV2(
        labelAr: String,
        baseType: ModuleType,
        widthMm: Int,
        depthMm: Int,
        heightMm: Int,
        sampledColorHex: String? = null
    ): String {
        val state = _studioState.value
        pushUndoSnapshot(state.placedItems)
        val newUid = "cam_scan_${UUID.randomUUID().toString().take(6)}"
        val safeW = widthMm.coerceIn(150, 3200)
        val safeD = depthMm.coerceIn(150, 1500)
        val safeH = heightMm.coerceIn(250, 2600)
        val rawItem = PlacedKitchenItem(
            uid = newUid,
            type = baseType,
            xCm = (safeW / 20).coerceIn(20, 200),
            yCm = if (baseType == ModuleType.ISLAND_WATERFALL) (state.roomDepthCm / 2).coerceAtLeast(120) else 0,
            widthCm = (safeW / 10).coerceAtLeast(15),
            depthCm = (safeD / 10).coerceAtLeast(15),
            customLabel = labelAr.ifBlank { "وحدة ممسوحة بالكاميرا (${safeW} ملم)" },
            priceSar = (baseType.defaultPriceSar * (safeW / (baseType.defaultWidthCm * 10f))).roundToInt().coerceAtLeast(250),
            fineWidthMmOffset = safeW % 10,
            fineDepthMmOffset = safeD % 10,
            customHeightMm = safeH,
            customElevationMm = if (baseType.category == ModuleCategory.WALL_CABINET) 1450 else 0,
            boardThicknessMm = 18,
            handleSystem = "GOLA_ALUMINUM"
        )
        val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = rawItem,
            desiredXMm = rawItem.xMm,
            desiredYMm = rawItem.yMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = state.magneticSnapEnabled
        )
        val jsonCode = JsonCadEngine.itemToJsonV2(
            item = solved.updatedItem,
            finishName = state.cabinetFinish,
            countertopName = state.countertopMaterial,
            sampledHexOverride = sampledColorHex,
            captureSource = "LIVE_CAMERA_SCANNER_V2"
        )
        _studioState.update {
            it.copy(
                placedItems = it.placedItems + solved.updatedItem,
                selectedItemUid = newUid,
                lastGeneratedJsonV2 = jsonCode,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                statusBannerMessage = "Camera ➔ JSON v2.0: تم استيراد الكائن عبر الكاميرا كملف JSON (${safeW}×${safeD}×${safeH} ملم)",
                errorBannerMessage = null
            )
        }
        return jsonCode
    }

    fun deleteSelectedItem() {
        val state = _studioState.value
        val selectedId = state.selectedItemUid ?: return
        pushUndoSnapshot(state.placedItems)
        _studioState.update { current ->
            val remaining = current.placedItems.filterNot { it.uid == selectedId }
            current.copy(
                placedItems = remaining,
                selectedItemUid = remaining.firstOrNull()?.uid,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                statusBannerMessage = "تم حذف الوحدة المحددة"
            )
        }
    }

    fun clearBannerMessages() {
        _studioState.update { it.copy(statusBannerMessage = null, errorBannerMessage = null) }
    }

    fun saveCurrentProjectToCloud(
        customName: String? = null,
        customNotes: String? = null,
        saveAsNew: Boolean = false,
        onSaved: (String) -> Unit = {}
    ) {
        val state = _studioState.value
        if (state.isSaving) return
        val resolvedName = (customName ?: state.projectName).trim().ifBlank { "مطبخ حديث 3D" }
        val resolvedNotes = (customNotes ?: state.notes).trim()

        _studioState.update {
            it.copy(
                projectName = resolvedName,
                notes = resolvedNotes,
                isSaving = true,
                errorBannerMessage = null,
                statusBannerMessage = null
            )
        }

        val viewportSnapshot = Saved3DViewportSnapshot(
            cameraYawDeg = state.cameraYawDeg,
            cameraPitchFactor = state.cameraPitchFactor,
            cameraZoom = state.cameraZoom,
            cameraPreset = state.activeCameraPreset,
            lightingModeName = state.lightingMode.name,
            doorsOpenPreview = state.doorsOpenPreview
        )
        val serializedItems = KitchenItemsSerializer.encodeWithViewport(
            items = state.placedItems,
            viewport = viewportSnapshot
        )
        val totalSar = state.costBreakdown.totalSar.toLong()
        val ergonomic = state.workTriangle.score.toLong()
        val targetId = if (saveAsNew) "" else state.activeProjectId

        val model = KitchenProject(
            id = targetId,
            userId = currentUserId,
            name = resolvedName,
            shape = state.shape.code,
            styleId = state.styleId,
            roomWidthCm = state.roomWidthCm.toLong(),
            roomDepthCm = state.roomDepthCm.toLong(),
            roomHeightCm = state.roomHeightCm.toLong(),
            cabinetFinish = state.cabinetFinish,
            countertopMaterial = state.countertopMaterial,
            handleStyle = state.handleStyle,
            wallColorHex = state.wallColorHex,
            floorMaterial = state.floorMaterial,
            itemsData = serializedItems,
            totalPriceSar = totalSar,
            ergonomicScore = ergonomic,
            notes = resolvedNotes
        )

        viewModelScope.launch {
            recordLocalRoomSnapshot(reason = "حفظ متزامن مع Firestore", isAutoSave = true)
            if (targetId.isBlank()) {
                val res = repository.createProject(model)
                res.onSuccess { newId ->
                    _studioState.update {
                        it.copy(
                            activeProjectId = newId,
                            hasUnsavedChanges = false,
                            isSaving = false,
                            statusBannerMessage = "تم حفظ تصميم المطبخ ثلاثي الأبعاد '${resolvedName}' في Firestore بنجاح"
                        )
                    }
                    onSaved(newId)
                }.onFailure { err ->
                    _studioState.update {
                        it.copy(
                            isSaving = false,
                            errorBannerMessage = err.localizedMessage ?: "تعذر حفظ المشروع في Firestore"
                        )
                    }
                }
            } else {
                val res = repository.updateProject(model)
                res.onSuccess {
                    _studioState.update {
                        it.copy(
                            hasUnsavedChanges = false,
                            isSaving = false,
                            statusBannerMessage = "تم حفظ التعديلات على مشروع '${resolvedName}' في Firestore بنجاح"
                        )
                    }
                    onSaved(targetId)
                }.onFailure { err ->
                    _studioState.update {
                        it.copy(
                            isSaving = false,
                            errorBannerMessage = err.localizedMessage ?: "تعذر تحديث المشروع في Firestore"
                        )
                    }
                }
            }
        }
    }

    fun duplicateSavedProject(project: KitchenProject, onDuplicated: () -> Unit = {}) {
        viewModelScope.launch {
            repository.duplicateProject(project)
                .onSuccess { newId ->
                    val duplicated = project.copy(
                        id = newId,
                        name = "${project.name.take(95)} (نسخة)"
                    )
                    loadSavedProject(duplicated)
                    _studioState.update {
                        it.copy(
                            statusBannerMessage = "تم إنشاء نسخة جديدة من المشروع في السحابة وفتحها للتعديل"
                        )
                    }
                    onDuplicated()
                }
                .onFailure { err ->
                    _studioState.update {
                        it.copy(errorBannerMessage = err.localizedMessage ?: "تعذر نسخ المشروع")
                    }
                }
        }
    }

    fun deleteCloudProject(projectId: String) {
        viewModelScope.launch {
            val res = repository.deleteProject(projectId)
            res.onSuccess {
                _studioState.update { current ->
                    val nextActiveId = if (current.activeProjectId == projectId) "" else current.activeProjectId
                    current.copy(
                        activeProjectId = nextActiveId,
                        statusBannerMessage = "تم حذف المشروع من السحابة"
                    )
                }
            }.onFailure { err ->
                _studioState.update {
                    it.copy(errorBannerMessage = err.localizedMessage ?: "تعذر حذف المشروع")
                }
            }
        }
    }

    fun submitQuoteForCurrentProject(
        showroom: CertifiedShowroom,
        customerName: String,
        customerPhone: String,
        city: String,
        preferredInstallWindow: String,
        specialNotes: String,
        onComplete: () -> Unit
    ) {
        val state = _studioState.value
        _studioState.update { it.copy(isSaving = true, errorBannerMessage = null, statusBannerMessage = null) }

        viewModelScope.launch {
            val targetProjectId = if (state.activeProjectId.isNotBlank()) {
                state.activeProjectId
            } else {
                val viewportSnapshot = Saved3DViewportSnapshot(
                    cameraYawDeg = state.cameraYawDeg,
                    cameraPitchFactor = state.cameraPitchFactor,
                    cameraZoom = state.cameraZoom,
                    cameraPreset = state.activeCameraPreset,
                    lightingModeName = state.lightingMode.name,
                    doorsOpenPreview = state.doorsOpenPreview
                )
                val serializedItems = KitchenItemsSerializer.encodeWithViewport(
                    items = state.placedItems,
                    viewport = viewportSnapshot
                )
                val newProject = KitchenProject(
                    userId = currentUserId,
                    name = state.projectName.ifBlank { "مطبخ حديث" },
                    shape = state.shape.code,
                    styleId = state.styleId,
                    roomWidthCm = state.roomWidthCm.toLong(),
                    roomDepthCm = state.roomDepthCm.toLong(),
                    roomHeightCm = state.roomHeightCm.toLong(),
                    cabinetFinish = state.cabinetFinish,
                    countertopMaterial = state.countertopMaterial,
                    handleStyle = state.handleStyle,
                    wallColorHex = state.wallColorHex,
                    floorMaterial = state.floorMaterial,
                    itemsData = serializedItems,
                    totalPriceSar = state.costBreakdown.totalSar.toLong(),
                    ergonomicScore = state.workTriangle.score.toLong(),
                    notes = state.notes
                )
                val createRes = repository.createProject(newProject)
                if (createRes.isFailure) {
                    _studioState.update {
                        it.copy(
                            isSaving = false,
                            errorBannerMessage = createRes.exceptionOrNull()?.localizedMessage
                                ?: "تعذر حفظ المخطط قبل إرسال طلب التسعير"
                        )
                    }
                    return@launch
                }
                val createdId = createRes.getOrThrow()
                _studioState.update { it.copy(activeProjectId = createdId, hasUnsavedChanges = false) }
                createdId
            }

            val quote = QuoteRequest(
                userId = currentUserId,
                projectId = targetProjectId,
                projectName = state.projectName.ifBlank { "مطبخ مخصص" },
                showroomName = showroom.nameAr,
                customerName = customerName.ifBlank { "عميل مخطط المطابخ" },
                customerPhone = customerPhone.ifBlank { "+966500000000" },
                city = city.ifBlank { showroom.cityAr },
                preferredInstallWindow = preferredInstallWindow.ifBlank { "خلال 30 يوم" },
                estimatedBudgetSar = state.costBreakdown.totalSar.toLong(),
                specialNotes = specialNotes
            )

            val quoteRes = repository.submitQuoteRequest(quote)
            quoteRes.onSuccess {
                _studioState.update {
                    it.copy(
                        isSaving = false,
                        statusBannerMessage = "تم إرسال المخطط وطلب عرض السعر إلى ${showroom.nameAr}"
                    )
                }
                onComplete()
            }.onFailure { err ->
                _studioState.update {
                    it.copy(
                        isSaving = false,
                        errorBannerMessage = err.localizedMessage ?: "تعذر إرسال طلب عرض السعر"
                    )
                }
            }
        }
    }

    fun cancelQuoteRequest(quoteId: String) {
        viewModelScope.launch {
            repository.cancelQuoteRequest(quoteId)
                .onSuccess {
                    _studioState.update {
                        it.copy(statusBannerMessage = "تم إلغاء طلب عرض السعر")
                    }
                }
                .onFailure { err ->
                    _studioState.update {
                        it.copy(errorBannerMessage = err.localizedMessage ?: "تعذر إلغاء الطلب")
                    }
                }
        }
    }

    /**
     * Calls Firebase AI Logic (Gemini 3.8 Flash) to suggest a complete kitchen layout
     * tailored to the user's room dimensions and style preferences, then applies it.
     */
    fun requestGeminiKitchenDesignSuggestion(
        roomWidthCm: Int,
        roomDepthCm: Int,
        roomHeightCm: Int,
        preferredShape: KitchenShape,
        stylePreferenceAr: String,
        customPrioritiesAr: String = ""
    ) {
        val safeW = roomWidthCm.coerceIn(220, 900)
        val safeD = roomDepthCm.coerceIn(200, 800)
        val safeH = roomHeightCm.coerceIn(230, 400)

        _studioState.update {
            it.copy(
                isGeneratingGeminiDesign = true,
                geminiSuggestionError = null,
                statusBannerMessage = "جاري تحليل أبعاد الغرفة (${safeW}×${safeD} سم) واقتراح تصميم مطبخ متكامل عبر Gemini AI..."
            )
        }

        viewModelScope.launch {
            val result = GeminiKitchenDesignService.suggestCompleteKitchenLayout(
                roomWidthCm = safeW,
                roomDepthCm = safeD,
                roomHeightCm = safeH,
                preferredShape = preferredShape,
                stylePreferenceAr = stylePreferenceAr,
                customPrioritiesAr = customPrioritiesAr
            )

            result.onSuccess { suggestion ->
                applyGeminiSuggestedDesign(
                    suggestion = suggestion,
                    roomWidthCm = safeW,
                    roomDepthCm = safeD,
                    roomHeightCm = safeH
                )
            }.onFailure { err ->
                Log.w(TAG, "Gemini suggestion error: ${err.message}", err)
                _studioState.update {
                    it.copy(
                        isGeneratingGeminiDesign = false,
                        geminiSuggestionError = err.localizedMessage
                            ?: "تعذر توليد التصميم عبر Gemini AI حالياً، يرجى المحاولة مرة أخرى."
                    )
                }
            }
        }
    }

    /**
     * Applies a Gemini AI suggested kitchen design into the active 2D/3D CAD Studio workspace.
     */
    fun applyGeminiSuggestedDesign(
        suggestion: GeminiSuggestedKitchenDesign,
        roomWidthCm: Int = _studioState.value.roomWidthCm,
        roomDepthCm: Int = _studioState.value.roomDepthCm,
        roomHeightCm: Int = _studioState.value.roomHeightCm
    ) {
        val current = _studioState.value
        pushUndoSnapshot(current.placedItems)
        val safeW = roomWidthCm.coerceIn(220, 900)
        val safeD = roomDepthCm.coerceIn(200, 800)
        val safeH = roomHeightCm.coerceIn(230, 400)

        _studioState.update { state ->
            state.copy(
                projectName = suggestion.designTitleAr.ifBlank { state.projectName },
                shape = suggestion.recommendedShape,
                roomWidthCm = safeW,
                roomDepthCm = safeD,
                roomHeightCm = safeH,
                fineWallLengthMmOverride = null,
                cabinetFinish = suggestion.cabinetFinishAr,
                countertopMaterial = suggestion.countertopMaterialAr,
                wallColorHex = suggestion.wallColorHex,
                notes = suggestion.architecturalRationaleAr,
                placedItems = suggestion.suggestedItems,
                selectedItemUid = suggestion.suggestedItems.firstOrNull()?.uid,
                isGeneratingGeminiDesign = false,
                lastGeminiSuggestion = suggestion,
                geminiSuggestionError = null,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = "Gemini AI: ${suggestion.ergonomicHighlightAr}",
                statusBannerMessage = "تم تطبيق تصميم Gemini AI '${suggestion.designTitleAr}' (${suggestion.suggestedItems.size} وحدة بدقة 1 ملم)"
            )
        }
        viewModelScope.launch {
            recordLocalRoomSnapshot("اقتراح تصميم كامل عبر Gemini AI (${suggestion.designTitleAr})", isAutoSave = true)
        }
    }

    /**
     * Augmented Reality Live Material Customizer:
     * Updates the wood/cabinet finish or marble/countertop material of a targeted cabinet in AR
     * and immediately applies/syncs the change to its JSON v2.0 representation.
     */
    fun applyArMaterialToUnitAndSyncJson(
        uid: String,
        newCabinetFinish: String? = null,
        newCountertopMaterial: String? = null
    ): String {
        val state = _studioState.value
        val targetItem = state.placedItems.firstOrNull { it.uid == uid }
            ?: state.placedItems.firstOrNull()
            ?: return ""

        pushUndoSnapshot(state.placedItems)

        val updatedItem = targetItem.copy(
            customCabinetFinish = newCabinetFinish ?: targetItem.customCabinetFinish ?: state.cabinetFinish,
            customCountertopMaterial = newCountertopMaterial ?: targetItem.customCountertopMaterial ?: state.countertopMaterial
        )

        val updatedJsonV2 = JsonCadEngine.itemToJsonV2(
            item = updatedItem,
            finishName = state.cabinetFinish,
            countertopName = state.countertopMaterial,
            captureSource = "AR_LIVE_MATERIAL_EDITOR"
        )

        val updatedList = state.placedItems.map { item ->
            if (item.uid == targetItem.uid) updatedItem else item
        }

        val resolvedFinish = updatedItem.customCabinetFinish ?: state.cabinetFinish
        val resolvedCounter = updatedItem.customCountertopMaterial ?: state.countertopMaterial

        _studioState.update { current ->
            current.copy(
                placedItems = updatedList,
                selectedItemUid = targetItem.uid,
                lastGeneratedJsonV2 = updatedJsonV2,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = "AR Material ➔ JSON: ${updatedItem.displayTitle} | خشب: $resolvedFinish | رخام: $resolvedCounter",
                statusBannerMessage = "تم تحديث خامة '${updatedItem.displayTitle}' بالواقع المعزز ومزامنة ملف JSON v2.0 فوراً"
            )
        }

        viewModelScope.launch {
            recordLocalRoomSnapshot("تعديل خامة بالواقع المعزز (${updatedItem.displayTitle})", isAutoSave = true)
        }

        return updatedJsonV2
    }

    /**
     * Toggles or sets the measurement system between Metric (mm / cm / m) and Imperial (in / ft / LF).
     * Automatically updates dimension formatting and linear countertop telemetry.
     */
    fun setMeasurementUnitSystem(system: MeasurementUnitSystem) {
        _studioState.update { state ->
            val roomDimsFormatted = system.formatDimensionsBox(state.roomWidthMm, state.roomDepthMm, state.roomHeightMm)
            val counterFormatted = system.formatLinearCountertop(state.costBreakdown.countertopLinearMeters)
            state.copy(
                unitSystem = system,
                snapTelemetryAr = "نظام القياس: ${system.titleAr} | الغرفة: $roomDimsFormatted | السطح: $counterFormatted",
                statusBannerMessage = "تم التبديل إلى ${system.titleAr} وتحديث جميع حسابات الأبعاد والتكلفة (${state.costBreakdown.totalSar} ر.س)"
            )
        }
    }

    fun toggleMeasurementUnitSystem() {
        val next = if (_studioState.value.unitSystem == MeasurementUnitSystem.METRIC) {
            MeasurementUnitSystem.IMPERIAL
        } else {
            MeasurementUnitSystem.METRIC
        }
        setMeasurementUnitSystem(next)
    }

    /**
     * Updates the 3D Natural & Artificial Lighting Simulation Engine parameters.
     */
    fun applyLightingSimulationPreset(preset: NaturalDaylightPreset) {
        _studioState.update { state ->
            state.copy(
                lightingMode = if (preset == NaturalDaylightPreset.NIGHT_SPOT_LED) {
                    LightingMode.WARM_LED
                } else {
                    LightingMode.DAYLIGHT
                },
                lightingSimulation = LightingSimulationConfig(
                    preset = preset,
                    sunAzimuthDeg = preset.sunAzimuthDeg,
                    naturalLightIntensity = preset.naturalIntensity,
                    artificialSpotIntensity = preset.artificialIntensity,
                    underCabinetLedIntensity = if (preset == NaturalDaylightPreset.NIGHT_SPOT_LED) 1.0f else 0.78f,
                    colorTemperatureKelvin = preset.colorTempKelvin,
                    shadowSoftnessAndDepth = preset.shadowIntensity,
                    surfaceSpecularReflection = preset.specularReflection
                ),
                snapTelemetryAr = "محاكاة الإضاءة: ${preset.titleAr} (${preset.colorTempKelvin}K • زاوية الشمس ${preset.sunAzimuthDeg.toInt()}°)"
            )
        }
    }

    fun updateLightingSimulationParams(
        sunAzimuthDeg: Float = _studioState.value.lightingSimulation.sunAzimuthDeg,
        naturalLightIntensity: Float = _studioState.value.lightingSimulation.naturalLightIntensity,
        artificialSpotIntensity: Float = _studioState.value.lightingSimulation.artificialSpotIntensity,
        underCabinetLedIntensity: Float = _studioState.value.lightingSimulation.underCabinetLedIntensity,
        colorTemperatureKelvin: Int = _studioState.value.lightingSimulation.colorTemperatureKelvin,
        shadowSoftnessAndDepth: Float = _studioState.value.lightingSimulation.shadowSoftnessAndDepth,
        surfaceSpecularReflection: Float = _studioState.value.lightingSimulation.surfaceSpecularReflection
    ) {
        _studioState.update { state ->
            state.copy(
                lightingSimulation = state.lightingSimulation.copy(
                    sunAzimuthDeg = sunAzimuthDeg.coerceIn(0f, 360f),
                    naturalLightIntensity = naturalLightIntensity.coerceIn(0f, 1f),
                    artificialSpotIntensity = artificialSpotIntensity.coerceIn(0f, 1f),
                    underCabinetLedIntensity = underCabinetLedIntensity.coerceIn(0f, 1f),
                    colorTemperatureKelvin = colorTemperatureKelvin.coerceIn(2700, 6500),
                    shadowSoftnessAndDepth = shadowSoftnessAndDepth.coerceIn(0f, 1f),
                    surfaceSpecularReflection = surfaceSpecularReflection.coerceIn(0f, 1f)
                )
            )
        }
    }

    /**
     * Smart Internal Kitchen Accessories Library:
     * Toggles automatic linking of a smart internal accessory to all compatible units in the layout and updates BOM.
     */
    fun toggleSmartInternalAccessory(accessoryCode: String) {
        _studioState.update { state ->
            val updatedSet = if (accessoryCode in state.enabledAccessoryCodes) {
                state.enabledAccessoryCodes - accessoryCode
            } else {
                state.enabledAccessoryCodes + accessoryCode
            }
            val nextState = state.copy(
                enabledAccessoryCodes = updatedSet,
                hasUnsavedChanges = true
            )
            val accessoryName = SmartAccessoriesCatalog.allAccessories
                .firstOrNull { it.code == accessoryCode }?.nameAr ?: accessoryCode
            val enabledNow = accessoryCode in updatedSet
            nextState.copy(
                statusBannerMessage = if (enabledNow) {
                    "تم ربط «$accessoryName» تلقائياً بالوحدات المتوافقة وتحديث قائمة المواد (BOM: ${nextState.costBreakdown.totalSar} ر.س)"
                } else {
                    "تم إلغاء ربط «$accessoryName» وتحديث قائمة المواد (BOM: ${nextState.costBreakdown.totalSar} ر.س)"
                }
            )
        }
    }

    /**
     * Automatically resolves engineering compliance violations (door swing clearances, upper cabinet 600mm clearance, boundaries).
     */
    fun autoFixEngineeringComplianceViolations() {
        val current = _studioState.value
        pushUndoSnapshot(current.placedItems)
        val fixedItems = EngineeringComplianceAnalyzer.autoFixComplianceIssues(
            items = current.placedItems,
            roomWidthMm = current.roomWidthMm,
            roomDepthMm = current.roomDepthMm
        )
        _studioState.update { state ->
            val updated = state.copy(
                placedItems = fixedItems,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true
            )
            updated.copy(
                snapTelemetryAr = "تم التصحيح الهندسي التلقائي لمعايير التركيب ومساحات فتح الأبواب (${updated.complianceReport.complianceScorePercent}%)",
                statusBannerMessage = "تم ضبط مسافات الأمان وخلوص فتح الأبواب تلقائياً وفق معايير التركيب الهندسية"
            )
        }
        viewModelScope.launch {
            recordLocalRoomSnapshot("إصلاح تلقائي لمعايير التركيب الهندسية", isAutoSave = true)
        }
    }

    /**
     * Toggles the Interactive Virtual Ruler & Clearance Distance Guides on the 2D/3D canvas.
     */
    fun toggleVirtualRuler() {
        _studioState.update { state ->
            val next = !state.showVirtualRuler
            state.copy(
                showVirtualRuler = next,
                snapTelemetryAr = if (next) {
                    "المسطرة الافتراضية مفعّلة: عرض مسافات الخزائن عن الجدران والوحدات المجاورة لحظياً"
                } else {
                    "تم إخفاء المسطرة الافتراضية"
                }
            )
        }
    }

    fun setVirtualRulerCustomPinsMm(pinA: Pair<Int, Int>?, pinB: Pair<Int, Int>?) {
        _studioState.update { state ->
            state.copy(
                virtualRulerPinAMm = pinA,
                virtualRulerPinBMm = pinB
            )
        }
    }

    /**
     * Direct 2D Interactive Canvas Touch Resizing:
     * Resizes a kitchen unit's width and depth directly from touch drag handles on the 2D canvas.
     */
    fun resizeItemDirectMm(uid: String, newWidthMm: Int, newDepthMm: Int) {
        val state = _studioState.value
        val target = state.placedItems.firstOrNull { it.uid == uid } ?: return
        val clampedW = newWidthMm.coerceIn(150, (state.roomWidthMm - target.xMm).coerceAtLeast(150))
        val clampedD = newDepthMm.coerceIn(250, (state.roomDepthMm - target.yMm).coerceAtLeast(250))
        val widthRatio = clampedW.toFloat() / (target.type.defaultWidthCm * 10).coerceAtLeast(100)
        val updatedPrice = (target.type.defaultPriceSar * widthRatio).roundToInt().coerceAtLeast(150)

        val resized = target.withDimensionsMm(
            newWidthMm = clampedW,
            newDepthMm = clampedD
        ).copy(priceSar = updatedPrice)

        val formattedDims = state.unitSystem.formatDimensionsBox(resized.widthMm, resized.depthMm, resized.heightMm)
        _studioState.update { current ->
            current.copy(
                placedItems = current.placedItems.map { if (it.uid == uid) resized else it },
                hasUnsavedChanges = true,
                snapTelemetryAr = "تغيير أبعاد باللمس (2D): ${resized.displayTitle} ➔ $formattedDims"
            )
        }
    }

    /**
     * Records a single Undo checkpoint at the start of an interactive 2D/3D item drag or resize gesture.
     */
    fun beginItemDragUndoCheckpoint() {
        val currentItems = _studioState.value.placedItems
        pushUndoSnapshot(currentItems)
        _studioState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    /**
     * Applies a coordinated 3D Wall Color & Floor Material Harmony Palette with optional cabinet/countertop pairing.
     */
    fun applyWallAndFloorHarmonyPalette(
        palette: KitchenCatalogData.WallFloorHarmonyPalette,
        alsoApplyCabinetAndCountertop: Boolean = false
    ) {
        _studioState.update { current ->
            val nextCabinet = if (alsoApplyCabinetAndCountertop) palette.recommendedCabinetFinish else current.cabinetFinish
            val nextCounter = if (alsoApplyCabinetAndCountertop) palette.recommendedCountertop else current.countertopMaterial
            val harmony = KitchenCatalogData.evaluateVisualColorHarmony(
                wallColorKey = palette.wallColorHex,
                floorMaterial = palette.floorMaterial,
                cabinetFinish = nextCabinet,
                countertopMaterial = nextCounter
            )
            current.copy(
                wallColorHex = palette.wallColorHex,
                floorMaterial = palette.floorMaterial,
                cabinetFinish = nextCabinet,
                countertopMaterial = nextCounter,
                hasUnsavedChanges = true,
                snapTelemetryAr = "تناسق الحوائط والأرضيات: ${palette.titleAr} (التناسق البصري ${harmony.harmonyScorePercent}%)",
                statusBannerMessage = "تم تطبيق باليت «${palette.titleAr}» — الحائط: ${palette.wallColorNameAr} | الأرضية: ${palette.floorMaterial}"
            )
        }
    }

    /**
     * Dedicated Custom Cabinet Designer Board from Front & Side Reference Photos:
     * Builds a complete 6-sided 3D cabinet (with full 18mm/8mm back panel, full 18mm left/right side panels,
     * custom doors, drawers, and internal shelves) from front & side reference views, adds it to the 3D scene,
     * and optionally saves it to the Firestore catalog.
     */
    fun createCustomCabinetFromFrontAndSidePhotos(
        labelAr: String,
        baseType: ModuleType,
        widthMm: Int,
        depthMm: Int,
        heightMm: Int,
        doorsCount: Int,
        drawersCount: Int,
        shelvesCount: Int,
        hasFullBackPanel: Boolean = true,
        backPanelThicknessMm: Int = 18,
        hasFullSidePanels: Boolean = true,
        boardThicknessMm: Int = 18,
        customFinishName: String? = null,
        frontPhotoUriString: String = "",
        sidePhotoUriString: String = "",
        saveToFirestoreCatalog: Boolean = true
    ): PlacedKitchenItem {
        val state = _studioState.value
        pushUndoSnapshot(state.placedItems)

        val safeW = widthMm.coerceIn(150, 3200)
        val safeD = depthMm.coerceIn(200, 1500)
        val safeH = heightMm.coerceIn(250, 2600)
        val safeDoors = doorsCount.coerceIn(0, 6)
        val safeDrawers = drawersCount.coerceIn(0, 6)
        val safeShelves = shelvesCount.coerceIn(0, 8)
        val safeBackThick = backPanelThicknessMm.coerceIn(8, 18)
        val safeBoardThick = boardThicknessMm.coerceIn(16, 25)
        val resolvedLabel = labelAr.ifBlank {
            "خزانة مخصصة كاملة (${safeW}×${safeD}×${safeH} ملم)"
        }
        val estimatedPrice = (
            baseType.defaultPriceSar *
                (safeW.toFloat() / (baseType.defaultWidthCm * 10).coerceAtLeast(300)) *
                (if (hasFullBackPanel && safeBackThick >= 18) 1.08f else 1.0f)
            ).roundToInt().coerceAtLeast(350)

        val newUid = "custom_photo_${UUID.randomUUID().toString().take(6)}"
        val isUpper = baseType.category == ModuleCategory.WALL_CABINET
        val rawItem = PlacedKitchenItem(
            uid = newUid,
            type = baseType,
            xCm = 60,
            yCm = if (baseType == ModuleType.ISLAND_WATERFALL) (state.roomDepthCm / 2).coerceAtLeast(120) else 0,
            widthCm = (safeW / 10).coerceAtLeast(15),
            depthCm = (safeD / 10).coerceAtLeast(20),
            rotationDeg = 0,
            customLabel = resolvedLabel,
            priceSar = estimatedPrice,
            fineWidthMmOffset = safeW % 10,
            fineDepthMmOffset = safeD % 10,
            customHeightMm = safeH,
            customElevationMm = if (isUpper) 1450 else 0,
            boardThicknessMm = safeBoardThick,
            handleSystem = "GOLA_ALUMINUM",
            customCabinetFinish = customFinishName?.takeIf { it.isNotBlank() } ?: state.cabinetFinish,
            hasFullBackPanel = hasFullBackPanel,
            backPanelThicknessMm = safeBackThick,
            hasFullSidePanels = hasFullSidePanels,
            shelvesCount = safeShelves,
            doorsCount = safeDoors,
            drawersCount = safeDrawers
        )

        val solved = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = rawItem,
            desiredXMm = rawItem.xMm,
            desiredYMm = rawItem.yMm,
            allItems = state.placedItems,
            roomWidthMm = state.roomWidthMm,
            roomDepthMm = state.roomDepthMm,
            enableMagneticSnap = state.magneticSnapEnabled
        )

        val jsonV2 = JsonCadEngine.itemToJsonV2(
            item = solved.updatedItem,
            finishName = solved.updatedItem.customCabinetFinish ?: state.cabinetFinish,
            countertopName = state.countertopMaterial,
            captureSource = "FRONT_SIDE_PHOTO_CABINET_DESIGNER"
        )

        _studioState.update { current ->
            current.copy(
                placedItems = current.placedItems + solved.updatedItem,
                selectedItemUid = newUid,
                lastGeneratedJsonV2 = jsonV2,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                hasUnsavedChanges = true,
                snapTelemetryAr = "تم تصميم خزانة كاملة من المسقط الأمامي والجانبي (${safeW}×${safeD}×${safeH} ملم • ظهر ${safeBackThick}mm + أجناب ${safeBoardThick}mm)",
                statusBannerMessage = "تمت إضافة الخزانة المخصصة «$resolvedLabel» كاملة من جميع الجهات إلى التصميم والكتالوج"
            )
        }

        if (saveToFirestoreCatalog) {
            val desc = buildString {
                append("خزانة مصممة من صورة أمامية وجانبية — ")
                append("ظهر كامل ${safeBackThick} ملم + أجناب كاملة ${safeBoardThick} ملم | ")
                append("ضلف: $safeDoors | أدراج: $safeDrawers | أرفف: $safeShelves")
                if (frontPhotoUriString.isNotBlank() || sidePhotoUriString.isNotBlank()) {
                    append(" (مرفق مراجع الصور)")
                }
            }
            addCustomCatalogUnitToFirestore(
                nameAr = resolvedLabel,
                baseType = baseType,
                widthCm = (safeW / 10).coerceAtLeast(15),
                depthCm = (safeD / 10).coerceAtLeast(20),
                priceSar = estimatedPrice,
                descriptionAr = desc
            )
        }

        viewModelScope.launch {
            recordLocalRoomSnapshot("تصميم خزانة مخصصة من صور أمامية وجانبية ($resolvedLabel)", isAutoSave = true)
        }
        return solved.updatedItem
    }

    companion object {
        const val TAG = "KitchenPlannerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val AUTO_SAVE_INTERVAL_SECONDS = 30
        const val AUTO_SAVE_INTERVAL_MS = 30_000L
    }
}

