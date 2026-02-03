package com.hytale2mc.ecs.data

import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.component.componentId
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

data class Entity(
    val id: EntityId,
) {

    val components = ConcurrentHashMap<String, Component>()

    internal val componentVersions = mutableMapOf<String, Long>()

}

operator fun <C : Component> Entity.get(component: KClass<C>): C? {
    return components[component.componentId] as? C
}

inline fun <reified C : Component> Entity.get(): C? {
    return get(C::class)
}

fun Entity.has(component: KClass<out Component>): Boolean {
    return components.containsKey(component.componentId)
}

inline fun <reified C : Component> Entity.has(): Boolean {
    return has(C::class)
}