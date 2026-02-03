package com.hytale2mc.ecs.platform.minestom

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.PlatformEntityId
import com.hytale2mc.ecs.data.component.*
import com.hytale2mc.ecs.data.get
import com.hytale2mc.ecs.platform.*
import korlibs.math.geom.Quaternion
import korlibs.math.geom.Vector2I
import korlibs.math.geom.Vector3F
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.*
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.entity.vehicle.PlayerInputs
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.event.player.PlayerSwapItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MinestomComponentHandler(platform: MinestomPlatform) : PlatformComponentHandler<MinestomPlatform>(platform) {

    override fun onStart() {
        platform.instance.eventNode().addListener(InventoryPreClickEvent::class.java) { event ->
            event.isCancelled = true
        }
        platform.instance.eventNode().addListener(PlayerSwapItemEvent::class.java) { event ->
            event.isCancelled = true
        }
        platform.instance.eventNode().addListener(PlayerHandAnimationEvent::class.java) { event ->
            event.isCancelled = true
        }
        platform.instance.eventNode().addListener(ItemDropEvent::class.java) { event ->
            event.isCancelled = true
        }
        platform.instance.eventNode().addListener(PlayerChangeHeldSlotEvent::class.java) { event ->
            event.player.refreshItemUse(null, 0)
        }
    }

    override val gameMode: ComponentHandlers.Ecs2Platform<GameModeComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = { gameMode ->
            val entity = getPlatformEntity()
            if (entity !is Player) {
                return@Ecs2Platform
            }
            entity.gameMode = when (gameMode.gameMode) {
                SURVIVAL -> GameMode.SURVIVAL
                SPECTATOR -> GameMode.SPECTATOR
            }
        },
        onRemovedOrNotPresentOnSpawn = { },
        onMutatedFromEcs = { gameMode ->
            val entity = getPlatformEntity()
            if (entity !is Player) {
                return@Ecs2Platform
            }
            entity.gameMode = when (gameMode.gameMode) {
                SURVIVAL -> GameMode.SURVIVAL
                SPECTATOR -> GameMode.SPECTATOR
            }
        },
    )

    private val tabs = mutableMapOf<PlatformEntityId, MinestomTab>()
    private fun getOrInitTab(
        tab: TabComponent,
        player: MinestomPlayer
    ): MinestomTab? {
        return when (val tab = tab.tab) {
            is TabComponent.Tab.Grid -> {
                val currentTab = tabs[player.entityId]
                if (currentTab != null) {
                    return currentTab
                }
                val minestomTab = MinestomTab(player)
                tabs[player.entityId] = minestomTab
                minestomTab.init(tab)
                null
            }
            null -> null
        }
    }
    override val tab: ComponentHandlers.Ecs2Platform<TabComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = { tab ->
            val entity = getPlatformEntity()
            if (entity !is Player) {
                return@Ecs2Platform
            }
            entity.sendPlayerListHeaderAndFooter(
                tab.header?.toAdventure() ?: Component.empty(),
                tab.footer?.toAdventure() ?: Component.empty(),
            )
            getOrInitTab(tab, entity)
        },
        onRemovedOrNotPresentOnSpawn = {
            val entity = getPlatformEntity()
            if (entity !is Player) {
                return@Ecs2Platform
            }
            entity.sendPlayerListHeaderAndFooter(
                Component.empty(),
                Component.empty(),
            )
        },
        onMutatedFromEcs = { tab ->
            val entity = getPlatformEntity()
            if (entity !is Player) {
                return@Ecs2Platform
            }
            entity.sendPlayerListHeaderAndFooter(
                Component.empty(),
                Component.empty(),
            )
            getOrInitTab(tab, entity)?.updateEntries(tab.tab as TabComponent.Tab.Grid)
        },
    )

    class MinestomTab(
        val player: Player
    ) {

        val rowAndColumn2TabEntry = mutableMapOf<Vector2I, PlayerInfoUpdatePacket.Entry>()

        fun init(
            tab: TabComponent.Tab.Grid
        ) {
            val packet = PlayerInfoUpdatePacket(
                EnumSet.allOf(PlayerInfoUpdatePacket.Action::class.java),
                buildList {
                    for (columnIndex in 0..<tab.columns) {
                        val columnPriority = 1000 - columnIndex * 20
                        for (rowIndex in 0..<20) {
                            val tabEntry = PlayerInfoUpdatePacket.Entry(
                                UUID.randomUUID(),
                                "",
                                listOf(
                                    PlayerInfoUpdatePacket.Property(
                                        "textures",
                                        "ewogICJ0aW1lc3RhbXAiIDogMTc2ODg1MTY1NTMwNSwKICAicHJvZmlsZUlkIiA6ICJjYzU2NjAzYmZiZjg0OGJmOTM3MzY1N2VjYzUyNGI4NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJpc2FhYWFhYV8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI3N2VmN2FmNDVjYWE4YTQ4MDM2ZTRkNzUyY2Q5N2NmMGJiODgyNGM3Mjc1YzRjZGNlYmYyMTk4NDZhOTJjYiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                                        "GU7m3cRdI7iEQPR0sXW7FHkJZsshQrM6Fbc07GklllbBFZtzNHBDP3N67lrcDA6oPAQlZZeUPiJqCdxtv0znMtUXNCqU+tqxAr0OKLG4fgQWEJo5DkZbqNPKoexeTVK7cg+RrBdCGhtZNtkypNN0wwiFZqoS/iArsp1pmFdJsXumG+bHZLE2mIYaFx9Xfylyz6Azzz0h7pmpv1Q4CdcrMc8pXAha5iIFTuNkzB1dYufZPpS/flE2bISRUQRATC7TmIWqR6iMS8jqQexXyar5jdGn02/7uAWhhzJ4AjtcpEoEWbgO+7Gxu1WTP9XNbwmMZJEDqz86Up94O2RIijG4JHsL/nhNczGH8b3+Ksq0PN2tx3Xv7Us9Q0HEz+fpj2a1ScAH9ErnlSszx8dQItTthBn1ASDX+ae+Fo5VKrwRZETuzLor06V1tw07i54oOdjsjAbAFbwOjw80MlT43fzOvgb2pX/M95lwvc42sd8eCSLI9ng9oO3x/QnUakNpSHweQK26FrGjEbPxJuzx8eF2ZTZWS8uhlr4WCutVWHaEe5qbayNGT0DT/dz0f+mVFRxeACASt9tX4AEC4J8JCBwLZQ9Ru8lv9cDzvFTt44q60PvebniJkRsl/yFsWOPlL/5liIR0IXu71FXfH4SCQ6uNIcEzQBgIQNCromvk0zUz/8A="
                                    )
                                ),
                                true,
                                0,
                                GameMode.CREATIVE,
                                tab.rows.getOrNull(rowIndex)?.columns?.getOrNull(columnIndex)?.text?.toAdventure(),
                                null,
                                columnPriority - rowIndex,
                                false
                            )
                            rowAndColumn2TabEntry[Vector2I(rowIndex, columnIndex)] = tabEntry
                            add(tabEntry)
                        }
                    }

                }
            )
            player.sendPacket(packet)
        }

        fun updateEntries(
            tab: TabComponent.Tab.Grid
        ) {
            val packet = PlayerInfoUpdatePacket(
                EnumSet.of(PlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                buildList {
                    for (columnIndex in 0..<tab.columns) {
                        for (rowIndex in 0..<20) {
                            val tabEntry = rowAndColumn2TabEntry.getValue(Vector2I(rowIndex, columnIndex))
                            val text = tab.rows.getOrNull(rowIndex)?.columns?.getOrNull(columnIndex)?.text?.toAdventure()
                            add(
                                PlayerInfoUpdatePacket.Entry(
                                    tabEntry.uuid,
                                    tabEntry.username,
                                    tabEntry.properties,
                                    tabEntry.listed,
                                    tabEntry.latency,
                                    tabEntry.gameMode,
                                    text,
                                    tabEntry.chatSession,
                                    tabEntry.listOrder,
                                    tabEntry.displayHat
                                )
                            )
                        }
                    }

                }
            )
            player.sendPacket(packet)
        }

    }

    override val visibility: ComponentHandlers.Ecs2Platform<VisibilityComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = {
            applyVisibility(getPlatformEntity(), it.visibility)
        },
        onRemovedOrNotPresentOnSpawn = {
            getPlatformEntity().updateViewableRule { false }
        },
        onMutatedFromEcs = {
            applyVisibility(getPlatformEntity(), it.visibility)
        },
    )

    private fun applyVisibility(entity: Entity, visibility: Visibility) {
        when (visibility) {
            is Visibility.ToOnly -> {
                if (visibility.entities.isEmpty()) {
                    entity.updateViewableRule { false }
                } else {
                    entity.updateViewableRule { viewer ->
                        val viewerEntityId = platform.platformEntityId2EntityId[viewer.entityId] ?: return@updateViewableRule true
                        viewerEntityId in visibility.entities
                    }
                }
            }
            is Visibility.ToEveryoneExcept -> {
                if (visibility.entities.isEmpty()) {
                    entity.updateViewableRule { true }
                } else {
                    entity.updateViewableRule { viewer ->
                        val viewerEntityId = platform.platformEntityId2EntityId[viewer.entityId] ?: return@updateViewableRule true
                        viewerEntityId !in visibility.entities
                    }
                }
            }
        }

        val meta = entity.entityMeta
        if (meta is TextDisplayMeta) {
            meta.isSeeThrough = visibility.renderThroughWalls
        }
    }

    fun InventoryItem.toPlatformItem(): MinestomItem {
        val plugin = platform.space.plugins.getValue(itemType.owner)
        return platform.getPlatformHandler(plugin).createItemUnsafe(platform, itemType)
    }

    private fun PlatformComponentHandlerContext.updatePlatformInventoryToMatchEcs(
        inventoryComponent: InventoryComponent
    ) {
        val platformEntity = getPlatformEntity()
        fun InventoryItem?.toInventoryItemStack(): ItemStack {
            if (this == null) {
                return ItemStack.AIR
            }
            return toPlatformItem()
                .withCustomName(textInInventory?.toAdventure()?.decoration(TextDecoration.ITALIC, false))
                .withAmount(amount)
        }
        when (platformEntity) {
            is Player -> {
                for ((index, slot) in inventoryComponent.slots.withIndex()) {
                    when {
                        slot == null || slot.amount < 1 -> {
                            platformEntity.inventory.setItemStack(index, ItemStack.AIR)
                            if (index == platformEntity.heldSlot.toInt()) {
                                platformEntity.refreshItemUse(null, 0)
                            }
                        }
                        else -> platformEntity.inventory.setItemStack(index, slot.toInventoryItemStack())
                    }

                }
            }
            is LivingEntity -> {
                val inputComponent = entity[InputComponent::class] ?: return
                platformEntity.itemInMainHand = inventoryComponent.slots[inputComponent.selectedInventorySlot].toInventoryItemStack()
            }
        }

    }

    override val inventory = ComponentHandlers.Ecs2Platform<InventoryComponent>(
        onAdded = { inventory ->
            updatePlatformInventoryToMatchEcs(inventory)
        },
        onRemovedOrNotPresentOnSpawn = {
            when (val platformEntity = getPlatformEntity()) {
                is Player -> {
                    val platformPlayer = platformEntity
                    platformPlayer.inventory.clear()
                    platformPlayer.refreshItemUse(null, 0)
                }
                is LivingEntity -> {
                    platformEntity.itemInMainHand = ItemStack.AIR
                }
            }

        },
        onMutatedFromEcs = { inventory ->
            updatePlatformInventoryToMatchEcs(inventory)
        },
    )

    fun PlayerInputs.movement(yawDegrees: Float): Vector3F {
        val forwardInput = (if (forward()) 1f else 0f) - (if (backward()) 1f else 0f)
        val strafeInput  = (if (right()) 1f else 0f) - (if (left()) 1f else 0f)
        if (forwardInput == 0f && strafeInput == 0f) {
            return Vector3F(0f, 0f, 0f)
        }
        val rads = Math.toRadians(yawDegrees.toDouble() + 90)
        val fwdX = cos(rads).toFloat()
        val fwdZ = sin(rads).toFloat()
        val rightX = -fwdZ
        val rightZ = fwdX
        val moveX = (fwdX * forwardInput) + (rightX * strafeInput)
        val moveZ = (fwdZ * forwardInput) + (rightZ * strafeInput)
        val wishDir = Vector3F(moveX, 0f, moveZ)
        val magnitude = kotlin.math.sqrt(wishDir.x * wishDir.x + wishDir.z * wishDir.z)
        return if (magnitude > 0) {
            wishDir.copy(
                x = wishDir.x / magnitude,
                z = wishDir.z / magnitude
            )
        } else {
            wishDir
        }
    }

    override val input = ComponentHandlers.Platform2Ecs<InputComponent>(
        onAdded = {},
        onRemovedOrNotPresentOnSpawn = {},
        mutateFromPlatformIfNeeded = {
            val platformEntity = getPlatformEntity()
            if (platformEntity !is Player) {
                return@Platform2Ecs
            }
            val new = InputComponent(
                isOnGround = platformEntity.isOnGround,
                sneaking = platformEntity.isSneaking,
                selectedInventorySlot = platformEntity.heldSlot.toInt(),
                rightClicking = platformEntity.isUsingItem,
                jumping = platformEntity.inputs().jump(),
                movement = platformEntity.inputs().movement(platformEntity.position.yaw),
                forward = platformEntity.inputs().forward(),
                backward = platformEntity.inputs().backward(),
            )
            val old = entity[InputComponent::class]
            if (new == old) {
                return@Platform2Ecs
            }
            platform.mutateComponent(
                space,
                entity.id,
                new,
            )
        },
        onMutatedFromEcsOnAnEntityThatWeDontOwn = { input ->
            val platformEntity = getPlatformEntity()
            platformEntity.isSneaking = input.sneaking
            if (platformEntity is LivingEntity) {
                val inventory = entity[InventoryComponent::class] ?: return@Platform2Ecs
                val slot = inventory.slots.getOrNull(input.selectedInventorySlot) ?: return@Platform2Ecs
                platformEntity.itemInMainHand = slot.toPlatformItem()
            }
        }
    )


    override val transform: ComponentHandlers.BiDirectional<TransformComponent> = ComponentHandlers.BiDirectional(
        onAdded = { transform ->
            val platformEntity = getPlatformEntity()
            if (platformEntity.instance == null) {
                // =)
                val posField = Entity::class.java.getDeclaredField("position")
                posField.isAccessible = true
                posField[platformEntity] = transform.toPos()
            } else {
                platformEntity.teleport(transform.toPos())
            }
        },
        onRemovedOrNotPresentOnSpawn = {
            // TODO: ??
        },
        onMutatedFromEcs = { transform ->
            val platformEntity = getPlatformEntity()
            val newPos = transform.toPos()
            platformEntity.setView(newPos.yaw, newPos.pitch)
            platformEntity.teleport(newPos)
        },
        mutateFromPlatformIfNeeded = { transform ->
            val platformEntity = getPlatformEntity()
            if (transform.translation != platformEntity.position.toVector3F()) {
                platform.mutateComponent(
                    space,
                    entity.id,
                    transform.copy(translation = platformEntity.position.toVector3F(), direction = platformEntity.position.direction().toVector3F()),
                )
            } else if (transform.direction != platformEntity.position.direction().toVector3F()) {
                platform.mutateComponent(
                    space,
                    entity.id,
                    transform.copy(translation = platformEntity.position.toVector3F(), direction = platformEntity.position.direction().toVector3F()),
                )
            }
        },
    )

    override val localTransform: ComponentHandlers.Ecs2Platform<LocalTransformComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = { localTransform ->
            val platformEntity = getPlatformEntity()
            if (!isDisplay(platformEntity.entityType)) {
                return@Ecs2Platform
            }
            val meta = platformEntity.entityMeta as AbstractDisplayMeta
            applyLocalTransform(
                entity.id,
                meta,
                localTransform.translation,
                localTransform.rotation,
                localTransform.scale
            )
        },
        onRemovedOrNotPresentOnSpawn = {

        },
        onMutatedFromEcs = { localTransform ->
            val platformEntity = getPlatformEntity()
            if (!isDisplay(platformEntity.entityType)) {
                return@Ecs2Platform
            }
            val meta = platformEntity.entityMeta as AbstractDisplayMeta
            applyLocalTransform(
                entity.id,
                meta,
                localTransform.translation,
                localTransform.rotation,
                localTransform.scale
            )
        },
    )

    private fun isDisplay(entityType: EntityType): Boolean {
        return entityType == BLOCK_DISPLAY || entityType == ITEM_DISPLAY || entityType == TEXT_DISPLAY
    }

    private fun applyLocalTransform(
        entityId: EntityId,
        meta: AbstractDisplayMeta,
        translation: Vector3F,
        rotation: Quaternion,
        scale: Vector3F,
    ) {
        meta.transformationInterpolationDuration = 2
        meta.transformationInterpolationStartDelta = 0
        meta.translation = (translation + platform.getOriginalProperty(entityId, EntityProperty.Translation)).toVec()
        meta.scale = (scale * platform.getOriginalProperty(entityId, EntityProperty.Scale)).toVec()
        meta.leftRotation = floatArrayOf(rotation.x, rotation.y, rotation.z, rotation.w)
    }


    private val platformEntityId2NametagPlatformEntityId = mutableMapOf<PlatformEntityId, PlatformEntityId>()
    override val nametag: ComponentHandlers.Ecs2Platform<NametagComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = { nametag ->
            val textDisplay = MinestomEntity(EntityType.TEXT_DISPLAY)
            val meta = textDisplay.entityMeta as TextDisplayMeta
            meta.billboardRenderConstraints = CENTER
            meta.text = nametag.text.toAdventure()
            meta.backgroundColor = nametag.backgroundColor.toRGBInt().argb.toInt()
            meta.lineWidth = 1000
            applyVisibility(textDisplay, Visibility.ToEveryoneExcept(listOf(entity.id), nametag.renderThroughWalls))
            applyLocalTransform(
                entity.id,
                meta,
                nametag.localTranslation + platform.getOriginalProperty(entity.id, EntityProperty.NametagTranslation),
                Quaternion.IDENTITY,
                nametag.scale * (Vector3F.ONE / platform.getOriginalProperty(entity.id, EntityProperty.Scale))
            )
            textDisplay.setInstance(platform.instance)

            val platformEntity = getPlatformEntity()

            platformEntity.addPassenger(textDisplay)
            platformEntityId2NametagPlatformEntityId[platformEntity.entityId] = textDisplay.entityId
        },
        onRemovedOrNotPresentOnSpawn = {
            val nametag = platformEntityId2NametagPlatformEntityId[platformEntityId] ?: return@Ecs2Platform
            platform.instance.getEntityById(nametag)!!.remove()
        },
        onMutatedFromEcs = { nametag ->
            val nametagPlatformEntityId = platformEntityId2NametagPlatformEntityId[platformEntityId]
            checkNotNull(nametagPlatformEntityId) { "No nametag for $platformEntityId in $platformEntityId2NametagPlatformEntityId" }
            val platformNametag = platform.instance.getEntityById(nametagPlatformEntityId)!!
            val meta = platformNametag.entityMeta as TextDisplayMeta
            meta.text = nametag.text.toAdventure()
            meta.backgroundColor = nametag.backgroundColor.toRGBInt().argb.toInt()
            applyLocalTransform(entity.id, meta, nametag.localTranslation, Quaternion.IDENTITY, nametag.scale)
        },
    )

    override val gravity = ComponentHandlers.Ecs2Platform<GravityComponent>(
        onAdded = {
            val platformEntity = getPlatformEntity()
            platformEntity.entityMeta.isHasNoGravity = false
            if (platformEntity !is LivingEntity) return@Ecs2Platform
        },
        onRemovedOrNotPresentOnSpawn = {
            val platformEntity = getPlatformEntity()
            platformEntity.entityMeta.isHasNoGravity = true
            if (platformEntity !is LivingEntity) return@Ecs2Platform
            val gravity = platformEntity.getAttribute(Attribute.GRAVITY)
            gravity.baseValue = 0.0
        },
        onMutatedFromEcs = {},
    )

    override val boundingBox: ComponentHandlers.Ecs2Platform<BoundingBoxComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = { boundingBox ->
            val platformEntity = getPlatformEntity()
            if (platformEntity is Player) {
                return@Ecs2Platform
            }
            platformEntity.boundingBox = BoundingBox(boundingBox.min.toVec(), boundingBox.max.toVec())
        },
        onRemovedOrNotPresentOnSpawn = {
            val platformEntity = getPlatformEntity()
            if (platformEntity is Player) {
                return@Ecs2Platform
            }
            platformEntity.boundingBox = BoundingBox(Vec.ZERO, Vec.ZERO)
        },
        onMutatedFromEcs = { boundingBox ->
            val platformEntity = getPlatformEntity()
            if (platformEntity is Player) {
                return@Ecs2Platform
            }
            platformEntity.boundingBox = BoundingBox(boundingBox.min.toVec(), boundingBox.max.toVec())
        },
    )

    private fun PlatformComponentHandlerContext.getPlatformEntity(): MinestomEntity {
        return platform.entityIdToPlatformEntity.getValue(entity.id)
    }

}