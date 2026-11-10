package com.example.data.cad

import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 1. Measurement Unit System (Metric mm/cm/m vs Imperial in/ft/LF)
 */
enum class MeasurementUnitSystem(
    val code: String,
    val titleAr: String,
    val shortUnitAr: String,
    val badgeAr: String
) {
    METRIC("METRIC", "النظام المتري (ملم / سم)", "ملم", "ملم / سم (Metric)"),
    IMPERIAL("IMPERIAL", "النظام الإمبراطوري (بوصة / قدم)", "بوصة", "بوصة / قدم (Imperial)");

    fun formatMillimeters(mm: Int): String {
        return if (this == METRIC) {
            "$mm ملم"
        } else {
            val totalInches = mm / 25.4
            val roundedTenths = (totalInches * 10.0).roundToInt() / 10.0
            "${roundedTenths}\""
        }
    }

    fun formatMillimetersVerboseAr(mm: Int): String {
        return if (this == METRIC) {
            "$mm ملم (${String.format(Locale.US, "%.1f", mm / 10f)} سم)"
        } else {
            val totalInches = mm / 25.4
            val feet = (totalInches / 12.0).toInt()
            val remInches = totalInches - (feet * 12.0)
            if (feet > 0) {
                "${feet}' ${String.format(Locale.US, "%.1f", remInches)}\" (${String.format(Locale.US, "%.1f", totalInches)} بوصة)"
            } else {
                "${String.format(Locale.US, "%.1f", totalInches)}\" بوصة"
            }
        }
    }

    fun formatDimensionsBox(widthMm: Int, depthMm: Int, heightMm: Int? = null): String {
        return if (this == METRIC) {
            if (heightMm != null) {
                "${widthMm}×${depthMm}×${heightMm} ملم"
            } else {
                "${widthMm}×${depthMm} ملم"
            }
        } else {
            val wIn = String.format(Locale.US, "%.1f\"", widthMm / 25.4)
            val dIn = String.format(Locale.US, "%.1f\"", depthMm / 25.4)
            if (heightMm != null) {
                val hIn = String.format(Locale.US, "%.1f\"", heightMm / 25.4)
                "$wIn×$dIn×$hIn"
            } else {
                "$wIn×$dIn"
            }
        }
    }

    fun formatLinearCountertop(linearMeters: Double): String = formatLinearCountertop(linearMeters.toFloat())

    fun formatLinearCountertop(linearMeters: Float): String {
        return if (this == METRIC) {
            "${String.format(Locale.US, "%.2f", linearMeters)} م.ط"
        } else {
            val linearFeet = linearMeters * 3.28084f
            "${String.format(Locale.US, "%.2f", linearFeet)} قدم طولي (LF)"
        }
    }
}

/**
 * 2. 3D Natural & Artificial Lighting Simulation Engine Configuration
 */
enum class NaturalDaylightPreset(
    val titleAr: String,
    val sunAzimuthDeg: Float,
    val naturalIntensity: Float,
    val artificialIntensity: Float,
    val colorTempKelvin: Int,
    val shadowIntensity: Float,
    val specularReflection: Float,
    val underCabinetLedIntensity: Float = 0.85f
) {
    MORNING_SUN("شروق طبيعي ناعم", 55f, 0.82f, 0.35f, 4800, 0.50f, 0.55f, 0.65f),
    NOON_BRIGHT("إضاءة ظهيرة ساطعة", 135f, 1.00f, 0.25f, 5600, 0.72f, 0.75f, 0.55f),
    GOLDEN_SUNSET("غروب ذهبي دافئ", 240f, 0.68f, 0.65f, 3000, 0.80f, 0.68f, 0.88f),
    NIGHT_SPOT_LED("إضاءة ليلية اصطناعية LED", 180f, 0.12f, 0.98f, 3000, 0.62f, 0.88f, 1.00f);

    val artificialSpotIntensity: Float get() = artificialIntensity
    val kelvinTemp: Int get() = colorTempKelvin
    val shadowDepth: Float get() = shadowIntensity
    val specularGloss: Float get() = specularReflection
}

