package com.hytale2mc.math.serializers

import korlibs.math.geom.Quaternion
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.encoding.decodeStructure

object QuaternionSerializer : KSerializer<Quaternion> {
    override val descriptor = buildClassSerialDescriptor("korlibs.math.geom.Quaternion") {
        element<Float>("x")
        element<Float>("y")
        element<Float>("z")
        element<Float>("w")
    }

    override fun serialize(encoder: Encoder, value: Quaternion) {
        encoder.encodeStructure(descriptor) {
            encodeFloatElement(descriptor, 0, value.x)
            encodeFloatElement(descriptor, 1, value.y)
            encodeFloatElement(descriptor, 2, value.z)
            encodeFloatElement(descriptor, 3, value.w)
        }
    }

    override fun deserialize(decoder: Decoder): Quaternion {
        return decoder.decodeStructure(descriptor) {
            var x = 0f
            var y = 0f
            var z = 0f
            var w = 1f
            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> x = decodeFloatElement(descriptor, 0)
                    1 -> y = decodeFloatElement(descriptor, 1)
                    2 -> z = decodeFloatElement(descriptor, 2)
                    3 -> w = decodeFloatElement(descriptor, 3)
                    else -> throw SerializationException("Unknown index $index")
                }
            }
            Quaternion(x, y, z, w)
        }
    }
}
