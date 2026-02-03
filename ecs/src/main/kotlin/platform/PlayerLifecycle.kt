package com.hytale2mc.ecs.platform

import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.PlatformEntityId
import com.hytale2mc.ecs.data.event.PlayerLeftEvent
import com.hytale2mc.ecs.data.operation.DataCommand
import com.hytale2mc.ecs.data.operation.PlayerJoined
import com.hytale2mc.ecs.data.operation.PlayerLeft
import com.hytale2mc.ecs.space.Space

data object PlayerLifecycle {

    fun callThisWhenPlayerJoinsOurPlatform(
        platform: Platform<*, *>,
        playerPlatformId: PlatformEntityId,
        playerUsername: String
    ) {
        val space = platform.space
        if (space.isReplaying) {
            // we don't want to mess up the replay,
            // so we will just forcefully spawn the player
            return
        }

        // let our platform know that it is the one that owns this player
        if (!platform.owningPlayersPlatformIds.add(playerPlatformId)) {
            // the player is already joining
            return
        }

        val event = PlayerJoined(
            platform = platform.source,
            username = playerUsername,
            platformEntityIdOnOrigin = playerPlatformId
        )

        if (space.origin == PRIMARY) {
            // this event will be handled during the next
            // operation flush
            space.operations.add(event)
        } else {
            // since replicas can't handle this event anyway
            // we will just stream it
            space.streamLog.write(event)
        }
    }

    internal fun callThisWhenPlayerShouldBeSpawnedButIsOffline(
        platform: Platform<*, *>,
        entityId: EntityId,
        playerPlatformId: PlatformEntityId,
        username: String
    ) {
        check(platform.owningPlayersPlatformIds.remove(playerPlatformId))
        val event = PlayerLeft(entityId, username, platform.source.platformType)

        when (platform.space.origin) {
            PRIMARY -> platform.space.operations.add(event)
            REPLICA -> platform.space.streamLog.write(event)
        }
    }

    fun callThisWhenPlayerLeavesOurPlatform(
        platform: Platform<*, *>,
        playerPlatformId: PlatformEntityId,
        username: String
    ) {
        val entityId = platform.platformEntityId2EntityId[playerPlatformId]
        if (entityId == null) {
            // player was not yet added with AddEntities or we are replaying
            // we will call this function again when the primary attempts to add him
            return
        }

        callThisWhenPlayerShouldBeSpawnedButIsOffline(platform, entityId, playerPlatformId, username)
    }
    
    internal fun handlePlayerJoinedEvent(
        space: Space,
        event: PlayerJoined,
    ) {
        check(space.origin == PRIMARY)

        // spawn the actual entity
        space.operations.add(
            DataCommand.AddEntities(
                "player_join_${event.username}",
                listOf(space.playerInitializer.invoke(event))
            )
        )
    }

    internal fun handlePlayerLeftEvent(
        platform: Platform<*, *>,
        event: PlayerLeft,
    ) {
        val space = platform.space
        check(space.origin == PRIMARY)

        platform.eventHandler.write(PlayerLeftEvent(event.username, event.platformType))
        space.operations.add(
            DataCommand.RemoveEntities("player_leave", listOf(event.entityId))
        )
    }

}
