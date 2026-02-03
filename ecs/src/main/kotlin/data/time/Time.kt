package com.hytale2mc.ecs.data.time

import com.hytale2mc.ecs.data.Resource
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.TestTimeSource

data class Time(
    val startup: ComparableTimeMark = TestTimeSource().markNow(),
    val firstUpdate: ComparableTimeMark? = null,
    val lastUpdate: ComparableTimeMark? = null,
    val isPaused: Boolean = false,
    val relativeSpeed: Double = 1.0,
    val wrapPeriod: Duration = 1.hours,
    val delta: Duration = Duration.ZERO,
    val elapsed: Duration = Duration.ZERO,
    val rawDelta: Duration = Duration.ZERO,
    val rawElapsed: Duration = Duration.ZERO
) : Resource() {

    val deltaSeconds: Float
        get() = delta.toDouble(DurationUnit.SECONDS).toFloat()
    val deltaSecondsF64: Double
        get() = delta.toDouble(DurationUnit.SECONDS)

    val elapsedSeconds: Float
        get() = elapsed.toDouble(DurationUnit.SECONDS).toFloat()
    val elapsedSecondsF64: Double
        get() = elapsed.toDouble(DurationUnit.SECONDS)

    val rawDeltaSeconds: Float
        get() = rawDelta.toDouble(DurationUnit.SECONDS).toFloat()
    val rawDeltaSecondsF64: Double
        get() = rawDelta.toDouble(DurationUnit.SECONDS)

    val rawElapsedSeconds: Float
        get() = rawElapsed.toDouble(DurationUnit.SECONDS).toFloat()
    val rawElapsedSecondsF64: Double
        get() = rawElapsed.toDouble(DurationUnit.SECONDS)

    val elapsedWrapped: Duration
        get() = durationMod(elapsed, wrapPeriod)
    val elapsedSecondsWrapped: Float
        get() = elapsedWrapped.toDouble(DurationUnit.SECONDS).toFloat()
    val elapsedSecondsWrappedF64: Double
        get() = elapsedWrapped.toDouble(DurationUnit.SECONDS)

    val rawElapsedWrapped: Duration
        get() = durationMod(rawElapsed, wrapPeriod)
    val rawElapsedSecondsWrapped: Float
        get() = rawElapsedWrapped.toDouble(DurationUnit.SECONDS).toFloat()
    val rawElapsedSecondsWrappedF64: Double
        get() = rawElapsedWrapped.toDouble(DurationUnit.SECONDS)


    fun update(tick: Long, instant: ComparableTimeMark): Time {
        changedAt = tick
        val calculatedRawDelta = if (lastUpdate == null) {
            instant.minus(startup)
        } else {
            instant.minus(lastUpdate)
        }

        val calculatedDelta = if (isPaused) {
            Duration.ZERO
        } else if (relativeSpeed != 1.0) {
            calculatedRawDelta * relativeSpeed
        } else {
            calculatedRawDelta
        }

        val newFirstUpdate = firstUpdate ?: instant

        return this.copy(
            firstUpdate = newFirstUpdate,
            lastUpdate = instant,
            delta = calculatedDelta,
            rawDelta = calculatedRawDelta,
            elapsed = this.elapsed + calculatedDelta,
            rawElapsed = this.rawElapsed + calculatedRawDelta
        )
    }

    fun withRelativeSpeed(ratio: Float): Time = withRelativeSpeed(ratio.toDouble())

    fun withRelativeSpeed(ratio: Double): Time {
        require(ratio.isFinite()) { "tried to go infinitely fast" }
        require(ratio >= 0) { "tried to go back in time" }
        return this.copy(relativeSpeed = ratio)
    }

    fun withWrapPeriod(period: Duration): Time {
        require(period > Duration.ZERO) { "division by zero (wrap period must be > 0)" }
        return this.copy(wrapPeriod = period)
    }

    fun withPaused(paused: Boolean): Time {
        return this.copy(isPaused = paused)
    }

    fun relativeSpeedF32(): Float = relativeSpeed.toFloat()

    private fun durationMod(dividend: Duration, divisor: Duration): Duration {
        if (divisor == Duration.ZERO) return Duration.ZERO
        val remainderNanos = dividend.inWholeNanoseconds % divisor.inWholeNanoseconds
        return remainderNanos.nanoseconds
    }
}