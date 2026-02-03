package com.hytale2mc.ecs.platform.minestom

import com.hytale2mc.ecs.data.event.*
import com.hytale2mc.ecs.platform.PlatformEventHandler
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent
import net.minestom.server.network.packet.server.play.ExplosionPacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.CustomSoundEvent
import net.minestom.server.utils.WeightedList

class MinestomEventHandler(
    platform: MinestomPlatform
) : PlatformEventHandler<MinestomPlatform>(platform) {

    override fun onStart() {
        platform.instance.eventNode().addListener(PlayerChatEvent::class.java) { event ->
            event.isCancelled = true
            val entityId = platform.platformEntityId2EntityId[event.player.entityId] ?: return@addListener
            write(
                PlayerMessageEvent(
                    sender = entityId,
                    message = event.rawMessage
                )
            )
        }

        platform.instance.eventNode().addListener(EntityAttackEvent::class.java) { event ->
            val player = event.entity
            if (player !is Player) return@addListener
            val entityId = platform.platformEntityId2EntityId[event.entity.entityId] ?: return@addListener
            val targetEntityId = platform.platformEntityId2EntityId[event.target.entityId] ?: return@addListener
            write(
                EntityInputEvent(
                    entityId,
                    Click.AtEntity(
                        player.heldSlot.toInt(),
                        event.entity.position.direction().toVector3F(),
                        targetEntityId
                    ),
                )
            )
        }

        platform.instance.eventNode().addListener(PlayerUseItemOnBlockEvent::class.java) { event ->
            if (event.hand != MAIN) {
                return@addListener
            }

            val entityId = platform.platformEntityId2EntityId[event.player.entityId] ?: return@addListener
            write(
                EntityInputEvent(
                    entityId,
                    Click.AtNothing(
                        event.player.heldSlot.toInt(),
                        event.entity.position.direction().toVector3F(),
                    ),
                )
            )
        }

        platform.instance.eventNode().addListener(PlayerChangeHeldSlotEvent::class.java) { event ->
            val entityId = platform.platformEntityId2EntityId[event.player.entityId] ?: return@addListener
            write(
                HeldItemSlotChangeEvent(
                    entityId,
                    from = event.oldSlot.toInt(),
                    to = event.newSlot.toInt(),
                )
            )
        }
    }

    override fun handle(event: Event.Ecs2Platform) {
        when (event) {
            is ECSSoundEvent -> handleSoundEvent(event)
            is ECSModifyVelocityEvent -> handleAddVelocity(event)
        }
    }

    private val silentSound = CustomSoundEvent(Key.key("hytale2mc", "silent"), 0f)
    private fun handleAddVelocity(event: ECSModifyVelocityEvent) {
        val platformEntity = platform.entityIdToPlatformEntity.getValue(event.entityId)
        when {
            platformEntity.entityType == MANNEQUIN -> {}
            platformEntity is MinestomPlayer -> {
                when (event.action) {
                    ECSModifyVelocityEvent.Action.ADD -> {
                        platformEntity.sendPacket(
                            ExplosionPacket(
                                Vec.ZERO,
                                0f,
                                0,
                                event.velocity.times(0.05f).toVec(),
                                Particle.FLAME,
                                silentSound,
                                WeightedList(emptyList())
                            )
                        )
                    }
                    ECSModifyVelocityEvent.Action.SET -> {
                        platformEntity.velocity = event.velocity.toVec()
                    }
                }

            }
            platform.space.origin == PRIMARY && !platform.space.isReplaying -> {
                val newVelocity = when (event.action) {
                    ECSModifyVelocityEvent.Action.ADD -> platformEntity.velocity.add(event.velocity.toVec())
                    ECSModifyVelocityEvent.Action.SET -> event.velocity.toVec()
                }
                platformEntity.velocity = newVelocity
            }
        }
    }

    private fun handleSoundEvent(event: ECSSoundEvent) {
        val plugin = platform.space.plugins.getValue(event.sound.owner)
        val soundData = platform.getPlatformHandler(plugin).getSoundDataUnsafe(event.sound)

        val sounds = soundData.map {
            Sound.sound(
                /* name = */ Key.key(it.id),
                /* source = */ Sound.Source.PLAYER,
                /* volume = */ it.volume,
                /* pitch = */ it.pitch
            )
        }

        when (val receiver = event.receiver) {
            is ECSSoundEvent.SoundReceiver.Personal -> {
                for (entityId in receiver.entityIds) {
                    val platformEntity = platform.entityIdToPlatformEntity.getValue(entityId)
                    if (platformEntity !is Audience) {
                        continue
                    }
                    for (sound in sounds) {
                        platformEntity.playSound(sound, Sound.Emitter.self())
                    }
                }
            }
            is ECSSoundEvent.SoundReceiver.Position -> {
                for (sound in sounds) {
                    platform.instance.playSound(sound, receiver.position.toVec())
                }
            }
        }
    }

}