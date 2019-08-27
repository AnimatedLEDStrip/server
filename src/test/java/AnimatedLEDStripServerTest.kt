package animatedledstrip.test

import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.SocketConnections
import kotlinx.coroutines.*
import org.junit.Test
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import java.io.ByteArrayInputStream

class AnimatedLEDStripServerTest {

    init {
        SocketConnections.hostIP = "0.0.0.0"
        Configurator.defaultConfig().level(Level.OFF).activate()
    }

    val leds = EmulatedAnimatedLEDStrip(50)

    @Test
    fun testStart() = runBlocking {
        withTimeout(60000) {
            val server =
                AnimatedLEDStripServer(arrayOf("-Eq"), EmulatedAnimatedLEDStrip::class)

            GlobalScope.launch {
                delay(5000)
                checkAllPixels(leds, 0)
                server.stop()
            }

            server.start()
            Unit
        }
    }

    @Test
    fun testLocalTerminalThread() = runBlocking {
        withTimeout(60000) {
            val stream = ByteArrayInputStream("q".toByteArray())
            System.setIn(stream)

            AnimatedLEDStripServer(arrayOf("-Eq"), EmulatedAnimatedLEDStrip::class).start()
            Unit
        }
    }
}