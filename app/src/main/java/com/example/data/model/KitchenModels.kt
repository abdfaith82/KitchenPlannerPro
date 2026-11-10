package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class KitchenShape(val code: String, val titleAr: String, val subtitleAr: String) {
    L_SHAPE("L_SHAPE", "مطبخ حرف L", "مثالي للمساحات المفتوحة والزوايا الذكية"),
    U_SHAPE("U_SHAPE", "مطبخ حرف U", "أقصى سعة تخزين ومثلث عمل متقارب"),
    ISLAND("ISLAND", "مطبخ مع جزيرة وسطية", "تصميم عصري فاخر للضيافة والتحضير"),
    GALLEY("GALLEY", "مطبخ متوازي (خطين)", "كفاءة عالية للمطابخ الطولية الاحترافية"),
    SINGLE_WALL("SINGLE_WALL", "مطبخ جدار واحد", "حل أنيق ومدمج للشقق والاستوديوهات الحديثة");

    companion object {
        fun fromCode(code: String): KitchenShape =
            entries.firstOrNull { it.code == code } ?: L_SHAPE
    }
}

enum class CameraPreset3D(
    val titleAr: String,
    val yawDeg: Float,
    val pitchFactor: Float,
    val zoom: Float
) {
    ISOMETRIC_RIGHT("منظور يمين 3D", 38f, 0.54f, 1.0f),
    ISOMETRIC_LEFT("منظور يسار 3D", -42f, 0.54f, 1.0f),
    FRONT_ELEVATION("واجهة أمامية", 0f, 0.28f, 1.08f),
    TOP_PERSPECTIVE("منظور علوي شامل", 25f, 0.82f, 0.92f),
    ISLAND_CLOSEUP("تقريب الجزيرة", 15f, 0.45f, 1.28f),
    FIRST_PERSON_EYE("تجول داخلي (شخص أول)", 12f, 0.26f, 1.42f);

    companion object {
        fun fromNameOrDefault(name: String): CameraPreset3D =
            entries.firstOrNull { it.name == name } ?: ISOMETRIC_RIGHT
    }
}

enum class ModuleCategory(val code: String, val titleAr: String) {
    BASE_CABINET("BASE", "وحدات سفلية وأسطح"),
    WALL_CABINET("WALL", "خزائن علوية وفيترينات"),
    TALL_UNIT("TALL", "وحدات دولاب طويلة"),
    ISLAND_UNIT("ISLAND", "وحدات جزيرة"),
    APPLIANCE("APPLIANCE", "أجهزة كهربائية مدمجة"),
    SINK_WATER("SINK", "أحواض ومياه");

    companion object {
        fun fromCode(code: String): ModuleCategory =
            entries.firstOrNull { it.code == code } ?: BASE_CABINET
    }
}

