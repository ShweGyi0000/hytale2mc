package com.hytale2mc.game.aimtrainer

import com.hytale2mc.ecs.starter.ECSStarter
import com.hytale2mc.plugin.aimtrainer.aimTrainerPlugin
import com.hytale2mc.plugin.std.stdPlugin

val aimTrainerStarter = ECSStarter(
    "aim-trainer",
    25565,
) {
    listOf(stdPlugin, aimTrainerPlugin)
}