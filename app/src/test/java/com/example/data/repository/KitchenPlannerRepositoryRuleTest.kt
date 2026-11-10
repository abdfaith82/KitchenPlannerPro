package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CameraPreset3D
import com.example.data.model.KitchenCatalogUnit
import com.example.data.model.KitchenItemsSerializer
import com.example.data.model.KitchenProject
import com.example.data.model.ModuleType
import com.example.data.model.PlacedKitchenItem
import com.example.data.model.QuoteRequest
import com.example.data.model.Saved3DViewportSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class KitchenPlannerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleProject(name: String = "مطبخ اختباري حديث"): KitchenProject {
        val sampleItems = listOf(
            PlacedKitchenItem("item_1", ModuleType.SINK_DOUBLE, 120, 0),
            PlacedKitchenItem("item_2", ModuleType.STOVE_HOB, 240, 0),
            PlacedKitchenItem("item_3", ModuleType.FRIDGE_SMART, 0, 120)
        )
        val viewport = Saved3DViewportSnapshot(
            cameraYawDeg = 42f,
            cameraPitchFactor = 0.56f,
            cameraZoom = 1.1f,
            cameraPreset = CameraPreset3D.ISOMETRIC_RIGHT,
            lightingModeName = "WARM_LED",
            doorsOpenPreview = true
        )
        return KitchenProject(
            id = "proj_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            name = name,
            shape = "ISLAND",
            styleId = "MODERN_WALNUT",
            roomWidthCm = 420L,
            roomDepthCm = 360L,
            roomHeightCm = 280L,
            cabinetFinish = "جوز أمريكي طبيعي",
            countertopMaterial = "رخام كالكاتا إيطالي",
            handleStyle = "نحاس مصقول",
            wallColorHex = "#F5F2EB",
            floorMaterial = "باركيه بلوط دافئ",
            itemsData = KitchenItemsSerializer.encodeWithViewport(sampleItems, viewport),
            totalPriceSar = 24500L,
            ergonomicScore = 92L,
            notes = "ملاحظات هندسية"
        )
    }

    private fun sampleCatalogUnit(nameAr: String = "فرن وموقد بلت إن ذكي") = KitchenCatalogUnit(
        id = "unit_${UUID.randomUUID().toString().replace("-", "").take(12)}",
        code = "STOVE",
        nameAr = nameAr,
        category = "APPLIANCE",
        subType = "OVEN",
        widthCm = 90L,
        depthCm = 60L,
        heightCm = 85L,
        elevationCm = 0L,
        priceSar = 4400L,
        colorHex = 0xFFC85A32L,
        materialFinish = "ستانلس ستيل وسيراميك أسود",
        countertopIncluded = true,
        descriptionAr = "فرن مدمج 90 سم مع سطح موقد سيراميك 4 شعلات"
    )

    @Test
    fun createAndObserveCatalogUnits_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KitchenPlannerRepository(firestore)
        val unit = sampleCatalogUnit("ثلاجة وفريزر مدمج دولابي")

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCatalogUnit(unit)
        }
        assertTrue("Expected createCatalogUnit to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val unitId = createResult.getOrThrow()

        val fetchedResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchCatalogUnits(uid)
        }
        assertTrue("Expected fetchCatalogUnits to succeed", fetchedResult.isSuccess)
        assertTrue(fetchedResult.getOrThrow().any { it.id == unitId && it.nameAr == "ثلاجة وفريزر مدمج دولابي" })

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCatalogUnits(uid).first { list -> list.any { it.id == unitId } }
        }
        assertTrue(observed.any { it.id == unitId })
    }

    @Test
    fun observeCatalogUnits_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = KitchenPlannerRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCatalogUnits("unauthenticated_uid").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected exception for unauthenticated catalog query", caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun createRetrieveAndModifyProject_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KitchenPlannerRepository(firestore)
        val project = sampleProject("مطبخ أليس الفاخر")

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProject(project)
        }
        assertTrue("Expected createProject to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val projectId = createResult.getOrThrow()

        val retrieved = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProjectById(projectId).getOrThrow()
        }
        assertNotNull("Expected getProjectById to return saved project", retrieved)
        val decoded = KitchenItemsSerializer.decodeWithViewport(retrieved!!.itemsData)
        assertEquals(3, decoded.items.size)
        assertEquals(true, decoded.viewport?.doorsOpenPreview)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateProject(
                retrieved.copy(
                    name = "مطبخ أليس المعدل 3D",
                    roomWidthCm = 480L,
                    countertopMaterial = "جرانيت جالكسي أسود"
                )
            )
        }
        assertTrue("Expected updateProject to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProjects(uid).first { list ->
                list.any { it.id == projectId && it.name == "مطبخ أليس المعدل 3D" }
            }
        }
        assertTrue(observed.any { it.id == projectId && it.roomWidthCm == 480L })
    }

    @Test
    fun createRetrieveAndModifyProject_guestBypassMode_succeeds() = runBlocking {
        auth.signOut()
        val guestUid = "guest_studio_architect_test_1"
        val repository = KitchenPlannerRepository(firestore, fallbackUserId = guestUid)
        val project = sampleProject("مطبخ وضع الضيف 3D").copy(userId = guestUid)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProject(project)
        }
        assertTrue("Expected guest createProject to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val projectId = createResult.getOrThrow()

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateProject(
                project.copy(
                    id = projectId,
                    name = "مطبخ الضيف بعد التعديل"
                )
            )
        }
        assertTrue("Expected guest updateProject to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchUserProjects(guestUid).getOrThrow()
        }
        assertTrue(fetched.any { it.id == projectId && it.name == "مطبخ الضيف بعد التعديل" })
    }

    @Test
    fun getProjectById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = KitchenPlannerRepository(firestore)
        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProject(sampleProject()).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = KitchenPlannerRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProjectById(projectId)
        }
        assertTrue("Expected cross-user getProjectById to fail", crossReadResult.isFailure)
        val firestoreEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = KitchenPlannerRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserProjects("unauthenticated_uid").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected exception for unauthenticated query", caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun submitAndCancelQuoteRequest_validParentProject_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KitchenPlannerRepository(firestore)
        val projectId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createProject(sampleProject("مطبخ الياسمين")).getOrThrow()
        }

        val quote = QuoteRequest(
            projectId = projectId,
            projectName = "مطبخ الياسمين",
            showroomName = "دار المطابخ الألمانية - الرياض",
            customerName = "سارة العتيبي",
            customerPhone = "+966551122334",
            city = "الرياض",
            preferredInstallWindow = "خلال 30 يوم",
            estimatedBudgetSar = 28000L,
            specialNotes = "يرجى إرسال مهندس رفع مقاسات"
        )

        val quoteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.submitQuoteRequest(quote)
        }
        assertTrue("Expected submitQuoteRequest to succeed: ${quoteResult.exceptionOrNull()}", quoteResult.isSuccess)
        val quoteId = quoteResult.getOrThrow()

        val cancelResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.cancelQuoteRequest(quoteId)
        }
        assertTrue("Expected cancelQuoteRequest to succeed: ${cancelResult.isOrNull()}", cancelResult.isSuccess)

        val observedQuotes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserQuoteRequests(uid).first { list ->
                list.any { it.id == quoteId && it.status == "cancelled" }
            }
        }
        assertTrue(observedQuotes.any { it.id == quoteId && it.status == "cancelled" })
    }

    private fun <T> Result<T>.isOrNull() = exceptionOrNull()

    private companion object {
        const val ALICE_EMAIL = "alice_kitchen@test.com"
        const val BOB_EMAIL = "bob_kitchen@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
