package com.hytale2mc.ecs.composer

import com.hytale2mc.ecs.data.ECSMap
import com.hytale2mc.ecs.data.Resource
import com.hytale2mc.ecs.data.component.Component
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.event.Event
import com.hytale2mc.ecs.data.state.State
import com.hytale2mc.ecs.plugin.ECSPlugin
import com.hytale2mc.ecs.plugin.PluginKey
import com.hytale2mc.ecs.space.PlayerInitializer
import com.hytale2mc.ecs.space.Resources
import com.hytale2mc.ecs.space.Space
import com.hytale2mc.ecs.space.SpaceOrigin
import com.hytale2mc.ecs.starter.ECSStarter
import com.hytale2mc.ecs.stream.blockUntilPrimary2ReplicaAndReplica2Primary
import com.hytale2mc.ecs.stream.createOrGetPrimary2ReplicaAndReplica2Primary
import com.hytale2mc.math.serializers.math
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class SpaceComposer {

    private val composedComponents = mutableMapOf<KClass<out Component>, KSerializer<out Component>>()
    private val composedStates = mutableMapOf<KClass<out State>, State>()
    private val composedResources = mutableMapOf<KClass<out Resource>, Resource>()
    private val composedSystems = mutableListOf<ComposedSystems>()
    private val composedRenderables = mutableMapOf<PluginKey<*>, Map<KClass<out ECSDynamic.Renderable<*>>, KSerializer<out ECSDynamic.Renderable<*>>>>()
    private val composedEntityTypes = mutableMapOf<PluginKey<*>, Map<KClass<out ECSDynamic.EntityType<*>>, KSerializer<out ECSDynamic.EntityType<*>>>>()
    private val composedItemTypes = mutableMapOf<PluginKey<*>, Map<KClass<out ECSDynamic.ItemType<*>>, KSerializer<out ECSDynamic.ItemType<*>>>>()
    private val composedSoundTypes = mutableMapOf<PluginKey<*>, Map<KClass<out ECSDynamic.SoundType<*>>, KSerializer<out ECSDynamic.SoundType<*>>>>()

    private val jsonBuilders = mutableListOf<JsonBuilder.() -> Unit>()
    private val playerInits = mutableListOf<PlayerInitializer>()
    private val cleanUps = mutableListOf<() -> Unit>()
    val composedEvents = mutableMapOf<KClass<out Event>, RegisteredEvent<*>>()

    private val composedMaps = mutableMapOf<String, (Json) -> ECSMap>()

    private val plugins = mutableMapOf<PluginKey<*>, ECSPlugin<*, *, *, *, *, *>>()

    fun <
            KEY : PluginKey<KEY>,
            R : ECSDynamic.Renderable<KEY>,
            E : ECSDynamic.EntityType<KEY>,
            B : ECSDynamic.BlockType<KEY>,
            I : ECSDynamic.ItemType<KEY>,
            S : ECSDynamic.SoundType<KEY>,
            > plugin(plugin: ECSPlugin<KEY, R, E, B, I, S>) {
        check(plugins.put(plugin.key, plugin) == null) { "Plugin ${plugin.key} was already registered." }
        with(plugin) {
            components {
                components()
            }
            resources {
                resources()
            }
            states {
                states()
            }
            events {
                events()
            }
            maps {
                maps()
            }
            renderables(plugin.key) {
                fun registerRendererRecursively(type: KClass<out R>) {
                    when (type.isSealed) {
                        true -> type.sealedSubclasses.forEach { registerRendererRecursively(it) }
                        false -> register(type)
                    }
                }
                registerRendererRecursively(plugin.rendererType)
            }
            entityTypes(plugin.key) {
                fun registerEntityTypeRecursively(type: KClass<out E>) {
                    when (type.isSealed) {
                        true -> type.sealedSubclasses.forEach { registerEntityTypeRecursively(it) }
                        false -> register(type)
                    }
                }
                registerEntityTypeRecursively(plugin.entityType)
            }
            itemTypes(plugin.key) {
                fun registerItemTypeRecursively(type: KClass<out I>) {
                    when (type.isSealed) {
                        true -> type.sealedSubclasses.forEach { registerItemTypeRecursively(it) }
                        false -> register(type)
                    }
                }
                registerItemTypeRecursively(plugin.itemType)
            }
            soundTypes(plugin.key) {
                fun registerSoundTypeRecursively(type: KClass<out S>) {
                    when (type.isSealed) {
                        true -> type.sealedSubclasses.forEach { registerSoundTypeRecursively(it) }
                        false -> register(type)
                    }
                }
                registerSoundTypeRecursively(plugin.soundType)
            }
            systems()
        }
    }

    private fun <KEY : PluginKey<KEY>, R : ECSDynamic.Renderable<KEY>> renderables(
        owner: KEY,
        composer: DynamicPluginTypeComposer<KEY, R>.() -> Unit
    ) {
        val old = composedRenderables.put(owner, DynamicPluginTypeComposer<KEY, R>().apply(composer).types.toMap())
        check(old == null) { "Renderables with key '$owner' already registered." }
    }

    private fun <KEY : PluginKey<KEY>, E : ECSDynamic.EntityType<KEY>> entityTypes(
        owner: KEY,
        composer: DynamicPluginTypeComposer<KEY, E>.() -> Unit
    ) {
        val old = composedEntityTypes.put(owner, DynamicPluginTypeComposer<KEY, E>().apply(composer).types.toMap())
        check(old == null) { "EntityTypes with key '$owner' already registered." }
    }

    private fun <KEY : PluginKey<KEY>, I : ECSDynamic.ItemType<KEY>> itemTypes(
        owner: KEY,
        composer: DynamicPluginTypeComposer<KEY, I>.() -> Unit
    ) {
        val old = composedItemTypes.put(owner, DynamicPluginTypeComposer<KEY, I>().apply(composer).types.toMap())
        check(old == null) { "ItemTypes with key '$owner' already registered." }
    }

    private fun <KEY : PluginKey<KEY>, S : ECSDynamic.SoundType<KEY>> soundTypes(
        owner: KEY,
        composer: DynamicPluginTypeComposer<KEY, S>.() -> Unit
    ) {
        val old = composedSoundTypes.put(owner, DynamicPluginTypeComposer<KEY, S>().apply(composer).types.toMap())
        check(old == null) { "SoundTypes with key '$owner' already registered." }
    }

    private fun components(composer: ComponentComposer.() -> Unit) {
        val newComponents = ComponentComposer().apply(composer).components
        for (newComponent in newComponents.keys) {
            check(!composedComponents.containsKey(newComponent)) { "${newComponent.qualifiedName} was already registered" }
        }
        composedComponents.putAll(newComponents)
    }

    private fun states(composer: StateComposer.() -> Unit) {
        val newStates = StateComposer().apply(composer).states
        for (newState in newStates.keys) {
            check(!composedStates.containsKey(newState)) { "${newState.qualifiedName} was already registered" }
        }
        composedStates.putAll(newStates)
    }

    private fun resources(composer: ResourceComposer.() -> Unit) {
        val newResources = ResourceComposer().apply(composer).resources
        for (newResource in newResources.keys) {
            check(!composedResources.containsKey(newResource)) { "${newResource.qualifiedName} was already registered" }
        }
        composedResources.putAll(newResources)
    }

    private fun events(composer: EventComposer.() -> Unit) {
        val newEvents = EventComposer().apply(composer).events
        for (newEvent in newEvents.keys) {
            check(!composedEvents.containsKey(newEvent)) { "$newEvent was already registered" }
        }
        composedEvents.putAll(newEvents)
    }

    private fun maps(composer: MapComposer.() -> Unit) {
        val newMaps = MapComposer().apply(composer).maps
        for (newMap in newMaps.keys) {
            check(!composedMaps.containsKey(newMap)) { "$newMap was already registered" }
        }
        composedMaps.putAll(newMaps)
    }


    fun json(builder: JsonBuilder.() -> Unit) {
        jsonBuilders.add(builder)
    }

    fun playerInit(initializer: PlayerInitializer) {
        playerInits.add(initializer)
    }

    fun cleanUp(action: () -> Unit) {
        cleanUps.add(action)
    }

    @OptIn(InternalSerializationApi::class)
    fun composeJson(): Json {
        return Json {
            serializersModule = SerializersModule {
                math()
                polymorphic(PluginKey::class) {
                    fun <K : PluginKey<K>> registerSubclassUnsafe(
                        type: KClass<K>,
                        serializer: KSerializer<*>
                    ) {
                        subclass(type, serializer as KSerializer<K>)
                    }
                    for ((key, plugin) in plugins) {
                        registerSubclassUnsafe(key::class, plugin.keyType.serializer())
                    }
                }
                polymorphic(Component::class) {
                    fun <C : Component> registerComponentSerializerUnsafe(
                        type: KClass<C>,
                        serializer: KSerializer<out Component>
                    ) {
                        subclass(type, serializer as KSerializer<C>)
                    }
                    for ((type, serializer) in composedComponents) {
                        registerComponentSerializerUnsafe(type, serializer)
                    }
                }
                polymorphic(Event::class) {
                    fun <E : Event> registerEventSerializerUnsafe(
                        type: KClass<E>,
                        serializer: KSerializer<out Event>
                    ) {
                        subclass(type, serializer as KSerializer<E>)
                    }
                    for ((type, event) in composedEvents) {
                        registerEventSerializerUnsafe(type, event.serializer)
                    }
                }
                polymorphic(ECSDynamic.Renderable::class) {
                    fun <R : ECSDynamic.Renderable<*>> registerRenderableSerializer(
                        type: KClass<R>,
                        serializer: KSerializer<out ECSDynamic.Renderable<*>>
                    ) {
                        subclass(type, serializer as KSerializer<R>)
                    }
                    for ((_, renderables) in composedRenderables) {
                        for ((type, renderable) in renderables) {
                            registerRenderableSerializer(type, renderable)
                        }
                    }
                }
                polymorphic(ECSDynamic.EntityType::class) {
                    fun <E : ECSDynamic.EntityType<*>> registerEntityTypeSerializer(
                        type: KClass<E>,
                        serializer: KSerializer<out ECSDynamic.EntityType<*>>
                    ) {
                        subclass(type, serializer as KSerializer<E>)
                    }
                    for ((_, entityTypes) in composedEntityTypes) {
                        for ((type, entityType) in entityTypes) {
                            registerEntityTypeSerializer(type, entityType)
                        }
                    }
                }
                polymorphic(ECSDynamic.ItemType::class) {
                    fun <E : ECSDynamic.ItemType<*>> registerItemTypeSerializer(
                        type: KClass<E>,
                        serializer: KSerializer<out ECSDynamic.ItemType<*>>
                    ) {
                        subclass(type, serializer as KSerializer<E>)
                    }
                    for ((_, itemTypes) in composedItemTypes) {
                        for ((type, itemType) in itemTypes) {
                            registerItemTypeSerializer(type, itemType)
                        }
                    }
                }
                polymorphic(ECSDynamic.SoundType::class) {
                    fun <S : ECSDynamic.SoundType<*>> registerSoundTypeSerializer(
                        type: KClass<S>,
                        serializer: KSerializer<out ECSDynamic.SoundType<*>>
                    ) {
                        subclass(type, serializer as KSerializer<S>)
                    }
                    for ((_, soundTypes) in composedSoundTypes) {
                        for ((type, soundType) in soundTypes) {
                            registerSoundTypeSerializer(type, soundType)
                        }
                    }
                }
            }
            for (jsonBuilder in jsonBuilders) {
                jsonBuilder.invoke(this)
            }
        }
    }

    inline fun <reified E> getEventReader(): Event.EventReader<E> where E : Event {
        return composedEvents.getValue(E::class).reader as Event.EventReader<E>
    }

    fun systems(composer: BatchComposer.() -> Unit) {
        composedSystems.add(BatchComposer().apply(composer).compose())
    }

    fun compose(): ComposedSpace {
        val json = composeJson()
        val maps = composedMaps.values.map { it.invoke(json) }
        val systems = buildMap {
            for (composedSystem in composedSystems) {
                for ((phase, batch) in composedSystem) {
                    getOrPut(phase) { mutableListOf() }.addAll(batch)
                }
            }
        }
        for (plugin in plugins) {
            for (depKey in plugin.value.dependencies) {
                check(plugins.containsKey(depKey)) { "Plugin ${plugin.key} requires a dependency $depKey but it was not provided" }
            }
        }
        return ComposedSpace(
            plugins,
            composedStates,
            composedResources,
            systems,
            composedEvents,
            composedRenderables.toMap(),
            composedEntityTypes.toMap(),
            composedItemTypes.toMap(),
            json,
            playerInitializer = { event -> playerInits.flatMap { it.invoke(event) } },
            maps,
            cleanUp = {
                for (cleanUp in cleanUps) {
                    runCatching { cleanUp.invoke() }
                }
            }
        )
    }

}

