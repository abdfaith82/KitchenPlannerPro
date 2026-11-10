package com.example.data.model

import com.example.R

data class KitchenStylePreset(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val badgeAr: String,
    val imageResId: Int,
    val defaultShape: KitchenShape,
    val defaultWidthCm: Int,
    val defaultDepthCm: Int,
    val defaultHeightCm: Int,
    val cabinetFinish: String,
    val cabinetColorHex: Long,
    val countertopMaterial: String,
    val countertopColorHex: Long,
    val handleStyle: String,
    val wallColorHex: String,
    val floorMaterial: String,
    val floorColorHex: Long,
    val presetItems: List<PlacedKitchenItem>
)

data class MaterialFinishOption(
    val nameAr: String,
    val categoryAr: String,
    val colorHex: Long,
    val priceMultiplierPercent: Int,
    val textureResId: Int = R.drawable.img_tex_wood_walnut_oak_1794207924023
)

data class WallColorOption(
    val nameAr: String,
    val hexCode: String,
    val colorHex: Long
) {
    val key: String get() = hexCode
}

data class VisualHarmonyEvaluation(
    val harmonyScorePercent: Int,
    val verdictAr: String,
    val recommendationAr: String
) {
    val first: Int get() = harmonyScorePercent
    val second: String get() = verdictAr
}

data class CertifiedShowroom(
    val id: String,
    val nameAr: String,
    val cityAr: String,
    val specialtyAr: String,
    val rating: String,
    val warrantyYears: Int,
    val deliveryDays: String
)

object KitchenCatalogData {

    val cabinetFinishes = listOf(
        MaterialFinishOption(
            "جوز أمريكي طبيعي",
            "خشب طبيعي PBR",
            0xFF6F472B,
            0,
            R.drawable.img_tex_wood_walnut_oak_1794207924023
        ),
        MaterialFinishOption(
            "بلوط إسكندنافي فاتح",
            "خشب طبيعي PBR",
            0xFFC5A47E,
            0,
            R.drawable.img_tex_wood_walnut_oak_1794207924023
        ),
        MaterialFinishOption(
            "زجاج أسود مدخن وألمنيوم مؤكسد",
            "فيترينات PBR 8K",
            0xFF1F2428,
            12,
            R.drawable.img_tex_granite_galaxy_1794207924005
        ),
        MaterialFinishOption(
            "ألمنيوم مؤكسد شامبين",
            "معدن مؤكسد PBR",
            0xFF8C857B,
            10,
            R.drawable.img_tex_marble_calacatta_1794207923112
        ),
        MaterialFinishOption(
            "أخضر ميرمية مطفي",
            "لاكيه ألماني",
            0xFF6B7F6E,
            5,
            R.drawable.img_tex_marble_calacatta_1794207923112
        ),
        MaterialFinishOption(
            "أكريليك فحمي فاخر",
            "أكريليك مضاد للبصمات",
            0xFF2B3033,
            8,
            R.drawable.img_tex_granite_galaxy_1794207924005
        ),
        MaterialFinishOption(
            "أبيض حريري لؤلؤي",
            "لاكيه إيطالي",
            0xFFF2EFE9,
            0,
            R.drawable.img_tex_marble_calacatta_1794207923112
        ),
        MaterialFinishOption(
            "أزرق كحلي ملكي",
            "مات فينيش",
            0xFF23394D,
            6,
            R.drawable.img_tex_granite_galaxy_1794207924005
        )
    )

