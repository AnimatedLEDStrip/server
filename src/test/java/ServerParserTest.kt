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

import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.addColor
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.SocketConnections
import animatedledstrip.server.startServer
import animatedledstrip.utils.delayBlocking
import org.junit.Test
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

        testServer.leds.startAnimation(AnimationData().addColor(0xFF))
        delayBlocking(500)
        checkAllPixels(testServer.leds as EmulatedAnimatedLEDStrip, 0xFF)

        testServer.parseTextCommand("clear", null)
        delayBlocking(500)
        checkAllPixels(testServer.leds as EmulatedAnimatedLEDStrip, 0x0)
    }

    @Test
    fun testEnd() {
        val testServer =
            AnimatedLEDStripServer(
                arrayOf("-f", "src/test/resources/empty.config", "-P", "3200"),
                EmulatedAnimatedLEDStrip::class
            ).start()

        testServer.leds.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "1234"
        )
        testServer.leds.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "1357"
        )
        testServer.leds.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "2431"
        )
        testServer.leds.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "7654"
        )
        testServer.leds.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "2653"
        )
        testServer.leds.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "2521"
        )

        assertTrue { testServer.leds.runningAnimations.ids.contains("1234") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("1357") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2431") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("7654") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2653") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2521") }

        testServer.parseTextCommand("end 1234", null)
        delayBlocking(2000)
        assertFalse { testServer.leds.runningAnimations.ids.contains("1234") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("1357") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2431") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("7654") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2653") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2521") }

        testServer.parseTextCommand("end 1357 2431", null)
        delayBlocking(2000)
        assertFalse { testServer.leds.runningAnimations.ids.contains("1234") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("1357") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("2431") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("7654") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2653") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("2521") }

        testServer.parseTextCommand("end all", null)
        delayBlocking(2000)
        assertFalse { testServer.leds.runningAnimations.ids.contains("1234") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("1357") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("2431") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("7654") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("2653") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("2521") }

        redirectOutput()

        newCommandStream("end\n")
        startServer(arrayOf("-CP", "3200"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Animation ID or \"all\" must be specified")
    }

    @Test
    fun testRunning() {
        val testServer =
            AnimatedLEDStripServer(
                arrayOf("-qf", "src/test/resources/empty.config", "-P", "3201 3202 3203 3204"),
                EmulatedAnimatedLEDStrip::class
            ).start()
        delayBlocking(500)

        redirectOutput()

        newCommandStream("running\n")
        startServer(arrayOf("-CP", "3201"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Running Animations: []")

        newCommandStream("running 1234\n")
        startServer(arrayOf("-CP", "3202"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "1234: NOT FOUND")

        testServer.leds.startAnimation(AnimationData(animation = "Alternate", continuous = true), "5678")
        delayBlocking(500)
        newCommandStream("running\n")
        startServer(arrayOf("-CP", "3203"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Running Animations: [5678]")

        newCommandStream("running 5678\n")
        startServer(arrayOf("-CP", "3204"), EmulatedAnimatedLEDStrip::class)
        checkOutput(
            expected = "5678: AnimationData(animation=Alternate, colors=[0], center=120, continuous=true, delay=1000, delayMod=1.0, direction=FORWARD, distance=240, id=5678, section=, spacing=3)"
        )
    }

    @Test
    fun testConnections() {
        AnimatedLEDStripServer(
            arrayOf("-qf", "src/test/resources/ports.config", "-P", "3205"),
            EmulatedAnimatedLEDStrip::class
        ).start()
        delayBlocking(1500)

        redirectOutput()

        val connection = SocketConnections.connections[3205]!!

        newCommandStream("connections list\n")
        startServer(arrayOf("-CP", "3205"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Port 3005: WaitingPort 3006: WaitingPort 3007: WaitingPort 3205: Connected")

        connection.reset()
        newCommandStream("c list\n")
        startServer(arrayOf("-CP", "3205"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Port 3005: WaitingPort 3006: WaitingPort 3007: WaitingPort 3205: Connected")

        connection.reset()
        newCommandStream("connections stop 3005\n")
        startServer(arrayOf("-CP", "3205"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Stopping port 3005")

        connection.reset()
        newCommandStream("c list\n")
        startServer(arrayOf("-CP", "3205"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Port 3005: StoppedPort 3006: WaitingPort 3007: WaitingPort 3205: Connected")

        connection.reset()
        newCommandStream("connections start 3005\n")
        startServer(arrayOf("-CP", "3205"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Starting port 3005")

        connection.reset()
        newCommandStream("c list\n")
        startServer(arrayOf("-CP", "3205"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Port 3005: WaitingPort 3006: WaitingPort 3007: WaitingPort 3205: Connected")

        connection.reset()
        newCommandStream("connections add 3008\n")
        startServer(arrayOf("-CP", "3205"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Added port 3008")

        connection.reset()
        newCommandStream("c list\n")
        startServer(arrayOf("-CP", "3205"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "Port 3005: WaitingPort 3006: WaitingPort 3007: WaitingPort 3008: StoppedPort 3205: Connected")
    }

    @Test
    fun testNonCommand() {
        AnimatedLEDStripServer(
            arrayOf("-qf", "src/test/resources/empty.config", "-P", "3206"),
            EmulatedAnimatedLEDStrip::class
        ).start()
        delayBlocking(500)

        redirectOutput()

        newCommandStream("notacommand\n")
        startServer(arrayOf("-CP", "3206"), EmulatedAnimatedLEDStrip::class)
        checkOutput(expected = "BAD COMMAND: NOTACOMMAND")
    }

    @Test
    fun testHelp() {
        val testServer =
            AnimatedLEDStripServer(
                arrayOf("-qf", "src/test/resources/empty.config"),
                EmulatedAnimatedLEDStrip::class
            ).start()
        delayBlocking(500)

        testServer.parseTextCommand("help", null)
    }
}
