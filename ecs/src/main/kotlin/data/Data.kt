package com.hytale2mc.ecs.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.properties.Delegates

typealias DataId = String

typealias EntityId = Int
typealias PlatformEntityId = Int

@Serializable
abstract class Data {

    @Transient
    var addedAt by Delegates.notNull<Long>()
    @Transient
    var changedAt: Long? = null

}