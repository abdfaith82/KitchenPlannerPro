package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.LocalKitchenDatabase
import com.example.data.local.LocalSnapshotRepository
import com.example.data.repository.KitchenPlannerRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.BomAndQuoteRequestsScreen
import com.example.ui.screens.CamAndJsonStudioScreen
import com.example.ui.screens.CatalogAndMaterialsScreen
import com.example.ui.screens.LiveCameraArScreen
import com.example.ui.screens.ProjectsAndTemplatesScreen
import com.example.ui.screens.StudioTabScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.KitchenPlannerViewModel
import com.example.ai.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

private const val PREFS_NAME = "kitchen_planner_guest_prefs"
private const val KEY_GUEST_USER_ID = "guest_studio_user_id"
private const val KEY_BYPASS_ACTIVE = "guest_bypass_active"

fun getOrCreateGuestStudioUserId(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val existing = prefs.getString(KEY_GUEST_USER_ID, null)
    if (!existing.isNullOrBlank() && existing.startsWith("guest_studio_")) {
        return existing
    }
    val generated = "guest_studio_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    prefs.edit().putString(KEY_GUEST_USER_ID, generated).apply()
    return generated
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isBypassSignInActive by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_BYPASS_ACTIVE, false))
    }

    val user = currentUser
    if (user == null && !isBypassSignInActive) {
        AuthScreen(
            onAuthSuccess = {
                prefs.edit().putBoolean(KEY_BYPASS_ACTIVE, false).apply()
                isBypassSignInActive = false
            },
            onBypassSignIn = {
                prefs.edit().putBoolean(KEY_BYPASS_ACTIVE, true).apply()
                isBypassSignInActive = true
            }
        )
    } else {
        val resolvedUserId = user?.uid ?: remember { getOrCreateGuestStudioUserId(context) }
        val resolvedDisplayName = if (user != null) {
            user.displayName ?: user.email ?: "مهندس التصميم"
        } else {
            "وضع الاستوديو المباشر (بدون Google)"
        }
        MainKitchenPlannerApp(
            currentUserId = resolvedUserId,
            userDisplayName = resolvedDisplayName,
            isGuestBypassMode = (user == null),
            onExitGuestOrSignOut = {
                prefs.edit().putBoolean(KEY_BYPASS_ACTIVE, false).apply()
                isBypassSignInActive = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainKitchenPlannerApp(
    currentUserId: String,
    userDisplayName: String,
    isGuestBypassMode: Boolean = false,
    onExitGuestOrSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: KitchenPlannerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val localDb = LocalKitchenDatabase.getInstance(app)
                KitchenPlannerViewModel(
                    repository = KitchenPlannerRepository(
                        db = db,
                        fallbackUserId = currentUserId
                    ),
                    currentUserId = currentUserId,
                    localSnapshotRepository = LocalSnapshotRepository(localDb.projectSnapshotDao())
                )
            }
        }
    )

    val studioState by viewModel.studioState.collectAsStateWithLifecycle()
    val catalogUnitsState by viewModel.catalogUnitsState.collectAsStateWithLifecycle()
    val projectsState by viewModel.userProjectsState.collectAsStateWithLifecycle()
    val quotesState by viewModel.userQuotesState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.triggerImmediateAutoSaveToRoom(
                    reason = "حفظ طوارئ تلقائي عند إغلاق التطبيق أو الخروج (Room DB)"
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    // Sub-workspace inside Studio: 0 = CAD 2D/3D, 1 = Live Camera AR, 2 = CAM / Cut-List / JSON v2.0 / 8K Render
    var studioWorkspaceMode by rememberSaveable { mutableIntStateOf(0) }
    var isGlobalBarsHidden by rememberSaveable { mutableStateOf(false) }

    if (selectedTab != 0 || studioWorkspaceMode != 0) {
        BackHandler {
            if (studioWorkspaceMode != 0) {
                studioWorkspaceMode = 0
            } else {
                selectedTab = 0
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            AnimatedVisibility(visible = !isGlobalBarsHidden) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGuestBypassMode) {
                                    userDisplayName
                                } else {
                                    "مرحباً، $userDisplayName"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        FilterChip(
                            selected = false,
                            onClick = { isGlobalBarsHidden = true },
                            label = {
                                Text(
                                    text = "إخفاء الأشرطة",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("hide_global_bars_button")
                        )
                        IconButton(
                            onClick = { viewModel.saveCurrentProjectToCloud() },
                            enabled = !studioState.isSaving,
                            modifier = Modifier.testTag("top_bar_save_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = stringResource(R.string.save_cloud)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isGuestBypassMode) {
                                    onExitGuestOrSignOut()
                                } else {
                                    signOutUser(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = onExitGuestOrSignOut,
                                        scope = scope
                                    )
                                }
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = if (isGuestBypassMode) {
                                    Icons.AutoMirrored.Filled.Login
                                } else {
                                    Icons.AutoMirrored.Filled.Logout
                                },
                                contentDescription = if (isGuestBypassMode) {
                                    "تسجيل الدخول عبر Google"
                                } else {
                                    stringResource(R.string.sign_out)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = !isGlobalBarsHidden) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0 && studioWorkspaceMode == 0,
                        onClick = {
                            selectedTab = 0
                            studioWorkspaceMode = 0
                        },
                        icon = { Icon(Icons.Default.ViewInAr, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_studio)) },
                        modifier = Modifier.testTag("nav_tab_studio")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.FolderSpecial, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_projects)) },
                        modifier = Modifier.testTag("nav_tab_projects")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.DashboardCustomize, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_catalog)) },
                        modifier = Modifier.testTag("nav_tab_catalog")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.RequestQuote, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_bom_quotes)) },
                        modifier = Modifier.testTag("nav_tab_quotes")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4 || (selectedTab == 0 && studioWorkspaceMode != 0),
                        onClick = {
                            selectedTab = 4
                            if (studioWorkspaceMode == 0) studioWorkspaceMode = 2
                        },
                        icon = { Icon(Icons.Default.PrecisionManufacturing, contentDescription = null) },
                        label = { Text("التصنيع وAR") },
                        modifier = Modifier.testTag("nav_tab_cam_ar")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Engineering Workspace Mode Strip (CAD 1mm | Live Camera AR | CAM, Nesting, JSON v2.0 & 8K Render)
                AnimatedVisibility(visible = !isGlobalBarsHidden && (selectedTab == 0 || selectedTab == 4)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = studioWorkspaceMode == 0 && selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                studioWorkspaceMode = 0
                            },
                            label = { Text("مخطط 2D/3D CAD (دقة 1 ملم)") },
                            leadingIcon = {
                                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("workspace_submode_cad")
                        )
                        FilterChip(
                            selected = studioWorkspaceMode == 1,
                            onClick = {
                                selectedTab = 0
                                studioWorkspaceMode = 1
                            },
                            label = { Text("الكاميرا الحية AR + مسح الحائط") },
                            leadingIcon = {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("workspace_submode_ar")
                        )
                        FilterChip(
                            selected = studioWorkspaceMode == 2 || selectedTab == 4,
                            onClick = {
                                selectedTab = 4
                                studioWorkspaceMode = 2
                            },
                            label = { Text("التصنيع CAM • JSON v2.0 • رندر 8K") },
                            leadingIcon = {
                                Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("workspace_submode_cam_json")
                        )
                    }
                }

                val bannerText = studioState.errorBannerMessage ?: studioState.statusBannerMessage
                val isError = studioState.errorBannerMessage != null
                AnimatedVisibility(visible = bannerText != null) {
                    Surface(
                        color = if (isError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bannerText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearBannerMessages() }) {
                                Text("إغلاق")
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> when (studioWorkspaceMode) {
                            1 -> LiveCameraArScreen(
                                state = studioState,
                                catalogUnitsState = catalogUnitsState,
                                viewModel = viewModel,
                                onOpenJsonTab = {
                                    selectedTab = 4
                                    studioWorkspaceMode = 2
                                }
                            )
                            2 -> CamAndJsonStudioScreen(
                                state = studioState,
                                viewModel = viewModel
                            )
                            else -> StudioTabScreen(
                                state = studioState,
                                catalogUnitsState = catalogUnitsState,
                                projectsState = projectsState,
                                viewModel = viewModel,
                                onNavigateToQuote = { selectedTab = 3 },
                                onNavigateToProjects = { selectedTab = 1 }
                            )
                        }
                        1 -> ProjectsAndTemplatesScreen(
                            studioState = studioState,
                            projectsState = projectsState,
                            viewModel = viewModel,
                            onOpenStudio = {
                                selectedTab = 0
                                studioWorkspaceMode = 0
                            }
                        )
                        2 -> CatalogAndMaterialsScreen(
                            state = studioState,
                            catalogUnitsState = catalogUnitsState,
                            viewModel = viewModel,
                            onOpenStudio = {
                                selectedTab = 0
                                studioWorkspaceMode = 0
                            }
                        )
                        3 -> BomAndQuoteRequestsScreen(
                            state = studioState,
                            quotesState = quotesState,
                            viewModel = viewModel
                        )
                        4 -> CamAndJsonStudioScreen(
                            state = studioState,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Floating Global Navigation Capsule when top/bottom bars are hidden
            AnimatedVisibility(
                visible = isGlobalBarsHidden,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                    tonalElevation = 10.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.testTag("floating_global_nav_capsule")
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { isGlobalBarsHidden = false },
                            modifier = Modifier.testTag("show_global_bars_button")
                        ) {
                            Text(
                                text = "إظهار الأشرطة",
                                color = MaterialTheme.colorScheme.inversePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = {
                                selectedTab = 0
                                studioWorkspaceMode = 0
                            }
                        ) {
                            Text("2D/3D", color = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                        TextButton(
                            onClick = {
                                selectedTab = 0
                                studioWorkspaceMode = 1
                            }
                        ) {
                            Text("AR", color = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                        TextButton(
                            onClick = {
                                selectedTab = 4
                                studioWorkspaceMode = 2
                            }
                        ) {
                            Text("CAM/JSON", color = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                        TextButton(onClick = { selectedTab = 2 }) {
                            Text("الكتالوج", color = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                        TextButton(onClick = { selectedTab = 1 }) {
                            Text("المشاريع", color = MaterialTheme.colorScheme.inverseOnSurface)
                        }
                    }
                }
            }
        }
    }
}
