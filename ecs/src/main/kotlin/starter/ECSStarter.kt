package com.hytale2mc.ecs.starter

import com.hytale2mc.ecs.composer.SpaceComposer
import com.hytale2mc.ecs.composer.primary
import com.hytale2mc.ecs.composer.replica
import com.hytale2mc.ecs.plugin.ECSPlugin
import com.hytale2mc.ecs.space.Space
import com.hytale2mc.eventstream.nats.NatsConnection
import io.nats.client.JetStream
import io.nats.client.JetStreamOptions
import io.nats.client.Nats
import io.nats.client.Options
import java.util.UUID

class ECSStarter(
    gameId: String,
    val port: Int,
    replayId: String? = null,
    natsServerUrl: String = "nats://localhost:4222",
    plugins: (ECSStarter) -> List<ECSPlugin<*, *, *, *, *, *>>
) {

    val nats: NatsConnection = Nats.connect(
        Options.Builder()
            .server(natsServerUrl)
            .build()
    )

    val jetStream: JetStream = nats.jetStream(
        JetStreamOptions.builder()
            .publishNoAck(true)
            .build()
    )

    private val composition = SpaceComposer().apply {
        for (plugin in plugins.invoke(this@ECSStarter)) {
            plugin(plugin)
        }
    }.compose()

    fun primary(): Space {
        return primary(
            this,
            composition,
        ).apply {
            port = this@ECSStarter.port
        }
    }

    fun replica(): Space {
        val replicaId = "replica-${UUID.randomUUID()}"
        return replica(
            this,
            replicaId,
            composition,
        ).apply {
            port = this@ECSStarter.port
        }
    }

    val primary2ReplicaChannel = gameId
    val replicas2PrimaryChannel = "${gameId}-replicas"
    val replayChannel = "${gameId}-replay-${replayId ?: UUID.randomUUID()}"

}