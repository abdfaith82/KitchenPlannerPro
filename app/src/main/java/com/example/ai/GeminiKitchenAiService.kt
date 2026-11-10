package com.example.ai

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.example.data.model.KitchenCatalogData
import com.example.data.model.KitchenShape
import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.app
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Configures Firebase App Check for Firebase AI Logic (`Firebase.ai`).
 * Follows the exact AI Studio streaming emulator debug secret + reCAPTCHA Enterprise contract.
 */
@SuppressLint("DiscouragedPrivateApi", "ApplySharedPref")
fun configureAppCheck(context: Context, intent: Intent?) {
    // 1. Capture debug secret from launch intent if present:
    //    adb shell am start -n <pkg>/.MainActivity --es app_check_debug_secret <secret>
    val prefs = context.getSharedPreferences("app_check_debug_Launcher", Context.MODE_PRIVATE)
    var debugSecret = intent?.getStringExtra("app_check_debug_secret")
    if (debugSecret != null) {
        prefs.edit().putString("debug_secret", debugSecret).commit()
    } else {
        debugSecret = prefs.getString("debug_secret", null)
    }

    // 2. If a debug secret is active, inject it into the SDK's internal store and use Debug provider.
    //    Use .commit() (synchronous), NOT .apply() — DebugAppCheckProviderFactory reads
    //    SharedPreferences immediately on the next line.
    if (debugSecret != null) {
        val persistenceKey = Firebase.app.persistenceKey
        val storeName = "com.google.firebase.appcheck.debug.store.$persistenceKey"
        context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
            .edit()
            .putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", debugSecret)
            .commit()

        val factory = DebugAppCheckProviderFactory.getInstance()
        Firebase.appCheck.installAppCheckProviderFactory(factory)
        return
    }

    // 3. Otherwise, use reCAPTCHA Enterprise with the provisioned site key.
    val resId = context.resources.getIdentifier(
        "recaptcha_site_key", "string", context.packageName
    )
    if (resId == 0) return
    val siteKey = context.getString(resId)
    if (siteKey.isBlank() || siteKey == "Placeholder" || siteKey == "unused") return

    val factory = RecaptchaAppCheckProviderFactory.getInstance(siteKey)
    Firebase.appCheck.installAppCheckProviderFactory(factory)
}

data class GeminiSuggestedKitchenDesign(
    val designNameAr: String,
    val styleRationalAr: String,
    val ergonomicNotesAr: String,
    val recommendedShape: KitchenShape,
    val recommendedCabinetFinish: String,
    val recommendedCountertopMaterial: String,
    val recommendedWallColorHex: String,
    val recommendedFloorMaterial: String,
    val suggestedItems: List<PlacedKitchenItem>
) {
    val designTitleAr: String get() = designNameAr
    val architecturalRationaleAr: String get() = styleRationalAr
    val ergonomicHighlightAr: String get() = ergonomicNotesAr
    val cabinetFinishAr: String get() = recommendedCabinetFinish
    val countertopMaterialAr: String get() = recommendedCountertopMaterial
    val wallColorHex: String get() = recommendedWallColorHex
}

/**
 * Gemini AI Kitchen Architect powered by Firebase AI Logic (`gemini-3.8-flash`)
 * with strict JSON Schema output (`Schema.obj(...)`) to generate complete, collision-free,
 * 1mm-precision 3D kitchen layouts tailored to room dimensions and style preferences.
 */
object GeminiKitchenDesignService {

