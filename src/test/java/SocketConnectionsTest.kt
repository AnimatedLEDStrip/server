package animatedledstrip.test

import animatedledstrip.server.SocketConnections
import animatedledstrip.server.hostIP
import animatedledstrip.server.quit
import kotlinx.coroutines.*
import org.junit.Test
import java.io.BufferedInputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import kotlin.test.assertTrue

class SocketConnectionsTest {

    @Test
    fun testAdd() {
        hostIP = "0.0.0.0"
        SocketConnections.add(1200)

        assertTrue { SocketConnections.connections.containsKey(1200) }
    }

    @Test
    fun testOpenSocket() = runBlocking {
        quit = false
        withTimeout(60000) {
            hostIP = "0.0.0.0"
            val c = SocketConnections.add(1201)
            c.open()

            val job = GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val socket = Socket("0.0.0.0", 1201)
                    ObjectOutputStream(socket.getOutputStream())
                    ObjectInputStream(BufferedInputStream(socket.getInputStream()))

                    runBlocking { delay(5000) }

                    quit = true
                    socket.shutdownOutput()
                }
            }
            job.join()
        }
    }

}