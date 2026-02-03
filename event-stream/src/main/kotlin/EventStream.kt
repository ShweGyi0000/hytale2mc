package com.hytale2mc.eventstream

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class EventStream<T>(
    val streamId: String,
) {

    abstract fun createReader(readerUniqueId: String): Reader<T>

    abstract fun createWriter(): Writer<T>


    interface Reader<T> {

        fun read(
            maxMessages: Int = 100,
            maxDuration: Duration = 15.milliseconds,
        ): List<T>

    }

    interface Writer<T> {

        fun write(data: T)
        fun flush()

    }

}

