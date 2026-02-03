package com.hytale2mc.ecs.composer

import com.hytale2mc.ecs.phase.Phase
import com.hytale2mc.ecs.system.Condition
import com.hytale2mc.ecs.system.Executable
import com.hytale2mc.ecs.system.SystemBatch
import com.hytale2mc.ecs.system.exec

typealias ComposedSystems = Map<Phase, List<SystemBatch>>

@ComposerDsl
class BatchComposer internal constructor() {

    internal val batches = mutableMapOf<Phase, MutableList<SystemBatch>>()

    fun batch(id: String, phase: Phase, block: ComposingBatch.() -> Unit): SystemBatch {
        val composed = ComposingBatch(id).apply(block).compose()
        batch(composed, phase)
        return composed
    }

    fun batch(batch: SystemBatch, phase: Phase) {
        batches.getOrPut(phase) { mutableListOf() }.add(batch)
    }

    fun system(id: String, phase: Phase, system: Executable<*>): SystemBatch {
        return batch(id, phase) {
            system(id, system)
        }
    }

    fun xsystem(id: String, phase: Phase, system: Executable<*>): SystemBatch {
        return batch(id, phase) {}
    }

    fun SystemBatch.runIf(condition: Condition) {
        this.condition = condition
    }

    internal fun compose(): ComposedSystems {
        return batches
    }

}

@ComposerDsl
class ComposingBatch internal constructor(
    val batchId: String
) {

    internal val systems = mutableListOf<Pair<String, Executable<*>>>()

    fun system(id: String, system: Executable<*>): Executable<*> {
        systems.add(id to system)
        return system
    }

    fun xsystem(id: String, system: Executable<*>): Executable<*> {
        systems.add(id to exec {})
        return system
    }

    fun Executable<*>.runIf(condition: Condition) {
        check(!this.isCondition)
        this.condition = condition
        condition.isCondition = true
    }

    internal fun compose(): SystemBatch {
        return SystemBatch(batchId, systems, null)
    }

}