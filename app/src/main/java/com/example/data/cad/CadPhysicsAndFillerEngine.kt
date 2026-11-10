package com.example.data.cad

import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ElevationLayer {
    FLOOR_BASE,
    UPPER_WALL
}

data class CadMoveResult(
    val updatedItem: PlacedKitchenItem,
    val snappedToWall: Boolean,
    val snappedToNeighbor: Boolean,
    val collisionPrevented: Boolean,
    val distanceLeftWallMm: Int,
    val distanceRightWallMm: Int,
    val distanceNorthWallMm: Int,
    val safetyClearanceMm: Int,
    val telemetryMessageAr: String
)

enum class FillerPlacementMode(val titleAr: String) {
    RIGHT_END("فيلر يمين الحائط"),
    LEFT_END("فيلر يسار الحائط"),
    SPLIT_EQUAL_ENDS("فيلر متساوٍ يمين ويسار"),
    BETWEEN_UNITS("فواصل متساوية بين الخزائن")
}

data class FillerCalculationReport(
    val wallLengthMm: Int,
    val occupiedBaseMm: Int,
    val remainingGapBaseMm: Int,
    val occupiedWallMm: Int,
    val remainingGapWallMm: Int,
    val summaryAr: String
)

object CadPhysicsEngine {
    const val MIN_UPPER_SAFETY_CLEARANCE_MM = 600 // 60 cm minimum clearance above 850mm countertop
    const val DEFAULT_COUNTERTOP_HEIGHT_MM = 850
    const val MIN_WALL_CABINET_ELEVATION_MM = DEFAULT_COUNTERTOP_HEIGHT_MM + MIN_UPPER_SAFETY_CLEARANCE_MM // 1450 mm

    fun layerOf(item: PlacedKitchenItem): ElevationLayer {
        return if (item.type.category == ModuleCategory.WALL_CABINET || item.customElevationMm >= 1200) {
            ElevationLayer.UPPER_WALL
        } else {
            ElevationLayer.FLOOR_BASE
        }
    }

