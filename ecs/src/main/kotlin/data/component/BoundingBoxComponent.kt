package com.hytale2mc.ecs.data.component

import korlibs.math.geom.AABB3D
import korlibs.math.geom.Vector3F
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class BoundingBoxComponent(
    @Contextual
    val min: Vector3F,
    @Contextual
    val max: Vector3F
) : Component.Ecs2PlatformOnly() {
}

fun aabb3d(
    box: BoundingBoxComponent,
    transform: TransformComponent
): AABB3D {
    return AABB3D(
        (box.min * transform.scale) + transform.translation,
        (box.max * transform.scale) + transform.translation
    )
}

fun intersects(
    box1: BoundingBoxComponent,
    transform1: TransformComponent,
    box2: BoundingBoxComponent,
    transform2: TransformComponent
): Boolean {
    val first = aabb3d(box1, transform1)
    val second = aabb3d(box2, transform2)

    return first.intersectsAABB(second)
}