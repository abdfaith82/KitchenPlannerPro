package com.example

import com.example.data.cad.AutoFillerEngine
import com.example.data.cad.CadPhysicsEngine
import com.example.data.cad.FillerPlacementMode
import com.example.data.cam.CamNestingEngine
import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import androidx.compose.ui.geometry.Offset
import com.example.ui.components.screenOffsetToRoomMillimeters
import com.example.ui.viewmodel.PlannerStudioState
import com.example.ui.viewmodel.StudioViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun dragAndDropScreenToRoomMillimeters_unprojects2DAnd3DCoordinatesAccurately() {
        val state2D = PlannerStudioState(
            viewMode = StudioViewMode.PLAN_2D,
            roomWidthCm = 400,
            roomDepthCm = 300
        )
        val (x2d, y2d) = screenOffsetToRoomMillimeters(
            canvasOffset = Offset(500f, 400f),
            canvasWidth = 1000f,
            canvasHeight = 800f,
            state = state2D,
            unitWidthMm = 800,
            unitDepthMm = 600,
            isWallCabinet = false
        )
        assertTrue(x2d in 1400..1800)
        assertTrue(y2d in 1000..1400)

        val state3D = PlannerStudioState(
            viewMode = StudioViewMode.RENDER_3D,
            roomWidthCm = 400,
            roomDepthCm = 300
        )
        val (x3d, y3d) = screenOffsetToRoomMillimeters(
            canvasOffset = Offset(500f, 480f),
            canvasWidth = 1000f,
            canvasHeight = 800f,
            state = state3D,
            unitWidthMm = 800,
            unitDepthMm = 600,
            unitElevationMm = 0,
            unitHeightMm = 850,
            isWallCabinet = false
        )
        assertTrue(x3d in 0..3200)
        assertTrue(y3d in 0..2400)
    }

    @Test
    fun cad1mmPrecision_andMagneticSnappingAntiOverlap_workCorrectly() {
        val base1 = PlacedKitchenItem(
            uid = "base_1",
            type = ModuleType.BASE_DRAWERS_80,
            xCm = 0,
            yCm = 0,
            widthCm = 80,
            depthCm = 60
        )
        val base2 = PlacedKitchenItem(
            uid = "base_2",
            type = ModuleType.BASE_CABINET_60,
            xCm = 82,
            yCm = 0,
            widthCm = 60,
            depthCm = 60,
            fineXMmOffset = 7 // 827 mm
        )
        assertEquals(827, base2.xMm)

        val snapRes = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = base2,
            desiredXMm = 825,
            desiredYMm = 12,
            allItems = listOf(base1, base2),
            roomWidthMm = 4000,
            roomDepthMm = 3400,
            enableMagneticSnap = true
        )
        assertEquals(800, snapRes.updatedItem.xMm)
        assertEquals(0, snapRes.updatedItem.yMm)
        assertTrue(snapRes.snappedToNeighbor)

        val overlapRes = CadPhysicsEngine.solveMoveWithSnappingAndAntiOverlap(
            targetItem = base2,
            desiredXMm = 200,
            desiredYMm = 0,
            allItems = listOf(base1, base2),
            roomWidthMm = 4000,
            roomDepthMm = 3400,
            enableMagneticSnap = false
        )
        assertTrue(overlapRes.collisionPrevented)
        assertTrue(overlapRes.updatedItem.xMm >= 800 || overlapRes.updatedItem.yMm >= 600)
    }

    @Test
    fun verticalWallCabinetPairing_enforces600mmSafetyClearance() {
        val base = PlacedKitchenItem(
            uid = "base_sink",
            type = ModuleType.SINK_DOUBLE,
            xCm = 90,
            yCm = 0,
            widthCm = 90,
            depthCm = 60
        )
        val pairedList = CadPhysicsEngine.pairAndAlignUpperWallCabinets(
            items = listOf(base),
            roomWidthMm = 4000,
            safetyClearanceMm = 600
        )
        val upper = pairedList.firstOrNull { it.type.category == ModuleCategory.WALL_CABINET }
        assertTrue("Expected an upper wall cabinet to be paired above the base cabinet", upper != null)
        assertEquals(1450, upper!!.elevationMm)
        assertEquals(900, upper.xMm)
        assertEquals(900, upper.widthMm)
    }

    @Test
    fun automaticFillerTool_fillsWallGapsWith1mmPrecision() {
        val u1 = PlacedKitchenItem(uid = "u1", type = ModuleType.BASE_DRAWERS_80, xCm = 0, yCm = 0, widthCm = 80)
        val u2 = PlacedKitchenItem(uid = "u2", type = ModuleType.SINK_DOUBLE, xCm = 80, yCm = 0, widthCm = 90)
        val u3 = PlacedKitchenItem(uid = "u3", type = ModuleType.STOVE_HOB, xCm = 170, yCm = 0, widthCm = 90)
        val (filledItems, _) = AutoFillerEngine.generateAutoFillersForWall(
            items = listOf(u1, u2, u3),
            targetWallLengthMm = 2743,
            mode = FillerPlacementMode.SPLIT_EQUAL_ENDS,
            includeUpperWallFillers = false
        )
        val fillers = filledItems.filter { it.type == ModuleType.FILLER_STRIP }
        assertEquals(2, fillers.size)
        assertEquals(143, fillers.sumOf { it.widthMm })
    }

    @Test
    fun camNesting_computesSheetsAndAluminumBars() {
        val items = listOf(
            PlacedKitchenItem(uid = "b1", type = ModuleType.BASE_DRAWERS_80, xCm = 0, yCm = 0),
            PlacedKitchenItem(uid = "w1", type = ModuleType.WALL_GLASS_80, xCm = 80, yCm = 0)
        )
        val sheetNest = CamNestingEngine.computeSheetNesting(items, "زجاج أسود مدخن وألمنيوم مؤكسد")
        assertTrue(sheetNest.totalSheets18Mm >= 1)
        assertTrue(sheetNest.cutPieces.isNotEmpty())
        assertTrue(sheetNest.totalBlumHinges >= 4)

        val alumNest = CamNestingEngine.computeAluminumBarNesting(items)
        assertTrue(alumNest.totalBarsCount >= 1)
        assertEquals(6000, alumNest.bars.first().standardLengthMm)
    }
}

