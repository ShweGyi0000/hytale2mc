package com.hytale2mc.ecs.data.event

import com.hytale2mc.ecs.data.EntityId
import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class EntityInputEvent(
    val entityId: EntityId,
    val input: Input,
) : Event.Platform2Ecs() {

    @Serializable
    sealed interface Input {

    }
}

@Serializable
sealed interface Click : EntityInputEvent.Input {

    val forward: Vector3F
    val inventorySlot: Int

    @Serializable
    data class AtEntity(
        override val inventorySlot: Int,
        @Contextual
        override val forward: Vector3F,
        val atEntityId: EntityId
    ) : Click

    @Serializable
    data class AtNothing(
        override val inventorySlot: Int,
        @Contextual
        override val forward: Vector3F
    ) : Click

}