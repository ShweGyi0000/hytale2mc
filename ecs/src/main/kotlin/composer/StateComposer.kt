package com.hytale2mc.ecs.composer

import com.hytale2mc.ecs.data.state.State
import kotlin.reflect.KClass

typealias ComposedStates = Map<KClass<out State>, State>

@ComposerDsl
class StateComposer internal constructor() {

    @PublishedApi
    internal val states = mutableMapOf<KClass<out State>, State>()

    fun <S : State, INITIAL : S> state(
        type: KClass<S>,
        initial: INITIAL
    ) {
        states[type] = initial
    }

}