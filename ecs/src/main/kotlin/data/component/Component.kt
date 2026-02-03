package com.hytale2mc.ecs.data.component

import com.hytale2mc.ecs.data.Data
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
sealed class Component : Data() {
    @Serializable
    sealed interface HandledByPlatform
    @Serializable
    sealed interface Platform2Ecs : HandledByPlatform
    @Serializable
    sealed interface Ecs2Platform : HandledByPlatform

    @Serializable
    sealed class Platform2EcsOnly : Component(), Platform2Ecs
    @Serializable
    sealed class Ecs2PlatformOnly : Component(), Ecs2Platform
    @Serializable
    sealed class BiDirectional : Component(), Platform2Ecs, Ecs2Platform

    @Serializable
    abstract class EcsOnly : Component()

    sealed interface HandleAfterSpawn

    companion object {

        val Registry = mutableMapOf<KClass<out Component>, String>()

    }

}

val KClass<out Component>.componentId
    get() = Component.Registry[this] ?: throw IllegalStateException("Component '$qualifiedName' missing in registry")
