package com.jetbrains.rd.framework.test.cases

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.ISerializersOwner
import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.base.static
import com.jetbrains.rd.framework.impl.RdOptionalProperty
import com.jetbrains.rd.framework.impl.RdProperty
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rd.util.reactive.valueOrThrow
import org.junit.Ignore
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import com.jetbrains.rd.framework.test.util.*

class RdExtTest : RdFrameworkTestBase() {
    @Ignore
    @Test() // TODO: RIDER-14180
    fun testExtension() {
        val propertyId = 1
        val clientProperty = RdOptionalProperty<DynamicEntity<Boolean?>>().static(propertyId)
        val serverProperty = RdOptionalProperty<DynamicEntity<Boolean?>>().static(propertyId).slave()

        DynamicEntity.create(clientProtocol)
        DynamicEntity.create(serverProtocol)
        //bound
        clientProtocol.bindStatic(clientProperty, "top")
        serverProtocol.bindStatic(serverProperty, "top")


        clientWire.autoFlush = false
        serverWire.autoFlush = false

        var clientEntity = DynamicEntity<Boolean?>(true)
        clientProperty.set(clientEntity)
        clientWire.processAllMessages()

        val serverEntity = DynamicEntity<Boolean?>(true)
        serverProperty.set(serverEntity)
        serverWire.processAllMessages()

        //it's new!
        clientEntity = clientProperty.valueOrThrow

        clientEntity.getOrCreateExtension("ext", DynamicExt::class) { DynamicExt("Ext!", "client") }
        clientWire.processAllMessages()
        //client send READY

        val serverExt = serverEntity.getOrCreateExtension("ext", DynamicExt::class) { DynamicExt("", "server") }
        serverWire.processAllMessages()
        //server send READY


        clientWire.processAllMessages()
        //client send COUNTERPART_ACK

        serverWire.processAllMessages()
        //server send COUNTERPART_ACK

        assertEquals("Ext!", serverExt.bar.value)
    }

    private class DynamicExt(val _bar: RdProperty<String>, private val debugName: String) : RdExtBase(), ISerializersOwner {
        override fun registerSerializersCore(serializers: ISerializers) {}

        override val serializersOwner: ISerializersOwner get() = this

        val bar: IProperty<String> = _bar

        init {
            bindableChildren.add("bar" to _bar)
            _bar.slave()
        }

        companion object : IMarshaller<DynamicExt> {
            override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): DynamicExt {
                throw IllegalStateException()
            }

            override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: DynamicExt) {
                RdProperty.write(ctx, buffer, value._bar)
            }

            override val _type: KClass<*>
                get() = DynamicExt::class

            fun create(protocol: IProtocol) {
                protocol.serializers.register(DynamicExt)
            }
        }

        constructor(_bar: String, debugName: String) : this(RdProperty(_bar, FrameworkMarshallers.String), debugName)
    }

}