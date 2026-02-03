package com.hytale2mc.ecs.data.event

import com.hytale2mc.ecs.data.EntityId
import kotlinx.serialization.Serializable

@Serializable
data class HeldItemSlotChangeEvent(
    val entityId: EntityId,
    val from: Int,
    val to: Int
) : Event.Platform2Ecs()