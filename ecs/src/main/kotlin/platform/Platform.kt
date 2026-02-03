package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.*
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.component.FallbackServerComponent
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.event.Event
import com.hytale2mc.ecs.data.operation.*
import com.hytale2mc.ecs.metrics.ExecutionNode
import com.hytale2mc.ecs.platform.sync.catchUpAsReplica
import com.hytale2mc.ecs.platform.sync.waitForPrimary
import com.hytale2mc.ecs.plugin.ECSPlugin
import com.hytale2mc.ecs.plugin.PluginKey
import com.hytale2mc.ecs.plugin.PluginPlatformHandler
import com.hytale2mc.ecs.space.Space
import com.hytale2mc.ecs.space.flushOperations
import java.util.*
import kotlin.time.Duration

typealias EntityProperties = Map<EntityProperty.Key<*>, EntityProperty<*>>

abstract class Platform<P_E : Any, P_I : Any>(
    platformType: PlatformType,
    val space: Space,
) {

    init {
        for (plugin in space.plugins) {
            check(plugin.value.platformHandlers.containsKey(platformType)) { "Plugin ${plugin.key} does not contain a platform handler for $platformType" }
        }
    }

    val platformId = UUID.randomUUID().toString()

    val source = DataOperation.Source.Platform(platformType, platformId)
    val owningPlayersPlatformIds = mutableSetOf<PlatformEntityId>()
    val entityId2PlatformEntityId = mutableMapOf<EntityId, PlatformEntityId>()
    val platformEntityId2EntityId = mutableMapOf<PlatformEntityId, EntityId>()

    abstract val componentHandler: PlatformComponentHandler<*>
    abstract val eventHandler: PlatformEventHandler<*>
    val renderer: PlatformRenderer = PlatformRenderer(this)

    fun start() {
        renderMaps()
        when (space.origin) {
            REPLICA -> {
                waitForPrimary(space)
                catchUpAsReplica(this, space)
            }
            PRIMARY -> {
                // check if we are replaying
                val messages = space.streamReader.read(1)
                val primaryStarted = when {
                    messages.isEmpty() -> {
                        PrimaryStarted(false)
                    }
                    messages.size == 1 -> {
                        val message = messages.single()
                        check(message is PrimaryStarted) { "Invalid first message: $message" }
                        space.isReplaying = true
                        message.copy(replaying = true)
                    }
                    else -> throw IllegalStateException("Invalid first messages: $messages")
                }
                space.operations.add(primaryStarted)
            }
        }
        onStart()
    }

    fun update(): ExecutionNode.Space? {
        return when (space.origin) {
            PRIMARY -> {
                updateSpaceAsPrimary(this, space)
            }
            REPLICA -> {
                updateSpaceAsAReplica(this)
                null
            }
        }
    }

    protected abstract fun onStart()

    private val dataHandler = DataHandler(this)

    fun handle(operation: DataOperation) {
        when (operation) {
            is DataCommand<*> -> dataHandler.handleDataCommand(this, space, operation)
            is ComponentMutation -> dataHandler.handleMutation(this, space, operation)
            is PlayerJoined -> PlayerLifecycle.handlePlayerJoinedEvent(space, operation)
            is PlayerLeft -> PlayerLifecycle.handlePlayerLeftEvent(this, operation)
            is RenderingCommand -> renderer.render(operation)
            is EventWritten -> {
                val event = operation.event
                check(event is Event.Ecs2Platform) { event }
                eventHandler.handle(event)
            }
            is TickFinished, -> {}
            is PrimaryStarted -> {
                if (space.origin == PRIMARY) {
                    space.streamLog.write(operation)
                }
            }
            is PrimaryFinished -> {
                if (space.origin == PRIMARY) {
                    space.streamLog.write(operation)
                    flushOperations(space, this)
                }
                for (player in owningPlayersPlatformIds) {
                    val entityId = platformEntityId2EntityId[player] ?: continue
                    val entity = space.entities[entityId] ?: continue
                    val fallbackServer = entity.get<FallbackServerComponent>() ?: continue
                    transfer(player, fallbackServer.destination)
                }
                space.cleanUp.invoke()
                Thread.sleep(2000)
                shutdown()
            }
        }
    }

    abstract fun shutdown()

    internal val originalProperties = mutableMapOf<EntityId, Map<EntityProperty.Key<*>, EntityProperty<*>>>()
    protected fun <E : ECSDynamic.EntityType<*>> PluginPlatformHandler<*, P_E, P_I, Platform<P_E, P_I>, *, E, *, *, *>
            .spawnUnsafe(entityId: EntityId, entityType: ECSDynamic.EntityType<*>)
            = spawn(this@Platform, entityId, entityType as E)

    abstract fun spawnPlatformEntity(
        entityId: EntityId,
        entityType: ECSDynamic.EntityType<*>?
    ): PlatformEntitySpawner

    abstract fun removePlatformEntity(
        entityId: EntityId
    )
    abstract fun getLatency(playerPlatformId: PlatformEntityId): Duration

    abstract fun transfer(playerPlatformId: PlatformEntityId, destination: DataCommand.TransferPlayers.Destination)

    fun <
            KEY : PluginKey<KEY>,
            R : ECSDynamic.Renderable<KEY>,
            E : ECSDynamic.EntityType<KEY>,
            B : ECSDynamic.BlockType<KEY>,
            I : ECSDynamic.ItemType<KEY>,
            S : ECSDynamic.SoundType<KEY>
            > getPlatformHandler(
        plugin: ECSPlugin<KEY, R, E, B, I, S>,
    ): PluginPlatformHandler<KEY, P_E, P_I, Platform<P_E, P_I>, R, E, B, I, S> {
        return plugin.platformHandlers.getValue(source.platformType) as PluginPlatformHandler<KEY, P_E, P_I, Platform<P_E, P_I>, R, E, B, I, S>
    }

    fun renderMaps() {
        for (map in space.maps) {
            renderer.pasteMap(map)
        }
    }

}


fun Platform<*, *>.mutateComponent(
    space: Space,
    entityId: EntityId,
    new: Component
) {
    space.operations.add(
        ComponentMutation(
            entityId,
            new,
            space.origin,
            source,
        )
    )
}