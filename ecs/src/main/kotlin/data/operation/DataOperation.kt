package com.hytale2mc.ecs.data.operation

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.event.Event
import com.hytale2mc.ecs.platform.PlatformType

@Serializable
sealed interface Operation

@Serializable
@Polymorphic
sealed interface DataOperation : Operation {

    @Serializable
    sealed interface Source {

        @Serializable
        data class Platform(
            val platformType: PlatformType,
            val platformId: String,
        ): Source

        @Serializable
        data object ECS : Source
    }

}

@Serializable
sealed interface DataEvent : DataOperation

@Serializable
data class PlayerJoined(
    val platform: DataOperation.Source.Platform,
    val username: String,
    val platformEntityIdOnOrigin: EntityId,
) : DataEvent

@Serializable
data class PlayerLeft(
    val entityId: EntityId,
    val username: String,
    val platformType: PlatformType
) : DataEvent

@Serializable
data object TickFinished : DataEvent

@Serializable
data class PrimaryStarted(
    val replaying: Boolean
) : DataEvent

@Serializable
data object PrimaryFinished : DataEvent

@Serializable
data class EventWritten(
    val event: Event,
) : DataEvent