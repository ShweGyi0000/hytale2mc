package com.hytale2mc.ecs.platform.hytale

import com.hypixel.hytale.component.*
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.EcsEvent
import com.hypixel.hytale.component.system.EntityEventSystem
import com.hypixel.hytale.component.system.ICancellableEcsEvent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

inline fun <reified  E : EcsEvent> ComponentRegistryProxy<EntityStore>.restrict() {
    val system = object : EntityEventSystem<EntityStore, E>(E::class.java) {
        override fun handle(
            p0: Int,
            p1: ArchetypeChunk<EntityStore?>,
            p2: Store<EntityStore?>,
            p3: CommandBuffer<EntityStore?>,
            p4: E
        ) {
            if (p4 is ICancellableEcsEvent) {
                p4.isCancelled = true
            }
        }

        override fun getQuery(): Query<EntityStore?>? {
            return Archetype.empty()
        }

    }
    registerSystem(system)
}
