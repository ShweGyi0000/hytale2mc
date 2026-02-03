package com.hytale2mc.ecs.system

import com.hytale2mc.ecs.query.ExecutionContext
import com.hytale2mc.ecs.query.prepareQuery
import com.hytale2mc.ecs.space.Space

internal fun <R> exec(
    tick: Long,
    space: Space,
    executable: Executable<R>
): R {
    val context = ExecutionContext(space, tick, executable)
    for (query in executable.queries) {
        prepareQuery(space, executable, context, query)
    }
    return executable.exec.invoke(context)
}