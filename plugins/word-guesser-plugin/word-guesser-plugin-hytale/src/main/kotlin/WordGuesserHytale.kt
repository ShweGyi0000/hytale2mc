package com.hytale2mc.plugin.wordguesser

import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.PlatformEntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.hytale.HytalePlatform
import korlibs.math.geom.Vector3I

object WordGuesserHytale : WordGuesserPlatformHandler<PlatformEntityId, ItemStack, HytalePlatform> {

    override fun init(platform: HytalePlatform) {}

    override fun render(
        platform: HytalePlatform,
        renderable: WordGuesserRenderable
    ) {
        throw IllegalStateException()
    }

    override fun spawn(
        platform: HytalePlatform,
        entityId: EntityId,
        entityType: WordGuesserEntityType
    ): Pair<PlatformEntityId, EntityProperties?> {
        throw IllegalStateException()
    }

    override fun setBlock(
        platform: HytalePlatform,
        position: Vector3I,
        blockType: WordGuesserBlockType
    ) {
        when (blockType) {
            WordGuesserBlockType.Floor -> platform.world.setBlock(position.x, position.y, position.z, "Rock_Chalk")
        }
    }

    override fun createItem(
        platform: HytalePlatform,
        itemType: WordGuesserItemType
    ): ItemStack {
        throw IllegalStateException()
    }

    override fun getSoundData(soundType: WordGuesserSoundType): List<SoundData> {
        throw IllegalStateException()
    }

    override fun onRemove(platformEntity: PlatformEntityId) {}
}