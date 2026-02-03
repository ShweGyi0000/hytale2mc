package com.hytale2mc.ecs.platform.hytale

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.SystemGroup
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.EntityTickingSystem
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems.Visible
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hytale2mc.ecs.data.component.InventoryComponent
import com.hytale2mc.ecs.data.get

class ECSInventorySystem(
    val plugin: HytalePlatformPlugin
) : EntityTickingSystem<EntityStore>() {

    private val _query = Query.and(Visible.getComponentType(), PlatformEntityComponent.type)

    override fun getQuery(): Query<EntityStore?> {
        return _query
    }

    override fun getGroup(): SystemGroup<EntityStore?>? {
        return EntityTrackerSystems.QUEUE_UPDATE_GROUP
    }

    override fun tick(
        dt: Float,
        index: Int,
        chunk: ArchetypeChunk<EntityStore?>,
        store: Store<EntityStore?>,
        p4: CommandBuffer<EntityStore?>
    ) {
        val ref = chunk.getReferenceTo(index)
        val visible = chunk.getComponent(index, Visible.getComponentType()) ?: return
        val platformEntityComponent = chunk.getComponent(index, PlatformEntityComponent.type) ?: return

        val entityId = plugin.platform.platformEntityId2EntityId[platformEntityComponent.id] ?: return
        val entity = plugin.platform.space.entities[entityId] ?: return
        val inventory = entity[InventoryComponent::class] ?: return
        val inventoryUpdate = plugin.platform.componentHandler.getInventoryComponentUpdates(entity, inventory)
        for (newViewer in visible.newlyVisibleTo) {
            newViewer.value.queueUpdate(
                ref,
                inventoryUpdate
            )
        }
    }
}