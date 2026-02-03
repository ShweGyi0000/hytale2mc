package com.hytale2mc.ecs.util

import korlibs.math.geom.Vector3F
import kotlin.math.cos
import kotlin.math.sin

fun directionFromYawAndPitch(yaw: Double, pitch: Double): Vector3F {
    val rotX = yaw
    val rotY = pitch
    val xz = cos(Math.toRadians(rotY))
    return Vector3F(-xz * sin(Math.toRadians(rotX)),
        -sin(Math.toRadians(rotY)),
        xz * cos(Math.toRadians(rotX))
    )
}