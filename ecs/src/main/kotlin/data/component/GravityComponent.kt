package com.hytale2mc.ecs.data.component

import kotlinx.serialization.Serializable

@Serializable
data class GravityComponent(
    val multiplier: Double = 1.0
) : Component.Ecs2PlatformOnly()