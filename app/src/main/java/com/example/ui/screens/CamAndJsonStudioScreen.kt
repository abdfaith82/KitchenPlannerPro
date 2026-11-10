package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cam.AluminumNestingSummary
import com.example.data.cam.JsonCadEngine
import com.example.data.cam.ProjectExportManager
import com.example.data.cam.SheetNestingSummary
import com.example.data.cam.StandardAluminumBarPreset
import com.example.data.cam.StandardMelamineSheetPreset
import com.example.data.model.KitchenCatalogData
import com.example.data.model.ModuleCategory
import com.example.ui.viewmodel.KitchenPlannerViewModel
import com.example.ui.viewmodel.PlannerStudioState
import kotlin.math.min

@Composable
fun CamAndJsonStudioScreen(
    state: PlannerStudioState,
    viewModel: KitchenPlannerViewModel
) {
    var activeSection by rememberSaveable { mutableIntStateOf(0) }
    val sheetNesting = state.sheetNesting
    val aluminumNesting = state.aluminumNesting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Selector Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "مركز التصنيع الرقمي CAM، محرر JSON v2.0، ورندر 8K PBR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeSection == 0,
                        onClick = { activeSection = 0 },
                        label = { Text("تفصيل الألواح والألمنيوم (CAM & Nesting)") },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("cam_subtab_nesting")
                    )
                    FilterChip(
                        selected = activeSection == 1,
                        onClick = { activeSection = 1 },
                        label = { Text("محرر JSON v2.0 واستيراد 3D") },
                        leadingIcon = {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("cam_subtab_json")
                    )
                    FilterChip(
                        selected = activeSection == 2,
                        onClick = { activeSection = 2 },
                        label = { Text("رندر 8K PBR والتصدير الشامل") },
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("cam_subtab_render_export")
                    )
                }
            }
        }

        when (activeSection) {
            0 -> CamCutListAndNestingSection(
                state = state,
                viewModel = viewModel,
                sheetNesting = sheetNesting,
                aluminumNesting = aluminumNesting
            )
            1 -> JsonV2And3DModelImportSection(state = state, viewModel = viewModel)
            2 -> Photorealistic8KAndMultiExportSection(
                state = state,
                sheetNesting = sheetNesting,
                aluminumNesting = aluminumNesting
            )
        }
    }
}