data class LightingSimulationConfig(
    val preset: NaturalDaylightPreset = NaturalDaylightPreset.NOON_BRIGHT,
    val sunAzimuthDeg: Float = 135f,
    val naturalLightIntensity: Float = 0.90f,
    val artificialSpotIntensity: Float = 0.65f,
    val underCabinetLedIntensity: Float = 0.85f,
    val colorTemperatureKelvin: Int = 4200,
    val shadowSoftnessAndDepth: Float = 0.70f,
    val surfaceSpecularReflection: Float = 0.75f
) {
    val activePreset: NaturalDaylightPreset get() = preset

    companion object {
        fun fromPreset(preset: NaturalDaylightPreset): LightingSimulationConfig = LightingSimulationConfig(
            preset = preset,
            sunAzimuthDeg = preset.sunAzimuthDeg,
            naturalLightIntensity = preset.naturalIntensity,
            artificialSpotIntensity = preset.artificialIntensity,
            underCabinetLedIntensity = preset.underCabinetLedIntensity,
            colorTemperatureKelvin = preset.colorTempKelvin,
            shadowSoftnessAndDepth = preset.shadowIntensity,
            surfaceSpecularReflection = preset.specularReflection
        )
    }
}

/**
 * 3. Real-Time Engineering Installation Compliance & Clearance Validator
 */
enum class ComplianceSeverity(val labelAr: String) {
    CRITICAL("تعارض حرج"),
    WARNING("تنبيه هندسي")
}

enum class ComplianceRuleCategory {
    UNIT_INTERSECTION,
    DOOR_SWING_CLEARANCE,
    APPLIANCE_SEPARATION,
    CORNER_CABINET_SWING,
    UPPER_CABINET_CLEARANCE,
    ROOM_BOUNDARY
}

data class EngineeringComplianceAlert(
    val id: String,
    val severity: ComplianceSeverity,
    val titleAr: String,
    val detailsAr: String,
    val affectedUnitUid: String?,
    val secondaryUnitUid: String? = null,
    val requiredClearanceMm: Int,
    val actualClearanceMm: Int,
    val affectedUnitTitleAr: String = "وحدة مطبخ",
    val recommendationAr: String = "اضغط «إصلاح هندسي تلقائي» لضبط المسافات وفق معايير التركيب الهندسية.",
    val category: ComplianceRuleCategory = ComplianceRuleCategory.DOOR_SWING_CLEARANCE
) {
    val isCritical: Boolean get() = severity == ComplianceSeverity.CRITICAL
    val detailAr: String get() = detailsAr
    val measuredClearanceMm: Int get() = actualClearanceMm
}

data class EngineeringComplianceReport(
    val alerts: List<EngineeringComplianceAlert>,
    val complianceScorePercent: Int,
    val isFullyCompliant: Boolean
) {
    val complianceScore: Int get() = complianceScorePercent

    /**
     * All unit UIDs (both primary affected and secondary colliding/violating units) that violate
     * intersection or minimum distance rules and must be highlighted with a red bounding box in real time.
     */
    val violatingUnitUids: Set<String>
        get() = alerts.flatMap { listOfNotNull(it.affectedUnitUid, it.secondaryUnitUid) }.toSet()

    val statusSummaryAr: String
        get() = if (alerts.isEmpty()) {
            "التصميم مطابق 100% لمعايير التركيب ومساحات فتح الأبواب"
        } else {
            "تم رصد ${alerts.size} تنبيه هندسي (${criticalCount} حرج • ${warningCount} تنبيه)"
        }

    val criticalCount: Int
        get() = alerts.count { it.severity == ComplianceSeverity.CRITICAL }

    val warningCount: Int
        get() = alerts.count { it.severity == ComplianceSeverity.WARNING }
}

object EngineeringComplianceAnalyzer {

    private const val MIN_APPLIANCE_DOOR_SWING_CLEARANCE_MM = 850
    private const val MIN_CABINET_WALKWAY_CLEARANCE_MM = 750
    private const val MIN_STOVE_TO_SINK_OR_FRIDGE_MM = 300
    private const val MIN_UPPER_WALL_CLEARANCE_MM = 600

