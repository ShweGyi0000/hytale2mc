package com.hytale2mc.ecs.metrics

import kotlin.time.Duration

sealed class ExecutionNode {

    data class Space(
        val executions: MutableList<SpaceExecutionNode>,
    ) : ExecutionNode() {

        var duration: Duration = Duration.ZERO

    }

    sealed class SpaceExecutionNode : ExecutionNode()

    class Phase(
        val phase: com.hytale2mc.ecs.phase.Phase,
        val batches: MutableList<SystemBatch>,
    ) : SpaceExecutionNode() {

        var duration: Duration = Duration.INFINITE

    }

    class CommandFlush(
        val commands: MutableList<Command>
    ) : SpaceExecutionNode() {

        var duration: Duration = Duration.ZERO

    }
    
    class Command(
        val systemId: String,
        val commandId: String,
        val duration: Duration
    ) : ExecutionNode()

    class SystemBatch(
        val id: String,
        val systems: MutableList<System>,
        var status: BatchStatus = BatchStatus.NotReached
    ) : ExecutionNode() {

    }

    class System(
        val id: String,
        var status: SystemStatus = SystemStatus.NotReached
    ) : ExecutionNode() {

    }

    sealed interface BatchStatus {

        object NotReached : BatchStatus
        class ConditionFailed(val condition: System) : BatchStatus
        object Completed : BatchStatus

    }

    sealed interface SystemStatus {

        object NotReached : SystemStatus
        class ConditionFailed(val condition: System) : SystemStatus
        class Completed(val thread: String, val duration: Duration) : SystemStatus

    }

}