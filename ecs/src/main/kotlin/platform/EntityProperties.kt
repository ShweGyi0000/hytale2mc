package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.EntityId
import korlibs.math.geom.Vector3F

sealed interface EntityProperty<T : Any> {

    val original: T
    val key: Key<T>

    data class Scale(
        override val original: Vector3F,
    ) : EntityProperty<Vector3F> {

        override val key: Key<Vector3F> = Scale

        companion object : Key<Vector3F> {
            override val default: Vector3F = Vector3F.ONE
        }

    }

    data class Translation(
        override val original: Vector3F
    ) : EntityProperty<Vector3F> {
        override val key: Key<Vector3F> = Translation
        companion object : Key<Vector3F> {
            override val default: Vector3F = Vector3F.ZERO
        }
    }

    data class NametagTranslation(
        override val original: Vector3F
    ) : EntityProperty<Vector3F> {
        override val key: Key<Vector3F> = NametagTranslation
        companion object : Key<Vector3F> {
            override val default: Vector3F = Vector3F(0f, 0.15f, 0f)
        }
    }

    sealed interface Key<T : Any> {

        val default: T

    }

}

fun <T : Any> Platform<*, *>.getOriginalProperty(
    entityId: EntityId,
    key: EntityProperty.Key<T>
): T {
    return (originalProperties[entityId]?.get(key)?.original ?: key.default) as T
}