    fun validateLayout(
        items: List<PlacedKitchenItem>,
        roomWidthMm: Int,
        roomDepthMm: Int
    ): EngineeringComplianceReport {
        val alerts = mutableListOf<EngineeringComplianceAlert>()

        val floorUnits = items.filter { it.type.category != ModuleCategory.WALL_CABINET && it.type != ModuleType.FILLER_STRIP }
        val wallUnits = items.filter { it.type.category == ModuleCategory.WALL_CABINET }

        // Check 0: Direct Physical Intersection / Footprint Overlap Between Same-Tier Units
        fun checkTierOverlaps(tierUnits: List<PlacedKitchenItem>, tierLabelAr: String) {
            for (i in tierUnits.indices) {
                val u1 = tierUnits[i]
                for (j in i + 1 until tierUnits.size) {
                    val u2 = tierUnits[j]
                    val overlapX = min(u1.xMm + u1.effectiveWidthMm, u2.xMm + u2.effectiveWidthMm) - max(u1.xMm, u2.xMm)
                    val overlapY = min(u1.yMm + u1.effectiveDepthMm, u2.yMm + u2.effectiveDepthMm) - max(u1.yMm, u2.yMm)
                    // Require > 2mm overlap so flush side-by-side touching cabinets (0mm gap) do not trigger false collision
                    if (overlapX > 2 && overlapY > 2) {
                        alerts.add(
                            EngineeringComplianceAlert(
                                id = "intersect_${u1.uid}_${u2.uid}",
                                severity = ComplianceSeverity.CRITICAL,
                                titleAr = "تقاطع وتداخل مباشر بين «${u1.displayTitle}» و«${u2.displayTitle}»",
                                detailsAr = "تتداخل الوحدتان ($tierLabelAr) بمقدار ${overlapX}×${overlapY} ملم؛ يُمنع تقاطع الخزائن ويجب فصلها أو محاذاتها جنباً إلى جنب.",
                                affectedUnitUid = u1.uid,
                                secondaryUnitUid = u2.uid,
                                requiredClearanceMm = 0,
                                actualClearanceMm = -max(overlapX, overlapY),
                                affectedUnitTitleAr = u1.displayTitle,
                                recommendationAr = "اسحب إحدى الوحدتين المتقاطعتين بعيداً عن المربع الأحمر أو اضغط «إصلاح هندسي تلقائي».",
                                category = ComplianceRuleCategory.UNIT_INTERSECTION
                            )
                        )
                    }
                }
            }
        }
        checkTierOverlaps(floorUnits, "وحدات أرضية")
        checkTierOverlaps(wallUnits, "خزائن علوية")

        // Check 1: Door Swing & Front Walkway Clearance for Appliances and Cabinets
        floorUnits.forEach { unit ->
            val requiredSwingMm = when (unit.type) {
                ModuleType.DISHWASHER,
                ModuleType.STOVE_HOB,
                ModuleType.TALL_OVEN_TOWER,
                ModuleType.FRIDGE_SMART -> MIN_APPLIANCE_DOOR_SWING_CLEARANCE_MM
                else -> MIN_CABINET_WALKWAY_CLEARANCE_MM
            }

            // Front swing zone extends in front of the unit (towards room interior)
            val frontEdgeYMm = unit.yMm + unit.effectiveDepthMm
            val roomFrontClearanceMm = (roomDepthMm - frontEdgeYMm).coerceAtLeast(0)

            // Check if any opposing unit (like an Island) blocks the door opening arc in front of this unit
            val opposingBlocker = floorUnits.firstOrNull { other ->
                other.uid != unit.uid &&
                    other.yMm >= frontEdgeYMm - 20 &&
                    rangesOverlap(
                        unit.xMm,
                        unit.xMm + unit.effectiveWidthMm,
                        other.xMm,
                        other.xMm + other.effectiveWidthMm
                    ) &&
                    (other.yMm - frontEdgeYMm) < requiredSwingMm
            }

            if (opposingBlocker != null) {
                val actualGapMm = (opposingBlocker.yMm - frontEdgeYMm).coerceAtLeast(0)
                alerts.add(
                    EngineeringComplianceAlert(
                        id = "swing_block_${unit.uid}_${opposingBlocker.uid}",
                        severity = ComplianceSeverity.CRITICAL,
                        titleAr = "عدم كفاية المساحة لفتح باب «${unit.displayTitle}»",
                        detailsAr = "المسافة الفاصلة أمام «${unit.displayTitle}» و«${opposingBlocker.displayTitle}» هي $actualGapMm ملم فقط، بينما يتطلب فتح الباب والممر الهندسي $requiredSwingMm ملم على الأقل.",
                        affectedUnitUid = unit.uid,
                        secondaryUnitUid = opposingBlocker.uid,
                        requiredClearanceMm = requiredSwingMm,
                        actualClearanceMm = actualGapMm,
                        affectedUnitTitleAr = unit.displayTitle,
                        recommendationAr = "أزح الجزيرة أو الوحدة المقابلة لتوفير خلوص فتح باب لا يقل عن $requiredSwingMm ملم.",
                        category = ComplianceRuleCategory.DOOR_SWING_CLEARANCE
                    )
                )
            } else if (unit.yMm > 0 && roomFrontClearanceMm < 500) {
                alerts.add(
                    EngineeringComplianceAlert(
                        id = "wall_front_tight_${unit.uid}",
                        severity = ComplianceSeverity.WARNING,
                        titleAr = "ضيق المساحة الأمامية لفتح «${unit.displayTitle}»",
                        detailsAr = "المسافة المتبقية حتى الحائط الأمامي هي $roomFrontClearanceMm ملم؛ يُنصح بترك $requiredSwingMm ملم لفتح الأبواب والأدراج بحرية.",
                        affectedUnitUid = unit.uid,
                        requiredClearanceMm = requiredSwingMm,
                        actualClearanceMm = roomFrontClearanceMm,
                        affectedUnitTitleAr = unit.displayTitle,
                        recommendationAr = "أعد ضبط موقع الوحدة لتوفير ممر أمامي لا يقل عن $requiredSwingMm ملم.",
                        category = ComplianceRuleCategory.DOOR_SWING_CLEARANCE
                    )
                )
            }
        }

        // Check 2: Thermal & Splash Safety Separation between Stove/Hob and Fridge / Sink
        val stoves = floorUnits.filter { it.type == ModuleType.STOVE_HOB || it.type == ModuleType.TALL_OVEN_TOWER }
        val fridgesAndSinks = floorUnits.filter { it.type == ModuleType.FRIDGE_SMART || it.type == ModuleType.SINK_DOUBLE }

        stoves.forEach { stove ->
            fridgesAndSinks.forEach { target ->
                val sameWallRow = abs(stove.yMm - target.yMm) < 250
                if (sameWallRow) {
                    val horizontalGapMm = when {
                        stove.xMm + stove.effectiveWidthMm <= target.xMm ->
                            target.xMm - (stove.xMm + stove.effectiveWidthMm)
                        target.xMm + target.effectiveWidthMm <= stove.xMm ->
                            stove.xMm - (target.xMm + target.effectiveWidthMm)
                        else -> 0
                    }
                    if (horizontalGapMm < MIN_STOVE_TO_SINK_OR_FRIDGE_MM) {
                        val reason = if (target.type == ModuleType.FRIDGE_SMART) {
                            "العزل الحراري وحماية ضاغط الثلاجة وفتح بابها بزاوية كاملة"
                        } else {
                            "منع تطاير المياه على الشعلات وتوفير سطح تحضير آمن"
                        }
                        alerts.add(
                            EngineeringComplianceAlert(
                                id = "thermal_gap_${stove.uid}_${target.uid}",
                                severity = ComplianceSeverity.WARNING,
                                titleAr = "تلاصق «${stove.displayTitle}» مع «${target.displayTitle}» بدون فاصل أمان",
                                detailsAr = "المسافة الحالية بينهما $horizontalGapMm ملم؛ تشترط معايير NKBA ترك $MIN_STOVE_TO_SINK_OR_FRIDGE_MM ملم على الأقل لضمان $reason.",
                                affectedUnitUid = stove.uid,
                                secondaryUnitUid = target.uid,
                                requiredClearanceMm = MIN_STOVE_TO_SINK_OR_FRIDGE_MM,
                                actualClearanceMm = horizontalGapMm,
                                affectedUnitTitleAr = stove.displayTitle,
                                recommendationAr = "اترك مسافة فاصلة أو خزانة تحضير بعرض 300 ملم على الأقل بين الموقد والثلاجة/الحوض.",
                                category = ComplianceRuleCategory.APPLIANCE_SEPARATION
                            )
                        )
                    }
                }
            }
        }

        // Check 3: Corner Cabinet Door Swing Interference
        val corners = floorUnits.filter { it.type == ModuleType.CORNER_MAGIC }
        corners.forEach { corner ->
            val interfering = floorUnits.firstOrNull { other ->
                other.uid != corner.uid &&
                    other.yMm in (corner.yMm + 10)..(corner.yMm + corner.effectiveDepthMm + 350) &&
                    other.xMm < corner.xMm + corner.effectiveWidthMm + 250 &&
                    other.xMm + other.effectiveWidthMm > corner.xMm
            }
            if (interfering != null) {
                alerts.add(
                    EngineeringComplianceAlert(
                        id = "corner_swing_${corner.uid}_${interfering.uid}",
                        severity = ComplianceSeverity.CRITICAL,
                        titleAr = "إعاقة فتح سلة الزاوية «${corner.displayTitle}»",
                        detailsAr = "الوحدة «${interfering.displayTitle}» تقع في مسار فتح باب الزاوية الدوار؛ يلزم إزاحتها لتوفير خلوص لا يقل عن 450 ملم.",
                        affectedUnitUid = corner.uid,
                        secondaryUnitUid = interfering.uid,
                        requiredClearanceMm = 450,
                        actualClearanceMm = 120,
                        affectedUnitTitleAr = corner.displayTitle,
                        recommendationAr = "أزح الوحدة المجاورة للزاوية لتوفير مسار فتح حر لسلة الزاوية الدوارة.",
                        category = ComplianceRuleCategory.CORNER_CABINET_SWING
                    )
                )
            }
        }

        // Check 4: Upper Wall Cabinet Vertical Safety Clearance (>= 600mm above base countertop)
        wallUnits.forEach { wallUnit ->
            val baseBelow = floorUnits.firstOrNull { base ->
                base.type.category != ModuleCategory.TALL_UNIT &&
                    rangesOverlap(
                        wallUnit.xMm,
                        wallUnit.xMm + wallUnit.effectiveWidthMm,
                        base.xMm,
                        base.xMm + base.effectiveWidthMm
                    )
            }
            val baseTopMm = baseBelow?.heightMm ?: 850
            val verticalGapMm = wallUnit.elevationMm - baseTopMm
            if (verticalGapMm < MIN_UPPER_WALL_CLEARANCE_MM) {
                alerts.add(
                    EngineeringComplianceAlert(
                        id = "upper_clearance_${wallUnit.uid}",
                        severity = ComplianceSeverity.CRITICAL,
                        titleAr = "انخفاض ارتفاع الخزانة العلوية «${wallUnit.displayTitle}»",
                        detailsAr = "المسافة الرأسية فوق سطح العمل هي $verticalGapMm ملم، والحد الأدنى الهندسي المعتمد هو $MIN_UPPER_WALL_CLEARANCE_MM ملم.",
                        affectedUnitUid = wallUnit.uid,
                        secondaryUnitUid = baseBelow?.uid,
                        requiredClearanceMm = MIN_UPPER_WALL_CLEARANCE_MM,
                        actualClearanceMm = verticalGapMm.coerceAtLeast(0),
                        affectedUnitTitleAr = wallUnit.displayTitle,
                        recommendationAr = "ارفع الخزانة العلوية إلى ارتفاع 1450 ملم على الأقل من الأرض (600 ملم فوق الرخام).",
                        category = ComplianceRuleCategory.UPPER_CABINET_CLEARANCE
                    )
                )
            }
        }

        // Check 5: Room Boundary Overflow Check
        items.forEach { item ->
            if (item.xMm + item.effectiveWidthMm > roomWidthMm || item.yMm + item.effectiveDepthMm > roomDepthMm) {
                alerts.add(
                    EngineeringComplianceAlert(
                        id = "boundary_overflow_${item.uid}",
                        severity = ComplianceSeverity.CRITICAL,
                        titleAr = "تجاوز حدود الغرفة للوحدة «${item.displayTitle}»",
                        detailsAr = "الوحدة تمتد خارج الجدار الفعلي للغرفة (${roomWidthMm}×${roomDepthMm} ملم).",
                        affectedUnitUid = item.uid,
                        requiredClearanceMm = 0,
                        actualClearanceMm = -100,
                        affectedUnitTitleAr = item.displayTitle,
                        recommendationAr = "أعد محاذاة الوحدة داخل حدود جدران الغرفة.",
                        category = ComplianceRuleCategory.ROOM_BOUNDARY
                    )
                )
            }
        }

        val penalty = alerts.sumOf {
            if (it.severity == ComplianceSeverity.CRITICAL) 18 else 9
        }
        val score = (100 - penalty).coerceIn(35, 100)
        return EngineeringComplianceReport(
            alerts = alerts,
            complianceScorePercent = score,
            isFullyCompliant = alerts.isEmpty()
        )
    }