    /**
     * Moves an item with 1mm precision, magnetic wall/neighbor snapping, and strict anti-overlap collision solver.
     */
    fun solveMoveWithSnappingAndAntiOverlap(
        targetItem: PlacedKitchenItem,
        desiredXMm: Int,
        desiredYMm: Int,
        allItems: List<PlacedKitchenItem>,
        roomWidthMm: Int,
        roomDepthMm: Int,
        enableMagneticSnap: Boolean = true,
        snapThresholdMm: Int = 55
    ): CadMoveResult {
        val itemW = targetItem.effectiveWidthMm
        val itemD = targetItem.effectiveDepthMm
        val maxX = (roomWidthMm - itemW).coerceAtLeast(0)
        val maxY = (roomDepthMm - itemD).coerceAtLeast(0)

        var candidateX = desiredXMm.coerceIn(0, maxX)
        var candidateY = desiredYMm.coerceIn(0, maxY)
        var snappedWall = false
        var snappedNeighbor = false

        val sameLayerOthers = allItems.filter {
            it.uid != targetItem.uid && layerOf(it) == layerOf(targetItem)
        }

        if (enableMagneticSnap) {
            // 1. Magnetic Snap to Room Walls (North y=0, West x=0, East x=maxX, South y=maxY)
            if (candidateX <= snapThresholdMm) {
                candidateX = 0
                snappedWall = true
            } else if (abs(candidateX - maxX) <= snapThresholdMm) {
                candidateX = maxX
                snappedWall = true
            }

            if (candidateY <= snapThresholdMm) {
                candidateY = 0
                snappedWall = true
            } else if (abs(candidateY - maxY) <= snapThresholdMm) {
                candidateY = maxY
                snappedWall = true
            }

            // 2. Magnetic Snap to Adjacent Units in the Same Layer
            sameLayerOthers.forEach { other ->
                val otherLeft = other.xMm
                val otherRight = other.xMm + other.effectiveWidthMm
                val otherTop = other.yMm
                val otherBottom = other.yMm + other.effectiveDepthMm

                val yAligned = abs(candidateY - otherTop) <= snapThresholdMm * 2
                if (yAligned) {
                    // Snap left edge of target to right edge of neighbor
                    if (abs(candidateX - otherRight) <= snapThresholdMm) {
                        candidateX = otherRight.coerceIn(0, maxX)
                        candidateY = otherTop.coerceIn(0, maxY)
                        snappedNeighbor = true
                    }
                    // Snap right edge of target to left edge of neighbor
                    else if (abs((candidateX + itemW) - otherLeft) <= snapThresholdMm) {
                        candidateX = (otherLeft - itemW).coerceIn(0, maxX)
                        candidateY = otherTop.coerceIn(0, maxY)
                        snappedNeighbor = true
                    }
                }

                val xAligned = abs(candidateX - otherLeft) <= snapThresholdMm * 2
                if (xAligned) {
                    if (abs(candidateY - otherBottom) <= snapThresholdMm) {
                        candidateY = otherBottom.coerceIn(0, maxY)
                        candidateX = otherLeft.coerceIn(0, maxX)
                        snappedNeighbor = true
                    } else if (abs((candidateY + itemD) - otherTop) <= snapThresholdMm) {
                        candidateY = (otherTop - itemD).coerceIn(0, maxY)
                        candidateX = otherLeft.coerceIn(0, maxX)
                        snappedNeighbor = true
                    }
                }
            }
        }

        // 3. Strict Anti-Overlap Collision Resolution
        var collisionPrevented = false
        for (pass in 0 until 4) {
            val overlapping = sameLayerOthers.firstOrNull { other ->
                rectanglesOverlapMm(
                    ax1 = candidateX,
                    ay1 = candidateY,
                    ax2 = candidateX + itemW,
                    ay2 = candidateY + itemD,
                    bx1 = other.xMm,
                    by1 = other.yMm,
                    bx2 = other.xMm + other.effectiveWidthMm,
                    by2 = other.yMm + other.effectiveDepthMm
                )
            } ?: break

            collisionPrevented = true
            val pushRightX = overlapping.xMm + overlapping.effectiveWidthMm
            val pushLeftX = overlapping.xMm - itemW
            val pushDownY = overlapping.yMm + overlapping.effectiveDepthMm
            val pushUpY = overlapping.yMm - itemD

            val options = mutableListOf<Pair<Int, Int>>()
            if (pushRightX in 0..maxX) options.add(pushRightX to candidateY)
            if (pushLeftX in 0..maxX) options.add(pushLeftX to candidateY)
            if (pushDownY in 0..maxY) options.add(candidateX to pushDownY)
            if (pushUpY in 0..maxY) options.add(candidateX to pushUpY)

            val best = options
                .filter { (ox, oy) ->
                    sameLayerOthers.none { other ->
                        rectanglesOverlapMm(
                            ox, oy, ox + itemW, oy + itemD,
                            other.xMm, other.yMm, other.xMm + other.effectiveWidthMm, other.yMm + other.effectiveDepthMm
                        )
                    }
                }
                .minByOrNull { (ox, oy) ->
                    abs(ox - desiredXMm) + abs(oy - desiredYMm)
                }

            if (best != null) {
                candidateX = best.first
                candidateY = best.second
                break
            } else {
                // Fallback to original position of targetItem so overlap never occurs
                candidateX = targetItem.xMm.coerceIn(0, maxX)
                candidateY = targetItem.yMm.coerceIn(0, maxY)
                break
            }
        }

        // Enforce 600mm vertical safety clearance for upper wall cabinets
        val enforcedElevationMm = if (targetItem.type.category == ModuleCategory.WALL_CABINET) {
            max(targetItem.customElevationMm, MIN_WALL_CABINET_ELEVATION_MM)
        } else {
            targetItem.customElevationMm
        }
        val safetyClearance = if (targetItem.type.category == ModuleCategory.WALL_CABINET) {
            (enforcedElevationMm - DEFAULT_COUNTERTOP_HEIGHT_MM).coerceAtLeast(MIN_UPPER_SAFETY_CLEARANCE_MM)
        } else {
            MIN_UPPER_SAFETY_CLEARANCE_MM
        }

        val updated = targetItem
            .withPositionMm(candidateX, candidateY)
            .copy(customElevationMm = enforcedElevationMm)

        val distLeft = candidateX
        val distRight = (roomWidthMm - (candidateX + itemW)).coerceAtLeast(0)
        val distNorth = candidateY

        val statusParts = buildList {
            add("X=${candidateX}mm, Y=${candidateY}mm")
            if (snappedNeighbor) add("التصاق مغناطيسي بالوحدة المجاورة")
            else if (snappedWall) add("محاذاة مغناطيسية مع الجدار")
            if (collisionPrevented) add("منع تداخل فيزيائي تلقائي")
            if (targetItem.type.category == ModuleCategory.WALL_CABINET) {
                add("خلوص أمان علوي ${safetyClearance}mm")
            }
        }

        return CadMoveResult(
            updatedItem = updated,
            snappedToWall = snappedWall,
            snappedToNeighbor = snappedNeighbor,
            collisionPrevented = collisionPrevented,
            distanceLeftWallMm = distLeft,
            distanceRightWallMm = distRight,
            distanceNorthWallMm = distNorth,
            safetyClearanceMm = safetyClearance,
            telemetryMessageAr = statusParts.joinToString(" • ")
        )
    }

