package com.hytale2mc.ecs.data.container

import com.hytale2mc.ecs.data.state.State
import com.hytale2mc.ecs.data.state.StateMachine
import kotlin.reflect.KClass

class StateContainer : Container<StateMachine<*>>() {

    private val states = mutableMapOf<KClass<out State>, StateMachine<*>>()

    override fun contained(): Collection<StateMachine<*>> {
        return states.values
    }

    fun <S : State> add(type: KClass<S>, initial: S) {
        check(!states.containsKey(type))
        val stateMachine = StateMachine(initial)
        states[type] = stateMachine
    }

    fun <S : State> addUnsafe(type: KClass<out State>, initial: S) {
        add(type as KClass<S>, initial)
    }

    fun <S : State> get(type: KClass<S>): StateMachine<S>? {
        return states[type] as? StateMachine<S>
    }

}