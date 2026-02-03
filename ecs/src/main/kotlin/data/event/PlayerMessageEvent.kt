package com.hytale2mc.ecs.data.event

import com.hytale2mc.ecs.data.EntityId
import kotlinx.serialization.Serializable

@Serializable
data class PlayerMessageEvent(
    val sender: EntityId,
    val message: String
) : Event.Platform2Ecs()