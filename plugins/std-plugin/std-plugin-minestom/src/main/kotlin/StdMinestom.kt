package com.hytale2mc.plugin.std.minestom

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.minestom.*
import com.hytale2mc.plugin.std.*
import korlibs.math.geom.Vector3I
import net.minestom.server.MinecraftServer
import net.minestom.server.color.AlphaColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints.CENTER
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta.BillboardConstraints.FIXED
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Team

data object StdMinestom : StdPlatformHandler<MinestomEntity, MinestomItem, MinestomPlatform> {

    override fun render(
        platform: MinestomPlatform,
        renderable: StdRenderable
    ) {
        throw IllegalStateException()
    }

    lateinit var invisibleNametagTeam: Team

    override fun init(platform: MinestomPlatform) {
        invisibleNametagTeam = MinecraftServer.getTeamManager()
            .createBuilder("invisible_nametag")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build()
    }


    override fun onRemove(platformEntity: MinestomEntity) {
        if (platformEntity is MinestomPlayer) {
            invisibleNametagTeam.removeMember(platformEntity.username)
        }
    }

    override fun spawn(
        platform: MinestomPlatform,
        entityId: EntityId,
        entityType: StdEntityType
    ): Pair<MinestomEntity, EntityProperties?> {
        return when (entityType) {
            is StdEntityType.Player -> {
                val platformPlayer = if (entityType.platform.platformId == platform.platformId && !platform.space.isReplaying) {
                    val player = platform.instance.getEntityById(entityType.platformEntityIdOnOrigin) as MinestomPlayer
                    invisibleNametagTeam.addMember(player.username)
                    player
                } else {
                    LivingEntity(MANNEQUIN)
                }
                platformPlayer to null
            }

            is StdEntityType.FakePlayer -> {
                LivingEntity(MANNEQUIN) to null
            }

            is StdEntityType.Text -> {
                Entity(TEXT_DISPLAY).apply {
                    val meta = entityMeta as TextDisplayMeta
                    meta.text = entityType.text.toAdventure()
                    meta.posRotInterpolationDuration = 2
                    meta.backgroundColor = AlphaColor(0, 0, 0, 0).asARGB()
                    meta.billboardRenderConstraints = when (entityType.billboardConstraints) {
                        StdEntityType.Text.BillboardConstraints.FIXED -> FIXED
                        StdEntityType.Text.BillboardConstraints.CENTER -> CENTER
                    }
                } to null
            }
        }
    }

    override fun setBlock(
        platform: MinestomPlatform,
        position: Vector3I,
        blockType: StdBlockType
    ) {
        throw IllegalStateException()
    }

    override fun createItem(platform: MinestomPlatform, itemType: StdItemType): MinestomItem {
        throw IllegalStateException()
    }

    override fun getSoundData(soundType: StdSoundType): List<SoundData> {
        throw IllegalStateException()
    }

}