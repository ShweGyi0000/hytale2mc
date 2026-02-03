package com.hytale2mc.game.wordguesser

import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hytale2mc.ecs.platform.hytale.HytalePlatformPlugin
import com.hytale2mc.ecs.space.Space

class WordGuesserHytale(
    init: JavaPluginInit
) : HytalePlatformPlugin(init) {
    override val space: Space = wordGuesserStarter.replica()
}