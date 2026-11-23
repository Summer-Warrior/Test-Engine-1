package com.example.di

import com.example.data.ArmorRepository
import com.example.data.ArmoryRepository
import com.example.data.RoomArmorRepository
import com.example.data.RoomArmoryRepository
import com.example.data.RoomSkillRepository
import com.example.data.RoomWeaponRepository
import com.example.data.SkillRepository
import com.example.data.WeaponRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module binding Room-backed repository implementations to their domain
 * interfaces ([WeaponRepository], [ArmorRepository], [SkillRepository], [ArmoryRepository]).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

  @Binds
  @Singleton
  abstract fun bindWeaponRepository(impl: RoomWeaponRepository): WeaponRepository

  @Binds
  @Singleton
  abstract fun bindArmorRepository(impl: RoomArmorRepository): ArmorRepository

  @Binds
  @Singleton
  abstract fun bindSkillRepository(impl: RoomSkillRepository): SkillRepository

  @Binds
  @Singleton
  abstract fun bindArmoryRepository(impl: RoomArmoryRepository): ArmoryRepository
}
