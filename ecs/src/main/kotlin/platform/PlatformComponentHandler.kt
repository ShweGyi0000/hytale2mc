package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.*
import com.hytale2mc.ecs.data.component.BoundingBoxComponent
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.component.FallbackServerComponent
import com.hytale2mc.ecs.data.component.GameModeComponent
import com.hytale2mc.ecs.data.component.GravityComponent
import com.hytale2mc.ecs.data.component.InputComponent
import com.hytale2mc.ecs.data.component.InventoryComponent
import com.hytale2mc.ecs.data.component.KeepAliveComponent
import com.hytale2mc.ecs.data.component.LocalTransformComponent
import com.hytale2mc.ecs.data.component.NametagComponent
import com.hytale2mc.ecs.data.component.TabComponent
import com.hytale2mc.ecs.data.component.TransformComponent
import com.hytale2mc.ecs.data.component.VisibilityComponent
import com.hytale2mc.ecs.data.component.componentId
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.plugin.PluginPlatformHandler
import com.hytale2mc.ecs.space.Space

typealias ComponentHandler<COMPONENT> = PlatformComponentHandlerContext.(COMPONENT) -> Unit

interface ComponentHandlers<COMPONENT : Component.HandledByPlatform> {

    val onAdded: ComponentHandler<COMPONENT>
    val onRemovedOrNotPresentOnSpawn: PlatformComponentHandlerContext.() -> Unit

    class BiDirectional<COMPONENT : Component.BiDirectional>(
        override val onAdded: ComponentHandler<COMPONENT>,
        override val onRemovedOrNotPresentOnSpawn: PlatformComponentHandlerContext.() -> Unit,
        val onMutatedFromEcs: ComponentHandler<COMPONENT>,
        val mutateFromPlatformIfNeeded: ComponentHandler<COMPONENT>,
    ) : ComponentHandlers<COMPONENT>

    class Ecs2Platform<COMPONENT : Component.Ecs2PlatformOnly>(
        override val onAdded: ComponentHandler<COMPONENT>,
        override val onRemovedOrNotPresentOnSpawn: PlatformComponentHandlerContext.() -> Unit,
        val onMutatedFromEcs: ComponentHandler<COMPONENT>,
    ) : ComponentHandlers<COMPONENT>

    class Platform2Ecs<COMPONENT : Component.Platform2EcsOnly>(
        override val onAdded: ComponentHandler<COMPONENT>,
        override val onRemovedOrNotPresentOnSpawn: PlatformComponentHandlerContext.() -> Unit,
        val mutateFromPlatformIfNeeded: ComponentHandler<COMPONENT>,
        val onMutatedFromEcsOnAnEntityThatWeDontOwn: ComponentHandler<COMPONENT>,
    ) : ComponentHandlers<COMPONENT>

}

data class PlatformComponentHandlerContext(
    val space: Space,
    val entity: Entity,
    val platformEntityId: PlatformEntityId
)