data class ComposedSpace(
    val plugins: Map<PluginKey<*>, ECSPlugin<*, *, *, *, *, *>>,
    val states: ComposedStates,
    val resources: ComposedResources,
    val systems: ComposedSystems,
    val events: Map<KClass<out Event>, RegisteredEvent<*>>,
    val renderables: Map<PluginKey<*>, ComposedDynamics<ECSDynamic.Renderable<*>>>,
    val entityTypes: Map<PluginKey<*>, ComposedDynamics<ECSDynamic.EntityType<*>>>,
    val itemTypes: Map<PluginKey<*>, ComposedDynamics<ECSDynamic.ItemType<*>>>,
    val json: Json,
    val playerInitializer: PlayerInitializer,
    val maps: List<ECSMap>,
    val cleanUp: () -> Unit
)

fun primary(
    starter: ECSStarter,
    composition: ComposedSpace,
    executor: Executor = Executors.newWorkStealingPool(),
): Space {
    val streams = createOrGetPrimary2ReplicaAndReplica2Primary(
        starter,
        composition.json,
    )
    val space = Space(
        streamWriter = streams.primary2Replicas.createWriter(),
        streamReader = streams.replicas2Primary.createReader(UUID.randomUUID().toString()),
        PRIMARY,
        composition.playerInitializer,
        executor
    )
    space.replayEventLog = streams.replay?.createWriter()
    space.maps = composition.maps
    space.cleanUp = composition.cleanUp

    initialize(space, composition)
    return space
}