@Composable
private fun CamCutListAndNestingSection(
    state: PlannerStudioState,
    viewModel: KitchenPlannerViewModel,
    sheetNesting: SheetNestingSummary,
    aluminumNesting: AluminumNestingSummary
) {
    var selectedSheetIdx by remember { mutableIntStateOf(0) }
    val totalSheets = sheetNesting.totalSheets18Mm.coerceAtLeast(1)
    val safeSheetIndex = selectedSheetIdx.coerceIn(0, totalSheets - 1)
    val activeSheetPlacements = sheetNesting.placements.filter { it.sheetIndex == safeSheetIndex }
    val efficiencyPct = (100f - sheetNesting.wastePercent).coerceIn(10f, 97f)

    val standards = state.camMaterialStandards
    var customSheetWidthText by remember(standards.sheetWidthMm) { mutableStateOf(standards.sheetWidthMm.toString()) }
    var customSheetHeightText by remember(standards.sheetHeightMm) { mutableStateOf(standards.sheetHeightMm.toString()) }
    var customPanelDepthText by remember(standards.standardPanelDepthMm) { mutableStateOf(standards.standardPanelDepthMm.toString()) }
    var customBoardThicknessText by remember(standards.boardThicknessMm) { mutableStateOf(standards.boardThicknessMm.toString()) }
    var customSheetKerfText by remember(standards.sawKerfMm) { mutableStateOf(standards.sawKerfMm.toString()) }
    var customAlumBarLengthText by remember(standards.aluminumBarLengthMm) { mutableStateOf(standards.aluminumBarLengthMm.toString()) }
    var customAlumProfileWidthText by remember(standards.aluminumProfileWidthMm) { mutableStateOf(standards.aluminumProfileWidthMm.toString()) }
    var customAlumProfileDepthText by remember(standards.aluminumProfileDepthMm) { mutableStateOf(standards.aluminumProfileDepthMm.toString()) }
    var customAlumKerfText by remember(standards.aluminumSawKerfMm) { mutableStateOf(standards.aluminumSawKerfMm.toString()) }

    // 0. Interactive Standard Material Dimensions Control Panel (Melamine Sheets & Aluminum Bars)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cam_material_standards_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "إعدادات الأبعاد القياسية للألواح والبروفايلات (الطول • العرض • العمق)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تُعتمد هذه الأبعاد افتراضياً في محرك حساب التكاليف (${state.costBreakdown.totalSar} ر.س) وقائمة القص (Cut-List)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.resetCamMaterialStandardsToDefault() },
                    modifier = Modifier.testTag("reset_cam_material_standards_button")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افتراضي")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section A: Standard Melamine / MDF Sheet Dimensions (Length, Width, Depth, Thickness)
            Text(
                text = "1. الأبعاد القياسية للألواح (الطول × العرض × العمق القياسي × السماكة):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StandardMelamineSheetPreset.entries.forEach { preset ->
                    val isSelected = standards.sheetWidthMm == preset.widthMm &&
                        standards.sheetHeightMm == preset.heightMm
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.applyMelamineSheetPreset(preset) },
                        label = { Text(preset.titleAr) },
                        modifier = Modifier.testTag("melamine_preset_${preset.widthMm}_${preset.heightMm}")
                    )
                }
            }

            // Quick Board Thickness Chips (16mm / 18mm / 22mm / 25mm)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سماكة اللوح:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                listOf(16, 18, 22, 25).forEach { thickMm ->
                    FilterChip(
                        selected = standards.boardThicknessMm == thickMm,
                        onClick = {
                            customBoardThicknessText = thickMm.toString()
                            viewModel.updateCamMaterialStandards(boardThicknessMm = thickMm)
                        },
                        label = { Text("$thickMm ملم") },
                        modifier = Modifier.testTag("board_thickness_chip_$thickMm")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customSheetWidthText,
                    onValueChange = { customSheetWidthText = it },
                    label = { Text("الطول (ملم)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_sheet_width_mm")
                )
                OutlinedTextField(
                    value = customSheetHeightText,
                    onValueChange = { customSheetHeightText = it },
                    label = { Text("العرض (ملم)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_sheet_height_mm")
                )
                OutlinedTextField(
                    value = customPanelDepthText,
                    onValueChange = { customPanelDepthText = it },
                    label = { Text("العمق (ملم)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("input_panel_depth_mm")
                )
                OutlinedTextField(
                    value = customBoardThicknessText,
                    onValueChange = { customBoardThicknessText = it },
                    label = { Text("السماكة") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("input_board_thickness_mm")
                )
                OutlinedTextField(
                    value = customSheetKerfText,
                    onValueChange = { customSheetKerfText = it },
                    label = { Text("شفرة القص") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.75f)
                        .testTag("input_sheet_kerf_mm")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Section B: Standard Aluminum Profile Bar Dimensions (Length, Width, Depth)
            Text(
                text = "2. الأبعاد القياسية لبروفايلات الألمنيوم (الطول × العرض × العمق):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StandardAluminumBarPreset.entries.forEach { preset ->
                    val isSelected = standards.aluminumBarLengthMm == preset.lengthMm
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.applyAluminumBarPreset(preset) },
                        label = { Text(preset.titleAr) },
                        modifier = Modifier.testTag("aluminum_preset_${preset.lengthMm}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customAlumBarLengthText,
                    onValueChange = { customAlumBarLengthText = it },
                    label = { Text("طول البروفايل (ملم)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("input_aluminum_bar_length_mm")
                )
                OutlinedTextField(
                    value = customAlumProfileWidthText,
                    onValueChange = { customAlumProfileWidthText = it },
                    label = { Text("عرض البروفايل") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("input_aluminum_profile_width_mm")
                )
                OutlinedTextField(
                    value = customAlumProfileDepthText,
                    onValueChange = { customAlumProfileDepthText = it },
                    label = { Text("عمق البروفايل") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("input_aluminum_profile_depth_mm")
                )
                OutlinedTextField(
                    value = customAlumKerfText,
                    onValueChange = { customAlumKerfText = it },
                    label = { Text("خلوص المنشار") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("input_aluminum_kerf_mm")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "معتمد في التكلفة: ألواح ${state.costBreakdown.sheetsMaterialCostSar} ر.س • ألمنيوم ${state.costBreakdown.aluminumProfilesCostSar} ر.س • إجمالي ${state.costBreakdown.totalSar} ر.س",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.updateCamMaterialStandards(
                            sheetWidthMm = customSheetWidthText.toIntOrNull() ?: standards.sheetWidthMm,
                            sheetHeightMm = customSheetHeightText.toIntOrNull() ?: standards.sheetHeightMm,
                            standardPanelDepthMm = customPanelDepthText.toIntOrNull() ?: standards.standardPanelDepthMm,
                            boardThicknessMm = customBoardThicknessText.toIntOrNull() ?: standards.boardThicknessMm,
                            sawKerfMm = customSheetKerfText.toIntOrNull() ?: standards.sawKerfMm,
                            aluminumBarLengthMm = customAlumBarLengthText.toIntOrNull() ?: standards.aluminumBarLengthMm,
                            aluminumProfileWidthMm = customAlumProfileWidthText.toIntOrNull() ?: standards.aluminumProfileWidthMm,
                            aluminumProfileDepthMm = customAlumProfileDepthText.toIntOrNull() ?: standards.aluminumProfileDepthMm,
                            aluminumSawKerfMm = customAlumKerfText.toIntOrNull() ?: standards.aluminumSawKerfMm
                        )
                    },
                    modifier = Modifier.testTag("apply_cam_material_standards_button")
                ) {
                    Text("تطبيق الأبعاد في التكلفة والقص")
                }
            }
        }
    }

    // 1. KPI Summary Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "ملخص محرك التصنيع والتعشيق الهندسي (CNC ${sheetNesting.boardThicknessMm}mm & ${aluminumNesting.standardBarLengthMm}mm Aluminum)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "ألواح ميلامين ${sheetNesting.boardThicknessMm}mm (${sheetNesting.standardSheetWidthMm}×${sheetNesting.standardSheetHeightMm})",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "${sheetNesting.totalSheets18Mm} لوح (كفاءة ${efficiencyPct.toInt()}%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        "قضبان ألمنيوم (${aluminumNesting.standardBarLengthMm} ملم)",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "${aluminumNesting.totalBarsCount} قضيب قياسي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("المساحة المستخدمة", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "${sheetNesting.usedAreaSqM} م²",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = "الإكسسوارات وحصر الكميات: ${sheetNesting.totalBlumHinges} مفصلة Blum هيدروليك • ${sheetNesting.totalDrawerRunnersPairs} طقم مجاري أدراج • ${sheetNesting.totalEdgeBandingMeters} متر طولي شريط حواف PVC • ظهر 8mm (${sheetNesting.totalBackPanels8MmAreaSqM} م²)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    // 2. Visual 2D Sheet Nesting Map
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = "مخطط تعشيق اللوح #${safeSheetIndex + 1} (${sheetNesting.standardSheetWidthMm}×${sheetNesting.standardSheetHeightMm} ملم • سماكة ${sheetNesting.boardThicknessMm} ملم)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الاستغلال: ${efficiencyPct.toInt()}% • الهدر: ${sheetNesting.wastePercent}% • عدد القطع على اللوح: ${activeSheetPlacements.size} • شفرة القص: ${sheetNesting.kerfMm} ملم",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (totalSheets > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (idx in 0 until totalSheets) {
                        FilterChip(
                            selected = safeSheetIndex == idx,
                            onClick = { selectedSheetIdx = idx },
                            label = { Text("لوح #${idx + 1}") }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A242B))
                    .padding(10.dp)
                    .testTag("sheet_nesting_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / sheetNesting.standardSheetWidthMm.toFloat()
                    val scaleY = size.height / sheetNesting.standardSheetHeightMm.toFloat()
                    val scale = min(scaleX, scaleY)
                    val boardW = sheetNesting.standardSheetWidthMm * scale
                    val boardH = sheetNesting.standardSheetHeightMm * scale
                    val ox = (size.width - boardW) / 2f
                    val oy = (size.height - boardH) / 2f

                    drawRect(
                        color = Color(0xFF2B3942),
                        topLeft = Offset(ox, oy),
                        size = Size(boardW, boardH)
                    )
                    drawRect(
                        color = Color(0xFF80CBC4),
                        topLeft = Offset(ox, oy),
                        size = Size(boardW, boardH),
                        style = Stroke(width = 2.5f)
                    )

                    val partPaint = Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        textSize = 15f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = android.graphics.Color.WHITE
                    }

                    activeSheetPlacements.forEachIndexed { idx, placed ->
                        val px = ox + placed.xMm * scale
                        val py = oy + placed.yMm * scale
                        val pw = placed.placedWidthMm * scale
                        val ph = placed.placedHeightMm * scale
                        val palette = listOf(
                            Color(0xFF00897B),
                            Color(0xFF3949AB),
                            Color(0xFF6D4C41),
                            Color(0xFF00ACC1),
                            Color(0xFF5E35B1)
                        )
                        val fill = palette[idx % palette.size]
                        drawRect(
                            color = fill.copy(alpha = 0.88f),
                            topLeft = Offset(px, py),
                            size = Size(pw, ph)
                        )
                        drawRect(
                            color = Color(0xFFFFD54F),
                            topLeft = Offset(px, py),
                            size = Size(pw, ph),
                            style = Stroke(width = 1.4f)
                        )
                        if (pw > 38f && ph > 22f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "${placed.placedWidthMm}×${placed.placedHeightMm}",
                                px + pw / 2f,
                                py + ph / 2f + 5f,
                                partPaint
                            )
                        }
                    }
                }
            }
        }
    }

    // 3. Aluminum Bar Nesting Optimizer (Custom Standard Bar Length)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "تحسين قص قطاعات الألمنيوم ومقابض Gola وإطارات الزجاج (قضبان قياسية بطول ${aluminumNesting.standardBarLengthMm} ملم • خلوص القص ${standards.aluminumSawKerfMm} ملم):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            aluminumNesting.bars.forEach { bar ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "قضيب #${bar.barIndex} (${bar.standardLengthMm} ملم): ${bar.profileTypeAr}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "المستغل ${bar.usedMm} ملم • الفاقد ${bar.offcutWasteMm} ملم",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (bar.usedMm / bar.standardLengthMm.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "الأطوال المقصوصة: " + bar.cuts.joinToString(" | ") { "${it.lengthMm} ملم (${it.labelAr})" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 4. Detailed Cut-List Table
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "قائمة القص التفصيلية للألواح (Cut-List — ${sheetNesting.cutPieces.size} قطعة):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            sheetNesting.cutPieces.take(18).forEach { part ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${part.unitLabel} — ${part.partNameAr}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "سماكة ${part.thicknessMm} ملم • الخامة: ${part.materialAr} • شريط حواف: ${part.edgeBandingMeters} م.ط",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${part.lengthMm}×${part.widthMm} ملم",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }
        }
    }
}