    /**
     * Automatically resolves unit intersections, door swing clearance, island distance, and upper cabinet safety height violations.
     */
    fun autoFixComplianceIssues(
        items: List<PlacedKitchenItem>,
        roomWidthMm: Int,
        roomDepthMm: Int
    ): List<PlacedKitchenItem> {
        val resolved = mutableListOf<PlacedKitchenItem>()
        items.forEach { item ->
            var fixed = item
            // Ensure upper wall cabinets have at least 600mm clearance above 850mm base counter (>= 1450mm)
            if (fixed.type.category == ModuleCategory.WALL_CABINET && fixed.elevationMm < 1450) {
                fixed = fixed.copy(customElevationMm = 1450)
            }
            // Ensure islands leave at least 950mm walkway/door-swing clearance from back wall units (600mm + 950mm = 1550mm)
            if (fixed.type == ModuleType.ISLAND_WATERFALL && fixed.yMm < 1550) {
                val safeIslandY = 1600.coerceAtMost((roomDepthMm - fixed.effectiveDepthMm).coerceAtLeast(900))
                fixed = fixed.withPositionMm(fixed.xMm, safeIslandY)
            }
            // Clamp inside room boundaries
            val maxX = (roomWidthMm - fixed.effectiveWidthMm).coerceAtLeast(0)
            val maxY = (roomDepthMm - fixed.effectiveDepthMm).coerceAtLeast(0)
            fixed = fixed.withPositionMm(
                fixed.xMm.coerceIn(0, maxX),
                fixed.yMm.coerceIn(0, maxY)
            )
            // De-intersect against already resolved same-tier units
            val isWall = fixed.type.category == ModuleCategory.WALL_CABINET
            var attempt = 0
            while (attempt < 8) {
                val colliding = resolved.firstOrNull { peer ->
                    (peer.type.category == ModuleCategory.WALL_CABINET) == isWall &&
                        peer.type != ModuleType.FILLER_STRIP &&
                        fixed.type != ModuleType.FILLER_STRIP &&
                        (min(fixed.xMm + fixed.effectiveWidthMm, peer.xMm + peer.effectiveWidthMm) - max(fixed.xMm, peer.xMm)) > 2 &&
                        (min(fixed.yMm + fixed.effectiveDepthMm, peer.yMm + peer.effectiveDepthMm) - max(fixed.yMm, peer.yMm)) > 2
                } ?: break
                val shiftedRightX = colliding.xMm + colliding.effectiveWidthMm
                if (shiftedRightX + fixed.effectiveWidthMm <= roomWidthMm) {
                    fixed = fixed.withPositionMm(shiftedRightX, fixed.yMm)
                } else {
                    val shiftedDownY = (colliding.yMm + colliding.effectiveDepthMm).coerceAtMost(maxY)
                    fixed = fixed.withPositionMm(fixed.xMm.coerceIn(0, maxX), shiftedDownY)
                }
                attempt++
            }
            resolved.add(fixed)
        }
        return resolved
    }

