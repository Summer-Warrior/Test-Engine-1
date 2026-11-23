package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.model.ArmorEntity
import com.example.model.ArmorSkillCrossRef
import com.example.model.ArmorWithSkills
import com.example.model.ElementType
import com.example.model.RpgSystemOrigin
import com.example.model.WeaponArmorClass
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [ArmorEntity] standard CRUD operations,
 * multi-RPG filtering queries, and Materia/skill socket cross-reference management.
 */
@Dao
interface ArmorDao {

  // ---------------------------------------------------------------------------
  // READ (Reactive Flows & One-Shot Snapshots)
  // ---------------------------------------------------------------------------

  @Query("SELECT * FROM armors ORDER BY rarityStars DESC, reforgeTier DESC, upgradeLevel DESC, id ASC")
  fun observeArmors(): Flow<List<ArmorEntity>>

  @Transaction
  @Query("SELECT * FROM armors ORDER BY rarityStars DESC, reforgeTier DESC, id ASC")
  fun observeArmorsWithSkills(): Flow<List<ArmorWithSkills>>

  @Query("SELECT * FROM armors WHERE id = :armorId LIMIT 1")
  fun observeArmorById(armorId: Int): Flow<ArmorEntity?>

  @Query("SELECT * FROM armors WHERE rpgSystem = :rpgSystem ORDER BY rarityStars DESC, defenseRating DESC")
  fun observeArmorsBySystem(rpgSystem: RpgSystemOrigin): Flow<List<ArmorEntity>>

  @Query("SELECT * FROM armors WHERE armorClass = :armorClass ORDER BY rarityStars DESC, defenseRating DESC")
  fun observeArmorsByClass(armorClass: WeaponArmorClass): Flow<List<ArmorEntity>>

  @Query("SELECT * FROM armors WHERE elementResistElement = :element ORDER BY elementResistPercent DESC, defenseRating DESC")
  fun observeArmorsByElementResist(element: ElementType): Flow<List<ArmorEntity>>

  @Query("SELECT * FROM armors WHERE LOWER(setSeriesName) = LOWER(:setSeriesName) ORDER BY rarityStars DESC")
  fun observeArmorsBySetSeries(setSeriesName: String): Flow<List<ArmorEntity>>

  @Query("SELECT * FROM armors WHERE equippedByHeroId = :heroId")
  fun observeArmorsForHero(heroId: Int): Flow<List<ArmorEntity>>

  @Query("SELECT * FROM armors WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' ORDER BY rarityStars DESC, defenseRating DESC")
  fun searchArmorsByName(query: String): Flow<List<ArmorEntity>>

  @Query("SELECT * FROM armors ORDER BY rarityStars DESC, reforgeTier DESC, upgradeLevel DESC, id ASC")
  suspend fun getAllArmors(): List<ArmorEntity>

  @Query("SELECT * FROM armors WHERE id = :armorId LIMIT 1")
  suspend fun getArmorById(armorId: Int): ArmorEntity?

  @Transaction
  @Query("SELECT * FROM armors WHERE id = :armorId LIMIT 1")
  suspend fun getArmorWithSkillsById(armorId: Int): ArmorWithSkills?

  @Query("SELECT COUNT(*) FROM armors")
  suspend fun getArmorCount(): Int

  // ---------------------------------------------------------------------------
  // CREATE / UPSERT
  // ---------------------------------------------------------------------------

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertArmor(armor: ArmorEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertArmors(armors: List<ArmorEntity>): List<Long>

  @Upsert
  suspend fun upsertArmor(armor: ArmorEntity): Long

  @Upsert
  suspend fun upsertArmors(armors: List<ArmorEntity>): List<Long>

  // ---------------------------------------------------------------------------
  // UPDATE
  // ---------------------------------------------------------------------------

  @Update
  suspend fun updateArmor(armor: ArmorEntity): Int

  @Update
  suspend fun updateArmors(armors: List<ArmorEntity>): Int

  // ---------------------------------------------------------------------------
  // DELETE
  // ---------------------------------------------------------------------------

  @Delete
  suspend fun deleteArmor(armor: ArmorEntity): Int

  @Query("DELETE FROM armors WHERE id = :armorId")
  suspend fun deleteArmorById(armorId: Int): Int

  @Query("DELETE FROM armors")
  suspend fun deleteAllArmors(): Int

  // ---------------------------------------------------------------------------
  // MATERIA / SKILL SOCKET JUNCTION OPERATIONS
  // ---------------------------------------------------------------------------

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertArmorSkillSocket(ref: ArmorSkillCrossRef): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertArmorSkillSockets(refs: List<ArmorSkillCrossRef>): List<Long>

  @Query("DELETE FROM armor_skill_sockets WHERE armorId = :armorId AND skillId = :skillId")
  suspend fun deleteArmorSkillSocket(armorId: Int, skillId: Int): Int

  @Query("DELETE FROM armor_skill_sockets WHERE armorId = :armorId")
  suspend fun clearArmorSkillSockets(armorId: Int): Int
}
