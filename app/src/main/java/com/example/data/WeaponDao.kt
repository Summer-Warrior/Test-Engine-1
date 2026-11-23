package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.model.ElementType
import com.example.model.RpgSystemOrigin
import com.example.model.WeaponArmorClass
import com.example.model.WeaponEntity
import com.example.model.WeaponSkillCrossRef
import com.example.model.WeaponWithSkills
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [WeaponEntity] standard CRUD operations,
 * multi-RPG filtering queries, and Materia/skill socket cross-reference management.
 */
@Dao
interface WeaponDao {

  // ---------------------------------------------------------------------------
  // READ (Reactive Flows & One-Shot Snapshots)
  // ---------------------------------------------------------------------------

  @Query("SELECT * FROM weapons ORDER BY rarityStars DESC, reforgeTier DESC, upgradeLevel DESC, id ASC")
  fun observeWeapons(): Flow<List<WeaponEntity>>

  @Transaction
  @Query("SELECT * FROM weapons ORDER BY rarityStars DESC, reforgeTier DESC, id ASC")
  fun observeWeaponsWithSkills(): Flow<List<WeaponWithSkills>>

  @Query("SELECT * FROM weapons WHERE id = :weaponId LIMIT 1")
  fun observeWeaponById(weaponId: Int): Flow<WeaponEntity?>

  @Query("SELECT * FROM weapons WHERE rpgSystem = :rpgSystem ORDER BY rarityStars DESC, attackPower DESC")
  fun observeWeaponsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<WeaponEntity>>

  @Query("SELECT * FROM weapons WHERE weaponClass = :weaponClass ORDER BY rarityStars DESC, attackPower DESC")
  fun observeWeaponsByClass(weaponClass: WeaponArmorClass): Flow<List<WeaponEntity>>

  @Query("SELECT * FROM weapons WHERE elementBoost = :elementBoost ORDER BY rarityStars DESC, attackPower DESC")
  fun observeWeaponsByElement(elementBoost: ElementType): Flow<List<WeaponEntity>>

  @Query("SELECT * FROM weapons WHERE LOWER(setSeriesName) = LOWER(:setSeriesName) ORDER BY rarityStars DESC")
  fun observeWeaponsBySetSeries(setSeriesName: String): Flow<List<WeaponEntity>>

  @Query("SELECT * FROM weapons WHERE equippedByHeroId = :heroId")
  fun observeWeaponsForHero(heroId: Int): Flow<List<WeaponEntity>>

  @Query("SELECT * FROM weapons WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' ORDER BY rarityStars DESC, attackPower DESC")
  fun searchWeaponsByName(query: String): Flow<List<WeaponEntity>>

  @Query("SELECT * FROM weapons ORDER BY rarityStars DESC, reforgeTier DESC, upgradeLevel DESC, id ASC")
  suspend fun getAllWeapons(): List<WeaponEntity>

  @Query("SELECT * FROM weapons WHERE id = :weaponId LIMIT 1")
  suspend fun getWeaponById(weaponId: Int): WeaponEntity?

  @Transaction
  @Query("SELECT * FROM weapons WHERE id = :weaponId LIMIT 1")
  suspend fun getWeaponWithSkillsById(weaponId: Int): WeaponWithSkills?

  @Query("SELECT COUNT(*) FROM weapons")
  suspend fun getWeaponCount(): Int

  // ---------------------------------------------------------------------------
  // CREATE / UPSERT
  // ---------------------------------------------------------------------------

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWeapon(weapon: WeaponEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWeapons(weapons: List<WeaponEntity>): List<Long>

  @Upsert
  suspend fun upsertWeapon(weapon: WeaponEntity): Long

  @Upsert
  suspend fun upsertWeapons(weapons: List<WeaponEntity>): List<Long>

  // ---------------------------------------------------------------------------
  // UPDATE
  // ---------------------------------------------------------------------------

  @Update
  suspend fun updateWeapon(weapon: WeaponEntity): Int

  @Update
  suspend fun updateWeapons(weapons: List<WeaponEntity>): Int

  // ---------------------------------------------------------------------------
  // DELETE
  // ---------------------------------------------------------------------------

  @Delete
  suspend fun deleteWeapon(weapon: WeaponEntity): Int

  @Query("DELETE FROM weapons WHERE id = :weaponId")
  suspend fun deleteWeaponById(weaponId: Int): Int

  @Query("DELETE FROM weapons")
  suspend fun deleteAllWeapons(): Int

  // ---------------------------------------------------------------------------
  // MATERIA / SKILL SOCKET JUNCTION OPERATIONS
  // ---------------------------------------------------------------------------

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWeaponSkillSocket(ref: WeaponSkillCrossRef): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWeaponSkillSockets(refs: List<WeaponSkillCrossRef>): List<Long>

  @Query("DELETE FROM weapon_skill_sockets WHERE weaponId = :weaponId AND skillId = :skillId")
  suspend fun deleteWeaponSkillSocket(weaponId: Int, skillId: Int): Int

  @Query("DELETE FROM weapon_skill_sockets WHERE weaponId = :weaponId")
  suspend fun clearWeaponSkillSockets(weaponId: Int): Int
}