enum class ModuleType(
    val code: String,
    val titleAr: String,
    val category: ModuleCategory,
    val subType: String,
    val defaultWidthCm: Int,
    val defaultDepthCm: Int,
    val heightCm: Int,
    val elevationCm: Int,
    val defaultPriceSar: Int,
    val colorAccentHex: Long,
    val countertopIncluded: Boolean,
    val descriptionAr: String
) {
    SINK_DOUBLE(
        "SINK",
        "حوض غسيل مزدوج مع خلاط نحاسي",
        ModuleCategory.SINK_WATER,
        "SINK",
        90,
        60,
        85,
        0,
        2650,
        0xFF2B7A9B,
        true,
        "وحدة حوض غسيل مزدوج ستانلس ستيل مقاوم للصدأ مع خلاط سحب وسطح عمل علوي"
    ),
    STOVE_HOB(
        "STOVE",
        "فرن وموقد غاز/سيراميك بلت إن",
        ModuleCategory.APPLIANCE,
        "OVEN",
        90,
        60,
        85,
        0,
        4400,
        0xFFC85A32,
        true,
        "فرن مدمج 900 ملم مع سطح موقد سيراميك 4 شعلات وتحكم لمس ذكي"
    ),
    FRIDGE_SMART(
        "FRIDGE",
        "ثلاجة وفريزر مدمج دولابي",
        ModuleCategory.APPLIANCE,
        "FRIDGE",
        90,
        70,
        215,
        0,
        6200,
        0xFF3E6B89,
        false,
        "ثلاجة دولابية مدمجة بسعة 540 لتر مع نظام تبريد مزدوج No-Frost"
    ),
    DISHWASHER(
        "DISHWASHER",
        "غسالة صحون بلت إن 600 ملم",
        ModuleCategory.APPLIANCE,
        "DISHWASHER",
        60,
        60,
        85,
        0,
        3100,
        0xFF457B9D,
        true,
        "غسالة أطباق مدمجة بالكامل 14 فرد مع سطح عمل متصل"
    ),
    BASE_DRAWERS_80(
        "BASE_DRAWERS",
        "وحدة أدراج سفلية 3 مستويات مع سطح عمل",
        ModuleCategory.BASE_CABINET,
        "COUNTERTOP",
        80,
        60,
        85,
        0,
        1950,
        0xFF8C6239,
        true,
        "خزانة سفلية 18mm CNC مع 3 أدراج هيدروليك Blum ومقبض Gola ألمنيوم"
    ),
    BASE_CABINET_60(
        "BASE_60",
        "خزانة سفلية بدرفتين 600 ملم مع سطح عمل",
        ModuleCategory.BASE_CABINET,
        "CABINET",
        60,
        60,
        85,
        0,
        1450,
        0xFF7F5539,
        true,
        "وحدة تخزين سفلية قياسية 18mm CNC بدرفتين ورف داخلي ومقبض Gola"
    ),
    CORNER_MAGIC(
        "CORNER_BASE",
        "خزانة كورنر سفلية ذكية (ماجيك كورنر حرف L)",
        ModuleCategory.BASE_CABINET,
        "CORNER_BASE",
        90,
        90,
        85,
        0,
        2900,
        0xFF6C584C,
        true,
        "خزانة زاوية سفلية حرف L بظهر وجنبين كاملين 18mm CNC وسلال سحب دوارة"
    ),
    CORNER_BASE_CAROUSEL_90(
        "CORNER_CAROUSEL_90",
        "خزانة كورنر سفلية دوارة 360° (90×90 سم)",
        ModuleCategory.BASE_CABINET,
        "CORNER_BASE",
        90,
        90,
        85,
        0,
        3150,
        0xFF5E4B3C,
        true,
        "خزانة زاوية سفلية كاملة بظهر محكم 18mm وصواني كروم دوارة 360 درجة"
    ),
    CORNER_BASE_DIAGONAL_105(
        "CORNER_DIAG_105",
        "خزانة كورنر سفلية مشطوفة 45° (105×105 سم)",
        ModuleCategory.BASE_CABINET,
        "CORNER_BASE",
        105,
        105,
        85,
        0,
        3350,
        0xFF7A5C43,
        true,
        "وحدة ركن سفلية مشطوفة بواجهة مائلة 45° وظهر وجوانب كاملة 18mm مع سطح رخام"
    ),
    CORNER_BASE_BLIND_110(
        "CORNER_BLIND_110",
        "خزانة كورنر سفلية مخفية LeMans (110×60 سم)",
        ModuleCategory.BASE_CABINET,
        "CORNER_BASE",
        110,
        60,
        85,
        0,
        2980,
        0xFF694E37,
        true,
        "خزانة ركن سفلية مستطيلة بآلية سحب انسيابية LeMans وظهر وجنب كامل"
    ),
    CORNER_WALL_L_65(
        "CORNER_WALL_L",
        "خزانة كورنر علوية حرف L مضاءة LED (65×65 سم)",
        ModuleCategory.WALL_CABINET,
        "CORNER_WALL",
        65,
        65,
        75,
        145,
        1950,
        0xFF8D6E63,
        false,
        "خزانة زاوية علوية حرف L بظهر وجنبين كاملين 18mm ورفوف داخلية مضاءة LED"
    ),
    CORNER_WALL_DIAGONAL_60(
        "CORNER_WALL_DIAG",
        "خزانة كورنر علوية مشطوفة 45° زجاجية (65×65 سم)",
        ModuleCategory.WALL_CABINET,
        "CORNER_WALL",
        65,
        65,
        75,
        145,
        2100,
        0xFF795548,
        false,
        "وحدة زاوية علوية مشطوفة 45° بباب زجاج مدخن وظهر وجوانب كاملة محكمة"
    ),
    BASE_CABINET_45(
        "BASE_45",
        "خزانة سفلية ضلفة ودرج 450 ملم",
        ModuleCategory.BASE_CABINET,
        "CABINET",
        45,
        60,
        85,
        0,
        1250,
        0xFF8B5E3C,
        true,
        "خزانة سفلية مفردة 45 سم بظهر وجنبين كاملين 18mm ودرج علوي هيدروليك"
    ),
    BASE_CABINET_90(
        "BASE_90",
        "خزانة سفلية عريضة بدرفتين ورفوف 900 ملم",
        ModuleCategory.BASE_CABINET,
        "CABINET",
        90,
        60,
        85,
        0,
        1850,
        0xFF744C2E,
        true,
        "خزانة سفلية عريضة 90 سم بظهر 18mm كامل وأجناب مدعمة مع سطح عمل"
    ),
    BASE_POT_DRAWERS_90(
        "BASE_POT_90",
        "وحدة أدراج أواني عميقة 900 ملم (Blum Tandembox)",
        ModuleCategory.BASE_CABINET,
        "COUNTERTOP",
        90,
        60,
        85,
        0,
        2350,
        0xFF6E472B,
        true,
        "خزانة سفلية بدرجين عميقين للأواني الثقيلة بظهر وجنب كامل 18mm"
    ),
    BASE_SPICE_PULLOUT_30(
        "BASE_SPICE_30",
        "خزانة سحب بهارات وزيوت 300 ملم",
        ModuleCategory.BASE_CABINET,
        "CABINET",
        30,
        60,
        85,
        0,
        1180,
        0xFF9C6B43,
        true,
        "وحدة سفلية نحيفة 30 سم بسلال كروم قابلة للسحب الكامل وظهر وجنبين 18mm"
    ),
    WALL_GLASS_80(
        "WALL_GLASS",
        "فيترينة علوية زجاج أسود مدخن LED",
        ModuleCategory.WALL_CABINET,
        "VITRINE",
        80,
        35,
        75,
        145,
        1750,
        0xFF9C7A54,
        false,
        "وحدة حائط علوية بإطار ألمنيوم مؤكسد وزجاج أسود مدخن وإضاءة LED 3000K"
    ),
    WALL_LIFT_60(
        "WALL_60",
        "خزانة علوية قلاب هيدروليك",
        ModuleCategory.WALL_CABINET,
        "CABINET",
        60,
        35,
        75,
        145,
        1350,
        0xFFA68A64,
        false,
        "خزانة علوية تفتح للأعلى بمفصلات هيدروليكية مزدوجة على ارتفاع أمان 600 ملم"
    ),
    WALL_CABINET_90(
        "WALL_90",
        "خزانة علوية عريضة بدرفتين 900 ملم",
        ModuleCategory.WALL_CABINET,
        "CABINET",
        90,
        35,
        75,
        145,
        1650,
        0xFF937252,
        false,
        "خزانة حائط علوية بدرفتين ورفين داخليين مع ظهر وجنبين كاملين 18mm"
    ),
    WALL_OPEN_SHELVES_60(
        "WALL_OPEN_60",
        "وحدة أرفف علوية مفتوحة بظهر خشبي 600 ملم",
        ModuleCategory.WALL_CABINET,
        "VITRINE",
        60,
        35,
        75,
        145,
        1190,
        0xFFB08968,
        false,
        "خزانة عرض علوية مفتوحة بظهر خشبي كامل 18mm وأجناب سميكة وإضاءة مخفية"
    ),
    WALL_HOOD_CABINET_90(
        "WALL_HOOD_90",
        "خزانة علوية لشفاط المطبخ المخفي 900 ملم",
        ModuleCategory.WALL_CABINET,
        "CABINET",
        90,
        35,
        60,
        160,
        1550,
        0xFF84674B,
        false,
        "خزانة علوية مخصصة لاحتواء الشفاط المدمج مع توابل جانبية وظهر كامل"
    ),
    TALL_PANTRY(
        "TALL_PANTRY",
        "دولاب مؤونة طولي بسلال سحب",
        ModuleCategory.TALL_UNIT,
        "CABINET",
        60,
        60,
        215,
        0,
        3400,
        0xFF583E26,
        false,
        "وحدة دولاب تخزين طويل بارتفاع 2150 ملم مع 6 سلال كروم قابلة للسحب"
    ),
    TALL_OVEN_TOWER(
        "TALL_OVEN",
        "وحدة برج فرن وميكروويف مدمج",
        ModuleCategory.TALL_UNIT,
        "OVEN",
        60,
        60,
        215,
        0,
        4800,
        0xFF4A3525,
        false,
        "برج أجهزة مدمجة يشمل فرن كهربائي على مستوى النظر وميكروويف وأدراج سفلية"
    ),
    TALL_GLASS_DISPLAY_80(
        "TALL_VITRINE_80",
        "دولاب فيترينة زجاجي طويل بإضاءة LED كاملة",
        ModuleCategory.TALL_UNIT,
        "VITRINE",
        80,
        60,
        215,
        0,
        4200,
        0xFF3E2C1E,
        false,
        "دولاب عرض زجاجي كامل الارتفاع بظهر خشبي مضاء وأجناب كاملة 18mm"
    ),
    TALL_COFFEE_STATION_90(
        "TALL_COFFEE_90",
        "دولاب ركن القهوة والبار الصباحي (Coffee Station)",
        ModuleCategory.TALL_UNIT,
        "CABINET",
        90,
        60,
        215,
        0,
        4650,
        0xFF634832,
        false,
        "وحدة ركن قهوة متكاملة بسطح رخام داخلي وأبواب جيبية وظهر وجوانب كاملة"
    ),
    ISLAND_WATERFALL(
        "ISLAND_MAIN",
        "جزيرة وسطية رخام شلال مع إفطار",
        ModuleCategory.ISLAND_UNIT,
        "ISLAND",
        180,
        95,
        90,
        0,
        6900,
        0xFFB08968,
        true,
        "جزيرة مطبخ وسطية فاخرة بسطح رخام شلال جانبي وخزائن تخزين مزدوجة"
    ),
    FILLER_STRIP(
        "FILLER_STRIP",
        "شريحة فيلر تعبئة فراغ (1 ملم)",
        ModuleCategory.BASE_CABINET,
        "FILLER",
        10,
        60,
        85,
        0,
        320,
        0xFFA98467,
        true,
        "شريحة فيلر CNC مخصصة لتعبئة فراغات الحائط بدقة 1 ملم"
    );

    val isCornerCabinet: Boolean
        get() = code.startsWith("CORNER_") || subType.startsWith("CORNER")

    fun toDefaultCatalogUnit(userId: String = ""): KitchenCatalogUnit = KitchenCatalogUnit(
        id = "std_${code.lowercase()}",
        userId = userId,
        code = code,
        nameAr = titleAr,
        category = category.code,
        subType = subType,
        widthCm = defaultWidthCm.toLong(),
        depthCm = defaultDepthCm.toLong(),
        heightCm = heightCm.toLong(),
        elevationCm = elevationCm.toLong(),
        priceSar = defaultPriceSar.toLong(),
        colorHex = colorAccentHex,
        materialFinish = "جوز أمريكي طبيعي",
        countertopIncluded = countertopIncluded,
        descriptionAr = descriptionAr
    )

    companion object {
        fun fromCode(code: String): ModuleType =
            entries.firstOrNull { it.code == code } ?: BASE_CABINET_60
    }
}