    private fun rangesOverlap(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean {
        return max(aStart, bStart) < min(aEnd, bEnd)
    }
}

/**
 * 4. Smart Internal Kitchen Accessories Library Auto-Linked to Compatible Units & BOM
 */
data class InternalKitchenAccessory(
    val code: String,
    val nameAr: String,
    val descriptionAr: String,
    val compatibleModuleTypes: Set<ModuleType>,
    val unitPriceSar: Int,
    val specCode: String,
    val autoAttachByDefault: Boolean = true,
    val minUnitWidthMm: Int = 400,
    val brandAr: String = "Blum / Kesseböhmer"
) {
    val compatibleTypes: Set<ModuleType> get() = compatibleModuleTypes
    val pricePerUnitSar: Int get() = unitPriceSar
    val specAr: String get() = descriptionAr
}

data class LinkedAccessoryBomItem(
    val accessory: InternalKitchenAccessory,
    val matchedUnitsCount: Int,
    val matchedUnitLabels: List<String>,
    val totalCostSar: Int
) {
    val quantity: Int get() = matchedUnitsCount
    val matchedUnitsSummaryAr: String get() = matchedUnitLabels.take(2).joinToString("، ")
    val totalPriceSar: Int get() = totalCostSar
}

object SmartAccessoriesCatalog {

