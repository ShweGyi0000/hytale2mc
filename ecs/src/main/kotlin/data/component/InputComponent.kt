package com.hytale2mc.ecs.data.component

import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class InputComponent(
    val isOnGround: Boolean = false,
    val sneaking: Boolean = false,
    val jumping: Boolean = false,
    val rightClicking: Boolean = false,
    val movement: @Contextual Vector3F = Vector3F.ZERO,
    val forward: Boolean = false,
    val backward: Boolean = false,
    val selectedInventorySlot: Int = 0,
): Component.Platform2EcsOnly()