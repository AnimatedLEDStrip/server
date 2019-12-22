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

import animatedledstrip.animationutils.Animation
import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.addColor
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.delayBlocking
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerParserTest {

    init {
        SocketConnections.hostIP = "0.0.0.0"
    }

    @Test
    fun testClear() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        testServer.leds.addAnimation(AnimationData().addColor(0xFF))
        delayBlocking(500)
        checkAllPixels(testServer.leds as EmulatedAnimatedLEDStrip, 0xFF)

        testServer.parseTextCommand("clear")
        delayBlocking(500)
        checkAllPixels(testServer.leds as EmulatedAnimatedLEDStrip, 0x0)
    }

    @Test
    fun testEnd() {
        val stderr: PrintStream = System.err
        val tempOut = ByteArrayOutputStream()
        System.setErr(PrintStream(tempOut))

        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        testServer.leds.addAnimation(
            AnimationData(animation = Animation.ALTERNATE, continuous = true, delay = 50),
            "1234"
        )
        testServer.leds.addAnimation(
            AnimationData(animation = Animation.ALTERNATE, continuous = true, delay = 50),
            "1357"
        )
        testServer.leds.addAnimation(
            AnimationData(animation = Animation.ALTERNATE, continuous = true, delay = 50),
            "2431"
        )
        testServer.leds.addAnimation(
            AnimationData(animation = Animation.ALTERNATE, continuous = true, delay = 50),
            "7654"
        )
        testServer.leds.addAnimation(
            AnimationData(animation = Animation.ALTERNATE, continuous = true, delay = 50),
            "2653"
        )
        testServer.leds.addAnimation(
            AnimationData(animation = Animation.ALTERNATE, continuous = true, delay = 50),
            "2521"
        )

        assertTrue { testServer.leds.runningAnimations.ids.contains("1234") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("1357") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2431") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("7654") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2653") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2521") }

        testServer.parseTextCommand("end 1234")
        delayBlocking(500)
        assertFalse { testServer.leds.runningAnimations.ids.contains("1234") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("1357") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2431") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("7654") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2653") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2521") }

        testServer.parseTextCommand("end 1357 2431")
        delayBlocking(500)
        assertFalse { testServer.leds.runningAnimations.ids.contains("1234") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("1357") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("2431") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("7654") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2653") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2521") }

        testServer.parseTextCommand("end all")
        delayBlocking(500)
        assertFalse { testServer.leds.runningAnimations.ids.contains("1234") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("1357") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("2431") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("7654") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("2653") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("2521") }

        tempOut.reset()

        testServer.parseTextCommand("end")
        assertTrue {
            tempOut
                .toString("utf-8")
                .replace("\r\n", "\n") ==
                    "WARNING: Animation ID or \"all\" must be specified\n"
        }
        tempOut.reset()

        System.setErr(stderr)
    }

    @Test
    fun testShow() {
        val stdout: PrintStream = System.out
        val tempOut = ByteArrayOutputStream()
        System.setOut(PrintStream(tempOut))

        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        testServer.parseTextCommand("show")
        assertTrue {
            tempOut
                .toString("utf-8")
                .replace("\r\n", "\n") ==
                    "INFO:    Running Animations: []\n"
        }
        tempOut.reset()

        testServer.parseTextCommand("show 1234")
        assertTrue {
            tempOut
                .toString("utf-8")
                .replace("\r\n", "\n") ==
                    "INFO:    1234: NOT FOUND\n"
        }
        tempOut.reset()

        testServer.leds.addAnimation(AnimationData(animation = Animation.ALTERNATE, continuous = true), "5678")
        delayBlocking(500)
        testServer.parseTextCommand("show")
        assertTrue {
            tempOut
                .toString("utf-8")
                .replace("\r\n", "\n") ==
                    "INFO:    Running Animations: [5678]\n"
        }
        tempOut.reset()

        testServer.parseTextCommand("show 5678")
        assertTrue {
            tempOut
                .toString("utf-8")
                .replace("\r\n", "\n") ==
                    "INFO:    5678: AnimationData(animation=ALTERNATE, colors=[0], center=120, continuous=true, delay=1000, delayMod=1.0, direction=FORWARD, distance=240, endPixel=239, id=5678, spacing=3, startPixel=0)\n"
        }
        tempOut.reset()
        testServer.parseTextCommand("end 5678")

        System.setOut(stdout)
    }

    @Test
    fun testNonCommand() {
        val stderr: PrintStream = System.err
        val tempOut = ByteArrayOutputStream()
        System.setErr(PrintStream(tempOut))

        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        testServer.parseTextCommand("notacommand")
        delayBlocking(200)
        assertTrue {
            tempOut
                .toString("utf-8")
                .replace("\r\n", "\n")
                .replace("LOGGER ERROR: Cannot find a writer for the name \"socket\"\n", "") ==
                    "WARNING: notacommand is not a valid command\n"
        }

        System.setErr(stderr)
    }

    @Test
    fun testConnections() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/ports.config"), EmulatedAnimatedLEDStrip::class)
        testServer.parseTextCommand("connections list")
        testServer.start()
        delayBlocking(5000)

        testServer.parseTextCommand("connections list")
    }
}