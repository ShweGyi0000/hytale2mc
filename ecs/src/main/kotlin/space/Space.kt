package com.hytale2mc.ecs.space

import com.hytale2mc.ecs.data.ECSMap
import com.hytale2mc.ecs.data.Entity
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.Resource
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.container.StateContainer
import com.hytale2mc.ecs.data.container.TypedContainer
import com.hytale2mc.ecs.data.event.Event
import com.hytale2mc.ecs.data.operation.Command
import com.hytale2mc.ecs.data.operation.ComponentMutation
import com.hytale2mc.ecs.data.operation.DataCommand
import com.hytale2mc.ecs.data.operation.DataEvent
import com.hytale2mc.ecs.data.operation.DataOperation
import com.hytale2mc.ecs.data.operation.EventWritten
import com.hytale2mc.ecs.data.operation.Operation
import com.hytale2mc.ecs.data.operation.PlayerJoined
import com.hytale2mc.ecs.data.state.StateMachine
import com.hytale2mc.ecs.metrics.ExecutionNode
import com.hytale2mc.ecs.phase.Phase
import com.hytale2mc.ecs.platform.Platform
import com.hytale2mc.ecs.query.QueryAction
import com.hytale2mc.ecs.system.Condition
import com.hytale2mc.ecs.system.Executable
import com.hytale2mc.ecs.system.SystemBatch
import com.hytale2mc.ecs.system.SystemContainer
import com.hytale2mc.ecs.system.exec
import com.hytale2mc.ecs.data.state.State
import com.hytale2mc.ecs.plugin.ECSPlugin
import com.hytale2mc.ecs.plugin.PluginKey
import com.hytale2mc.eventstream.EventStream
import java.util.Random
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.reflect.KClass
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

typealias PlayerInitializer = (PlayerJoined) -> List<Component>
class Space(
    streamWriter: EventStream.Writer<DataOperation>,
    val streamReader: EventStream.Reader<DataOperation>,
    val origin: SpaceOrigin,
    val playerInitializer: PlayerInitializer,
    val executor: Executor
) {

    var maps = listOf<ECSMap>()
    var port = 0
    val eventWriters = mutableMapOf<KClass<out Event.Platform2Ecs>, Event.EventWriter<out Event.Platform2Ecs>>()
    var isReplaying: Boolean = false
    var cleanUp: () -> Unit = {}

    var tick = 0L

    var entityIdCounter = 0
    val entities = mutableMapOf<EntityId, Entity>()

    var replayEventLog: EventStream.Writer<DataOperation>? = null
    val streamLog = streamWriter

    val operations = ConcurrentLinkedQueue<Operation>()
    var onlinePlayers = 0

    fun <E : Event.Platform2Ecs> getEventWriter(type: KClass<E>): Event.EventWriter<E> {
        return eventWriters.getValue(type) as Event.EventWriter<E>
    }

    val random = Random(0)
    var updateCounter = 0L

    val states = StateContainer()
    val resources = TypedContainer<Resource>()
    val systems: SystemContainer = mutableMapOf()

    var plugins: Map<PluginKey<*>, ECSPlugin<*, *, *, *, *, *>> = emptyMap()

}

fun updateSpace(
    space: Space,
    platform: Platform<*, *>,
): ExecutionNode.Space {
    val spaceExec = ExecutionNode.Space(mutableListOf())

    fun executePhase(phase: Phase) {
        val (executedPhase, phaseDuration) = measureTimedValue {
            runPhaseSync(space, phase)
        }
        if (executedPhase == null) {
            // nothing was executed, no need to do anything
            return
        }

        executedPhase.duration = phaseDuration
        val commandFlush = flushOperations(space, platform)
        spaceExec.executions.add(executedPhase)
        spaceExec.executions.add(commandFlush)
        spaceExec.duration += phaseDuration + commandFlush.duration
        space.streamLog.flush()
    }

    if (space.updateCounter == 0L) {
        executePhase(Phase.StartUp)
    }
    executePhase(Phase.StateTransition)
    // State transitions
    for (state in space.states.contained()) {
        while (true) {
            val old = state.state
            val new = state.stateQueue.poll() ?: break
            fun <S : State> setStateUnsafe(machine: StateMachine<S>, newState: Any) {
                val old = machine.state
                machine.state = newState as S
                machine.previous = old
            }
            setStateUnsafe(state, new)
            executePhase(Phase.StateTransitions.Exit(old::class))
            executePhase(Phase.StateTransitions.Transition(old::class, new::class))
            executePhase(Phase.StateTransitions.Enter(new::class))
        }
    }
    executePhase(Phase.Before)
    executePhase(Phase.Update)
    executePhase(Phase.After)

    space.updateCounter++

    return spaceExec
}

