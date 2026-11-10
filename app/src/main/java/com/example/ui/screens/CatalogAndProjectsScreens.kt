package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.cam.ProjectExportManager
import com.example.data.model.KitchenCatalogData
import com.example.data.model.KitchenCatalogUnit
import com.example.data.model.KitchenItemsSerializer
import com.example.data.model.KitchenProject
import com.example.data.model.KitchenShape
import com.example.data.model.ModuleCategory
import com.example.data.model.ModuleType
import com.example.data.model.QuoteRequest
import com.example.ui.viewmodel.KitchenPlannerViewModel
import com.example.ui.viewmodel.PlannerStudioState
import com.example.ui.viewmodel.UiState

@Composable
fun ProjectsAndTemplatesScreen(
    studioState: PlannerStudioState,
    projectsState: UiState<List<KitchenProject>>,
    viewModel: KitchenPlannerViewModel,
    onOpenStudio: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Studio Project Save / Update Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التصميم المفتوح حالياً في الاستوديو 3D",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = studioState.projectName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${studioState.shape.titleAr} • ${studioState.roomWidthCm}×${studioState.roomDepthCm} سم • ${studioState.placedItems.size} وحدة 3D",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Surface(
                            color = if (studioState.activeProjectId.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (studioState.activeProjectId.isNotBlank()) {
                                    "مرتبط بالسحابة"
                                } else {
                                    "غير محفوظ بعد"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.saveCurrentProjectToCloud(saveAsNew = false) },
                            enabled = !studioState.isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("projects_save_current_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (studioState.activeProjectId.isBlank()) {
                                    "حفظ التصميم في Firestore"
                                } else {
                                    "حفظ التعديلات"
                                }
                            )
                        }

                        if (studioState.activeProjectId.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveCurrentProjectToCloud(
                                        customName = "${studioState.projectName} (نسخة معدلة)",
                                        saveAsNew = true
                                    )
                                },
                                enabled = !studioState.isSaving,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("projects_save_as_new_button")
                            ) {
                                Text("حفظ كنسخة جديدة")
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مشاريعي السحابية المحفوظة (Firestore)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "استرجع أي تصميم ثلاثي الأبعاد سابق لتعديله أو نسخه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = {
                        viewModel.startNewBlankProject()
                        onOpenStudio()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("new_blank_project_button")
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشروع جديد")
                }
            }
        }

        when (projectsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = projectsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                if (projectsState.data.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "لا توجد مشاريع محفوظة بعد",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اضغط 'حفظ التصميم في Firestore' بالأعلى لحفظ تصميمك ثلاثي الأبعاد الحالي والرجوع لتعديله في أي وقت.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(projectsState.data, key = { it.id }) { project ->
                        SavedProjectCard(
                            project = project,
                            isActiveInStudio = project.id == studioState.activeProjectId,
                            onLoad = {
                                viewModel.loadSavedProject(project)
                                onOpenStudio()
                            },
                            onDuplicate = {
                                viewModel.duplicateSavedProject(project) {
                                    onOpenStudio()
                                }
                            },
                            onDelete = { viewModel.deleteCloudProject(project.id) }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "قوالب المطابخ المعمارية الجاهزة (3D Presets)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "تصاميم احترافية مستوحاة من أرقى المعارض الأوروبية جاهزة للتخصيص الفوري",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(KitchenCatalogData.stylePresets, key = { it.id }) { preset ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("style_preset_${preset.id}"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    ) {
                        Image(
                            painter = painterResource(id = preset.imageResId),
                            contentDescription = preset.titleAr,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            color = Color(0xFFC88A4B),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = preset.badgeAr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F1305),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = preset.titleAr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = preset.subtitleAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "الخزائن: ${preset.cabinetFinish} • السطح: ${preset.countertopMaterial} • المقاس: ${preset.defaultWidthCm}×${preset.defaultDepthCm} سم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = {
                                viewModel.applyStylePreset(preset)
                                onOpenStudio()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("فتح وتخصيص هذا التصميم في الاستوديو 3D")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedProjectCard(
    project: KitchenProject,
    isActiveInStudio: Boolean,
    onLoad: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val shapeTitle = KitchenShape.fromCode(project.shape).titleAr
    val decoded = remember(project.itemsData) { KitchenItemsSerializer.decodeWithViewport(project.itemsData) }
    val unitCount = decoded.items.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActiveInStudio) 2.dp else 0.dp,
                color = if (isActiveInStudio) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("saved_project_card_${project.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActiveInStudio) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "قيد التعديل حالياً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "$shapeTitle • ${project.roomWidthCm}×${project.roomDepthCm} سم • $unitCount وحدة 3D • الكفاءة ${project.ergonomicScore}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.testTag("duplicate_saved_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ المشروع",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_saved_project_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف المشروع",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = "الخزائن: ${project.cabinetFinish} | السطح: ${project.countertopMaterial}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (project.notes.isNotBlank()) {
                Text(
                    text = project.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${project.totalPriceSar} ر.س",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onLoad,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("load_saved_project_${project.id}")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استرجاع وتعديل 3D")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogAndMaterialsScreen(
    state: PlannerStudioState,
    catalogUnitsState: UiState<List<KitchenCatalogUnit>>,
    viewModel: KitchenPlannerViewModel,
    onOpenStudio: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ModuleCategory?>(null) }
    var filterCornersOnly by remember { mutableStateOf(false) }
    var showAddUnitDialog by remember { mutableStateOf(false) }
    var showPhotoCabinetDesignerDialog by remember { mutableStateOf(false) }
    var showWallFloorHarmonyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section 1: Material, Surface & Wall Color Customizer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تخصيص مواد الأسطح والألوان المعمارية (3D Materials)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FilledTonalButton(
                            onClick = { showWallFloorHarmonyDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("catalog_open_wall_floor_harmony_button")
                        ) {
                            Text("أداة تناسق الحوائط والأرضيات")
                        }
                    }

                    Text(
                        text = "1. خامة ولون واجهات الخزائن (خلفيات واقعية Photorealistic):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KitchenCatalogData.cabinetFinishes.forEach { option ->
                            val isSelected = state.cabinetFinish == option.nameAr
                            PhotorealisticMaterialSwatchCard(
                                titleAr = option.nameAr,
                                subtitleAr = option.categoryAr,
                                colorHex = option.colorHex,
                                textureResId = option.textureResId,
                                isSelected = isSelected,
                                onClick = { viewModel.updateFinishes(cabinetFinish = option.nameAr) },
                                modifier = Modifier.testTag("catalog_cabinet_swatch_${option.nameAr}")
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "2. خامة أسطح العمل والجزيرة (رخام • جرانيت • خشب واقعي):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KitchenCatalogData.countertopMaterials.forEach { option ->
                            val isSelected = state.countertopMaterial == option.nameAr
                            PhotorealisticMaterialSwatchCard(
                                titleAr = option.nameAr,
                                subtitleAr = option.categoryAr,
                                colorHex = option.colorHex,
                                textureResId = option.textureResId,
                                isSelected = isSelected,
                                onClick = { viewModel.updateFinishes(countertopMaterial = option.nameAr) },
                                modifier = Modifier.testTag("catalog_countertop_swatch_${option.nameAr}")
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "3. ألوان الجدران والأرضيات والمقابض:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KitchenCatalogData.wallColors.forEach { wall ->
                            val isSelected = state.wallColorHex.equals(wall.hexCode, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateFinishes(wallColorHex = wall.hexCode) },
                                label = { Text("جدار: ${wall.nameAr}") }
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KitchenCatalogData.floorMaterials.forEach { floor ->
                            FilterChip(
                                selected = state.floorMaterial == floor.nameAr,
                                onClick = { viewModel.updateFinishes(floorMaterial = floor.nameAr) },
                                label = { Text("أرضية: ${floor.nameAr}") }
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KitchenCatalogData.handleStyles.forEach { handle ->
                            FilterChip(
                                selected = state.handleStyle == handle,
                                onClick = { viewModel.updateFinishes(handleStyle = handle) },
                                label = { Text(handle) }
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Firestore Kitchen Units Catalog (Cabinets, Countertops, Appliances)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "كتالوج وحدات المطبخ والأجهزة (Firestore Catalog)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "خزائن كورنرات علوية وسفلية، خزائن متعددة 6 أوجه بظهر وجنب كامل، وأجهزة ذكية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { showPhotoCabinetDesignerDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("catalog_design_from_photos_button")
                        ) {
                            Text("تصميم من صور أمامية/جانبية")
                        }
                        OutlinedButton(
                            onClick = { showAddUnitDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("add_custom_catalog_unit_button")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("وحدة مخصصة")
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null && !filterCornersOnly,
                        onClick = {
                            selectedCategory = null
                            filterCornersOnly = false
                        },
                        label = { Text("الكل") }
                    )
                    FilterChip(
                        selected = filterCornersOnly,
                        onClick = {
                            filterCornersOnly = !filterCornersOnly
                            if (filterCornersOnly) selectedCategory = null
                        },
                        label = { Text("خزائن الكورنرات العلوية والسفلية") },
                        modifier = Modifier.testTag("catalog_screen_corner_filter_chip")
                    )
                    ModuleCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category && !filterCornersOnly,
                            onClick = {
                                selectedCategory = category
                                filterCornersOnly = false
                            },
                            label = { Text(category.titleAr) }
                        )
                    }
                }
            }
        }

        when (catalogUnitsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = catalogUnitsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val filteredUnits = catalogUnitsState.data.filter { unit ->
                    when {
                        filterCornersOnly -> unit.isCornerUnit
                        selectedCategory != null -> unit.resolvedCategory == selectedCategory
                        else -> true
                    }
                }

                items(filteredUnits, key = { it.id.ifBlank { it.code } }) { unit ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalog_unit_card_${unit.code}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(unit.colorHex)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${unit.widthCm}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = unit.nameAr,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (unit.isCornerUnit) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "زاوية (Corner)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${unit.resolvedCategory.titleAr} • عرض ${unit.widthCm} × عمق ${unit.depthCm} × ارتفاع ${unit.heightCm} سم • ظهر وجنب كامل 18 ملم",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (unit.descriptionAr.isNotBlank()) {
                                            Text(
                                                text = unit.descriptionAr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${unit.priceSar} ر.س ${if (unit.countertopIncluded) "• يشمل سطح عمل" else ""}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = {
                                        viewModel.addCatalogUnitToLayout(unit)
                                        onOpenStudio()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("catalog_add_${unit.code}")
                                ) {
                                    Text("إضافة للمطبخ 3D")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPhotoCabinetDesignerDialog) {
        CustomCabinetPhotoDesignerDialog(
            state = state,
            onDismiss = { showPhotoCabinetDesignerDialog = false },
            onCreateCustomCabinet = { labelAr, baseType, widthMm, depthMm, heightMm, doorsCount, drawersCount, shelvesCount, hasFullBackPanel, backPanelThicknessMm, hasFullSidePanels, boardThicknessMm, customFinishName, frontPhotoUriString, sidePhotoUriString, saveToFirestoreCatalog ->
                viewModel.createCustomCabinetFromFrontAndSidePhotos(
                    labelAr = labelAr,
                    baseType = baseType,
                    widthMm = widthMm,
                    depthMm = depthMm,
                    heightMm = heightMm,
                    doorsCount = doorsCount,
                    drawersCount = drawersCount,
                    shelvesCount = shelvesCount,
                    hasFullBackPanel = hasFullBackPanel,
                    backPanelThicknessMm = backPanelThicknessMm,
                    hasFullSidePanels = hasFullSidePanels,
                    boardThicknessMm = boardThicknessMm,
                    customFinishName = customFinishName,
                    frontPhotoUriString = frontPhotoUriString,
                    sidePhotoUriString = sidePhotoUriString,
                    saveToFirestoreCatalog = saveToFirestoreCatalog
                )
                showPhotoCabinetDesignerDialog = false
                onOpenStudio()
            }
        )
    }

    if (showWallFloorHarmonyDialog) {
        WallAndFloorHarmonyStudioDialog(
            state = state,
            onDismiss = { showWallFloorHarmonyDialog = false },
            onUpdateWallAndFloor = { wallHex, floorMat, cabFin, topMat ->
                viewModel.updateFinishes(
                    wallColorHex = wallHex,
                    floorMaterial = floorMat,
                    cabinetFinish = cabFin,
                    countertopMaterial = topMat
                )
            },
            onApplyHarmonyPalette = { palette, applyCabinets ->
                viewModel.applyWallAndFloorHarmonyPalette(palette, applyCabinets)
            }
        )
    }

    if (showAddUnitDialog) {
        AddCustomCatalogUnitDialog(
            onDismiss = { showAddUnitDialog = false },
            onConfirm = { nameAr, baseType, w, d, price, desc ->
                viewModel.addCustomCatalogUnitToFirestore(
                    nameAr = nameAr,
                    baseType = baseType,
                    widthCm = w,
                    depthCm = d,
                    priceSar = price,
                    descriptionAr = desc
                )
                showAddUnitDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomCatalogUnitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ModuleType, Int, Int, Int, String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ModuleType.BASE_DRAWERS_80) }
    var widthText by remember { mutableStateOf("90") }
    var depthText by remember { mutableStateOf("60") }
    var priceText by remember { mutableStateOf("2200") }
    var descriptionAr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة وحدة جديدة لكتالوج Firestore",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text("اسم الوحدة أو الجهاز") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "نوع الوحدة الأساسي (3D Model):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        ModuleType.BASE_DRAWERS_80 to "خزانة سفلية",
                        ModuleType.STOVE_HOB to "فرن وموقد",
                        ModuleType.FRIDGE_SMART to "ثلاجة",
                        ModuleType.ISLAND_WATERFALL to "جزيرة"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                widthText = type.defaultWidthCm.toString()
                                depthText = type.defaultDepthCm.toString()
                                priceText = type.defaultPriceSar.toString()
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { widthText = it },
                        label = { Text("العرض (سم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = depthText,
                        onValueChange = { depthText = it },
                        label = { Text("العمق (سم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("السعر (ر.س)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionAr,
                    onValueChange = { descriptionAr = it },
                    label = { Text("وصف المواصفات") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        nameAr,
                        selectedType,
                        widthText.toIntOrNull() ?: selectedType.defaultWidthCm,
                        depthText.toIntOrNull() ?: selectedType.defaultDepthCm,
                        priceText.toIntOrNull() ?: selectedType.defaultPriceSar,
                        descriptionAr
                    )
                }
            ) {
                Text("حفظ في الكتالوج")
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
fun BomAndQuoteRequestsScreen(
    state: PlannerStudioState,
    quotesState: UiState<List<QuoteRequest>>,
    viewModel: KitchenPlannerViewModel
) {
    val context = LocalContext.current
    val cost = state.costBreakdown
    var selectedShowroom by remember { mutableStateOf(KitchenCatalogData.certifiedShowrooms.first()) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(selectedShowroom.cityAr) }
    var installWindow by remember { mutableStateOf("خلال 30 يوم") }
    var specialNotes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Interactive Graphical Cost Distribution Chart Card (Materials, Labor, Accessories, Appliances)
        item {
            ProjectCostDistributionChartCard(
                state = state,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 1. Detailed Bill of Materials (BOM) Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "جدول الكميات والتسعير التفصيلي (BOM)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مشروع: ${state.projectName} (${state.placedItems.size} وحدة • ${state.unitSystem.formatMillimeters(state.roomWidthMm)}×${state.unitSystem.formatMillimeters(state.roomDepthMm)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilterChip(
                            selected = state.unitSystem.code == "IMPERIAL",
                            onClick = { viewModel.toggleMeasurementUnitSystem() },
                            label = { Text(state.unitSystem.badgeAr) },
                            modifier = Modifier.testTag("bom_toggle_unit_system_chip")
                        )
                    }

                    HorizontalDivider()

                    BomLineRow(
                        label = "وحدات الخزائن (${state.cabinetFinish})",
                        value = "${cost.cabinetsCostSar} ر.س"
                    )
                    BomLineRow(
                        label = "أسطح العمل (${state.countertopMaterial} • ${state.unitSystem.formatLinearCountertop(cost.countertopLinearMeters)})",
                        value = "${cost.countertopCostSar} ر.س"
                    )
                    BomLineRow(
                        label = "الألواح القياسية (${state.sheetNesting.totalSheets18Mm} لوح ${state.camMaterialStandards.sheetWidthMm}×${state.camMaterialStandards.sheetHeightMm}×${state.camMaterialStandards.standardPanelDepthMm} ملم)",
                        value = "${cost.sheetsMaterialCostSar} ر.س"
                    )
                    BomLineRow(
                        label = "بروفايلات الألمنيوم (${state.aluminumNesting.totalBarsCount} قضيب ${state.camMaterialStandards.aluminumBarLengthMm}×${state.camMaterialStandards.aluminumProfileWidthMm}×${state.camMaterialStandards.aluminumProfileDepthMm} ملم)",
                        value = "${cost.aluminumProfilesCostSar} ر.س"
                    )
                    BomLineRow(
                        label = "الإكسسوارات والمفصلات (Blum + Gola + ${cost.linkedSmartAccessoriesCount} إكسسوار ذكي)",
                        value = "${cost.accessoriesCostSar} ر.س"
                    )
                    BomLineRow(
                        label = "الأجهزة المدمجة والأحواض",
                        value = "${cost.appliancesCostSar} ر.س"
                    )
                    BomLineRow(
                        label = "العمالة والتصنيع CNC والتركيب الهندسي",
                        value = "${cost.laborAndCncSar} ر.س"
                    )

                    if (state.linkedAccessoriesBom.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "الإكسسوارات الداخلية الذكية المرتبطة تلقائياً بالوحدات المتوافقة (${cost.smartInternalAccessoriesCostSar} ر.س):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        state.linkedAccessoriesBom.forEach { linked ->
                            BomLineRow(
                                label = "• ${linked.accessory.nameAr} (×${linked.quantity} - ${linked.matchedUnitsSummaryAr})",
                                value = "${linked.totalPriceSar} ر.س"
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الإجمالي التقديري شامل الضريبة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${cost.totalSar} ر.س",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()

                    Button(
                        onClick = {
                            ProjectExportManager.generateEngineeringPdfReport(
                                context = context,
                                state = state,
                                nesting = state.sheetNesting,
                                aluminum = state.aluminumNesting,
                                shareViaMessagingApps = true
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bom_export_pdf_report_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء ومشاركة مستند عرض سعر PDF (عبر البريد الإلكتروني أو المراسلة)")
                    }
                }
            }
        }

        // 2. Certified Showrooms Selection & Quote Submission Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "إرسال المخطط لمعرض مطابخ معتمد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "احصل على عرض سعر رسمي وموعد رفع مقاسات بالليزر مجاناً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    KitchenCatalogData.certifiedShowrooms.forEach { showroom ->
                        val isSelected = selectedShowroom.id == showroom.id
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedShowroom = showroom
                                    city = showroom.cityAr
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = showroom.nameAr,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "معرض موثق",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "${showroom.cityAr} • ${showroom.specialtyAr} • تسليم خلال ${showroom.deliveryDays} يوم",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFE09F3E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${showroom.rating}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("الاسم الكامل") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quote_customer_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("رقم الجوال") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quote_customer_phone_input")
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("المدينة") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quote_city_input")
                        )
                    }

                    OutlinedTextField(
                        value = installWindow,
                        onValueChange = { installWindow = it },
                        label = { Text("موعد التركيب المفضل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = specialNotes,
                        onValueChange = { specialNotes = it },
                        label = { Text("ملاحظات إضافية للمهندس المختص") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.submitQuoteForCurrentProject(
                                showroom = selectedShowroom,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                city = city,
                                preferredInstallWindow = installWindow,
                                specialNotes = specialNotes,
                                onComplete = {
                                    specialNotes = ""
                                }
                            )
                        },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_quote_request_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حفظ المخطط وإرسال طلب عرض السعر",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Submitted Quote Requests History
        item {
            Text(
                text = "سجل طلبات التسعير المرسلة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        when (quotesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = quotesState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                if (quotesState.data.isEmpty()) {
                    item {
                        Text(
                            text = "لم يتم إرسال طلبات تسعير بعد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(quotesState.data, key = { it.id }) { quote ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quote_card_${quote.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${quote.projectName} • ${quote.showroomName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "المدينة: ${quote.city} • الميزانية: ${quote.estimatedBudgetSar} ر.س",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = when (quote.status) {
                                            "pending" -> "الحالة: قيد المراجعة الهندسية"
                                            "cancelled" -> "الحالة: ملغى"
                                            else -> "الحالة: مكتمل"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (quote.status == "cancelled") {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                                if (quote.status == "pending") {
                                    TextButton(
                                        onClick = { viewModel.cancelQuoteRequest(quote.id) },
                                        modifier = Modifier.testTag("cancel_quote_${quote.id}")
                                    ) {
                                        Text("إلغاء الطلب", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BomLineRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Photorealistic Kitchen Material Selection Swatch Card:
 * Uses the AI-generated photorealistic textures (Marble, Wood, Granite) as a rich visual background
 * with color-accurate tinting and clear selection indicator.
 */
@Composable
fun PhotorealisticMaterialSwatchCard(
    titleAr: String,
    subtitleAr: String,
    colorHex: Long,
    textureResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(164.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                if (textureResId != 0) {
                    Image(
                        painter = painterResource(id = textureResId),
                        contentDescription = titleAr,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Subtle realistic material finish tint + bottom gradient for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(colorHex).copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.52f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(colorHex))
                        .border(1.5.dp, Color.White, CircleShape)
                )
                if (isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "مُختار ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = titleAr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitleAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private data class CostChartSlice(
    val labelAr: String,
    val amountSar: Int,
    val color: Color,
    val subtitleAr: String
)

/**
 * Interactive Graphical Cost Distribution Chart Component:
 * Displays a Recharts-style interactive Donut Ring Chart + Comparative Horizontal Bar Chart
 * showing the exact distribution of project costs across:
 * 1. المواد والألواح والأسطح (Materials: Cabinets + Countertops + Standard Sheets & Aluminum Profiles)
 * 2. العمالة والتصنيع والتركيب (Labor, CNC Cutting & Engineering Installation)
 * 3. الإكسسوارات والتجهيزات الذكية (Blum Hinges, Gola Profiles & Auto-Linked Internal Accessories)
 * 4. الأجهزة المدمجة والأحواض (Built-in Appliances & Sinks)
 */
@Composable
fun ProjectCostDistributionChartCard(
    state: PlannerStudioState,
    modifier: Modifier = Modifier
) {
    val cost = state.costBreakdown
    val std = state.camMaterialStandards
    var showBarMode by remember { mutableStateOf(false) }

    val slices = remember(cost, std) {
        listOf(
            CostChartSlice(
                labelAr = "المواد والألواح والأسطح",
                amountSar = cost.materialsTotalSar,
                color = Color(0xFF00897B),
                subtitleAr = "${state.sheetNesting.totalSheets18Mm} لوح (${std.sheetWidthMm}×${std.sheetHeightMm} ملم) + ${state.aluminumNesting.totalBarsCount} بروفايل"
            ),
            CostChartSlice(
                labelAr = "العمالة والتصنيع CNC والتركيب",
                amountSar = cost.laborAndCncSar,
                color = Color(0xFFFB8C00),
                subtitleAr = "قص CNC وتجميع شريط الحواف وتركيب موقعي"
            ),
            CostChartSlice(
                labelAr = "الإكسسوارات والمفصلات الذكية",
                amountSar = cost.accessoriesCostSar,
                color = Color(0xFF3949AB),
                subtitleAr = "مفصلات Blum + مقابض + ${cost.linkedSmartAccessoriesCount} منظم داخلي"
            ),
            CostChartSlice(
                labelAr = "الأجهزة المدمجة والأحواض",
                amountSar = cost.appliancesCostSar,
                color = Color(0xFF8E24AA),
                subtitleAr = "الأفران والثلاجات وغسالات الصحون والأحواض"
            )
        )
    }
    val totalComponentsSar = slices.sumOf { it.amountSar }.coerceAtLeast(1)

    Card(
        modifier = modifier.testTag("cost_distribution_chart_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تحليل توزيع التكاليف الرسومي (المواد • العمالة • الإكسسوارات)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "معتمد تلقائياً على الأبعاد القياسية للألواح (${std.sheetWidthMm}×${std.sheetHeightMm}×${std.standardPanelDepthMm} ملم) والبروفايلات (${std.aluminumBarLengthMm} ملم)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilterChip(
                    selected = showBarMode,
                    onClick = { showBarMode = !showBarMode },
                    label = { Text(if (showBarMode) "مخطط دائري (Donut)" else "مخطط أعمدة (Bars)") },
                    modifier = Modifier.testTag("toggle_cost_chart_mode_chip")
                )
            }

            if (!showBarMode) {
                // Donut Ring Chart + Interactive Legend Side-by-Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(148.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(134.dp)) {
                            val strokeW = 28f
                            var startAngle = -90f
                            slices.forEach { slice ->
                                val sweep = (slice.amountSar.toFloat() / totalComponentsSar.toFloat()) * 360f
                                if (sweep > 0.5f) {
                                    drawArc(
                                        color = slice.color,
                                        startAngle = startAngle,
                                        sweepAngle = (sweep - 2.5f).coerceAtLeast(1f),
                                        useCenter = false,
                                        topLeft = Offset(strokeW / 2f, strokeW / 2f),
                                        size = Size(size.width - strokeW, size.height - strokeW),
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "الإجمالي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${cost.totalSar}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "ر.س",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slices.forEach { slice ->
                            val pct = ((slice.amountSar.toFloat() / totalComponentsSar.toFloat()) * 100f).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = slice.labelAr,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = slice.subtitleAr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = "${slice.amountSar} ر.س ($pct%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = slice.color
                                )
                            }
                        }
                    }
                }
            } else {
                // Comparative Horizontal Bar Chart Mode
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    slices.forEach { slice ->
                        val ratio = (slice.amountSar.toFloat() / totalComponentsSar.toFloat()).coerceIn(0.03f, 1f)
                        val pct = (ratio * 100f).toInt()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = slice.labelAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${slice.amountSar} ر.س ($pct%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = slice.color
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(slice.color)
                                )
                            }
                            Text(
                                text = slice.subtitleAr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
