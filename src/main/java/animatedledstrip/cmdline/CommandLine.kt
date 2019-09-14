package animatedledstrip.cmdline

import kotlinx.coroutines.*
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.net.Socket

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
                        while (!endCmdLine) {
                            println(socIn.readObject() as String? ?: "ERROR")
                        }
                    }
                }

                while (!endCmdLine) {
                    print("> ")
                    val str = readLine() ?: continue
                    socOut.writeObject(str)
                    when (str.toUpperCase()) {
                        "Q", "QUIT" -> endCmdLine = true
                    }
                }

            } catch (e: Exception) {
                readerJob?.cancel()
            }
        }
    }
}