package com.hytale2mc.ecs.platform.hytale

import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.dependency.Dependency
import com.hypixel.hytale.component.dependency.Order
import com.hypixel.hytale.component.dependency.SystemDependency
import com.hypixel.hytale.component.system.tick.TickingSystem
import com.hypixel.hytale.math.shape.Box2D
import com.hypixel.hytale.math.vector.Vector2d
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.protocol.ComponentUpdate
import com.hypixel.hytale.protocol.ComponentUpdateType
import com.hypixel.hytale.protocol.EntityUpdate
import com.hypixel.hytale.protocol.MovementStates
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.*
import com.hypixel.hytale.protocol.packets.setup.ClientFeature
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.event.events.PrepareUniverseEvent
import com.hypixel.hytale.server.core.event.events.ecs.*
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems
import com.hypixel.hytale.server.core.modules.entity.stamina.StaminaSystems
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.PlayerUtil
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.WorldConfig
import com.hypixel.hytale.server.core.universe.world.WorldConfigProvider
import com.hypixel.hytale.server.core.universe.world.spawn.GlobalSpawnProvider
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkUnloadingSystem
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.VoidWorldGenProvider
import com.hytale2mc.ecs.data.component.InputComponent
import com.hytale2mc.ecs.data.get
import com.hytale2mc.ecs.platform.PlayerLifecycle
import com.hytale2mc.ecs.space.Space
import com.hytale2mc.ecs.util.directionFromYawAndPitch
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.math.max

private const val WORLD_NAME = "default"

abstract class HytalePlatformPlugin(
    init: JavaPluginInit
) : JavaPlugin(init) {

    companion object {

        lateinit var plugin: HytalePlatformPlugin

    }

    init {
        com.hytale2mc.ecs.data.classLoader = classLoader
    }

    abstract val space: Space
    val platform by lazy { HytalePlatform(space, this) }

    override fun setup() {
        plugin = this

        entityStoreRegistry.restrict<PlaceBlockEvent>()
        entityStoreRegistry.restrict<BreakBlockEvent>()
        entityStoreRegistry.restrict<DropItemEvent>()
        entityStoreRegistry.restrict<DropItemEvent.Drop>()
        entityStoreRegistry.restrict<DropItemEvent.PlayerRequest>()
        entityStoreRegistry.restrict<InteractivelyPickupItemEvent>()
        entityStoreRegistry.restrict<CraftRecipeEvent>()
        entityStoreRegistry.restrict<CraftRecipeEvent.Pre>()
        entityStoreRegistry.restrict<CraftRecipeEvent.Post>()
        entityStoreRegistry.restrict<UseBlockEvent.Pre>()

        eventRegistry.registerGlobal(PrepareUniverseEvent::class.java) { event ->
            event.worldConfigProvider = object : WorldConfigProvider {
                override fun load(savePath: Path, name: String?): CompletableFuture<WorldConfig?> {
                    return CompletableFuture.completedFuture(
                        WorldConfig().apply {
                            displayName = WORLD_NAME
                            worldGenProvider = VoidWorldGenProvider()
                            isSavingPlayers = false
                            isDeleteOnUniverseStart = true
                            chunkConfig = WorldConfig.ChunkConfig().apply {
                                val region = Box2D(Vector2d(-200.0, -200.0), Vector2d(200.0, 200.0))
                                this.keepLoadedRegion = region
                                this.pregenerateRegion = region
                            }
                            // =)
                            val isFallDamageEnabledField = WorldConfig::class.java.getDeclaredField("isFallDamageEnabled")
                            isFallDamageEnabledField.isAccessible = true
                            isFallDamageEnabledField.set(this, false)

                            setCanUnloadChunks(false)
                            setCanSaveChunks(false)
                            spawnProvider = GlobalSpawnProvider(
                                com.hypixel.hytale.math.vector.Transform(
                                    Vector3d(0.0, 0.0, 0.0),
                                    directionFromYawAndPitch(0.0, 50.0).toHytaleRotation()
                                )
                            )
                        }
                    )
                }

                override fun save(savePath: Path, config: WorldConfig?, world: World?): CompletableFuture<Void?> {
                    return CompletableFuture.completedFuture(null)
                }
            }
        }

        PlatformEntityComponent.type = this.entityStoreRegistry.registerComponent(
            PlatformEntityComponent::class.java
        ) { throw NotImplementedError() }
    }

    override fun start() {
        entityStoreRegistry.registerSystem(WorldTick(this))
        entityStoreRegistry.registerSystem(ECSInventorySystem(this))
        eventRegistry.registerGlobal(AddPlayerToWorldEvent::class.java) { event ->
            event.setBroadcastJoinMessage(false)
        }
        eventRegistry.registerGlobal(PlayerReadyEvent::class.java) { event ->
            val platformEntityId = platform.platformEntityId.getAndIncrement()
            platform.world.execute {
                if (!platform.space.isReplaying) {
                    event.playerRef.store.addComponent(
                        event.playerRef,
                        PlatformEntityComponent.type,
                        PlatformEntityComponent(platformEntityId, MovementStates())
                    )
                    platform.tempPlatformEntityId2Ref[platformEntityId] = event.playerRef
                } else {
                    val player = event.playerRef.store.getComponent(event.playerRef, Player.getComponentType())!!
                    val playerRef = event.playerRef.store.getComponent(event.playerRef, PlayerRef.getComponentType())!!
                    player.hudManager.hideHudComponents(
                        playerRef,
                        Compass, Health, Stamina, Oxygen, StatusIcons, AmmoIndicator,
                        InputBindings, PortalPanel, Sleep, Mana, BlockVariantSelector,
                        ObjectivePanel, UtilitySlotSelector, BuilderToolsLegend,
                        Chat, Reticle, Hotbar
                    )
                    event.playerRef.store.putComponent(
                        event.playerRef,
                        Teleport.getComponentType(),
                        Teleport(
                            Vector3d(0.0, 160.0, 0.0),
                            directionFromYawAndPitch(0.0, 0.0).toHytaleRotation()
                        )
                    )
                }
                PlayerLifecycle.callThisWhenPlayerJoinsOurPlatform(
                    platform,
                    platformEntityId,
                    event.player.displayName
                )
            }
        }

        eventRegistry.registerGlobal(PlayerDisconnectEvent::class.java) { event ->
            platform.world.execute {
                val platformEntityId = event.playerRef.reference?.getPlatformEntityId() ?: return@execute
                PlayerLifecycle.callThisWhenPlayerLeavesOurPlatform(
                    platform,
                    platformEntityId,
                    event.playerRef.username
                )
            }
        }

        platform.componentHandler.onStart()
        platform.eventHandler.onStart()

        ChunkStore.REGISTRY.unregisterSystem(ChunkUnloadingSystem::class.java)
        EntityStore.REGISTRY.unregisterSystem(StaminaSystems.SprintStaminaEffectSystem::class.java)
        EntityStore.REGISTRY.unregisterSystem(DamageSystems.OutOfWorldDamage::class.java)
        EntityStore.REGISTRY.unregisterSystem(DamageSystems.ApplyDamage::class.java)
    }

    fun init() {
        platform.initialized = true
        platform.start()
    }

}

