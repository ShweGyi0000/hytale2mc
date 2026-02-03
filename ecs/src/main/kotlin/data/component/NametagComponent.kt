package com.hytale2mc.ecs.data.component

import com.github.ajalt.colormath.model.RGB
import com.hytale2mc.ecs.text.Text
import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class NametagComponent(
    val text: Text,
    @Contextual
    val backgroundColor: RGB = RGB(0f, 0f, 0f, 0f),
    @Contextual
    val localTranslation: Vector3F = Vector3F.ZERO,
    @Contextual
    val scale: Vector3F = Vector3F.ONE,
    val renderThroughWalls: Boolean = false
) : Component.Ecs2PlatformOnly(), Component.HandleAfterSpawn