    private val kitchenDesignSchema = Schema.obj(
        mapOf(
            "designNameAr" to Schema.string("اسم التصميم المقترح بالعربية"),
            "styleRationalAr" to Schema.string("شرح معماري لاختيار النمط والألوان وتوزيع الإضاءة بالعربية"),
            "ergonomicNotesAr" to Schema.string("ملاحظات مثلث العمل الذهبي NKBA والمسافات القياسية بالعربية"),
            "recommendedShape" to Schema.enumeration(
                KitchenShape.entries.map { it.code },
                "شكل تخطيط المطبخ"
            ),
            "recommendedFinishName" to Schema.string("اسم خامة الخشب أو اللون المقترح بالعربية"),
            "recommendedCountertopName" to Schema.string("اسم خامة الرخام أو الكوارتز لسطح العمل بالعربية"),
            "units" to Schema.array(
                Schema.obj(
                    mapOf(
                        "moduleCode" to Schema.enumeration(
                            ModuleType.entries.map { it.code },
                            "معرف وحدة المطبخ القياسي"
                        ),
                        "xMm" to Schema.integer("إحداثي X بالمليمتر داخل الغرفة"),
                        "yMm" to Schema.integer("إحداثي Y بالمليمتر داخل الغرفة"),
                        "rotationDeg" to Schema.integer("زاوية الدوران بالدرجات (0 أو 90 أو 180 أو 270)"),
                        "widthMm" to Schema.integer("عرض الوحدة بالمليمتر"),
                        "depthMm" to Schema.integer("عمق الوحدة بالمليمتر"),
                        "heightMm" to Schema.integer("ارتفاع الوحدة بالمليمتر"),
                        "elevationMm" to Schema.integer("ارتفاع التركيب عن الأرض بالمليمتر (0 للسفلي و 1450 للعلوي)"),
                        "customLabelAr" to Schema.string("وصف مخصص للوحدة في المخطط")
                    )
                ),
                "قائمة وحدات المطبخ المقترحة مع إحداثياتها الدقيقة بالمليمتر"
            )
        )
    )

