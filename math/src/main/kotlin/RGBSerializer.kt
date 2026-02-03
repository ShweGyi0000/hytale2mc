package com.hytale2mc.math.serializers

import com.github.ajalt.colormath.model.RGB
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object RGBSerializer : KSerializer<RGB> {

    @OptIn(InternalSerializationApi::class)
    override val descriptor = buildSerialDescriptor("RGBA", StructureKind.LIST) {
        element<Float>("r")
        element<Float>("g")
        element<Float>("b")
        element<Float>("a")
    }

    override fun serialize(encoder: Encoder, value: RGB) {
        val r = value.r
        val g = value.g
        val b = value.b
        val a = value.alpha

        encoder.encodeStructure(descriptor) {
            encodeFloatElement(descriptor, 0, r)
            encodeFloatElement(descriptor, 1, g)
            encodeFloatElement(descriptor, 2, b)
            encodeFloatElement(descriptor, 3, a)
        }
    }

    override fun deserialize(decoder: Decoder): RGB {
        var r = 0f
        var g = 0f
        var b = 0f
        var a = 0f

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> r = decodeFloatElement(descriptor, 0)
                    1 -> g = decodeFloatElement(descriptor, 1)
                    2 -> b = decodeFloatElement(descriptor, 2)
                    3 -> a = decodeFloatElement(descriptor, 3)
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
        }

        return RGB(r, g, b, a)
    }
}