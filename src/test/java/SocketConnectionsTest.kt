package animatedledstrip.test

import animatedledstrip.server.SocketConnections
import animatedledstrip.server.isTest
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
        isTest = true
        SocketConnections.add(1200)

        assertTrue { SocketConnections.connections.containsKey(1200) }
    }

    @Test
    fun testOpenSocket() = runBlocking {
        withTimeout(60000) {
            val c = SocketConnections.add(1201)

            isTest = true
            GlobalScope.launch {
                c.openSocket()
            }

            val job = GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val socket = Socket("0.0.0.0", 1201)
                    ObjectOutputStream(socket.getOutputStream())
                    ObjectInputStream(BufferedInputStream(socket.getInputStream()))

                    runBlocking { delay(5000) }

                    quit = true
                }
            }

            job.join()
        }
    }

}