package com.hytale2mc.ecs.platform.minestom

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.PlatformEntityId
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.operation.DataCommand
import com.hytale2mc.ecs.platform.*
import com.hytale2mc.ecs.space.Space
import com.hytale2mc.ecs.space.flushOperations
import korlibs.time.milliseconds
import net.kyori.adventure.text.Component
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.ChunkRange
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.*
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.ping.Status
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess
import kotlin.time.Duration

typealias MinestomPlayer = Player
typealias MinestomEntity = Entity
typealias MinestomItem = ItemStack

class MinestomPlatform(
    space: Space,
) : Platform<MinestomEntity, MinestomItem>(MINECRAFT, space) {
    val beforeEcsTick = mutableListOf<() -> Unit>()
    val afterEcsTick = mutableListOf<() -> Unit>()
    override val componentHandler: MinestomComponentHandler = MinestomComponentHandler(this)

    override val eventHandler: PlatformEventHandler<*> = MinestomEventHandler(this)

    val entityIdToPlatformEntity = mutableMapOf<EntityId, MinestomEntity>()

    private val minecraftServer = MinecraftServer.init(Auth.Online())

    val instance: InstanceContainer

    init {
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator { it.modifier().fillHeight(0, 2, Block.AIR) }
        instance.chunkSupplier = { instance, x, z -> LightingChunk(instance, x, z) }
        MinecraftServer.getGlobalEventHandler().addListener(ServerListPingEvent::class.java) { event ->
            event.status = Status.builder()
                .description(Component.empty())
                .playerInfo(space.onlinePlayers, 100)
                .build()
        }

        MinecraftServer.getGlobalEventHandler().addListener<AsyncPlayerConfigurationEvent>(AsyncPlayerConfigurationEvent::class.java) { event ->
            event.player.respawnPoint = Pos(0.0, 160.0, 0.0, 0f, 0f)
            event.spawningInstance = instance
        }

        // Player Lifecycle
        MinecraftServer.getGlobalEventHandler().addListener<PlayerSpawnEvent>(PlayerSpawnEvent::class.java) { event ->
            if (!event.isFirstSpawn) return@addListener
            event.player.gameMode = GameMode.SURVIVAL
            PlayerLifecycle.callThisWhenPlayerJoinsOurPlatform(
                this,
                event.player.entityId, event.player.username
            )
            if (space.isReplaying) {
                event.player.gameMode = SPECTATOR
            }
        }
        MinecraftServer.getGlobalEventHandler().addListener<PlayerDisconnectEvent>(PlayerDisconnectEvent::class.java) { event ->
            PlayerLifecycle.callThisWhenPlayerLeavesOurPlatform(
                this,
                event.player.entityId,
                event.player.username
            )
        }

        val chunkFutures = mutableListOf<CompletableFuture<*>>()
        ChunkRange.chunksInRange(
            Vec(0.0, 0.0, 0.0),
            10
        ) { x, z ->
            chunkFutures.add(instance.loadChunk(x, z))
        }

        CompletableFuture.allOf(*chunkFutures.toTypedArray()).join()
        repeat(5) {
            instance.tick(0)
        }

        MinecraftServer.getGlobalEventHandler().addListener(ServerTickMonitorEvent::class.java) {
            for (action in beforeEcsTick) {
                action.invoke()
            }
            flushOperations(space, this)
            update()
            for (action in afterEcsTick) {
                action.invoke()
            }
        }

        componentHandler.onStart()
        eventHandler.onStart()

        for (plugin in space.plugins.values) {
            getPlatformHandler(plugin).init(this)
        }
    }

    override fun onStart() {
        minecraftServer.start(
            "0.0.0.0",
            space.port
        )
    }

    override fun shutdown() {
        exitProcess(0)
    }

    override fun spawnPlatformEntity(entityId: EntityId, entityType: ECSDynamic.EntityType<*>?): PlatformEntitySpawner {
        val (platformEntity, properties: EntityProperties?) = if (entityType == null) {
            Entity(TEXT_DISPLAY).apply {
                isAutoViewable = false
            } to null
        } else {
            val plugin = space.plugins.getValue(entityType.owner)
            val handler = getPlatformHandler(plugin)
            handler.spawnUnsafe(entityId, entityType)
        }
        entityIdToPlatformEntity[entityId] = platformEntity
        return PlatformEntitySpawner(
            platformEntity.entityId,
            properties,
            spawn = {
                if (platformEntity.instance == instance) {
                    // only for players that we own
                    return@PlatformEntitySpawner
                }
                platformEntity.setInstance(instance)
            }
        )

    }

    override fun removePlatformEntity(entityId: EntityId) {
        val platformEntity = entityIdToPlatformEntity.remove(entityId)!!
        for (plugin in space.plugins.values) {
            getPlatformHandler(plugin).onRemove(platformEntity)
        }
        platformEntity.remove()
    }

    override fun getLatency(playerPlatformId: PlatformEntityId): Duration {
        val entity = instance.getEntityById(playerPlatformId)
        if (entity !is MinestomPlayer) {
            return Duration.ZERO
        }
        return entity.latency.milliseconds
    }

    override fun transfer(playerPlatformId: PlatformEntityId, destination: DataCommand.TransferPlayers.Destination) {
        val player = instance.getEntityById(playerPlatformId) as Player
        player.playerConnection.transfer(destination.host, destination.port)
    }

}