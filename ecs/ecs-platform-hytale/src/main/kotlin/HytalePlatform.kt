package com.hytale2mc.ecs.platform.hytale

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.protocol.MovementStates
import com.hypixel.hytale.protocol.PlayerSkin
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.Intangible
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.PlatformEntityId
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.operation.DataCommand
import com.hytale2mc.ecs.platform.Platform
import com.hytale2mc.ecs.platform.PlatformEntitySpawner
import com.hytale2mc.ecs.platform.PlatformEventHandler
import com.hytale2mc.ecs.space.Space
import korlibs.time.seconds
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.get
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.toJavaInstant

class HytalePlatform(
    space: Space,
    val plugin: HytalePlatformPlugin
) : Platform<PlatformEntityId, ItemStack>(HYTALE, space) {

    var initialized = false
    lateinit var world: World

    val tempPlatformEntityId2Ref = mutableMapOf<PlatformEntityId, Ref<EntityStore?>>()
    val entityId2Ref = mutableMapOf<EntityId, Ref<EntityStore?>>()
    val platformEntityId = AtomicInteger(0)

    override val componentHandler = HytaleComponentHandler(this)
    override val eventHandler: PlatformEventHandler<*> = HytaleEventHandler(this)

    override fun onStart() {

    }

    override fun shutdown() {
        val universe = Universe.get()
        universe.disconnectAllPLayers()
        universe.shutdownAllWorlds()
        exitProcess(0)
    }

    private val playerSkin: PlayerSkin
        get() = PlayerSkin().apply {
            haircut = "Witch.Copper"
            eyebrows = "Thin.Copper"
            eyes = "Large_Eyes.Green"
            underwear = "Boxer.Blue"
            face = "Face_Neutral_Freckles"
            undertop = "Short_Sleeves_Shirt.Lime"
            pants = "BulkySuede.Brown"
            shoes = "Boots_Long.Grey"
            mouth = "Mouth_Thin"
            ears = "Default"
            bodyCharacteristic = "Default.02"
        }

    fun newHytaleEntity(
        entityId: EntityId,
        model: Model? = null,
        itemStack: ItemStack? = null,
        entityScale: Float = 1f,
        name: String? = null,
        animateDroppedItem: Boolean = true,
        isPlayer: Boolean = false
    ): PlatformEntityId {
        val holder = EntityStore.REGISTRY.newHolder()
        val platformEntityId = platformEntityId.getAndIncrement()

        holder.addComponent(NetworkId.getComponentType(), NetworkId(world.entityStore.takeNextNetworkId()))
        holder.ensureComponent(UUIDComponent.getComponentType())
        holder.addComponent(
            PlatformTransformComponent.getComponentType(),
            PlatformTransformComponent(
                Vector3d(15.0, 17.0, -9.0),
                Vector3f(0f, 0f, 0f)
            )
        )
        holder.addComponent(HeadRotation.getComponentType(), HeadRotation())
        holder.addComponent(HitboxCollision.getComponentType(), HitboxCollision(
            HitboxCollisionConfig(
                "SoftCollision"
            )
        ))
        if (model != null) {
            val model = if (isPlayer) {
                val playerSkin = playerSkin
                holder.addComponent(PlayerSkinComponent.getComponentType(), PlayerSkinComponent(playerSkin))
                CosmeticsModule.get().createModel(playerSkin)
            } else {
                model
            }
            holder.addComponent(ModelComponent.getComponentType(), ModelComponent(model))
            holder.addComponent(PersistentModel.getComponentType(), PersistentModel(model!!.toReference()))
        }

        if (itemStack != null) {
            itemStack.setOverrideDroppedItemAnimation(!animateDroppedItem)
            holder.addComponent(ItemComponent.getComponentType(), ItemComponent(itemStack))
            holder.addComponent<PreventPickup?>(PreventPickup.getComponentType(), PreventPickup.INSTANCE)
            holder.addComponent<PreventItemMerging?>(
                PreventItemMerging.getComponentType(),
                PreventItemMerging.INSTANCE
            )
            holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE)
        }

        if (name != null) {
            holder.addComponent(Nameplate.getComponentType(), Nameplate(name))
        }

        holder.addComponent(EntityScaleComponent.getComponentType(), EntityScaleComponent(entityScale))
        holder.addComponent(DisplayNameComponent.getComponentType(), DisplayNameComponent(Message.raw("")))

        holder.addComponent(PlatformEntityComponent.type, PlatformEntityComponent(platformEntityId, MovementStates()))

        val ref = world.entityStore.store.addEntity(
            holder,
            AddReason.SPAWN
        )!!
        entityId2Ref[entityId] = ref
        return platformEntityId
    }

    override fun spawnPlatformEntity(
        entityId: EntityId,
        entityType: ECSDynamic.EntityType<*>?
    ): PlatformEntitySpawner {
        val platformEntityId = when (entityType) {
            null -> {
                newHytaleEntity(entityId)
            }
            else -> {
                val plugin = space.plugins.getValue(entityType.owner)
                val handler = getPlatformHandler(plugin)
                handler.spawnUnsafe(entityId, entityType).first
            }
        }

        return PlatformEntitySpawner(
            platformEntityId, null, {}
        )
    }

    override fun removePlatformEntity(entityId: EntityId) {
        val ref = entityId2Ref.remove(entityId)!!
        if (ref.isValid) {
            world.entityStore.store.putComponent(
                ref,
                DespawnComponent.getComponentType(),
                DespawnComponent(kotlin.time.Instant.DISTANT_PAST.toJavaInstant())
            )
        }
    }

    override fun getLatency(playerPlatformId: PlatformEntityId): Duration {
        // TODO
        return 0.seconds
    }

    override fun transfer(
        playerPlatformId: PlatformEntityId,
        destination: DataCommand.TransferPlayers.Destination
    ) {
        val entityId = platformEntityId2EntityId[playerPlatformId]
        val ref = entityId2Ref[entityId]!!
        val playerRef = ref.store.getComponent(ref, PlayerRef.getComponentType())!!
        playerRef.referToServer(destination.host, destination.port)
    }

}