private fun runPhaseSync(
    space: Space,
    phase: Phase
): ExecutionNode.Phase? {
    val systemBatches = space.systems[phase]
    if (systemBatches.isNullOrEmpty()) {
        return null
    }

    // DEBUG START
    val execution = ExecutionNode.Phase(phase, mutableListOf())
    // DEBUG END

    for (batch in systemBatches) {
        fun <R> exec(
            systemId: String,
            system: Executable<R>,
            systemExecution: ExecutionNode.System = ExecutionNode.System(systemId)
        ): Pair<R, ExecutionNode.System> {
            // we will use this tick as the running tick for the system
            val tick = space.tick++
            // delegate the system to the executor
            val result = measureTimedValue {
                exec(tick, space, system)
            }
            system.lastRun = tick
            // DEBUG START
            systemExecution.status = ExecutionNode.SystemStatus.Completed(Thread.currentThread().name, result.duration)
            // DEBUG END
            return result.value to systemExecution
        }
        val batchExecution = ExecutionNode.SystemBatch(batch.id, mutableListOf())
        execution.batches.add(batchExecution)
        when (val condition = batch.condition) {
            null -> {}
            else -> {
                val (result, execNode) = exec("run_if", condition)
                if (!result) {
                    batchExecution.status = ExecutionNode.BatchStatus.ConditionFailed(execNode)
                    continue
                }
            }
        }
        for ((systemId, system) in batch.systems) {
            val condition = system.condition
            val systemExec = ExecutionNode.System(systemId)
            batchExecution.systems.add(systemExec)
            if (condition != null) {
                val (result, execNode) = exec("run_if", condition, systemExec)
                if (!result) {
                    continue
                }
            }
            exec(systemId, system, systemExec)
        }
        batchExecution.status = ExecutionNode.BatchStatus.Completed
    }
    return execution
}