abstract class PlatformComponentHandler<P : Platform<*, *>>(
    val platform: P
) {

    private val keepAlive = ComponentHandlers.BiDirectional<KeepAliveComponent>(
        onAdded = {},
        onRemovedOrNotPresentOnSpawn = {},
        onMutatedFromEcs = {},
        mutateFromPlatformIfNeeded = { keepAlive ->
            if (!keepAlive.wants) {
                return@BiDirectional
            }
            platform.mutateComponent(
                space,
                entity.id,
                keepAlive.copy(latency = platform.getLatency(platformEntityId), wants = false),
            )
        },
    )

    internal val handlersById by lazy {
        mapOf(
            TransformComponent::class.componentId to transform,
            BoundingBoxComponent::class.componentId to boundingBox,
            GravityComponent::class.componentId to gravity,
            InputComponent::class.componentId to input,
            KeepAliveComponent::class.componentId to keepAlive,
            LocalTransformComponent::class.componentId to localTransform,
            NametagComponent::class.componentId to nametag,
            InventoryComponent::class.componentId to inventory,
            VisibilityComponent::class.componentId to visibility,
            TabComponent::class.componentId to tab,
            GameModeComponent::class.componentId to gameMode,
            FallbackServerComponent::class.componentId to fallbackServer
        )
    }

    abstract fun onStart()

    fun <C : Component.HandledByPlatform> handleAddition(context: PlatformComponentHandlerContext, new: C) {
        getHandler(new).onAdded.invoke(context, new)
    }

    fun handleRemovalOrNotPresentOnSpawn(context: PlatformComponentHandlerContext, componentId: String) {
        handlersById[componentId]?.onRemovedOrNotPresentOnSpawn?.invoke(context)
    }

    fun <C : Component.HandledByPlatform> handleMutationFromEcs(context: PlatformComponentHandlerContext, new: C) {
        when (new) {
            is TransformComponent -> transform.onMutatedFromEcs.invoke(context, new)
            is BoundingBoxComponent -> boundingBox.onMutatedFromEcs.invoke(context, new)
            is GravityComponent -> gravity.onMutatedFromEcs.invoke(context, new)
            is InputComponent -> input.onMutatedFromEcsOnAnEntityThatWeDontOwn.invoke(context, new)
            is KeepAliveComponent -> keepAlive.onMutatedFromEcs.invoke(context, new)
            is LocalTransformComponent -> localTransform.onMutatedFromEcs(context, new)
            is NametagComponent -> nametag.onMutatedFromEcs(context, new)
            is InventoryComponent -> inventory.onMutatedFromEcs(context, new)
            is VisibilityComponent -> visibility.onMutatedFromEcs(context, new)
            is TabComponent -> tab.onMutatedFromEcs(context, new)
            is GameModeComponent -> gameMode.onMutatedFromEcs(context, new)
            is FallbackServerComponent -> {}
        }
    }

    fun <C : Component.Platform2Ecs> mutateFromPlatformIfNeeded(context: PlatformComponentHandlerContext, current: C) {
        when (current) {
            is TransformComponent -> transform.mutateFromPlatformIfNeeded.invoke(context, current)
            is InputComponent -> input.mutateFromPlatformIfNeeded.invoke(context, current)
            is KeepAliveComponent -> keepAlive.mutateFromPlatformIfNeeded.invoke(context, current)
        }
    }

    private fun <C : Component.HandledByPlatform> getHandler(component: C): ComponentHandlers<C> {
        return when (component) {
            is TransformComponent -> transform
            is BoundingBoxComponent -> boundingBox
            is GravityComponent -> gravity
            is InputComponent -> input
            is KeepAliveComponent -> keepAlive
            is LocalTransformComponent -> localTransform
            is NametagComponent -> nametag
            is InventoryComponent -> inventory
            is VisibilityComponent -> visibility
            is TabComponent -> tab
            is GameModeComponent -> gameMode
            is FallbackServerComponent -> fallbackServer
        } as ComponentHandlers<C>
    }

    private val fallbackServer: ComponentHandlers.Ecs2Platform<FallbackServerComponent> = ComponentHandlers.Ecs2Platform(
        onAdded = {},
        onRemovedOrNotPresentOnSpawn = {},
        onMutatedFromEcs = {}
    )

    protected abstract val gameMode: ComponentHandlers.Ecs2Platform<GameModeComponent>
    protected abstract val tab: ComponentHandlers.Ecs2Platform<TabComponent>
    protected abstract val visibility: ComponentHandlers.Ecs2Platform<VisibilityComponent>
    protected abstract val input: ComponentHandlers.Platform2Ecs<InputComponent>
    protected abstract val transform: ComponentHandlers.BiDirectional<TransformComponent>
    protected abstract val localTransform: ComponentHandlers.Ecs2Platform<LocalTransformComponent>
    protected abstract val gravity: ComponentHandlers.Ecs2Platform<GravityComponent>
    protected abstract val boundingBox: ComponentHandlers.Ecs2Platform<BoundingBoxComponent>
    protected abstract val nametag: ComponentHandlers.Ecs2Platform<NametagComponent>
    protected abstract val inventory: ComponentHandlers.Ecs2Platform<InventoryComponent>

    fun <P_I : Any, P : Platform<*, P_I>, I : ECSDynamic.ItemType<*>> PluginPlatformHandler<*, *, P_I, P, *, *, *, I, *>.createItemUnsafe(
        platform: P,
        itemType: ECSDynamic.ItemType<*>
    ): P_I {
        return createItem(platform, itemType as I)
    }
}
