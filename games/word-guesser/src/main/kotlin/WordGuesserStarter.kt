package com.hytale2mc.game.wordguesser

import com.hytale2mc.ecs.starter.ECSStarter
import com.hytale2mc.plugin.chat.chatPlugin
import com.hytale2mc.plugin.std.stdPlugin
import com.hytale2mc.plugin.wordguesser.wordGuesserPlugin

val wordGuesserStarter = ECSStarter(
    "word-guesser",
    25565,
) {
    listOf(stdPlugin, chatPlugin(), wordGuesserPlugin)
}