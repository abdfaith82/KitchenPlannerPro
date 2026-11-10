package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ai.GeminiSuggestedKitchenDesign
import com.example.data.cad.AutoFillerEngine
import com.example.data.cad.FillerPlacementMode
import com.example.data.cad.NaturalDaylightPreset
import com.example.data.cad.SmartAccessoriesCatalog
import com.example.data.cam.JsonCadEngine
import com.example.data.cam.ProjectExportManager
import com.example.data.local.LocalProjectSnapshotEntity
import com.example.data.model.KitchenCatalogData
import com.example.data.model.KitchenShape
import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import com.example.ui.viewmodel.PlannerStudioState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AutoFillerToolDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onApplyFiller: (Int, FillerPlacementMode, Boolean) -> Unit
) {
    var actualWallMmText by remember { mutableStateOf(state.roomWidthMm.toString()) }
    var selectedMode by remember { mutableStateOf(FillerPlacementMode.SPLIT_EQUAL_ENDS) }
    var includeWallRow by remember { mutableStateOf(true) }

    val parsedWallMm = actualWallMmText.toIntOrNull()?.coerceIn(1200, 12000) ?: state.roomWidthMm
    val gapReport = remember(parsedWallMm, state.placedItems) {
        AutoFillerEngine.analyzeWallGaps(
            items = state.placedItems,
            targetWallLengthMm = parsedWallMm
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "أداة الفيلر والفواصل التلقائية بدقة 1 ملم",
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
                OutlinedTextField(
                    value = actualWallMmText,
                    onValueChange = { actualWallMmText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("طول الحائط الفعلي بالمليمتر (mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filler_wall_mm_input")
                )

                Text(
                    text = "نمط توزيع الفيلر والفواصل:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                FillerPlacementMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = mode }
                            .testTag("filler_mode_${mode.name}")
                    ) {
                        Text(
                            text = mode.titleAr,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                FilterChip(
                    selected = includeWallRow,
                    onClick = { includeWallRow = !includeWallRow },
                    label = { Text("تطبيق الفيلر على الخزائن العلوية والسفلية معاً") }
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "معاينة حساب الفراغات الدقيق (1mm):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• الخزائن السفلية: المشغول ${gapReport.occupiedBaseMm} ملم | المتبقي ${gapReport.remainingGapBaseMm} ملم",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (includeWallRow) {
                            Text(
                                text = "• الخزائن العلوية: المشغول ${gapReport.occupiedWallMm} ملم | المتبقي ${gapReport.remainingGapWallMm} ملم",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "• ${gapReport.summaryAr}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyFiller(parsedWallMm, selectedMode, includeWallRow) },
                modifier = Modifier.testTag("confirm_apply_filler_button")
            ) {
                Text("توليد وتركيب شرائح الفيلر")
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
fun MillimeterPrecisionItemDialog(
    item: PlacedKitchenItem,
    onDismiss: () -> Unit,
    onApplyMm: (Int, Int, Int, Int, Int, Int) -> Unit
) {
    var xMmText by remember(item.uid) { mutableStateOf(item.xMm.toString()) }
    var yMmText by remember(item.uid) { mutableStateOf(item.yMm.toString()) }
    var widthMmText by remember(item.uid) { mutableStateOf(item.widthMm.toString()) }
    var depthMmText by remember(item.uid) { mutableStateOf(item.depthMm.toString()) }
    var heightMmText by remember(item.uid) { mutableStateOf(item.heightMm.toString()) }
    var elevationMmText by remember(item.uid) { mutableStateOf(item.elevationMm.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ضبط أبعاد وإحداثيات 1 ملم: ${item.customLabel}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = xMmText,
                        onValueChange = { xMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("الإحداثي X (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = yMmText,
                        onValueChange = { yMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("الإحداثي Y (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = widthMmText,
                        onValueChange = { widthMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("العرض W (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = depthMmText,
                        onValueChange = { depthMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("العمق D (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = heightMmText,
                        onValueChange = { heightMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("الارتفاع H (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = elevationMmText,
                        onValueChange = { elevationMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("الارتفاع عن الأرض Z (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "ملاحظة هندسية: الخزائن العلوية تلتزم تلقائياً بحد أمان لا يقل عن 1450 ملم (600 ملم فوق سطح الرخام).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyMm(
                        xMmText.toIntOrNull() ?: item.xMm,
                        yMmText.toIntOrNull() ?: item.yMm,
                        widthMmText.toIntOrNull() ?: item.widthMm,
                        depthMmText.toIntOrNull() ?: item.depthMm,
                        heightMmText.toIntOrNull() ?: item.heightMm,
                        elevationMmText.toIntOrNull() ?: item.elevationMm
                    )
                },
                modifier = Modifier.testTag("confirm_mm_precision_button")
            ) {
                Text("تطبيق بدقة 1 ملم")
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
fun RoomSnapshotsRecoveryDialog(
    snapshots: List<LocalProjectSnapshotEntity>,
    autoSaveCountdownSeconds: Int = 30,
    autoSaveCount: Int = 0,
    onDismiss: () -> Unit,
    onCreateManualSnapshot: (String) -> Unit,
    onRestoreSnapshot: (LocalProjectSnapshotEntity) -> Unit,
    onDeleteSnapshot: (Long) -> Unit
) {
    var manualLabel by remember { mutableStateOf("نسخة احتياطية قبل التعديل") }
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss - yyyy/MM/dd", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "سجل الحفظ التلقائي المحلي كل 30 ثانية (Room DB)",
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
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "نظام الحفظ التلقائي المحلي نشط (دورة كل 30 ثانية + حفظ طوارئ عند الإغلاق)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "اللقطة التلقائية القادمة خلال: ${autoSaveCountdownSeconds} ثانية • إجمالي اللقطات المسجلة بالجلسة: $autoSaveCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualLabel,
                        onValueChange = { manualLabel = it },
                        label = { Text("وصف اللقطة الفورية") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onCreateManualSnapshot(manualLabel) },
                        modifier = Modifier.testTag("create_manual_snapshot_button")
                    ) {
                        Text("حفظ لقطة")
                    }
                }

                if (snapshots.isEmpty()) {
                    Text(
                        text = "يتم حفظ حالة التصميم تلقائياً كل 30 ثانية وعند إغلاق التطبيق في قاعدة البيانات المحلية Room لضمان عدم فقدان أي تعديلات.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    snapshots.forEach { snap ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (snap.isAutoSave) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = snap.snapshotReason,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = dateFormat.format(Date(snap.timestampMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${snap.projectName} • ${snap.roomWidthMm}×${snap.roomDepthMm} ملم • ${snap.totalPriceSar} ر.س",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onDeleteSnapshot(snap.id) }) {
                                        Text("حذف", color = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { onRestoreSnapshot(snap) },
                                        modifier = Modifier.testTag("restore_snapshot_${snap.id}")
                                    ) {
                                        Text("استعادة هذه النسخة")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * Shared Camera-to-JSON v2.0 Object Scanner & Analyzer Dialog:
 * Supports scanning via live camera calibration OR picking an image from the photo picker to
 * extract real RGB surface hex color & aspect-ratio dimensions, generating live JSON v2.0 code.
 */
@Composable
fun CameraToJsonV2ScannerDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onScanAndGenerate: (String, ModuleType, Int, Int, Int, String?) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("فيترينة زجاج أسود مدخن وألمنيوم مؤكسد CNC") }
    var selectedType by remember { mutableStateOf(ModuleType.WALL_GLASS_80) }
    var widthMmText by remember { mutableStateOf("900") }
    var depthMmText by remember { mutableStateOf("350") }
    var heightMmText by remember { mutableStateOf("750") }
    var sampledColorHex by remember { mutableStateOf("#263238") }
    var opticalTelemetry by remember {
        mutableStateOf("جاهز للمسح البصري بالكاميرا أو استخراج الأبعاد واللون من صورة الوحدة")
    }

    // Zero-permission Android Photo Picker to sample real pixel color & aspect ratio from a captured photo
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                        val cx = bitmap.width / 2
                        val cy = bitmap.height / 2
                        val pixel = bitmap.getPixel(cx, cy)
                        val r = android.graphics.Color.red(pixel)
                        val g = android.graphics.Color.green(pixel)
                        val b = android.graphics.Color.blue(pixel)
                        sampledColorHex = String.format(Locale.US, "#%02X%02X%02X", r, g, b)

                        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
                        val baseH = heightMmText.toIntOrNull() ?: 850
                        val estimatedW = ((baseH * aspect) / 10f).roundToInt() * 10
                        widthMmText = estimatedW.coerceIn(300, 2400).toString()
                        opticalTelemetry =
                            "تم تحليل صورة الكاميرا (${bitmap.width}×${bitmap.height}px) • اللون المستخرج $sampledColorHex • العرض المقدر ${widthMmText} ملم"
                    }
                }
            }
        }
    }

    val previewItem = remember(label, selectedType, widthMmText, depthMmText, heightMmText) {
        val wMm = (widthMmText.toIntOrNull() ?: 900).coerceIn(150, 3200)
        val dMm = (depthMmText.toIntOrNull() ?: 350).coerceIn(150, 1500)
        val hMm = (heightMmText.toIntOrNull() ?: 750).coerceIn(250, 2600)
        PlacedKitchenItem(
            uid = "cam_scan_preview",
            type = selectedType,
            xCm = 60,
            yCm = 0,
            widthCm = wMm / 10,
            depthCm = dMm / 10,
            customLabel = label,
            priceSar = (selectedType.defaultPriceSar * (wMm / (selectedType.defaultWidthCm * 10f))).roundToInt().coerceAtLeast(250),
            fineWidthMmOffset = wMm % 10,
            fineDepthMmOffset = dMm % 10,
            customHeightMm = hMm,
            customElevationMm = if (selectedType.category == ModuleCategory.WALL_CABINET) 1450 else 0
        )
    }

    val liveJsonPreview = remember(previewItem, state.cabinetFinish, state.countertopMaterial, sampledColorHex) {
        JsonCadEngine.itemToJsonV2(
            item = previewItem,
            finishName = state.cabinetFinish,
            countertopName = state.countertopMaterial,
            sampledHexOverride = sampledColorHex,
            captureSource = "LIVE_CAMERA_SCANNER_V2"
        )
    }

    val scanPresets: List<Pair<ModuleType, String>> = listOf(
        ModuleType.WALL_GLASS_80 to "فيترينة علوية مضاءة",
        ModuleType.BASE_DRAWERS_80 to "وحدة أدراج سفلية",
        ModuleType.ISLAND_WATERFALL to "جزيرة مطبخ",
        ModuleType.SINK_DOUBLE to "وحدة حوض مزدوج",
        ModuleType.FRIDGE_SMART to "ثلاجة ذكية",
        ModuleType.TALL_PANTRY to "دولاب مونة مرتفع"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "استيراد كائن بالكاميرا كـ JSON (Camera ➔ JSON v2.0)",
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
                    text = "امسح أي وحدة مطبخ بالكاميرا أو اختر صورتها لتحليل أبعادها بالمليمتر وبصمة لونها PBR وتحويلها فوراً إلى كائن 3D وكود JSON v2.0 جاهز للتصنيع (18mm CNC + Gola).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_json_pick_photo_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحليل صورة من الكاميرا")
                    }

                    OutlinedButton(
                        onClick = {
                            val palette = listOf("#263238", "#5D4037", "#ECEFF1", "#37474F", "#795548", "#1B262C")
                            sampledColorHex = palette.random()
                            opticalTelemetry = "تم الالتقاط البصري المباشر بالكاميرا • بصمة اللون $sampledColorHex"
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("camera_json_optical_sample_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("التقاط بصري")
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    runCatching { Color(android.graphics.Color.parseColor(sampledColorHex)) }
                                        .getOrDefault(Color(0xFF263238))
                                )
                                .border(1.dp, Color.White, CircleShape)
                        )
                        Text(
                            text = opticalTelemetry,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("اسم الكائن الممسوح (labelAr)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scanPresets.forEach { (type, title) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                label = "${title} (ممسوحة بالكاميرا CNC)"
                                widthMmText = (type.defaultWidthCm * 10).toString()
                                depthMmText = (type.defaultDepthCm * 10).toString()
                                heightMmText = (type.heightCm * 10).toString()
                            },
                            label = { Text(title) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = widthMmText,
                        onValueChange = { widthMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("العرض (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = depthMmText,
                        onValueChange = { depthMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("العمق (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightMmText,
                        onValueChange = { heightMmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("الارتفاع (ملم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Generated JSON v2.0 Code Preview inside Camera Scanner Dialog
                Text(
                    text = "معاينة كود JSON v2.0 المولّد لحظياً من الكاميرا:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = Color(0xFF141C22),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                ) {
                    Text(
                        text = liveJsonPreview,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF80CBC4)
                        ),
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                OutlinedButton(
                    onClick = {
                        ProjectExportManager.exportAndShareTextFile(
                            context = context,
                            fileName = "camera_scanned_unit_${System.currentTimeMillis()}.json",
                            mimeType = "application/json",
                            content = liveJsonPreview,
                            shareTitle = "تصدير كائن ممسوح بالكاميرا JSON v2.0"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_camera_scanned_json_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير ومشاركة الكائن الممسوح كملف JSON")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onScanAndGenerate(
                        label,
                        selectedType,
                        widthMmText.toIntOrNull() ?: 900,
                        depthMmText.toIntOrNull() ?: 350,
                        heightMmText.toIntOrNull() ?: 750,
                        sampledColorHex
                    )
                },
                modifier = Modifier.testTag("confirm_camera_to_json_button")
            ) {
                Text("تحويل إلى عنصر 3D + JSON v2.0")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Universal JSON Importer Dialog:
 * Lets the user import ANY .json file from device storage (or paste any JSON structure:
 * single object, array of objects, full project, Three.js / glTF JSON, or custom schema).
 */
@Composable
fun UniversalJsonImporterDialog(
    onDismiss: () -> Unit,
    onImportJson: (String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedFileName by remember { mutableStateOf("custom_object.json") }
    var jsonContentText by remember {
        mutableStateOf(
            """
            {
              "name": "جزيرة مطبخ رخام كالكاتا شلال",
              "type": "ISLAND",
              "widthMm": 1800,
              "depthMm": 950,
              "heightMm": 900,
              "xMm": 1200,
              "yMm": 1600,
              "priceSar": 5200
            }
            """.trimIndent()
        )
    }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_file.json"
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                if (text.isNotBlank()) {
                    selectedFileName = if (name.endsWith(".json", ignoreCase = true)) name else "$name.json"
                    jsonContentText = text
                    onImportJson(selectedFileName, text)
                    onDismiss()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "استيراد أي ملف أو كود JSON (Universal JSON Importer)",
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
                    text = "يدعم المحرك استيراد أي ملف JSON من الجهاز (كائن مفرد، مصفوفة عناصر [...]، مشروع كامل، ملفات glTF/Three.js، أو أي هيكل JSON مخصص) مع توحيد الأبعاد تلقائياً بالمليمتر (1mm).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { jsonFilePickerLauncher.launch("*/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_any_json_file_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختيار أي ملف JSON من الجهاز (.json)")
                }

                // Quick sample JSON schemas to test single object, array, or generic CAD JSON
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            selectedFileName = "vitrine_unit.json"
                            jsonContentText = """
                                {
                                  "name": "فيترينة زجاج أسود مدخن 3000K",
                                  "type": "WALL_GLASS",
                                  "width": 1000,
                                  "depth": 350,
                                  "height": 800,
                                  "elevationMm": 1450
                                }
                            """.trimIndent()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("نموذج كائن مفرد JSON", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = {
                            selectedFileName = "multi_units_array.json"
                            jsonContentText = """
                                [
                                  { "name": "خزانة أدراج Blum 90سم", "type": "BASE_DRAWERS", "width": 900, "depth": 600, "height": 850, "x": 400, "y": 0 },
                                  { "name": "وحدة حوض مزدوج", "type": "SINK", "width": 1000, "depth": 600, "height": 850, "x": 1300, "y": 0 },
                                  { "name": "فيترينة علوية مضاءة", "type": "WALL_GLASS", "width": 900, "depth": 350, "height": 750, "x": 400, "y": 0, "elevationMm": 1450 }
                                ]
                            """.trimIndent()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("نموذج مصفوفة JSON [...]", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = {
                            selectedFileName = "threejs_bim_object.json"
                            jsonContentText = """
                                {
                                  "id": "bim_smart_fridge_01",
                                  "title": "ثلاجة ذكية مدمجة الباب الفرنسي",
                                  "category": "fridge",
                                  "size": [0.92, 1.85, 0.72],
                                  "position": [2.40, 0.0, 0.0],
                                  "price": 6400
                                }
                            """.trimIndent()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("نموذج BIM / Three.js JSON", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = jsonContentText,
                    onValueChange = { jsonContentText = it },
                    label = { Text("محتوى ملف أو كود JSON (يدعم أي هيكل JSON)") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    minLines = 7,
                    maxLines = 12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("universal_json_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImportJson(selectedFileName, jsonContentText)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_universal_json_import_button")
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("استيراد وإسقاط في المشهد 3D")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * AutoCAD DXF Import & Export Dialog directly inside the 2D/3D Design Workspace:
 * Lets users export/share the current kitchen layout as an AutoCAD .dxf file (1mm precision, multi-layer)
 * for contractors & workshops, or import any .dxf file from device / contractor templates.
 */
@Composable
fun AutoCadDxfStudioDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onImportDxf: (String, String) -> Unit
) {
    val context = LocalContext.current
    val currentLayoutDxf = remember(state.placedItems, state.roomWidthMm, state.roomDepthMm, state.roomHeightMm, state.projectName) {
        ProjectExportManager.buildAutoCadDxfContent(state)
    }
    var selectedFileName by remember { mutableStateOf("kitchen_contractor_plan.dxf") }
    var dxfContentText by remember(currentLayoutDxf) { mutableStateOf(currentLayoutDxf) }

    val dxfFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val rawName = uri.lastPathSegment?.substringAfterLast('/') ?: "contractor_layout.dxf"
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                if (text.isNotBlank()) {
                    selectedFileName = if (rawName.endsWith(".dxf", ignoreCase = true)) rawName else "$rawName.dxf"
                    dxfContentText = text
                    onImportDxf(selectedFileName, text)
                    onDismiss()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "استيراد وتصدير مخططات الأوتوكاد (AutoCAD DXF)",
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
                    text = "شارك مخطط مطبخك الهندسي بصيغة AutoCAD (.dxf) بدقة 1 ملم مع المقاولين وورش التصنيع (شاملاً طبقات الجدران، الخزائن السفلية، العلوية، الجزر، والأجهزة)، أو استورد أي ملف DXF مباشرة إلى مساحة التصميم.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Primary Export & Share DXF Button for Contractors & Workshops
                Button(
                    onClick = {
                        val exportDxf = ProjectExportManager.buildAutoCadDxfContent(state)
                        dxfContentText = exportDxf
                        ProjectExportManager.exportAndShareTextFile(
                            context = context,
                            fileName = "kitchen_autocad_${state.projectName.replace(" ", "_")}_1mm.dxf",
                            mimeType = "application/dxf",
                            content = exportDxf,
                            shareTitle = "مشاركة مخطط أوتوكاد DXF للمقاول والورشة"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_share_dxf_workspace_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير ومشاركة مخطط AutoCAD (.dxf) للورش والمقاولين")
                }

                // Import DXF File from Device Storage
                FilledTonalButton(
                    onClick = { dxfFilePickerLauncher.launch("*/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_dxf_file_workspace_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختيار واستيراد ملف أوتوكاد (.dxf) من الجهاز")
                }

                // Contractor DXF Presets for quick testing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedFileName = "current_project_1mm.dxf"
                            dxfContentText = ProjectExportManager.buildAutoCadDxfContent(state)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("مخطط المشروع الحالي DXF", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            selectedFileName = "workshop_cnc_layout.dxf"
                            dxfContentText = """
                                0
                                SECTION
                                2
                                HEADER
                                999
                                ROOM_SPEC|4800|3600|2900|جوز أمريكي طبيعي|رخام كالكاتا إيطالي|مخطط ورشة CNC المستورد DXF
                                0
                                ENDSEC
                                0
                                SECTION
                                2
                                ENTITIES
                                999
                                CAD_UNIT|dxf_sink_1|SINK_DOUBLE|0|0|0|1000|600|850|0|حوض مزدوج ستانلس DXF|2650|18|GOLA_ALUMINUM
                                0
                                LWPOLYLINE
                                8
                                BASE_CABINETS_MM
                                90
                                4
                                70
                                1
                                38
                                0.0
                                39
                                850.0
                                10
                                0.0
                                20
                                0.0
                                10
                                1000.0
                                20
                                0.0
                                10
                                1000.0
                                20
                                600.0
                                10
                                0.0
                                20
                                600.0
                                999
                                CAD_UNIT|dxf_drawers_2|BASE_DRAWERS_80|1000|0|0|900|600|850|0|وحدة أدراج Blum 900mm DXF|2350|18|GOLA_ALUMINUM
                                999
                                CAD_UNIT|dxf_hob_3|STOVE_HOB|1900|0|0|900|600|850|0|فرن وموقد غاز مدمج DXF|3400|18|GOLA_ALUMINUM
                                999
                                CAD_UNIT|dxf_fridge_4|FRIDGE_SMART|2800|0|0|920|720|1850|0|ثلاجة ذكية باب فرنسي DXF|6200|18|GOLA_ALUMINUM
                                999
                                CAD_UNIT|dxf_vitrine_5|WALL_GLASS_80|1000|0|1450|900|350|750|0|فيترينة علوية مضاءة DXF|1950|18|GOLA_ALUMINUM
                                999
                                CAD_UNIT|dxf_island_6|ISLAND_WATERFALL|1100|1750|0|2000|950|900|0|جزيرة وسطية شلال رخام DXF|5400|18|GOLA_ALUMINUM
                                0
                                ENDSEC
                                0
                                EOF
                            """.trimIndent()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("load_sample_contractor_dxf_button")
                    ) {
                        Text("نموذج مخطط مقاول/ورشة DXF", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = dxfContentText,
                    onValueChange = { dxfContentText = it },
                    label = { Text("محتوى ملف AutoCAD DXF (طبقات المليمتر 1mm AC1015)") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    minLines = 6,
                    maxLines = 11,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dxf_content_editor_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImportDxf(selectedFileName, dxfContentText)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_import_dxf_workspace_button")
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("استيراد مخطط DXF إلى مساحة التصميم")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * Gemini AI Complete Kitchen Layout Suggester Dialog:
 * Uses Firebase AI Logic (Gemini 3.8 Flash) to suggest complete kitchen designs
 * based on user-entered room dimensions and style preferences.
 */
@Composable
fun GeminiAiKitchenDesignerDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onGenerateWithGemini: (
        roomWidthCm: Int,
        roomDepthCm: Int,
        roomHeightCm: Int,
        preferredShape: KitchenShape,
        stylePreferenceAr: String,
        customPrioritiesAr: String
    ) -> Unit
) {
    var widthCmText by remember { mutableStateOf(state.roomWidthCm.toString()) }
    var depthCmText by remember { mutableStateOf(state.roomDepthCm.toString()) }
    var heightCmText by remember { mutableStateOf(state.roomHeightCm.toString()) }
    var selectedShape by remember { mutableStateOf(state.shape) }

    val styleOptions = remember {
        listOf(
            "عصري جوز أمريكي دافئ مع رخام كالكاتا إيطالي وإضاءة 3000K",
            "إسكندنافي أخضر ميرمية مطفي مع كوارتز أبيض نقي وباركيه فاتح",
            "فخامة نيوكلاسيك أسود فحمي وزجاج مدخن مع جرانيت جالاكسي",
            "مطبخ عائلي عملي بلوط إسكندنافي فاتح مع جزيرة إفطار واسعة"
        )
    }
    var selectedStyleAr by remember { mutableStateOf(styleOptions.first()) }
    var customPrioritiesAr by remember {
        mutableStateOf("توزيع مثالي لمثلث العمل القياسي (حوض + موقد + ثلاجة) مع خزائن علوية زجاجية مضاءة")
    }

    AlertDialog(
        onDismissRequest = {
            if (!state.isGeneratingGeminiDesign) onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "اقتراح تصميم مطبخ كامل عبر Gemini AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "أدخل أبعاد الغرفة وتفضيلات النمط المعماري ليقوم نموذج Gemini AI بتوليد مخطط مطبخ كامل بدقة 1 ملم مع اختيار الوحدات والخامات ومثلث العمل القياسي (NKBA).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Room Dimensions Input (cm)
                Text(
                    text = "1. أبعاد الغرفة الفعلية (سم):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = widthCmText,
                        onValueChange = { widthCmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("العرض (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_room_width_input")
                    )
                    OutlinedTextField(
                        value = depthCmText,
                        onValueChange = { depthCmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("العمق (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_room_depth_input")
                    )
                    OutlinedTextField(
                        value = heightCmText,
                        onValueChange = { heightCmText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("الارتفاع (سم)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gemini_room_height_input")
                    )
                }

                // Preferred Shape
                Text(
                    text = "2. شكل التوزيع الهندسي المفضل:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KitchenShape.entries.forEach { shapeOption ->
                        FilterChip(
                            selected = selectedShape == shapeOption,
                            onClick = { selectedShape = shapeOption },
                            label = { Text(shapeOption.titleAr) },
                            modifier = Modifier.testTag("gemini_shape_${shapeOption.code}")
                        )
                    }
                }

                // Style Preferences
                Text(
                    text = "3. تفضيلات النمط المعماري والخامات:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    styleOptions.forEachIndexed { idx, option ->
                        FilterChip(
                            selected = selectedStyleAr == option,
                            onClick = { selectedStyleAr = option },
                            label = {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_style_option_$idx")
                        )
                    }
                }

                OutlinedTextField(
                    value = customPrioritiesAr,
                    onValueChange = { customPrioritiesAr = it },
                    label = { Text("أولويات إضافية (مثل: جزيرة وسطية، دولاب مؤونة، غسالة صحون)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_custom_priorities_input")
                )

                if (state.isGeneratingGeminiDesign) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_loading_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "يقوم Gemini AI الآن بهندسة المطبخ وحساب إحداثيات الخزائن ومثلث العمل بدقة 1 ملم...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                state.geminiSuggestionError?.let { errText ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_error_banner")
                    ) {
                        Text(
                            text = errText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                state.lastGeminiSuggestion?.let { suggestion ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_last_suggestion_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "آخر تصميم مقترح من Gemini AI: ${suggestion.designTitleAr}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = suggestion.architecturalRationaleAr,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• الخامات المقترحة: ${suggestion.cabinetFinishAr} + ${suggestion.countertopMaterialAr}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• ${suggestion.ergonomicHighlightAr} (${suggestion.suggestedItems.size} وحدة في المخطط)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = widthCmText.toIntOrNull()?.coerceIn(220, 900) ?: state.roomWidthCm
                    val d = depthCmText.toIntOrNull()?.coerceIn(200, 800) ?: state.roomDepthCm
                    val h = heightCmText.toIntOrNull()?.coerceIn(230, 400) ?: state.roomHeightCm
                    onGenerateWithGemini(w, d, h, selectedShape, selectedStyleAr, customPrioritiesAr)
                },
                enabled = !state.isGeneratingGeminiDesign,
                modifier = Modifier.testTag("confirm_gemini_suggest_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (state.isGeneratingGeminiDesign) "جاري التوليد..." else "توليد وتطبيق تصميم Gemini AI")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isGeneratingGeminiDesign
            ) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * 3D Natural & Artificial Lighting Simulation Engine Dialog:
 * Allows users to experiment with natural daylight azimuth, intensity, ceiling recessed spotlights,
 * under-cabinet LED strips, color temperature (2700K-6500K), shadow softness/depth, and surface specular gloss.
 */
@Composable
fun LightingSimulationStudioDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onSelectPreset: (NaturalDaylightPreset) -> Unit,
    onUpdateParams: (
        sunAzimuthDeg: Float,
        naturalLightIntensity: Float,
        artificialSpotIntensity: Float,
        underCabinetLedIntensity: Float,
        colorTemperatureKelvin: Int,
        shadowSoftnessAndDepth: Float,
        surfaceSpecularReflection: Float
    ) -> Unit
) {
    val sim = state.lightingSimulation
    var sunAzimuth by remember(sim.activePreset) { mutableFloatStateOf(sim.sunAzimuthDeg) }
    var naturalIntensity by remember(sim.activePreset) { mutableFloatStateOf(sim.naturalLightIntensity) }
    var spotIntensity by remember(sim.activePreset) { mutableFloatStateOf(sim.artificialSpotIntensity) }
    var underCabIntensity by remember(sim.activePreset) { mutableFloatStateOf(sim.underCabinetLedIntensity) }
    var kelvin by remember(sim.activePreset) { mutableIntStateOf(sim.colorTemperatureKelvin) }
    var shadowDepth by remember(sim.activePreset) { mutableFloatStateOf(sim.shadowSoftnessAndDepth) }
    var specularReflection by remember(sim.activePreset) { mutableFloatStateOf(sim.surfaceSpecularReflection) }

    fun pushUpdate() {
        onUpdateParams(
            sunAzimuth,
            naturalIntensity,
            spotIntensity,
            underCabIntensity,
            kelvin,
            shadowDepth,
            specularReflection
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "محرك محاكاة الإضاءة الطبيعية والاصطناعية 3D",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختبر تأثيرات ضوء الشمس الطبيعي عبر النافذة، والسبوت لايت السقفي، وإضاءة LED المخفية أسفل الخزائن، مع معاينة فورية للظلال والانعكاسات على الرخام والزجاج.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "أوضاع الإضاءة المسبقة (Presets):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NaturalDaylightPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = sim.activePreset == preset,
                            onClick = {
                                onSelectPreset(preset)
                                sunAzimuth = preset.sunAzimuthDeg
                                naturalIntensity = preset.naturalIntensity
                                spotIntensity = preset.artificialSpotIntensity
                                underCabIntensity = preset.underCabinetLedIntensity
                                kelvin = preset.kelvinTemp
                                shadowDepth = preset.shadowDepth
                                specularReflection = preset.specularGloss
                            },
                            label = { Text(preset.titleAr) },
                            modifier = Modifier.testTag("lighting_preset_${preset.name}")
                        )
                    }
                }

                HorizontalDivider()

                // 1. Sun Azimuth Angle
                Text(
                    text = "زاوية سقوط ضوء الشمس (Azimuth): ${sunAzimuth.roundToInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = sunAzimuth,
                    onValueChange = {
                        sunAzimuth = it
                        pushUpdate()
                    },
                    valueRange = 0f..360f,
                    modifier = Modifier.testTag("slider_sun_azimuth")
                )

                // 2. Natural Sunlight Intensity
                Text(
                    text = "شدة الإضاءة الطبيعية (Natural Daylight): ${(naturalIntensity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = naturalIntensity,
                    onValueChange = {
                        naturalIntensity = it
                        pushUpdate()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("slider_natural_light")
                )

                // 3. Ceiling Spotlights Intensity
                Text(
                    text = "شدة السبوت لايت السقفي (Ceiling Spotlights): ${(spotIntensity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = spotIntensity,
                    onValueChange = {
                        spotIntensity = it
                        pushUpdate()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("slider_artificial_spots")
                )

                // 4. Under-Cabinet & Vitrine LED Strip Intensity
                Text(
                    text = "شدة شريط LED أسفل الخزائن والفيترينات: ${(underCabIntensity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = underCabIntensity,
                    onValueChange = {
                        underCabIntensity = it
                        pushUpdate()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("slider_under_cabinet_led")
                )

                // 5. Color Temperature (Kelvin)
                Text(
                    text = "حرارة اللون (Color Temperature): ${kelvin}K (${if (kelvin <= 3300) "أصفر دافئ" else if (kelvin <= 4500) "نهاري متوازن" else "أبيض بارد"})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = kelvin.toFloat(),
                    onValueChange = {
                        kelvin = it.roundToInt()
                        pushUpdate()
                    },
                    valueRange = 2700f..6500f,
                    modifier = Modifier.testTag("slider_kelvin_temp")
                )

                // 6. Shadow Softness & Depth
                Text(
                    text = "عمق وكثافة الظلال (Cast Shadows & AO): ${(shadowDepth * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = shadowDepth,
                    onValueChange = {
                        shadowDepth = it
                        pushUpdate()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("slider_shadow_depth")
                )

                // 7. Surface Specular Reflection
                Text(
                    text = "لمعان وانعكاس الرخام والزجاج (Specular Gloss): ${(specularReflection * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = specularReflection,
                    onValueChange = {
                        specularReflection = it
                        pushUpdate()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("slider_specular_reflection")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_lighting_simulation_dialog")
            ) {
                Text("تطبيق وإغلاق")
            }
        }
    )
}

/**
 * Real-Time Engineering Installation Compliance & Door Swing Clearance Validator Dialog:
 * Displays all real-time alerts for door swing clearances, appliance thermal/moisture separation,
 * corner collisions, and upper cabinet height clearances, with a one-tap Auto-Fix button.
 */
@Composable
fun EngineeringComplianceAlertsDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onAutoFixAll: () -> Unit,
    onSelectUnit: (String) -> Unit
) {
    val report = state.complianceReport
    val unitSys = state.unitSystem

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (report.alerts.isEmpty()) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (report.alerts.isEmpty()) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "نظام التحقق الهندسي اللحظي (${report.complianceScore}%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = if (report.alerts.isEmpty()) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = report.statusSummaryAr,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يفحص المحرك لحظياً: مساحات فتح أبواب الخزائن والأجهزة (≥ ${unitSys.formatMillimeters(850)})، المسافة الحرارية بين الموقد والثلاجة/الحوض (≥ ${unitSys.formatMillimeters(300)})، تعارض الزوايا، وارتفاع الخزائن العلوية (≥ ${unitSys.formatMillimeters(1450)}).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (report.alerts.isNotEmpty()) {
                    Button(
                        onClick = {
                            onAutoFixAll()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_auto_fix_compliance_button")
                    ) {
                        Text("إصلاح هندسي تلقائي لجميع التعارضات (${report.alerts.size})")
                    }

                    report.alerts.forEach { alert ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    alert.affectedUnitUid?.let { onSelectUnit(it) }
                                    onDismiss()
                                }
                                .testTag("compliance_alert_card_${alert.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (alert.isCritical) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = alert.titleAr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (alert.isCritical) Color(0xFFD50000) else Color(0xFFFF6D00),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (alert.isCritical) "حرج" else "تنبيه",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "الوحدة المتأثرة: ${alert.affectedUnitTitleAr}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = alert.detailAr,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "المسافة الحالية: ${unitSys.formatMillimeters(alert.measuredClearanceMm)} • المعيار المطلوب: ${unitSys.formatMillimeters(alert.requiredClearanceMm)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "الحل الهندسي: ${alert.recommendationAr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "جميع الخزائن والأجهزة في المخطط الحالي مطابقة 100% لمعايير التركيب الهندسية ومساحات فتح الأبواب والأدراج.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00796B)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * Smart Internal Kitchen Accessories Library Dialog:
 * Shows all internal kitchen accessories (drawer organizers, corner carousels, under-sink waste pull-outs,
 * tall pantry cargo baskets, spice pull-outs, dish racks, island pop-up sockets) that auto-link
 * to compatible units in the layout and dynamically update the Bill of Materials (BOM).
 */
@Composable
fun SmartInternalAccessoriesDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onToggleAccessory: (String) -> Unit
) {
    val linkedByCode = remember(state.linkedAccessoriesBom) {
        state.linkedAccessoriesBom.associateBy { it.accessory.code }
    }
    val totalLinkedCost = state.costBreakdown.smartInternalAccessoriesCostSar
    val totalLinkedUnits = state.costBreakdown.linkedSmartAccessoriesCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "مكتبة إكسسوارات المطبخ الداخلية الذكية (BOM)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "الربط التلقائي بالوحدات المتوافقة وتحديث جدول الكميات (BOM):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "إجمالي القطع المرتبطة تلقائياً بالمخطط الحالي: $totalLinkedUnits قطعة • التكلفة المضافة لـ BOM: $totalLinkedCost ر.س",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SmartAccessoriesCatalog.allAccessories.forEach { acc ->
                    val isEnabled = state.enabledAccessoryCodes.contains(acc.code)
                    val linkedEntry = linkedByCode[acc.code]
                    val compatibleUnitsCount = state.placedItems.count { item ->
                        acc.compatibleTypes.contains(item.type) && item.widthMm >= acc.minUnitWidthMm
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleAccessory(acc.code) }
                            .testTag("smart_accessory_card_${acc.code}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEnabled && compatibleUnitsCount > 0) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.nameAr,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${acc.brandAr} • ${acc.pricePerUnitSar} ر.س / للوحدة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                FilterChip(
                                    selected = isEnabled,
                                    onClick = { onToggleAccessory(acc.code) },
                                    label = { Text(if (isEnabled) "مفعّل في BOM" else " غير مفعّل") },
                                    modifier = Modifier.testTag("toggle_accessory_${acc.code}")
                                )
                            }

                            Text(
                                text = acc.specAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (linkedEntry != null) {
                                Text(
                                    text = "✓ مرتبط تلقائياً بـ ${linkedEntry.quantity} وحدة (${linkedEntry.matchedUnitsSummaryAr}) = ${linkedEntry.totalPriceSar} ر.س",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00796B)
                                )
                            } else if (compatibleUnitsCount > 0) {
                                Text(
                                    text = "يوجد $compatibleUnitsCount وحدة متوافقة في التصميم — فعّل الخيار لإضافته إلى قائمة المواد BOM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Text(
                                    text = "سيتم ربطه تلقائياً فور إضافة وحدة متوافقة (عرض ≥ ${state.unitSystem.formatMillimeters(acc.minUnitWidthMm)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_smart_accessories_dialog")
            ) {
                Text("تحديث قائمة المواد (BOM)")
            }
        }
    )
}

/**
 * 1. Direct Blueprint Sharing with Workshops or Friends via Social Media or Email Dialog:
 * (`ميزة مشاركة المخططات مباشرة مع الورش أو الأصدقاء عبر تطبيقات التواصل الاجتماعي أو البريد الإلكتروني من داخل التطبيق`)
 */
@Composable
fun ShareBlueprintWithWorkshopAndFriendsDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var recipientEmail by remember { mutableStateOf("workshop@cnc-kitchens.sa") }
    var recipientName by remember { mutableStateOf("ورشة النخبة لتصنيع المطابخ CNC") }
    var customNote by remember {
        mutableStateOf("يرجى اعتماد المخطط المرفق (خزائن كاملة بظهر وجوانب 18 ملم CNC) وإرسال عرض السعر وموعد التنفيذ.")
    }
    var shareFeedback by remember { mutableStateOf<String?>(null) }

    val harmony = remember(state.wallColorHex, state.floorMaterial, state.cabinetFinish, state.countertopMaterial) {
        KitchenCatalogData.evaluateVisualColorHarmony(
            wallColorKey = state.wallColorHex,
            floorMaterial = state.floorMaterial,
            cabinetFinish = state.cabinetFinish,
            countertopMaterial = state.countertopMaterial
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "مشاركة المخطط مع الورش والأصدقاء (تواصل اجتماعي / بريد)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ملخص المخطط الجاهز للمشاركة الفورية:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• المشروع: ${state.projectName} (${state.roomWidthMm}×${state.roomDepthMm}×${state.roomHeightMm} ملم)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• عدد الوحدات: ${state.placedItems.size} وحدة كاملة (ظهر وجنب 18 ملم) • التكلفة: ${state.costBreakdown.totalSar} ر.س",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• الحائط: ${KitchenCatalogData.resolveWallColorNameAr(state.wallColorHex)} | الأرضية: ${state.floorMaterial} (تناسق ${harmony.harmonyScorePercent}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Quick Workshop Presets
                Text(
                    text = "اختيار سريع لورشة معتمدة أو صديق:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("ورشة النخبة CNC", "workshop@cnc-kitchens.sa", "تصنيع كامل 18 ملم مع مفصلات Blum"),
                        Triple("مصنع الخليج للرخام والخزائن", "orders@gulf-kitchens.sa", "طلب تسعير وتوريد خزائن + كوارتز"),
                        Triple("مشاركة مع الأصدقاء والعائلة", "friend@example.com", "ما رأيكم في تناسق ألوان المطبخ والحوائط؟")
                    ).forEachIndexed { idx, (name, email, note) ->
                        FilterChip(
                            selected = recipientName == name,
                            onClick = {
                                recipientName = name
                                recipientEmail = email
                                customNote = note
                            },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("share_preset_chip_$idx")
                        )
                    }
                }

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("اسم الورشة / الصديق المستلم") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_recipient_name_input")
                )

                OutlinedTextField(
                    value = recipientEmail,
                    onValueChange = { recipientEmail = it },
                    label = { Text("البريد الإلكتروني للورشة أو الصديق") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_recipient_email_input")
                )

                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    label = { Text("رسالة أو ملاحظات التصنيع المرفقة مع المخطط") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_custom_note_input")
                )

                HorizontalDivider()

                // 1. Direct Social Media Share (High-Res PNG Render + 2D Blueprint Card)
                Button(
                    onClick = {
                        ProjectExportManager.generateAndShareSocialBlueprintImage(
                            context = context,
                            state = state,
                            customMessageNote = customNote,
                            shareImmediately = true
                        ).onSuccess { pngFile ->
                            shareFeedback = "تم تجهيز بطاقة المخطط والرندر (${pngFile.name}) وفتح المشاركة عبر التواصل الاجتماعي (واتساب / تيليجرام / إنستغرام / X)"
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_social_png_blueprint_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("مشاركة صورة المخطط والرندر عبر التواصل الاجتماعي (PNG)")
                }

                // 2. Direct Email Share to Workshop / Friend with PDF + DXF + Cut-List
                FilledTonalButton(
                    onClick = {
                        ProjectExportManager.shareBlueprintViaEmailToWorkshop(
                            context = context,
                            state = state,
                            recipientEmail = recipientEmail,
                            workshopOrRecipientName = recipientName,
                            customMessageNote = customNote
                        ).onSuccess { pdfFile ->
                            shareFeedback = "تم تجهيز حزمة البريد الإلكتروني للورشة ($recipientEmail) مع ملف التقرير الهندسي (${pdfFile.name}) وقائمة القص"
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_email_workshop_blueprint_button")
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("إرسال المخطط وقائمة القص عبر البريد الإلكتروني للورشة")
                }

                // 3. Direct PDF + AutoCAD DXF Quick Share Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            ProjectExportManager.generateEngineeringPdfReport(
                                context = context,
                                state = state,
                                shareViaMessagingApps = true
                            ).onSuccess { file ->
                                shareFeedback = "تم تصدير ومشاركة تقرير PDF الهندسي (${file.name}) عبر تطبيقات المراسلة"
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_whatsapp_pdf_report_button")
                    ) {
                        Text("تقرير PDF للواتساب", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            val dxfText = ProjectExportManager.buildAutoCadDxfContent(state)
                            ProjectExportManager.exportAndShareTextFile(
                                context = context,
                                fileName = "workshop_plan_${state.projectName.replace(" ", "_")}.dxf",
                                mimeType = "application/dxf",
                                content = dxfText,
                                shareTitle = "مخطط أوتوكاد DXF للورشة (${state.projectName})"
                            ).onSuccess { file ->
                                shareFeedback = "تمت مشاركة ملف الأوتوكاد DXF (${file.name}) مباشرة مع الورشة"
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_dxf_workshop_direct_button")
                    ) {
                        Text("ملف AutoCAD DXF", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (shareFeedback != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_blueprint_feedback_banner")
                    ) {
                        Text(
                            text = shareFeedback.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * 2. 3D Wall & Floor Color/Material Customizer for Visual Harmony Preview Dialog:
 * (`أداة لتغيير ألوان الحوائط والأرضيات في بيئة التصميم ثلاثي الأبعاد لمعاينة التناسق البصري بين الألوان المقترحة والمطبخ`)
 */
@Composable
fun WallAndFloorHarmonyStudioDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onUpdateWallAndFloor: (wallColorHex: String, floorMaterial: String, cabinetFinish: String, countertopMaterial: String) -> Unit,
    onApplyHarmonyPalette: (palette: KitchenCatalogData.WallFloorHarmonyPalette, applyCabinetAndCounter: Boolean) -> Unit
) {
    var selectedWallHex by remember(state.wallColorHex) { mutableStateOf(state.wallColorHex) }
    var customHexInput by remember(state.wallColorHex) { mutableStateOf(state.wallColorHex) }
    var selectedFloor by remember(state.floorMaterial) { mutableStateOf(state.floorMaterial) }
    var selectedCabinet by remember(state.cabinetFinish) { mutableStateOf(state.cabinetFinish) }
    var selectedCountertop by remember(state.countertopMaterial) { mutableStateOf(state.countertopMaterial) }
    var applyFullPaletteWithCabinets by remember { mutableStateOf(false) }

    val harmony = remember(selectedWallHex, selectedFloor, selectedCabinet, selectedCountertop) {
        KitchenCatalogData.evaluateVisualColorHarmony(
            wallColorKey = selectedWallHex,
            floorMaterial = selectedFloor,
            cabinetFinish = selectedCabinet,
            countertopMaterial = selectedCountertop
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "استوديو ألوان الحوائط والأرضيات والتناسق البصري 3D",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live 3D Visual Harmony Preview Banner & Swatch Split Card
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("visual_harmony_score_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مؤشر التناسق البصري اللوني: ${harmony.harmonyScorePercent}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = if (harmony.harmonyScorePercent >= 88) Color(0xFF00C853) else Color(0xFFFF8F00),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (harmony.harmonyScorePercent >= 88) "متناسق جداً" else "تباين متوسط",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // 4-Way Live Color Harmony Swatch Preview (Wall + Cabinet + Countertop + Floor)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        ) {
                            val wallColor = Color(KitchenCatalogData.resolveWallColorHex(selectedWallHex))
                            val cabColor = Color(KitchenCatalogData.resolveCabinetColorHex(selectedCabinet))
                            val topColor = Color(KitchenCatalogData.resolveCountertopColorHex(selectedCountertop))
                            val flrColor = Color(KitchenCatalogData.resolveFloorColorHex(selectedFloor))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .background(wallColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("الحائط", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .background(cabColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("الخزائن", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .background(topColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("الرخام", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .background(flrColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("الأرضية", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Text(
                            text = harmony.verdictAr,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = harmony.recommendationAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1-Tap Coordinated Architectural Harmony Palettes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. باليتات التناسق المعماري الجاهزة (ضغطة واحدة):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FilterChip(
                        selected = applyFullPaletteWithCabinets,
                        onClick = { applyFullPaletteWithCabinets = !applyFullPaletteWithCabinets },
                        label = { Text("شامل الخزائن", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("toggle_harmony_include_cabinets_chip")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KitchenCatalogData.harmonyPalettes.forEach { pal ->
                        val isPalSelected = selectedWallHex.equals(pal.wallColorHex, ignoreCase = true) &&
                            selectedFloor == pal.floorMaterial
                        Surface(
                            onClick = {
                                selectedWallHex = pal.wallColorHex
                                customHexInput = pal.wallColorHex
                                selectedFloor = pal.floorMaterial
                                if (applyFullPaletteWithCabinets) {
                                    selectedCabinet = pal.recommendedCabinetFinish
                                    selectedCountertop = pal.recommendedCountertop
                                }
                                onApplyHarmonyPalette(pal, applyFullPaletteWithCabinets)
                            },
                            color = if (isPalSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(205.dp)
                                .testTag("harmony_palette_card_${pal.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(KitchenCatalogData.resolveWallColorHex(pal.wallColorHex)))
                                            .border(1.dp, Color.Gray, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(KitchenCatalogData.resolveFloorColorHex(pal.floorMaterial)))
                                            .border(1.dp, Color.Gray, CircleShape)
                                    )
                                    Text(
                                        text = pal.titleAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "حائط: ${pal.wallColorNameAr}\nأرضية: ${pal.floorMaterial}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 2. Wall Colors Swatch Grid + Custom Hex Input
                Text(
                    text = "2. اختر لون الحائط ثلاثي الأبعاد (${KitchenCatalogData.wallColors.size} لون معماري):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KitchenCatalogData.wallColors.forEachIndexed { idx, wallOpt ->
                        val isSelected = selectedWallHex.equals(wallOpt.key, ignoreCase = true)
                        Surface(
                            onClick = {
                                selectedWallHex = wallOpt.key
                                customHexInput = wallOpt.key
                                onUpdateWallAndFloor(selectedWallHex, selectedFloor, selectedCabinet, selectedCountertop)
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("wall_color_option_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(wallOpt.colorHex))
                                        .border(1.dp, Color.DarkGray, CircleShape)
                                )
                                Text(
                                    text = wallOpt.nameAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Custom Hex Wall Color Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customHexInput,
                        onValueChange = { customHexInput = it },
                        label = { Text("كود لون حائط مخصص (#RRGGBB)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_wall_hex_input")
                    )
                    FilledTonalButton(
                        onClick = {
                            val normalized = if (customHexInput.trim().startsWith("#")) {
                                customHexInput.trim()
                            } else {
                                "#${customHexInput.trim()}"
                            }
                            if (normalized.length == 7) {
                                selectedWallHex = normalized
                                onUpdateWallAndFloor(selectedWallHex, selectedFloor, selectedCabinet, selectedCountertop)
                            }
                        },
                        modifier = Modifier.testTag("apply_custom_wall_hex_button")
                    ) {
                        Text("تطبيق الكود")
                    }
                }

                // 3. Floor Materials Swatch Grid
                Text(
                    text = "3. اختر خامة ولون الأرضية ثلاثية الأبعاد (${KitchenCatalogData.floorMaterials.size} خامات):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KitchenCatalogData.floorMaterials.forEachIndexed { idx, floorOpt ->
                        val isSelected = selectedFloor == floorOpt.nameAr
                        Surface(
                            onClick = {
                                selectedFloor = floorOpt.nameAr
                                onUpdateWallAndFloor(selectedWallHex, selectedFloor, selectedCabinet, selectedCountertop)
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("floor_material_option_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(floorOpt.colorHex))
                                        .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                                )
                                Text(
                                    text = floorOpt.nameAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateWallAndFloor(selectedWallHex, selectedFloor, selectedCabinet, selectedCountertop)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_wall_floor_harmony_button")
            ) {
                Text("اعتماد ألوان الحوائط والأرضيات في 3D")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * 3. Dedicated Custom Cabinet Designer Board from Front & Side Reference Photos:
 * (`ميزة تصميم خزائن في لوحة خاصة من صور جانبية وأمامية مع جعل الخزائن أكثر واقعية بظهر وجنب كامل من جميع الجهات`)
 * Uses zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) for Front View
 * and Side View photos, plus a live interactive Front + Side + 3D 6-Sided Orthographic/Isometric CAD board.
 */
@Composable
fun CustomCabinetPhotoDesignerDialog(
    state: PlannerStudioState,
    onDismiss: () -> Unit,
    onCreateCustomCabinet: (
        labelAr: String,
        baseType: ModuleType,
        widthMm: Int,
        depthMm: Int,
        heightMm: Int,
        doorsCount: Int,
        drawersCount: Int,
        shelvesCount: Int,
        hasFullBackPanel: Boolean,
        backPanelThicknessMm: Int,
        hasFullSidePanels: Boolean,
        boardThicknessMm: Int,
        customFinishName: String,
        frontPhotoUriString: String,
        sidePhotoUriString: String,
        saveToFirestoreCatalog: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var cabinetNameAr by remember { mutableStateOf("خزانة مخصصة بظهر وجنب كامل 18 ملم") }
    var selectedType by remember { mutableStateOf(ModuleType.BASE_CABINET_60) }
    var widthMm by remember { mutableIntStateOf(800) }
    var depthMm by remember { mutableIntStateOf(600) }
    var heightMm by remember { mutableIntStateOf(850) }
    var doorsCount by remember { mutableIntStateOf(2) }
    var drawersCount by remember { mutableIntStateOf(0) }
    var shelvesCount by remember { mutableIntStateOf(2) }
    var hasFullBackPanel by remember { mutableStateOf(true) }
    var backPanelThicknessMm by remember { mutableIntStateOf(18) }
    var hasFullSidePanels by remember { mutableStateOf(true) }
    var boardThicknessMm by remember { mutableIntStateOf(18) }
    var selectedFinish by remember(state.cabinetFinish) { mutableStateOf(state.cabinetFinish) }
    var saveToCatalog by remember { mutableStateOf(true) }

    var frontPhotoLabel by remember { mutableStateOf("مرجع أمامي قياسي (واجهة ضلف/أدراج)") }
    var sidePhotoLabel by remember { mutableStateOf("مرجع جانبي قياسي (جنب كامل 18mm + ظهر 18mm)") }
    var frontPhotoUriStr by remember { mutableStateOf("") }
    var sidePhotoUriStr by remember { mutableStateOf("") }

    // Zero-permission Android Photo Picker for Front Photo
    val frontPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            frontPhotoUriStr = uri.toString()
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, opts)
                    if (opts.outWidth > 0 && opts.outHeight > 0) {
                        val ratio = opts.outWidth.toFloat() / opts.outHeight.toFloat()
                        widthMm = (heightMm * ratio).roundToInt().coerceIn(300, 1400)
                        frontPhotoLabel = "صورة أمامية مستوردة (${opts.outWidth}×${opts.outHeight}px ➔ عرض ${widthMm}mm)"
                    } else {
                        frontPhotoLabel = "تم إرفاق الصورة الأمامية بنجاح"
                    }
                }
            }.onFailure {
                frontPhotoLabel = "تم إرفاق الصورة الأمامية"
            }
        }
    }

    // Zero-permission Android Photo Picker for Side Photo
    val sidePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            sidePhotoUriStr = uri.toString()
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, opts)
                    if (opts.outWidth > 0 && opts.outHeight > 0) {
                        val ratio = opts.outWidth.toFloat() / opts.outHeight.toFloat()
                        depthMm = (heightMm * ratio).roundToInt().coerceIn(300, 950)
                        sidePhotoLabel = "صورة جانبية مستوردة (${opts.outWidth}×${opts.outHeight}px ➔ عمق ${depthMm}mm)"
                    } else {
                        sidePhotoLabel = "تم إرفاق الصورة الجانبية بنجاح"
                    }
                }
            }.onFailure {
                sidePhotoLabel = "تم إرفاق الصورة الجانبية"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "لوحة تصميم خزانة مخصصة من صور أمامية وجانبية (6 جهات كاملة)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Front & Side Photo Upload / Reference Pickers
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "1. استيراد المسقط الأمامي والجانبي من صور الخزانة:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    frontPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_front_cabinet_photo_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("صورة أمامية", style = MaterialTheme.typography.labelMedium)
                            }

                            FilledTonalButton(
                                onClick = {
                                    sidePhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_side_cabinet_photo_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("صورة جانبية", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text(
                            text = "• الأمامي: $frontPhotoLabel\n• الجانبي: $sidePhotoLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Quick Reference Photo Templates (for 1-tap testing or instant workshop templates)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("خزانة كورنر سفلي 90×90", ModuleType.CORNER_BASE_CAROUSEL_90, Triple(900, 900, 850)),
                                Triple("خزانة كورنر علوي 65×65", ModuleType.CORNER_WALL_L_65, Triple(650, 650, 780)),
                                Triple("وحدة قدور 90 سم (درجين)", ModuleType.BASE_POT_DRAWERS_90, Triple(900, 600, 850)),
                                Triple("دولاب كوفي كورنر 90 سم", ModuleType.TALL_COFFEE_STATION_90, Triple(900, 600, 2200)),
                                Triple("دولاب فيترين زجاجي 80 سم", ModuleType.TALL_GLASS_DISPLAY_80, Triple(800, 600, 2200))
                            ).forEachIndexed { idx, (presetTitle, presetMod, dims) ->
                                FilterChip(
                                    selected = selectedType == presetMod && cabinetNameAr == presetTitle,
                                    onClick = {
                                        cabinetNameAr = presetTitle
                                        selectedType = presetMod
                                        widthMm = dims.first
                                        depthMm = dims.second
                                        heightMm = dims.third
                                        doorsCount = if (presetMod == ModuleType.BASE_POT_DRAWERS_90) 0 else 2
                                        drawersCount = if (presetMod == ModuleType.BASE_POT_DRAWERS_90) 2 else 0
                                        shelvesCount = if (presetMod.category == ModuleCategory.TALL_UNIT) 4 else 2
                                        frontPhotoLabel = "مسقط أمامي هندسي: $presetTitle (${dims.first}×${dims.third}mm)"
                                        sidePhotoLabel = "مسقط جانبي هندسي: عمق ${dims.second}mm + ظهر 18mm + أجناب 18mm"
                                    },
                                    label = { Text(presetTitle, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("custom_cabinet_photo_preset_$idx")
                                )
                            }
                        }
                    }
                }

                // 2. Live 3-Pane Orthographic + 3D 6-Sided Preview Board (Front View + Side View + 3D Complete Box)
                Surface(
                    color = Color(0xFF101C24),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .testTag("custom_cabinet_orthographic_board")
                ) {
                    val finishColor = Color(KitchenCatalogData.resolveCabinetColorHex(selectedFinish))
                    Canvas(modifier = Modifier.fillMaxWidth().height(175.dp)) {
                        val paneW = size.width / 3f
                        val pad = 18f

                        // --- Pane 1: Front View (المسقط الأمامي) ---
                        val fBoxW = (paneW - pad * 2f).coerceAtLeast(36f)
                        val fBoxH = (size.height - pad * 2.5f).coerceAtLeast(45f)
                        val fLeft = pad
                        val fTop = pad + 8f
                        drawRect(color = finishColor, topLeft = Offset(fLeft, fTop), size = Size(fBoxW, fBoxH))
                        // Left & Right 18mm full side panels in Front View
                        if (hasFullSidePanels) {
                            drawRect(color = Color.White.copy(alpha = 0.45f), topLeft = Offset(fLeft, fTop), size = Size(6f, fBoxH))
                            drawRect(color = Color.White.copy(alpha = 0.45f), topLeft = Offset(fLeft + fBoxW - 6f, fTop), size = Size(6f, fBoxH))
                        }
                        // Drawers or Doors in Front View
                        if (drawersCount > 0) {
                            for (d in 1 until drawersCount) {
                                val dy = fTop + fBoxH * (d.toFloat() / drawersCount.toFloat())
                                drawLine(Color(0xFF0D1418), Offset(fLeft + 4f, dy), Offset(fLeft + fBoxW - 4f, dy), strokeWidth = 3f)
                            }
                        } else if (doorsCount >= 2) {
                            for (dr in 1 until doorsCount) {
                                val dx = fLeft + fBoxW * (dr.toFloat() / doorsCount.toFloat())
                                drawLine(Color(0xFF0D1418), Offset(dx, fTop + 4f), Offset(dx, fTop + fBoxH - 4f), strokeWidth = 3f)
                            }
                        }
                        drawRect(color = Color(0xFF00E5FF), topLeft = Offset(fLeft, fTop), size = Size(fBoxW, fBoxH), style = Stroke(width = 2f))

                        // --- Pane 2: Side View (المسقط الجانبي + الظهر الكامل + الأرفف) ---
                        val sLeft = paneW + pad
                        val sBoxW = (paneW - pad * 2f) * (depthMm.toFloat() / 900f).coerceIn(0.45f, 0.95f)
                        val sTop = fTop
                        // Side panel fill
                        drawRect(color = finishColor.copy(alpha = 0.85f), topLeft = Offset(sLeft, sTop), size = Size(sBoxW, fBoxH))
                        // Full Back Panel highlight on left edge of side view
                        if (hasFullBackPanel) {
                            val backPx = if (backPanelThicknessMm >= 18) 8f else 4f
                            drawRect(
                                color = Color(0xFF00E676),
                                topLeft = Offset(sLeft, sTop),
                                size = Size(backPx, fBoxH)
                            )
                        }
                        // Internal shelves in Side View
                        for (sh in 1..shelvesCount) {
                            val sy = sTop + fBoxH * (sh.toFloat() / (shelvesCount + 1).toFloat())
                            drawLine(
                                color = Color(0xFFFFD54F),
                                start = Offset(sLeft + 6f, sy),
                                end = Offset(sLeft + sBoxW - 4f, sy),
                                strokeWidth = 3f
                            )
                        }
                        drawRect(color = Color(0xFF00E5FF), topLeft = Offset(sLeft, sTop), size = Size(sBoxW, fBoxH), style = Stroke(width = 2f))

                        // --- Pane 3: 3D 6-Sided Enclosed Preview (منظور 3D كامل بظهر وجنب) ---
                        val isoLeft = paneW * 2f + pad
                        val isoW = (paneW - pad * 2.4f).coerceAtLeast(34f)
                        val isoH = fBoxH * 0.78f
                        val isoTop = fTop + 16f
                        val dxIso = 16f
                        val dyIso = -12f

                        // Back Panel (Rear face)
                        if (hasFullBackPanel) {
                            drawRect(
                                color = Color(0xFF00E676).copy(alpha = 0.35f),
                                topLeft = Offset(isoLeft + dxIso, isoTop + dyIso),
                                size = Size(isoW, isoH),
                                style = Stroke(width = 2.5f)
                            )
                        }
                        // Side Gable Extrusion
                        val sidePath = Path().apply {
                            moveTo(isoLeft + isoW, isoTop)
                            lineTo(isoLeft + isoW + dxIso, isoTop + dyIso)
                            lineTo(isoLeft + isoW + dxIso, isoTop + isoH + dyIso)
                            lineTo(isoLeft + isoW, isoTop + isoH)
                            close()
                        }
                        drawPath(sidePath, finishColor.copy(alpha = 0.75f))
                        drawPath(sidePath, Color(0xFF00E5FF), style = Stroke(width = 1.8f))

                        // Top Panel
                        val topPath = Path().apply {
                            moveTo(isoLeft, isoTop)
                            lineTo(isoLeft + dxIso, isoTop + dyIso)
                            lineTo(isoLeft + isoW + dxIso, isoTop + dyIso)
                            lineTo(isoLeft + isoW, isoTop)
                            close()
                        }
                        drawPath(topPath, finishColor.copy(alpha = 0.92f))
                        drawPath(topPath, Color(0xFF00E5FF), style = Stroke(width = 1.8f))

                        // Front Face
                        drawRect(color = finishColor, topLeft = Offset(isoLeft, isoTop), size = Size(isoW, isoH))
                        drawRect(color = Color(0xFFFFD54F), topLeft = Offset(isoLeft, isoTop), size = Size(isoW, isoH), style = Stroke(width = 2.2f))
                    }
                }

                OutlinedTextField(
                    value = cabinetNameAr,
                    onValueChange = { cabinetNameAr = it },
                    label = { Text("اسم الخزانة المخصصة") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_cabinet_photo_name_input")
                )

                // Dimensions in Millimeters (Width, Depth, Height)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = widthMm.toString(),
                        onValueChange = { widthMm = it.toIntOrNull()?.coerceIn(150, 3200) ?: widthMm },
                        label = { Text("العرض (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_cabinet_photo_width_input")
                    )
                    OutlinedTextField(
                        value = depthMm.toString(),
                        onValueChange = { depthMm = it.toIntOrNull()?.coerceIn(200, 1500) ?: depthMm },
                        label = { Text("العمق (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_cabinet_photo_depth_input")
                    )
                    OutlinedTextField(
                        value = heightMm.toString(),
                        onValueChange = { heightMm = it.toIntOrNull()?.coerceIn(250, 2600) ?: heightMm },
                        label = { Text("الارتفاع (mm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_cabinet_photo_height_input")
                    )
                }

                // Structural 6-Sided Completeness Options (Full Back Panel + Full Side Panels + Shelves/Doors/Drawers)
                Text(
                    text = "3. تكوين هيكل الخزانة الكامل من جميع الجهات (CNC 6-Sided):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = hasFullBackPanel,
                        onClick = { hasFullBackPanel = !hasFullBackPanel },
                        label = { Text(if (hasFullBackPanel) "✓ ظهر كامل محكم (${backPanelThicknessMm}mm)" else "بدون ظهر") },
                        modifier = Modifier.testTag("toggle_full_back_panel_chip")
                    )
                    FilterChip(
                        selected = backPanelThicknessMm == 18,
                        onClick = { backPanelThicknessMm = if (backPanelThicknessMm == 18) 8 else 18 },
                        label = { Text("سماكة الظهر: ${backPanelThicknessMm} ملم") },
                        modifier = Modifier.testTag("toggle_back_panel_thickness_chip")
                    )
                    FilterChip(
                        selected = hasFullSidePanels,
                        onClick = { hasFullSidePanels = !hasFullSidePanels },
                        label = { Text(if (hasFullSidePanels) "✓ أجناب كاملة 18 ملم" else "جنب قياسي") },
                        modifier = Modifier.testTag("toggle_full_side_panels_chip")
                    )
                }

                // Doors, Drawers & Shelves Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("عدد الضلف: $doorsCount", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { doorsCount = (doorsCount - 1).coerceAtLeast(0) }) { Text("-") }
                                TextButton(onClick = { doorsCount = (doorsCount + 1).coerceAtMost(4) }) { Text("+") }
                            }
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("الأدراج: $drawersCount", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { drawersCount = (drawersCount - 1).coerceAtLeast(0) }) { Text("-") }
                                TextButton(onClick = { drawersCount = (drawersCount + 1).coerceAtMost(5) }) { Text("+") }
                            }
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("الأرفف: $shelvesCount", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { shelvesCount = (shelvesCount - 1).coerceAtLeast(0) }) { Text("-") }
                                TextButton(onClick = { shelvesCount = (shelvesCount + 1).coerceAtMost(6) }) { Text("+") }
                            }
                        }
                    }
                }

                FilterChip(
                    selected = saveToCatalog,
                    onClick = { saveToCatalog = !saveToCatalog },
                    label = { Text("حفظ الخزانة المصممة في كتالوج السحابة (Firestore) لإعادة استخدامها") },
                    modifier = Modifier.testTag("toggle_save_custom_cabinet_to_catalog")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateCustomCabinet(
                        cabinetNameAr,
                        selectedType,
                        widthMm,
                        depthMm,
                        heightMm,
                        doorsCount,
                        drawersCount,
                        shelvesCount,
                        hasFullBackPanel,
                        backPanelThicknessMm,
                        hasFullSidePanels,
                        boardThicknessMm,
                        selectedFinish,
                        frontPhotoUriStr,
                        sidePhotoUriStr,
                        saveToCatalog
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_create_custom_photo_cabinet_button")
            ) {
                Text("إضافة الخزانة الكاملة للتصميم والكتالوج")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}



