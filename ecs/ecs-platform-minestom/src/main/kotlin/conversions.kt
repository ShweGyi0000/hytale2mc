package com.hytale2mc.ecs.platform.minestom

import com.github.ajalt.colormath.Color
import korlibs.math.geom.Vector3F
import com.hytale2mc.ecs.data.component.TransformComponent
import com.hytale2mc.ecs.text.TextPart
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.color.AlphaColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec

fun Pos.toVector3F(): Vector3F {
    return Vector3F(x, y, z)
}
fun Vec.toVector3F(): Vector3F {
    return Vector3F(x, y, z)
}

fun Vector3F.toVec(): Vec {
    return Vec(x.toDouble(), y.toDouble(), z.toDouble())
}

fun TransformComponent.toPos(): Pos {
    return Pos(
        translation.x.toDouble(),
        translation.y.toDouble(),
        translation.z.toDouble(),
    ).withDirection(direction.toVec())
}

fun Color.toPlatformColor(): AlphaColor {
    val rgb = toSRGB()
    return AlphaColor(rgb.alphaInt, rgb.redInt, rgb.greenInt, rgb.blueInt)
}

fun List<TextPart>?.toAdventure(): TextComponent {
    if (this == null) {
        return Component.empty()
    }
    var builder = Component.text()
    for (component in this) {
        builder = builder.append(
            Component.text(component.text)
                .color(component.color?.toPlatformColor()?.let { TextColor.color(it) })
        )
    }
    return builder.build()
}