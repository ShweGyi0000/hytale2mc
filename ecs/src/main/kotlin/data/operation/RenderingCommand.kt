package com.hytale2mc.ecs.data.operation

import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import kotlinx.serialization.Serializable

@Serializable
sealed interface RenderingCommand : Command<Unit> {

    @Serializable
    data class Render(
        override val commandId: String,
        val renderables: List<ECSDynamic.Renderable<*>>
    ) : RenderingCommand {

        constructor(commandId: String, renderable: ECSDynamic.Renderable<*>) : this(commandId, listOf(renderable))

    }

}