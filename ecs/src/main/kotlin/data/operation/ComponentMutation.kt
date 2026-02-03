package com.hytale2mc.ecs.data.operation

import kotlinx.serialization.Serializable
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.space.SpaceOrigin
import kotlin.time.Instant

@Serializable
data class ComponentMutation(
    val entityId: EntityId,
    val new: Component,
    val origin: SpaceOrigin,
    val source: DataOperation.Source,
    val version: Long = 0L,
    val timeNs: Instant = Instant.DISTANT_PAST
) : DataEvent {

    init {
        if (source is DataOperation.Source.ECS) {
            check(origin == PRIMARY)
        }
    }
}