private fun runPhaseAsync(
    space: Space,
    phase: Phase
): ExecutionNode.Phase? {
    val systemBatches = space.systems[phase]
        ?.takeUnless { it.isEmpty() }
        ?.map { RunningSystemBatch(it, -1) }
        ?: return null
    // DEBUG START
    val execution = ExecutionNode.Phase(phase, mutableListOf())
    systemBatches.forEach { execution.batches.add(it.execution) }
    // DEBUG END
    val futures = mutableMapOf<RunningSystemBatch, CompletableFuture<Executable<*>>>()
    val reading = mutableListOf<KClass<out Any>>()
    val writing = mutableListOf<KClass<out Any>>()
    val evaluatingConditions = mutableMapOf<Executable<*>, CompletableFuture<Boolean>>()
    val evaluatedConditions = mutableMapOf<Executable<*>, Boolean>()

    fun getBlockList(action: QueryAction<*>) = when (action) {
        is QueryAction.Read<*> -> reading
        is QueryAction.Write<*> -> writing
    }
    fun isBlocked(system: Executable<*>): Boolean {
        if (system in evaluatingConditions) return true
        for (action in system.allActions) {
            if (action.type in getBlockList(action)) return true
        }
        return false
    }

    fun block(
        batch: RunningSystemBatch,
        system: Executable<*>
    ) {
        // make sure the batch has already been blocked
        check(!futures.containsKey(batch))
        for (action in system.allActions) {
            getBlockList(action).add(action.type)
        }
    }

    fun unblock(
        batch: RunningSystemBatch,
        system: Executable<*>
    ) {
        // make sure the batch has already been unblocked
        check(!futures.containsKey(batch))
        for (action in system.allActions) {
            getBlockList(action).remove(action.type)
        }
    }


    fun tryRunCondition(
        runningBatch: RunningSystemBatch,
        system: Executable<*>,
        condition: Condition,
    ): Boolean? {
        return when (evaluatedConditions[system]) {
            true -> {
                // allow system to proceed
                true
            }
            false -> {
                // evaluation failed, this system will not run

                false
            }
            null -> {
                // evaluation has either not finished or has not started
                if (evaluatingConditions.containsKey(system)) {
                    // already evaluating
                    return null
                }
                if (isBlocked(condition)) {
                    // we can't check the condition yet due to it being blocked
                    return null
                }
                // DEBUG START
                val executionBatch = ExecutionNode.System("run_if")
                runningBatch.currentConditionExecution = executionBatch
                // DEBUG END
                // we will evaluate it now
                val tick = space.tick++
                val future = CompletableFuture.supplyAsync {
                    val result = measureTimedValue {
                        exec(tick, space, condition)
                    }
                    system.lastRun = tick
                    // DEBUG START
                    executionBatch.status = ExecutionNode.SystemStatus.Completed(Thread.currentThread().name, result.duration)
                    // DEBUG END
                    result.value
                }
                evaluatingConditions[system] = future
                null
            }
        }
    }


    while (true) {
        // perform unblocks for all resources that were used by the completed queries
        futures.removeCompleted { batch, query -> unblock(batch, query) }

        // allow queries whose conditions have been evaluated to continue, or remove them if the condition failed
        evaluatingConditions.removeCompleted { system, passed ->
            evaluatedConditions[system] = passed
        }

        var runningSystems = 0
        var ranSystems = 0

        for (runningBatch in systemBatches) {
            if (futures.containsKey(runningBatch)) {
                // this batch is busy, it will become free when the current system is finished
                runningSystems++
                continue
            }
            val batch = runningBatch.batch
            if (runningBatch.index == -1) {
                // first batch cycle
                // we need to check for the condition
                when (val condition = batch.condition) {
                    null -> runningBatch.index++
                    else -> when (tryRunCondition(runningBatch, condition, condition)) {
                        true -> {
                            // allow the batch to start
                            runningBatch.index++
                        }
                        false -> {
                            // we won't increment the index
                            // which means the batch will never start
                            runningBatch.execution.status = ExecutionNode.BatchStatus.ConditionFailed(runningBatch.currentConditionExecution!!)
                            continue
                        }
                        null -> {
                            runningSystems++
                            continue
                        }
                    }
                }
            }
            val (systemId, system) = batch.systems.getOrNull(runningBatch.index) ?: run {
                // this batch has finished executing all its systems
                // DEBUG START
                runningBatch.execution.status = ExecutionNode.BatchStatus.Completed
                // DEBUG END
                continue
            }
            runningSystems++

            // check the condition
            if (system.condition != null) {
                val condition = system.condition!!
                when (tryRunCondition(runningBatch, system, condition)) {
                    true -> {
                        // allow the system to proceed
                    }
                    false -> {
                        // evaluation failed, this system will not run
                        val runningBatchIndex = runningBatch.index++
                        ranSystems++
                        // DEBUG START
                        runningBatch.execution.systems[runningBatchIndex].status = ExecutionNode.SystemStatus.ConditionFailed(runningBatch.currentConditionExecution!!)
                        // DEBUG END
                        continue
                    }
                    null -> {
                        continue
                    }
                }
            }

            if (isBlocked(system)) {
                // system is blocked, try again next tick
                continue
            }

            // at this point we are sure we are running the system
            // first we block it
            block(runningBatch, system)
            // we will use this tick as the running tick for the system
            val tick = space.tick++
            // DEBUG START
            val execSystem = runningBatch.execution.systems[runningBatch.index]
            // DEBUG END
            // delegate the system to the executor
            val future = CompletableFuture.supplyAsync<Executable<*>>(
                {
                    val result = measureTimedValue {
                        exec(tick, space, system)
                    }
                    system.lastRun = tick
                    // DEBUG START
                    execSystem.status = ExecutionNode.SystemStatus.Completed(Thread.currentThread().name, result.duration)
                    // DEBUG END
                    system
                },
                space.executor
            )
            futures[runningBatch] = future
            runningBatch.index++
            ranSystems++
        }

        if (runningSystems == 0) {
            // everything finished
            break
        }
        if (ranSystems == 0) {
            // everything is blocked, wait until at least 1 task is completed
            // hoping that some component gets unblocked
            CompletableFuture.anyOf(*(futures.values + evaluatingConditions.values).toTypedArray()).join()
        }
    }
    // no need to wait for evaluatingConditions here because this point
    // is unreachable if evaluatingConditions aren't empty
    CompletableFuture.allOf(*(futures.values).toTypedArray()).join()
    return execution
}

