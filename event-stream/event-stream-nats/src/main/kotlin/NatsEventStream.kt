package com.hytale2mc.eventstream.nats

import com.hytale2mc.eventstream.EventStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import io.nats.client.Connection
import io.nats.client.Consumer
import io.nats.client.JetStream
import io.nats.client.JetStreamManagement
import io.nats.client.StreamContext
import io.nats.client.api.AckPolicy
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.api.DeliverPolicy
import io.nats.client.api.StreamConfiguration
import io.nats.client.api.StreamInfo

typealias NatsConnection = Connection
typealias NatsConsumer = Consumer

internal class NatsEventStream<T : Any>(
    private val nats: NatsConnection,
    private val jetStream: JetStream,
    val stream: StreamContext,
    private val json: Json,
    private val serializer: KSerializer<T>,
) : EventStream<T>(stream.streamName) {

    override fun createReader(readerUniqueId: String): NatsReader<T> {
        val consumer = stream.createOrUpdateConsumer(
            ConsumerConfiguration.builder()
                .name(readerUniqueId)
                .deliverPolicy(DeliverPolicy.All)
                .ackPolicy(AckPolicy.None)
                .maxPullWaiting(10000)
                .maxAckPending(10000)
                .build()
        )

        return NatsReader(
            consumer,
            json,
            serializer
        )
    }

    override fun createWriter(): NatsWriter<T> {
        return NatsWriter(
            nats,
            jetStream,
            stream.streamName,
            json, serializer
        )
    }

}

fun <T : Any> natsJetStream(
    nats: NatsConnection,
    jetStream: JetStream,
    stream: StreamContext,
    json: Json,
    serializer: KSerializer<T>
): EventStream<T> {
    return NatsEventStream(
        nats,
        jetStream,
        stream,
        json, serializer
    )
}

fun JetStreamManagement.createOrReplaceStream(
    config: StreamConfiguration
): Result<StreamInfo> {
    try {
        deleteStream(config.name)
    } catch (_: Exception) { }
    return runCatching {
        addStream(config)
    }
}

fun JetStreamManagement.createStreamIfMissing(
    config: StreamConfiguration
): Result<StreamInfo> {
    return runCatching {
        addStream(config)
    }
}

fun JetStreamManagement.getStream(
    name: String
): Result<StreamInfo> {
    return runCatching { getStreamInfo(name) }
}