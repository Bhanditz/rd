package com.jetbrains.rider.rdtext.test.cases

import com.jetbrains.rider.framework.*
import com.jetbrains.rider.framework.base.IRdBindable
import com.jetbrains.rider.framework.base.RdDelegateBase
import com.jetbrains.rider.rdtext.*
import com.jetbrains.rider.rdtext.impl.RdDeferrableTextBuffer
import com.jetbrains.rider.rdtext.impl.intrinsics.RdChangeOrigin
import com.jetbrains.rider.rdtext.impl.intrinsics.RdTextBufferState
import com.jetbrains.rider.rdtext.impl.ot.RdDeferrableOtBasedText
import com.jetbrains.rider.rdtext.impl.ot.intrinsics.RdOtState
import com.jetbrains.rider.rdtext.intrinsics.RdTextChange
import com.jetbrains.rider.rdtext.intrinsics.RdTextChangeKind
import com.jetbrains.rider.rdtext.test.util.*
import com.jetbrains.rider.util.Closeable
import com.jetbrains.rider.util.ILoggerFactory
import com.jetbrains.rider.util.Statics
import com.jetbrains.rider.util.lifetime.Lifetime
import com.jetbrains.rider.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.util.log.ErrorAccumulatorLoggerFactory
import com.jetbrains.rider.util.reactive.IScheduler
import org.jetbrains.jetCheck.Generator
import org.jetbrains.jetCheck.ImperativeCommand
import org.jetbrains.jetCheck.IntDistribution
import org.jetbrains.jetCheck.PropertyChecker
import org.junit.Assert
import kotlin.test.Test

private fun IRdBindable.top(lifetime: Lifetime, protocol: IProtocol) {
    identify(protocol.identity, RdId.Null.mix(this.javaClass.simpleName))
    bind(lifetime, protocol, this.javaClass.simpleName)
}

private data class TextBufferCommand(val change: RdTextChange, val isQueued: Boolean, val origin: RdChangeOrigin)

class TextBufferTest {
    companion object {
        const val MAX_STEPS = 100
    }

    class RandomTextChanges(createDeferrableBuffer: (Boolean) -> IDeferrableITextBuffer) : ImperativeCommand {
        private val serializers = Serializers()
        private val clientWire = TestWire(clientScheduler).apply { autoFlush = false }
        private val serverWire = TestWire(serverScheduler).apply { autoFlush = false }
        private var clientProtocol: IProtocol = Protocol(serializers, Identities(), clientScheduler, clientWire)
        private var serverProtocol: IProtocol = Protocol(serializers, Identities(), serverScheduler, serverWire)
        private var clientLifetimeDef: LifetimeDefinition = Lifetime.create(Lifetime.Eternal)
        private var serverLifetimeDef: LifetimeDefinition = Lifetime.create(Lifetime.Eternal)
        private var disposeLoggerFactory = Statics<ILoggerFactory>().push(ErrorAccumulatorLoggerFactory) as Closeable
        private val clientScheduler: IScheduler get() = TestScheduler
        private val serverScheduler: IScheduler get() = TestScheduler

        private val master: IDeferrableITextBuffer
        private val slave: IDeferrableITextBuffer
        var masterText = ""
        var slaveText = ""

        init {
            val (w1, w2) = (clientProtocol.wire as TestWire) to (serverProtocol.wire as TestWire)
            w1.counterpart = w2
            w2.counterpart = w1

            master = createDeferrableBuffer(true).apply {
                (this as RdDelegateBase<*>).top(clientLifetimeDef.lifetime, clientProtocol)
            }
            slave = createDeferrableBuffer(false).apply {
                (this as RdDelegateBase<*>).top(serverLifetimeDef.lifetime, serverProtocol)
            }

            master.advise(clientLifetimeDef.lifetime, {
                masterText = playChange(masterText, it)
                master.assertState(masterText)
            })
            slave.advise(serverLifetimeDef.lifetime, {
                slaveText = playChange(slaveText, it)
                slave.assertState(slaveText)
            })
        }

