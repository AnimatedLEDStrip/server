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
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertTrue

class CommandLineTest {

    @Test
    fun testCommandLine() {
        GlobalScope.launch {
            delay(5000)

            redirectOutput()

            val stream = ByteArrayInputStream("quit".toByteArray())
            System.setIn(stream)
            startServer(arrayOf("-CP", "3101"), EmulatedAnimatedLEDStrip::class)

            assertTrue {
                outStream
                    .toString("utf-8")
                    .replace("\r\n", "\n") ==
                        "Welcome to the AnimatedLEDStrip Server console\nConnected\n"
            }
        }
        startServer(arrayOf("-qP", "3101"), EmulatedAnimatedLEDStrip::class)
    }

    @Test
    fun testQuit() {
        GlobalScope.launch {
            delay(5000)

            val stream = ByteArrayInputStream("q".toByteArray())
            System.setIn(stream)
            startServer(arrayOf("-qCP", "3102"), EmulatedAnimatedLEDStrip::class)
        }
        startServer(arrayOf("-qP", "3102"), EmulatedAnimatedLEDStrip::class)
    }

    @Test
    fun testNoCommand() {
        GlobalScope.launch {
            delay(5000)

            val stdout = System.out
            val tempOut = ByteArrayOutputStream()
            System.setOut(PrintStream(tempOut))

            val stream = ByteArrayInputStream("\nquit".toByteArray())
            System.setIn(stream)
            startServer(arrayOf("-CP", "3103"), EmulatedAnimatedLEDStrip::class)

            System.setOut(stdout)
        }
        startServer(arrayOf("-qP", "3103"), EmulatedAnimatedLEDStrip::class)
    }

    @Test
    fun testExit() {
        GlobalScope.launch {
            delay(5000)

            val stream = ByteArrayInputStream("exit".toByteArray())
            System.setIn(stream)
            startServer(arrayOf("-qCP", "3104"), EmulatedAnimatedLEDStrip::class)
        }
        val testServer =
            AnimatedLEDStripServer(arrayOf("-qP", "3104"), EmulatedAnimatedLEDStrip::class).start()
        delayBlocking(10000)
        testServer.stop()
    }

    @Test
    fun testNoConnection() {
        redirectOutput()

        val stream = ByteArrayInputStream("quit".toByteArray())
        System.setIn(stream)
        startServer(arrayOf("-CP", "3105"), EmulatedAnimatedLEDStrip::class)

        assertTrue {
            outStream
                .toString("utf-8")
                .replace("\r\n", "\n") ==
                    "Welcome to the AnimatedLEDStrip Server console\nCould not connect to server\n"
        }
    }
}