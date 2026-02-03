package com.hytale2mc.ecs.platform.hytale

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.ComponentUpdate
import com.hypixel.hytale.protocol.ComponentUpdateType
import com.hypixel.hytale.protocol.EntityUpdate
import com.hypixel.hytale.protocol.Equipment
import com.hypixel.hytale.protocol.GameMode
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.modules.entity.EntityModule
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.HiddenFromAdventurePlayers
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.PlayerUtil
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hytale2mc.ecs.data.Entity
import com.hytale2mc.ecs.data.component.BoundingBoxComponent
import com.hytale2mc.ecs.data.component.GameModeComponent
import com.hytale2mc.ecs.data.component.GravityComponent
import com.hytale2mc.ecs.data.component.InputComponent
import com.hytale2mc.ecs.data.component.InventoryComponent
import com.hytale2mc.ecs.data.component.LocalTransformComponent
import com.hytale2mc.ecs.data.component.NametagComponent
import com.hytale2mc.ecs.data.component.TabComponent
import com.hytale2mc.ecs.data.component.TransformComponent
import com.hytale2mc.ecs.data.component.Visibility
import com.hytale2mc.ecs.data.component.VisibilityComponent
import com.hytale2mc.ecs.data.get
import com.hytale2mc.ecs.platform.ComponentHandlers
import com.hytale2mc.ecs.platform.PlatformComponentHandler
import com.hytale2mc.ecs.platform.PlatformComponentHandlerContext
import com.hytale2mc.ecs.platform.mutateComponent
import com.hytale2mc.ecs.text.raw
import korlibs.math.geom.Vector3F
import korlibs.math.geom.Vector3F.Companion.ZERO

typealias PlatformTransformComponent = com.hypixel.hytale.server.core.modules.entity.component.TransformComponent

