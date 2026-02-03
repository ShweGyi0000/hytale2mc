package com.hytale2mc.ecs.data.state

import com.hytale2mc.ecs.query.Query
import com.hytale2mc.ecs.query.getValue
import com.hytale2mc.ecs.system.Condition
import com.hytale2mc.ecs.system.exec

interface State

class StateMachine<S : State>(
    state: S,
) {

    var previous: S? = null
        internal set

    var state: S = state
        internal set

    @PublishedApi
    internal val stateQueue = java.util.LinkedList<S>()

}

data class StateView<S : State>(
    val previous: S?,
    val current: S,
    val next: S?
)

inline fun <reified S : State, reified WANTED : S> inState(): Condition = exec(
    object : Query() {
        val state by read<S>()
    }
) { q ->
    q.state is WANTED
}

inline fun <reified S : State, reified WANTED1 : S, reified WANTED2 : S> inStates(): Condition = exec(
    object : Query() {
        val state by read<S>()
    }
) { q ->
    q.state is WANTED1 || q.state is WANTED2
}

fun Condition.not(): Condition {
    return copy(exec = { !exec.invoke(this) })
}