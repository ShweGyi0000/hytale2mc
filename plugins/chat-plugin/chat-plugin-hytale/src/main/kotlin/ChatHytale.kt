package com.hytale2mc.chat.hytale

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.util.EventTitleUtil
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.PlatformEntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.hytale.HytalePlatform
import com.hytale2mc.ecs.platform.hytale.toMessage
import com.hytale2mc.ecs.text.raw
import com.hytale2mc.plugin.chat.ChatBlockType
import com.hytale2mc.plugin.chat.ChatEntityType
import com.hytale2mc.plugin.chat.ChatItemType
import com.hytale2mc.plugin.chat.ChatPlatformHandler
import com.hytale2mc.plugin.chat.ChatRenderable
import com.hytale2mc.plugin.chat.ChatSoundType
import korlibs.math.geom.Vector3I
import korlibs.time.seconds

data object ChatHytale : ChatPlatformHandler<PlatformEntityId, ItemStack, HytalePlatform> {

    override fun init(platform: HytalePlatform) {}

    override fun render(
        platform: HytalePlatform,
        renderable: ChatRenderable
    ) {
        when (renderable) {
            is ChatRenderable.ChatMessage -> {
                when (renderable.receivers) {
                    null -> {
                        when (val message = renderable.message) {
                            is Chat -> {
                                platform.world.sendMessage(message.message.toMessage())
                            }
                            is Title -> {
                                EventTitleUtil.showEventTitleToWorld(
                                    message.title.toMessage(),
                                    message.subtitle.toMessage(),
                                    false,
                                    null,
                                    message.duration?.seconds?.toFloat() ?: 3.5f,
                                    message.fadeIn?.seconds?.toFloat() ?: 0.1f,
                                    message.fadeOut?.seconds?.toFloat() ?: 0.1f,
                                    platform.world.entityStore.store
                                )
                            }
                        }
                    }
                    else -> {
                        for (e in renderable.receivers) {
                            val ref = platform.entityId2Ref[e] ?: continue
                            if (!ref.isValid) {
                                continue
                            }
                            val playerRef = platform.world.entityStore.store.getComponent(ref, PlayerRef.getComponentType())
                            if (playerRef == null) {
                                continue
                            }
                            when (val message = renderable.message) {
                                is Chat -> {
                                    playerRef.sendMessage(message.message.toMessage())
                                }
                                is Title -> {
                                    EventTitleUtil.showEventTitleToPlayer(
                                        playerRef,
                                        Message.raw(message.title?.raw() ?: ""),
                                        Message.raw(message.subtitle?.raw() ?: ""),
                                        false,
                                        null,
                                        message.duration?.seconds?.toFloat() ?: 3.5f,
                                        message.fadeIn?.seconds?.toFloat() ?: 0f,
                                        message.fadeOut?.seconds?.toFloat() ?: 0f,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun spawn(
        platform: HytalePlatform,
        entityId: EntityId,
        entityType: ChatEntityType
    ): Pair<PlatformEntityId, EntityProperties?> {
        throw IllegalStateException()
    }

    override fun setBlock(
        platform: HytalePlatform,
        position: Vector3I,
        blockType: ChatBlockType
    ) {
        throw IllegalStateException()
    }

    override fun createItem(
        platform: HytalePlatform,
        itemType: ChatItemType
    ): ItemStack {
        throw IllegalStateException()
    }

    override fun getSoundData(soundType: ChatSoundType): List<SoundData> {
        throw IllegalStateException()
    }

    override fun onRemove(platformEntity: PlatformEntityId) {}
}