@IgnoreExtraProperties
data class KitchenCatalogUnit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "BASE_60",
    val nameAr: String = "",
    val category: String = "BASE",
    val subType: String = "CABINET",
    val widthCm: Long = 60L,
    val depthCm: Long = 60L,
    val heightCm: Long = 85L,
    val elevationCm: Long = 0L,
    val priceSar: Long = 1450L,
    val colorHex: Long = 0xFF7F5539L,
    val materialFinish: String = "جوز أمريكي طبيعي",
    val countertopIncluded: Boolean = true,
    val descriptionAr: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val resolvedModuleType: ModuleType
        get() = ModuleType.fromCode(code)

    val resolvedCategory: ModuleCategory
        get() = ModuleCategory.fromCode(category)

    val isCornerUnit: Boolean
        get() = resolvedModuleType.isCornerCabinet || subType.contains("CORNER", ignoreCase = true) || nameAr.contains("كورنر") || nameAr.contains("زاوية")
}

/**
 * Supports both centimeter compatibility (xCm, yCm, widthCm, depthCm) and
 * true 1mm CAD precision via fine millimeter sub-offsets (0..9 mm) or direct mm getters,
 * plus full 6-sided realistic cabinet construction (18mm full side gables + full back panel).
 */
data class PlacedKitchenItem(
    val uid: String,
    val type: ModuleType,
    val xCm: Int,
    val yCm: Int,
    val widthCm: Int = type.defaultWidthCm,
    val depthCm: Int = type.defaultDepthCm,
    val rotationDeg: Int = 0,
    val customLabel: String = type.titleAr,
    val priceSar: Int = type.defaultPriceSar,
    val fineXMmOffset: Int = 0,
    val fineYMmOffset: Int = 0,
    val fineWidthMmOffset: Int = 0,
    val fineDepthMmOffset: Int = 0,
    val customHeightMm: Int = type.heightCm * 10,
    val customElevationMm: Int = if (type.category == ModuleCategory.WALL_CABINET) {
        (type.elevationCm * 10).coerceAtLeast(1450)
    } else {
        type.elevationCm * 10
    },
    val boardThicknessMm: Int = 18,
    val hasFullBackPanel: Boolean = true,
    val backPanelThicknessMm: Int = 18,
    val hasFullSidePanels: Boolean = true,
    val shelvesCount: Int = if (type.category == ModuleCategory.TALL_UNIT) 4 else 2,
    val doorsCount: Int = if (type.defaultWidthCm >= 70) 2 else 1,
    val drawersCount: Int = if (type == ModuleType.BASE_DRAWERS_80 || type == ModuleType.BASE_POT_DRAWERS_90) 3 else 0,
    val handleSystem: String = "GOLA_ALUMINUM",
    val pairedWithBaseUid: String? = null,
    val customCabinetFinish: String? = null,
    val customCountertopMaterial: String? = null
) {
    val displayTitle: String
        get() = customLabel.ifBlank { type.titleAr }

    val xMm: Int
        get() = (xCm * 10 + fineXMmOffset).coerceAtLeast(0)

    val yMm: Int
        get() = (yCm * 10 + fineYMmOffset).coerceAtLeast(0)

    val widthMm: Int
        get() = (widthCm * 10 + fineWidthMmOffset).coerceAtLeast(15)

    val depthMm: Int
        get() = (depthCm * 10 + fineDepthMmOffset).coerceAtLeast(15)

    val heightMm: Int
        get() = customHeightMm.coerceAtLeast(100)

    val elevationMm: Int
        get() = customElevationMm.coerceAtLeast(0)

    val effectiveWidthCm: Int
        get() = if (rotationDeg == 90 || rotationDeg == 270) depthCm else widthCm

    val effectiveDepthCm: Int
        get() = if (rotationDeg == 90 || rotationDeg == 270) widthCm else depthCm

    val effectiveWidthMm: Int
        get() = if (rotationDeg == 90 || rotationDeg == 270) depthMm else widthMm

    val effectiveDepthMm: Int
        get() = if (rotationDeg == 90 || rotationDeg == 270) widthMm else depthMm

    val centerXCm: Float
        get() = xMm / 10f + effectiveWidthMm / 20f

    val centerYCm: Float
        get() = yMm / 10f + effectiveDepthMm / 20f

    fun withPositionMm(newXMm: Int, newYMm: Int): PlacedKitchenItem {
        val clampedX = newXMm.coerceAtLeast(0)
        val clampedY = newYMm.coerceAtLeast(0)
        return copy(
            xCm = clampedX / 10,
            fineXMmOffset = clampedX % 10,
            yCm = clampedY / 10,
            fineYMmOffset = clampedY % 10
        )
    }

    fun withDimensionsMm(newWidthMm: Int, newDepthMm: Int = depthMm): PlacedKitchenItem {
        val safeW = newWidthMm.coerceIn(15, 3600)
        val safeD = newDepthMm.coerceIn(15, 1800)
        return copy(
            widthCm = (safeW / 10).coerceAtLeast(2),
            fineWidthMmOffset = safeW % 10,
            depthCm = (safeD / 10).coerceAtLeast(2),
            fineDepthMmOffset = safeD % 10
        )
    }
}