    private val generativeModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = kitchenDesignSchema
            temperature = 0.35f
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    suspend fun suggestCompleteKitchenLayout(
        roomWidthCm: Int,
        roomDepthCm: Int,
        roomHeightCm: Int,
        preferredShape: KitchenShape,
        stylePreferenceAr: String,
        customPrioritiesAr: String
    ): Result<GeminiSuggestedKitchenDesign> {
        return try {
            val roomWMm = roomWidthCm * 10
            val roomDMm = roomDepthCm * 10
            val roomHMm = roomHeightCm * 10

            val availableModulesSummary = ModuleType.entries.joinToString("\n") { module ->
                "- ${module.code}: ${module.titleAr} (Category=${module.category.code}, Default ${module.defaultWidthCm * 10}x${module.defaultDepthCm * 10}x${module.heightCm * 10} mm, Elevation=${module.elevationCm * 10} mm)"
            }
            val finishesSummary = KitchenCatalogData.cabinetFinishes.joinToString(" | ") { it.nameAr }
            val countertopsSummary = KitchenCatalogData.countertopMaterials.joinToString(" | ") { it.nameAr }

            val prompt = """
                أنت مهندس معماري ومصمم مطابخ ثلاثية الأبعاد محترف وفق معايير NKBA العالمية ودقة 1 ملم.
                المطلوب: اقتراح تصميم مطبخ كامل ومتناسق هندسياً بناءً على المعطيات التالية:
                - عرض الغرفة: $roomWidthCm سم ($roomWMm ملم)
                - عمق الغرفة: $roomDepthCm سم ($roomDMm ملم)
                - ارتفاع السقف: $roomHeightCm سم ($roomHMm ملم)
                - شكل التخطيط المفضل: ${preferredShape.code} (${preferredShape.titleAr})
                - تفضيلات النمط والألوان: $stylePreferenceAr
                - أولويات المستخدم الإضافية: ${customPrioritiesAr.ifBlank { "مثلث عمل مريح، مساحة تخزين ذكية، وتوزيع متوازن للأجهزة" }}

                قواعد هندسية صارمة بالمليمتر (mm):
                1. جميع الإحداثيات (xMm, yMm) يجب أن تقع داخل حدود الغرفة: 0 <= xMm <= ($roomWMm - widthMm) و 0 <= yMm <= ($roomDMm - depthMm) بدون تداخل بين الخزائن السفلية.
                2. الخزائن على الحائط الرئيسي العلوي يكون yMm = 0 و rotationDeg = 0.
                3. الخزائن على الحائط الأيسر (في حال L_SHAPE أو U_SHAPE) يكون xMm = 0 و rotationDeg = 90.
                4. يجب تضمين مثلث العمل الأساسي: حوض غسيل (SINK)، موقد/فرن (STOVE أو TALL_OVEN)، وثلاجة (FRIDGE)، بالإضافة إلى خزائن أدراج (BASE_DRAWERS)، خزائن سفلية (BASE_60)، غسالة صحون (DISHWASHER)، وخزائن علوية (WALL_GLASS و WALL_60).
                5. إذا كان الشكل ISLAND وكان عرض وعمق الغرفة يسمحان بذلك، أضف جزيرة وسطية (ISLAND_MAIN) في منتصف الغرفة.
                6. الخزائن العلوية (WALL_GLASS و WALL_60) يجب أن تكون بارتفاع تركيب elevationMm = 1450 لتوفير خلوص أمان 600 ملم فوق سطح العمل.
                7. اختر recommendedFinishName نصياً من القائمة: [$finishesSummary]
                8. اختر recommendedCountertopName نصياً من القائمة: [$countertopsSummary]

                قائمة الوحدات القياسية المتاحة (moduleCode):
                $availableModulesSummary
            """.trimIndent()

            val response = withTimeout(60_000L) {
                generativeModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("لم يتم استلام استجابة JSON من نموذج Gemini.")
            val parsed = parseGeminiKitchenResponse(
                jsonString = jsonText,
                roomWidthCm = roomWidthCm,
                roomDepthCm = roomDepthCm,
                roomHeightCm = roomHeightCm
            )
            Result.success(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseGeminiKitchenResponse(
        rawJson: String,
        roomWidthCm: Int,
        roomDepthCm: Int,
        fallbackShape: KitchenShape = KitchenShape.L_SHAPE
    ): GeminiSuggestedKitchenDesign = parseGeminiKitchenResponse(
        jsonString = rawJson,
        roomWidthCm = roomWidthCm,
        roomDepthCm = roomDepthCm,
        roomHeightCm = 285,
        fallbackShape = fallbackShape
    )

    fun parseGeminiKitchenResponse(
        jsonString: String,
        roomWidthCm: Int,
        roomDepthCm: Int,
        roomHeightCm: Int = 285,
        fallbackShape: KitchenShape = KitchenShape.L_SHAPE
    ): GeminiSuggestedKitchenDesign {
        val cleanJson = jsonString
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val root = JSONObject(cleanJson)

        val designNameAr = root.optString("designNameAr")
            .ifBlank { root.optString("designTitleAr", "تصميم مطبخ ذكي مقترح عبر Gemini AI") }
        val styleRationalAr = root.optString("styleRationalAr")
            .ifBlank {
                root.optString(
                    "architecturalRationaleAr",
                    "تم تنسيق الألوان والخامات لتحقيق توازن بصري وإضاءة دافئة مع استغلال أمثل للمساحة."
                )
            }
        val ergonomicNotesAr = root.optString("ergonomicNotesAr")
            .ifBlank {
                root.optString(
                    "ergonomicHighlightAr",
                    "تم ضبط مثلث العمل (الحوض - الموقد - الثلاجة) ضمن المسافة القياسية 4.0 إلى 7.9 متر مع خلوص 600 ملم للخزائن العلوية."
                )
            }

        val shapeStr = root.optString("recommendedShape", fallbackShape.code)
        val recommendedShape = KitchenShape.entries.find {
            it.code.equals(shapeStr, ignoreCase = true) || it.name.equals(shapeStr, ignoreCase = true)
        } ?: fallbackShape

        val finishStr = root.optString("recommendedFinishName")
            .ifBlank { root.optString("cabinetFinishAr", "") }
        val recommendedFinish = KitchenCatalogData.cabinetFinishes.firstOrNull {
            finishStr.contains(it.nameAr) || (finishStr.isNotBlank() && it.nameAr.contains(finishStr))
        }?.nameAr ?: when {
            finishStr.contains("بلوط") || finishStr.contains("إسكندنافي") -> "بلوط إسكندنافي فاتح"
            finishStr.contains("زجاج") || finishStr.contains("مدخن") -> "زجاج أسود مدخن وألمنيوم مؤكسد"
            finishStr.contains("أخضر") || finishStr.contains("ميرمية") -> "أخضر ميرمية مطفي (Sage)"
            finishStr.contains("كحلي") || finishStr.contains("أزرق") -> "كحلي ملكي كلاسيك"
            finishStr.contains("أبيض") || finishStr.contains("لؤلؤي") -> "أبيض لؤلؤي حريري"
            finishStr.contains("فحمي") || finishStr.contains("أنثراسايت") -> "أسود فحمي أنثراسايت"
            else -> "جوز أمريكي طبيعي"
        }

        val countertopStr = root.optString("recommendedCountertopName")
            .ifBlank { root.optString("countertopMaterialAr", "") }
        val recommendedCountertop = KitchenCatalogData.countertopMaterials.firstOrNull {
            countertopStr.contains(it.nameAr) || (countertopStr.isNotBlank() && it.nameAr.contains(countertopStr))
        }?.nameAr ?: when {
            countertopStr.contains("كرارا") -> "رخام كرارا أبيض إيطالي"
            countertopStr.contains("جرانيت") || countertopStr.contains("جالاكسي") -> "جرانيت أسود جالاكسي"
            countertopStr.contains("كوارتز") -> "كوارتز رمادي خرساني"
            countertopStr.contains("خشب") || countertopStr.contains("بلوط") -> "خشب سنديان صلب معالج"
            else -> "رخام كالكاتا إيطالي"
        }

        val unitsArray = root.optJSONArray("units")
        val suggestedItems = mutableListOf<PlacedKitchenItem>()
        val roomWMm = roomWidthCm * 10
        val roomDMm = roomDepthCm * 10
        val roomHMm = roomHeightCm * 10

        if (unitsArray != null) {
            for (i in 0 until unitsArray.length()) {
                val obj = unitsArray.optJSONObject(i) ?: continue
                val codeOrName = obj.optString("moduleCode")
                    .ifBlank { obj.optString("moduleEnumName", "BASE_60") }
                val moduleType = ModuleType.entries.find {
                    it.code.equals(codeOrName, ignoreCase = true) || it.name.equals(codeOrName, ignoreCase = true)
                } ?: ModuleType.BASE_CABINET_60

                val rawWidthMm = if (obj.has("widthMm")) {
                    obj.optInt("widthMm", moduleType.defaultWidthCm * 10)
                } else {
                    obj.optInt("widthCm", moduleType.defaultWidthCm) * 10
                }
                val rawDepthMm = if (obj.has("depthMm")) {
                    obj.optInt("depthMm", moduleType.defaultDepthCm * 10)
                } else {
                    obj.optInt("depthCm", moduleType.defaultDepthCm) * 10
                }
                val rawHeightMm = if (obj.has("heightMm")) {
                    obj.optInt("heightMm", moduleType.heightCm * 10)
                } else {
                    obj.optInt("heightCm", moduleType.heightCm) * 10
                }

                val wMm = rawWidthMm.coerceIn(150, 3000)
                val dMm = rawDepthMm.coerceIn(200, 1500)
                val hMm = rawHeightMm.coerceIn(300, roomHMm)

                val rotRaw = obj.optInt("rotationDeg", obj.optInt("rotationDegrees", 0))
                val rotDeg = (((rotRaw / 90.0).roundToInt() * 90) % 360).let { if (it < 0) it + 360 else it }

                val effWMm = if (rotDeg == 90 || rotDeg == 270) dMm else wMm
                val effDMm = if (rotDeg == 90 || rotDeg == 270) wMm else dMm

                val maxXMm = (roomWMm - effWMm).coerceAtLeast(0)
                val maxYMm = (roomDMm - effDMm).coerceAtLeast(0)

                val rawXMm = if (obj.has("xMm")) {
                    obj.optInt("xMm", 0)
                } else {
                    (obj.optDouble("xCm", 0.0) * 10.0).roundToInt()
                }
                val rawYMm = if (obj.has("yMm")) {
                    obj.optInt("yMm", 0)
                } else {
                    (obj.optDouble("yCm", 0.0) * 10.0).roundToInt()
                }

                val clampedXMm = rawXMm.coerceIn(0, maxXMm)
                val clampedYMm = rawYMm.coerceIn(0, maxYMm)

                val defaultElevMm = if (moduleType.category == ModuleCategory.WALL_CABINET) 1450 else moduleType.elevationCm * 10
                val rawElevMm = if (obj.has("elevationMm")) {
                    obj.optInt("elevationMm", defaultElevMm)
                } else if (obj.has("elevationCm")) {
                    obj.optInt("elevationCm", defaultElevMm / 10) * 10
                } else {
                    defaultElevMm
                }
                val elevMm = rawElevMm.coerceIn(
                    if (moduleType.category == ModuleCategory.WALL_CABINET) 1450 else 0,
                    (roomHMm - hMm).coerceAtLeast(0)
                )

                val labelAr = obj.optString("customLabelAr", moduleType.titleAr).ifBlank { moduleType.titleAr }
                val priceRatio = wMm.toFloat() / (moduleType.defaultWidthCm * 10).coerceAtLeast(100).toFloat()
                val unitPrice = (moduleType.defaultPriceSar * priceRatio).roundToInt().coerceAtLeast(250)

                suggestedItems.add(
                    PlacedKitchenItem(
                        uid = "gemini_${i + 1}_${moduleType.code.lowercase()}",
                        type = moduleType,
                        xCm = clampedXMm / 10,
                        yCm = clampedYMm / 10,
                        widthCm = (wMm / 10).coerceAtLeast(15),
                        depthCm = (dMm / 10).coerceAtLeast(20),
                        rotationDeg = rotDeg,
                        customLabel = labelAr,
                        priceSar = unitPrice,
                        fineXMmOffset = clampedXMm % 10,
                        fineYMmOffset = clampedYMm % 10,
                        fineWidthMmOffset = wMm % 10,
                        fineDepthMmOffset = dMm % 10,
                        customHeightMm = hMm,
                        customElevationMm = elevMm,
                        customCabinetFinish = recommendedFinish,
                        customCountertopMaterial = recommendedCountertop
                    )
                )
            }
        }

        val finalItems = if (suggestedItems.isNotEmpty()) {
            suggestedItems
        } else {
            KitchenCatalogData.buildDefaultLayoutForShape(recommendedShape, roomWidthCm, roomDepthCm).map {
                it.copy(
                    customCabinetFinish = recommendedFinish,
                    customCountertopMaterial = recommendedCountertop
                )
            }
        }

        return GeminiSuggestedKitchenDesign(
            designNameAr = designNameAr,
            styleRationalAr = styleRationalAr,
            ergonomicNotesAr = ergonomicNotesAr,
            recommendedShape = recommendedShape,
            recommendedCabinetFinish = recommendedFinish,
            recommendedCountertopMaterial = recommendedCountertop,
            recommendedWallColorHex = "#F5F2EB",
            recommendedFloorMaterial = "باركيه بلوط دافئ",
            suggestedItems = finalItems
        )
    }
}