    val countertopMaterials = listOf(
        MaterialFinishOption(
            "رخام كالكاتا إيطالي",
            "رخام عروق ذهبية PBR",
            0xFFF4F1EA,
            10,
            R.drawable.img_tex_marble_calacatta_1794207923112
        ),
        MaterialFinishOption(
            "رخام كرارا أبيض إيطالي",
            "رخام طبيعي PBR",
            0xFFECEFF1,
            8,
            R.drawable.img_tex_marble_calacatta_1794207923112
        ),
        MaterialFinishOption(
            "جرانيت أسود جالكسي مصقول",
            "جرانيت طبيعي PBR",
            0xFF1B1E20,
            12,
            R.drawable.img_tex_granite_galaxy_1794207924005
        ),
        MaterialFinishOption(
            "رخام نيرو ماركينا أسود",
            "رخام إسباني فاخر",
            0xFF222527,
            12,
            R.drawable.img_tex_granite_galaxy_1794207924005
        ),
        MaterialFinishOption(
            "كوارتز أبيض نقي",
            "كوارتز مقاوم للخدش",
            0xFFFAFAFA,
            5,
            R.drawable.img_tex_marble_calacatta_1794207923112
        ),
        MaterialFinishOption(
            "سطح خشب بلوط صلب",
            "خشب معالج ضد الماء",
            0xFFB88B58,
            0,
            R.drawable.img_tex_wood_walnut_oak_1794207924023
        ),
        MaterialFinishOption(
            "بورسلين ترافرتين رملي",
            "بورسلين إيطالي حراري",
            0xFFDAD2C4,
            8,
            R.drawable.img_tex_marble_calacatta_1794207923112
        )
    )

    val wallColors = listOf(
        WallColorOption("ألباستر كريمي دافئ", "#F5F2EB", 0xFFF5F2EB),
        WallColorOption("رمادي حجري ناعم", "#E4E2DD", 0xFFE4E2DD),
        WallColorOption("بيج رملي معماري", "#EAE0D0", 0xFFEAE0D0),
        WallColorOption("أخضر ميرمية ضبابي", "#DCE4DF", 0xFFDCE4DF),
        WallColorOption("عاجي لؤلؤي مشرق", "#FAF8F5", 0xFFFAF8F5),
        WallColorOption("طيني تيراكوتا هادئ", "#E6CFC3", 0xFFE6CFC3),
        WallColorOption("أزرق ضبابي إسكندنافي", "#D7E2E8", 0xFFD7E2E8),
        WallColorOption("رمادي غريج فندقي", "#D5CFC7", 0xFFD5CFC7),
        WallColorOption("أخضر زيتوني معماري", "#CBD3C6", 0xFFCBD3C6),
        WallColorOption("موكا كشمير ناعم", "#DDD1C7", 0xFFDDD1C7),
        WallColorOption("أنثراسيت فحمي عصري", "#374046", 0xFF374046),
        WallColorOption("كحلي مسائي داكن", "#263238", 0xFF263238)
    )

    val handleStyles = listOf(
        "مقابض مدمجة مخفية (Gola)",
        "نحاس مصقول",
        "بدون مقابض (Push-to-Open)",
        "ألمنيوم مؤكسد أسود",
        "ستانلس ستيل ألماني"
    )

    val floorMaterials = listOf(
        MaterialFinishOption("باركيه بلوط دافئ", "خشب هندسي", 0xFFC8A986, 0, R.drawable.img_tex_wood_walnut_oak_1794207924023),
        MaterialFinishOption("باركيه جوز شيفرون (Herringbone)", "خشب طبيعي فاخر", 0xFF8A5A3B, 5, R.drawable.img_tex_wood_walnut_oak_1794207924023),
        MaterialFinishOption("بورسلين رمادي معماري", "بورسلين 120×120", 0xFF8E9194, 0, R.drawable.img_tex_granite_galaxy_1794207924005),
        MaterialFinishOption("رخام كالكاتا بيج كريمي", "رخام مصقول", 0xFFE6DEC8, 6, R.drawable.img_tex_marble_calacatta_1794207923112),
        MaterialFinishOption("تيرازو إيطالي حديث", "تيرازو فينيسي", 0xFFD4CEC3, 4, R.drawable.img_tex_marble_calacatta_1794207923112),
        MaterialFinishOption("ترافرتين رملي مطفي", "حجر طبيعي", 0xFFD7C9B4, 4, R.drawable.img_tex_marble_calacatta_1794207923112),
        MaterialFinishOption("حجر أردواز رمادي داكن (Slate)", "حجر طبيعي مضاد للانزلاق", 0xFF4A5258, 5, R.drawable.img_tex_granite_galaxy_1794207924005),
        MaterialFinishOption("خرسانة مصقولة ميكروسمنت", "طابع صناعي حديث", 0xFFA7ABB0, 3, R.drawable.img_tex_granite_galaxy_1794207924005),
        MaterialFinishOption("خشب أشقر شمالي فاتح", "باركيه إسكندنافي", 0xFFE2D3BE, 2, R.drawable.img_tex_wood_walnut_oak_1794207924023),
        MaterialFinishOption("بورسلين أبيض عروق ذهبية", "بورسلين لامع 8K", 0xFFF2EFE9, 5, R.drawable.img_tex_marble_calacatta_1794207923112)
    )