class HytaleComponentHandler(
    platform: HytalePlatform
) : PlatformComponentHandler<HytalePlatform>(platform) {

    override fun onStart() {

    }

    private fun PlatformComponentHandlerContext.applyGameMode(gameMode: GameModeComponent) {
        val ref = getPlatformEntityIfValid() ?: return
        if (ref.store.getComponent(ref, Player.getComponentType()) == null) {
            return
        }
        val mode = when (gameMode.gameMode) {
            SURVIVAL -> {
                GameMode.Adventure
            }
            SPECTATOR -> {
                GameMode.Creative
            }
        }
        Player.setGameMode(
            ref,
            mode,
            ref.store
        )
    }

    override val gameMode: ComponentHandlers.Ecs2Platform<GameModeComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = { gameMode ->
            applyGameMode(gameMode)
        },
        onRemovedOrNotPresentOnSpawn = {},
        onMutatedFromEcs = { gameMode ->
            applyGameMode(gameMode)
        },
    )

    // Not implemented yet
    override val tab: ComponentHandlers.Ecs2Platform<TabComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = {},
        onRemovedOrNotPresentOnSpawn = {},
        onMutatedFromEcs = {}
    )

    private fun applyVisibility(
        ref: Ref<EntityStore?>,
        visibility: Visibility
    ) {
        when (visibility) {
            is Visibility.ToEveryoneExcept -> {
                // TODO
                ref.store.ensureComponent(ref, EntityModule.get().visibleComponentType)
            }
            is Visibility.ToOnly -> {
                if (visibility.entities.isEmpty()) {
                    ref.store.removeComponentIfExists(ref, EntityModule.get().visibleComponentType)
                } else {
                    ref.store.ensureComponent(ref, EntityModule.get().visibleComponentType)
                }
            }
        }
    }
    override val visibility: ComponentHandlers.Ecs2Platform<VisibilityComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = { visibility ->
            val ref = getPlatformEntityIfValid() ?: return@Ecs2Platform
            applyVisibility(ref, visibility.visibility)
        },
        onRemovedOrNotPresentOnSpawn = {
            val ref = getPlatformEntityIfValid() ?: return@Ecs2Platform
            ref.store.ensureComponent(ref, HiddenFromAdventurePlayers.getComponentType())
        },
        onMutatedFromEcs = { visibility ->
            val ref = getPlatformEntityIfValid() ?: return@Ecs2Platform
            applyVisibility(ref, visibility.visibility)
        },
    )

    override val input: ComponentHandlers.Platform2Ecs<InputComponent> = ComponentHandlers.Platform2Ecs(
        onAdded = {},
        onRemovedOrNotPresentOnSpawn = {},
        mutateFromPlatformIfNeeded = { input ->
            val ref = getPlatformEntityIfValid() ?: return@Platform2Ecs
            val movementStates = ref.store.getComponent(ref, MovementStatesComponent.getComponentType())
                ?.movementStates
            val player = ref.store.getComponent(ref, Player.getComponentType())!!
            val new = InputComponent(
                isOnGround = movementStates?.onGround ?: true,
                sneaking = movementStates?.crouching ?: false,
                jumping = movementStates?.jumping ?: false,
                rightClicking = false,
                selectedInventorySlot = player.inventory.activeHotbarSlot.toInt()
            )
            if (new != input) {
                platform.mutateComponent(
                    space,
                    entity.id,
                    new,
                )
            }
        },
        onMutatedFromEcsOnAnEntityThatWeDontOwn = {
            val inventory = entity[InventoryComponent::class]
            if (inventory != null) {
                applyInventory(inventory)
            }
        }
    )

    private fun PlatformComponentHandlerContext.applyTransform(
        transform: TransformComponent
    ) {
        val ref = getPlatformEntityIfValid() ?: return
        val direction = transform.direction.toHytaleRotation()
        val playerComponent = ref.store.getComponent(ref, Player.getComponentType())
        if (playerComponent == null) {
            ref.store.putComponent(ref, HeadRotation.getComponentType(), HeadRotation(direction))
            ref.store.getComponent(ref, PlatformTransformComponent.getComponentType())?.apply {
                position = transform.translation.toVector3d()
                rotation = Vector3f(0f, direction.y, 0f)
            }
        } else {
            ref.store.addComponent(
                ref,
                Teleport.getComponentType(),
                Teleport(
                    platform.world,
                    transform.translation.toVector3d(),
                    transform.direction.toHytaleRotation()
                )
            )
        }
    }

    override val transform: ComponentHandlers.BiDirectional<TransformComponent> = ComponentHandlers.BiDirectional(
        onAdded = { transform ->
            applyTransform(transform)
        },
        onRemovedOrNotPresentOnSpawn = {},
        onMutatedFromEcs = { transform ->
            val ref = getPlatformEntityIfValid() ?: return@BiDirectional
            val platformTransform = ref.store.getComponent(ref, PlatformTransformComponent.getComponentType())!!
            var delta: Vector3F = transform.translation.minus(platformTransform.position.toVector3F())
            applyTransform(transform)
            val platformEntityComponent = ref.store.getComponent(ref, PlatformEntityComponent.type)!!
            platformEntityComponent.movementStates.apply {
                when {
                    delta.y > 0f -> flying = true
                    delta.y < 0f -> falling = true
                    delta.isAlmostEquals(ZERO) -> idle = true
                    else -> running = true
                }
            }
            if (!delta.isAlmostEquals(ZERO)) {
                platformEntityComponent.moved = true
            }
        },
        mutateFromPlatformIfNeeded = { transform ->
            val ref = getPlatformEntityIfValid() ?: return@BiDirectional
            val platformTransform = ref.store.getComponent(ref, PlatformTransformComponent.getComponentType())!!
            val platformHeadRotation = ref.store.getComponent(ref, HeadRotation.getComponentType())!!
            val hytale2mcDirection = platformHeadRotation.rotation.toHytale2mcDirection()
            val movement = ref.store.getComponent(ref, MovementManager.getComponentType())
            val playerRef = ref.store.getComponent(ref, PlayerRef.getComponentType())
            if (movement != null) {
                if (playerRef != null) {
                    movement.update(playerRef.packetHandler)
                }
            }

            if (!transform.translation.isAlmostEquals(platformTransform.position.toVector3F())) {
                platform.mutateComponent(
                    space,
                    entity.id,
                    transform.copy(
                        translation = platformTransform.position.toVector3F(),
                        direction = hytale2mcDirection
                    ),
                )
            } else if (!transform.direction.isAlmostEquals(hytale2mcDirection)) {
                platform.mutateComponent(
                    space,
                    entity.id,
                    transform.copy(
                        translation = platformTransform.position.toVector3F(),
                        direction = hytale2mcDirection
                    ),
                )
            }
        },
    )

    override val localTransform: ComponentHandlers.Ecs2Platform<LocalTransformComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = {},
        onRemovedOrNotPresentOnSpawn = {},
        onMutatedFromEcs = {},
    )

    override val gravity: ComponentHandlers.Ecs2Platform<GravityComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = {},
        onRemovedOrNotPresentOnSpawn = {},
        onMutatedFromEcs = {},
    )

    override val boundingBox: ComponentHandlers.Ecs2Platform<BoundingBoxComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = {},
        onRemovedOrNotPresentOnSpawn = {},
        onMutatedFromEcs = {},
    )

    private fun PlatformComponentHandlerContext.applyNametag(
        nametag: NametagComponent
    ) {
        val ref = getPlatformEntityIfValid() ?: return
        ref.store.putComponent(ref, Nameplate.getComponentType(), Nameplate(nametag.text.raw()))
    }
    override val nametag: ComponentHandlers.Ecs2Platform<NametagComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = {
            applyNametag(it)
        },
        onRemovedOrNotPresentOnSpawn = {
            val ref = getPlatformEntityIfValid() ?: return@Ecs2Platform
            ref.store.removeComponentIfExists(ref, Nameplate.getComponentType())
        },
        onMutatedFromEcs = {
            applyNametag(it)
        },
    )

    fun getInventoryComponentUpdates(
        entity: Entity,
        inventoryComponent: InventoryComponent
    ): ComponentUpdate? {
        val input = entity[InputComponent::class] ?: return null
        val armorIds = arrayOf("", "", "", "")
        val equipment = inventoryComponent.slots[input.selectedInventorySlot].let {
            if (it == null || it.amount < 1) {
                Equipment(
                    armorIds,
                    "Empty",
                    "Empty"
                )
            } else {
                val plugin = platform.space.plugins.getValue(it.itemType.owner)
                val itemStack = platform.getPlatformHandler(plugin).createItemUnsafe(platform, it.itemType)
                Equipment(
                    armorIds,
                    itemStack.itemId,
                    "Empty"
                )
            }
        }
        val update = ComponentUpdate()
        update.type = ComponentUpdateType.Equipment
        update.equipment = equipment
        return update
    }
    fun PlatformComponentHandlerContext.applyInventory(inventoryComponent: InventoryComponent) {
        val ref = getPlatformEntityIfValid() ?: return
        val playerRef = ref.store.getComponent(ref, Player.getComponentType())

        if (playerRef != null) {
            val inventory = playerRef.inventory
            for ((index, slot) in inventoryComponent.slots.withIndex()) {
                if (slot == null || slot.amount < 1) {
                    inventory.hotbar.removeItemStackFromSlot(index.toShort())
                } else {
                    val plugin = platform.space.plugins.getValue(slot.itemType.owner)
                    val itemStack = platform.getPlatformHandler(plugin).createItemUnsafe(platform, slot.itemType)
                    inventory.hotbar.setItemStackForSlot(
                        index.toShort(),
                        itemStack
                            .withQuantity(slot.amount)
                    )
                }
            }
        } else {
            val networkId = ref.store.getComponent(ref, NetworkId.getComponentType())?.id ?: return
            PlayerUtil.broadcastPacketToPlayersNoCache(
                platform.world.entityStore.store,
                EntityUpdates(
                    null,
                    arrayOf(EntityUpdate(networkId, null, arrayOf(getInventoryComponentUpdates(entity, inventoryComponent) ?: return)))
                )
            )
        }
    }
    override val inventory: ComponentHandlers.Ecs2Platform<InventoryComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = { inventory ->
            applyInventory(inventory)
        },
        onRemovedOrNotPresentOnSpawn = {
            applyInventory(InventoryComponent(InventoryComponent.createEmptySlots()))
        },
        onMutatedFromEcs = { inventory ->
            getPlatformEntityIfValid() ?: return@Ecs2Platform
            applyInventory(inventory)
        },
    )

    private fun PlatformComponentHandlerContext.getPlatformEntityIfValid(): Ref<EntityStore?>? {
        val ref = platform.entityId2Ref[entity.id]
        if (ref == null) {
            throw IllegalStateException("wanted ref of $platformEntityId but it didn't exist, ${platform.world.entityStore}")
        }
        return ref.takeIf { it.isValid }
    }

}