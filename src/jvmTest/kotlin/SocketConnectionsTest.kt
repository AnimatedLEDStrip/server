/*
 *  Copyright (c) 2018-2020 AnimatedLEDStrip
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

package animatedledstrip.test

//import animatedledstrip.leds.emulation.EmulatedWS281x
//import animatedledstrip.server.AnimatedLEDStripServer
//import animatedledstrip.server.SocketConnections
//import kotlinx.coroutines.*
//import org.junit.Ignore
//import org.junit.Test
//import org.pmw.tinylog.Configurator
//import org.pmw.tinylog.Level
//import java.io.ByteArrayOutputStream
//import java.io.PrintStream
//import java.net.Socket
//import kotlin.test.assertTrue
//
//class SocketConnectionsTest {
//
//    @Test
//    fun testAdd() {
//        val server =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        SocketConnections.hostIP = "0.0.0.0"
//        SocketConnections.add(1200, server)
//
//        assertTrue { SocketConnections.connections.containsKey(1200) }
//    }
//
//    @Test
//    fun testOpenSocket() = runBlocking {
//        withTimeout(60000) {
//            val server =
//                AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class).start()
//            SocketConnections.hostIP = "0.0.0.0"
//            val c = SocketConnections.add(1201, server)
//            c.open()
//
//            val job = GlobalScope.launch(Dispatchers.IO) {
//                val socket = Socket("0.0.0.0", 1201)
//
//                Thread.sleep(5000)
//
//                server.stop()
//                socket.close()
//            }
//            job.join()
//        }
//    }
//
//    @Test
//    fun testToString() {
//        val port = 1202
//
//        val server =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        SocketConnections.hostIP = "0.0.0.0"
//        val socket = SocketConnections.add(port, server)
//
//        assertTrue { socket.toString() == "Connection@0.0.0.0:$port" }
//    }
//
//    @Test
//    @Ignore
//    fun testDisconnection() {
//        val port = 1203
//        val server =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class).start()
//
//        val stderr: PrintStream = System.err
//        val tempOut = ByteArrayOutputStream()
//        System.setErr(PrintStream(tempOut))
//
//        tempOut.reset()
//        Configurator.defaultConfig()
//            .formatPattern("{{level}:|min-size=8} {message}")
//            .level(Level.WARNING)
//            .activate()
//
//        SocketConnections.add(port, server).open()
//
//        val socket = Socket("0.0.0.0", port)
//
//        Thread.sleep(5000)
//
//        server.stop()
//        socket.close()
//        Thread.sleep(2000)
//
//        assertTrue {
//            Regex(
//                "WARNING: Connection on port $port lost: " +
//                "java.net.SocketException.*\n"
//            ).matches(
//                tempOut
//                    .toString("utf-8")
//                    .replace("\r\n", "\n")
//            )
//        }
//
//        System.setErr(stderr)
//        Configurator.defaultConfig()
//            .level(Level.OFF)
//            .activate()
//    }
//
//}
