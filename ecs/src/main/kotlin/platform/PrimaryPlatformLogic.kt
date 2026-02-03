package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.operation.Command
import com.hytale2mc.ecs.data.operation.ComponentMutation
import com.hytale2mc.ecs.data.operation.DataOperation
import com.hytale2mc.ecs.data.operation.EventWritten
import com.hytale2mc.ecs.data.operation.PlayerJoined
import com.hytale2mc.ecs.data.operation.PlayerLeft
import com.hytale2mc.ecs.data.operation.PrimaryFinished
import com.hytale2mc.ecs.data.operation.PrimaryStarted
import com.hytale2mc.ecs.data.operation.TickFinished
import com.hytale2mc.ecs.data.time.Time
import com.hytale2mc.ecs.metrics.ExecutionNode
import com.hytale2mc.ecs.platform.sync.performNeededPlatformToEcsMutations
import com.hytale2mc.ecs.space.Resources
import com.hytale2mc.ecs.space.Space
import com.hytale2mc.ecs.space.flushOperations
import com.hytale2mc.ecs.space.updateSpace
import com.hytale2mc.eventstream.EventStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
fun updateSpaceAsPrimary(
    platform: Platform<*, *>,
    space: Space
): ExecutionNode.Space {
    if (!space.isReplaying) {
        // queue the events from replicas
        queueEventsFromReplicas(space)

        // sync platform -> ecs
        performNeededPlatformToEcsMutations(platform)
        flushOperations(space, platform)
    } else {
        // queue events from replay
        executeEventsFromReplayUntilTickFinished(
            space,
            platform,
            space.streamReader
        )
    }

    // tick the logic
    val execution = updateSpace(space, platform)

    // sync ecs -> platform
    space.shouldHaveNothingToFlush()

    // flush TickFinished
    space.operations.add(TickFinished)
    flushOperations(space, platform)
    space.streamLog.flush()

    space.shouldHaveNothingToFlush()

    val time = space.resources.get(Time::class)
    if (time != null) {
        Resources.replaceResource(space, time.update(space.tick, time.startup.plus(time.elapsed + 50.milliseconds)), space.tick)
    }

    return execution
}

private fun queueEventsFromReplicas(
    space: Space
) {
    for (operation in space.streamReader.read(maxDuration = 15.milliseconds)) {
        when (operation) {
            is Command<*>, TickFinished, is PrimaryStarted, is PrimaryFinished -> throw IllegalStateException("Operation $operation is not allowed from a replica")
            is ComponentMutation, is PlayerJoined, is PlayerLeft, is EventWritten -> {
                space.operations.add(operation)
            }
        }
    }
}

private fun executeEventsFromReplayUntilTickFinished(
    space: Space,
    platform: Platform<*, *>,
    replayLog: EventStream.Reader<DataOperation>,
) {
    val operations = mutableListOf<DataOperation>()
    while (true) {
        val readOperations = replayLog.read(1, Duration.ZERO)
        if (readOperations.isEmpty()) {
            break
        }
        val operation = readOperations.single()
        when (operation) {
            is TickFinished -> break
            is PrimaryStarted -> continue
            else -> {
                operations.add(operation)
                continue
            }
        }
    }
    for (operation in operations) {
        space.operations.add(operation)
    }
    flushOperations(space, platform)
}