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
            println("ab")
            val c = SocketConnections.add(1201)
            println("cd")
            GlobalScope.launch {
                println("e")
                c.openSocket()
                println("f")
            }
            println("g")
            val job = GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    println("h")
                    val socket = Socket("0.0.0.0", 1201)
                    println("i")
                    ObjectOutputStream(socket.getOutputStream())
                    println("j")
                    ObjectInputStream(BufferedInputStream(socket.getInputStream()))

                    println("k")
                    runBlocking { delay(5000) }
                    println("l")

                    quit = true
                    println("m")
                    socket.shutdownOutput()
                    println("n")
                }
            }
            println("o")
            job.join()
            println("p")
        }
    }

}