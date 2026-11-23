package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.model.ElementType
import com.example.model.RpgSystemOrigin
import com.example.model.SkillCategory
import com.example.model.SkillCombatEffect
import com.example.model.SkillEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [SkillEntity] standard CRUD operations,
 * category/system/element/effect filtering, and search queries.
 */
@Dao
interface SkillDao {

  // ---------------------------------------------------------------------------
  // READ (Reactive Flows & One-Shot Snapshots)
  // ---------------------------------------------------------------------------

  @Query("SELECT * FROM skills ORDER BY category ASC, rankLevel DESC, id ASC")
  fun observeSkills(): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills WHERE id = :skillId LIMIT 1")
  fun observeSkillById(skillId: Int): Flow<SkillEntity?>

  @Query("SELECT * FROM skills WHERE category = :category ORDER BY rankLevel DESC, id ASC")
  fun observeSkillsByCategory(category: String): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills WHERE rpgSystem = :rpgSystem ORDER BY rankLevel DESC, id ASC")
  fun observeSkillsBySystem(rpgSystem: String): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills WHERE element = :element ORDER BY rankLevel DESC, id ASC")
  fun observeSkillsByElement(element: String): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills WHERE category = :category ORDER BY rankLevel DESC, id ASC")
  fun observeSkillsByCategoryEnum(category: SkillCategory): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills WHERE rpgSystem = :rpgSystem ORDER BY rankLevel DESC, id ASC")
  fun observeSkillsBySystemEnum(rpgSystem: RpgSystemOrigin): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills WHERE element = :element ORDER BY rankLevel DESC, id ASC")
  fun observeSkillsByElementEnum(element: ElementType): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills WHERE effectType = :effectType ORDER BY rankLevel DESC, id ASC")
  fun observeSkillsByEffect(effectType: SkillCombatEffect): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' ORDER BY rankLevel DESC, id ASC")
  fun searchSkillsByName(query: String): Flow<List<SkillEntity>>

  @Query("SELECT * FROM skills ORDER BY category ASC, rankLevel DESC, id ASC")
  suspend fun getAllSkills(): List<SkillEntity>

  @Query("SELECT * FROM skills WHERE id = :skillId LIMIT 1")
  suspend fun getSkillById(skillId: Int): SkillEntity?

  @Query("SELECT COUNT(*) FROM skills")
  suspend fun getSkillCount(): Int

  // ---------------------------------------------------------------------------
  // CREATE / UPSERT
  // ---------------------------------------------------------------------------

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSkill(skill: SkillEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSkills(skills: List<SkillEntity>): List<Long>

  @Upsert
  suspend fun upsertSkill(skill: SkillEntity): Long

  @Upsert
  suspend fun upsertSkills(skills: List<SkillEntity>): List<Long>

  // ---------------------------------------------------------------------------
  // UPDATE
  // ---------------------------------------------------------------------------

  @Update
  suspend fun updateSkill(skill: SkillEntity): Int

  @Update
  suspend fun updateSkills(skills: List<SkillEntity>): Int

  // ---------------------------------------------------------------------------
  // DELETE
  // ---------------------------------------------------------------------------

  @Delete
  suspend fun deleteSkill(skill: SkillEntity): Int

  @Query("DELETE FROM skills WHERE id = :skillId")
  suspend fun deleteSkillById(skillId: Int): Int

  @Query("DELETE FROM skills")
  suspend fun deleteAllSkills(): Int
}
