package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.model.BattleLogEntity
import com.example.model.CharacterLoadoutRelation
import com.example.model.EngineConfigEntity
import com.example.model.EquipmentEntity
import com.example.model.HeroEntity
import com.example.model.HeroGearRelation
import com.example.model.ImportedAssetEntity
import com.example.model.MedalEntity
import com.example.model.RealmMetadataEntity
import com.example.model.RealmStageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Character ([HeroEntity] / [com.example.model.CharacterEntity]) CRUD
 * and relational loadout queries.
 */
@Dao
interface HeroDao {
  // READ
  @Query("SELECT * FROM heroes ORDER BY inParty DESC, level DESC")
  fun observeHeroes(): Flow<List<HeroEntity>>

  @Transaction
  @Query("SELECT * FROM heroes ORDER BY inParty DESC, level DESC")
  fun observeCharacterLoadouts(): Flow<List<CharacterLoadoutRelation>>

  @Query("SELECT * FROM heroes WHERE inParty = 1 ORDER BY id ASC LIMIT 4")
  fun observeActiveParty(): Flow<List<HeroEntity>>

  @Query("SELECT * FROM heroes WHERE LOWER(realm) = LOWER(:realmName) ORDER BY level DESC")
  fun observeHeroesByRealm(realmName: String): Flow<List<HeroEntity>>

  @Query("SELECT * FROM heroes WHERE id = :heroId LIMIT 1")
  suspend fun getHeroById(heroId: Int): HeroEntity?

  @Transaction
  @Query("SELECT * FROM heroes WHERE id = :heroId LIMIT 1")
  suspend fun getCharacterLoadoutById(heroId: Int): CharacterLoadoutRelation?

  @Transaction
  @Query("SELECT * FROM heroes WHERE id = :heroId LIMIT 1")
  suspend fun getHeroGearRelationById(heroId: Int): HeroGearRelation?

  // CREATE / UPSERT
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHeroes(heroes: List<HeroEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHero(hero: HeroEntity): Long

  @Upsert
  suspend fun upsertHero(hero: HeroEntity): Long

  // UPDATE
  @Update
  suspend fun updateHero(hero: HeroEntity): Int

  // DELETE
  @Delete
  suspend fun deleteHero(hero: HeroEntity): Int

  @Query("DELETE FROM heroes WHERE id = :heroId")
  suspend fun deleteHeroById(heroId: Int): Int
}

typealias CharacterDao = HeroDao

/**
 * Data Access Object for Equipment ([EquipmentEntity]) CRUD operations, slot filtering,
 * and Realm Synergy lookups.
 */
@Dao
interface EquipmentDao {
  // READ
  @Query("SELECT * FROM equipment ORDER BY rarityStars DESC, upgradeLevel DESC")
  fun observeEquipment(): Flow<List<EquipmentEntity>>

  @Query("SELECT * FROM equipment WHERE slotType = :slotType ORDER BY rarityStars DESC, upgradeLevel DESC")
  fun observeEquipmentBySlot(slotType: String): Flow<List<EquipmentEntity>>

  @Query("SELECT * FROM equipment WHERE LOWER(originRealm) = LOWER(:originRealm) ORDER BY rarityStars DESC, upgradeLevel DESC")
  fun observeEquipmentByRealm(originRealm: String): Flow<List<EquipmentEntity>>

  @Query("SELECT * FROM equipment WHERE equippedByHeroId = :heroId")
  fun observeEquipmentForHero(heroId: Int): Flow<List<EquipmentEntity>>

  @Query("SELECT * FROM equipment WHERE id = :equipmentId LIMIT 1")
  suspend fun getEquipmentById(equipmentId: Int): EquipmentEntity?

  // CREATE / UPSERT
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEquipmentList(items: List<EquipmentEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEquipment(item: EquipmentEntity): Long

  @Upsert
  suspend fun upsertEquipment(item: EquipmentEntity): Long

  // UPDATE
  @Update
  suspend fun updateEquipment(item: EquipmentEntity): Int

  // DELETE
  @Delete
  suspend fun deleteEquipment(item: EquipmentEntity): Int

  @Query("DELETE FROM equipment WHERE id = :equipmentId")
  suspend fun deleteEquipmentById(equipmentId: Int): Int
}

/**
 * Data Access Object for Realm metadata ([RealmMetadataEntity]) CRUD operations
 * and progression tracking.
 */
@Dao
interface RealmMetadataDao {
  // READ
  @Query("SELECT * FROM realm_metadata ORDER BY id ASC")
  fun observeRealmMetadata(): Flow<List<RealmMetadataEntity>>

  @Query("SELECT * FROM realm_metadata WHERE isUnlocked = 1 ORDER BY id ASC")
  fun observeUnlockedRealms(): Flow<List<RealmMetadataEntity>>

