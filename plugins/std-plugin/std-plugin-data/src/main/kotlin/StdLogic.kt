package com.hytale2mc.plugin.std

import com.hytale2mc.ecs.composer.SpaceComposer
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.component.EntityTypeComponent
import com.hytale2mc.ecs.data.component.KeepAliveComponent
import com.hytale2mc.ecs.data.operation.DataCommand
import com.hytale2mc.ecs.data.time.Time
import com.hytale2mc.ecs.phase.Phase
import com.hytale2mc.ecs.query.Query
import com.hytale2mc.ecs.query.get
import com.hytale2mc.ecs.query.getValue
import com.hytale2mc.ecs.query.mutate
import com.hytale2mc.ecs.system.exec
import com.hytale2mc.ecs.system.queueCommand
import korlibs.time.seconds

fun SpaceComposer.stdSystems() = systems {
    playerInit {
        listOf(
            EntityTypeComponent(StdEntityType.Player(it)),
            KeepAliveComponent()
        )
    }
    val pingFrequency = 5.seconds
    system(
        "keep_alive:tick",
        Phase.Update,
        exec(
            object : Query() {
                val keepAlive = write<KeepAliveComponent>()
                val time by read<Time>()
            }
        ) { q ->
            val toRemove = mutableListOf<EntityId>()
            for (e in q.entities) {
                val keepAlive = q.keepAlive[e]
                keepAlive.responseTimer = keepAlive.responseTimer.tick(q.time.delta)
                when {
                    keepAlive.wants && keepAlive.responseTimer.isFinished -> {
                        toRemove.add(e)
                    }
                    !keepAlive.wants && keepAlive.responseTimer.elapsed() >= pingFrequency -> {
                        q.keepAlive.mutate(e) { it.copy(wants = true) }
                    }
                }
            }
            if (toRemove.isNotEmpty()) {
                queueCommand(
                    DataCommand.RemoveEntities("keep_alive:timeout", toRemove)
                )
            }
        }
    )
}