package com.hytale2mc.ecs.platform.hytale

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.protocol.ChangeVelocityType
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.protocol.Packet
import com.hypixel.hytale.protocol.SoundCategory
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.io.PacketHandler
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.physics.component.Velocity
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.SoundUtil
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hytale2mc.ecs.data.event.*
import com.hytale2mc.ecs.platform.PlatformEventHandler
import korlibs.math.geom.Vector3F

internal class HytaleEventHandler(
    platform: HytalePlatform
) : PlatformEventHandler<HytalePlatform>(platform), PacketWatcher {

    override fun onStart() {
        PacketAdapters.registerInbound(this)
        platform.plugin.eventRegistry.registerGlobal(PlayerChatEvent::class.java) { event ->
            event.isCancelled = true

            platform.world.execute {
                val networkId = event.sender.reference?.getPlatformEntityId()
                if (networkId == null) {
                    return@execute
                }
                val entityId = platform.platformEntityId2EntityId[networkId] ?: return@execute
                write(
                    PlayerMessageEvent(
                        sender = entityId,
                        message = event.content
                    )
                )

            }
        }

    }

    override fun accept(packetHandler: PacketHandler, packet: Packet) {
        if (packet.id != 290) {
            return
        }
        val interactionChains = packet as SyncInteractionChains
        val updates = interactionChains.updates
        for (item in updates) {
            if (item.interactionType != InteractionType.Primary) {
                continue
            }
            platform.world.execute {
                val ref = platform.world.entityStore.getRefFromUUID(packetHandler.auth?.uuid ?: return@execute) ?: return@execute
                if (!ref.isValid) {
                    return@execute
                }
                val store = platform.world.entityStore.store
                val platformEntityId = store.getComponent(ref, PlatformEntityComponent.type)?.id ?: return@execute
                val entityId = platform.platformEntityId2EntityId[platformEntityId] ?: return@execute
                val targetNetworkId = item.data?.entityId
                val inventorySlot = store.getComponent(ref, Player.getComponentType())?.inventory?.activeHotbarSlot?.toInt() ?: 0
                val forward = item.data?.hitNormal
                    ?.let { Vector3F(it.x, it.y, it.z) }
                    ?: store.getComponent(ref, HeadRotation.getComponentType())?.rotation?.toHytale2mcDirection() ?: return@execute
                if (targetNetworkId == null) {
                    write(
                        EntityInputEvent(
                            entityId,
                            Click.AtNothing(inventorySlot, forward),
                        )
                    )
                    return@execute
                }

                val targetRef = platform.world.entityStore.getRefFromNetworkId(targetNetworkId) ?: return@execute
                if (!targetRef.isValid) {
                    return@execute
                }
                val targetPlatformEntityId = platform.world.entityStore.store.getComponent(
                    targetRef,
                    PlatformEntityComponent.type
                )?.id ?: return@execute
                val targetEntityId = platform.platformEntityId2EntityId[targetPlatformEntityId] ?: return@execute
                write(
                    EntityInputEvent(
                        entityId,
                        Click.AtEntity(
                            inventorySlot,
                            forward,
                            targetEntityId
                        )
                    )
                )
            }
        }
    }

    override fun handle(event: Event.Ecs2Platform) {
        when (event) {
            is ECSModifyVelocityEvent -> {
                if (platform.space.isReplaying) {
                    return
                }
                platform.world.execute {
                    val ref = platform.entityId2Ref[event.entityId] ?: return@execute
                    val platformId = platform.entityId2PlatformEntityId[event.entityId] ?: return@execute
                    if (platformId !in platform.owningPlayersPlatformIds) {
                        return@execute
                    }
                    if (ref.isValid) {
                        val velocity = ref.store.getComponent(ref, Velocity.getComponentType())
                        val changeType = when (event.action) {
                            ADD -> ChangeVelocityType.Add
                            SET -> ChangeVelocityType.Set
                        }
                        velocity?.instructions?.add(
                            Velocity.Instruction(
                                event.velocity.times(0.85f).toVector3d(),
                                null,
                                changeType
                            )
                        )
                    }
                }
            }
            is ECSSoundEvent -> {
                platform.world.execute {
                    val plugin = platform.space.plugins.getValue(event.sound.owner)
                    val soundDatas = platform.getPlatformHandler(plugin).getSoundDataUnsafe(event.sound)
                    val indices = soundDatas.map { SoundEvent.getAssetMap().getIndex(it.id) }
                    val soundCategory = SoundCategory.SFX
                    when (val receiver = event.receiver) {
                        is ECSSoundEvent.SoundReceiver.Personal -> {
                            for (e in receiver.entityIds) {
                                val ref = platform.entityId2Ref[e] ?: continue
                                if (!ref.isValid) {
                                    continue
                                }
                                val playerRef = ref.store.getComponent(ref, PlayerRef.getComponentType()) ?: continue
                                for (index in indices) {
                                    SoundUtil.playSoundEvent2dToPlayer(playerRef, index, soundCategory)
                                }
                            }
                        }
                        is ECSSoundEvent.SoundReceiver.Position -> {
                            for (index in indices) {
                                SoundUtil.playSoundEvent3d(
                                    index,
                                    SoundCategory.SFX,
                                    receiver.position.toVector3d(),
                                    platform.world.entityStore.store
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

fun Ref<EntityStore>.getPlatformEntityId(): Int? {
    if (!isValid) return null
    return store.getComponent(this, PlatformEntityComponent.type)?.id
}