    data class WallFloorHarmonyPalette(
        val id: String,
        val titleAr: String,
        val subtitleAr: String,
        val wallColorHex: String,
        val floorMaterial: String,
        val recommendedCabinetFinish: String,
        val recommendedCountertop: String
    ) {
        val wallColorNameAr: String get() = resolveWallColorNameAr(wallColorHex)
    }

    val harmonyPalettes = listOf(
        WallFloorHarmonyPalette(
            id = "WARM_MEDITERRANEAN",
            titleAr = "تناغم الجوز الدافئ والألباستر",
            subtitleAr = "حائط ألباستر كريمي مع أرضية باركيه بلوط وخزائن جوز أمريكي",
            wallColorHex = "#F5F2EB",
            floorMaterial = "باركيه بلوط دافئ",
            recommendedCabinetFinish = "جوز أمريكي طبيعي",
            recommendedCountertop = "رخام كالكاتا إيطالي"
        ),
        WallFloorHarmonyPalette(
            id = "NORDIC_SAGE",
            titleAr = "إشراقة الميرمية الإسكندنافية",
            subtitleAr = "حائط ميرمية ضبابي مع خشب أشقر شمالي فاتح ورخام كرارا",
            wallColorHex = "#DCE4DF",
            floorMaterial = "خشب أشقر شمالي فاتح",
            recommendedCabinetFinish = "أخضر ميرمية مطفي",
            recommendedCountertop = "رخام كرارا أبيض إيطالي"
        ),
        WallFloorHarmonyPalette(
            id = "ARCHITECTURAL_DARK",
            titleAr = "الفخامة الفندقية المعاصرة",
            subtitleAr = "حائط غريج فندقي مع بورسلين رمادي معماري وزجاج أسود مدخن",
            wallColorHex = "#D5CFC7",
            floorMaterial = "بورسلين رمادي معماري",
            recommendedCabinetFinish = "زجاج أسود مدخن وألمنيوم مؤكسد",
            recommendedCountertop = "جرانيت أسود جالكسي مصقول"
        ),
        WallFloorHarmonyPalette(
            id = "ROYAL_CONTRAST",
            titleAr = "تباين الكحلي الملكي والرخام الكريمي",
            subtitleAr = "حائط عاجي لؤلؤي مع أرضية رخام كالكاتا بيج وخزائن كحلي ملكي",
            wallColorHex = "#FAF8F5",
            floorMaterial = "رخام كالكاتا بيج كريمي",
            recommendedCabinetFinish = "أزرق كحلي ملكي",
            recommendedCountertop = "رخام كالكاتا إيطالي"
        ),
        WallFloorHarmonyPalette(
            id = "TERRACOTTA_OAK",
            titleAr = "دفء التيراكوتا وبلوط إسكندنافي",
            subtitleAr = "حائط طيني تيراكوتا هادئ مع أرضية ترافرتين رملي وخزائن بلوط فاتح",
            wallColorHex = "#E6CFC3",
            floorMaterial = "ترافرتين رملي مطفي",
            recommendedCabinetFinish = "بلوط إسكندنافي فاتح",
            recommendedCountertop = "بورسلين ترافرتين رملي"
        ),
        WallFloorHarmonyPalette(
            id = "INDUSTRIAL_LOFT",
            titleAr = "طابع اللوفت المعماري الحديث",
            subtitleAr = "حائط أنثراسيت فحمي مع أرضية ترافرتين رملي وبلوط فاتح",
            wallColorHex = "#374046",
            floorMaterial = "ترافرتين رملي مطفي",
            recommendedCabinetFinish = "بلوط إسكندنافي فاتح",
            recommendedCountertop = "بورسلين ترافرتين رملي"
        )
    )

