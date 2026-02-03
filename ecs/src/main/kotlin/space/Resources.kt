package com.hytale2mc.ecs.space

import com.hytale2mc.ecs.data.Resource
import kotlin.reflect.KClass

object Resources {

    fun addResource(
        space: Space,
        resource: Resource
    ) {
        space.resources.add(resource)
        resource.addedAt = space.tick
    }

    fun removeResource(
        space: Space,
        resource: KClass<out Resource>
    ) {
        space.resources.remove(resource)
    }

    fun replaceResource(
        space: Space,
        newResource: Resource,
        tick: Long
    ) {
        val old = space.resources.remove(newResource::class)
        check(old != null)
        space.resources.add(newResource)
        newResource.addedAt = old.addedAt
        newResource.changedAt = tick
    }

}
