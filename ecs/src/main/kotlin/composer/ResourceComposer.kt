package com.hytale2mc.ecs.composer

import com.hytale2mc.ecs.data.Resource
import kotlin.reflect.KClass

typealias ComposedResources = Map<KClass<out Resource>, Resource>

@ComposerDsl
class ResourceComposer internal constructor() {

    internal val resources = mutableMapOf<KClass<out Resource>, Resource>()

    fun <R : Resource> resource(initial: R) {
        check(!resources.containsKey(initial::class)) { "Resource $initial was already registered" }
        resources[initial::class] = initial
    }

}