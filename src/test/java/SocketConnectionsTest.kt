package animatedledstrip.test

import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.delayBlocking
import kotlinx.coroutines.*
import org.junit.Test
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import java.io.BufferedInputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import kotlin.test.assertTrue

class SocketConnectionsTest {

    init {
        Configurator.defaultConfig().level(Level.OFF).activate()
    }

    @Test
    fun testAdd() {
        val server =
            AnimatedLEDStripServer(arrayOf("-E"), EmulatedAnimatedLEDStrip::class)
        SocketConnections.hostIP = "0.0.0.0"
        SocketConnections.add(1200, server)

        assertTrue { SocketConnections.connections.containsKey(1200) }
    }

    @Test
    fun testOpenSocket() = runBlocking {
        withTimeout(60000) {
            val server =
                AnimatedLEDStripServer(arrayOf("-E"), EmulatedAnimatedLEDStrip::class).start()
            SocketConnections.hostIP = "0.0.0.0"
            val c = SocketConnections.add(1201, server)
            c.open()

            val job = GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val socket = Socket("0.0.0.0", 1201)
                    ObjectOutputStream(socket.getOutputStream())
                    ObjectInputStream(BufferedInputStream(socket.getInputStream()))

                    delayBlocking(5000)

                    server.stop()
                    socket.shutdownOutput()
                }
            }
            job.join()
        }
    }

}