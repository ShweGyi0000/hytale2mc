package com.hytale2mc.math.serializers

import korlibs.math.geom.Vector3F
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object Vector3FSerializer : KSerializer<Vector3F> {
    private val floatListSerializer = ListSerializer(Float.serializer())

    override val descriptor: SerialDescriptor = floatListSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Vector3F) {
        encoder.encodeSerializableValue(floatListSerializer, listOf(value.x, value.y, value.z))
    }

    override fun deserialize(decoder: Decoder): Vector3F {
        val list = decoder.decodeSerializableValue(floatListSerializer)
        if (list.size != 3) throw SerializationException("Expected 3 floats for Vector3F, got ${list.size}")
        return Vector3F(list[0], list[1], list[2])
    }
}
