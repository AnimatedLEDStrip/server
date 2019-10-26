package animatedledstrip.test

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


import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.delayBlocking
import kotlinx.coroutines.*
import org.junit.Test
import java.net.Socket
import kotlin.test.assertTrue

class SocketConnectionsTest {

    @Test
    fun testAdd() {
        val server =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        SocketConnections.hostIP = "0.0.0.0"
        SocketConnections.add(1200, server)

        assertTrue { SocketConnections.connections.containsKey(1200) }
    }

    @Test
    fun testOpenSocket() = runBlocking {
        withTimeout(60000) {
            val server =
                AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class).start()
            SocketConnections.hostIP = "0.0.0.0"
            val c = SocketConnections.add(1201, server)
            c.open()

            val job = GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val socket = Socket("0.0.0.0", 1201)

                    delayBlocking(5000)

                    server.stop()
                    socket.shutdownOutput()
                }
            }
            job.join()
        }
    }

}