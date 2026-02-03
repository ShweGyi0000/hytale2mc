package com.hytale2mc.plugin.chat

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.event.PlayerMessageEvent
import com.hytale2mc.ecs.plugin.PluginKey
import com.hytale2mc.ecs.plugin.PluginPlatformHandler
import com.hytale2mc.ecs.text.Text
import kotlinx.serialization.Serializable
import kotlin.time.Duration

typealias ChatPlatformHandler<P_E, P_I, P> = PluginPlatformHandler<ChatKey, P_E, P_I, P, ChatRenderable, ChatEntityType, ChatBlockType, ChatItemType, ChatSoundType>
@Serializable
data object ChatKey : PluginKey<ChatKey>

@Serializable
sealed interface ChatBlockType : ECSDynamic.BlockType<ChatKey>
@Serializable
sealed interface ChatEntityType : ECSDynamic.EntityType<ChatKey>
@Serializable
sealed interface ChatItemType : ECSDynamic.ItemType<ChatKey>
@Serializable
sealed interface ChatRenderable : ECSDynamic.Renderable<ChatKey> {

    override val owner: ChatKey
        get() = ChatKey

    @Serializable
    data class ChatMessage(
        val message: SendableMessage,
        val receivers: List<EntityId>? = Everyone
    ) : ChatRenderable {

        companion object {
            val Everyone = null
        }

    }

}

@Serializable
sealed interface ChatSoundType : ECSDynamic.SoundType<ChatKey>


@Serializable
sealed interface SendableMessage {

    @Serializable
    data class Chat(val message: Text) : SendableMessage

    @Serializable
    data class Title(
        val title: Text? = null,
        val subtitle: Text? = null,
        val duration: Duration? = null,
        val fadeIn: Duration? = null,
        val fadeOut: Duration? = null
    ) : SendableMessage
}

typealias ChatMessageFormatter = (ECSDynamic.EntityType<*>, PlayerMessageEvent) -> Text