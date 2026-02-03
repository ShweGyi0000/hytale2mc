package com.hytale2mc.ecs.data.event

import com.hytale2mc.ecs.data.operation.EventWritten
import com.hytale2mc.ecs.query.ExecutionContext
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
sealed class Event {

    @Serializable
    sealed class Platform2Ecs : Event()
    @Serializable
    sealed class Ecs2Platform : Event()

    @Serializable
    abstract class EcsOnly : Event()

    class EventWriter<E : Event>(
        val reader: EventReader<E>
    )

    class EventReader<E : Event>(
        val type: KClass<E>
    ) {

        private val indices: MutableMap<Int, Int> = mutableMapOf()

        var events = mutableListOf<E>()

        fun consume(
            readerId: Int
        ): List<E> {
            val index = indices.getOrDefault(readerId, 0)
            if (index == events.size) {
                return emptyList()
            }
            indices[readerId] = events.size
            return events.slice(index..<events.size)
        }

    }

}

context(execution: ExecutionContext)
fun <E : Event.Ecs2Platform> Event.EventWriter<E>.write(event: E) {
    execution.space.operations.add(EventWritten(event))
}

context(_: ExecutionContext)
fun <E : Event.EcsOnly> Event.EventWriter<E>.write(event: E) {
    reader.events.add(event)
}

context(execution: ExecutionContext)
fun <E : Event.Platform2Ecs> Event.EventReader<E>.consume(): List<E> {
    return consume(execution.executable.id)
}

context(execution: ExecutionContext)
@JvmName("consume2")
fun <E : Event.EcsOnly> Event.EventReader<E>.consume(): List<E> {
    return consume(execution.executable.id)
}