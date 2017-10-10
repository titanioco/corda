package net.corda.nodeapi.internal.serialization.amqp

import net.corda.core.serialization.CordaSerializationTransformEnumDefault
import net.corda.core.serialization.CordaSerializationTransformEnumDefaults
import org.junit.Test
import java.io.File


// To regenerate any of the binary test files do the following
//
//  1. Uncomment the code where the original form of the class is defined in the test
//  2. Comment out the rest of the test
//  3. Run the test
//  4. Using the printed path copy that file to the resources directory
//  5. Comment back out the generation code and uncomment the actual test
class EnumEvolvabilityTests {
    @CordaSerializationTransformEnumDefault("D", "A")
    enum class AnnotatedEnumOnce {
        A, B, C, D
    }

    @CordaSerializationTransformEnumDefaults(
            CordaSerializationTransformEnumDefault("E", "D"),
            CordaSerializationTransformEnumDefault("D", "A"))
    enum class AnnotatedEnumTwice {
        A, B, C, D, E
    }

    @Test
    fun annotationIsAddedToEnvelope() {
        data class C (val annotatedEnum: AnnotatedEnumOnce)

        val sf = testDefaultFactory()
        val sc = SerializationOutput(sf).serializeAndReturnSchema(C(AnnotatedEnumOnce.D))
    }

    @Test
    fun doubleAnnotationIsAddedToEnvelope() {
        data class C (val annotatedEnum: AnnotatedEnumTwice)

        val sf = testDefaultFactory()
        val sc = SerializationOutput(sf).serializeAndReturnSchema(C(AnnotatedEnumTwice.E))
    }

    /*
    @Test
    fun addOneValue() {
        val sf = testDefaultFactory()
        val path = EvolvabilityTests::class.java.getResource("EnumEvolvabilityTests.addOneValue")
        val f = File(path.toURI())

        val A = 1

        // Original version of the class for the serialised version of this class
        //
        data class C (val addOneValue: addOneValue)
        val sc = SerializationOutput(sf).serialize(C(addOneValue.A))
        f.writeBytes(sc.bytes)

        // new version of the class, in this case the order of the parameters has been swapped
    }
    */

}
