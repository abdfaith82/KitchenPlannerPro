package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.viewmodel.KitchenPlannerViewModel
import com.example.ui.viewmodel.PlannerStudioState
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pure optical-flow block-matching & radial loom calculator used by the real-time camera sensor analyzer.
 * Returns Triple(panFlowX, tiltFlowY, forwardWalkStepCm).
 */
object VrCameraOpticalFlowEngine {
    const val GRID_COLS = 12
    const val GRID_ROWS = 9

    fun computeGridOpticalFlow(
        prevGrid: FloatArray,
        currGrid: FloatArray,
        cols: Int = GRID_COLS,
        rows: Int = GRID_ROWS
    ): Triple<Float, Float, Float> {
        if (prevGrid.size < cols * rows || currGrid.size < cols * rows) {
            return Triple(0f, 0f, 0f)
        }

        var sumDx = 0f
        var sumDy = 0f
        var radialOutwardScore = 0f
        var validBlocks = 0

        val centerCol = (cols - 1) / 2f
        val centerRow = (rows - 1) / 2f

        for (r in 1 until (rows - 1)) {
            for (c in 1 until (cols - 1)) {
                val baseVal = prevGrid[r * cols + c]
                // Check local contrast so flat uniform walls don't produce noise
                val rightDiff = abs(baseVal - prevGrid[r * cols + (c + 1)])
                val downDiff = abs(baseVal - prevGrid[(r + 1) * cols + c])
                if (rightDiff + downDiff < 4.5f) continue

                var bestSad = Float.MAX_VALUE
                var bestDc = 0
                var bestDr = 0

                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = r + dr
                        val nc = c + dc
                        val diff = abs(baseVal - currGrid[nr * cols + nc])
                        // Slight preference for zero displacement unless movement is clear
                        val penalty = if (dr == 0 && dc == 0) 0f else 1.2f
                        if (diff + penalty < bestSad) {
                            bestSad = diff + penalty
                            bestDc = dc
                            bestDr = dr
                        }
                    }
                }

                if (bestDc != 0 || bestDr != 0) {
                    sumDx += bestDc.toFloat()
                    sumDy += bestDr.toFloat()

                    val relX = c - centerCol
                    val relY = r - centerRow
                    val dotRadial = bestDc * relX + bestDr * relY
                    radialOutwardScore += dotRadial
                    validBlocks++
                }
            }
        }

        if (validBlocks < 2) {
            return Triple(0f, 0f, 0f)
        }

        // Camera features moving right means the camera panned left (invert for natural camera pan)
        val avgFlowX = (-sumDx / validBlocks.toFloat()).coerceIn(-2.5f, 2.5f)
        val avgFlowY = (-sumDy / validBlocks.toFloat()).coerceIn(-2.5f, 2.5f)
        val forwardWalkCm = ((radialOutwardScore / validBlocks.toFloat()) * 1.6f).coerceIn(-6.5f, 6.5f)

        return Triple(avgFlowX, avgFlowY, forwardWalkCm)
    }
}

private class RealTimeVrCameraAnalyzer(
    private val onMotionDetected: (panYawDeltaDeg: Float, tiltPitchDelta: Float, walkForwardDeltaCm: Float, flowX: Float, flowY: Float) -> Unit
) : ImageAnalysis.Analyzer {

    private var previousGrid: FloatArray? = null
    private var lastFrameTimestampMs: Long = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastFrameTimestampMs < 55L) {
                return
            }
            lastFrameTimestampMs = now

            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride.coerceAtLeast(1)
            val width = image.width.coerceAtLeast(16)
            val height = image.height.coerceAtLeast(16)

            val cols = VrCameraOpticalFlowEngine.GRID_COLS
            val rows = VrCameraOpticalFlowEngine.GRID_ROWS
            val currentGrid = FloatArray(cols * rows)

            val stepX = (width / cols).coerceAtLeast(1)
            val stepY = (height / rows).coerceAtLeast(1)

            for (r in 0 until rows) {
                val sampleY = (r * stepY + stepY / 2).coerceIn(0, height - 1)
                for (c in 0 until cols) {
                    val sampleX = (c * stepX + stepX / 2).coerceIn(0, width - 1)
                    val idx = sampleY * rowStride + sampleX * pixelStride
                    val luma = if (idx in 0 until buffer.limit()) {
                        buffer.get(idx).toInt() and 0xFF
                    } else {
                        128
                    }
                    currentGrid[r * cols + c] = luma.toFloat()
                }
            }

            val prev = previousGrid
            previousGrid = currentGrid
            if (prev != null) {
                val (flowX, flowY, forwardStepCm) = VrCameraOpticalFlowEngine.computeGridOpticalFlow(
                    prevGrid = prev,
                    currGrid = currentGrid,
                    cols = cols,
                    rows = rows
                )
                if (abs(flowX) > 0.08f || abs(flowY) > 0.08f || abs(forwardStepCm) > 0.25f) {
                    val panDeltaDeg = flowX * 2.4f
                    val tiltDelta = flowY * 0.014f
                    onMotionDetected(panDeltaDeg, tiltDelta, forwardStepCm, flowX, flowY)
                }
            }
        } catch (_: Exception) {
            // Ignore transient buffer exceptions during camera lifecycle transitions
        } finally {
            image.close()
        }
    }
}

