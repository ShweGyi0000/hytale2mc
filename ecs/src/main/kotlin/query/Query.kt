package com.hytale2mc.ecs.query

import com.hytale2mc.ecs.composer.SpaceComposer
import com.hytale2mc.ecs.data.*
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.event.Event
import com.hytale2mc.ecs.data.state.StateMachine
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.typeOf
import com.hytale2mc.ecs.data.state.State

typealias Components = Map<Int, Array<Component?>>

@PublishedApi
internal object DataTypes {

    val component = typeOf<Component>()
    val resource = typeOf<Resource>()
    val state = typeOf<State>()

}

abstract class Query {

    @PublishedApi
    internal val componentActions = mutableListOf<QueryAction<out Component?>>()
    @PublishedApi
    internal val resourceActions = mutableListOf<QueryAction<out Resource?>>()
    @PublishedApi
    internal val stateActions = mutableListOf<QueryAction<out State?>>()

    @PublishedApi
    internal val allActions = mutableListOf<QueryAction<*>>()
    
    open val componentFilter: ComponentFilter? = null

    var components: Components = mapOf()
        internal set

    val entities: List<EntityId>
        get() = components.keys.toList()

    @PublishedApi
    internal var resources = mutableListOf<Resource?>()
    @PublishedApi
    internal var states = mutableListOf<StateMachine<*>>()

    protected inline fun <reified D> read(): QueryAction.Read<D> {
        val type = typeOf<D>()
        val action = QueryAction.Read<D>(type, this)
        addActionByType(type, action)
        return action
    }

    protected inline fun <reified D> write(): QueryAction.Write<D> {
        val type = typeOf<D>()
        val action = QueryAction.Write<D>(type, this)
        addActionByType(type, action)
        return action
    }

    context(composer: SpaceComposer)
    protected inline fun <reified E : Event> readEvent(): Event.EventReader<E> {
        allActions.add(QueryAction.Read<E>(typeOf<E>(), this@Query))
        return composer.getEventReader()
    }

    context(composer: SpaceComposer)
    protected inline fun <reified E : Event> writeEvent(): Event.EventWriter<E> {
        allActions.add(QueryAction.Write<E>(typeOf<E>(), this@Query))
        return Event.EventWriter(composer.getEventReader<E>())
    }

    @PublishedApi
    internal fun addActionByType(type: KType, action: QueryAction<*>) {
        val type = type.withNullability(false)
        val list = when {
            type.isSubtypeOf(DataTypes.component) -> {
                componentActions.add(action as QueryAction<out Component>)
                componentActions
            }
            type.isSubtypeOf(DataTypes.resource) -> {
                resourceActions.add(action as QueryAction<out Resource>)
                resourceActions
            }
            type.isSubtypeOf(DataTypes.state) -> {
                stateActions.add(action as QueryAction<out State>)
                stateActions
            }
            else -> throw IllegalArgumentException("Can't query for type: $type")
        }
        allActions.add(action)
        action.index = list.size - 1
    }

}