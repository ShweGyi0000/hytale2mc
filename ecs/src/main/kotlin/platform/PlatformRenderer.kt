package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.ECSMap
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.operation.RenderingCommand
import com.hytale2mc.ecs.plugin.PluginPlatformHandler
import korlibs.math.geom.Vector3I

class PlatformRenderer(
    val platform: Platform<*, *>
) {

    private fun <P : Platform<*, *>, R : ECSDynamic.Renderable<*>> PluginPlatformHandler<*, *, *, P, R, *, *, *, *>.renderUnsafe(
        renderable: ECSDynamic.Renderable<*>
    ) {
        render(platform as P, renderable as R)
    }

    fun render(command: RenderingCommand) {
        return when (command) {
            is RenderingCommand.Render -> {
                for (renderable in command.renderables) {
                    val plugin = platform.space.plugins.getValue(renderable.owner)
                    platform.getPlatformHandler(plugin).renderUnsafe(renderable)
                }
            }
        }
    }

    private fun <P : Platform<*, *>, B : ECSDynamic.BlockType<*>> PluginPlatformHandler<*, *, *, P, *, *, B, *, *>.setBlockUnsafe(
        position: Vector3I,
        blockType: ECSDynamic.BlockType<*>
    ) {
        setBlock(platform as P, position, blockType as B)
    }

    fun setBlock(
        position: Vector3I,
        blockType: ECSDynamic.BlockType<*>
    ) {
        val plugin = platform.space.plugins.getValue(blockType.owner)
        platform.getPlatformHandler(plugin).setBlockUnsafe(position, blockType)
    }

    fun pasteMap(map: ECSMap) {
        for ((pos, block) in map.blocks) {
            setBlock(pos, block.blockType)
        }
    }


}