data class Saved3DViewportSnapshot(
    val cameraYawDeg: Float = 38f,
    val cameraPitchFactor: Float = 0.54f,
    val cameraZoom: Float = 1.0f,
    val cameraPreset: CameraPreset3D = CameraPreset3D.ISOMETRIC_RIGHT,
    val lightingModeName: String = "DAYLIGHT",
    val doorsOpenPreview: Boolean = false
)

data class Decoded3DLayout(
    val items: List<PlacedKitchenItem>,
    val viewport: Saved3DViewportSnapshot?
)

object KitchenItemsSerializer {
    private const val VIEW3D_PREFIX = "#VIEW3D"

    fun encode(items: List<PlacedKitchenItem>): String {
        return items.joinToString(";") { item ->
            val safeLabel = item.customLabel.replace("|", " ").replace(";", " ")
            val safeFinish = (item.customCabinetFinish ?: "").replace("|", " ").replace(";", " ")
            val safeCounter = (item.customCountertopMaterial ?: "").replace("|", " ").replace(";", " ")
            "${item.type.code}|${item.xCm}|${item.yCm}|${item.widthCm}|${item.depthCm}|${item.rotationDeg}|$safeLabel|${item.priceSar}|${item.fineXMmOffset}|${item.fineYMmOffset}|${item.fineWidthMmOffset}|${item.customElevationMm}|$safeFinish|$safeCounter"
        }
    }

