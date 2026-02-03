package com.hytale2mc.ecs.data.container

abstract class Container<T : Any>(
) {

    abstract fun contained(): Collection<T>

}