    /**
     * Vertical Pairing & 600mm Safety Clearance:
     * Aligns existing upper wall cabinets above base cabinets and generates paired upper cabinets
     * for base cabinets along the North wall if needed, enforcing >= 600mm clearance above the 850mm countertop.
     */
    fun pairAndAlignUpperWallCabinets(
        items: List<PlacedKitchenItem>,
        roomWidthMm: Int,
        safetyClearanceMm: Int = MIN_UPPER_SAFETY_CLEARANCE_MM
    ): List<PlacedKitchenItem> {
        val safeClearance = safetyClearanceMm.coerceAtLeast(MIN_UPPER_SAFETY_CLEARANCE_MM)
        val targetElevationMm = DEFAULT_COUNTERTOP_HEIGHT_MM + safeClearance

        val baseCabinetsOnNorthWall = items.filter {
            (it.type.category == ModuleCategory.BASE_CABINET || it.type.category == ModuleCategory.SINK_WATER) &&
                it.yMm <= 150 &&
                it.type != ModuleType.FILLER_STRIP
        }.sortedBy { it.xMm }

        val updatedExisting = items.map { item ->
            if (item.type.category == ModuleCategory.WALL_CABINET) {
                val nearestBase = baseCabinetsOnNorthWall.minByOrNull { abs(it.xMm - item.xMm) }
                if (nearestBase != null && abs(nearestBase.xMm - item.xMm) <= 250) {
                    item.withPositionMm(nearestBase.xMm, 0)
                        .withDimensionsMm(nearestBase.widthMm, 350)
                        .copy(
                            customElevationMm = targetElevationMm,
                            pairedWithBaseUid = nearestBase.uid
                        )
                } else {
                    item.copy(customElevationMm = max(item.customElevationMm, targetElevationMm))
                }
            } else {
                item
            }
        }.toMutableList()

        // If any North-wall base cabinet does not have a paired upper cabinet above it, pair one automatically
        baseCabinetsOnNorthWall.forEach { base ->
            val hasUpper = updatedExisting.any {
                it.type.category == ModuleCategory.WALL_CABINET &&
                    abs(it.xMm - base.xMm) < (base.widthMm / 2)
            }
            if (!hasUpper && base.xMm + base.effectiveWidthMm <= roomWidthMm) {
                val wallType = if (base.widthMm >= 750) ModuleType.WALL_GLASS_80 else ModuleType.WALL_LIFT_60
                val newWallUnit = PlacedKitchenItem(
                    uid = "wall_pair_${UUID.randomUUID().toString().take(6)}",
                    type = wallType,
                    xCm = base.xCm,
                    yCm = 0,
                    widthCm = base.widthCm,
                    depthCm = 35,
                    rotationDeg = 0,
                    customLabel = "${wallType.titleAr} (مرتبط رأسياً)",
                    priceSar = wallType.defaultPriceSar,
                    fineXMmOffset = base.fineXMmOffset,
                    fineYMmOffset = 0,
                    fineWidthMmOffset = base.fineWidthMmOffset,
                    customElevationMm = targetElevationMm,
                    pairedWithBaseUid = base.uid
                )
                updatedExisting.add(newWallUnit)
            }
        }

        return updatedExisting
    }

