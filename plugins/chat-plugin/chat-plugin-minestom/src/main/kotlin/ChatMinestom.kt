package com.hytale2mc.chat.minestom

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.minestom.MinestomEntity
import com.hytale2mc.ecs.platform.minestom.MinestomItem
import com.hytale2mc.ecs.platform.minestom.MinestomPlatform
import com.hytale2mc.ecs.platform.minestom.toAdventure
import com.hytale2mc.plugin.chat.*
import korlibs.math.geom.Vector3I
import korlibs.time.millisecondsInt
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.title.Title

data object ChatMinestom : ChatPlatformHandler<MinestomEntity, MinestomItem, MinestomPlatform> {

    override fun init(platform: MinestomPlatform) {}

    override fun render(
        platform: MinestomPlatform,
        renderable: ChatRenderable
    ) {
        when (renderable) {
            is ChatRenderable.ChatMessage -> handleChatMessage(platform, renderable)
        }
    }

    private fun handleChatMessage(platform: MinestomPlatform, message: ChatRenderable.ChatMessage) {
        val audiences = when (val receivers = message.receivers) {
            null -> listOf(platform.instance)
            else -> {
                receivers
                    .map { platform.instance.getEntityById(platform.entityId2PlatformEntityId.getValue(it)) }
                    .filterIsInstance<Audience>()
            }
        }

        for (audience in audiences) {
            when (val message = message.message) {
                is SendableMessage.Chat -> audience.sendMessage(message.message.toAdventure())
                is SendableMessage.Title -> {
                    audience.showTitle(
                        Title.title(
                            message.title.toAdventure(),
                            message.subtitle.toAdventure(),
                            message.fadeIn?.millisecondsInt?.div(50) ?: 1,
                            message.duration?.millisecondsInt?.div(50) ?: 70,
                            message.fadeOut?.millisecondsInt?.div(50) ?: 1,
                        )
                    )
                }
            }
        }
    }

    override fun spawn(
        platform: MinestomPlatform,
        entityId: EntityId,
        entityType: ChatEntityType
    ): Pair<MinestomEntity, EntityProperties?> {
        throw IllegalStateException()
    }

    override fun setBlock(
        platform: MinestomPlatform,
        position: Vector3I,
        blockType: ChatBlockType
    ) {
        throw IllegalStateException()
    }

    override fun createItem(
        platform: MinestomPlatform,
        itemType: ChatItemType
    ): MinestomItem {
        throw IllegalStateException()
    }

    override fun getSoundData(soundType: ChatSoundType): List<SoundData> {
        throw IllegalStateException()
    }

    override fun onRemove(platformEntity: MinestomEntity) {}
}