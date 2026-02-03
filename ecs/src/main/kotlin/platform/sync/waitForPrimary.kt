package com.hytale2mc.ecs.platform.sync

import com.hytale2mc.ecs.data.operation.DataOperation
import korlibs.time.milliseconds
import korlibs.time.seconds
import com.hytale2mc.ecs.data.operation.PrimaryStarted
import com.hytale2mc.ecs.platform.Platform
import com.hytale2mc.ecs.platform.executeOperationsFromPrimary
import com.hytale2mc.ecs.space.Space

fun waitForPrimary(space: Space) {
    check(space.origin == REPLICA)

    var messages = emptyList<DataOperation>()
    var counter = 0
    while (messages.isEmpty() && counter < 10) {
        messages = space.streamReader.read(maxMessages = 1, maxDuration = 45.seconds)
        Thread.sleep(1000)
        counter++
    }
    if (messages.isEmpty()) throw IllegalStateException("Primary failed to start in time")
    val primaryStarted = messages.single()
    check(primaryStarted is PrimaryStarted) { messages }
    space.isReplaying = primaryStarted.replaying
}

fun catchUpAsReplica(
    platform: Platform<*, *>,
    space: Space
) {
    check(space.origin == REPLICA)

    val batch = 250
    while (true) {
        val operations = space.streamReader.read(batch, maxDuration = 15.milliseconds)
        executeOperationsFromPrimary(platform, operations)
        if (operations.size < batch) {
            break
        }
    }
}