package com.hytale2mc.game.aimtrainer

import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hytale2mc.ecs.platform.hytale.HytalePlatformPlugin
import com.hytale2mc.ecs.space.Space

class AimTrainerHytale(
    init: JavaPluginInit
) : HytalePlatformPlugin(init) {
    override val space: Space = aimTrainerStarter.replica()
}