package com.hytale2mc.ecs.composer

import com.hytale2mc.ecs.data.ECSMap
import com.hytale2mc.ecs.data.loadMapFromResources
import korlibs.math.geom.Vector3I
import kotlinx.serialization.json.Json

@ComposerDsl
class MapComposer() {

    internal val maps = mutableMapOf<String, (Json) -> ECSMap>()

    fun fromResources(
        mapFileNameWithoutExt: String,
        center: Vector3I
    ) {
        val old = maps.put(mapFileNameWithoutExt) { json ->
            loadMapFromResources(mapFileNameWithoutExt, center, json)
        }
        check(old == null) { "Map with id '$mapFileNameWithoutExt' already registered." }
    }

    fun preBuilt(mapId: String, map: ECSMap) {
        val old = maps.put(mapId) { map }
        check(old == null) { "Map with id '$mapId' already registered." }
    }

}