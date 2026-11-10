package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.KitchenCatalogUnit
import com.example.data.model.PlacedKitchenItem
import kotlin.math.roundToInt

/**
 * Represents an active drag-and-drop operation from any floating menu into the 2D/3D/AR design canvas.
 */
data class CatalogDragSession(
    val unit: KitchenCatalogUnit? = null,
    val cabinetFinishNameAr: String? = null,
    val countertopMaterialNameAr: String? = null,
    val pointerRootOffset: Offset = Offset.Zero,
    val isOverCanvas: Boolean = false,
    val previewPlacedItem: PlacedKitchenItem? = null
) {
    val displayTitleAr: String
        get() = when {
            unit != null -> unit.nameAr
            cabinetFinishNameAr != null -> "خامة خزائن: $cabinetFinishNameAr"
            countertopMaterialNameAr != null -> "سطح رخام: $countertopMaterialNameAr"
            else -> ""
        }

    val displayColorHex: Long
        get() = unit?.colorHex ?: 0xFF00BFA5L
}

/**
 * Descriptor for a toggleable item in the Floating Menus Dock Bar.
 */
data class FloatingMenuToggleItem(
    val id: String,
    val titleAr: String,
    val icon: ImageVector,
    val isVisible: Boolean,
    val badgeText: String? = null,
    val testTag: String,
    val onToggle: () -> Unit
)

/**
 * Master Floating Dock Bar that allows hiding/showing all menus at once (100% Canvas Zen Mode)
 * or toggling individual floating menus on/off.
 */