    fun encodeWithViewport(
        items: List<PlacedKitchenItem>,
        viewport: Saved3DViewportSnapshot
    ): String {
        val roundedYaw = (viewport.cameraYawDeg * 10f).roundToInt() / 10f
        val roundedPitch = (viewport.cameraPitchFactor * 100f).roundToInt() / 100f
        val roundedZoom = (viewport.cameraZoom * 100f).roundToInt() / 100f
        val header = listOf(
            VIEW3D_PREFIX,
            roundedYaw.toString(),
            roundedPitch.toString(),
            roundedZoom.toString(),
            viewport.cameraPreset.name,
            viewport.lightingModeName,
            if (viewport.doorsOpenPreview) "1" else "0"
        ).joinToString("|")
        val itemsPart = encode(items)
        return if (itemsPart.isBlank()) header else "$header;$itemsPart"
    }

    fun decodeWithViewport(serialized: String): Decoded3DLayout {
        if (serialized.isBlank()) return Decoded3DLayout(emptyList(), null)
        var viewport: Saved3DViewportSnapshot? = null
        val items = mutableListOf<PlacedKitchenItem>()

        serialized.split(";").forEachIndexed { index, token ->
            val trimmed = token.trim()
            if (trimmed.isEmpty()) return@forEachIndexed
            val parts = trimmed.split("|")
            if (parts[0] == VIEW3D_PREFIX && parts.size >= 6) {
                val yaw = parts.getOrNull(1)?.toFloatOrNull() ?: 38f
                val pitch = parts.getOrNull(2)?.toFloatOrNull() ?: 0.54f
                val zoom = parts.getOrNull(3)?.toFloatOrNull() ?: 1.0f
                val preset = CameraPreset3D.fromNameOrDefault(parts.getOrNull(4) ?: "")
                val lighting = parts.getOrNull(5) ?: "DAYLIGHT"
                val doorsOpen = parts.getOrNull(6) == "1"
                viewport = Saved3DViewportSnapshot(
                    cameraYawDeg = yaw,
                    cameraPitchFactor = pitch,
                    cameraZoom = zoom,
                    cameraPreset = preset,
                    lightingModeName = lighting,
                    doorsOpenPreview = doorsOpen
                )
            } else if (parts.size >= 6) {
                val type = ModuleType.fromCode(parts[0])
                val x = parts[1].toIntOrNull() ?: 0
                val y = parts[2].toIntOrNull() ?: 0
                val w = parts[3].toIntOrNull() ?: type.defaultWidthCm
                val d = parts[4].toIntOrNull() ?: type.defaultDepthCm
                val rot = parts[5].toIntOrNull() ?: 0
                val label = parts.getOrNull(6)?.takeIf { it.isNotBlank() } ?: type.titleAr
                val price = parts.getOrNull(7)?.toIntOrNull() ?: type.defaultPriceSar
                val fineX = parts.getOrNull(8)?.toIntOrNull() ?: 0
                val fineY = parts.getOrNull(9)?.toIntOrNull() ?: 0
                val fineW = parts.getOrNull(10)?.toIntOrNull() ?: 0
                val elevMm = parts.getOrNull(11)?.toIntOrNull()
                    ?: if (type.category == ModuleCategory.WALL_CABINET) 1450 else type.elevationCm * 10
                val customCabFinish = parts.getOrNull(12)?.takeIf { it.isNotBlank() }
                val customTopMat = parts.getOrNull(13)?.takeIf { it.isNotBlank() }
                items.add(
                    PlacedKitchenItem(
                        uid = "item_${index}_${type.code}_${x}_$y",
                        type = type,
                        xCm = x,
                        yCm = y,
                        widthCm = w,
                        depthCm = d,
                        rotationDeg = rot,
                        customLabel = label,
                        priceSar = price,
                        fineXMmOffset = fineX,
                        fineYMmOffset = fineY,
                        fineWidthMmOffset = fineW,
                        customElevationMm = elevMm,
                        customCabinetFinish = customCabFinish,
                        customCountertopMaterial = customTopMat
                    )
                )
            }
        }
        return Decoded3DLayout(items = items, viewport = viewport)
    }

