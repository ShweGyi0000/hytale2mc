package com.hytale2mc.ecs.data.component

import korlibs.math.geom.Quaternion
import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class LocalTransformComponent(
    @Contextual
    val translation: Vector3F = Vector3F(0f, 0f, 0f),
    @Contextual
    val rotation: Quaternion = Quaternion.IDENTITY,
    @Contextual
    val scale: Vector3F = Vector3F(1f, 1f, 1f),
) : Component.Ecs2PlatformOnly()