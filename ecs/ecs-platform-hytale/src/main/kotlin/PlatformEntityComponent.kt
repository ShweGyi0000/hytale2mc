package com.hytale2mc.ecs.platform.hytale

import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.protocol.MovementStates
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hytale2mc.ecs.data.PlatformEntityId

data class PlatformEntityComponent(
    val id: PlatformEntityId,
    var movementStates: MovementStates,
    var moved: Boolean = false
) : Component<EntityStore?> {

    companion object {
        lateinit var type: ComponentType<EntityStore?, PlatformEntityComponent>
    }

    override fun clone(): Component<EntityStore?> {
        return copy()
    }

}