private class RunningSystemBatch(
    val batch: SystemBatch,
    var index: Int
) {
    val execution = ExecutionNode.SystemBatch(
        batch.id,
        batch.systems.mapTo(mutableListOf()) { ExecutionNode.System(it.first) },
    )

    var currentConditionExecution: ExecutionNode.System? = null

}

private inline fun <K, T> MutableMap<K, CompletableFuture<T>>.removeCompleted(
    onRemove: (K, T) -> Unit
) {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val (query, future) = iterator.next()
        val value = future.getNow(null) ?: continue
        iterator.remove()
        onRemove.invoke(query, value)
    }
}

fun flushOperations(
    space: Space,
    platform: Platform<*, *>
): ExecutionNode.CommandFlush {
    val execFlush = ExecutionNode.CommandFlush(mutableListOf())
    val operations = mutableListOf<Operation>()
    val removals = mutableListOf<DataCommand.RemoveEntities>()

    while (true) {
        val operation = space.operations.poll() ?: break
        when (operation) {
            is DataCommand.RemoveEntities -> removals.add(operation)
            else -> operations.add(operation)
        }
    }

    for (operation in (operations + removals)) {
        if (operation is ComponentMutation && operation.new is Component.EcsOnly) {
            // no one needs to know about this component as it is fully
            // managed by the ECS
            continue
        }
        if (operation is EventWritten) {
            val event = operation.event
            check(event !is Event.EcsOnly) { event }
            when (event) {
                is Event.Platform2Ecs -> {
                    // we must be a primary
                    when (space.origin) {
                        PRIMARY -> {
                            // we need to let the ECS know about this event
                            fun <E : Event.Platform2Ecs> writeUnsafe(writer: Event.EventWriter<E>, event: Event) {
                                writer.reader.events.add(event as E)
                            }
                            writeUnsafe(space.getEventWriter(event::class), event)
                            space.replayEventLog?.write(operation)
                            continue
                        }
                        REPLICA -> {
                            space.streamLog.write(operation)
                            continue
                        }
                    }
                }
                is Event.Ecs2Platform -> {
                    // if we are a primary we need to let replicas know about this event, as well as
                    // handle it on our own platform
                    if (space.origin == PRIMARY) {
                        space.streamLog.write(operation)
                    }
                }
            }
        }
        val duration = measureTime { platform.handle(operation as DataOperation) }
        when (operation) {
            is Command<*> -> {
                execFlush.commands.add(
                    ExecutionNode.Command(
                        systemId = "unknown",
                        commandId = operation.commandId,
                        duration = duration
                    )
                )
                execFlush.duration += duration

                if (space.origin == PRIMARY) {
                    space.streamLog.write(operation)
                }
            }
            // save the event for the replay if it is coming from a platform
            is DataEvent -> {
                when {
                    operation is EventWritten -> {
                        if (operation.event is Event.Platform2Ecs) {
                            space.replayEventLog?.write(operation)
                        }
                    }
                    operation !is ComponentMutation || operation.source is DataOperation.Source.Platform -> {
                        space.replayEventLog?.write(operation)
                    }
                }
            }
        }
    }

    return execFlush
}