  @Query("SELECT * FROM realm_metadata WHERE id = :realmId LIMIT 1")
  suspend fun getRealmById(realmId: Int): RealmMetadataEntity?

  @Query("SELECT * FROM realm_metadata WHERE LOWER(realmName) = LOWER(:realmName) LIMIT 1")
  suspend fun getRealmByName(realmName: String): RealmMetadataEntity?

  // CREATE / UPSERT
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRealmMetadataList(realms: List<RealmMetadataEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRealmMetadata(realm: RealmMetadataEntity): Long

  @Upsert
  suspend fun upsertRealmMetadata(realm: RealmMetadataEntity): Long

  // UPDATE
  @Update
  suspend fun updateRealmMetadata(realm: RealmMetadataEntity): Int

  // DELETE
  @Delete
  suspend fun deleteRealmMetadata(realm: RealmMetadataEntity): Int

  @Query("DELETE FROM realm_metadata WHERE id = :realmId")
  suspend fun deleteRealmById(realmId: Int): Int
}

typealias RealmDao = RealmMetadataDao

@Dao
interface MedalDao {
  @Query("SELECT * FROM medals ORDER BY rarityStars DESC, guiltBonusPercent DESC")
  fun observeMedals(): Flow<List<MedalEntity>>

  @Query("SELECT * FROM medals WHERE attribute = :attribute ORDER BY rarityStars DESC, guiltBonusPercent DESC")
  fun observeMedalsByAttribute(attribute: String): Flow<List<MedalEntity>>

  @Query("SELECT * FROM medals WHERE id = :medalId LIMIT 1")
  suspend fun getMedalById(medalId: Int): MedalEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMedals(medals: List<MedalEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMedal(medal: MedalEntity): Long

  @Update
  suspend fun updateMedal(medal: MedalEntity): Int

  @Delete
  suspend fun deleteMedal(medal: MedalEntity): Int

  @Query("DELETE FROM medals WHERE id = :medalId")
  suspend fun deleteMedalById(medalId: Int): Int
}

@Dao
interface RealmStageDao {
  @Query("SELECT * FROM realm_stages ORDER BY difficulty ASC")
  fun observeRealmStages(): Flow<List<RealmStageEntity>>

  @Query("SELECT * FROM realm_stages WHERE LOWER(realm) = LOWER(:realmName) ORDER BY difficulty ASC")
  fun observeStagesForRealm(realmName: String): Flow<List<RealmStageEntity>>

  @Query("SELECT * FROM realm_stages WHERE id = :stageId LIMIT 1")
  suspend fun getStageById(stageId: Int): RealmStageEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStages(stages: List<RealmStageEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStage(stage: RealmStageEntity): Long

  @Update
  suspend fun updateStage(stage: RealmStageEntity): Int

  @Delete
  suspend fun deleteStage(stage: RealmStageEntity): Int

  @Query("DELETE FROM realm_stages WHERE id = :stageId")
  suspend fun deleteStageById(stageId: Int): Int
}

@Dao
interface EngineConfigDao {
  @Query("SELECT * FROM engine_configs WHERE id = 1")
  fun observeEngineConfig(): Flow<EngineConfigEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveEngineConfig(config: EngineConfigEntity): Long
}

@Dao
interface BattleLogDao {
  @Query("SELECT * FROM battle_logs ORDER BY timestampMillis DESC LIMIT 30")
  fun observeBattleLogs(): Flow<List<BattleLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBattleLog(log: BattleLogEntity): Long

  @Query("DELETE FROM battle_logs")
  suspend fun clearBattleLogs(): Int
}

@Dao
interface ImportedAssetDao {
  @Query("SELECT * FROM imported_assets ORDER BY createdAtMillis DESC")
  fun observeImportedAssets(): Flow<List<ImportedAssetEntity>>

  @Query("SELECT * FROM imported_assets WHERE assetType = :assetType ORDER BY createdAtMillis DESC")
  fun observeAssetsByType(assetType: String): Flow<List<ImportedAssetEntity>>

  @Query("SELECT * FROM imported_assets WHERE id = :assetId LIMIT 1")
  suspend fun getAssetById(assetId: Int): ImportedAssetEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAsset(asset: ImportedAssetEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAssets(assets: List<ImportedAssetEntity>): List<Long>

  @Update
  suspend fun updateAsset(asset: ImportedAssetEntity): Int

  @Delete
  suspend fun deleteAsset(asset: ImportedAssetEntity): Int

  @Query("DELETE FROM imported_assets WHERE id = :assetId")
  suspend fun deleteAssetById(assetId: Int): Int
}

@Dao
interface RpgDao :
  HeroDao,
  EquipmentDao,
  WeaponDao,
  ArmorDao,
  RealmMetadataDao,
  MedalDao,
  RealmStageDao,
  EngineConfigDao,
  BattleLogDao,
  ImportedAssetDao,
  SkillDao
