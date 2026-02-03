package com.hytale2mc.ecs.data.dynamic

import com.hytale2mc.ecs.plugin.PluginKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface ECSDynamic<KEY : PluginKey<KEY>> {

    val owner: KEY

    interface EntityType<KEY : PluginKey<KEY>> : ECSDynamic<KEY> {

        val isRealPlayer: Boolean
            get() = false

    }

    interface Renderable<KEY : PluginKey<KEY>> : ECSDynamic<KEY>

    interface BlockType<KEY : PluginKey<KEY>> : ECSDynamic<KEY> {

        val hasCollision
            get() = true

    }

    interface ItemType<KEY : PluginKey<KEY>> : ECSDynamic<KEY> {

    }

    interface SoundType<KEY : PluginKey<KEY>> : ECSDynamic<KEY>

}