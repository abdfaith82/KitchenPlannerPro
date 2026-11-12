package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * تخزين محلي للنسخ التلقائية.
 *
 * تم استبدال Room (الذي كان يعتمد على كود مولَّد مكتوب يدوياً بواجهات داخلية هشّة وبدون KSP)
 * بـ SQLiteOpenHelper المباشر: لا يحتاج معالج توليد كود، ولا يتأثر بتغيّر إصدارات Room.
 */
data class LocalProjectSnapshotEntity(
    val id: Long = 0L,
    val projectId: String,
    val projectName: String,
    val shapeCode: String,
    val styleId: String,
    val roomWidthMm: Int,
    val roomDepthMm: Int,
    val roomHeightMm: Int,
    val cabinetFinish: String,
    val countertopMaterial: String,
    val handleStyle: String,
    val wallColorHex: String,
    val floorMaterial: String,
    val itemsData: String,
    val totalPriceSar: Int,
    val ergonomicScore: Int,
    val snapshotReason: String,
    val isAutoSave: Boolean = true,
    val timestampMillis: Long = System.currentTimeMillis()
)

interface ProjectSnapshotDao {
    fun observeRecentSnapshots(): Flow<List<LocalProjectSnapshotEntity>>
    suspend fun getLatestSnapshot(): LocalProjectSnapshotEntity?
    suspend fun insertSnapshot(snapshot: LocalProjectSnapshotEntity): Long
    suspend fun deleteSnapshotById(id: Long)
    suspend fun pruneOldSnapshots(keepCount: Int = 35)
}

private const val TABLE = "project_snapshots"

private class SnapshotSqlHelper(context: Context, name: String?) :
    SQLiteOpenHelper(context, name, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "projectId TEXT NOT NULL, projectName TEXT NOT NULL, shapeCode TEXT NOT NULL, " +
                "styleId TEXT NOT NULL, roomWidthMm INTEGER NOT NULL, roomDepthMm INTEGER NOT NULL, " +
                "roomHeightMm INTEGER NOT NULL, cabinetFinish TEXT NOT NULL, " +
                "countertopMaterial TEXT NOT NULL, handleStyle TEXT NOT NULL, " +
                "wallColorHex TEXT NOT NULL, floorMaterial TEXT NOT NULL, itemsData TEXT NOT NULL, " +
                "totalPriceSar INTEGER NOT NULL, ergonomicScore INTEGER NOT NULL, " +
                "snapshotReason TEXT NOT NULL, isAutoSave INTEGER NOT NULL, " +
                "timestampMillis INTEGER NOT NULL)"
        )
    }

    // يعادل fallbackToDestructiveMigration: النسخ التلقائية بيانات مؤقتة.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}

private class SqliteProjectSnapshotDao(private val helper: SnapshotSqlHelper) : ProjectSnapshotDao {

    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    private fun Cursor.toEntity() = LocalProjectSnapshotEntity(
        id = getLong(getColumnIndexOrThrow("id")),
        projectId = getString(getColumnIndexOrThrow("projectId")),
        projectName = getString(getColumnIndexOrThrow("projectName")),
        shapeCode = getString(getColumnIndexOrThrow("shapeCode")),
        styleId = getString(getColumnIndexOrThrow("styleId")),
        roomWidthMm = getInt(getColumnIndexOrThrow("roomWidthMm")),
        roomDepthMm = getInt(getColumnIndexOrThrow("roomDepthMm")),
        roomHeightMm = getInt(getColumnIndexOrThrow("roomHeightMm")),
        cabinetFinish = getString(getColumnIndexOrThrow("cabinetFinish")),
        countertopMaterial = getString(getColumnIndexOrThrow("countertopMaterial")),
        handleStyle = getString(getColumnIndexOrThrow("handleStyle")),
        wallColorHex = getString(getColumnIndexOrThrow("wallColorHex")),
        floorMaterial = getString(getColumnIndexOrThrow("floorMaterial")),
        itemsData = getString(getColumnIndexOrThrow("itemsData")),
        totalPriceSar = getInt(getColumnIndexOrThrow("totalPriceSar")),
        ergonomicScore = getInt(getColumnIndexOrThrow("ergonomicScore")),
        snapshotReason = getString(getColumnIndexOrThrow("snapshotReason")),
        isAutoSave = getInt(getColumnIndexOrThrow("isAutoSave")) != 0,
        timestampMillis = getLong(getColumnIndexOrThrow("timestampMillis"))
    )