    val allAccessories: List<InternalKitchenAccessory> = listOf(
        InternalKitchenAccessory(
            code = "ACC_CUTLERY_ORG_80",
            nameAr = "منظم أدراج السكاكين والملاعق الخشبي (Orga-Line)",
            descriptionAr = "تقسيم داخلي من خشب الجوز والستانلس ستيل مخصص لوحدات الأدراج العريضة",
            compatibleModuleTypes = setOf(ModuleType.BASE_DRAWERS_80, ModuleType.BASE_POT_DRAWERS_90, ModuleType.BASE_CABINET_90),
            unitPriceSar = 380,
            specCode = "BLUM-AMBIA-800",
            autoAttachByDefault = true,
            minUnitWidthMm = 600,
            brandAr = "Blum Ambia-Line"
        ),
        InternalKitchenAccessory(
            code = "ACC_MAGIC_CORNER_SWING",
            nameAr = "دوار زاوية هيدروليكي ذكي (Magic Corner Carousel)",
            descriptionAr = "سلال زاوية مزدوجة تخرج بالكامل عند فتح باب الخزانة الزاوية",
            compatibleModuleTypes = setOf(
                ModuleType.CORNER_MAGIC,
                ModuleType.CORNER_BASE_CAROUSEL_90,
                ModuleType.CORNER_BASE_DIAGONAL_105,
                ModuleType.CORNER_BASE_BLIND_110
            ),
            unitPriceSar = 1150,
            specCode = "KESSEBOHMER-MC-90",
            autoAttachByDefault = true,
            minUnitWidthMm = 800,
            brandAr = "Kesseböhmer"
        ),
        InternalKitchenAccessory(
            code = "ACC_UNDER_SINK_WASTE",
            nameAr = "نظام فرز النفايات والمنظفات السحاب تحت الحوض",
            descriptionAr = "حاويات مدمجة بغطاء أوتوماتيكي ومجرى هيدروليكي أسفل حوض الغسيل",
            compatibleModuleTypes = setOf(ModuleType.SINK_DOUBLE),
            unitPriceSar = 540,
            specCode = "BLANCO-SELECT-60",
            autoAttachByDefault = true,
            minUnitWidthMm = 600,
            brandAr = "Blanco Select"
        ),
        InternalKitchenAccessory(
            code = "ACC_PANTRY_TALL_CARGO",
            nameAr = "طقم سلال سحب كروم لدولاب المؤونة (6 طبقات Tandem)",
            descriptionAr = "آلية سحب متزامنة تتحمل حتى 100 كجم لدولاب المؤونة الطولي",
            compatibleModuleTypes = setOf(
                ModuleType.TALL_PANTRY,
                ModuleType.TALL_GLASS_DISPLAY_80,
                ModuleType.TALL_COFFEE_STATION_90
            ),
            unitPriceSar = 1420,
            specCode = "HAFELE-DISPENSA-215",
            autoAttachByDefault = true,
            minUnitWidthMm = 500,
            brandAr = "Häfele Dispensa"
        ),
        InternalKitchenAccessory(
            code = "ACC_SPICE_BOTTLE_PULLOUT",
            nameAr = "منظم الزيوت والتوابل الجانبي السحاب (Soft-Close)",
            descriptionAr = "رفوف كروم مانعة للانزلاق تركب داخل الخزائن السفلية القياسية",
            compatibleModuleTypes = setOf(
                ModuleType.BASE_CABINET_60,
                ModuleType.BASE_CABINET_45,
                ModuleType.BASE_SPICE_PULLOUT_30
            ),
            unitPriceSar = 320,
            specCode = "INXA-SPICE-600",
            autoAttachByDefault = true,
            minUnitWidthMm = 300,
            brandAr = "Inoxa Italy"
        ),
        InternalKitchenAccessory(
            code = "ACC_WALL_DISH_DRYER",
            nameAr = "صفاية أطباق وأكواب ستانلس 304 للخزائن العلوية",
            descriptionAr = "حامل أطباق مزدوج مع صينية تجميع مياه مخفية للفيترينات والخزائن العلوية",
            compatibleModuleTypes = setOf(
                ModuleType.WALL_GLASS_80,
                ModuleType.WALL_LIFT_60,
                ModuleType.WALL_CABINET_90,
                ModuleType.CORNER_WALL_L_65,
                ModuleType.CORNER_WALL_DIAGONAL_60
            ),
            unitPriceSar = 290,
            specCode = "INOX-DRYER-800",
            autoAttachByDefault = true,
            minUnitWidthMm = 600,
            brandAr = "Vibo Inox"
        ),
        InternalKitchenAccessory(
            code = "ACC_ISLAND_POPUP_SOCKET",
            nameAr = "برج مقابس كهرباء ذكي منبثق للجزيرة مع شاحن لاسلكي Qi",
            descriptionAr = "مقبس ألمنيوم مقاوم للماء يختفي داخل رخام الجزيرة الوسطية",
            compatibleModuleTypes = setOf(ModuleType.ISLAND_WATERFALL),
            unitPriceSar = 680,
            specCode = "EVO-LINE-PORT-QI",
            autoAttachByDefault = true,
            minUnitWidthMm = 1200,
            brandAr = "Schulte Evoline"
        )
    )

