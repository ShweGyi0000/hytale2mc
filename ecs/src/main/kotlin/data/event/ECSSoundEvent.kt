package com.hytale2mc.ecs.data.event

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ECSSoundEvent(
    val sound: ECSDynamic.SoundType<*>,
    val receiver: SoundReceiver,
) : Event.Ecs2Platform() {

    @Serializable
    sealed interface SoundReceiver {

        @Serializable
        data class Personal(
            val entityIds: List<EntityId>,
        ) : SoundReceiver

        @Serializable
        data class Position(val position: @Contextual Vector3F) : SoundReceiver

    }

}