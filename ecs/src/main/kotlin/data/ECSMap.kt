package com.hytale2mc.ecs.data

import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.math.serializers.Vector3ISerializer
import korlibs.math.geom.Vector3I
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

data class ECSMap(
    val blocks: Map<Vector3I, ECSBlock>
) {

    val hittableBlocks = blocks
        .filterValues { it.blockType.hasCollision }

    constructor(blocks: Collection<ECSBlock>) : this(blocks.associateBy { it.pos })

}

@Serializable
data class ECSBlock(
    @Serializable(with = Vector3ISerializer::class)
    val pos: Vector3I,
    val blockType: ECSDynamic.BlockType<*>,
)

var classLoader: ClassLoader? = null
@OptIn(ExperimentalSerializationApi::class)
internal fun loadMapFromResources(
    mapId: String,
    center: Vector3I,
    json: Json
): ECSMap {
    return (classLoader ?: Thread.currentThread().contextClassLoader).getResourceAsStream("maps/$mapId.json")!!.use { stream ->
        json.decodeFromStream<Set<ECSBlock>>(stream)
            .map { it.copy(pos = Vector3I(it.pos.x + center.x, it.pos.y + center.y, it.pos.z + center.z)) }
            .associateBy { it.pos }
            .let { ECSMap(it) }
    }
}