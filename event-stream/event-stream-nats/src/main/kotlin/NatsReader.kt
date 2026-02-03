package com.hytale2mc.eventstream.nats

import com.hytale2mc.eventstream.EventStream
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import io.nats.client.ConsumerContext
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Clock
import kotlin.time.Duration

internal data class NatsReader<T : Any>(
    private val consumer: ConsumerContext,
    private val json: Json,
    private val deserializer: DeserializationStrategy<T>
) : EventStream.Reader<T> {

    private val buffer = ConcurrentLinkedQueue<T>()

    init {
        consumer.consume { message ->
            when {
                message == null -> return@consume
                message.isStatusMessage -> return@consume
                else -> buffer.add(json.decodeFromString(deserializer, message.data.decodeToString()))
            }
        }
    }

    override fun read(
        maxMessages: Int,
        maxDuration: Duration
    ): List<T> {
        val until = Clock.System.now() + maxDuration
        val messages = mutableListOf<T>()
        var i = 0
        while (true) {
            val message = buffer.poll()
            if (message == null) {
                val toSleep = (until - Clock.System.now()).inWholeMilliseconds
                if (toSleep > 0L) {
                    Thread.sleep(toSleep)
                    continue
                } else {
                    break
                }
            }
            messages.add(message)
            if (++i >= maxMessages) {
                break
            }
        }
        return messages
    }

}