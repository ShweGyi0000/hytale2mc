package com.hytale2mc.ecs.data.time

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class Stopwatch(
    val elapsed: Duration = Duration.ZERO,
    val isPaused: Boolean = false
) {

    fun elapsed(elapsed: Duration): Stopwatch {
        return copy(elapsed = elapsed)
    }

    fun tick(delta: Duration): Stopwatch {
        return when (isPaused) {
            true -> this
            false -> copy(elapsed = elapsed + delta)
        }
    }

    fun reset(): Stopwatch {
        return copy(elapsed = Duration.ZERO)
    }

    fun pause(): Stopwatch {
        return when (isPaused) {
            true -> this
            false -> copy(isPaused = true)
        }
    }

    fun unpause(): Stopwatch {
        return when (isPaused) {
            true -> copy(isPaused = false)
            false -> this
        }
    }

}