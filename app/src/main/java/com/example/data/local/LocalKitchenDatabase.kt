package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
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
