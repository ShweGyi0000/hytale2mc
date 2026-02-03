package com.hytale2mc.ecs.phase

import kotlin.reflect.KClass
import com.hytale2mc.ecs.data.state.State

sealed interface Phase {

    object StartUp : Phase

    object StateTransition : Phase
    object Before: Phase
    object Update : Phase
    object After : Phase

    sealed interface StateTransitions<S : State> : Phase {

        data class Enter<S : State>(
            val state: KClass<S>,
        ) : StateTransitions<S>

        data class Exit<S : State>(
            val state: KClass<S>
        ) : StateTransitions<S>

        data class Transition<S : State, FROM : S, TO : S>(
            val from: KClass<FROM>,
            val to: KClass<TO>
        ) : StateTransitions<S>

    }

}