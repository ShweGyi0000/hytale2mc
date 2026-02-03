package com.hytale2mc.ecs.data.container

import kotlin.reflect.KClass

class TypedContainer<T : Any> : Container<T>() {

    val contained = mutableMapOf<KClass<out T>, T>()

    fun <C : T> get(type: KClass<C>): C? {
        return contained[type] as? C
    }

    override fun contained(): Collection<T> {
        return contained.values
    }

    fun add(value: T) {
        check(!contained.containsKey(value::class))
        contained[value::class] = value
    }

    fun <C : T> remove(type: KClass<C>): C? {
        return contained.remove(type) as? C
    }

}