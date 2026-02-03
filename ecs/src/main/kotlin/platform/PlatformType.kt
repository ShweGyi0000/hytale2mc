package com.hytale2mc.ecs.platform

import com.github.ajalt.colormath.model.RGB
import com.hytale2mc.ecs.text.TextPart

enum class PlatformType(
    val short: String,
    val color: RGB,
) {
    MINECRAFT(
        "MC",
        RGB.from255(192, 178, 175),
    ),
    HYTALE(
        "HY",
        RGB.from255(129, 176, 221),
    )
}

fun PlatformType.shortenedWithColor(): TextPart {
    return TextPart(short, color = color)
}