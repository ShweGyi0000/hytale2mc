package com.hytale2mc.plugin.chat

import com.hytale2mc.ecs.composer.SpaceComposer
import com.hytale2mc.ecs.data.component.EntityTypeComponent
import com.hytale2mc.ecs.data.event.PlayerMessageEvent
import com.hytale2mc.ecs.data.event.consume
import com.hytale2mc.ecs.data.operation.RenderingCommand
import com.hytale2mc.ecs.phase.Phase
import com.hytale2mc.ecs.query.Query
import com.hytale2mc.ecs.query.getIfEntityIsPresent
import com.hytale2mc.ecs.system.exec
import com.hytale2mc.ecs.system.queueCommand

fun SpaceComposer.chatSystems(
    chatMessageFormatter: ChatMessageFormatter
) {
    systems {
        system(
            "chat:message_handler",
            Phase.Update,
            exec(
                object : Query() {
                    val entityType = read<EntityTypeComponent>()
                    val incomingChat = readEvent<PlayerMessageEvent>()
                }
            ) { q ->
                val messages = mutableListOf<ChatRenderable.ChatMessage>()
                for (message in q.incomingChat.consume()) {
                    val entityType = q.entityType.getIfEntityIsPresent(message.sender)?.type ?: continue
                    messages.add(
                        ChatRenderable.ChatMessage(
                            SendableMessage.Chat(
                                chatMessageFormatter.invoke(entityType, message)
                            )
                        )
                    )
                }
                if (messages.isNotEmpty()) {
                    queueCommand(RenderingCommand.Render("render_chat", messages))
                }
            }
        )
    }
}