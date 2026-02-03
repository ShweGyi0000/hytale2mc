package com.hytale2mc.plugin.aimtrainer.minestom

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.minestom.MinestomEntity
import com.hytale2mc.ecs.platform.minestom.MinestomItem
import com.hytale2mc.ecs.platform.minestom.MinestomPlatform
import com.hytale2mc.plugin.aimtrainer.*
import korlibs.math.geom.Vector3I
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.component.AttackRange
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent

data object AimTrainerMinestom : AimTrainerPlatformHandler<MinestomEntity, MinestomItem, MinestomPlatform> {

    override fun init(platform: MinestomPlatform) {}

    override fun render(
        platform: MinestomPlatform,
        renderable: AimTrainerRenderable
    ) {
        when (renderable) {
            is AimTrainerRenderable.TargetKilled -> {
                val at = renderable.at
                val packet = ParticlePacket(
                    Particle.SPIT,
                    true, true,
                    at.x.toDouble(), at.y.toDouble(), at.z.toDouble(),
                    0.2f, 0.2f, 0.2f, 0f, 2
                )
                platform.instance.sendGroupedPacket(packet)
            }
        }
    }

    override fun spawn(
        platform: MinestomPlatform,
        entityId: EntityId,
        entityType: AimTrainerEntityType
    ): Pair<MinestomEntity, EntityProperties?> {
        return when (entityType) {
            AimTrainerEntityType.Target -> {
                val entity = MinestomEntity(EntityType.ZOMBIE)
                entity to null
            }
        }
    }

    override fun setBlock(
        platform: MinestomPlatform,
        position: Vector3I,
        blockType: AimTrainerBlockType
    ) {
        val block = when (blockType) {
            AimTrainerBlockType.Floor -> Block.STONE
            AimTrainerBlockType.Wall -> Block.BLACK_CONCRETE
        }
        platform.instance.setBlock(
            position.x, position.y, position.z,
            block
        )
    }

    override fun createItem(
        platform: MinestomPlatform,
        itemType: AimTrainerItemType
    ): MinestomItem {
        return when (itemType) {
            AimTrainerItemType.Gun -> {
                ItemStack.of(EMERALD)
                    .with(
                        DataComponents.ATTACK_RANGE,
                        AttackRange(
                            /* minReach = */ 0f,
                            /* maxReach = */ 64f,
                            /* minCreativeReach = */ 0f,
                            /* maxCreativeReach = */ 64f,
                            /* hitboxMargin = */ 0f,
                            /* mobFactor = */ 1f
                        )
                    )
            }
        }
    }

    override fun getSoundData(soundType: AimTrainerSoundType): List<SoundData> {
        return when (soundType) {
            AimTrainerSoundType.Hit -> listOf(SoundData(SoundEvent.BLOCK_NOTE_BLOCK_BELL.key().toString(), 1f, 0.75f))
        }
    }

    override fun onRemove(platformEntity: MinestomEntity) {}

}