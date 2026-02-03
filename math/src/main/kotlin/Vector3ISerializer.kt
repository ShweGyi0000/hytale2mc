package com.hytale2mc.math.serializers

import korlibs.math.geom.Vector3I
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object Vector3ISerializer : KSerializer<Vector3I> {
    private val intListSerializer = ListSerializer(Int.serializer())

    override val descriptor: SerialDescriptor = intListSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Vector3I) {
        encoder.encodeSerializableValue(intListSerializer, listOf(value.x, value.y, value.z))
    }

    override fun deserialize(decoder: Decoder): Vector3I {
        val list = decoder.decodeSerializableValue(intListSerializer)
        if (list.size != 3) throw SerializationException("Expected 3 ints for Vector3I, got ${list.size}")
        return Vector3I(list[0], list[1], list[2])
    }
}