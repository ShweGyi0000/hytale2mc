package com.hytale2mc.ecs.data.component

import com.hytale2mc.ecs.data.operation.DataCommand
import kotlinx.serialization.Serializable

@Serializable
data class FallbackServerComponent(
    val destination: DataCommand.TransferPlayers.Destination
) : Component.Ecs2PlatformOnly()