    val certifiedShowrooms = listOf(
        CertifiedShowroom(
            id = "showroom_riyadh_1",
            nameAr = "دار المطابخ الألمانية - الرياض",
            cityAr = "الرياض",
            specialtyAr = "مطابخ جزيرة حديثة وأنظمة Blum الذكية",
            rating = "4.9",
            warrantyYears = 15,
            deliveryDays = "25 - 35 يوم"
        ),
        CertifiedShowroom(
            id = "showroom_jeddah_1",
            nameAr = "استوديو ميلانو للمطابخ الفاخرة - جدة",
            cityAr = "جدة",
            specialtyAr = "تصاميم إيطالية رخام شلال وخزائن زجاجية",
            rating = "4.8",
            warrantyYears = 12,
            deliveryDays = "30 - 40 يوم"
        ),
        CertifiedShowroom(
            id = "showroom_dammam_1",
            nameAr = "مصنع النخبة للمطابخ العصرية - الخبر",
            cityAr = "الدمام / الخبر",
            specialtyAr = "تفصيل CNC 18mm حسب المقاس مع أجهزة بلت إن",
            rating = "4.9",
            warrantyYears = 10,
            deliveryDays = "20 - 30 يوم"
        ),
        CertifiedShowroom(
            id = "showroom_dubai_1",
            nameAr = "أرتيزان كيتشن ديزاين - دبي",
            cityAr = "دبي",
            specialtyAr = "مطابخ ذكية وهندسة إضاءة معمارية 8K",
            rating = "4.9",
            warrantyYears = 15,
            deliveryDays = "25 - 35 يوم"
        )
    )

    val stylePresets: List<KitchenStylePreset> = listOf(
        KitchenStylePreset(
            id = "MODERN_WALNUT",
            titleAr = "مطبخ الجوز والرخام بالجزيرة",
            subtitleAr = "تناغم دافئ بين خشب الجوز الأمريكي وجزيرة رخام كالكاتا شلال مع إضاءة نحاسية",
            badgeAr = "الأكثر طلباً",
            imageResId = R.drawable.img_hero_kitchen_1793913766470,
            defaultShape = KitchenShape.ISLAND,
            defaultWidthCm = 440,
            defaultDepthCm = 380,
            defaultHeightCm = 285,
            cabinetFinish = "جوز أمريكي طبيعي",
            cabinetColorHex = 0xFF6F472B,
            countertopMaterial = "رخام كالكاتا إيطالي",
            countertopColorHex = 0xFFF4F1EA,
            handleStyle = "مقابض مدمجة مخفية (Gola)",
            wallColorHex = "#F5F2EB",
            floorMaterial = "باركيه بلوط دافئ",
            floorColorHex = 0xFFC8A986,
            presetItems = buildDefaultLayoutForShape(KitchenShape.ISLAND, 440, 380)
        ),
        KitchenStylePreset(
            id = "SCANDI_OAK",
            titleAr = "التصميم الإسكندنافي الطبيعي",
            subtitleAr = "خزائن سفلية بلون أخضر الميرمية الهادئ مع وحدات علوية من خشب البلوط الفاتح",
            badgeAr = "إشراقة طبيعية",
            imageResId = R.drawable.img_style_scandi_1793913767766,
            defaultShape = KitchenShape.L_SHAPE,
            defaultWidthCm = 390,
            defaultDepthCm = 330,
            defaultHeightCm = 275,
            cabinetFinish = "أخضر ميرمية مطفي",
            cabinetColorHex = 0xFF6B7F6E,
            countertopMaterial = "رخام كرارا أبيض إيطالي",
            countertopColorHex = 0xFFECEFF1,
            handleStyle = "مقابض مدمجة مخفية (Gola)",
            wallColorHex = "#F5F2EB",
            floorMaterial = "باركيه بلوط دافئ",
            floorColorHex = 0xFFC8A986,
            presetItems = buildDefaultLayoutForShape(KitchenShape.L_SHAPE, 390, 330)
        ),
        KitchenStylePreset(
            id = "LUXURY_DARK",
            titleAr = "الفخامة المعمارية الداكنة (زجاج مدخن)",
            subtitleAr = "واجهات زجاج أسود مدخن وألمنيوم مؤكسد مع إضاءة LED 3000K وجرانيت أسود",
            badgeAr = "تصميم 8K PBR",
            imageResId = R.drawable.img_style_luxury_1793913768004,
            defaultShape = KitchenShape.U_SHAPE,
            defaultWidthCm = 460,
            defaultDepthCm = 360,
            defaultHeightCm = 290,
            cabinetFinish = "زجاج أسود مدخن وألمنيوم مؤكسد",
            cabinetColorHex = 0xFF1F2428,
            countertopMaterial = "جرانيت أسود جالكسي مصقول",
            countertopColorHex = 0xFF1B1E20,
            handleStyle = "ألمنيوم مؤكسد أسود",
            wallColorHex = "#EAE0D0",
            floorMaterial = "بورسلين رمادي معماري",
            floorColorHex = 0xFF8E9194,
            presetItems = buildDefaultLayoutForShape(KitchenShape.U_SHAPE, 460, 360)
        )
    )

