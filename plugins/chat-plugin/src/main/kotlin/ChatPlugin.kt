package com.hytale2mc.plugin.chat

import com.github.ajalt.colormath.model.RGB
import com.hytale2mc.chat.hytale.ChatHytale
import com.hytale2mc.chat.minestom.ChatMinestom
import com.hytale2mc.ecs.platform.PlatformType
import com.hytale2mc.ecs.platform.shortenedWithColor
import com.hytale2mc.ecs.plugin.ecsPlugin
import com.hytale2mc.ecs.text.TextPart
import com.hytale2mc.plugin.std.StdEntityType

fun chatPlugin(
    chatMessageFormatter: ChatMessageFormatter = { entityType, event ->
        val (username, platform) = when (entityType) {
            is StdEntityType.Player -> {
                entityType.username to entityType.platform.platformType
            }
            else -> "Entity" to PlatformType.MINECRAFT
        }
        listOf(
            platform.shortenedWithColor(),
            TextPart(" $username: ${event.message}", color = RGB(1f, 1f, 1f))
        )
    }
) = ecsPlugin<ChatKey, ChatRenderable, ChatEntityType, ChatBlockType, ChatItemType, ChatSoundType>(
    ChatKey,
    handlers = {
      minecraft { ChatMinestom }
      hytale { ChatHytale }
    },
    systems = { chatSystems(chatMessageFormatter) }
)