    private fun rectanglesOverlapMm(
        ax1: Int,
        ay1: Int,
        ax2: Int,
        ay2: Int,
        bx1: Int,
        by1: Int,
        bx2: Int,
        by2: Int
    ): Boolean {
        val overlapW = min(ax2, bx2) - max(ax1, bx1)
        val overlapH = min(ay2, by2) - max(ay1, by1)
        return overlapW > 2 && overlapH > 2
    }
}

object AutoFillerEngine {

    fun analyzeWallGaps(
        items: List<PlacedKitchenItem>,
        targetWallLengthMm: Int
    ): FillerCalculationReport {
        val northWallBaseUnits = items.filter {
            it.yMm <= 120 &&
                it.rotationDeg == 0 &&
                it.type.category != ModuleCategory.WALL_CABINET &&
                it.type.category != ModuleCategory.ISLAND_UNIT &&
                it.type != ModuleType.FILLER_STRIP
        }
        val northWallUpperUnits = items.filter {
            it.yMm <= 120 &&
                it.rotationDeg == 0 &&
                it.type.category == ModuleCategory.WALL_CABINET &&
                it.type != ModuleType.FILLER_STRIP
        }

        val occupiedBaseMm = northWallBaseUnits.sumOf { it.effectiveWidthMm }
        val remainingBaseMm = (targetWallLengthMm - occupiedBaseMm).coerceAtLeast(0)

        val occupiedWallMm = northWallUpperUnits.sumOf { it.effectiveWidthMm }
        val remainingWallMm = (targetWallLengthMm - occupiedWallMm).coerceAtLeast(0)

        val summary = "طول الحائط: ${targetWallLengthMm} ملم | إشغال السفلي: ${occupiedBaseMm} ملم (الفراغ المتبقي: ${remainingBaseMm} ملم) | إشغال العلوي: ${occupiedWallMm} ملم"
        return FillerCalculationReport(
            wallLengthMm = targetWallLengthMm,
            occupiedBaseMm = occupiedBaseMm,
            remainingGapBaseMm = remainingBaseMm,
            occupiedWallMm = occupiedWallMm,
            remainingGapWallMm = remainingWallMm,
            summaryAr = summary
        )
    }

