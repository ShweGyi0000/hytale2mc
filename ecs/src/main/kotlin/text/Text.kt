package com.hytale2mc.ecs.text

import com.github.ajalt.colormath.model.RGB
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable


typealias Text = List<TextPart>

@Serializable
data class TextPart(
    val text: String,
    @Contextual
    val color: RGB? = null
)

fun Text.raw(): String {
    return joinToString("") { it.text }
}