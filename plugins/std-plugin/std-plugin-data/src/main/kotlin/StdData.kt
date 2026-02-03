package com.hytale2mc.plugin.std

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.operation.DataOperation
import com.hytale2mc.ecs.data.operation.PlayerJoined
import com.hytale2mc.ecs.plugin.PluginKey
import com.hytale2mc.ecs.plugin.PluginPlatformHandler
import com.hytale2mc.ecs.text.TextPart
import kotlinx.serialization.Serializable

typealias StdPlatformHandler<P_E, P_I, P> = PluginPlatformHandler<StdKey, P_E, P_I, P, StdRenderable, StdEntityType, StdBlockType, StdItemType, StdSoundType>

@Serializable
data object StdKey : PluginKey<StdKey>

@Serializable
sealed interface StdRenderable : ECSDynamic.Renderable<StdKey>
@Serializable
sealed interface StdEntityType : ECSDynamic.EntityType<StdKey> {

    override val owner
        get() = StdKey

    @Serializable
    data class Player(
        val username: String,
        val platformEntityIdOnOrigin: EntityId,
        val platform: DataOperation.Source.Platform
    ) : StdEntityType {

        override val isRealPlayer: Boolean = true

        constructor(playerJoined: PlayerJoined) : this(
            playerJoined.username,
            playerJoined.platformEntityIdOnOrigin,
            playerJoined.platform
        )

    }

    @Serializable
    data object FakePlayer : StdEntityType

    @Serializable
    data class Text(
        val text: List<TextPart>,
        val billboardConstraints: BillboardConstraints = CENTER
    ) : StdEntityType {

        enum class BillboardConstraints {
            FIXED,
            CENTER
        }

    }

}

@Serializable
sealed interface StdBlockType : ECSDynamic.BlockType<StdKey>
@Serializable
sealed interface StdItemType : ECSDynamic.ItemType<StdKey>
@Serializable
sealed interface StdSoundType : ECSDynamic.SoundType<StdKey>