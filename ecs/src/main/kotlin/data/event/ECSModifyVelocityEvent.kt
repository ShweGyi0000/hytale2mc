package com.hytale2mc.ecs.data.event

import com.hytale2mc.ecs.data.EntityId
import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ECSModifyVelocityEvent(
    val entityId: EntityId,
    @Contextual
    val velocity: Vector3F,
    val action: Action
) : Event.Ecs2Platform() {

    enum class Action {
        ADD,
        SET
    }

}

