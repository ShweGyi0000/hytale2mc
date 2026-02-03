package com.hytale2mc.game.aimtrainer

import com.hytale2mc.ecs.platform.minestom.MinestomPlatform

fun main() {
    System.setProperty("minestom.enforce-entity-interaction-range", false.toString())
    MinestomPlatform(aimTrainerStarter.primary()).start()
}