package com.hytale2mc.ecs.composer

import com.hytale2mc.ecs.data.event.Event
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@ComposerDsl
class EventComposer internal constructor() {

    @PublishedApi
    internal val events = mutableMapOf<KClass<out Event>, RegisteredEvent<*>>()

    inline fun <reified E : Event> event() {
        check(!events.containsKey(E::class)) { "Event ${E::class.qualifiedName} was already registered" }
        events[E::class] = RegisteredEvent(serializer<E>(), Event.EventReader(E::class))
    }

}

data class RegisteredEvent<E : Event>(
    val serializer: KSerializer<E>,
    val reader: Event.EventReader<E>
) {

}