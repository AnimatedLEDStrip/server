package animatedledstrip.test

import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.*
import kotlinx.coroutines.*
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import kotlin.test.assertFalse

class MainTest {

    private fun setSocketPorts(port1: Int, port2: Int) {
        socketPort1 = port1
        socketPort2 = port2
        hostIP = "0.0.0.0"
    }

    @Test
    fun testMainStart() = runBlocking {
        quit = false
        withTimeout(60000) {
            setSocketPorts(1105, 1106)

            GlobalScope.launch {
                delay(5000)
                checkAllPixels(leds as EmulatedAnimatedLEDStrip, 0)
                quit = true
            }

            main(arrayOf("-Eq"))
        }
    }

    @Test
    fun testLocalTerminalThread() = runBlocking {
        quit = false
        withTimeout(60000) {
            setSocketPorts(1107, 1108)
            val stream = ByteArrayInputStream("q".toByteArray())
            System.setIn(stream)

            main(arrayOf("-Eq"))
        }
    }

    @Test
    fun testConnection1() = runBlocking {
        quit = false
        withTimeout(60000) {
            setSocketPorts(1109, 1110)

            GlobalScope.launch {
                delay(5000)
                withContext(Dispatchers.IO) {
                    val socket = Socket("0.0.0.0", 1109)
                    val socOut = ObjectOutputStream(socket.getOutputStream())
                    ObjectInputStream(BufferedInputStream(socket.getInputStream()))
                    socOut.writeObject(mapOf("ClientData" to true, "TextBased" to false))

                    assertFalse { SocketConnections.connections[1109]!!.textBased }

                    socOut.writeObject(mapOf("ClientData" to true, "TextBased" to true))

                    quit = true
                }
            }

            main(arrayOf("-Eq"))
        }
    }
}