@Composable
private fun JsonV2And3DModelImportSection(
    state: PlannerStudioState,
    viewModel: KitchenPlannerViewModel
) {
    val context = LocalContext.current
    var showCameraToJsonDialog by remember { mutableStateOf(false) }
    var showUniversalJsonDialog by remember { mutableStateOf(false) }

    val selectedUnit = state.placedItems.firstOrNull { it.uid == state.selectedItemUid } ?: state.placedItems.firstOrNull()
    val defaultJsonText = remember(selectedUnit, state.cabinetFinish, state.countertopMaterial, state.lastGeneratedJsonV2) {
        state.lastGeneratedJsonV2 ?: if (selectedUnit != null) {
            JsonCadEngine.itemToJsonV2(selectedUnit, state.cabinetFinish, state.countertopMaterial)
        } else {
            JsonCadEngine.projectToJsonV2(state)
        }
    }
    var editorText by remember(defaultJsonText) { mutableStateOf(defaultJsonText) }

    // File launcher for ANY .json file
    val anyJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_object.json"
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    editorText = content
                    viewModel.importAnyJsonFileOrContent(fileName, content)
                }
            }
        }
    }

    // File launcher for 3D / CAD models (OBJ, GLB/glTF, DXF, JSON)
    val modelFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_model.obj"
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    viewModel.importExternal3DOrCadModel(fileName, content)
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "استيراد الكائنات عبر الكاميرا كـ JSON واستيراد أي ملف JSON أو نموذج 3D (OBJ / GLB / DXF):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { anyJsonFileLauncher.launch("*/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("import_any_json_file_button")
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استيراد أي ملف JSON (.json)")
                }

                Button(
                    onClick = { showCameraToJsonDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("cam_tab_camera_to_json_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استيراد كائن بالكاميرا ➔ JSON")
                }

                FilledTonalButton(
                    onClick = { showUniversalJsonDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_universal_json_dialog_button")
                ) {
                    Text("لصق / استيراد أي هيكل JSON")
                }

                OutlinedButton(
                    onClick = { modelFileLauncher.launch("*/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("import_3d_file_picker_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختيار ملف (OBJ / GLB / DXF)")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val sampleObj = """
                            # Wavefront OBJ Kitchen Island 1800x900x850mm
                            v 0.0 0.0 0.0
                            v 1.80 0.85 0.90
                        """.trimIndent()
                        viewModel.importExternal3DOrCadModel("Luxury_Island_1800mm.obj", sampleObj)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("import_sample_obj_button")
                ) {
                    Text("+ استيراد نموذج OBJ قياسي")
                }

                FilledTonalButton(
                    onClick = {
                        val sampleGlb = "GLTF_ASSET_VITRINE_900MM"
                        viewModel.importExternal3DOrCadModel("SmokedGlass_Vitrine_900mm.gltf", sampleGlb)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("import_sample_gltf_button")
                ) {
                    Text("+ استيراد GLB/glTF")
                }

                FilledTonalButton(
                    onClick = {
                        val sampleDxf = "0\nSECTION\n2\nENTITIES\n0\nENDSEC"
                        viewModel.importExternal3DOrCadModel("CNC_Island_1600mm.dxf", sampleDxf)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("import_sample_dxf_button")
                ) {
                    Text("+ استيراد أوتوكاد DXF")
                }
            }
        }
    }

    // Embedded Interactive JSON v2.0 Editor Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "محرر JSON التفاعلي الشامل (يدعم JSON v2.0 وأي هيكل JSON مخصص)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "عدّل أو الصق أي كود JSON (كائن مفرد، مصفوفة وحدات، أو مشروع كامل) ثم اضغط 'مزامنة مع المشهد 3D'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { editorText = JsonCadEngine.projectToJsonV2(state) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("كامل المشروع JSON")
                    }
                    Button(
                        onClick = { viewModel.applyJsonV2ToSelectedOrScene(editorText) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sync_json_v2_to_scene_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مزامنة مع المشهد 3D")
                    }
                }
            }

            OutlinedTextField(
                value = editorText,
                onValueChange = { editorText = it },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                minLines = 12,
                maxLines = 20,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("json_v2_editor_textfield")
            )
        }
    }

    if (showCameraToJsonDialog) {
        CameraToJsonV2ScannerDialog(
            state = state,
            onDismiss = { showCameraToJsonDialog = false },
            onScanAndGenerate = { label, type, wMm, dMm, hMm, sampledHex ->
                val generated = viewModel.createUnitFromCameraScanToJsonV2(
                    labelAr = label,
                    baseType = type,
                    widthMm = wMm,
                    depthMm = dMm,
                    heightMm = hMm,
                    sampledColorHex = sampledHex
                )
                editorText = generated
                showCameraToJsonDialog = false
            }
        )
    }

    if (showUniversalJsonDialog) {
        UniversalJsonImporterDialog(
            onDismiss = { showUniversalJsonDialog = false },
            onImportJson = { fileName, rawJson ->
                editorText = rawJson
                viewModel.importAnyJsonFileOrContent(fileName, rawJson)
            }
        )
    }
}

@Composable
private fun Photorealistic8KAndMultiExportSection(
    state: PlannerStudioState,
    sheetNesting: SheetNestingSummary,
    aluminumNesting: AluminumNestingSummary
) {
    val context = LocalContext.current
    var exportedBanner by remember { mutableStateOf<String?>(null) }
    val archVizPrompt = remember(state) {
        buildString {
            appendLine("ArchViz 8K Photorealistic PBR Render Spec (Geometry-Locked 100%):")
            appendLine("• Room: ${state.roomWidthMm}x${state.roomDepthMm}x${state.roomHeightMm} mm (${state.shape.titleAr})")
            appendLine("• Cabinet PBR Finish: ${state.cabinetFinish} (18mm CNC, Gola Anodized Aluminum)")
            appendLine("• Countertop PBR Slab: ${state.countertopMaterial} (Roughness 0.14, Specular 0.88)")
            appendLine("• Upper Vitrines: Smoked Black Tempered Glass + Warm 3000K Internal LED Strips (>=600mm Safety Clearance)")
        }
    }

    // 1. 8K Photorealistic PBR & AI ArchViz Viewport Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "محرك الرندر الواقعي الفائق (8K Photorealistic PBR & AI ArchViz)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "معالجة PBR تحافظ على الأبعاد الهندسية 100% مع انعكاسات الأرضية، ظلال الإغلاق المحيطي (AO)، زجاج أسود مدخن، إضاءة LED دافئة 3000K، وعروق رخام كرارا/كلكتا.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0E1317), Color(0xFF1C252C), Color(0xFF101619))
                        )
                    )
                    .testTag("pbr_8k_render_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val floorY = h * 0.72f

                    // Polished Floor with Specular Gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF28353C), Color(0xFF161E22), Color(0xFF0D1215)),
                            startY = floorY,
                            endY = h
                        ),
                        topLeft = Offset(0f, floorY),
                        size = Size(w, h - floorY)
                    )

                    // Ceiling Warm Volumetric Light Cone
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(w * 0.5f, h * 0.32f),
                            radius = w * 0.58f
                        ),
                        radius = w * 0.58f,
                        center = Offset(w * 0.5f, h * 0.32f)
                    )

                    val cabColor = Color(KitchenCatalogData.resolveCabinetColorHex(state.cabinetFinish))
                    val marbleColor = Color(KitchenCatalogData.resolveCountertopColorHex(state.countertopMaterial))
                    val scaleMm = (w * 0.84f) / state.roomWidthMm.toFloat()
                    val startX = w * 0.08f

                    state.placedItems.forEach { item ->
                        val left = startX + item.xMm * scaleMm
                        val uw = item.effectiveWidthMm * scaleMm
                        val uh = item.heightMm * scaleMm
                        val bottom = floorY - item.elevationMm * scaleMm
                        val top = bottom - uh
                        val isWall = item.type.category == ModuleCategory.WALL_CABINET

                        // Mirror floor reflection + contact shadow for base units
                        if (!isWall) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(cabColor.copy(alpha = 0.22f), Color.Transparent),
                                    startY = floorY,
                                    endY = (floorY + uh * 0.45f).coerceAtMost(h)
                                ),
                                topLeft = Offset(left, floorY + 2f),
                                size = Size(uw, (uh * 0.42f).coerceAtMost(h - floorY)),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.48f),
                                topLeft = Offset(left - 2f, floorY - 3f),
                                size = Size(uw + 4f, 5f)
                            )
                        }

                        drawRoundRect(
                            color = if (isWall) Color(0xFF182026).copy(alpha = 0.90f) else cabColor,
                            topLeft = Offset(left, top),
                            size = Size(uw, uh),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        if (isWall) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFFFCA28).copy(alpha = 0.45f),
                                        Color(0xFFFF8F00).copy(alpha = 0.16f)
                                    ),
                                    startY = top,
                                    endY = bottom
                                ),
                                topLeft = Offset(left + 4f, top + 4f),
                                size = Size((uw - 8f).coerceAtLeast(4f), (uh - 8f).coerceAtLeast(4f)),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                            // Internal illuminated glass shelf
                            drawLine(
                                color = Color(0xFFFFE082).copy(alpha = 0.72f),
                                start = Offset(left + 5f, top + uh * 0.5f),
                                end = Offset(left + uw - 5f, top + uh * 0.5f),
                                strokeWidth = 1.6f
                            )
                        }

                        if (item.type.countertopIncluded) {
                            drawRect(
                                color = marbleColor,
                                topLeft = Offset(left - 2f, top - 6f),
                                size = Size(uw + 4f, 6f)
                            )
                        }

                        drawRoundRect(
                            color = Color(0xFFB0BEC5).copy(alpha = 0.65f),
                            topLeft = Offset(left, top),
                            size = Size(uw, uh),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(width = 1.6f)
                        )
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "8K PBR • 3000K LED • Geometry Locked",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD54F),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = archVizPrompt,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    // 2. Multi-Format Export & Share Hub (JSON v2.0, AutoCAD DXF, PDF Report, CSV Cut-List)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "التصدير الشامل متعدد الصيغ (JSON v2.0 • AutoCAD DXF • PDF • CSV):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (exportedBanner != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = exportedBanner ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val content = JsonCadEngine.projectToJsonV2(state)
                        ProjectExportManager.exportAndShareTextFile(
                            context = context,
                            fileName = "kitchen_project_v2.json",
                            mimeType = "application/json",
                            content = content,
                            shareTitle = "تصدير مشروع JSON v2.0"
                        ).onSuccess { file ->
                            exportedBanner = "تم توليد ملف JSON v2.0 بنجاح: ${file.name}"
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("export_json_v2_button")
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير JSON v2.0")
                }

                Button(
                    onClick = {
                        val dxf = ProjectExportManager.buildAutoCadDxfContent(state)
                        ProjectExportManager.exportAndShareTextFile(
                            context = context,
                            fileName = "kitchen_layout_1mm.dxf",
                            mimeType = "application/dxf",
                            content = dxf,
                            shareTitle = "تصدير مخطط AutoCAD DXF"
                        ).onSuccess { file ->
                            exportedBanner = "تم توليد مخطط أوتوكاد DXF بالمليمتر: ${file.name}"
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("export_autocad_dxf_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير AutoCAD DXF")
                }

                Button(
                    onClick = {
                        ProjectExportManager.generateEngineeringPdfReport(
                            context = context,
                            state = state,
                            nesting = sheetNesting,
                            aluminum = aluminumNesting
                        ).onSuccess { file ->
                            exportedBanner = "تم توليد التقرير الهندسي PDF بنجاح: ${file.name}"
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("export_pdf_report_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير تقرير PDF")
                }

                Button(
                    onClick = {
                        val csv = ProjectExportManager.buildCutListCsvContent(
                            state = state,
                            nesting = sheetNesting,
                            aluminum = aluminumNesting
                        )
                        ProjectExportManager.exportAndShareTextFile(
                            context = context,
                            fileName = "kitchen_cutlist_bom.csv",
                            mimeType = "text/csv",
                            content = csv,
                            shareTitle = "تصدير جداول القص والكميات CSV"
                        ).onSuccess { file ->
                            exportedBanner = "تم توليد جداول القص والكميات CSV: ${file.name}"
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("export_csv_cutlist_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير جداول CSV")
                }
            }
        }
    }
}
