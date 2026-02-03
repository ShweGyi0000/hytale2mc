package com.hytale2mc.ecs.composer

import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.plugin.PluginKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

typealias ComposedDynamics<D> = Map<KClass<out D>, KSerializer<out D>>

@ComposerDsl
class DynamicPluginTypeComposer<
        KEY : PluginKey<KEY>,
        D : ECSDynamic<KEY>
        > {

    val types = mutableMapOf<KClass<out D>, KSerializer<out D>>()

    @OptIn(InternalSerializationApi::class)
    fun register(type: KClass<out D>) {
        types[type] = type.serializer()
    }
}