    /**
     * Packs North-wall units flush and inserts 1mm-precision Filler Strips (FILLER_STRIP)
     * according to the chosen FillerPlacementMode.
     */
    fun generateAutoFillersForWall(
        items: List<PlacedKitchenItem>,
        targetWallLengthMm: Int,
        mode: FillerPlacementMode,
        includeUpperWallFillers: Boolean = true
    ): Pair<List<PlacedKitchenItem>, String> {
        val nonNorthOrFillersRemoved = items.filterNot {
            it.type == ModuleType.FILLER_STRIP && it.yMm <= 120
        }

        val northBase = nonNorthOrFillersRemoved.filter {
            it.yMm <= 120 &&
                it.rotationDeg == 0 &&
                it.type.category != ModuleCategory.WALL_CABINET &&
                it.type.category != ModuleCategory.ISLAND_UNIT
        }.sortedBy { it.xMm }

        val otherItems = nonNorthOrFillersRemoved.filterNot { northBase.contains(it) }.toMutableList()
        if (northBase.isEmpty()) {
            return items to "لا توجد وحدات على الحائط الرئيسي لتوليد شرائح الفيلر"
        }

        val occupiedBaseMm = northBase.sumOf { it.effectiveWidthMm }
        val gapMm = (targetWallLengthMm - occupiedBaseMm).coerceAtLeast(0)
        if (gapMm <= 0) {
            return items to "الحائط ممتلئ بالكامل (0 ملم فراغ متبقٍ)"
        }

        val arrangedBase = mutableListOf<PlacedKitchenItem>()
        val generatedFillers = mutableListOf<PlacedKitchenItem>()

        when (mode) {
            FillerPlacementMode.RIGHT_END -> {
                var cursorMm = 0
                northBase.forEach { unit ->
                    arrangedBase.add(unit.withPositionMm(cursorMm, 0))
                    cursorMm += unit.effectiveWidthMm
                }
                generatedFillers.add(
                    createFillerItem(
                        xMm = cursorMm,
                        widthMm = gapMm,
                        isUpperWall = false,
                        label = "شريحة فيلر يمين (${gapMm} ملم)"
                    )
                )
            }

            FillerPlacementMode.LEFT_END -> {
                generatedFillers.add(
                    createFillerItem(
                        xMm = 0,
                        widthMm = gapMm,
                        isUpperWall = false,
                        label = "شريحة فيلر يسار (${gapMm} ملم)"
                    )
                )
                var cursorMm = gapMm
                northBase.forEach { unit ->
                    arrangedBase.add(unit.withPositionMm(cursorMm, 0))
                    cursorMm += unit.effectiveWidthMm
                }
            }

            FillerPlacementMode.SPLIT_EQUAL_ENDS -> {
                val leftMm = gapMm / 2
                val rightMm = gapMm - leftMm
                if (leftMm > 0) {
                    generatedFillers.add(
                        createFillerItem(
                            xMm = 0,
                            widthMm = leftMm,
                            isUpperWall = false,
                            label = "فيلر يسار (${leftMm} ملم)"
                        )
                    )
                }
                var cursorMm = leftMm
                northBase.forEach { unit ->
                    arrangedBase.add(unit.withPositionMm(cursorMm, 0))
                    cursorMm += unit.effectiveWidthMm
                }
                if (rightMm > 0) {
                    generatedFillers.add(
                        createFillerItem(
                            xMm = cursorMm,
                            widthMm = rightMm,
                            isUpperWall = false,
                            label = "فيلر يمين (${rightMm} ملم)"
                        )
                    )
                }
            }

            FillerPlacementMode.BETWEEN_UNITS -> {
                val slots = (northBase.size - 1).coerceAtLeast(1)
                val stepGapMm = gapMm / slots
                var remainderMm = gapMm % slots
                var cursorMm = 0
                northBase.forEachIndexed { idx, unit ->
                    arrangedBase.add(unit.withPositionMm(cursorMm, 0))
                    cursorMm += unit.effectiveWidthMm
                    if (idx < northBase.lastIndex || northBase.size == 1) {
                        val thisFillerMm = stepGapMm + if (remainderMm > 0) {
                            remainderMm--
                            1
                        } else {
                            0
                        }
                        if (thisFillerMm > 0) {
                            generatedFillers.add(
                                createFillerItem(
                                    xMm = cursorMm,
                                    widthMm = thisFillerMm,
                                    isUpperWall = false,
                                    label = "فاصل فيلر ${idx + 1} (${thisFillerMm} ملم)"
                                )
                            )
                            cursorMm += thisFillerMm
                        }
                    }
                }
            }
        }

        if (includeUpperWallFillers && generatedFillers.isNotEmpty()) {
            generatedFillers.toList().forEach { baseFiller ->
                generatedFillers.add(
                    createFillerItem(
                        xMm = baseFiller.xMm,
                        widthMm = baseFiller.widthMm,
                        isUpperWall = true,
                        label = "فيلر علوي (${baseFiller.widthMm} ملم)"
                    )
                )
            }
        }

        val finalItems = arrangedBase + otherItems + generatedFillers
        val msg = "تم توليد ${generatedFillers.size} شريحة فيلر بدقة 1 ملم لتعبئة فراغ ${gapMm} ملم (${mode.titleAr})"
        return finalItems to msg
    }

    private fun createFillerItem(
        xMm: Int,
        widthMm: Int,
        isUpperWall: Boolean,
        label: String
    ): PlacedKitchenItem {
        val safeW = widthMm.coerceAtLeast(10)
        val depthMm = if (isUpperWall) 350 else 600
        val heightMm = if (isUpperWall) 750 else 850
        val elevationMm = if (isUpperWall) CadPhysicsEngine.MIN_WALL_CABINET_ELEVATION_MM else 0
        val price = (180 + (safeW * 0.65f)).roundToInt()
        return PlacedKitchenItem(
            uid = "filler_${UUID.randomUUID().toString().take(6)}",
            type = ModuleType.FILLER_STRIP,
            xCm = xMm / 10,
            yCm = 0,
            widthCm = (safeW / 10).coerceAtLeast(1),
            depthCm = depthMm / 10,
            rotationDeg = 0,
            customLabel = label,
            priceSar = price,
            fineXMmOffset = xMm % 10,
            fineYMmOffset = 0,
            fineWidthMmOffset = safeW % 10,
            fineDepthMmOffset = 0,
            customHeightMm = heightMm,
            customElevationMm = elevationMm
        )
    }
}
