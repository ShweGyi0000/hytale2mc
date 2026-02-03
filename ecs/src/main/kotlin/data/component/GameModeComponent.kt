package com.hytale2mc.ecs.data.component

import kotlinx.serialization.Serializable

@Serializable
data class GameModeComponent(
    val gameMode: GameMode
) : Component.Ecs2PlatformOnly()

enum class GameMode {
    SURVIVAL,
    SPECTATOR
}