@Composable
fun FloatingMenusMasterDockBar(
    allMenusHidden: Boolean,
    onToggleAllMenus: () -> Unit,
    menuItems: List<FloatingMenuToggleItem>,
    modifier: Modifier = Modifier,
    dockTitleAr: String = "القوائم العائمة"
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
            .testTag("floating_menus_master_dock"),
        shape = RoundedCornerShape(50),
        color = Color(0xFF10181D).copy(alpha = 0.88f),
        tonalElevation = 8.dp,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle for moving the dock itself anywhere on screen
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                            dragOffsetY += dragAmount.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "تحريك شريط القوائم العائمة",
                    tint = Color(0xFF80CBC4),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Master Hide/Show All Floating Menus Button
            FilterChip(
                selected = !allMenusHidden,
                onClick = onToggleAllMenus,
                label = {
                    Text(
                        text = if (allMenusHidden) {
                            "إظهار $dockTitleAr"
                        } else {
                            "إخفاء جميع القوائم"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (allMenusHidden) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xFF00695C),
                    labelColor = Color.White,
                    iconColor = Color.White,
                    selectedContainerColor = Color(0xFF00897B),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color(0xFFFFD54F)
                ),
                modifier = Modifier.testTag("toggle_all_floating_menus_button")
            )

            if (!allMenusHidden) {
                menuItems.forEach { item ->
                    FilterChip(
                        selected = item.isVisible,
                        onClick = item.onToggle,
                        label = {
                            Text(
                                text = if (item.badgeText != null) {
                                    "${item.titleAr} (${item.badgeText})"
                                } else {
                                    item.titleAr
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White.copy(alpha = 0.85f),
                            iconColor = Color.White.copy(alpha = 0.75f),
                            selectedContainerColor = Color(0xFF004D40),
                            selectedLabelColor = Color(0xFFA7FFEB),
                            selectedLeadingIconColor = Color(0xFF64FFDA)
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    }
}

/**
 * Reusable Draggable, Collapsible (Minimize), and Hideable (Close) Floating Panel Container.
 */
@Composable
fun FloatingCollapsibleWindow(
    titleAr: String,
    subtitleAr: String? = null,
    icon: ImageVector,
    visible: Boolean,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
    initialMinimized: Boolean = false,
    fadeWhenDraggingCatalogItem: Boolean = false,
    headerActions: @Composable () -> Unit = {},
    testTag: String = "floating_window",
    content: @Composable () -> Unit
) {
    var isMinimized by remember { mutableStateOf(initialMinimized) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.94f),
        exit = fadeOut() + scaleOut(targetScale = 0.94f),
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .alpha(if (fadeWhenDraggingCatalogItem) 0.18f else 0.96f)
            .testTag(testTag)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Draggable & Collapsible Floating Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "سحب القائمة العائمة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = titleAr,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!subtitleAr.isNullOrBlank() && !isMinimized) {
                                Text(
                                    text = subtitleAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        headerActions()

                        if (offsetX != 0f || offsetY != 0f) {
                            IconButton(
                                onClick = {
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenWith,
                                    contentDescription = "إعادة موضع القائمة",
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { isMinimized = !isMinimized },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("${testTag}_minimize_button")
                        ) {
                            Icon(
                                imageVector = if (isMinimized) {
                                    Icons.Default.KeyboardArrowDown
                                } else {
                                    Icons.Default.KeyboardArrowUp
                                },
                                contentDescription = if (isMinimized) "توسيع القائمة" else "طي القائمة",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onHide,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("${testTag}_hide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إخفاء القائمة العائمة",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !isMinimized,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Draggable & Clickable Catalog Unit Card:
 * Supports both:
 * 1) Touch-and-drag directly from the floating catalog menu into the 2D/3D/AR design canvas.
 * 2) Single tap for instant placement.
 */
@Composable
fun DraggableCatalogUnitCard(
    unit: KitchenCatalogUnit,
    onQuickTapAdd: (KitchenCatalogUnit) -> Unit,
    onDragStartUnit: (KitchenCatalogUnit, Offset) -> Unit,
    onDragMoveUnit: (Offset) -> Unit,
    onDragEndUnit: () -> Unit,
    onDragCancelUnit: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    actionLabelAr: String = "اسحب للتصميم أو اضغط +"
) {
    var cardRootPosition by remember { mutableStateOf(Offset.Zero) }
    val currentUnit by rememberUpdatedState(unit)
    val currentOnDragStart by rememberUpdatedState(onDragStartUnit)
    val currentOnDragMove by rememberUpdatedState(onDragMoveUnit)
    val currentOnDragEnd by rememberUpdatedState(onDragEndUnit)
    val currentOnDragCancel by rememberUpdatedState(onDragCancelUnit)

    Card(
        modifier = modifier
            .width(168.dp)
            .onGloballyPositioned { coords ->
                cardRootPosition = coords.positionInRoot()
            }
            .clickable { onQuickTapAdd(unit) }
            .pointerInput(unit.id, unit.code) {
                var currentRootOffset = Offset.Zero
                detectDragGestures(
                    onDragStart = { startLocal ->
                        currentRootOffset = cardRootPosition + startLocal
                        currentOnDragStart(currentUnit, currentRootOffset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentRootOffset += dragAmount
                        currentOnDragMove(currentRootOffset)
                    },
                    onDragEnd = {
                        currentOnDragEnd()
                    },
                    onDragCancel = {
                        currentOnDragCancel()
                    }
                )
            }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(unit.colorHex))
                    )
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "اسحب للإفلات",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "${unit.widthCm * 10}×${unit.depthCm * 10} ملم",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = unit.nameAr,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${unit.priceSar} ر.س",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = actionLabelAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Floating Drag Ghost Badge that follows the user's finger across the screen
 * while dragging a catalog unit or material finish into the 2D/3D/AR canvas.
 */
@Composable
fun FloatingCatalogDragGhostOverlay(
    dragSession: CatalogDragSession?,
    containerRootOffset: Offset
) {
    if (dragSession == null) return

    val localX = (dragSession.pointerRootOffset.x - containerRootOffset.x - 85f)
    val localY = (dragSession.pointerRootOffset.y - containerRootOffset.y - 78f)
    val preview = dragSession.previewPlacedItem

    Surface(
        modifier = Modifier
            .offset { IntOffset(localX.roundToInt(), localY.roundToInt()) }
            .testTag("active_catalog_drag_ghost"),
        shape = RoundedCornerShape(16.dp),
        color = if (dragSession.isOverCanvas) {
            Color(0xFF004D40).copy(alpha = 0.94f)
        } else {
            Color(0xFF1A2429).copy(alpha = 0.92f)
        },
        tonalElevation = 12.dp,
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = if (dragSession.isOverCanvas) Color(0xFF00E676) else Color(0xFF80CBC4),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(dragSession.displayColorHex))
                )
                Text(
                    text = dragSession.displayTitleAr,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            if (preview != null && dragSession.isOverCanvas) {
                Text(
                    text = "إفلات عند (${preview.xMm}، ${preview.yMm} ملم) • ${preview.widthMm}×${preview.depthMm} ملم",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA7FFEB),
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "اسحب وأفلت داخل مساحة التصميم...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFE082)
                )
            }
        }
    }
}

/**
 * Draggable & Clickable PBR Material Swatch Chip:
 * Users can tap or drag-and-drop any cabinet finish or marble countertop swatch directly onto the 3D kitchen canvas.
 */
@Composable
fun DraggableMaterialFinishChip(
    labelAr: String,
    colorHex: Long,
    isSelected: Boolean,
    onTapSelect: () -> Unit,
    onDragStartSwatch: (Offset) -> Unit,
    onDragMoveSwatch: (Offset) -> Unit,
    onDragEndSwatch: () -> Unit,
    onDragCancelSwatch: () -> Unit,
    modifier: Modifier = Modifier,
    textureResId: Int? = null
) {
    var chipRootPosition by remember { mutableStateOf(Offset.Zero) }
    val currentOnDragStart by rememberUpdatedState(onDragStartSwatch)
    val currentOnDragMove by rememberUpdatedState(onDragMoveSwatch)
    val currentOnDragEnd by rememberUpdatedState(onDragEndSwatch)
    val currentOnDragCancel by rememberUpdatedState(onDragCancelSwatch)

    Surface(
        shape = RoundedCornerShape(50),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier
            .onGloballyPositioned { coords ->
                chipRootPosition = coords.positionInRoot()
            }
            .clickable { onTapSelect() }
            .pointerInput(labelAr) {
                var currentRootOffset = Offset.Zero
                detectDragGestures(
                    onDragStart = { startLocal ->
                        currentRootOffset = chipRootPosition + startLocal
                        currentOnDragStart(currentRootOffset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentRootOffset += dragAmount
                        currentOnDragMove(currentRootOffset)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragCancel() }
                )
            }
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(colorHex))
                    .border(0.8.dp, Color.Gray.copy(alpha = 0.6f), CircleShape)
            ) {
                if (textureResId != null) {
                    Image(
                        painter = painterResource(id = textureResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(16.dp)
                            .alpha(0.65f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelAr,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}


