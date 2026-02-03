package com.hytale2mc.math.serializers

import com.github.ajalt.colormath.model.RGB
import korlibs.math.geom.Quaternion
import korlibs.math.geom.Vector3F
import korlibs.math.geom.Vector3I
import kotlinx.serialization.modules.SerializersModuleBuilder

fun SerializersModuleBuilder.math() {
    contextual(Vector3F::class, Vector3FSerializer)
    contextual(Vector3I::class, Vector3ISerializer)
    contextual(Quaternion::class, QuaternionSerializer)
    contextual(RGB::class, RGBSerializer)
}