package com.hytale2mc.plugin.std

import com.hytale2mc.ecs.data.component.*
import com.hytale2mc.ecs.data.event.*
import com.hytale2mc.ecs.data.time.Time
import com.hytale2mc.ecs.plugin.ecsPlugin
import com.hytale2mc.plugin.std.hytale.StdHytale
import com.hytale2mc.plugin.std.minestom.StdMinestom

val stdPlugin = ecsPlugin<StdKey, StdRenderable, StdEntityType, StdBlockType, StdItemType, StdSoundType>(
    StdKey,
    handlers = {
        minecraft { StdMinestom }
        hytale { StdHytale }
    },
    components = {
        component<BoundingBoxComponent>()
        component<InputComponent>()
        component<EntityTypeComponent>()
        component<TransformComponent>()
        component<LocalTransformComponent>()
        component<KeepAliveComponent>()
        component<NametagComponent>()
        component<InventoryComponent>()
        component<VisibilityComponent>()
        component<TabComponent>()
        component<GameModeComponent>()
        component<FallbackServerComponent>()
        component<GravityComponent>()
    },
    resources = {
        resource(Time())
    },
    events = {
        event<PlayerMessageEvent>()
        event<ECSSoundEvent>()
        event<EntityInputEvent>()
        event<ECSModifyVelocityEvent>()
        event<HeldItemSlotChangeEvent>()
        event<PlayerLeftEvent>()
    },
    systems = {
        stdSystems()
    }
)