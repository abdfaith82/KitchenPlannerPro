package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.cam.CamNestingEngine
import com.example.data.cam.JsonCadEngine
import com.example.data.cam.ProjectExportManager
import com.example.data.local.LocalKitchenDatabase
import com.example.data.local.LocalProjectSnapshotEntity
import com.example.data.local.LocalSnapshotRepository
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import com.example.ui.viewmodel.PlannerStudioState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مخطط المطابخ Pro", appName)
    }

    @Test
    fun `offline room database saves and retrieves auto-save snapshots`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LocalKitchenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = LocalSnapshotRepository(db.projectSnapshotDao())

        val snapshot = LocalProjectSnapshotEntity(
            projectId = "proj_test_1",
            projectName = "مطبخ اختبار 1 ملم",
            shapeCode = "ISLAND",
            styleId = "MODERN_WALNUT",
            roomWidthMm = 4400,
            roomDepthMm = 3800,
            roomHeightMm = 2850,
            cabinetFinish = "زجاج أسود مدخن وألمنيوم مؤكسد",
            countertopMaterial = "رخام كرارا أبيض إيطالي",
            handleStyle = "مقابض مدمجة مخفية (Gola)",
            wallColorHex = "#F5F2EB",
            floorMaterial = "باركيه بلوط دافئ",
            itemsData = "SINK|90|0|90|60|0|حوض|2650|0|0|0|0",
            totalPriceSar = 18500,
            ergonomicScore = 92,
            snapshotReason = "حفظ تلقائي دوري (Room DB)",
            isAutoSave = true
        )
        repo.saveSnapshot(snapshot)
        val latest = repo.getLatestSnapshot()
        assertNotNull(latest)
        assertEquals(4400, latest?.roomWidthMm)
        assertEquals("حفظ تلقائي دوري (Room DB)", latest?.snapshotReason)
        db.close()
    }

    @Test
    fun `multi-format export and json v2 3d importer work accurately`() {
        val state = PlannerStudioState()
        val dxf = ProjectExportManager.buildAutoCadDxfContent(state)
        assertTrue(dxf.contains("ROOM_WALLS_MM"))
        assertTrue(dxf.contains("BASE_CABINETS_MM"))

        val sheetNest = CamNestingEngine.computeSheetNesting(state.placedItems, state.cabinetFinish)
        val alumNest = CamNestingEngine.computeAluminumBarNesting(state.placedItems)
        val csv = ProjectExportManager.buildCutListCsvContent(state, sheetNest, alumNest)
        assertTrue(csv.contains("2440×1220"))
        assertTrue(csv.contains("6000"))

        val unit = PlacedKitchenItem(uid = "b1", type = ModuleType.BASE_DRAWERS_80, xCm = 45, yCm = 0, fineXMmOffset = 6)
        val jsonCode = JsonCadEngine.itemToJsonV2(unit, "جوز أمريكي طبيعي", "رخام كرارا أبيض إيطالي")
        val parsed = JsonCadEngine.parseItemFromJsonV2(jsonCode).getOrThrow()
        assertEquals(456, parsed.xMm)
        assertEquals(800, parsed.widthMm)
        assertEquals(18, parsed.boardThicknessMm)

        val objImport = JsonCadEngine.parseExternal3DOrCadContent(
            "island.obj",
            "v 0.0 0.0 0.0\nv 1.65 0.85 0.90"
        ).getOrThrow()
        assertEquals(1650, objImport.first.widthMm)
        assertEquals(900, objImport.first.depthMm)
    }

    @Test
    fun `universal json importer and camera to json support any json structure`() {
        // 1. Root JSONArray of multiple objects
        val arrayJson = """
            [
              { "name": "خزانة أدراج Blum", "type": "BASE_DRAWERS", "width": 900, "depth": 600, "height": 850 },
              { "name": "فيترينة علوية", "type": "WALL_GLASS", "width": 1000, "depth": 350, "height": 780 }
            ]
        """.trimIndent()
        val arrayResult = JsonCadEngine.parseUniversalJsonContent(arrayJson, "units_list.json").getOrThrow()
        assertEquals(2, arrayResult.items.size)
        assertEquals(900, arrayResult.items[0].widthMm)
        assertEquals(ModuleType.WALL_GLASS_80, arrayResult.items[1].type)
        assertTrue(arrayResult.items[1].elevationMm >= 1450)

        // 2. Generic Three.js / BIM JSON with meter dimensions [0.92, 1.85, 0.72]
        val bimJson = """
            {
              "id": "bim_fridge_01",
              "title": "Smart French Door Fridge",
              "category": "fridge",
              "size": [0.92, 1.85, 0.72],
              "position": [2.40, 0.0, 0.0],
              "price": 6400
            }
        """.trimIndent()
        val bimResult = JsonCadEngine.parseUniversalJsonContent(bimJson, "bim_fridge.json").getOrThrow()
        assertEquals(1, bimResult.items.size)
        assertEquals(ModuleType.FRIDGE_SMART, bimResult.items.first().type)
        assertEquals(920, bimResult.items.first().widthMm)
        assertEquals(720, bimResult.items.first().depthMm)
        assertEquals(1850, bimResult.items.first().heightMm)
        assertEquals(2400, bimResult.items.first().xMm)

        // 3. Camera-to-JSON v2.0 with sampled pixel hex color
        val camUnit = PlacedKitchenItem(uid = "cam_1", type = ModuleType.WALL_GLASS_80, xCm = 60, yCm = 0, widthCm = 95, depthCm = 35)
        val camJson = JsonCadEngine.itemToJsonV2(
            item = camUnit,
            finishName = "زجاج أسود مدخن وألمنيوم مؤكسد",
            countertopName = "رخام كرارا أبيض إيطالي",
            sampledHexOverride = "#1B262C",
            captureSource = "LIVE_CAMERA_SCANNER_V2"
        )
        assertTrue(camJson.contains("LIVE_CAMERA_SCANNER_V2"))
        assertTrue(camJson.contains("#1B262C"))
    }

    @Test
    fun `autocad dxf round-trip export and import and firestore automatic cost estimator work accurately`() {
        // 1. Verify 30-second Room auto-save configuration
        assertEquals(30, com.example.ui.viewmodel.KitchenPlannerViewModel.AUTO_SAVE_INTERVAL_SECONDS)
        assertEquals(30_000L, com.example.ui.viewmodel.KitchenPlannerViewModel.AUTO_SAVE_INTERVAL_MS)

        // 2. Verify AutoCAD DXF export and multi-unit import round-trip
        val initialState = PlannerStudioState(
            projectName = "مطبخ اختبار المقاول DXF",
            roomWidthCm = 460,
            roomDepthCm = 360,
            roomHeightCm = 290
        )
        val exportedDxf = ProjectExportManager.buildAutoCadDxfContent(initialState)
        assertTrue(exportedDxf.contains("\$INSUNITS"))
        assertTrue(exportedDxf.contains("LINE"))
        assertTrue(exportedDxf.contains("ROOM_SPEC|NAME:مطبخ اختبار المقاول DXF|W:4600|D:3600|H:2900"))

        val importedFromDxf = JsonCadEngine.parseAutoCadDxfContent(
            rawDxfText = exportedDxf,
            fileName = "contractor_workshop_plan.dxf"
        ).getOrThrow()
        assertEquals(initialState.placedItems.size, importedFromDxf.items.size)
        assertEquals(4600, importedFromDxf.roomWidthMm)
        assertEquals(3600, importedFromDxf.roomDepthMm)

        // 3. Verify generic AutoCAD LWPOLYLINE contractor DXF import without CAD_UNIT metadata
        val rawContractorDxf = """
            0
            SECTION
            2
            ENTITIES
            0
            LWPOLYLINE
            8
            ROOM_WALLS_MM
            10
            0.0
            20
            0.0
            10
            4200.0
            20
            3400.0
            0
            LWPOLYLINE
            8
            BASE_CABINETS_MM
            10
            200.0
            20
            0.0
            10
            1100.0
            20
            600.0
            0
            LWPOLYLINE
            8
            ISLAND_UNITS_MM
            10
            1000.0
            20
            1600.0
            10
            2800.0
            20
            2550.0
            0
            ENDSEC
            0
            EOF
        """.trimIndent()
        val parsedContractor = JsonCadEngine.parseAutoCadDxfContent(
            rawDxfText = rawContractorDxf,
            fileName = "workshop_layers.dxf"
        ).getOrThrow()
        assertEquals(4200, parsedContractor.roomWidthMm)
        assertEquals(3400, parsedContractor.roomDepthMm)
        assertEquals(2, parsedContractor.items.size)
        assertEquals(900, parsedContractor.items[0].widthMm)
        assertEquals(1800, parsedContractor.items[1].widthMm)

        // 4. Verify real-time automatic cost estimator synced with Firestore catalog units
        val defaultCatalog = com.example.data.model.KitchenCatalogData.defaultCatalogUnitsForUser("test_user")
        val stateWithDefaultCatalog = initialState.copy(firestoreCatalogUnits = defaultCatalog)
        val initialCost = stateWithDefaultCatalog.costBreakdown
        assertTrue(initialCost.cabinetsCostSar > 0)
        assertTrue(initialCost.countertopCostSar > 0)
        assertTrue(initialCost.accessoriesCostSar > 0)
        assertTrue(initialCost.firestoreSyncedUnitsCount > 0)

        // Simulate Firestore catalog price update (+1000 SAR on each catalog unit)
        val updatedFirestoreCatalog = defaultCatalog.map { it.copy(priceSar = it.priceSar + 1000L) }
        val stateWithUpdatedFirestorePrices = initialState.copy(firestoreCatalogUnits = updatedFirestoreCatalog)
        val updatedCost = stateWithUpdatedFirestorePrices.costBreakdown
        assertTrue(
            "Expected total cost to increase automatically when Firestore catalog prices increase",
            updatedCost.totalSar > initialCost.totalSar
        )
    }

    @Test
    fun `gemini ai layout parser and pdf report with 3d render and ar material json sync work accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify Gemini AI structured JSON response parsing into complete 1mm kitchen layout
        val sampleGeminiJson = """
            {
              "designTitleAr": "مطبخ الذكاء الاصطناعي العصري بالجزيرة",
              "recommendedShape": "ISLAND",
              "cabinetFinishAr": "بلوط إسكندنافي فاتح",
              "countertopMaterialAr": "رخام كالكاتا إيطالي",
              "wallColorHex": "#F5F2EB",
              "architecturalRationaleAr": "تم توزيع الحوض والموقد والثلاجة وفق معايير مثلث العمل القياسي مع جزيرة إفطار وسطية.",
              "ergonomicHighlightAr": "مثلث عمل متوازن بنسبة 96%",
              "units": [
                {
                  "moduleCode": "SINK",
                  "labelAr": "وحدة حوض مزدوج ذكية",
                  "xMm": 200,
                  "yMm": 0,
                  "widthMm": 1000,
                  "depthMm": 600,
                  "heightMm": 850,
                  "elevationMm": 0,
                  "rotationDeg": 0
                },
                {
                  "moduleCode": "HOB",
                  "labelAr": "فرن وموقد غاز 5 عيون",
                  "xMm": 1200,
                  "yMm": 0,
                  "widthMm": 900,
                  "depthMm": 600,
                  "heightMm": 850,
                  "elevationMm": 0,
                  "rotationDeg": 0
                },
                {
                  "moduleCode": "FRIDGE",
                  "labelAr": "ثلاجة ذكية باب فرنسي",
                  "xMm": 2100,
                  "yMm": 0,
                  "widthMm": 920,
                  "depthMm": 720,
                  "heightMm": 1850,
                  "elevationMm": 0,
                  "rotationDeg": 0
                },
                {
                  "moduleCode": "ISLAND",
                  "labelAr": "جزيرة شلال رخام كالكاتا",
                  "xMm": 900,
                  "yMm": 1650,
                  "widthMm": 1900,
                  "depthMm": 950,
                  "heightMm": 900,
                  "elevationMm": 0,
                  "rotationDeg": 0
                }
              ]
            }
        """.trimIndent()

        val geminiSuggestion = com.example.ai.GeminiKitchenDesignService.parseGeminiKitchenResponse(
            rawJson = sampleGeminiJson,
            roomWidthCm = 450,
            roomDepthCm = 380,
            fallbackShape = com.example.data.model.KitchenShape.ISLAND
        )
        assertEquals("مطبخ الذكاء الاصطناعي العصري بالجزيرة", geminiSuggestion.designTitleAr)
        assertEquals(4, geminiSuggestion.suggestedItems.size)
        assertEquals(1000, geminiSuggestion.suggestedItems[0].widthMm)
        assertEquals("بلوط إسكندنافي فاتح", geminiSuggestion.cabinetFinishAr)

        // 2. Verify AR Material Customization immediately syncs with the cabinet's JSON v2.0 representation
        val arCabinet = geminiSuggestion.suggestedItems.first().copy(
            customCabinetFinish = "أخضر ميرمية مطفي (Sage)",
            customCountertopMaterial = "جرانيت أسود جالاكسي"
        )
        val syncedArJson = JsonCadEngine.itemToJsonV2(
            item = arCabinet,
            finishName = geminiSuggestion.cabinetFinishAr,
            countertopName = geminiSuggestion.countertopMaterialAr,
            captureSource = "AR_LIVE_MATERIAL_EDITOR"
        )
        assertTrue(syncedArJson.contains("AR_LIVE_MATERIAL_EDITOR"))
        assertTrue(syncedArJson.contains("أخضر ميرمية مطفي (Sage)"))
        assertTrue(syncedArJson.contains("جرانيت أسود جالاكسي"))

        val reparsedFromArJson = JsonCadEngine.parseItemFromJsonV2(syncedArJson).getOrThrow()
        assertEquals("أخضر ميرمية مطفي (Sage)", reparsedFromArJson.customCabinetFinish)
        assertEquals("جرانيت أسود جالاكسي", reparsedFromArJson.customCountertopMaterial)

        // 3. Verify Professional Multi-Page PDF Export (3D Render + 2D Blueprint + BOM)
        val studioState = PlannerStudioState(
            projectName = geminiSuggestion.designTitleAr,
            placedItems = listOf(arCabinet) + geminiSuggestion.suggestedItems.drop(1)
        )
        val pdfFile = ProjectExportManager.generateEngineeringPdfReport(
            context = context,
            state = studioState,
            nesting = studioState.sheetNesting,
            aluminum = studioState.aluminumNesting,
            shareViaMessagingApps = false
        ).getOrThrow()
        assertTrue("Expected generated PDF report file to exist and be non-empty", pdfFile.exists() && pdfFile.length() > 300L)
    }

    @Test
    fun `lighting simulation, real-time compliance validator, smart accessories bom, metric-imperial switcher, and virtual ruler work accurately`() {
        // 1. Metric vs Imperial Measurement System Switcher
        val metric = com.example.data.cad.MeasurementUnitSystem.METRIC
        val imperial = com.example.data.cad.MeasurementUnitSystem.IMPERIAL
        assertTrue(metric.formatMillimeters(900).contains("900 ملم"))
        assertTrue(imperial.formatMillimeters(914).contains("\""))
        assertTrue(imperial.formatLinearCountertop(3.0).contains("LF"))

        // 2. 3D Lighting Simulation Engine Presets
        val sunsetLighting = com.example.data.cad.LightingSimulationConfig.fromPreset(
            com.example.data.cad.NaturalDaylightPreset.GOLDEN_SUNSET
        )
        assertEquals(3000, sunsetLighting.colorTemperatureKelvin)
        assertTrue(sunsetLighting.shadowSoftnessAndDepth > 0.7f)

        // 3. Real-Time Engineering Compliance Validator & Auto-Fix
        // Create a layout with:
        // - Stove directly adjacent to Fridge (0mm gap < 300mm thermal requirement)
        // - Island placed too close to front of base cabinets (500mm aisle < 900mm requirement)
        // - Upper cabinet placed too low (1250mm elevation < 1450mm safety clearance)
        val violatingItems = listOf(
            PlacedKitchenItem(uid = "stove_1", type = ModuleType.STOVE_HOB, xCm = 0, yCm = 0, widthCm = 90, depthCm = 60),
            PlacedKitchenItem(uid = "fridge_1", type = ModuleType.FRIDGE_SMART, xCm = 90, yCm = 0, widthCm = 92, depthCm = 72),
            PlacedKitchenItem(uid = "island_1", type = ModuleType.ISLAND_WATERFALL, xCm = 40, yCm = 110, widthCm = 180, depthCm = 90),
            PlacedKitchenItem(uid = "wall_low_1", type = ModuleType.WALL_GLASS_80, xCm = 200, yCm = 0, widthCm = 80, depthCm = 35, customElevationMm = 1250)
        )
        val complianceReport = com.example.data.cad.EngineeringComplianceAnalyzer.validateLayout(
            items = violatingItems,
            roomWidthMm = 4200,
            roomDepthMm = 2800
        )
        assertTrue("Expected real-time compliance validator to trigger alerts", complianceReport.alerts.isNotEmpty())
        assertTrue(complianceReport.alerts.any { it.category == com.example.data.cad.ComplianceRuleCategory.APPLIANCE_SEPARATION })
        assertTrue(complianceReport.alerts.any { it.category == com.example.data.cad.ComplianceRuleCategory.UPPER_CABINET_CLEARANCE })

        val autoFixedItems = com.example.data.cad.EngineeringComplianceAnalyzer.autoFixComplianceIssues(
            items = violatingItems,
            roomWidthMm = 4200,
            roomDepthMm = 2800
        )
        val fixedUpperCab = autoFixedItems.first { it.uid == "wall_low_1" }
        assertTrue("Expected upper cabinet elevation to be auto-fixed to >= 1450mm", fixedUpperCab.elevationMm >= 1450)

        // 4. Smart Internal Kitchen Accessories Auto-Linking & BOM Update
        val defaultState = PlannerStudioState()
        assertTrue("Expected smart internal accessories to auto-link to compatible units", defaultState.linkedAccessoriesBom.isNotEmpty())
        assertTrue("Expected smart internal accessories cost to be > 0 SAR", defaultState.costBreakdown.smartInternalAccessoriesCostSar > 0)
        assertTrue("Expected counter linear feet to be computed", defaultState.costBreakdown.countertopLinearFeet > 0.0)

        // 5. Interactive Virtual Ruler Distances (Wall & Neighbor Clearances)
        val targetUnit = defaultState.placedItems.first()
        val distances = com.example.data.cad.VirtualRulerEngine.measureUnitDistances(
            selected = targetUnit,
            allItems = defaultState.placedItems,
            roomWidthMm = defaultState.roomWidthMm,
            roomDepthMm = defaultState.roomDepthMm
        )
        assertEquals(targetUnit.xMm, distances.leftWallMm)
        assertEquals(defaultState.roomWidthMm - (targetUnit.xMm + targetUnit.effectiveWidthMm), distances.rightWallMm)
        assertTrue(distances.summaryAr(metric).contains("يسار"))
    }

    @Test
    fun `corner cabinets, 6-sided full back and side panels, photo cabinet designer, wall-floor harmony, and direct blueprint sharing work accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Verify Expanded Catalog with Upper & Lower Corner Cabinets and Multi-Option Units
        assertTrue("Expected at least 25 module types in catalog", ModuleType.entries.size >= 25)
        val cornerTypes = ModuleType.entries.filter { it.isCornerCabinet }
        assertTrue("Expected at least 6 upper and lower corner cabinet types", cornerTypes.size >= 6)
        assertTrue(cornerTypes.contains(ModuleType.CORNER_BASE_CAROUSEL_90))
        assertTrue(cornerTypes.contains(ModuleType.CORNER_BASE_DIAGONAL_105))
        assertTrue(cornerTypes.contains(ModuleType.CORNER_WALL_L_65))
        assertTrue(cornerTypes.contains(ModuleType.CORNER_WALL_DIAGONAL_60))

        // 2. Verify 6-Sided Complete Cabinet Structure (Full Back Panel + Full Left/Right Side Panels)
        val customPhotoCabinet = PlacedKitchenItem(
            uid = "photo_cab_1",
            type = ModuleType.TALL_COFFEE_STATION_90,
            xCm = 120,
            yCm = 0,
            widthCm = 90,
            depthCm = 60,
            customHeightMm = 2200,
            customLabel = "ركن قهوة مصمم من صور أمامية وجانبية",
            priceSar = 4500,
            hasFullBackPanel = true,
            backPanelThicknessMm = 18,
            hasFullSidePanels = true,
            shelvesCount = 4,
            doorsCount = 2,
            drawersCount = 2
        )
        val serializedItems = com.example.data.model.KitchenItemsSerializer.encode(listOf(customPhotoCabinet))
        val deserializedItems = com.example.data.model.KitchenItemsSerializer.decode(serializedItems)
        assertEquals(1, deserializedItems.size)
        val restored = deserializedItems.first()
        assertEquals(true, restored.hasFullBackPanel)
        assertEquals(18, restored.backPanelThicknessMm)
        assertEquals(true, restored.hasFullSidePanels)

        val jsonV2 = JsonCadEngine.itemToJsonV2(customPhotoCabinet, "بلوط إسكندنافي فاتح", "رخام كرارا أبيض إيطالي")
        val parsedV2 = JsonCadEngine.parseItemFromJsonV2(jsonV2).getOrThrow()
        assertEquals(true, parsedV2.hasFullBackPanel)
        assertEquals(18, parsedV2.backPanelThicknessMm)
        assertEquals(true, parsedV2.hasFullSidePanels)
        assertEquals(4, parsedV2.shelvesCount)

        // Verify CAM Sheet Nesting includes full 18mm back panel and full side panels
        val sheetNest = CamNestingEngine.computeSheetNesting(listOf(customPhotoCabinet), "بلوط إسكندنافي فاتح")
        assertTrue(sheetNest.cutPieces.any { it.partNameAr.contains("ظهر") })
        assertTrue(sheetNest.cutPieces.any { it.partNameAr.contains("جنب") })

        // 3. Verify 3D Wall & Floor Color Harmony Customizer & Custom Hex Support
        val customHexColor = com.example.data.model.KitchenCatalogData.resolveWallColorHex("#DCE3DC")
        assertEquals(0xFFDCE3DC, customHexColor)
        assertTrue(com.example.data.model.KitchenCatalogData.wallColors.size >= 12)
        assertTrue(com.example.data.model.KitchenCatalogData.floorMaterials.size >= 10)
        assertTrue(com.example.data.model.KitchenCatalogData.harmonyPalettes.size >= 6)

        val harmonyEvaluation = com.example.data.model.KitchenCatalogData.evaluateVisualColorHarmony(
            wallColorHex = "#DCE3DC",
            floorMaterial = "باركيه بلوط دافئ",
            cabinetFinish = "أبيض حريري لؤلؤي",
            countertopMaterial = "رخام كرارا أبيض إيطالي"
        )
        assertTrue("Expected harmony score in valid range 74..98", harmonyEvaluation.harmonyScorePercent in 74..98)
        assertTrue(harmonyEvaluation.verdictAr.isNotBlank())

        // 4. Verify Direct Blueprint Sharing via Social Media PNG and Workshop Email
        val studioState = PlannerStudioState(
            projectName = "مطبخ اختبار المشاركة المباشرة والتناسق اللوني",
            wallColorHex = "#DCE3DC",
            floorMaterial = "باركيه بلوط دافئ",
            placedItems = listOf(customPhotoCabinet) + PlannerStudioState().placedItems.take(3)
        )
        val socialPngFile = ProjectExportManager.generateAndShareSocialBlueprintImage(
            context = context,
            state = studioState,
            customMessageNote = "مشاركة مباشرة للمخطط والرندر",
            shareImmediately = false
        ).getOrThrow()
        assertTrue("Expected social blueprint PNG card to exist and be non-empty", socialPngFile.exists() && socialPngFile.length() > 30L)

        val workshopEmailPdfFile = ProjectExportManager.shareBlueprintViaEmailToWorkshop(
            context = context,
            state = studioState,
            recipientEmail = "workshop@example.com",
            workshopOrRecipientName = "ورشة النخبة",
            customMessageNote = "يرجى اعتماد ظهر كامل 18 ملم وأجناب كاملة لجميع الخزائن"
        ).getOrThrow()
        assertTrue("Expected workshop email PDF package to exist", workshopEmailPdfFile.exists() && workshopEmailPdfFile.length() > 300L)
    }

    @Test
    fun `vr camera sensor navigation and customizable standard material dimensions for melamine sheets and aluminum bars work accurately`() {
        val gridCols = 16
        val gridRows = 12

        // 1. Verify VrCameraOpticalFlowEngine detects camera pan (horizontal shift) and tilt (vertical shift)
        val prevGrid = FloatArray(gridCols * gridRows) { idx ->
            val gx = idx % gridCols
            val gy = idx / gridCols
            // Create a distinctive bright feature around (7, 5)
            val distSq = (gx - 7) * (gx - 7) + (gy - 5) * (gy - 5)
            if (distSq <= 4) 220f else (gx * 6f + gy * 8f)
        }
        val currShiftedRightDown = FloatArray(gridCols * gridRows) { idx ->
            val gx = idx % gridCols
            val gy = idx / gridCols
            // Feature moved from (7, 5) -> (8, 6) (+1 in X, +1 in Y)
            val distSq = (gx - 8) * (gx - 8) + (gy - 6) * (gy - 6)
            if (distSq <= 4) 220f else ((gx - 1).coerceAtLeast(0) * 6f + (gy - 1).coerceAtLeast(0) * 8f)
        }

        val (panFlowX, tiltFlowY, _) = com.example.ui.components.VrCameraOpticalFlowEngine.computeGridOpticalFlow(
            prevGrid = prevGrid,
            currGrid = currShiftedRightDown,
            cols = gridCols,
            rows = gridRows
        )
        assertTrue("Expected non-zero horizontal optical flow from camera pan", panFlowX != 0f)
        assertTrue("Expected non-zero vertical optical flow from camera tilt", tiltFlowY != 0f)

        // 2. Verify customizable standard Melamine sheet dimensions (e.g. Jumbo 2800x2070 vs Compact 2100x1220, thickness 22mm)
        val defaultState = PlannerStudioState()
        val defaultSheetNesting = defaultState.sheetNesting
        assertEquals(2440, defaultSheetNesting.standardSheetWidthMm)
        assertEquals(1220, defaultSheetNesting.standardSheetHeightMm)
        assertEquals(18, defaultSheetNesting.boardThicknessMm)

        val jumboStandards = com.example.data.cam.CamMaterialStandardsConfig(
            sheetWidthMm = com.example.data.cam.StandardMelamineSheetPreset.EUROPEAN_2800_2070.widthMm,
            sheetHeightMm = com.example.data.cam.StandardMelamineSheetPreset.EUROPEAN_2800_2070.heightMm,
            boardThicknessMm = 22,
            sawKerfMm = 5,
            aluminumBarLengthMm = com.example.data.cam.StandardAluminumBarPreset.BAR_3000.lengthMm,
            aluminumSawKerfMm = 4
        )
        val jumboState = defaultState.copy(camMaterialStandards = jumboStandards)
        val jumboSheetNesting = jumboState.sheetNesting
        assertEquals(2800, jumboSheetNesting.standardSheetWidthMm)
        assertEquals(2070, jumboSheetNesting.standardSheetHeightMm)
        assertEquals(22, jumboSheetNesting.boardThicknessMm)
        assertEquals(5, jumboSheetNesting.kerfMm)
        assertTrue(
            "Expected jumbo 2800x2070 sheets to require less or equal sheets compared to standard 2440x1220",
            jumboSheetNesting.totalSheets18Mm <= defaultSheetNesting.totalSheets18Mm
        )

        // 3. Verify customizable standard Aluminum profile bar dimensions (3000mm vs 6000mm)
        val defaultAluminumNesting = defaultState.aluminumNesting
        val shortBarAluminumNesting = jumboState.aluminumNesting
        assertEquals(6000, defaultAluminumNesting.standardBarLengthMm)
        assertEquals(3000, shortBarAluminumNesting.standardBarLengthMm)
        assertEquals(4, jumboState.camMaterialStandards.aluminumSawKerfMm)
        assertTrue(
            "Expected 3000mm aluminum bars to require at least as many bars as 6000mm bars",
            shortBarAluminumNesting.totalBarsCount >= defaultAluminumNesting.totalBarsCount
        )

        val exportedCsv = ProjectExportManager.buildCutListCsvContent(
            state = jumboState,
            nesting = jumboSheetNesting,
            aluminum = shortBarAluminumNesting
        )
        assertTrue(exportedCsv.contains("2800×2070"))
        assertTrue(exportedCsv.contains("3000 ملم"))

        // 4. Verify VR Camera Walkthrough view mode state properties
        val vrState = jumboState.copy(
            viewMode = com.example.ui.viewmodel.StudioViewMode.VR_CAMERA_WALKTHROUGH,
            firstPersonStepForwardCm = 85f,
            vrStrafeLateralCm = -30f,
            vrEyeHeightCm = 168f,
            vrStereoscopicSplitEnabled = true
        )
        assertEquals(com.example.ui.viewmodel.StudioViewMode.VR_CAMERA_WALKTHROUGH, vrState.viewMode)
        assertEquals(85f, vrState.firstPersonStepForwardCm)
        assertEquals(-30f, vrState.vrStrafeLateralCm)
        assertEquals(168f, vrState.vrEyeHeightCm)
        assertTrue(vrState.vrStereoscopicSplitEnabled)
    }

    @Test
    fun `real-time red box intersection detector, 3d standard material dimensions in cost engine, and photorealistic textures work accurately`() {
        // 1. Verify Photorealistic Marble, Wood, and Granite textures are assigned to all catalog finishes
        assertTrue(
            "Expected all cabinet finishes to have a photorealistic texture resource ID",
            com.example.data.model.KitchenCatalogData.cabinetFinishes.all { it.textureResId != 0 }
        )
        assertTrue(
            "Expected all countertop materials (marble, wood, granite) to have a photorealistic texture resource ID",
            com.example.data.model.KitchenCatalogData.countertopMaterials.all { it.textureResId != 0 }
        )
        assertTrue(
            "Expected all floor materials to have a photorealistic texture resource ID",
            com.example.data.model.KitchenCatalogData.floorMaterials.all { it.textureResId != 0 }
        )

        // 2. Verify Real-Time Red Box Intersection & Minimum-Distance Violation Detection (violatingUnitUids)
        val intersectingCabinets = listOf(
            PlacedKitchenItem(uid = "cab_a", type = ModuleType.BASE_DRAWERS_80, xCm = 20, yCm = 0, widthCm = 80, depthCm = 60),
            // cab_b overlaps cab_a by 300mm (xCm = 70 vs cab_a ending at 100)
            PlacedKitchenItem(uid = "cab_b", type = ModuleType.BASE_CABINET_60, xCm = 70, yCm = 0, widthCm = 60, depthCm = 60)
        )
        val intersectionReport = com.example.data.cad.EngineeringComplianceAnalyzer.validateLayout(
            items = intersectingCabinets,
            roomWidthMm = 4000,
            roomDepthMm = 3400
        )
        assertTrue(
            "Expected UNIT_INTERSECTION alert when two cabinets overlap",
            intersectionReport.alerts.any { it.category == com.example.data.cad.ComplianceRuleCategory.UNIT_INTERSECTION }
        )
        assertTrue(
            "Expected both intersecting cabinets (cab_a and cab_b) to be in violatingUnitUids for red bounding box rendering",
            "cab_a" in intersectionReport.violatingUnitUids && "cab_b" in intersectionReport.violatingUnitUids
        )

        val resolvedAfterAutoFix = com.example.data.cad.EngineeringComplianceAnalyzer.autoFixComplianceIssues(
            items = intersectingCabinets,
            roomWidthMm = 4000,
            roomDepthMm = 3400
        )
        val reportAfterFix = com.example.data.cad.EngineeringComplianceAnalyzer.validateLayout(
            items = resolvedAfterAutoFix,
            roomWidthMm = 4000,
            roomDepthMm = 3400
        )
        assertTrue(
            "Expected autoFixComplianceIssues to eliminate UNIT_INTERSECTION between cab_a and cab_b",
            reportAfterFix.alerts.none { it.category == com.example.data.cad.ComplianceRuleCategory.UNIT_INTERSECTION }
        )

        // 3. Verify 3D Standard Material Dimensions (Length, Width, Depth) for Panels & Profiles are adopted in Cost Engine & Cut-List
        val baseState = PlannerStudioState()
        val baseCost = baseState.costBreakdown
        assertTrue(baseCost.materialsTotalSar > 0)
        assertTrue(baseCost.laborAndCncSar > 0)

        val deepThickCustomStandards = com.example.data.cam.CamMaterialStandardsConfig(
            sheetWidthMm = 2800,
            sheetHeightMm = 2070,
            boardThicknessMm = 25,
            standardPanelDepthMm = 700,
            aluminumBarLengthMm = 3000,
            aluminumProfileWidthMm = 60,
            aluminumProfileDepthMm = 30
        )
        val customStdState = baseState.copy(camMaterialStandards = deepThickCustomStandards)
        val updatedCost = customStdState.costBreakdown
        assertEquals(700, customStdState.sheetNesting.standardPanelDepthMm)
        assertEquals(60, customStdState.aluminumNesting.profileWidthMm)
        assertEquals(30, customStdState.aluminumNesting.profileDepthMm)
        assertTrue(
            "Expected 25mm board thickness + 700mm standard panel depth + 60x30mm aluminum profile to update total cost",
            updatedCost.totalSar != baseCost.totalSar
        )
    }
}