        private val nextCommandGen: Generator<TextBufferCommand> by lazy {
            Generator.from({ r ->
                val isMasterTurn = r.generate(Generator.booleans())
                val origin = if (isMasterTurn) RdChangeOrigin.Master else RdChangeOrigin.Slave
                val text = if (isMasterTurn) masterText else slaveText
                val isQueued = if (isMasterTurn) r.generate(Generator.booleans()) else false

                val kind = if (text.isEmpty()) RdTextChangeKind.Insert else  r.generate(Generator.sampledFrom(
                        RdTextChangeKind.Insert,
                        RdTextChangeKind.Remove,
                        RdTextChangeKind.Replace))

                val change = when (kind) {
                    RdTextChangeKind.Insert -> {
                        val offset = r.generate(Generator.integers(0, text.length))
                        val newText = r.generate(Generator.stringsOf(IntDistribution.uniform(1, 10), Generator.asciiLetters()))
                        RdTextChange(kind, offset, "", newText, text.length + newText.length)
                    }
                    RdTextChangeKind.Remove -> {
                        val x0 = r.generate(Generator.integers(0, text.length - 1))
                        val x1 = r.generate(Generator.integers(x0 + 1, text.length))
                        val old = text.substring(x0, x1)
                        RdTextChange(kind, x0, old, "", text.length - old.length)
                    }
                    RdTextChangeKind.Replace -> {
                        val x0 = r.generate(Generator.integers(0, text.length - 1))
                        val x1 = r.generate(Generator.integers(x0 + 1, text.length))
                        val old = text.substring(x0, x1)
                        val newText = r.generate(Generator.stringsOf(IntDistribution.uniform(1, 10), Generator.asciiLetters()))
                        RdTextChange(kind, x0, old, newText, text.length - old.length + newText.length)
                    }
                    else -> throw IllegalArgumentException("Unexpected kind: $kind")
                }
                return@from TextBufferCommand(change, isQueued, origin)
            })
        }


        override fun performCommand(env: ImperativeCommand.Environment) {
            try {
                val initialText = env.generateValue(Generator.stringsOf(IntDistribution.uniform(1, 50), Generator.asciiLetters()), null)

                var op = TextBufferCommand(
                        RdTextChange(RdTextChangeKind.Reset, 0, "", initialText, initialText.length),
                        false,
                        RdChangeOrigin.Master)
                var prevChange: RdTextChange? = null
                var stepCounter = 0
                while (stepCounter != MAX_STEPS) {
//                    println("-----------------------------------------------------------------")
//                    println("#$stepCounter: $op")
                    val (change, isQueued, origin) = op

                    if (origin == RdChangeOrigin.Master) {
                        masterText = playChange(masterText, change)
                        if (isQueued)
                            master.queue(change)
                        else
                            master.fire(change)
                        master.assertState(masterText)
                    } else {
                        slaveText = playChange(slaveText, change)
                        slave.fire(change)
                        slave.assertState(slaveText)
                    }

                    val pumpNow = env.generateValue(Generator.frequency(60, Generator.constant(true), 40, Generator.constant(false)), null)
                    if (pumpNow || change.kind == RdTextChangeKind.Reset) {
                        clientWire.processAllMessages()
                        serverWire.processAllMessages()
                        clientWire.processAllMessages()
                        serverWire.processAllMessages()
                    }
                    ErrorAccumulatorLoggerFactory.throwAndClear()
//                    println("Master state: version = ${master.bufferVersion}; text = '$masterText'")
//                    println("Slave state: version = ${slave.bufferVersion}; text = '$slaveText'")
//                    println("Texts are equal:${masterText == slaveText}")

                    if (masterText.isEmpty() && (prevChange?.delta() == 0) && change.delta() == 0) break

                    stepCounter++
                    prevChange = if (change.kind != RdTextChangeKind.Reset) change else null
                    op = env.generateValue(nextCommandGen, null)!!
                }

                // flush queued changes
                master.fire(RdTextChange(RdTextChangeKind.Insert, 0, "", "", masterText.length))

                clientWire.processAllMessages()
                serverWire.processAllMessages()
                clientWire.processAllMessages()
                serverWire.processAllMessages()
                Assert.assertEquals(masterText, slaveText)
            } finally {
                tearDown()
            }
        }


        private fun tearDown() {
            disposeLoggerFactory.close()

            clientLifetimeDef.terminate()
            serverLifetimeDef.terminate()
            ErrorAccumulatorLoggerFactory.throwAndClear()
        }
    }

    @Test
    fun convergenceForOtBasedText() {
        PropertyChecker.customized()
                .withIterationCount(100)
                .checkScenarios { RandomTextChanges({ RdDeferrableOtBasedText(RdOtState(), it) }) }
    }

    @Test
    fun convergenceForNaiveBuffer() {
        PropertyChecker.customized()
                .withIterationCount(100)
                .checkScenarios { RandomTextChanges({ RdDeferrableTextBuffer(RdTextBufferState(), it) }) }
    }

}

private fun playChange(initText: String, change: RdTextChange): String {
    val x0 = change.startOffset
    val x1 = change.startOffset + change.old.length
    val newText = when (change.kind) {
        RdTextChangeKind.Reset -> change.new
        else -> initText.replaceRange(x0, x1, change.new)
    }
    return newText
}