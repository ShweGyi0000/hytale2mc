package com.hytale2mc.ecs.data.component

import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class TransformComponent(
    @Contextual
    val translation: Vector3F,
    @Contextual
    val direction: Vector3F = Vector3F(0f, 0f, 1f),
    @Contextual
    val scale: Vector3F = Vector3F(1f, 1f, 1f),
) : Component.BiDirectional()