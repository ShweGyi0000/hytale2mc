package com.hytale2mc.ecs.space

import com.hytale2mc.ecs.data.Entity
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.component.componentId
import com.hytale2mc.ecs.data.operation.ComponentMutation
import com.hytale2mc.ecs.data.operation.DataOperation

object DataOperations {

    fun addEntity(
        space: Space,
    ): Entity {
        val id = space.entityIdCounter++
        val entity = Entity(id)
        check(space.entities.put(id, entity) == null)
        return entity
    }

    fun removeEntity(
        space: Space,
        id: EntityId
    ) {
        check(space.entities.remove(id) != null)
    }

    fun addComponent(
        space: Space,
        entity: Entity,
        component: Component
    ) {
        check(entity.components.put(Component.Registry.getValue(component::class), component) == null) { "Tried to add $component to $entity but it already had it"}
        component.addedAt = space.tick
    }

    fun removeComponent(
        entity: Entity,
        componentId: String
    ): Component {
        val removed = entity.components.remove(componentId)
        check(removed != null) { "Tried to remove $componentId from $entity but it didn't have it" }
        return removed
    }

    fun replaceComponent(
        space: Space,
        mutation: ComponentMutation,
        stream: Boolean = true,
    ): Boolean {
        val entity = space.entities[mutation.entityId] ?: return false
        checkNotNull(entity) { "${mutation.entityId} !in ${space.entities.keys}" }
        val old = removeComponent(entity, mutation.new::class.componentId)
        val new = mutation.new
        addComponent(space, entity, new)
        new.addedAt = old.addedAt
        new.changedAt = space.tick
        val mutation = when (space.origin) {
            PRIMARY -> {
                // we are a primary
                when (mutation.origin) {
                    PRIMARY -> {
                        // the mutation is coming from a primary (us)
                        // which means we will increment the version and ignore
                        // future replacements of this component coming from replicas
                        // until they update the version to match the new version
                        if (new is Component.BiDirectional) {
                            val newVersion = (entity.componentVersions[new::class.componentId] ?: 0) + 1
                            entity.componentVersions[new::class.componentId] = newVersion
                            mutation.copy(version = newVersion)
                        } else {
                            mutation
                        }.also {
                            if (mutation.source is DataOperation.Source.ECS) {
                                // we can't handle it on the platform from here
                                // we will add it to the event queue, and it will be handled later
                                space.operations.add(mutation)
                            }
                        }
                    }
                    REPLICA -> {
                        // the mutation is coming from a replica
                        // we need to check the version and discard the mutation
                        // if the versions don't match
                        val currentVersion = entity.componentVersions[new::class.componentId]
                        if (currentVersion != null && currentVersion > mutation.version) {
                            // replica has an outdated component version
                            // we will ignore the mutations until it updates the version
                            return false
                        }
                        mutation.copy(origin = PRIMARY)
                    }
                }
            }
            REPLICA -> {
                // we are a replica
                if (new is Component.Platform2Ecs) {
                    if (mutation.origin == REPLICA) {
                        mutation.copy(version = entity.componentVersions[new::class.componentId] ?: 0L)
                    } else {
                        entity.componentVersions[new::class.componentId] = mutation.version
                        mutation
                    }
                } else {
                    mutation
                }
            }
        }
        if (stream && mutation.new !is Component.EcsOnly) {
            space.streamLog.write(mutation)
        }
        return true
    }

}