    val defaultEnabledCodes: Set<String> = allAccessories
        .filter { it.autoAttachByDefault }
        .map { it.code }
        .toSet()

    /**
     * Automatically matches enabled smart accessories to compatible units currently placed in the kitchen layout.
     */
    fun computeAutoLinkedAccessoriesBom(
        placedItems: List<PlacedKitchenItem>,
        enabledAccessoryCodes: Set<String>
    ): List<LinkedAccessoryBomItem> {
        return allAccessories
            .filter { it.code in enabledAccessoryCodes }
            .mapNotNull { accessory ->
                val matchingUnits = placedItems.filter { it.type in accessory.compatibleModuleTypes }
                if (matchingUnits.isEmpty()) {
                    null
                } else {
                    LinkedAccessoryBomItem(
                        accessory = accessory,
                        matchedUnitsCount = matchingUnits.size,
                        matchedUnitLabels = matchingUnits.map { it.displayTitle },
                        totalCostSar = matchingUnits.size * accessory.unitPriceSar
                    )
                }
            }
    }
}

/**
 * 5. Interactive Virtual Ruler & Wall/Neighbor Distance Measurement Engine
 */
data class UnitClearanceDistancesMm(
    val unitUid: String,
    val unitLabel: String,
    val distanceToLeftWallMm: Int,
    val distanceToRightWallMm: Int,
    val distanceToBackWallMm: Int,
    val distanceToFrontWallMm: Int,
    val nearestLeftNeighborGapMm: Int?,
    val nearestLeftNeighborLabel: String?,
    val nearestRightNeighborGapMm: Int?,
    val nearestRightNeighborLabel: String?
) {
    val leftWallMm: Int get() = distanceToLeftWallMm
    val rightWallMm: Int get() = distanceToRightWallMm
    val backWallMm: Int get() = distanceToBackWallMm
    val frontWallMm: Int get() = distanceToFrontWallMm
    val nearestLeftUnitGapMm: Int? get() = nearestLeftNeighborGapMm
    val nearestRightUnitGapMm: Int? get() = nearestRightNeighborGapMm

    fun summaryAr(unitSystem: MeasurementUnitSystem): String {
        val leftPart = if (nearestLeftNeighborGapMm != null && nearestLeftNeighborGapMm < distanceToLeftWallMm) {
            "جار يسار: ${unitSystem.formatMillimeters(nearestLeftNeighborGapMm)}"
        } else {
            "جدار يسار: ${unitSystem.formatMillimeters(distanceToLeftWallMm)}"
        }
        val rightPart = if (nearestRightNeighborGapMm != null && nearestRightNeighborGapMm < distanceToRightWallMm) {
            "جار يمين: ${unitSystem.formatMillimeters(nearestRightNeighborGapMm)}"
        } else {
            "جدار يمين: ${unitSystem.formatMillimeters(distanceToRightWallMm)}"
        }
        return "$leftPart • $rightPart • ممر أمامي: ${unitSystem.formatMillimeters(distanceToFrontWallMm)}"
    }
}

object VirtualRulerEngine {

