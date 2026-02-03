package com.hytale2mc.ecs.data.component

import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntityTypeComponent(
    @SerialName("entityType")
    val type: ECSDynamic.EntityType<*>
) : Component.EcsOnly()