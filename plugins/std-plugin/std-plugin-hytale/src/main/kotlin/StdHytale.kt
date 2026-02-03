package com.hytale2mc.plugin.std.hytale

import com.hypixel.hytale.protocol.packets.interface_.HudComponent.AmmoIndicator
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.BlockVariantSelector
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.BuilderToolsLegend
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.Compass
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.Health
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.InputBindings
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.Mana
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.ObjectivePanel
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.Oxygen
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.PortalPanel
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.Sleep
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.Stamina
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.StatusIcons
import com.hypixel.hytale.protocol.packets.interface_.HudComponent.UtilitySlotSelector
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.PlatformEntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.hytale.HytalePlatform
import com.hytale2mc.ecs.text.raw
import com.hytale2mc.plugin.std.StdBlockType
import com.hytale2mc.plugin.std.StdEntityType
import com.hytale2mc.plugin.std.StdItemType
import com.hytale2mc.plugin.std.StdPlatformHandler
import com.hytale2mc.plugin.std.StdRenderable
import com.hytale2mc.plugin.std.StdSoundType
import korlibs.math.geom.Vector3I
import kotlin.collections.set

data object StdHytale : StdPlatformHandler<PlatformEntityId, ItemStack, HytalePlatform> {

    override fun init(platform: HytalePlatform) {}

    override fun render(
        platform: HytalePlatform,
        renderable: StdRenderable
    ) {}

    override fun spawn(
        platform: HytalePlatform,
        entityId: EntityId,
        entityType: StdEntityType
    ): Pair<PlatformEntityId, EntityProperties?> {
        return when (entityType) {
            is StdEntityType.Player -> {
                if (entityType.platform.platformId == platform.platformId && !platform.space.isReplaying) {
                    val ref = platform.tempPlatformEntityId2Ref.remove(entityType.platformEntityIdOnOrigin)!!

                    val playerRef = ref.store.getComponent(ref, PlayerRef.getComponentType())!!
                    val player = ref.store.getComponent(ref, Player.getComponentType())!!
                    player.hudManager.hideHudComponents(
                        playerRef,
                        Compass, Health, Stamina, Oxygen, StatusIcons, AmmoIndicator,
                        InputBindings, PortalPanel, Sleep, Mana, BlockVariantSelector,
                        ObjectivePanel, UtilitySlotSelector, BuilderToolsLegend,
                    )
                    val entityStats = ref.store.getComponent(ref, EntityStatMap.getComponentType())!!
                    entityStats.setStatValue(EntityStatType.getAssetMap().getIndex("Health"), 50f)
                    platform.entityId2Ref[entityId] = ref
                    entityType.platformEntityIdOnOrigin
                } else {
                    platform.newHytaleEntity(entityId, replicaPlayerModel, isPlayer = true)
                }
            }
            StdEntityType.FakePlayer -> {
                platform.newHytaleEntity(entityId, replicaPlayerModel, isPlayer = true)
            }
            is StdEntityType.Text -> {
                platform.newHytaleEntity(
                    entityId,
                    itemStack = invisibleModel,
                    name = entityType.text.raw(),
                    entityScale = 0.00001f,
                    animateDroppedItem = false
                )
            }
        } to null
    }

    private val replicaPlayerModel by lazy { Model.createUnitScaleModel(
        ModelAsset.getAssetMap().getAsset("Player")!!
    ) }

    private val invisibleModel by lazy {
        ItemStack("Barrier")
    }

    override fun setBlock(
        platform: HytalePlatform,
        position: Vector3I,
        blockType: StdBlockType
    ) {
        throw IllegalStateException()
    }

    override fun createItem(
        platform: HytalePlatform,
        itemType: StdItemType
    ): ItemStack {
        throw IllegalStateException()
    }

    override fun getSoundData(soundType: StdSoundType): List<SoundData> {
        throw IllegalStateException()
    }

    override fun onRemove(platformEntity: PlatformEntityId) {

    }
}