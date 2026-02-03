package com.hytale2mc.ecs.system

class SystemBatch(
    val id: String,
    internal val systems: List<Pair<String, Executable<*>>>,
    var condition: Condition?
)