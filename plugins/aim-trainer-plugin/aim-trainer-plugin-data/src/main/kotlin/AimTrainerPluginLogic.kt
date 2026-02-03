package com.hytale2mc.plugin.aimtrainer

import com.hytale2mc.ecs.composer.SpaceComposer
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.component.*
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.event.*
import com.hytale2mc.ecs.data.operation.DataCommand
import com.hytale2mc.ecs.data.operation.RenderingCommand
import com.hytale2mc.ecs.phase.Phase
import com.hytale2mc.ecs.query.ComponentFilters
import com.hytale2mc.ecs.query.Query
import com.hytale2mc.ecs.query.get
import com.hytale2mc.ecs.query.mutate
import com.hytale2mc.ecs.system.exec
import com.hytale2mc.ecs.system.queueCommand
import korlibs.math.geom.Vector3F

private const val NUMBER_OF_TARGETS = 10

const val floorY = 150
val gridMin = Vector3F(30f, 140f, -15f)
val gridMax = Vector3F(gridMin.x, 160f, 15f)

fun SpaceComposer.aimTrainerSystems() {
    playerInit {
        listOf(
            TransformComponent(
                translation = Vector3F(0f, floorY + 1f, 0f),
                direction = Vector3F.RIGHT
            ),
            VisibilityComponent(Visibility.ToEveryone),
            InputComponent(),
            GravityComponent(),
            InventoryComponent(InventoryComponent.createEmptySlots().apply { this[0] = InventoryItem(AimTrainerItemType.Gun, 1, null) })
        )
    }
    systems {
        system(
            "aim_trainer:init_targets",
            Phase.StartUp,
            initTargets
        )

        system(
            "aim_trainer:check_hits",
            Phase.Update,
            checkHits()
        )

        system(
            "aim_trainer:respawn_dead_targets",
            Phase.After,
            respawnDeadTargets
        )
    }
}

private val initTargets = exec {
    queueCommand(
        DataCommand.AddEntities(
            "init_targets",
            List(NUMBER_OF_TARGETS) {
                listOf(
                    EntityTypeComponent(AimTrainerEntityType.Target),
                    TransformComponent(Vector3F.ZERO),
                    DeadComponent(),
                    VisibilityComponent(Visibility.ToEveryone),
                    TargetComponent()
                )
            }
        )
    )
}

private fun SpaceComposer.checkHits() = exec(
    object : Query() {
        val inputs = readEvent<EntityInputEvent>()
        val sounds = writeEvent<ECSSoundEvent>()
    },
    object : Query() {
        val transform = read<TransformComponent>()
        override val componentFilter = ComponentFilters.and(
            ComponentFilters.with<TargetComponent>(),
            ComponentFilters.without<DeadComponent>()
        )
    }
) { q, targets ->
    val toMarkAsDead = mutableListOf<EntityId>()
    val toRender = mutableListOf<ECSDynamic.Renderable<*>>()
    for (input in q.inputs.consume()) {
        val click = input.input
        if (click !is Click.AtEntity) {
            continue
        }
        if (click.atEntityId !in targets.entities) {
            continue
        }
        if (click.atEntityId in toMarkAsDead) {
            continue
        }
        toMarkAsDead.add(click.atEntityId)
        q.sounds.write(
            ECSSoundEvent(
                AimTrainerSoundType.Hit,
                ECSSoundEvent.SoundReceiver.Personal(listOf(input.entityId))
            )
        )
        toRender.add(AimTrainerRenderable.TargetKilled(targets.transform[click.atEntityId].translation))
    }
    if (toMarkAsDead.isNotEmpty()) {
        queueCommand(
            DataCommand.AddComponents(
                "aim_trainer:mark_as_dead",
                toMarkAsDead.associateWith { listOf(DeadComponent()) }
            )
        )
    }
    if (toRender.isNotEmpty()) {
        queueCommand(
            RenderingCommand.Render(
                "aim_trainer:render_deaths",
                toRender
            )
        )
    }
}

private val respawnDeadTargets = exec(
    object : Query() {
        val dead = read<DeadComponent>()
        val transform = write<TransformComponent>()
    }
) { q ->
    for (e in q.entities) {
        val respawnTranslation = Vector3F(
            gridMax.x - 1,
            space.random.nextFloat(gridMin.y, gridMax.y),
            space.random.nextFloat(gridMin.z, gridMax.z)
        )
        q.transform.mutate(e) {
            it.copy(
                translation = respawnTranslation,
                direction = Vector3F.LEFT
            )
        }
    }

    if (q.entities.isNotEmpty()) {
        queueCommand(
            DataCommand.RemoveComponents(
                "aim_trainer:remove_dead_components",
                q.entities.associateWith { listOf(DeadComponent::class.componentId) }
            )
        )
    }
}
