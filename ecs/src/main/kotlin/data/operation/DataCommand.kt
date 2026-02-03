package com.hytale2mc.ecs.data.operation

import kotlinx.serialization.Serializable
import com.hytale2mc.ecs.data.DataId
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.component.Component

@Serializable
sealed interface DataCommand<R> : Command<R> {

    @Serializable
    data class AddEntities(
        override val commandId: String,
        val components: List<List<Component>>
    ) : DataCommand<List<EntityId>> {

        init {
            check(components.isNotEmpty())
        }

    }

    @Serializable
    data class RemoveEntities(
        override val commandId: String,
        val entities: List<EntityId>
    ) : DataCommand<Unit> {

        init {
            check(entities.isNotEmpty())
        }

    }

    @Serializable
    data class AddComponents(
        override val commandId: String,
        val components: Map<EntityId, List<Component>>
    ) : DataCommand<Unit> {

        init {
            check(components.isNotEmpty())
        }

    }
    @Serializable
    data class RemoveComponents(
        override val commandId: String,
        val components: Map<EntityId, List<DataId>>
    ) : DataCommand<Unit> {

        init {
            check(components.isNotEmpty())
        }

    }

    @Serializable
    data class TransferPlayers(
        override val commandId: String,
        val transfers: Map<EntityId, Destination>
    ) : DataCommand<Unit> {

        @Serializable
        data class Destination(val host: String, val port: Int)

    }

}