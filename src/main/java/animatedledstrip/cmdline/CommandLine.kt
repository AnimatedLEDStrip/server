package animatedledstrip.cmdline

import kotlinx.coroutines.*
import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import kotlin.system.exitProcess

class CommandLine {

    private val socket = Socket()

    private var endCmdLine = false

    private var readerJob: Job? = null

    fun loop() {
        println("Welcome to the AnimatedLEDStrip Server console")
        while (!endCmdLine) {
            try {
                socket.connect(InetSocketAddress("localhost", 1118), 5000)
            } catch (e: Exception) {
                continue
            }
            println("Connected")
            try {
                val socOut = ObjectOutputStream(socket.getOutputStream())
                val socIn = ObjectInputStream(socket.getInputStream())
                readerJob = GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            while (!endCmdLine) {
                                println(socIn.readObject() as String? ?: "ERROR")
                            }
                        } catch (e: SocketException) {
                            println("Connection lost: $e")
                        } catch (e: EOFException) {
                            println("Connection lost: $e")
                        }
                    }
                }

                input@ while (!endCmdLine) {
                    val str = readLine() ?: continue
                    when (str.toUpperCase()) {
                        "" -> continue@input
                        "EXIT" -> exitProcess(0)
                        "Q", "QUIT" -> {
                            socOut.writeObject(str)
                            exitProcess(0)
                        }
                        else -> socOut.writeObject(str)
                    }
                }

            } catch (e: SocketException) {
                println("Connection lost: $e")
                readerJob?.cancel()
            } catch (e: EOFException) {
                println("Connection lost: $e")
                readerJob?.cancel()
            }
        }
    }
}