    private fun queryRecent(limit: Int): List<LocalProjectSnapshotEntity> {
        val out = ArrayList<LocalProjectSnapshotEntity>()
        helper.readableDatabase.rawQuery(
            "SELECT * FROM $TABLE ORDER BY timestampMillis DESC, id DESC LIMIT $limit",
            null
        ).use { c -> while (c.moveToNext()) out.add(c.toEntity()) }
        return out
    }

    override fun observeRecentSnapshots(): Flow<List<LocalProjectSnapshotEntity>> =
        changes.onStart { emit(Unit) }
            .map { queryRecent(40) }
            .flowOn(Dispatchers.IO)

    override suspend fun getLatestSnapshot(): LocalProjectSnapshotEntity? =
        withContext(Dispatchers.IO) { queryRecent(1).firstOrNull() }

    override suspend fun insertSnapshot(snapshot: LocalProjectSnapshotEntity): Long =
        withContext(Dispatchers.IO) {
            val cv = ContentValues().apply {
                // id = 0 يعني «توليد تلقائي»
                if (snapshot.id != 0L) put("id", snapshot.id)
                put("projectId", snapshot.projectId)
                put("projectName", snapshot.projectName)
                put("shapeCode", snapshot.shapeCode)
                put("styleId", snapshot.styleId)
                put("roomWidthMm", snapshot.roomWidthMm)
                put("roomDepthMm", snapshot.roomDepthMm)
                put("roomHeightMm", snapshot.roomHeightMm)
                put("cabinetFinish", snapshot.cabinetFinish)
                put("countertopMaterial", snapshot.countertopMaterial)
                put("handleStyle", snapshot.handleStyle)
                put("wallColorHex", snapshot.wallColorHex)
                put("floorMaterial", snapshot.floorMaterial)
                put("itemsData", snapshot.itemsData)
                put("totalPriceSar", snapshot.totalPriceSar)
                put("ergonomicScore", snapshot.ergonomicScore)
                put("snapshotReason", snapshot.snapshotReason)
                put("isAutoSave", if (snapshot.isAutoSave) 1 else 0)
                put("timestampMillis", snapshot.timestampMillis)
            }
            val rowId = helper.writableDatabase.insertWithOnConflict(
                TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE
            )
            changes.tryEmit(Unit)
            rowId
        }

    override suspend fun deleteSnapshotById(id: Long) {
        withContext(Dispatchers.IO) {
            helper.writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
            changes.tryEmit(Unit)
        }
    }

    override suspend fun pruneOldSnapshots(keepCount: Int) {
        withContext(Dispatchers.IO) {
            helper.writableDatabase.execSQL(
                "DELETE FROM $TABLE WHERE id NOT IN " +
                    "(SELECT id FROM $TABLE ORDER BY timestampMillis DESC, id DESC LIMIT ?)",
                arrayOf<Any>(keepCount.coerceAtLeast(0))
            )
            changes.tryEmit(Unit)
        }
    }
}

class LocalKitchenDatabase private constructor(context: Context, fileName: String?) {
    private val helper = SnapshotSqlHelper(context.applicationContext, fileName)
    private val dao: ProjectSnapshotDao = SqliteProjectSnapshotDao(helper)

    fun projectSnapshotDao(): ProjectSnapshotDao = dao

    fun close() = helper.close()

    companion object {
        @Volatile
        private var INSTANCE: LocalKitchenDatabase? = null

        fun getInstance(context: Context): LocalKitchenDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalKitchenDatabase(context, "kitchen_cad_offline_autosave.db")
                    .also { INSTANCE = it }
            }

        /** قاعدة في الذاكرة للاختبارات. */
        fun createInMemory(context: Context): LocalKitchenDatabase =
            LocalKitchenDatabase(context, null)
    }
}

class LocalSnapshotRepository(private val dao: ProjectSnapshotDao) {
    val recentSnapshots: Flow<List<LocalProjectSnapshotEntity>> = dao.observeRecentSnapshots()

    suspend fun saveSnapshot(snapshot: LocalProjectSnapshotEntity): Long {
        val insertedId = dao.insertSnapshot(snapshot)
        dao.pruneOldSnapshots(35)
        return insertedId
    }

    suspend fun getLatestSnapshot(): LocalProjectSnapshotEntity? = dao.getLatestSnapshot()

    suspend fun deleteSnapshot(id: Long) = dao.deleteSnapshotById(id)
}