    fun decode(serialized: String): List<PlacedKitchenItem> =
        decodeWithViewport(serialized).items
}

data class WorkTriangleAnalysis(
    val hasCompleteTriangle: Boolean,
    val sinkToStoveCm: Int,
    val stoveToFridgeCm: Int,
    val fridgeToSinkCm: Int,
    val perimeterCm: Int,
    val score: Int,
    val statusTitleAr: String,
    val recommendationAr: String
)

object ErgonomicAnalyzer {
    fun analyze(items: List<PlacedKitchenItem>): WorkTriangleAnalysis {
        val sink = items.firstOrNull { it.type == ModuleType.SINK_DOUBLE }
        val stove = items.firstOrNull { it.type == ModuleType.STOVE_HOB || it.type == ModuleType.TALL_OVEN_TOWER }
        val fridge = items.firstOrNull { it.type == ModuleType.FRIDGE_SMART }

        if (sink == null || stove == null || fridge == null) {
            val missing = buildList {
                if (sink == null) add("حوض الغسيل")
                if (stove == null) add("الموقد/الفرن")
                if (fridge == null) add("الثلاجة")
            }.joinToString("، ")
            return WorkTriangleAnalysis(
                hasCompleteTriangle = false,
                sinkToStoveCm = 0,
                stoveToFridgeCm = 0,
                fridgeToSinkCm = 0,
                perimeterCm = 0,
                score = 45,
                statusTitleAr = "مثلث العمل غير مكتمل",
                recommendationAr = "لتحقيق معايير NKBA الهندسية، أضف العناصر التالية للمخطط: $missing."
            )
        }

        val dSinkStove = hypot(
            (sink.centerXCm - stove.centerXCm).toDouble(),
            (sink.centerYCm - stove.centerYCm).toDouble()
        ).roundToInt()

        val dStoveFridge = hypot(
            (stove.centerXCm - fridge.centerXCm).toDouble(),
            (stove.centerYCm - fridge.centerYCm).toDouble()
        ).roundToInt()

        val dFridgeSink = hypot(
            (fridge.centerXCm - sink.centerXCm).toDouble(),
            (fridge.centerYCm - sink.centerYCm).toDouble()
        ).roundToInt()

        val perimeter = dSinkStove + dStoveFridge + dFridgeSink

        var penalties = 0
        listOf(dSinkStove, dStoveFridge, dFridgeSink).forEach { leg ->
            if (leg < 110) penalties += ((110 - leg) / 4).coerceAtMost(20)
            if (leg > 280) penalties += ((leg - 280) / 8).coerceAtMost(20)
        }
        if (perimeter < 380) penalties += 12
        if (perimeter > 800) penalties += 15

        val score = (98 - penalties).coerceIn(50, 99)
        val (title, rec) = when {
            score >= 88 -> "مثلث عمل مثالي (معيار NKBA ذهبي)" to
                "المسافات بين الحوض (${dSinkStove * 10} ملم للموقد) والثلاجة (${dFridgeSink * 10} ملم للحوض) متوازنة وتوفر حركة سلسة."
            score >= 74 -> "توزيع هندسي جيد قابل للتحسين" to
                "إجمالي محيط مثلث العمل ${(perimeter * 10)} ملم؛ يفضل تقريب العناصر الرئيسية لتكون كل مسافة بين 1200 و2700 ملم."
            else -> "المسافات متقاربة أو متباعدة أكثر من اللازم" to
                "يُنصح بإعادة تموضع الحوض أو الثلاجة بحيث لا تقل المسافة بينهما عن 1200 ملم ولا تزيد عن 2700 ملم."
        }

        return WorkTriangleAnalysis(
            hasCompleteTriangle = true,
            sinkToStoveCm = dSinkStove,
            stoveToFridgeCm = dStoveFridge,
            fridgeToSinkCm = dFridgeSink,
            perimeterCm = perimeter,
            score = score,
            statusTitleAr = title,
            recommendationAr = rec
        )
    }
}

@IgnoreExtraProperties
data class KitchenProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val shape: String = "L_SHAPE",
    val styleId: String = "MODERN_WALNUT",
    val roomWidthCm: Long = 400L,
    val roomDepthCm: Long = 340L,
    val roomHeightCm: Long = 280L,
    val cabinetFinish: String = "جوز أمريكي طبيعي",
    val countertopMaterial: String = "رخام كالكاتا إيطالي",
    val handleStyle: String = "نحاس مصقول",
    val wallColorHex: String = "#F5F2EB",
    val floorMaterial: String = "باركيه بلوط دافئ",
    val itemsData: String = "",
    val totalPriceSar: Long = 0L,
    val ergonomicScore: Long = 85L,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuoteRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val showroomName: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val city: String = "",
    val preferredInstallWindow: String = "",
    val estimatedBudgetSar: Long = 0L,
    val specialNotes: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
