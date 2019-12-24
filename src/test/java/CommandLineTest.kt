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

package animatedledstrip.test

import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.startServer
import animatedledstrip.utils.delayBlocking
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertTrue

class CommandLineTest {

    @Test
    fun testCommandLine() = runBlocking {
        GlobalScope.launch {
            delay(5000)

            val stdout = System.out
            val tempOut = ByteArrayOutputStream()
            System.setOut(PrintStream(tempOut))

            val stream = ByteArrayInputStream("quit".toByteArray())
            System.setIn(stream)
            startServer(arrayOf("-CL", "3101"), EmulatedAnimatedLEDStrip::class)

            assertTrue {
                tempOut
                    .toString("utf-8")
                    .replace("\r\n", "\n") ==
                        "Welcome to the AnimatedLEDStrip Server console\nConnected\n"
            }

            System.setOut(stdout)
        }
        startServer(arrayOf("-qL", "3101"), EmulatedAnimatedLEDStrip::class)
        Unit
    }

    @Test
    fun testQuit() = runBlocking {
        GlobalScope.launch {
            delay(5000)

            val stream = ByteArrayInputStream("q".toByteArray())
            System.setIn(stream)
            startServer(arrayOf("-qCL", "3102"), EmulatedAnimatedLEDStrip::class)
        }
        startServer(arrayOf("-qL", "3102"), EmulatedAnimatedLEDStrip::class)
        Unit
    }

    @Test
    fun testNoCommand() = runBlocking {
        GlobalScope.launch {
            delay(5000)

            val stdout = System.out
            val tempOut = ByteArrayOutputStream()
            System.setOut(PrintStream(tempOut))

            val stream = ByteArrayInputStream("\nquit".toByteArray())
            System.setIn(stream)
            startServer(arrayOf("-CL", "3103"), EmulatedAnimatedLEDStrip::class)

            System.setOut(stdout)
        }
        startServer(arrayOf("-qL", "3103"), EmulatedAnimatedLEDStrip::class)
        Unit
    }

    @Test
    fun testExit() = runBlocking {
        GlobalScope.launch {
            delay(5000)

            val stream = ByteArrayInputStream("exit".toByteArray())
            System.setIn(stream)
            startServer(arrayOf("-qCL", "3104"), EmulatedAnimatedLEDStrip::class)
        }
        val testServer =
            AnimatedLEDStripServer(arrayOf("-qL", "3104"), EmulatedAnimatedLEDStrip::class).start()
        delayBlocking(10000)
        testServer.stop()
        Unit
    }

    @Test
    fun testNoConnection() {
        val stdout = System.out
        val tempOut = ByteArrayOutputStream()
        System.setOut(PrintStream(tempOut))

        val stream = ByteArrayInputStream("quit".toByteArray())
        System.setIn(stream)
        startServer(arrayOf("-CL", "3105"), EmulatedAnimatedLEDStrip::class)

        assertTrue {
            tempOut
                .toString("utf-8")
                .replace("\r\n", "\n") ==
                    "Welcome to the AnimatedLEDStrip Server console\nCould not connect to server\n"
        }

        System.setOut(stdout)
    }


}