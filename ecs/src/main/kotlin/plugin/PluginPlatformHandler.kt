package com.hytale2mc.ecs.plugin

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.Platform
import korlibs.math.geom.Vector3I

interface PluginPlatformHandler<
        KEY : PluginKey<KEY>,
        P_E : Any,
        P_I : Any,
        P : Platform<P_E, P_I>,
        R : ECSDynamic.Renderable<KEY>,
        E : ECSDynamic.EntityType<KEY>,
        B : ECSDynamic.BlockType<KEY>,
        I : ECSDynamic.ItemType<KEY>,
        S : ECSDynamic.SoundType<KEY>
        > {

    fun init(platform: P)
    fun render(platform: P, renderable: R)
    fun spawn(platform: P, entityId: EntityId, entityType: E): Pair<P_E, EntityProperties?>
    fun setBlock(platform: P, position: Vector3I, blockType: B)
    fun createItem(platform: P, itemType: I): P_I
    fun getSoundData(soundType: S): List<SoundData>

    fun onRemove(platformEntity: P_E)

}
