package com.hytale2mc.ecs.data.component

import com.hytale2mc.ecs.data.time.Timer
import com.hytale2mc.ecs.data.time.TimerMode
import korlibs.time.seconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration

@Serializable
data class KeepAliveComponent(
    val latency: Duration? = null,
    val wants: Boolean = false
) : Component.BiDirectional() {

    @Transient
    var responseTimer: Timer = Timer(5.seconds, TimerMode.ONCE)

}