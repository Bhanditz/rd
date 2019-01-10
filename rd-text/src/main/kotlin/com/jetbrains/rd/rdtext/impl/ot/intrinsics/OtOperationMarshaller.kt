package com.jetbrains.rd.rdtext.impl.ot.intrinsics

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.AbstractBuffer
import com.jetbrains.rd.rdtext.impl.intrinsics.RdChangeOrigin
import com.jetbrains.rd.rdtext.impl.ot.*
import kotlin.reflect.*

@Suppress("unused")
object OtOperationMarshaller : IMarshaller<OtOperation> {
    private const val RetainCode: Byte = 1
    private const val InsertCode: Byte = 2
    private const val DeleteCode: Byte = 3

    override val _type: KClass<*>
        get() = OtOperation::class

    override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): OtOperation {
        val changes = buffer.readList {
            val id = buffer.readByte()
            when (id) {
                RetainCode -> {
                    val offset = buffer.readInt()
                    Retain(offset)
                }
                InsertCode -> {
                    val text = buffer.readString()
                    InsertText(text)
                }
                DeleteCode -> {
                    val text = buffer.readString()
                    DeleteText(text)
                }
                else -> throw IllegalStateException("Can't find reader by id: " + this.id.toString())
            }
        }
        val origin = buffer.readEnum<RdChangeOrigin>()
        val remoteTs = buffer.readInt()
        val kind = buffer.readEnum<OtOperationKind>()
        return OtOperation(changes, origin, remoteTs, kind)
    }

    override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: OtOperation) {
        buffer.writeList(value.changes) { v ->
            when(v) {
                is Retain -> {
                    buffer.writeByte(RetainCode)
                    buffer.writeInt(v.offset)
                }
                is InsertText -> {
                    buffer.writeByte(InsertCode)
                    buffer.writeString(v.text)
                }
                is DeleteText -> {
                    buffer.writeByte(DeleteCode)
                    buffer.writeString(v.text)
                }
            }
        }
        buffer.writeEnum(value.origin)
        buffer.writeInt(value.timestamp)
        buffer.writeEnum(value.kind)
    }
}