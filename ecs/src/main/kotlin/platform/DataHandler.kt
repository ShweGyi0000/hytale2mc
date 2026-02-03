package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.*
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.component.EntityTypeComponent
import com.hytale2mc.ecs.data.component.componentId
import com.hytale2mc.ecs.data.operation.ComponentMutation
import com.hytale2mc.ecs.data.operation.DataCommand
import com.hytale2mc.ecs.data.operation.DataOperation
import com.hytale2mc.ecs.space.DataOperations
import com.hytale2mc.ecs.space.Space

internal data class DataHandler(val platform: Platform<*, *>) {

    private fun createComponentHandlerContext(
        platform: Platform<*, *>,
        space: Space,
        mutation: ComponentMutation,
    ): PlatformComponentHandlerContext {
        return PlatformComponentHandlerContext(
            space,
            space.entities.getValue(mutation.entityId),
            platform.entityId2PlatformEntityId.getValue(mutation.entityId)
        )
    }

    internal fun handleMutation(
        platform: Platform<*, *>,
        space: Space,
        mutation: ComponentMutation
    ) {
        val source = mutation.source
        val new = mutation.new
        when (space.origin) {
            PRIMARY -> {
                // we are a primary
                when (mutation.source) {
                    DataOperation.Source.ECS -> {
                        // this mutation was already performed and streamed but not handled
                        // we only need to handle it on the platform
                        check(mutation.origin == PRIMARY)
                        check(new is Component.Ecs2Platform) { "$new" }
                        platform.componentHandler.handleMutationFromEcs(
                            createComponentHandlerContext(platform, space, mutation),
                            new
                        )
                    }
                    is DataOperation.Source.Platform -> {
                        when (mutation.origin) {
                            PRIMARY -> {
                                // this mutation is coming from our platform, so it is already handled (unless we're replaying)
                                // we need to replace and stream it to replicas
                                DataOperations.replaceComponent(
                                    space,
                                    mutation,
                                    stream = true
                                )
                                
                                if (space.isReplaying && new is Component.HandledByPlatform) {
                                    platform.componentHandler.handleMutationFromEcs(
                                        createComponentHandlerContext(platform, space, mutation),
                                        new
                                    )
                                }
                            }
                            REPLICA -> {
                                // this mutation is coming from a replica
                                // we need to attempt to replace it and handle it on the platform if it succeeds
                                val replaced = DataOperations.replaceComponent(space, mutation, stream = true)
                                if (replaced && new is Component.HandledByPlatform) {
                                    platform.componentHandler.handleMutationFromEcs(
                                        createComponentHandlerContext(platform, space, mutation),
                                        new
                                    )
                                }
                            }
                        }


                    }
                }
            }
            REPLICA -> {
                // we are a replica
                when (source) {
                    DataOperation.Source.ECS -> {
                        // this mutation is coming from the primary, we need to perform it and handle but not stream it
                        DataOperations.replaceComponent(space, mutation, stream = false)
                        if (new is Component.Ecs2Platform) {
                            platform.componentHandler.handleMutationFromEcs(
                                createComponentHandlerContext(platform, space, mutation),
                                new
                            )
                        }
                    }
                    is DataOperation.Source.Platform -> {
                        // this mutation could be coming from ourselves
                        when (mutation.origin) {
                            // if it is coming from a replica, it means that it is coming from us,
                            // it is already handled, so we need to replace it and stream it
                            REPLICA -> {
                                check(new is Component.Platform2Ecs) { new }
                                check(mutation.source is DataOperation.Source.Platform)
                                DataOperations.replaceComponent(space, mutation, stream = true)
                            }
                            // if it is coming from the primary, we will perform the mutation only if the source is not us
                            PRIMARY -> {
                                if (source.platformId != platform.platformId) {
                                    // this mutation is coming from a different replica
                                    check(new is Component.Platform2Ecs) { new }
                                    // other replicas aren't allowed to modify components of entities we own
                                    check(platform.entityId2PlatformEntityId.getValue(mutation.entityId) !in platform.owningPlayersPlatformIds)
                                    DataOperations.replaceComponent(space, mutation, stream = false)
                                    platform.componentHandler.handleMutationFromEcs(
                                        createComponentHandlerContext(platform, space, mutation),
                                        new
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    internal fun handleDataCommand(
        platform: Platform<*, *>,
        space: Space,
        command: DataCommand<*>
    ) {
        when (command) {
            is DataCommand.AddComponents -> {
                for ((entityId, components) in command.components) {
                    val platformEntityId = platform.entityId2PlatformEntityId.getValue(entityId)
                    val entity = space.entities.getValue(entityId)
                    val context = PlatformComponentHandlerContext(
                        space,
                        entity,
                        platformEntityId
                    )
                    for (componentId in components) {
                        addComponent(context, componentId)
                    }
                }
            }
            is DataCommand.AddEntities -> {
                for (components in command.components) {
                    val entity = DataOperations.addEntity(space)
                    spawnPlatformEntity(entity, platform, space, components)
                }
            }
            is DataCommand.RemoveComponents -> {
                for ((entityId, componentIds) in command.components) {
                    val entity = space.entities.getValue(entityId)
                    val platformEntityId = platform.entityId2PlatformEntityId.getValue(entityId)
                    val context = PlatformComponentHandlerContext(
                        space,
                        entity,
                        platformEntityId
                    )
                    for (componentId in componentIds) {
                        removeComponent(context, componentId)
                    }
                }
            }
            is DataCommand.RemoveEntities -> {
                for (entityId in command.entities) {
                    val platformEntityId = platform.entityId2PlatformEntityId[entityId] ?: continue
                    val entity = space.entities.getValue(entityId)
                    val componentsToRemove = entity.components.keys.toList()
                    for (componentId in componentsToRemove) {
                        val entity = space.entities.getValue(entityId)
                        val platformEntityId = platform.entityId2PlatformEntityId.getValue(entityId)
                        val context = PlatformComponentHandlerContext(
                            space,
                            entity,
                            platformEntityId
                        )
                        removeComponent(context, componentId)
                    }

                    // remove the platform entity
                    platform.removePlatformEntity(entityId)

                    // remove the space <-> platform mapping
                    platform.entityId2PlatformEntityId.remove(entityId)
                    platform.platformEntityId2EntityId.remove(platformEntityId)
                    platform.originalProperties.remove(entityId)

                    // remove the entity from the space
                    DataOperations.removeEntity(space, entityId)
                }
            }
            is DataCommand.TransferPlayers -> {
                for ((entityId, destination) in command.transfers) {
                    val platformEntityId = platform.entityId2PlatformEntityId.getValue(entityId)
                    if (platformEntityId !in platform.owningPlayersPlatformIds) {
                        continue
                    }
                    platform.transfer(platformEntityId, destination)
                }
            }
        }
    }

    private fun spawnPlatformEntity(
        entity: Entity,
        platform: Platform<*, *>,
        space: Space,
        components: List<Component>,
    ) {
        // find a player component if it's present
        val entityTypeComponent = components.find { it is EntityTypeComponent } as? EntityTypeComponent

        // tell the platform to create the entity and give us its platform id
        val platformEntitySpawner = platform.spawnPlatformEntity(entity.id, entityTypeComponent?.type)
        val platformEntityId = platformEntitySpawner.platformEntityId
        if (platformEntitySpawner.properties != null) {
            platform.originalProperties[entity.id] = platformEntitySpawner.properties
        }

        // make sure to save the space <-> platform id mappings
        platform.entityId2PlatformEntityId[entity.id] = platformEntityId
        platform.platformEntityId2EntityId[platformEntityId] = entity.id

        // call all platform component handlers for the components that this entity has
        val context = PlatformComponentHandlerContext(
            space,
            entity,
            platformEntityId
        )
        val absentComponents = platform.componentHandler.handlersById.toMutableMap()
        val handleAfterSpawn = mutableListOf<Component>()
        for (component in components) {
            if (component is Component.HandleAfterSpawn) {
                handleAfterSpawn.add(component)
            } else {
                addComponent(context, component)
            }
            absentComponents.remove(component::class.componentId)
        }
        for (handler in absentComponents) {
            handler.value.onRemovedOrNotPresentOnSpawn(context)
        }
        platformEntitySpawner.spawn()
        for (component in handleAfterSpawn) {
            addComponent(context, component)
        }
    }

    private fun addComponent(
        context: PlatformComponentHandlerContext,
        component: Component
    ) {
        DataOperations.addComponent(context.space, context.entity, component)
        if (component is Component.HandledByPlatform) {
            platform.componentHandler.handleAddition(context, component)
        }
    }

    private fun removeComponent(
        context: PlatformComponentHandlerContext,
        componentId: DataId
    ) {
        DataOperations.removeComponent(context.entity, componentId)
        platform.componentHandler.handleRemovalOrNotPresentOnSpawn(context, componentId)
    }

}