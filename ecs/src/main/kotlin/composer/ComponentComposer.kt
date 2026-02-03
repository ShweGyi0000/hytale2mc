package com.hytale2mc.ecs.composer

import com.hytale2mc.ecs.data.component.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@ComposerDsl
class ComponentComposer() {

    val components = mutableMapOf<KClass<out Component>, KSerializer<out Component>>()

    fun <C : Component> component(type: KClass<C>, serializer: KSerializer<C>) {
        registerComponentUnsafe(type, serializer)
    }

    private fun registerComponentUnsafe(type: KClass<out Component>, serializer: KSerializer<*>) {
        check(!components.containsKey(type)) { "Component ${type.qualifiedName} was already registered" }
        components[type] = serializer as KSerializer<out Component>
        Component.Registry[type] = type.qualifiedName!!
    }

    inline fun <reified C : Component> component() {
        component(C::class, serializer<C>())
    }

}