/**
 * Interactive Real-Time VR Navigation Overlay utilizing the device's live Camera Sensor (Optical Flow)
 * fused with IMU Gyroscope/Accelerometer sensors to pan, tilt, and walk through the 3D rendered kitchen.
 */
@Composable
fun VrCameraSensorNavigationOverlay(
    state: PlannerStudioState,
    viewModel: KitchenPlannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentState by rememberUpdatedState(state)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // 1. Continuous Auto-Walk Loop when vrAutoWalkEnabled is active
    LaunchedEffect(state.vrAutoWalkEnabled, state.viewMode) {
        while (state.vrAutoWalkEnabled) {
            delay(65L)
            viewModel.applyVrCameraSensorMotion(
                panYawDeltaDeg = 0f,
                tiltPitchDelta = 0f,
                walkForwardDeltaCm = 1.8f,
                walkStrafeDeltaCm = 0f,
                flowX = 0f,
                flowY = -0.45f,
                detectedStep = false
            )
        }
    }

    // 2. Device IMU Gyroscope + Rotation Vector + Pedometer Step Sensor Fusion
    DisposableEffect(state.vrCameraSensorTrackingEnabled) {
        if (!state.vrCameraSensorTrackingEnabled) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            var lastGyroTimestampNs = 0L
            var lastStepTimestampMs = 0L
            var smoothedAccelMag = 9.81f

            val sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val ev = event ?: return
                    if (!currentState.vrCameraSensorTrackingEnabled) return

                    when (ev.sensor.type) {
                        Sensor.TYPE_GYROSCOPE -> {
                            if (lastGyroTimestampNs != 0L) {
                                val dt = (ev.timestamp - lastGyroTimestampNs) * 1e-9f
                                if (dt in 0.001f..0.25f) {
                                    // Device rotation around Y/Z pans yaw, rotation around X tilts pitch
                                    val yawRateRad = -ev.values[1]
                                    val pitchRateRad = ev.values[0]
                                    val deltaYawDeg = Math.toDegrees(yawRateRad.toDouble()).toFloat() * dt * 1.35f
                                    val deltaPitch = pitchRateRad * dt * 0.22f
                                    if (abs(deltaYawDeg) > 0.08f || abs(deltaPitch) > 0.0015f) {
                                        viewModel.applyVrCameraSensorMotion(
                                            panYawDeltaDeg = deltaYawDeg,
                                            tiltPitchDelta = deltaPitch,
                                            walkForwardDeltaCm = 0f,
                                            flowX = (deltaYawDeg / 2.2f).coerceIn(-2f, 2f),
                                            flowY = (deltaPitch * 40f).coerceIn(-2f, 2f)
                                        )
                                    }
                                }
                            }
                            lastGyroTimestampNs = ev.timestamp
                        }

                        Sensor.TYPE_ACCELEROMETER -> {
                            val ax = ev.values[0]
                            val ay = ev.values[1]
                            val az = ev.values[2]
                            val mag = sqrt(ax * ax + ay * ay + az * az)
                            val delta = mag - smoothedAccelMag
                            smoothedAccelMag = smoothedAccelMag * 0.85f + mag * 0.15f

                            val nowMs = System.currentTimeMillis()
                            if (delta > 2.35f && nowMs - lastStepTimestampMs > 380L) {
                                lastStepTimestampMs = nowMs
                                // Physical walking step detected -> advance forward in 3D kitchen!
                                viewModel.applyVrCameraSensorMotion(
                                    panYawDeltaDeg = 0f,
                                    tiltPitchDelta = 0f,
                                    walkForwardDeltaCm = 14f,
                                    flowX = 0f,
                                    flowY = -0.8f,
                                    detectedStep = true
                                )
                            }
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            if (gyroSensor != null) {
                sensorManager.registerListener(sensorListener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
            }
            if (accelSensor != null) {
                sensorManager.registerListener(sensorListener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            }

            onDispose {
                sensorManager?.unregisterListener(sensorListener)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // =========================================================================
        // 1. TOP-START: LIVE CAMERA SENSOR OPTICAL-FLOW TRACKER WINDOW (PiP)
        // =========================================================================
        if (state.vrCameraPipVisible) {
            Surface(
                color = Color(0xFF0E171D).copy(alpha = 0.88f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 64.dp, start = 12.dp)
                    .width(210.dp)
                    .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                    .testTag("vr_camera_sensor_pip_card")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.vrCameraSensorTrackingEnabled) Color(0xFF00E676) else Color(0xFFFFB300))
                            )
                            Text(
                                text = "حساس كاميرا VR الحي",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${state.cameraYawDeg.roundToInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF16242C))
                    ) {
                        if (hasCameraPermission && state.vrCameraSensorTrackingEnabled) {
                            val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
                            DisposableEffect(Unit) {
                                onDispose {
                                    runCatching { analysisExecutor.shutdown() }
                                }
                            }

                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    }
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        runCatching {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.surfaceProvider = previewView.surfaceProvider
                                            }
                                            val imageAnalysis = ImageAnalysis.Builder()
                                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .build()
                                                .also { analysis ->
                                                    analysis.setAnalyzer(
                                                        analysisExecutor,
                                                        RealTimeVrCameraAnalyzer { panDeg, tiltDelta, walkCm, fx, fy ->
                                                            previewView.post {
                                                                if (currentState.vrCameraSensorTrackingEnabled) {
                                                                    viewModel.applyVrCameraSensorMotion(
                                                                        panYawDeltaDeg = panDeg,
                                                                        tiltPitchDelta = tiltDelta,
                                                                        walkForwardDeltaCm = walkCm,
                                                                        flowX = fx,
                                                                        flowY = fy
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageAnalysis
                                            )
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!hasCameraPermission) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "فعّل الكاميرا لتتبع حركة المشي والالتفات بصرياً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("vr_grant_camera_permission_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تفعيل الكاميرا", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "تتبع الحساس متوقف مؤقتاً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Optical-Flow Grid & Motion Vector Radar Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stepX = size.width / 4f
                            val stepY = size.height / 3f
                            for (i in 1..3) {
                                drawLine(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                                    start = Offset(i * stepX, 0f),
                                    end = Offset(i * stepX, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (j in 1..2) {
                                drawLine(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                                    start = Offset(0f, j * stepY),
                                    end = Offset(size.width, j * stepY),
                                    strokeWidth = 1f
                                )
                            }
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                                radius = 12f,
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.5f)
                            )
                            val vx = (cx + state.vrSensorDeltaFlowX * 24f).coerceIn(8f, size.width - 8f)
                            val vy = (cy + state.vrSensorDeltaFlowY * 24f).coerceIn(8f, size.height - 8f)
                            drawLine(
                                color = Color(0xFF00E676),
                                start = Offset(cx, cy),
                                end = Offset(vx, vy),
                                strokeWidth = 3f
                            )
                            drawCircle(
                                color = Color(0xFF00E676),
                                radius = 4f,
                                center = Offset(vx, vy)
                            )
                        }
                    }

                    Text(
                        text = "الموقع: تقدم ${state.firstPersonStepForwardCm.roundToInt()} سم • جانبي ${state.vrStrafeLateralCm.roundToInt()} سم",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB2EBF2)
                    )
                    Text(
                        text = "ارتفاع العين: ${state.vrEyeHeightCm.roundToInt()} سم • خطوات: ${state.vrSensorStepCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // =========================================================================
        // 2. BOTTOM-CENTER: INTERACTIVE VR WALK, PAN, TILT & HEADSET CONTROLS HUD
        // =========================================================================
        Surface(
            color = Color(0xFF0D161C).copy(alpha = 0.90f),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp, start = 12.dp, end = 12.dp)
                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                .testTag("vr_navigation_control_pad")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: VR Mode Quick Toggles (Sensor Tracking, Stereoscopic Split, Auto-Walk, PiP, Recenter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.vrCameraSensorTrackingEnabled,
                        onClick = { viewModel.toggleVrCameraSensorTracking() },
                        label = {
                            Text(
                                if (state.vrCameraSensorTrackingEnabled) "تتبع الكاميرا والحساس: نشط" else "تتبع الكاميرا: متوقف",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.testTag("vr_toggle_sensor_tracking_chip")
                    )

                    FilterChip(
                        selected = state.vrStereoscopicSplitEnabled,
                        onClick = { viewModel.toggleVrStereoscopicSplit() },
                        label = {
                            Text(
                                if (state.vrStereoscopicSplitEnabled) "نظارة VR مزدوجة العدسة" else "شاشة VR كاملة",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.testTag("vr_toggle_stereoscopic_chip")
                    )

                    FilterChip(
                        selected = state.vrAutoWalkEnabled,
                        onClick = { viewModel.toggleVrAutoWalk() },
                        label = {
                            Text(
                                if (state.vrAutoWalkEnabled) "مشي تلقائي: نشط" else "مشي تلقائي",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.testTag("vr_toggle_autowalk_chip")
                    )

                    FilterChip(
                        selected = state.vrCameraPipVisible,
                        onClick = { viewModel.toggleVrCameraPipVisible() },
                        label = { Text("نافذة حساس الكاميرا", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.testTag("vr_toggle_pip_chip")
                    )

                    FilledTonalButton(
                        onClick = { viewModel.resetVrWalkthroughPosition() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("vr_reset_position_button")
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة للمركز", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Row 2: Walk Forward / Back / Strafe Left / Strafe Right + Pan/Tilt + Eye Height
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Strafe Left
                    FilledTonalButton(
                        onClick = { viewModel.moveVrWalkthroughPosition(forwardDeltaCm = 0f, strafeDeltaCm = -22f) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("vr_walk_left_button")
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "خطوة يسار", modifier = Modifier.size(16.dp))
                        Text("خطوة يسار", style = MaterialTheme.typography.labelSmall)
                    }

                    // Walk Forward
                    Button(
                        onClick = { viewModel.moveVrWalkthroughPosition(forwardDeltaCm = 24f, strafeDeltaCm = 0f) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ACC1)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("vr_walk_forward_button")
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "تقدم للأمام", modifier = Modifier.size(16.dp))
                        Text("تقدم للأمام (+24 سم)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    // Walk Backward
                    FilledTonalButton(
                        onClick = { viewModel.moveVrWalkthroughPosition(forwardDeltaCm = -24f, strafeDeltaCm = 0f) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("vr_walk_backward_button")
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تراجع للخلف", modifier = Modifier.size(16.dp))
                        Text("تراجع للخلف", style = MaterialTheme.typography.labelSmall)
                    }

                    // Strafe Right
                    FilledTonalButton(
                        onClick = { viewModel.moveVrWalkthroughPosition(forwardDeltaCm = 0f, strafeDeltaCm = 22f) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("vr_walk_right_button")
                    ) {
                        Text("خطوة يمين", style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "خطوة يمين", modifier = Modifier.size(16.dp))
                    }

                    // Eye Height Up / Down
                    IconButton(
                        onClick = { viewModel.moveVrWalkthroughPosition(0f, 0f, eyeHeightDeltaCm = 10f) },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("vr_eye_height_up_button")
                    ) {
                        Text("عين+", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { viewModel.moveVrWalkthroughPosition(0f, 0f, eyeHeightDeltaCm = -10f) },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("vr_eye_height_down_button")
                    ) {
                        Text("عين-", style = MaterialTheme.typography.labelSmall, color = Color(0xFF80DEEA), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
