package animatedledstrip.cmdline

/*
 *  Copyright (c) 2019 AnimatedLEDStrip
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */


import kotlinx.coroutines.*
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OptionalDataException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import kotlin.system.exitProcess

class CommandLine {

    private val socket = Socket()

    private var endCmdLine = false

    private var readerJob: Job? = null

    init {
        Configurator.defaultConfig().level(Level.OFF).activate()
    }

    fun start() {
        println("Welcome to the AnimatedLEDStrip Server console")
        try {
            socket.connect(InetSocketAddress("localhost", 1118), 5000)
        } catch (e: Exception) {
            println("Could not connect to server")
            exitProcess(1)
        }
        println("Connected")
        try {
            val socOut = ObjectOutputStream(socket.getOutputStream())
            val socIn = ObjectInputStream(socket.getInputStream())
            readerJob = GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        while (!endCmdLine) {
                            try {
                                println(socIn.readObject() as String? ?: "ERROR")
                            } catch (e: OptionalDataException) {
                                println("Exception: $e")
                            }
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
            exitProcess(1)
        } catch (e: EOFException) {
            println("Connection lost: $e")
            readerJob?.cancel()
            exitProcess(1)
        }
    }
}