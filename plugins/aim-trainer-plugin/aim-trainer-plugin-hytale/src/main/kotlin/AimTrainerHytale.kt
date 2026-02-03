package com.hytale2mc.plugin.aimtrainer.hytale

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.spatial.SpatialResource
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.entity.EntityModule
import com.hypixel.hytale.server.core.universe.world.ParticleUtil
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.PlatformEntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.hytale.HytalePlatform
import com.hytale2mc.ecs.platform.hytale.toVector3d
import com.hytale2mc.plugin.aimtrainer.*
import korlibs.math.geom.Vector3I

data object AimTrainerHytale : AimTrainerPlatformHandler<PlatformEntityId, ItemStack, HytalePlatform> {

    override fun init(platform: HytalePlatform) {}

    override fun render(
        platform: HytalePlatform,
        renderable: AimTrainerRenderable
    ) {
        when (renderable) {
            is AimTrainerRenderable.TargetKilled -> {
                val componentAccessor = platform.world.entityStore.store
                val playerSpatialResource = componentAccessor.getResource<SpatialResource<Ref<EntityStore?>?, EntityStore?>?>(
                    EntityModule.get().playerSpatialResourceType
                )
                val playerRefs = SpatialResource.getThreadLocalReferenceList<EntityStore?>()

                playerSpatialResource.getSpatialStructure().collect(renderable.at.toVector3d(), 75.0, playerRefs)
                ParticleUtil.spawnParticleEffect(
                    "Explosion_Small",
                    renderable.at.x.toDouble(),
                    renderable.at.y.toDouble(),
                    renderable.at.z.toDouble(),
                    0f, 0f, 0f,
                    1f,
                    null,
                    null,
                    playerRefs,
                    componentAccessor
                )
            }
        }
    }

    val zombieModel by lazy { Model.createUnitScaleModel(
        ModelAsset.getAssetMap().getAsset("Zombie")!!
    ) }

    override fun spawn(
        platform: HytalePlatform,
        entityId: EntityId,
        entityType: AimTrainerEntityType
    ): Pair<PlatformEntityId, EntityProperties?> {
        return when (entityType) {
            AimTrainerEntityType.Target -> {
                platform.newHytaleEntity(entityId, model = zombieModel)
            }
        } to null
    }

    override fun setBlock(
        platform: HytalePlatform,
        position: Vector3I,
        blockType: AimTrainerBlockType
    ) {
        val id = when (blockType) {
            AimTrainerBlockType.Floor -> "Rock_Chalk"
            AimTrainerBlockType.Wall -> "Soil_Clay_Smooth_Black"
        }

        platform.world.setBlock(position.x, position.y, position.z, id)
    }

    override fun createItem(
        platform: HytalePlatform,
        itemType: AimTrainerItemType
    ): ItemStack {
        return when (itemType) {
            AimTrainerItemType.Gun -> ItemStack("AimTrainerGun")
        }
    }

    override fun getSoundData(soundType: AimTrainerSoundType): List<SoundData> {
        return when (soundType) {
            AimTrainerSoundType.Hit -> listOf(SoundData("SFX_Hit_Beep"))
        }
    }

    override fun onRemove(platformEntity: PlatformEntityId) {

    }

}