package com.hytale2mc.ecs.plugin

import com.hytale2mc.ecs.composer.ComponentComposer
import com.hytale2mc.ecs.composer.EventComposer
import com.hytale2mc.ecs.composer.MapComposer
import com.hytale2mc.ecs.composer.ResourceComposer
import com.hytale2mc.ecs.composer.SpaceComposer
import com.hytale2mc.ecs.composer.StateComposer
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.platform.PlatformType
import kotlin.reflect.KClass

@ConsistentCopyVisibility
data class ECSPlugin<
        KEY : PluginKey<KEY>,
        R : ECSDynamic.Renderable<KEY>,
        E : ECSDynamic.EntityType<KEY>,
        B : ECSDynamic.BlockType<KEY>,
        I : ECSDynamic.ItemType<KEY>,
        S : ECSDynamic.SoundType<KEY>
        > @PublishedApi internal constructor(
    val key: KEY,
    val dependencies: Set<PluginKey<*>>,
    val keyType: KClass<KEY>,
    val rendererType: KClass<R>,
    val entityType: KClass<E>,
    val blockType: KClass<B>,
    val itemType: KClass<I>,
    val soundType: KClass<S>,
    val platformHandlers: Map<PlatformType, PluginPlatformHandler<KEY, *, *, *, R, E, B, I, S>>,
    val components: ComponentComposer.() -> Unit,
    val resources: ResourceComposer.() -> Unit,
    val states: StateComposer.() -> Unit,
    val events: EventComposer.() -> Unit,
    val maps: MapComposer.() -> Unit,
    val systems: SpaceComposer.() -> Unit
)

inline fun <
        reified KEY : PluginKey<KEY>,
        reified R : ECSDynamic.Renderable<KEY>,
        reified E : ECSDynamic.EntityType<KEY>,
        reified B : ECSDynamic.BlockType<KEY>,
        reified I : ECSDynamic.ItemType<KEY>,
        reified S : ECSDynamic.SoundType<KEY>
> ecsPlugin(
    key: KEY,
    handlers: HandlersBuilder<KEY, R, E, B, I, S>.() -> Unit,
    dependencies: Set<PluginKey<*>> = emptySet(),
    noinline components: ComponentComposer.() -> Unit = {},
    noinline resources: ResourceComposer.() -> Unit = {},
    noinline states: StateComposer.() -> Unit = {},
    noinline events: EventComposer.() -> Unit = {},
    noinline maps: MapComposer.() -> Unit = {},
    noinline systems: SpaceComposer.() -> Unit = {}
): ECSPlugin<KEY, R, E, B, I, S> = ECSPlugin(
    key,
    dependencies,
    KEY::class,
    R::class,
    E::class,
    B::class,
    I::class,
    S::class,
    HandlersBuilder<KEY, R, E, B, I, S>().apply(handlers).build(),
    components, resources, states,
    events, maps, systems
)

class HandlersBuilder<
        KEY : PluginKey<KEY>,
        R : ECSDynamic.Renderable<KEY>,
        E : ECSDynamic.EntityType<KEY>,
        B : ECSDynamic.BlockType<KEY>,
        I : ECSDynamic.ItemType<KEY>,
        S : ECSDynamic.SoundType<KEY>
        > {

    private var minecraft: PluginPlatformHandler<KEY, *, *, *, R, E, B, I, S>? = null
    private var hytale: PluginPlatformHandler<KEY, *, *, *, R, E, B, I, S>? = null

    fun minecraft(init: () -> PluginPlatformHandler<KEY, *, *, *, R, E, B, I, S>) {
        runCatching { minecraft = init() }
    }

    fun hytale(init: () -> PluginPlatformHandler<KEY, *, *, *, R, E, B, I, S>) {
        runCatching { hytale = init() }
    }

    fun build(): Map<PlatformType, PluginPlatformHandler<KEY, *, *, *, R, E, B, I, S>> = buildMap {
        minecraft?.let { put(MINECRAFT, it) }
        hytale?.let { put(HYTALE, it) }
    }

}