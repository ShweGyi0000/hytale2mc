package com.hytale2mc.ecs.platform.sync

import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.component.EntityTypeComponent
import com.hytale2mc.ecs.data.component.componentId
import com.hytale2mc.ecs.platform.Platform
import com.hytale2mc.ecs.platform.PlatformComponentHandlerContext

fun performNeededPlatformToEcsMutations(
    platform: Platform<*, *>,
) {
    val space = platform.space
    for (entity in space.entities.values) {
        val platformEntityId = platform.entityId2PlatformEntityId.getValue(entity.id)
        val entityType = entity.components[EntityTypeComponent::class.componentId] as? EntityTypeComponent

        // if we are a replica we can ignore non players
        if (platform.space.origin == REPLICA && (entityType == null || !entityType.type.isRealPlayer)) {
            continue
        }

        // we ignore players that we don't own
        if (entityType?.type?.isRealPlayer == true && platformEntityId !in platform.owningPlayersPlatformIds) {
            // we don't own this player
            continue
        }

        val context by lazy(LazyThreadSafetyMode.NONE) {
            PlatformComponentHandlerContext(
                space,
                entity,
                platform.entityId2PlatformEntityId.getValue(entity.id)
            )
        }
        for ((_, component) in entity.components.toList()) {
            if (component !is Component.Platform2Ecs) {
                continue
            }
            platform.componentHandler.mutateFromPlatformIfNeeded(context, component)
        }
    }
}