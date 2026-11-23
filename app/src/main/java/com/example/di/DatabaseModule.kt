package com.example.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ArmorDao
import com.example.data.BattleLogDao
import com.example.data.EngineConfigDao
import com.example.data.EquipmentDao
import com.example.data.HeroDao
import com.example.data.ImportedAssetDao
import com.example.data.MedalDao
import com.example.data.RealmMetadataDao
import com.example.data.RealmStageDao
import com.example.data.RpgDao
import com.example.data.SkillDao
import com.example.data.WeaponDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module providing the singleton [AppDatabase] instance
 * and all RPG engine Data Access Objects (DAOs).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideAppDatabase(
    @ApplicationContext context: Context
  ): AppDatabase = AppDatabase.getInstance(context)

  @Provides
  @Singleton
  fun provideHeroDao(database: AppDatabase): HeroDao = database.heroDao()

  @Provides
  @Singleton
  fun provideEquipmentDao(database: AppDatabase): EquipmentDao = database.equipmentDao()

  @Provides
  @Singleton
  fun provideWeaponDao(database: AppDatabase): WeaponDao = database.weaponDao()

  @Provides
  @Singleton
  fun provideArmorDao(database: AppDatabase): ArmorDao = database.armorDao()

  @Provides
  @Singleton
  fun provideRealmMetadataDao(database: AppDatabase): RealmMetadataDao = database.realmMetadataDao()

  @Provides
  @Singleton
  fun provideMedalDao(database: AppDatabase): MedalDao = database.medalDao()

  @Provides
  @Singleton
  fun provideRealmStageDao(database: AppDatabase): RealmStageDao = database.realmStageDao()

  @Provides
  @Singleton
  fun provideEngineConfigDao(database: AppDatabase): EngineConfigDao = database.engineConfigDao()

  @Provides
  @Singleton
  fun provideBattleLogDao(database: AppDatabase): BattleLogDao = database.battleLogDao()

  @Provides
  @Singleton
  fun provideImportedAssetDao(database: AppDatabase): ImportedAssetDao = database.importedAssetDao()

  @Provides
  @Singleton
  fun provideSkillDao(database: AppDatabase): SkillDao = database.skillDao()

  @Provides
  @Singleton
  fun provideRpgDao(database: AppDatabase): RpgDao = database.rpgDao()
}