private class WorldTick(
    val plugin: HytalePlatformPlugin
) : TickingSystem<EntityStore>() {

    private val dependencies = setOf(SystemDependency(Order.BEFORE, EntityTrackerSystems.SendPackets::class.java))

    override fun getDependencies(): Set<Dependency<EntityStore?>?> {
        return super.getDependencies() + dependencies
    }

    fun refreshWorldTime(world: World) {
        val periodTime: Float = max(0.0f, WorldTimeResource.HOURS_PER_DAY / 2.0f)
        val worldTimeResource: WorldTimeResource =
            world.entityStore.store.getResource<WorldTimeResource?>(WorldTimeResource.getResourceType())
        worldTimeResource.setDayTime((periodTime / WorldTimeResource.HOURS_PER_DAY).toDouble(), world, world.entityStore.store)
    }

    var tick = 0

    private var initializing = false

    override fun tick(
        dt: Float,
        systemIndex: Int,
        store: Store<EntityStore?>
    ) {
        when {
            initializing -> {}
            !plugin.platform.initialized && Universe.get().worlds.contains(WORLD_NAME) -> {
                initializing = true
                val world = Universe.get().worlds.getValue(WORLD_NAME)

                world.tps = 20

                plugin.platform.world = world
                refreshWorldTime(world)
                world.registerFeature(ClientFeature.CrouchSlide, false)
                world.registerFeature(ClientFeature.SprintForce, false)
                world.registerFeature(ClientFeature.Mantling, false)
                world.registerFeature(ClientFeature.SafetyRoll, false)
                world.registerFeature(ClientFeature.SplitVelocity, false)
                world.registerFeature(ClientFeature.DisplayCombatText, false)
                world.registerFeature(ClientFeature.DisplayHealthBars, false)

                plugin.init()

                initializing = false
            }
            else -> {
                val space = plugin.platform.space
                plugin.platform.update()
                tick++
                if (tick % 1000 == 0) {
                    refreshWorldTime(plugin.platform.world)
                }
                val updates = mutableListOf<EntityUpdate>()
                for (e in space.entities.values) {
                    val ref = plugin.platform.entityId2Ref[e.id] ?: continue
                    if (!ref.isValid) {
                        continue
                    }
                    val platformEntityComponent = plugin.platform.world.entityStore.store.getComponent(ref, PlatformEntityComponent.type) ?: continue
                    if (platformEntityComponent.id in plugin.platform.owningPlayersPlatformIds) {
                        continue
                    }
                    val movementStates = platformEntityComponent.movementStates.clone()
                    platformEntityComponent.movementStates = MovementStates()

                    if (!platformEntityComponent.moved) {
                        movementStates.idle = true
                    } else {
                        movementStates.idle = false
                        movementStates.horizontalIdle = false
                    }
                    val input = e[InputComponent::class]
                    if (input != null) {
                        movementStates.apply {
                            crouching = input.sneaking
                            jumping = input.jumping
                            onGround = input.isOnGround
                        }
                    }
                    platformEntityComponent.moved = false

                    val networkId = plugin.platform.world.entityStore.store.getComponent(ref, NetworkId.getComponentType()) ?: continue
                    updates.add(
                        EntityUpdate(
                            networkId.id,
                            null,
                            arrayOf(
                                ComponentUpdate().apply {
                                    type = ComponentUpdateType.MovementStates
                                    this.movementStates = movementStates
                                },
                            )
                        )
                    )
                }
                PlayerUtil.broadcastPacketToPlayers(
                    plugin.platform.world.entityStore.store,
                    EntityUpdates(null, updates.toTypedArray())
                )
            }
        }
    }

}