    fun defaultCatalogUnitsForUser(userId: String): List<KitchenCatalogUnit> =
        ModuleType.entries.map { it.toDefaultCatalogUnit(userId) }

    fun buildDefaultLayoutForShape(
        shape: KitchenShape,
        roomWidthCm: Int,
        roomDepthCm: Int
    ): List<PlacedKitchenItem> {
        val w = roomWidthCm.coerceAtLeast(280)
        val d = roomDepthCm.coerceAtLeast(240)
        return when (shape) {
            KitchenShape.ISLAND -> listOf(
                PlacedKitchenItem("item_fridge", ModuleType.FRIDGE_SMART, 0, 0, rotationDeg = 0),
                PlacedKitchenItem("item_pantry", ModuleType.TALL_PANTRY, 90, 0, rotationDeg = 0),
                PlacedKitchenItem("item_sink", ModuleType.SINK_DOUBLE, 150, 0, rotationDeg = 0),
                PlacedKitchenItem("item_dw", ModuleType.DISHWASHER, 240, 0, rotationDeg = 0),
                PlacedKitchenItem("item_stove", ModuleType.STOVE_HOB, 300, 0, rotationDeg = 0),
                PlacedKitchenItem("item_wall1", ModuleType.WALL_GLASS_80, 150, 0, rotationDeg = 0, customElevationMm = 1450),
                PlacedKitchenItem("item_wall2", ModuleType.WALL_LIFT_60, 230, 0, rotationDeg = 0, customElevationMm = 1450),
                PlacedKitchenItem(
                    "item_island",
                    ModuleType.ISLAND_WATERFALL,
                    ((w - 180) / 2).coerceAtLeast(60),
                    (d / 2).coerceAtLeast(150),
                    rotationDeg = 0
                )
            )

            KitchenShape.L_SHAPE -> listOf(
                PlacedKitchenItem("item_corner", ModuleType.CORNER_MAGIC, 0, 0, rotationDeg = 0),
                PlacedKitchenItem("item_corner_wall", ModuleType.CORNER_WALL_L_65, 0, 0, rotationDeg = 0, customElevationMm = 1450),
                PlacedKitchenItem("item_sink", ModuleType.SINK_DOUBLE, 90, 0, rotationDeg = 0),
                PlacedKitchenItem("item_dw", ModuleType.DISHWASHER, 180, 0, rotationDeg = 0),
                PlacedKitchenItem("item_stove", ModuleType.STOVE_HOB, 240, 0, rotationDeg = 0),
                PlacedKitchenItem("item_wall1", ModuleType.WALL_GLASS_80, 90, 0, rotationDeg = 0, customElevationMm = 1450),
                PlacedKitchenItem("item_wall2", ModuleType.WALL_LIFT_60, 170, 0, rotationDeg = 0, customElevationMm = 1450),
                PlacedKitchenItem("item_drawers", ModuleType.BASE_DRAWERS_80, 0, 90, rotationDeg = 90),
                PlacedKitchenItem("item_fridge", ModuleType.FRIDGE_SMART, 0, 170, rotationDeg = 90)
            )

            KitchenShape.U_SHAPE -> listOf(
                PlacedKitchenItem("item_corner_l", ModuleType.CORNER_MAGIC, 0, 0, rotationDeg = 0),
                PlacedKitchenItem("item_corner_wall_l", ModuleType.CORNER_WALL_L_65, 0, 0, rotationDeg = 0, customElevationMm = 1450),
                PlacedKitchenItem("item_sink", ModuleType.SINK_DOUBLE, 90, 0, rotationDeg = 0),
                PlacedKitchenItem("item_dw", ModuleType.DISHWASHER, 180, 0, rotationDeg = 0),
                PlacedKitchenItem("item_base60", ModuleType.BASE_CABINET_60, 240, 0, rotationDeg = 0),
                PlacedKitchenItem("item_corner_r", ModuleType.CORNER_BASE_CAROUSEL_90, (w - 90).coerceAtLeast(300), 0, rotationDeg = 0),
                PlacedKitchenItem("item_stove", ModuleType.STOVE_HOB, 0, 90, rotationDeg = 90),
                PlacedKitchenItem("item_drawers", ModuleType.BASE_DRAWERS_80, 0, 180, rotationDeg = 90),
                PlacedKitchenItem("item_fridge", ModuleType.FRIDGE_SMART, (w - 70).coerceAtLeast(320), 90, rotationDeg = 270),
                PlacedKitchenItem("item_oven_tower", ModuleType.TALL_OVEN_TOWER, (w - 60).coerceAtLeast(330), 180, rotationDeg = 270),
                PlacedKitchenItem("item_wall1", ModuleType.WALL_GLASS_80, 90, 0, rotationDeg = 0, customElevationMm = 1450)
            )

            KitchenShape.GALLEY -> listOf(
                PlacedKitchenItem("item_fridge", ModuleType.FRIDGE_SMART, 0, 0, rotationDeg = 0),
                PlacedKitchenItem("item_sink", ModuleType.SINK_DOUBLE, 90, 0, rotationDeg = 0),
                PlacedKitchenItem("item_dw", ModuleType.DISHWASHER, 180, 0, rotationDeg = 0),
                PlacedKitchenItem("item_drawers", ModuleType.BASE_DRAWERS_80, 240, 0, rotationDeg = 0),
                PlacedKitchenItem("item_wall1", ModuleType.WALL_GLASS_80, 90, 0, rotationDeg = 0, customElevationMm = 1450),
                PlacedKitchenItem("item_stove", ModuleType.STOVE_HOB, 90, (d - 60).coerceAtLeast(180), rotationDeg = 180),
                PlacedKitchenItem("item_base60", ModuleType.BASE_CABINET_60, 180, (d - 60).coerceAtLeast(180), rotationDeg = 180),
                PlacedKitchenItem("item_pantry", ModuleType.TALL_PANTRY, 240, (d - 60).coerceAtLeast(180), rotationDeg = 180)
            )

            KitchenShape.SINGLE_WALL -> listOf(
                PlacedKitchenItem("item_fridge", ModuleType.FRIDGE_SMART, 0, 0, rotationDeg = 0),
                PlacedKitchenItem("item_sink", ModuleType.SINK_DOUBLE, 90, 0, rotationDeg = 0),
                PlacedKitchenItem("item_dw", ModuleType.DISHWASHER, 180, 0, rotationDeg = 0),
                PlacedKitchenItem("item_stove", ModuleType.STOVE_HOB, 240, 0, rotationDeg = 0),
                PlacedKitchenItem("item_wall1", ModuleType.WALL_GLASS_80, 90, 0, rotationDeg = 0, customElevationMm = 1450),
                PlacedKitchenItem("item_wall2", ModuleType.WALL_LIFT_60, 170, 0, rotationDeg = 0, customElevationMm = 1450)
            )
        }
    }

