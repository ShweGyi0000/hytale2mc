package com.hytale2mc.ecs.data.operation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Command<R> : DataOperation {

    val commandId: String

}