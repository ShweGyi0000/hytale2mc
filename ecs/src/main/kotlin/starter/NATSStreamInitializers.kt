package com.hytale2mc.ecs.stream

import com.hytale2mc.ecs.data.operation.DataOperation
import com.hytale2mc.ecs.starter.ECSStarter
import com.hytale2mc.eventstream.EventStream
import com.hytale2mc.eventstream.nats.createOrReplaceStream
import com.hytale2mc.eventstream.nats.getStream
import com.hytale2mc.eventstream.nats.natsJetStream
import io.nats.client.JetStreamApiException
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.api.StreamInfo
import kotlinx.serialization.json.Json

fun blockUntilPrimary2ReplicaAndReplica2Primary(
    starter: ECSStarter,
    json: Json,
): Pair<EventStream<DataOperation>, EventStream<DataOperation>> {
    val nats = starter.nats
    val jetStream = starter.jetStream
    return listOf(
        starter.primary2ReplicaChannel,
        starter.replicas2PrimaryChannel
    )
        .map {
            while (true) {
                try {
                    val stream = jetStream.getStreamContext(it)
                    return@map natsJetStream(
                        nats,
                        jetStream,
                        stream = stream,
                        json = json,
                        serializer = DataOperation.serializer(),
                    )
                } catch (e: JetStreamApiException) {
                    if (e.errorCode != 404) {
                        throw e
                    }
                    Thread.sleep(1000)
                }
            }
            throw IllegalStateException()
        }.let { it.first() to it.last() }
}

data class PrimaryStreams(
    val primary2Replicas: EventStream<DataOperation>,
    val replicas2Primary: EventStream<DataOperation>,
    val replay: EventStream<DataOperation>?
)

fun createOrGetPrimary2ReplicaAndReplica2Primary(
    starter: ECSStarter,
    json: Json,
): PrimaryStreams {
    val nats = starter.nats
    val jetStream = starter.jetStream
    fun getStreamConfig(
        name: String,
        isReplay: Boolean
    ) = StreamConfiguration.builder()
        .name(name)
        .apply {
            if (isReplay) {
                storageType(StorageType.File)
            } else {
                storageType(StorageType.Memory)
            }
        }
        .noAck(true)
        .build()

    fun createOrReplaceStream(
        name: String,
        isReplay: Boolean
    ): StreamInfo {
        return nats.jetStreamManagement().createOrReplaceStream(
            getStreamConfig(name, isReplay)
        ).getOrThrow()
    }
    fun natsJetStream(info: StreamInfo): EventStream<DataOperation> {
        return natsJetStream(
            nats,
            jetStream,
            stream = nats.getStreamContext(info.config.name),
            json = json,
            serializer = DataOperation.serializer(),
        )
    }
    val replayStream = nats.jetStreamManagement()
        .getStream(starter.replayChannel)
        .getOrNull()
    if (replayStream != null) {
        val primary2Replicas = createOrReplaceStream(starter.primary2ReplicaChannel, false)
        val replicas2Primary = replayStream
        return PrimaryStreams(
            natsJetStream(primary2Replicas),
            natsJetStream(replicas2Primary),
            null
        )
    }
    // not replaying
    val primary2Replicas = createOrReplaceStream(starter.primary2ReplicaChannel, false)
    val replicas2Primary = createOrReplaceStream(starter.replicas2PrimaryChannel, false)
    return PrimaryStreams(
        natsJetStream(primary2Replicas),
        natsJetStream(replicas2Primary),
        natsJetStream(createOrReplaceStream(starter.replayChannel, isReplay = true))
    )
}