    fun measureUnitDistances(
        selected: PlacedKitchenItem,
        allItems: List<PlacedKitchenItem>,
        roomWidthMm: Int,
        roomDepthMm: Int
    ): UnitClearanceDistancesMm {
        val leftWallMm = selected.xMm.coerceAtLeast(0)
        val rightWallMm = (roomWidthMm - (selected.xMm + selected.effectiveWidthMm)).coerceAtLeast(0)
        val backWallMm = selected.yMm.coerceAtLeast(0)
        val frontWallMm = (roomDepthMm - (selected.yMm + selected.effectiveDepthMm)).coerceAtLeast(0)

        val sameTierPeers = allItems.filter {
            it.uid != selected.uid &&
                (it.type.category == ModuleCategory.WALL_CABINET) == (selected.type.category == ModuleCategory.WALL_CABINET) &&
                abs(it.yMm - selected.yMm) < 350
        }

        val leftPeer = sameTierPeers
            .filter { it.xMm + it.effectiveWidthMm <= selected.xMm + 5 }
            .maxByOrNull { it.xMm + it.effectiveWidthMm }

        val rightPeer = sameTierPeers
            .filter { it.xMm >= selected.xMm + selected.effectiveWidthMm - 5 }
            .minByOrNull { it.xMm }

        return UnitClearanceDistancesMm(
            unitUid = selected.uid,
            unitLabel = selected.displayTitle,
            distanceToLeftWallMm = leftWallMm,
            distanceToRightWallMm = rightWallMm,
            distanceToBackWallMm = backWallMm,
            distanceToFrontWallMm = frontWallMm,
            nearestLeftNeighborGapMm = leftPeer?.let { (selected.xMm - (it.xMm + it.effectiveWidthMm)).coerceAtLeast(0) },
            nearestLeftNeighborLabel = leftPeer?.displayTitle,
            nearestRightNeighborGapMm = rightPeer?.let { (it.xMm - (selected.xMm + selected.effectiveWidthMm)).coerceAtLeast(0) },
            nearestRightNeighborLabel = rightPeer?.displayTitle
        )
    }

    fun measurePointToPointDistanceMm(
        startXMm: Int,
        startYMm: Int,
        endXMm: Int,
        endYMm: Int
    ): Int {
        val dx = (endXMm - startXMm).toDouble()
        val dy = (endYMm - startYMm).toDouble()
        return hypot(dx, dy).roundToInt()
    }
}
