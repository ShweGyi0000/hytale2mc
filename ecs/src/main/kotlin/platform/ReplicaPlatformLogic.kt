package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.event.Event
import com.hytale2mc.ecs.data.operation.Command
import com.hytale2mc.ecs.data.operation.DataEvent
import com.hytale2mc.ecs.data.operation.DataOperation
import com.hytale2mc.ecs.platform.sync.performNeededPlatformToEcsMutations
import com.hytale2mc.ecs.space.Space
import com.hytale2mc.ecs.space.flushOperations
import kotlin.time.Duration.Companion.milliseconds

internal fun executeOperationsFromPrimary(
    platform: Platform<*, *>,
    operations: Collection<DataOperation>
) {

    check(platform.space.origin == REPLICA)
    for (operation in operations) {
        check(operation is Command<*> || operation is Event || operation is DataEvent) { operation }
        platform.handle(operation)
    }
}

fun syncPlatformToEcs(
    platform: Platform<*, *>,
) {
    performNeededPlatformToEcsMutations(platform)
    flushOperations(platform.space, platform)
    platform.space.streamLog.flush()
    platform.space.shouldHaveNothingToFlush()
}

fun updateSpaceAsAReplica(
    platform: Platform<*, *>,
) {
    check(platform.space.origin == REPLICA)
    // platform -> ecs
    syncPlatformToEcs(platform)

    // sync with primary
    executeOperationsFromPrimary(platform, platform.space.streamReader.read(maxDuration = 15.milliseconds))

    platform.space.shouldHaveNothingToFlush()
}

fun Space.shouldHaveNothingToFlush() {
    check(operations.isEmpty()) { "Operations should have been empty" }
}