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

package animatedledstrip.cmdline

import kotlinx.coroutines.*
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import java.io.EOFException
import java.io.OptionalDataException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.nio.charset.Charset

class CommandLine(private val port: Int, private val quiet: Boolean = false) {

    private val socket = Socket()

    private var endCmdLine = false

    private lateinit var readerJob: Job

    init {
        Configurator.defaultConfig().level(Level.OFF).activate()
    }

    private fun println(message: String) {
        if (!quiet) kotlin.io.println(message)
    }

    private fun sendCmd(cmd: String, stream: OutputStream) {
        stream.write("CMD :$cmd".toByteArray())
    }

    fun start() {
        println("Welcome to the AnimatedLEDStrip Server console")
        try {
            socket.connect(InetSocketAddress("localhost", port), 5000)
        } catch (e: Exception) {
            println("Could not connect to server")
            return
        }
        println("Connected")
        try {
            val socOut = socket.getOutputStream()
            val socIn = socket.getInputStream()
            readerJob = GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        while (true) {
                            val input = ByteArray(1000)
                            try {
                                socIn.read(input)
                                println(input.toString(Charset.forName("utf-8")))
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
                    "EXIT" -> {
                        readerJob.cancel()
                        return
                    }
                    "Q", "QUIT" -> {
                        sendCmd(str, socOut)
                        readerJob.cancel()
                        return
                    }
                    "HELP" -> {
                        kotlin.io.println("""
                            Valid commands:
                            quit          Stop the server and close the command line connection
                            exit          Close the command line connection (server continues to run)
                            debug         Change logging level to debug
                            trace         Change logging level to trace
                            info          Change logging level to info
                            logs [on|off] Turn logs to this port on or off
                            clear         Clear the LED strip (i.e. turn off all pixels)
                            show          Print a list of all running animations
                            show ID       Print information about a specific running animation
                            end ID        End a specific running animation
                            end all       End all running animations
                            help          Show this help message
                        """.trimIndent())
                    }
                    else -> sendCmd(str, socOut)
                }
            }

        } catch (e: SocketException) {
            println("Connection lost: $e")
            readerJob.cancel()
            return
        } catch (e: EOFException) {
            println("Connection lost: $e")
            readerJob.cancel()
            return
        }
    }
}