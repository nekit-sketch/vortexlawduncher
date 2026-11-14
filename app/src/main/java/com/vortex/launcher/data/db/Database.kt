package com.vortex.launcher.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int = 0,
    /** null = по умолчанию (первые 5 приложений), иначе csv пакетов */
    val pinned: String? = null
)

@Entity(tableName = "app_stats")
data class AppStat(
    @PrimaryKey val pkg: String,
    val launches: Int = 0,
    val lastLaunch: Long = 0L
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)
}

@Dao
interface StatDao {
    @Query("SELECT * FROM app_stats")
    fun observeAll(): Flow<List<AppStat>>

    @Query("SELECT * FROM app_stats WHERE pkg = :pkg")
    suspend fun get(pkg: String): AppStat?

    @Upsert
    suspend fun upsert(stat: AppStat)
}

@Database(entities = [ProfileEntity::class, AppStat::class], version = 1, exportSchema = false)
abstract class VortexDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun statDao(): StatDao
}
