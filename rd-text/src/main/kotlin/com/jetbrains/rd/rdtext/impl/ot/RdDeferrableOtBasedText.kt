package com.jetbrains.rd.rdtext.impl.ot

import com.jetbrains.rd.framework.base.RdReactiveBase
import com.jetbrains.rd.rdtext.IDeferrableITextBuffer
import com.jetbrains.rd.rdtext.intrinsics.RdTextChange
import com.jetbrains.rd.rdtext.impl.ot.intrinsics.RdOtState
import com.jetbrains.rd.util.debug
import com.jetbrains.rd.util.reflection.usingTrueFlag


class RdDeferrableOtBasedText(delegate: RdOtState, isMaster: Boolean = true) : RdOtBasedText(delegate, isMaster), IDeferrableITextBuffer {
    private val queue = mutableListOf<OtOperation>()
    private val localTs2QueuedAck = mutableMapOf<Int, MutableList<Int>>()
    private var isQueueing = false

    override fun receiveOperation(operation: OtOperation) {
        if (operation.kind == OtOperationKind.Reset) {
            queue.clear()
            localTs2QueuedAck.clear()
        }
        super.receiveOperation(operation)
    }

    override fun sendAck(timestamp: Int) {
        if (queue.isEmpty()) {
            super.sendAck(timestamp)
        } else {
            val ts = getCurrentTs()
            localTs2QueuedAck.getOrPut(ts, { mutableListOf() })
                    .add(timestamp)
        }
    }

    override fun sendOperation(operation: OtOperation) {
        if (operation.kind == OtOperationKind.Reset) {
            queue.clear()
            localTs2QueuedAck.clear()
            super.sendOperation(operation)
            return
        }

        if (isQueueing) {
            queue.add(operation)
        } else {
            if (queue.isNotEmpty()) flush()
            super.sendOperation(operation)
        }
    }

    override fun flush() {
        protocol.scheduler.assertThread()
        require(!queue.isEmpty())
        RdReactiveBase.logSend.debug { "Sending queued changes and acks" }
        try {
            for (e in queue) {
                super.sendOperation(e)
                val list = localTs2QueuedAck[e.timestamp]
                if (list != null) {
                    for (ts in list) super.sendAck(ts)
                }
            }
        } finally {
            queue.clear()
            localTs2QueuedAck.clear()
        }
    }

    override fun queue(newChange: RdTextChange) {
        usingTrueFlag(RdDeferrableOtBasedText::isQueueing) {
            fire(newChange)
        }
    }

    override val isQueueEmpty: Boolean
        get() = queue.isEmpty()
}