package com.hytale2mc.ecs.query

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.Resource
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.operation.ComponentMutation
import com.hytale2mc.ecs.data.operation.DataOperation
import com.hytale2mc.ecs.data.state.State
import com.hytale2mc.ecs.data.state.StateMachine
import com.hytale2mc.ecs.data.state.StateView
import com.hytale2mc.ecs.space.DataOperations
import com.hytale2mc.ecs.space.Resources
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

sealed class QueryAction<D>(
    kType: KType,
) {

    @PublishedApi
    internal abstract val query: Query
    @PublishedApi
    internal var index by Delegates.notNull<Int>()

    internal val type = kType.jvmErasure as KClass<out D>
    internal val isOptional = kType.isMarkedNullable

    @PublishedApi
    internal var tickContext by Delegates.notNull<TickContext>()

    class Write<D>(kType: KType, @PublishedApi override val query: Query) : QueryAction<D>(kType)
    class Read<D>(kType: KType, @PublishedApi override val query: Query) : QueryAction<D>(kType)

}

// Components
context(executionContext: ExecutionContext)
inline operator fun <reified C : Component?> QueryAction<C>.get(
    entityId: Int,
): C {
    return query.components.getValue(entityId)[index] as C
}

context(executionContext: ExecutionContext)
inline fun <reified C : Component?> QueryAction<C>.getIfEntityIsPresent(
    entityId: Int,
): C? {
    val components = query.components[entityId] ?: return null
    return components[index] as C
}

fun QueryAction.Write<*>.set(
    entity: EntityId,
    newComponent: Component
) {
    check(tickContext.space.origin == PRIMARY)
    DataOperations.replaceComponent(
        tickContext.space,
        ComponentMutation(
            entity,
            newComponent,
            PRIMARY,
            DataOperation.Source.ECS
        )
    )
    query.components.getValue(entity)[index] = newComponent
}

inline fun <reified C : Component> QueryAction.Write<C>.mutate(
    entity: EntityId,
    block: (C) -> C
): C {
    val old = query.components.getValue(entity)[this.index]!! as C
    val new = block.invoke(old)
    set(entity, new)
    return new
}

inline fun <reified C : Component?> QueryAction.Write<C>.mutateIfPresent(
    entity: EntityId,
    block: (C & Any) -> C & Any
): C? {
    val old = query.components.getValue(entity)[this.index] as? C ?: return null
    val new = block.invoke(old)
    set(entity, new)
    return new
}

// Resources
inline fun <reified R : Resource?> QueryAction<R>.get(): R {
    return query.resources[index] as R
}

inline operator fun <reified R : Resource?> QueryAction<R>.getValue(
    thisRef: Any?,
    property: KProperty<*>
): R {
    return get()
}

inline operator fun <reified R : Resource?> QueryAction.Write<R>.setValue(
    thisRef: Any?,
    property: KProperty<*>,
    newValue: R
) {
    set(newValue)
}

inline fun <reified R : Resource?> QueryAction.Write<R>.set(
    newResource: R
) {
    Resources.replaceResource(
        tickContext.space,
        newResource!!,
        tickContext.tick,
    )
    query.resources[index] = newResource
}

inline fun <reified R : Resource?> QueryAction.Write<R>.mutate(
    block: (R) -> R
) {
    val old = query.resources[this.index] as R
    set(block.invoke(old))
}

// States
inline fun <reified S : State> QueryAction<S>.get(): StateView<S> {
    val stateMachine = query.states[index] as StateMachine<S>
    return StateView(stateMachine.previous, stateMachine.state, stateMachine.stateQueue.peek())
}

inline fun <reified S : State> QueryAction.Write<S>.get(): StateMachine<S> {
    val stateMachine = query.states[index] as StateMachine<S>
    return stateMachine
}

inline operator fun <reified S : State> QueryAction<S>.getValue(
    thisRef: Any?,
    property: KProperty<*>
): S {
    return get().current
}

inline operator fun <reified S : State> QueryAction.Write<S>.setValue(
    thisRef: Any?,
    property: KProperty<*>,
    newValue: S
) {
    return queue(newValue)
}

inline fun <reified S : State> QueryAction.Write<S>.queue(newState: S) {
    get().queue(newState)
}

inline fun <reified S : State> StateMachine<S>.queue(newState: S) {
    stateQueue.add(newState)
}