fun replica(
    starter: ECSStarter,
    replicaUniqueId: String,
    composition: ComposedSpace,
): Space {
    val (primary2Replica, replica2Primary) = blockUntilPrimary2ReplicaAndReplica2Primary(
        starter,
        composition.json
    )
    return Space(
        streamWriter = replica2Primary.createWriter(),
        streamReader = primary2Replica.createReader(replicaUniqueId),
        SpaceOrigin.REPLICA,
        { emptyList() },
        Executors.newSingleThreadExecutor()
    ).apply {
        maps = composition.maps
        plugins = composition.plugins
    }
}


fun initialize(space: Space, composition: ComposedSpace) {
    if (space.origin == PRIMARY) {
        for ((type, initial) in composition.states) {
            space.states.addUnsafe(type, initial)
        }

        for ((_, resource) in composition.resources) {
            Resources.addResource(space, resource)
        }

        for ((phase, systemBatches) in composition.systems) {
            for (batch in systemBatches) {
                space.systems.getOrPut(phase) { mutableListOf() }.add(batch)
            }
        }

        for ((type, reader) in composition.events) {
            if (type.isSubclassOf(Event.Platform2Ecs::class)) {
                space.eventWriters[type as KClass<out Event.Platform2Ecs>] = Event.EventWriter(reader.reader) as Event.EventWriter<Event.Platform2Ecs>
            }
        }
    }

    space.plugins = composition.plugins
}