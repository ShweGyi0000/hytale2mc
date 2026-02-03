package com.hytale2mc.ecs.query

import com.hytale2mc.ecs.data.Entity
import com.hytale2mc.ecs.data.Resource
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.get
import com.hytale2mc.ecs.space.Space
import com.hytale2mc.ecs.system.Executable

internal fun prepareQuery(
    space: Space,
    executable: Executable<*>,
    tickContext: TickContext,
    query: Query
) {
    val resources = mutableListOf<Resource?>()
    for (toQuery in query.resourceActions) {
        when (val resource = space.resources.get(toQuery.type)) {
            null -> if (toQuery.isOptional) resources.add(null) else throw IllegalStateException("Tried to query a resource ${toQuery.type.qualifiedName} that doesn't exist")
            else -> resources.add(resource)
        }
        toQuery.tickContext = tickContext
    }
    val components = getComponents(space, executable, tickContext, query.componentActions, query.componentFilter)
    val states = query.stateActions.mapTo(mutableListOf()) {
        it.tickContext = tickContext
        space.states.get(it.type)!!
    }
    query.components = components
    query.resources = resources
    query.states = states
}

private fun filterEntities(
    executable: Executable<*>,
    entities: Collection<Entity>,
    filter: ComponentFilter,
): Collection<Entity> {
    return entities.filter {
        val result = filter.invoke(executable.lastRun, it)
        result
    }
}

private fun getComponents(
    space: Space,
    executable: Executable<*>,
    tickContext: TickContext,
    actions: Collection<QueryAction<*>>,
    filter: ComponentFilter?
): Components {
    val components = mutableMapOf<Int, Array<Component?>>()
    val entities = filter?.let { filterEntities(executable, space.entities.values, it) } ?: space.entities.values
    outer@for (entity in entities) {
        val entityComponents = Array<Component?>(actions.size) { null }
        for ((i, action) in actions.withIndex()) {
            val action = action as QueryAction<out Component>
            action.tickContext = tickContext
            when (val component = entity[action.type]) {
                null -> if (action.isOptional) entityComponents[i] = null else continue@outer
                else -> entityComponents[i] = component
            }
        }
        components[entity.id] = entityComponents
    }
    return components
}