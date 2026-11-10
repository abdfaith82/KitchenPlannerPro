package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.KitchenCatalogData
import com.example.data.model.KitchenCatalogUnit
import com.example.data.model.KitchenProject
import com.example.data.model.QuoteRequest
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KitchenPlannerRepository(
    private val db: FirebaseFirestore,
    private val fallbackUserId: String? = null
) {

    constructor(context: Context, fallbackUserId: String? = null) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        fallbackUserId
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: fallbackUserId?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google or in Guest Studio mode before accessing Firestore.")
    }

    // --- 1. Kitchen Units Catalog (Firestore Repository Methods) ---

    fun observeCatalogUnits(userId: String): Flow<List<KitchenCatalogUnit>> {
        val path = COLLECTION_CATALOG
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    KitchenCatalogUnit::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception) ?: error
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun fetchCatalogUnits(userId: String): Result<List<KitchenCatalogUnit>> = runCatching {
        val path = COLLECTION_CATALOG
        try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.toObjects(
                KitchenCatalogUnit::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun createCatalogUnit(unit: KitchenCatalogUnit): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = if (unit.id.isNotBlank()) {
            db.collection(COLLECTION_CATALOG).document(unit.id)
        } else {
            db.collection(COLLECTION_CATALOG).document()
        }
        val payload = mapOf(
            "userId" to uid,
            "code" to unit.code.trim().take(40).ifEmpty { "BASE_60" },
            "nameAr" to unit.nameAr.trim().take(120).ifEmpty { "وحدة مطبخ مخصصة" },
            "category" to unit.category.trim().take(30).ifEmpty { "BASE" },
            "subType" to unit.subType.trim().take(40).ifEmpty { "CABINET" },
            "widthCm" to unit.widthCm.coerceIn(30L, 300L),
            "depthCm" to unit.depthCm.coerceIn(25L, 150L),
            "heightCm" to unit.heightCm.coerceIn(30L, 260L),
            "elevationCm" to unit.elevationCm.coerceIn(0L, 220L),
            "priceSar" to unit.priceSar.coerceIn(0L, 500000L),
            "colorHex" to unit.colorHex,
            "materialFinish" to unit.materialFinish.trim().take(80).ifEmpty { "جوز أمريكي طبيعي" },
            "countertopIncluded" to unit.countertopIncluded,
            "descriptionAr" to unit.descriptionAr.trim().take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun seedDefaultCatalogIfEmpty(userId: String): Result<Int> = runCatching {
        val existing = fetchCatalogUnits(userId).getOrThrow()
        if (existing.isNotEmpty()) {
            return@runCatching 0
        }
        val defaults = KitchenCatalogData.defaultCatalogUnitsForUser(userId)
        var createdCount = 0
        defaults.forEach { defaultUnit ->
            val sanitizedUid = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(32)
            val docId = "cat_${sanitizedUid}_${defaultUnit.code.lowercase()}"
            createCatalogUnit(defaultUnit.copy(id = docId)).getOrThrow()
            createdCount++
        }
        createdCount
    }

    suspend fun deleteCatalogUnit(unitId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection(COLLECTION_CATALOG).document(unitId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 2. Kitchen 2D/3D Projects (Save, Retrieve, Modify, Duplicate, Delete) ---

    fun observeUserProjects(userId: String): Flow<List<KitchenProject>> {
        val path = COLLECTION_PROJECTS
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    KitchenProject::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception) ?: error
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun fetchUserProjects(userId: String): Result<List<KitchenProject>> = runCatching {
        val path = COLLECTION_PROJECTS
        try {
            val snapshot = db.collection(path)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.toObjects(
                KitchenProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    fun observeUserQuoteRequests(userId: String): Flow<List<QuoteRequest>> {
        val path = COLLECTION_QUOTES
        return db.collection(path)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    QuoteRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception) ?: error
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getProjectById(projectId: String): Result<KitchenProject?> = runCatching {
        val docRef = db.collection(COLLECTION_PROJECTS).document(projectId)
        try {
            val snap = docRef.get().await()
            snap.toObject(
                KitchenProject::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createProject(project: KitchenProject): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = if (project.id.isNotBlank()) {
            db.collection(COLLECTION_PROJECTS).document(project.id)
        } else {
            db.collection(COLLECTION_PROJECTS).document()
        }
        val payload = mapOf(
            "userId" to uid,
            "name" to project.name.trim().take(120).ifEmpty { "مطبخ جديد" },
            "shape" to project.shape,
            "styleId" to project.styleId,
            "roomWidthCm" to project.roomWidthCm.coerceIn(150L, 1200L),
            "roomDepthCm" to project.roomDepthCm.coerceIn(150L, 1200L),
            "roomHeightCm" to project.roomHeightCm.coerceIn(200L, 450L),
            "cabinetFinish" to project.cabinetFinish.take(80),
            "countertopMaterial" to project.countertopMaterial.take(80),
            "handleStyle" to project.handleStyle.take(80),
            "wallColorHex" to project.wallColorHex.take(12),
            "floorMaterial" to project.floorMaterial.take(80),
            "itemsData" to project.itemsData.take(20000),
            "totalPriceSar" to project.totalPriceSar.coerceIn(0L, 10000000L),
            "ergonomicScore" to project.ergonomicScore.coerceIn(0L, 100L),
            "notes" to project.notes.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProject(project: KitchenProject): Result<Unit> = runCatching {
        requireUserId()
        require(project.id.isNotBlank()) { "Project ID must not be blank for update" }
        val docRef = db.collection(COLLECTION_PROJECTS).document(project.id)
        val updates = mapOf(
            "name" to project.name.trim().take(120).ifEmpty { "مطبخ معدل" },
            "shape" to project.shape,
            "styleId" to project.styleId,
            "roomWidthCm" to project.roomWidthCm.coerceIn(150L, 1200L),
            "roomDepthCm" to project.roomDepthCm.coerceIn(150L, 1200L),
            "roomHeightCm" to project.roomHeightCm.coerceIn(200L, 450L),
            "cabinetFinish" to project.cabinetFinish.take(80),
            "countertopMaterial" to project.countertopMaterial.take(80),
            "handleStyle" to project.handleStyle.take(80),
            "wallColorHex" to project.wallColorHex.take(12),
            "floorMaterial" to project.floorMaterial.take(80),
            "itemsData" to project.itemsData.take(20000),
            "totalPriceSar" to project.totalPriceSar.coerceIn(0L, 10000000L),
            "ergonomicScore" to project.ergonomicScore.coerceIn(0L, 100L),
            "notes" to project.notes.take(1000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun duplicateProject(project: KitchenProject): Result<String> {
        val copyModel = project.copy(
            id = "",
            name = "${project.name.take(95)} (نسخة)"
        )
        return createProject(copyModel)
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection(COLLECTION_PROJECTS).document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 3. Showroom Quote Requests ---

    suspend fun submitQuoteRequest(quote: QuoteRequest): Result<String> = runCatching {
        val uid = requireUserId()
        require(quote.projectId.isNotBlank()) { "projectId is required" }
        val docRef = db.collection(COLLECTION_QUOTES).document()
        val payload = mapOf(
            "userId" to uid,
            "projectId" to quote.projectId,
            "projectName" to quote.projectName.trim().take(120).ifEmpty { "مشروع مطبخ" },
            "showroomName" to quote.showroomName.trim().take(120).ifEmpty { "معرض معتمد" },
            "customerName" to quote.customerName.trim().take(100),
            "customerPhone" to quote.customerPhone.trim().take(30),
            "city" to quote.city.trim().take(80),
            "preferredInstallWindow" to quote.preferredInstallWindow.trim().take(80),
            "estimatedBudgetSar" to quote.estimatedBudgetSar.coerceIn(0L, 10000000L),
            "specialNotes" to quote.specialNotes.trim().take(1000),
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun cancelQuoteRequest(quoteId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection(COLLECTION_QUOTES).document(quoteId)
        val updates = mapOf(
            "status" to "cancelled",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    companion object {
        const val COLLECTION_CATALOG = "kitchen_units_catalog"
        const val COLLECTION_PROJECTS = "kitchen_projects"
        const val COLLECTION_QUOTES = "quote_requests"
    }
}
