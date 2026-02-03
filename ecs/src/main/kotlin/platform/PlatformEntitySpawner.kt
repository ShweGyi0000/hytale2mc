package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.PlatformEntityId

data class PlatformEntitySpawner(
    val platformEntityId: PlatformEntityId,
    val properties: EntityProperties?,
    val spawn: () -> Unit
)