package net.corda.nodeapi.internal.serialization

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.DefaultClassResolver
import net.corda.core.serialization.*
import net.corda.node.services.statemachine.SessionData
import net.corda.nodeapi.internal.serialization.amqp.DeserializationInput
import net.corda.nodeapi.internal.serialization.amqp.Envelope
import net.corda.nodeapi.internal.serialization.amqp.SerializerFactory
import net.corda.testing.TestDependencyInjectionBase
import net.corda.testing.amqpSpecific
import net.corda.testing.kryoSpecific
import org.assertj.core.api.Assertions
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.NotSerializableException
import java.nio.charset.StandardCharsets.*
import java.util.*

class ListsSerializationTest : TestDependencyInjectionBase() {
    private companion object {
        val javaEmptyListClass = Collections.emptyList<Any>().javaClass

        fun <T : Any> verifyEnvelope(serBytes: SerializedBytes<T>, envVerBody: (Envelope) -> Unit) =
                amqpSpecific("AMQP specific envelope verification") {
                    val context = SerializationFactory.defaultFactory.defaultContext
                    val envelope = DeserializationInput(SerializerFactory(context.whitelist, context.deserializationClassLoader)).getEnvelope(serBytes)
                    envVerBody(envelope)
                }
    }

    @Test
    fun `check list can be serialized as root of serialization graph`() {
        assertEqualAfterRoundTripSerialization(emptyList<Int>())
        assertEqualAfterRoundTripSerialization(listOf(1))
        assertEqualAfterRoundTripSerialization(listOf(1, 2))
    }

    @Test
    fun `check list can be serialized as part of SessionData`() {
        run {
            val sessionData = SessionData(123, listOf(1))
            assertEqualAfterRoundTripSerialization(sessionData)
        }
        run {
            val sessionData = SessionData(123, listOf(1, 2))
            assertEqualAfterRoundTripSerialization(sessionData)
        }
        run {
            val sessionData = SessionData(123, emptyList<Int>())
            assertEqualAfterRoundTripSerialization(sessionData)
        }
    }

    @Test
    fun `check empty list serialises as Java emptyList`() = kryoSpecific("Kryo specific test") {
        val nameID = 0
        val serializedForm = emptyList<Int>().serialize()
        val output = ByteArrayOutputStream().apply {
            write(KryoHeaderV0_1.bytes)
            write(DefaultClassResolver.NAME + 2)
            write(nameID)
            write(javaEmptyListClass.name.toAscii())
            write(Kryo.NOT_NULL.toInt())
        }
        assertArrayEquals(output.toByteArray(), serializedForm.bytes)
    }

    @CordaSerializable
    data class WrongPayloadType(val payload: ArrayList<Int>)

    @Test
    fun `check throws for forbidden declared type`() = amqpSpecific("Such exceptions are not expected in Kryo mode.") {
        val payload = ArrayList<Int>()
        payload.add(1)
        payload.add(2)
        val wrongPayloadType = WrongPayloadType(payload)
        Assertions.assertThatThrownBy { wrongPayloadType.serialize() }
                .isInstanceOf(NotSerializableException::class.java).hasMessageContaining("Cannot derive collection type for declaredType")
    }

    @CordaSerializable
    interface Parent

    data class Child(val value: Int) : Parent

    @CordaSerializable
    data class CovariantContainer<out T : Parent>(val payload: List<T>)

    @Test
    fun `check covariance`() {
        val payload = ArrayList<Child>()
        payload.add(Child(1))
        payload.add(Child(2))
        val container = CovariantContainer(payload)

        fun verifyEnvelopeBody(envelope: Envelope) {
            envelope.schema.types.single { typeNotation -> typeNotation.name == java.util.List::class.java.name + "<?>" }
        }

        assertEqualAfterRoundTripSerialization(container, { bytes -> verifyEnvelope(bytes, ::verifyEnvelopeBody) })
    }
}

internal inline fun <reified T : Any> assertEqualAfterRoundTripSerialization(obj: T, noinline streamValidation: ((SerializedBytes<T>) -> Unit)? = null) {

    val serializedForm: SerializedBytes<T> = obj.serialize()
    streamValidation?.invoke(serializedForm)
    val deserializedInstance = serializedForm.deserialize()

    assertEquals(obj, deserializedInstance)
}

internal fun String.toAscii(): ByteArray = toByteArray(US_ASCII).apply {
    this[lastIndex] = (this[lastIndex] + 0x80).toByte()
}
