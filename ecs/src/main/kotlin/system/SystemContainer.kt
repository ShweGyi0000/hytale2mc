package com.hytale2mc.ecs.system

import com.hytale2mc.ecs.phase.Phase

internal typealias SystemContainer = MutableMap<Phase, MutableList<SystemBatch>>