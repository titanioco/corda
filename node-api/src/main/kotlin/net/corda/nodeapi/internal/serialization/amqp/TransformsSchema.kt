package net.corda.nodeapi.internal.serialization.amqp

import java.util.*
import net.corda.core.serialization.CordaSerializationTransformEnumDefaults
import net.corda.core.serialization.CordaSerializationTransformEnumDefault

enum class TransformTypes {
    EnumDefault
}

/**
 * Represent a specific type of transform, could be one or more instances of it
 */
sealed class Transform : Iterable<Transform>



/**
 * @property types is a list of serialised types that have transforms, each list element is a
 */
data class TransformsSchema (val types: Map<String, EnumMap<TransformTypes, Transform>>) {
    companion object {
        fun build(schema : Schema, sf: SerializerFactory) : TransformsSchema {
            val rtn = mutableMapOf<String, EnumMap<TransformTypes, Transform>>()
            schema.types.forEach {
                val clazz = sf.classloader.loadClass(it.name)
//                println (clazz)
//                println (clazz.annotations)
//                println (clazz.annotations.size)
//                clazz.annotations.forEach { println (it) }
                println ("JAM 1")
                clazz.getAnnotation(CordaSerializationTransformEnumDefaults::class.java)
                println ("JAM 2")
                println (clazz.getDeclaredAnnotationsByType<CordaSerializationTransformEnumDefault>(null))

            }
            return TransformsSchema(rtn)
        }
    }
}