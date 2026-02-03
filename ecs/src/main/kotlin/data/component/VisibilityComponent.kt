package com.hytale2mc.ecs.data.component

import com.hytale2mc.ecs.data.EntityId
import kotlinx.serialization.Serializable

@Serializable
data class VisibilityComponent(
    val visibility: Visibility
) : Component.Ecs2PlatformOnly() {

}

@Serializable
sealed interface Visibility {

    val renderThroughWalls: Boolean

    @Serializable
    data class ToOnly(
        val entities: List<EntityId>,
        override val renderThroughWalls: Boolean = false
    ) : Visibility

    @Serializable
    data class ToEveryoneExcept(
        val entities: List<EntityId>,
        override val renderThroughWalls: Boolean = false
    ) : Visibility

    companion object {

        val ToEveryone = ToEveryoneExcept(emptyList())
        val ToNoOne = ToOnly(emptyList())

    }

}