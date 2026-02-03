package com.hytale2mc.plugin.aimtrainer

import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.plugin.PluginKey
import com.hytale2mc.ecs.plugin.PluginPlatformHandler
import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

typealias AimTrainerPlatformHandler<P_E, P_I, P> = PluginPlatformHandler<AimTrainerKey, P_E, P_I, P, AimTrainerRenderable, AimTrainerEntityType, AimTrainerBlockType, AimTrainerItemType, AimTrainerSoundType>
@Serializable
data object AimTrainerKey : PluginKey<AimTrainerKey>

@Serializable
sealed interface AimTrainerBlockType : ECSDynamic.BlockType<AimTrainerKey> {

    override val owner: AimTrainerKey
        get() = AimTrainerKey

    @Serializable
    data object Floor : AimTrainerBlockType
    @Serializable
    data object Wall : AimTrainerBlockType

}
@Serializable
sealed interface AimTrainerEntityType : ECSDynamic.EntityType<AimTrainerKey> {

    override val owner: AimTrainerKey
        get() = AimTrainerKey

    @Serializable
    data object Target : AimTrainerEntityType

}
@Serializable
sealed interface AimTrainerItemType : ECSDynamic.ItemType<AimTrainerKey> {

    override val owner: AimTrainerKey
        get() = AimTrainerKey

    @Serializable
    data object Gun : AimTrainerItemType

}
@Serializable
sealed interface AimTrainerRenderable : ECSDynamic.Renderable<AimTrainerKey> {

    override val owner: AimTrainerKey
        get() = AimTrainerKey

    @Serializable
    data class TargetKilled(
        val at: @Contextual Vector3F,
    ) : AimTrainerRenderable

}


@Serializable
sealed interface AimTrainerSoundType : ECSDynamic.SoundType<AimTrainerKey> {

    override val owner: AimTrainerKey
        get() = AimTrainerKey

    @Serializable
    data object Hit : AimTrainerSoundType

}


@Serializable
class DeadComponent : Component.EcsOnly()
@Serializable
class TargetComponent : Component.EcsOnly()
