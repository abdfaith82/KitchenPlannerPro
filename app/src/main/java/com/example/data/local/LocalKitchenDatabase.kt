package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.EntityInsertAdapter
import androidx.room.Insert
import androidx.room.InvalidationTracker
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomOpenDelegate
import androidx.room.coroutines.createFlow
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.execSQL
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "project_snapshots")
data class LocalProjectSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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

@Dao
interface ProjectSnapshotDao {
    @Query("SELECT * FROM project_snapshots ORDER BY timestampMillis DESC LIMIT 40")
    fun observeRecentSnapshots(): Flow<List<LocalProjectSnapshotEntity>>

    @Query("SELECT * FROM project_snapshots ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun getLatestSnapshot(): LocalProjectSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: LocalProjectSnapshotEntity): Long

    @Query("DELETE FROM project_snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: Long)

    @Query(
        "DELETE FROM project_snapshots WHERE id NOT IN " +
            "(SELECT id FROM project_snapshots ORDER BY timestampMillis DESC LIMIT :keepCount)"
    )
    suspend fun pruneOldSnapshots(keepCount: Int = 35)
}

@Database(
    entities = [LocalProjectSnapshotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalKitchenDatabase : RoomDatabase() {
    abstract fun projectSnapshotDao(): ProjectSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: LocalKitchenDatabase? = null

        fun getInstance(context: Context): LocalKitchenDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalKitchenDatabase::class.java,
                    "kitchen_cad_offline_autosave.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
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

@Suppress("UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL")
class ProjectSnapshotDao_Impl(
    private val __db: RoomDatabase
) : ProjectSnapshotDao {

    private val __insertAdapterOfLocalProjectSnapshotEntity: EntityInsertAdapter<LocalProjectSnapshotEntity> =
        object : EntityInsertAdapter<LocalProjectSnapshotEntity>() {
            override fun createQuery(): String =
                "INSERT OR REPLACE INTO `project_snapshots` (`id`,`projectId`,`projectName`,`shapeCode`,`styleId`,`roomWidthMm`,`roomDepthMm`,`roomHeightMm`,`cabinetFinish`,`countertopMaterial`,`handleStyle`,`wallColorHex`,`floorMaterial`,`itemsData`,`totalPriceSar`,`ergonomicScore`,`snapshotReason`,`isAutoSave`,`timestampMillis`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

            override fun bind(statement: SQLiteStatement, entity: LocalProjectSnapshotEntity) {
                statement.bindLong(1, entity.id)
                statement.bindText(2, entity.projectId)
                statement.bindText(3, entity.projectName)
                statement.bindText(4, entity.shapeCode)
                statement.bindText(5, entity.styleId)
                statement.bindLong(6, entity.roomWidthMm.toLong())
                statement.bindLong(7, entity.roomDepthMm.toLong())
                statement.bindLong(8, entity.roomHeightMm.toLong())
                statement.bindText(9, entity.cabinetFinish)
                statement.bindText(10, entity.countertopMaterial)
                statement.bindText(11, entity.handleStyle)
                statement.bindText(12, entity.wallColorHex)
                statement.bindText(13, entity.floorMaterial)
                statement.bindText(14, entity.itemsData)
                statement.bindLong(15, entity.totalPriceSar.toLong())
                statement.bindLong(16, entity.ergonomicScore.toLong())
                statement.bindText(17, entity.snapshotReason)
                val isAuto = if (entity.isAutoSave) 1L else 0L
                statement.bindLong(18, isAuto)
                statement.bindLong(19, entity.timestampMillis)
            }
        }

    override suspend fun insertSnapshot(snapshot: LocalProjectSnapshotEntity): Long =
        performSuspending(__db, false, true) { connection ->
            __insertAdapterOfLocalProjectSnapshotEntity.insertAndReturnId(connection, snapshot)
        }

    override fun observeRecentSnapshots(): Flow<List<LocalProjectSnapshotEntity>> {
        val sql = "SELECT * FROM project_snapshots ORDER BY timestampMillis DESC LIMIT 40"
        return createFlow(__db, false, arrayOf("project_snapshots")) { connection ->
            val stmt = connection.prepare(sql)
            try {
                val colId = getColumnIndexOrThrow(stmt, "id")
                val colProjectId = getColumnIndexOrThrow(stmt, "projectId")
                val colProjectName = getColumnIndexOrThrow(stmt, "projectName")
                val colShapeCode = getColumnIndexOrThrow(stmt, "shapeCode")
                val colStyleId = getColumnIndexOrThrow(stmt, "styleId")
                val colRoomWidthMm = getColumnIndexOrThrow(stmt, "roomWidthMm")
                val colRoomDepthMm = getColumnIndexOrThrow(stmt, "roomDepthMm")
                val colRoomHeightMm = getColumnIndexOrThrow(stmt, "roomHeightMm")
                val colCabinetFinish = getColumnIndexOrThrow(stmt, "cabinetFinish")
                val colCountertopMaterial = getColumnIndexOrThrow(stmt, "countertopMaterial")
                val colHandleStyle = getColumnIndexOrThrow(stmt, "handleStyle")
                val colWallColorHex = getColumnIndexOrThrow(stmt, "wallColorHex")
                val colFloorMaterial = getColumnIndexOrThrow(stmt, "floorMaterial")
                val colItemsData = getColumnIndexOrThrow(stmt, "itemsData")
                val colTotalPriceSar = getColumnIndexOrThrow(stmt, "totalPriceSar")
                val colErgonomicScore = getColumnIndexOrThrow(stmt, "ergonomicScore")
                val colSnapshotReason = getColumnIndexOrThrow(stmt, "snapshotReason")
                val colIsAutoSave = getColumnIndexOrThrow(stmt, "isAutoSave")
                val colTimestampMillis = getColumnIndexOrThrow(stmt, "timestampMillis")
                val result = mutableListOf<LocalProjectSnapshotEntity>()
                while (stmt.step()) {
                    result.add(
                        LocalProjectSnapshotEntity(
                            id = stmt.getLong(colId),
                            projectId = stmt.getText(colProjectId),
                            projectName = stmt.getText(colProjectName),
                            shapeCode = stmt.getText(colShapeCode),
                            styleId = stmt.getText(colStyleId),
                            roomWidthMm = stmt.getLong(colRoomWidthMm).toInt(),
                            roomDepthMm = stmt.getLong(colRoomDepthMm).toInt(),
                            roomHeightMm = stmt.getLong(colRoomHeightMm).toInt(),
                            cabinetFinish = stmt.getText(colCabinetFinish),
                            countertopMaterial = stmt.getText(colCountertopMaterial),
                            handleStyle = stmt.getText(colHandleStyle),
                            wallColorHex = stmt.getText(colWallColorHex),
                            floorMaterial = stmt.getText(colFloorMaterial),
                            itemsData = stmt.getText(colItemsData),
                            totalPriceSar = stmt.getLong(colTotalPriceSar).toInt(),
                            ergonomicScore = stmt.getLong(colErgonomicScore).toInt(),
                            snapshotReason = stmt.getText(colSnapshotReason),
                            isAutoSave = stmt.getLong(colIsAutoSave).toInt() != 0,
                            timestampMillis = stmt.getLong(colTimestampMillis)
                        )
                    )
                }
                result
            } finally {
                stmt.close()
            }
        }
    }

    override suspend fun getLatestSnapshot(): LocalProjectSnapshotEntity? {
        val sql = "SELECT * FROM project_snapshots ORDER BY timestampMillis DESC LIMIT 1"
        return performSuspending(__db, true, false) { connection ->
            val stmt = connection.prepare(sql)
            try {
                val colId = getColumnIndexOrThrow(stmt, "id")
                val colProjectId = getColumnIndexOrThrow(stmt, "projectId")
                val colProjectName = getColumnIndexOrThrow(stmt, "projectName")
                val colShapeCode = getColumnIndexOrThrow(stmt, "shapeCode")
                val colStyleId = getColumnIndexOrThrow(stmt, "styleId")
                val colRoomWidthMm = getColumnIndexOrThrow(stmt, "roomWidthMm")
                val colRoomDepthMm = getColumnIndexOrThrow(stmt, "roomDepthMm")
                val colRoomHeightMm = getColumnIndexOrThrow(stmt, "roomHeightMm")
                val colCabinetFinish = getColumnIndexOrThrow(stmt, "cabinetFinish")
                val colCountertopMaterial = getColumnIndexOrThrow(stmt, "countertopMaterial")
                val colHandleStyle = getColumnIndexOrThrow(stmt, "handleStyle")
                val colWallColorHex = getColumnIndexOrThrow(stmt, "wallColorHex")
                val colFloorMaterial = getColumnIndexOrThrow(stmt, "floorMaterial")
                val colItemsData = getColumnIndexOrThrow(stmt, "itemsData")
                val colTotalPriceSar = getColumnIndexOrThrow(stmt, "totalPriceSar")
                val colErgonomicScore = getColumnIndexOrThrow(stmt, "ergonomicScore")
                val colSnapshotReason = getColumnIndexOrThrow(stmt, "snapshotReason")
                val colIsAutoSave = getColumnIndexOrThrow(stmt, "isAutoSave")
                val colTimestampMillis = getColumnIndexOrThrow(stmt, "timestampMillis")
                if (stmt.step()) {
                    LocalProjectSnapshotEntity(
                        id = stmt.getLong(colId),
                        projectId = stmt.getText(colProjectId),
                        projectName = stmt.getText(colProjectName),
                        shapeCode = stmt.getText(colShapeCode),
                        styleId = stmt.getText(colStyleId),
                        roomWidthMm = stmt.getLong(colRoomWidthMm).toInt(),
                        roomDepthMm = stmt.getLong(colRoomDepthMm).toInt(),
                        roomHeightMm = stmt.getLong(colRoomHeightMm).toInt(),
                        cabinetFinish = stmt.getText(colCabinetFinish),
                        countertopMaterial = stmt.getText(colCountertopMaterial),
                        handleStyle = stmt.getText(colHandleStyle),
                        wallColorHex = stmt.getText(colWallColorHex),
                        floorMaterial = stmt.getText(colFloorMaterial),
                        itemsData = stmt.getText(colItemsData),
                        totalPriceSar = stmt.getLong(colTotalPriceSar).toInt(),
                        ergonomicScore = stmt.getLong(colErgonomicScore).toInt(),
                        snapshotReason = stmt.getText(colSnapshotReason),
                        isAutoSave = stmt.getLong(colIsAutoSave).toInt() != 0,
                        timestampMillis = stmt.getLong(colTimestampMillis)
                    )
                } else {
                    null
                }
            } finally {
                stmt.close()
            }
        }
    }

    override suspend fun deleteSnapshotById(id: Long) {
        val sql = "DELETE FROM project_snapshots WHERE id = ?"
        return performSuspending(__db, false, true) { connection ->
            val stmt = connection.prepare(sql)
            try {
                stmt.bindLong(1, id)
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    override suspend fun pruneOldSnapshots(keepCount: Int) {
        val sql =
            "DELETE FROM project_snapshots WHERE id NOT IN (SELECT id FROM project_snapshots ORDER BY timestampMillis DESC LIMIT ?)"
        return performSuspending(__db, false, true) { connection ->
            val stmt = connection.prepare(sql)
            try {
                stmt.bindLong(1, keepCount.toLong())
                stmt.step()
            } finally {
                stmt.close()
            }
        }
    }

    companion object {
        fun getRequiredConverters(): List<KClass<*>> = emptyList()
    }
}

@Suppress("UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL")
class LocalKitchenDatabase_Impl : LocalKitchenDatabase() {
    private val _projectSnapshotDao: Lazy<ProjectSnapshotDao> = lazy {
        ProjectSnapshotDao_Impl(this)
    }

    override fun createOpenDelegate(): RoomOpenDelegate {
        return object : RoomOpenDelegate(
            1,
            "d88e6f68576bd41d72a4551c579d0718",
            "77e06bcac95867336e7402938ec95f72"
        ) {
            override fun createAllTables(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `project_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` TEXT NOT NULL, `projectName` TEXT NOT NULL, `shapeCode` TEXT NOT NULL, `styleId` TEXT NOT NULL, `roomWidthMm` INTEGER NOT NULL, `roomDepthMm` INTEGER NOT NULL, `roomHeightMm` INTEGER NOT NULL, `cabinetFinish` TEXT NOT NULL, `countertopMaterial` TEXT NOT NULL, `handleStyle` TEXT NOT NULL, `wallColorHex` TEXT NOT NULL, `floorMaterial` TEXT NOT NULL, `itemsData` TEXT NOT NULL, `totalPriceSar` INTEGER NOT NULL, `ergonomicScore` INTEGER NOT NULL, `snapshotReason` TEXT NOT NULL, `isAutoSave` INTEGER NOT NULL, `timestampMillis` INTEGER NOT NULL)"
                )
                connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd88e6f68576bd41d72a4551c579d0718')")
            }

            override fun dropAllTables(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS `project_snapshots`")
            }

            override fun onCreate(connection: SQLiteConnection) {}

            override fun onOpen(connection: SQLiteConnection) {
                internalInitInvalidationTracker(connection)
            }

            override fun onPreMigrate(connection: SQLiteConnection) {
                dropFtsSyncTriggers(connection)
            }

            override fun onPostMigrate(connection: SQLiteConnection) {}

            override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
                val columnsProjectSnapshots = mutableMapOf<String, TableInfo.Column>()
                columnsProjectSnapshots["id"] = TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["projectId"] = TableInfo.Column("projectId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["projectName"] = TableInfo.Column("projectName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["shapeCode"] = TableInfo.Column("shapeCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["styleId"] = TableInfo.Column("styleId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["roomWidthMm"] = TableInfo.Column("roomWidthMm", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["roomDepthMm"] = TableInfo.Column("roomDepthMm", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["roomHeightMm"] = TableInfo.Column("roomHeightMm", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["cabinetFinish"] = TableInfo.Column("cabinetFinish", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["countertopMaterial"] = TableInfo.Column("countertopMaterial", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["handleStyle"] = TableInfo.Column("handleStyle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["wallColorHex"] = TableInfo.Column("wallColorHex", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["floorMaterial"] = TableInfo.Column("floorMaterial", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["itemsData"] = TableInfo.Column("itemsData", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["totalPriceSar"] = TableInfo.Column("totalPriceSar", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["ergonomicScore"] = TableInfo.Column("ergonomicScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["snapshotReason"] = TableInfo.Column("snapshotReason", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["isAutoSave"] = TableInfo.Column("isAutoSave", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                columnsProjectSnapshots["timestampMillis"] = TableInfo.Column("timestampMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY)
                val infoProjectSnapshots = TableInfo(
                    "project_snapshots",
                    columnsProjectSnapshots,
                    mutableSetOf(),
                    mutableSetOf()
                )
                val existingProjectSnapshots = read(connection, "project_snapshots")
                if (infoProjectSnapshots != existingProjectSnapshots) {
                    return RoomOpenDelegate.ValidationResult(
                        false,
                        "project_snapshots(com.example.data.local.LocalProjectSnapshotEntity).\n Expected:\n$infoProjectSnapshots\n Found:\n$existingProjectSnapshots"
                    )
                }
                return RoomOpenDelegate.ValidationResult(true, null)
            }
        }
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        return InvalidationTracker(this, mutableMapOf(), mutableMapOf(), "project_snapshots")
    }

    override fun clearAllTables() {
        super.performClear(false, "project_snapshots")
    }

    override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
        return mapOf(ProjectSnapshotDao::class to ProjectSnapshotDao_Impl.getRequiredConverters())
    }

    override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> = emptySet()

    override fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> = emptyList()

    override fun projectSnapshotDao(): ProjectSnapshotDao = _projectSnapshotDao.value
}