    fun resolveCabinetColorHex(finishName: String): Long =
        cabinetFinishes.firstOrNull { it.nameAr == finishName }?.colorHex ?: 0xFF6F472B

    fun resolveCountertopColorHex(materialName: String): Long =
        countertopMaterials.firstOrNull { it.nameAr == materialName }?.colorHex ?: 0xFFF4F1EA

    fun resolveFloorColorHex(floorName: String): Long =
        floorMaterials.firstOrNull { it.nameAr == floorName }?.colorHex ?: 0xFFC8A986

    fun resolveWallColorHex(wallHex: String): Long {
        wallColors.firstOrNull { it.hexCode.equals(wallHex.trim(), ignoreCase = true) }?.let { return it.colorHex }
        val clean = wallHex.trim().removePrefix("#")
        if (clean.length == 6) {
            val rgb = clean.toLongOrNull(16)
            if (rgb != null) return 0xFF000000L or rgb
        }
        return 0xFFF5F2EB
    }

    fun resolveWallColorNameAr(wallHex: String): String =
        wallColors.firstOrNull { it.hexCode.equals(wallHex.trim(), ignoreCase = true) }?.nameAr
            ?: "لون حائط مخصص ($wallHex)"

    /**
     * Computes a visual color harmony score (0..100%) and architectural recommendation
     * between the selected Wall Color, Floor Material, Cabinet Finish, and Countertop.
     */
    fun evaluateVisualColorHarmony(
        wallColorHex: String = "",
        floorMaterial: String = "",
        cabinetFinish: String = "",
        countertopMaterial: String = "",
        wallColorKey: String = ""
    ): VisualHarmonyEvaluation {
        val effectiveWallHex = wallColorHex.ifBlank { wallColorKey.ifBlank { "#F5F2EB" } }
        val wallLong = resolveWallColorHex(effectiveWallHex)
        val floorLong = resolveFloorColorHex(floorMaterial)
        val cabLong = resolveCabinetColorHex(cabinetFinish)
        val topLong = resolveCountertopColorHex(countertopMaterial)

        fun luminance(hex: Long): Float {
            val r = ((hex shr 16) and 0xFF) / 255f
            val g = ((hex shr 8) and 0xFF) / 255f
            val b = (hex and 0xFF) / 255f
            return 0.299f * r + 0.587f * g + 0.114f * b
        }

        val wallLum = luminance(wallLong)
        val floorLum = luminance(floorLong)
        val cabLum = luminance(cabLong)
        val topLum = luminance(topLong)

        val wallCabContrast = kotlin.math.abs(wallLum - cabLum)
        val floorCabContrast = kotlin.math.abs(floorLum - cabLum)
        val topCabContrast = kotlin.math.abs(topLum - cabLum)

        var score = 84
        if (wallCabContrast in 0.18f..0.72f) score += 7 else if (wallCabContrast < 0.08f) score -= 9
        if (floorCabContrast >= 0.12f) score += 5 else score -= 5
        if (topCabContrast >= 0.15f) score += 4
        val finalScore = score.coerceIn(74, 98)

        val verdict = when {
            finalScore >= 92 -> "تناسق بصري ممتاز ($finalScore%)"
            finalScore >= 82 -> "تناسق لوني متناغم ($finalScore%)"
            else -> "تناسق مقبول ($finalScore%)"
        }
        val recommendation = when {
            finalScore >= 92 -> "تباين متوازن بين لون الحائط والأرضية يبرز جمال الخزائن والرخام في الإضاءة ثلاثية الأبعاد."
            finalScore >= 82 -> "تدرجات الحائط والأرضية متكاملة مع تشطيب الخزائن وأسطح العمل."
            else -> "يفضل زيادة التباين الضوئي بين لون الحائط أو الأرضية ولون الخزائن لإبراز التفاصيل ثلاثية الأبعاد."
        }
        return VisualHarmonyEvaluation(
            harmonyScorePercent = finalScore,
            verdictAr = verdict,
            recommendationAr = recommendation
        )
    }
}
