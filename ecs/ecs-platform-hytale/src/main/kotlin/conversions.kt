package com.hytale2mc.ecs.platform.hytale

import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3f
import com.hypixel.hytale.server.core.Message
import com.hytale2mc.ecs.text.Text
import korlibs.math.PIF
import korlibs.math.geom.Vector3F
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun Vector3d.toVector3F(): Vector3F {
    return Vector3F(x, y, z)
}

fun Vector3F.toVector3d(): Vector3d {
    return Vector3d(x.toDouble(), y.toDouble(), z.toDouble())
}

fun Vector3F.toHytaleRotation(): Vector3f {
    val horizontalLen = sqrt(x * x + z * z)
    val pitchRad = atan2(y, horizontalLen)
    val yawRad = atan2(-x, -z)
    val rollRad = 0f
    return Vector3f(pitchRad, yawRad, rollRad)
}

fun Vector3f.toHytale2mcDirection(): Vector3F {
    val rotX = -yaw + PIF
    val rotY = -pitch
    val xz = cos(rotY)
    return Vector3F(
        -xz * sin(rotX),
        -sin(rotY),
        xz * cos(rotX)
    )
}

fun Text?.toMessage(): Message {
    if (this == null) {
        return Message.empty()
    }
    val messages = map {
        val message = Message.raw(it.text)
        when (val color = it.color) {
            null -> message
            else -> message.color(Color(color.r, color.g, color.b))
        }
    }
    return Message.join(*messages.toTypedArray())
}