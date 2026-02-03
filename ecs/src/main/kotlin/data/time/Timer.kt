package com.hytale2mc.ecs.data.time

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

@Serializable
data class Timer(
    val duration: Duration = Duration.ZERO,
    val mode: TimerMode = TimerMode.ONCE,
    val stopwatch: Stopwatch = Stopwatch(),
    val isFinished: Boolean = false,
    val timesFinishedThisTick: Int = 0
) {

    val justFinished: Boolean
        get() = timesFinishedThisTick > 0

    fun elapsed(): Duration {
        return stopwatch.elapsed
    }

    fun elapsed(time: Duration): Timer {
        return copy(stopwatch = stopwatch.elapsed(time))
    }

    fun finish(): Timer {
        if (isFinished) return this
        val remaining = remaining()
        return tick(remaining)
    }

    fun withMode(mode: TimerMode): Timer {
        return Timer(
            mode = mode,
            isFinished = if (this.mode != TimerMode.REPEATING && mode == TimerMode.REPEATING && isFinished) {
                stopwatch.reset()
                justFinished
            } else isFinished
        )
    }

    fun tick(delta: Duration): Timer {
        if (isPaused()) {
            return copy(timesFinishedThisTick = 0, isFinished = if (mode == TimerMode.REPEATING) false else isFinished)
        }

        if (mode != TimerMode.REPEATING && isFinished) {
            return copy(timesFinishedThisTick = 0)
        }

        var stopwatch = stopwatch.tick(delta)
        val isFinished = stopwatch.elapsed >= duration
        var timesFinishedThisTick: Int

        if (isFinished) {
            if (mode == TimerMode.REPEATING) {
                val durationNanos = duration.inWholeNanoseconds

                timesFinishedThisTick = if (durationNanos != 0L) {
                    (stopwatch.elapsed.inWholeNanoseconds / durationNanos).toInt()
                } else {
                    Int.MAX_VALUE
                }

                val remNanos = if (durationNanos != 0L) {
                    stopwatch.elapsed.inWholeNanoseconds % durationNanos
                } else {
                    0L
                }

                stopwatch = stopwatch.elapsed(remNanos.nanoseconds)
            } else {
                timesFinishedThisTick = 1
                stopwatch = stopwatch.elapsed(duration)
            }
        } else {
            timesFinishedThisTick = 0
        }

        return copy(isFinished = isFinished, stopwatch = stopwatch, timesFinishedThisTick = timesFinishedThisTick)
    }

    fun pause(): Timer = when (stopwatch.isPaused) {
        true -> this
        false -> copy(stopwatch = stopwatch.pause())
    }

    fun unpause(): Timer = when (stopwatch.isPaused) {
        true -> copy(stopwatch = stopwatch.unpause())
        false -> this
    }

    fun isPaused(): Boolean {
        return stopwatch.isPaused
    }

    fun reset(): Timer {
        return Timer(
            duration = duration,
            mode = mode,
            stopwatch = stopwatch.reset(),
            isFinished = false,
            timesFinishedThisTick = 0
        )
    }

    fun fraction(): Float {
        return when (duration) {
            ZERO -> 1f
            INFINITE -> 0f
            else -> (elapsed().inWholeNanoseconds.toDouble() / duration.inWholeNanoseconds.toDouble()).toFloat()
        }
    }

    fun remaining(): Duration {
        return duration - elapsed()
    }
}

enum class TimerMode {
    REPEATING,
    ONCE
}