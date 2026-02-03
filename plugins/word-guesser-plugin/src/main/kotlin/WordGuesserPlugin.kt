package com.hytale2mc.plugin.wordguesser

import WordGuesserMinestom
import com.hytale2mc.ecs.data.ECSBlock
import com.hytale2mc.ecs.data.ECSMap
import com.hytale2mc.ecs.plugin.ecsPlugin
import com.hytale2mc.plugin.chat.ChatKey
import korlibs.math.geom.Vector3I

val wordGuesserPlugin = ecsPlugin<WordGuesserKey, WordGuesserRenderable, WordGuesserEntityType, WordGuesserBlockType, WordGuesserItemType, WordGuesserSoundType>(
    WordGuesserKey,
    dependencies = setOf(ChatKey),
    handlers = {
        minecraft { WordGuesserMinestom }
        hytale  { WordGuesserHytale }
    },
    resources = {
        resource(WordTimer())
    },
    states = {
        state(WordGuesserState::class, WordGuesserState.Waiting)
    },
    maps = {
        preBuilt(
            "floor",
            ECSMap(
                buildList {
                    val y = 149
                    for (x in -10..10) {
                        for (z in -10..10) {
                            add(ECSBlock(Vector3I(x, y, z), WordGuesserBlockType.Floor))
                        }
                    }
                }
            )
        )
    },
    systems = {
        wordGuesserSystems()
    }
)