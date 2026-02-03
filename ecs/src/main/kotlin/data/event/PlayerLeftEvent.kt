package com.hytale2mc.ecs.data.event

import com.hytale2mc.ecs.platform.PlatformType
import kotlinx.serialization.Serializable

@Serializable
data class PlayerLeftEvent(
    val username: String,
    val platformType: PlatformType,
) : Event.Platform2Ecs()