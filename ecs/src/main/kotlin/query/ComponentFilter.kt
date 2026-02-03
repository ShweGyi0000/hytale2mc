package com.hytale2mc.ecs.query

import com.hytale2mc.ecs.data.Entity
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.get
import com.hytale2mc.ecs.data.has

typealias ComponentFilter = (
    previousSystemRunTick: Long,
    entity: Entity
) -> Boolean

object ComponentFilters {

    inline fun <reified C : Component> with(): ComponentFilter = { _, entity ->
        entity.has<C>()
    }

    inline fun <reified C : Component> without(): ComponentFilter = { _, entity ->
        !entity.has<C>()
    }

    inline fun <reified C : Component> changed(): ComponentFilter = { previousSystemRunTick, entity ->
        val component = entity.get<C>()
        if (component != null) {
            val changedAt = component.changedAt
            if (changedAt == null) {
                component.addedAt > previousSystemRunTick
            } else {
                changedAt > previousSystemRunTick
            }
        } else {
            false
        }
    }

    inline fun <reified C : Component> added(): ComponentFilter = { previousSystemRunTick, entity ->
        val component = entity.get<C>()
        if (component != null) {
            component.addedAt > previousSystemRunTick
        } else {
            false
        }
    }

    fun and(
        vararg filters: ComponentFilter
    ): ComponentFilter = { tick, entity ->
        filters.all { it.invoke(tick, entity) }
    }

    fun or(
        vararg filters: ComponentFilter
    ): ComponentFilter = { tick, entity ->
        filters.any { it.invoke(tick, entity) }
    }

}