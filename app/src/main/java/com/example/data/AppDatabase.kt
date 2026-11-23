package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.ArmorEntity
import com.example.model.ArmorSkillCrossRef
import com.example.model.BattleLogEntity
import com.example.model.EngineConfigEntity
import com.example.model.EquipmentEntity
import com.example.model.HeroEntity
import com.example.model.ImportedAssetEntity
import com.example.model.MedalEntity
import com.example.model.RealmMetadataEntity
import com.example.model.RealmStageEntity
import com.example.model.SkillEntity
import com.example.model.WeaponEntity
import com.example.model.WeaponSkillCrossRef

/**
 * Primary Room persistence database for Chronicle Nexus.
 *
 * Registers normalized tables for [WeaponEntity], [ArmorEntity], [SkillEntity],
 * Materia/skill socket junction tables ([WeaponSkillCrossRef], [ArmorSkillCrossRef]),
 * and core RPG engine entities. Uses [RpgTypeConverters] to serialize custom enum,
 * list, and map data types cleanly into SQLite.
 */
@Database(
  entities = [
    HeroEntity::class,
    EquipmentEntity::class,
    WeaponEntity::class,
    ArmorEntity::class,
    WeaponSkillCrossRef::class,
    ArmorSkillCrossRef::class,
    RealmMetadataEntity::class,
    MedalEntity::class,
    RealmStageEntity::class,
    EngineConfigEntity::class,
    BattleLogEntity::class,
    ImportedAssetEntity::class,
    SkillEntity::class
  ],
  version = AppDatabase.DATABASE_VERSION,
  exportSchema = false
)
@TypeConverters(RpgTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun heroDao(): HeroDao
  abstract fun equipmentDao(): EquipmentDao
  abstract fun weaponDao(): WeaponDao
  abstract fun armorDao(): ArmorDao
  abstract fun realmMetadataDao(): RealmMetadataDao
  abstract fun medalDao(): MedalDao
  abstract fun realmStageDao(): RealmStageDao
  abstract fun engineConfigDao(): EngineConfigDao
  abstract fun battleLogDao(): BattleLogDao
  abstract fun importedAssetDao(): ImportedAssetDao
  abstract fun skillDao(): SkillDao
  abstract fun rpgDao(): RpgDao

  fun characterDao(): CharacterDao = heroDao()
  fun realmDao(): RealmDao = realmMetadataDao()

  companion object {
    const val DATABASE_NAME = "chronicle_nexus_rpg.db"
    const val DATABASE_VERSION = 9

    private val foreignKeyCallback = object : Callback() {
      override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys = ON;")
      }
    }

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          DATABASE_NAME
        )
          .addCallback(foreignKeyCallback)
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }

    fun createInMemory(context: Context): AppDatabase {
      return Room.inMemoryDatabaseBuilder(
        context.applicationContext,
        AppDatabase::class.java
      )
        .addCallback(foreignKeyCallback)
        .allowMainThreadQueries()
        .build()
    }
  }
}
