package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.event.Event
import com.hytale2mc.ecs.data.operation.EventWritten
import com.hytale2mc.ecs.plugin.PluginPlatformHandler

abstract class PlatformEventHandler<P : Platform<*, *>>(
    val platform: P
) {

    abstract fun onStart()
    abstract fun handle(event: Event.Ecs2Platform)

    fun <E : Event.Platform2Ecs> write(
        event: E
    ) {
        platform.space.operations.add(EventWritten(event))
    }

    fun <S : ECSDynamic.SoundType<*>> PluginPlatformHandler<*, *, *, *, *, *, *, *, S>.getSoundDataUnsafe(
        soundType: ECSDynamic.SoundType<*>
    ): List<SoundData> {
        return getSoundData(soundType as S)
    }

}