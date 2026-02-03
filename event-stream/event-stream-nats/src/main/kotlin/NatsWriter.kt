package com.hytale2mc.eventstream.nats

import com.hytale2mc.eventstream.EventStream
import io.nats.client.JetStream
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

internal class NatsWriter<T : Any>(
    private val nats: NatsConnection,
    private val jetStream: JetStream,
    private val streamId: String,
    private val json: Json,
    private val serializer: SerializationStrategy<T>
) : EventStream.Writer<T> {

    override fun write(data: T) {
        jetStream.publishAsync(
            streamId,
            json.encodeToString(serializer, data).encodeToByteArray(),
        )
    }

    override fun flush() {
        nats.flushBuffer()
    }

}