package com.hytale2mc.ecs.query

import com.hytale2mc.ecs.space.Space
import com.hytale2mc.ecs.system.Executable
import com.hytale2mc.ecs.system.ExecutionDsl

@ExecutionDsl
data class ExecutionContext(
    override val space: Space,
    override val tick: Long,
    internal val executable: Executable<*>
) : TickContext {

}

interface TickContext {

    val space: Space
    val tick: Long

}