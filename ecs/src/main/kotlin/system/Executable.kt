package com.hytale2mc.ecs.system

import com.hytale2mc.ecs.data.LocalDelegate
import com.hytale2mc.ecs.data.Local
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.operation.DataCommand
import com.hytale2mc.ecs.data.operation.Operation
import com.hytale2mc.ecs.query.Query
import com.hytale2mc.ecs.query.ExecutionContext
import kotlinx.atomicfu.atomic

typealias Condition = Executable<Boolean>

private var counter = atomic(0)

data class Executable<R>(
    internal val queries: List<Query>,
    internal val exec: ExecutionContext.() -> R
) {

    internal val id = counter.getAndIncrement()
    internal var lastRun = -1L
    var condition: Condition? = null
    var isCondition: Boolean = false

    internal val allActions = queries.flatMap { it.allActions }

    internal val locals = mutableMapOf<String, LocalDelegate<*>>()

}

fun <T> ExecutionContext.local(
    initialValue: T
) : Local<T> {
    return Local(executable, initialValue)
}

fun ExecutionContext.queueCommand(
    command: Operation,
) {
    space.operations.add(command)
}

fun ExecutionContext.queueSpawnEntity(
    commandId: String,
    components: List<Component>
) {
    queueCommand(
        DataCommand.AddEntities(
            commandId,
            listOf(components)
        )
    )
}