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
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.delayBlocking
import org.junit.Test
import org.pmw.tinylog.Level
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerParserTest {

    init {
        SocketConnections.hostIP = "0.0.0.0"
    }

    @Test
    fun testQuit() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        startLogCapture()
        testServer.parseTextCommand("quit", null)
        assertLogs(setOf(Pair(Level.WARNING, "Shutting down server")))
        stopLogCapture()
    }

    @Test
    fun testLogs() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        val connection = SocketConnections.Connection(9999, testServer)

        // logs on

        startLogCapture()
        testServer.parseTextCommand("logs on", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Enabled logs to port null")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("logs on", connection)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port 9999: Enabled logs to port 9999")))
        stopLogCapture()


        // logs off

        startLogCapture()
        testServer.parseTextCommand("logs off", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Disabled logs to port null")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("logs off", connection)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port 9999: Disabled logs to port 9999")))
        stopLogCapture()


        // logs level set info

        startLogCapture()
        testServer.parseTextCommand("logs level set info", null)
        assertLogs(setOf(Pair(Level.INFO, "Set logging level to info")))
        stopLogCapture()


        // logs level set debug

        startLogCapture()
        testServer.parseTextCommand("logs level set debug", null)
        assertLogs(setOf(Pair(Level.DEBUG, "Set logging level to debug")))
        stopLogCapture()


        // logs level set trace

        startLogCapture()
        testServer.parseTextCommand("logs level set trace", null)
        assertLogs(setOf(Pair(Level.TRACE, "Set logging level to trace")))
        stopLogCapture()


        // logs level get

        startLogCapture()
        testServer.parseTextCommand("logs level get", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Logging level is TRACE")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("logs level get", connection)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port 9999: Logging level is TRACE")))
        stopLogCapture()
    }

    @Test
    fun testStrip() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        // strip clear

        testServer.leds.wholeStrip.setProlongedStripColor(0xFF)

        testServer.leds.wholeStrip.pixelTemporaryColorList.forEach {
            assertTrue("Pixel $it check failed (temporary).") { it == 0xFFL }
        }

        testServer.leds.wholeStrip.pixelProlongedColorList.forEach {
            assertTrue("Pixel $it check failed (prolonged).") { it == 0xFFL }
        }

        startLogCapture()
        testServer.parseTextCommand("strip clear", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Cleared strip")))
        stopLogCapture()

        testServer.leds.wholeStrip.pixelTemporaryColorList.forEach {
            assertTrue("Pixel $it check failed (temporary).") { it == 0L }
        }

        testServer.leds.wholeStrip.pixelProlongedColorList.forEach {
            assertTrue("Pixel $it check failed (prolonged).") { it == 0L }
        }


        // strip info

        testServer.parseTextCommand("strip info", null)

        val connection = SocketConnections.Connection(9998, testServer)

        startLogCapture()
        testServer.parseTextCommand("strip info", connection)
        assertLogs(setOf(Pair(Level.DEBUG, "Could not send to port 9998: Not Connected")))
        stopLogCapture()
    }

    @Test
    fun testRunning() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        testServer.leds.wholeStrip.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "1234"
        )
        testServer.leds.wholeStrip.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "5678"
        )
        testServer.leds.wholeStrip.startAnimation(
            AnimationData(animation = "Alternate", continuous = true, delay = 50),
            "9101"
        )

        // running list

        startLogCapture()
        testServer.parseTextCommand("running list", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Running Animations: [1234, 5678, 9101]")))
        stopLogCapture()


        // running info

        startLogCapture()
        testServer.parseTextCommand("running info", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: ID of animation required")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("running info 1234", null)
        assertLogs(
            setOf(
                Pair(
                    Level.TRACE,
                    "Replying to client on port null: AnimationData for 1234\n" +
                            "  animation: Alternate\n" +
                            "  colors: [0]\n" +
                            "  center: 120\n" +
                            "  continuous: true\n" +
                            "  delay: 50\n" +
                            "  delayMod: 1.0\n" +
                            "  direction: FORWARD\n" +
                            "  distance: 240\n" +
                            "  section: \n" +
                            "  spacing: 3\n" +
                            "End AnimationData"
                )
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("running info 1234 5678", null)
        assertLogs(
            setOf(
                Pair(
                    Level.TRACE,
                    "Replying to client on port null: AnimationData for 1234\n" +
                            "  animation: Alternate\n" +
                            "  colors: [0]\n" +
                            "  center: 120\n" +
                            "  continuous: true\n" +
                            "  delay: 50\n" +
                            "  delayMod: 1.0\n" +
                            "  direction: FORWARD\n" +
                            "  distance: 240\n" +
                            "  section: \n" +
                            "  spacing: 3\n" +
                            "End AnimationData"
                ),
                Pair(
                    Level.TRACE,
                    "Replying to client on port null: AnimationData for 5678\n" +
                            "  animation: Alternate\n" +
                            "  colors: [0]\n" +
                            "  center: 120\n" +
                            "  continuous: true\n" +
                            "  delay: 50\n" +
                            "  delayMod: 1.0\n" +
                            "  direction: FORWARD\n" +
                            "  distance: 240\n" +
                            "  section: \n" +
                            "  spacing: 3\n" +
                            "End AnimationData"
                )
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("running info 4321", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: 4321: NOT FOUND")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("running info 5432 2654", null)
        assertLogs(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: 5432: NOT FOUND"),
                Pair(Level.TRACE, "Replying to client on port null: 2654: NOT FOUND")
            )
        )
        stopLogCapture()


        // running end

        assertTrue { testServer.leds.runningAnimations.ids.contains("1234") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("5678") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("9101") }

        startLogCapture()
        testServer.parseTextCommand("running end", null)
        assertLogsInclude(setOf(Pair(Level.TRACE, "Replying to client on port null: ID of animation required")))
        stopLogCapture()

        assertTrue { testServer.leds.runningAnimations.ids.contains("1234") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("5678") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("9101") }

        startLogCapture()
        testServer.parseTextCommand("running end 1234", null)
        delayBlocking(100)
        assertLogsInclude(setOf(Pair(Level.TRACE, "Replying to client on port null: Ending animation 1234")))
        stopLogCapture()


        assertFalse { testServer.leds.runningAnimations.ids.contains("1234") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("5678") }
        assertTrue { testServer.leds.runningAnimations.ids.contains("9101") }

        startLogCapture()
        testServer.parseTextCommand("running end 5678 9101", null)
        assertLogsInclude(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: Ending animation 5678"),
                Pair(Level.TRACE, "Replying to client on port null: Ending animation 9101")
            )
        )
        stopLogCapture()

        delayBlocking(200)

        assertFalse { testServer.leds.runningAnimations.ids.contains("1234") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("5678") }
        assertFalse { testServer.leds.runningAnimations.ids.contains("9101") }
    }

    @Test
    fun testAnimation() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        startLogCapture()
        testServer.parseTextCommand("animation", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Name of animation required")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("animation notananimation", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Animation notananimation not found")))
        stopLogCapture()

        testServer.parseTextCommand("animation mpr", null)

        val connection = SocketConnections.Connection(9997, testServer)

        startLogCapture()
        testServer.parseTextCommand("animation mpr", connection)
        assertLogs(
            setOf(
                Pair(Level.DEBUG, "Could not send to port 9997: Not Connected"),
                Pair(
                    Level.DEBUG,
                    "Sent AnimationInfo(name=Multi Pixel Run, abbr=MPR, description=Similar to " +
                            "[Pixel Run](Pixel-Run) but with multiple LEDs at a specified spacing., " +
                            "signatureFile=multi_pixel_run.png, repetitive=true, minimumColors=1, " +
                            "unlimitedColors=false, center=NOTUSED, delay=USED, direction=USED, " +
                            "distance=NOTUSED, spacing=USED, delayDefault=100, distanceDefault=-1, " +
                            "spacingDefault=3)"
                )
            )
        )
        stopLogCapture()
    }

    @Test
    fun testConnections() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        // connections add

        startLogCapture()
        testServer.parseTextCommand("connections list", null)
        assertLogs(setOf())
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections add", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Port must be specified")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections add x", null)
        assertLogs(setOf(Pair(Level.TRACE, """Replying to client on port null: Invalid port: "x"""")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections add 4001", null)
        assertLogs(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: Added port 4001"),
                Pair(Level.DEBUG, "Adding port 4001")
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections add 4002", null)
        assertLogs(
            setOf(
                Pair(Level.DEBUG, "Adding port 4002"),
                Pair(Level.TRACE, "Replying to client on port null: Added port 4002")
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections list", null)
        assertLogs(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: Port 4001: Stopped"),
                Pair(Level.TRACE, "Replying to client on port null: Port 4002: Stopped")
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections add 4002", null)
        assertLogs(
            setOf(
                Pair(Level.DEBUG, "Adding port 4002"),
                Pair(Level.TRACE, "Replying to client on port null: ERROR: Port 4002 already has a connection")
            )
        )
        stopLogCapture()


        // connections start

        startLogCapture()
        testServer.parseTextCommand("connections start", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Port must be specified")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections start x", null)
        assertLogs(setOf(Pair(Level.TRACE, """Replying to client on port null: Invalid port: "x"""")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections list", null)
        assertLogs(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: Port 4001: Stopped"),
                Pair(Level.TRACE, "Replying to client on port null: Port 4002: Stopped")
            )
        )
        stopLogCapture()

        testServer.running = true

        startLogCapture()
        testServer.parseTextCommand("connections start 4002", null)
        delayBlocking(100)
        assertLogs(
            setOf(
                Pair(Level.DEBUG, "Manually starting connection at port 4002"),
                Pair(Level.TRACE, "Replying to client on port null: Starting port 4002"),
                Pair(Level.DEBUG, "Starting port 4002"),
                Pair(Level.DEBUG, "Socket at port 4002 started")
            )
        )
        stopLogCapture()

        delayBlocking(500)

        startLogCapture()
        testServer.parseTextCommand("connections list", null)
        assertLogs(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: Port 4001: Stopped"),
                Pair(Level.TRACE, "Replying to client on port null: Port 4002: Waiting")
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections start 4003", null)
        assertLogs(
            setOf(
                Pair(Level.DEBUG, "Manually starting connection at port 4003"),
                Pair(Level.TRACE, "Replying to client on port null: Starting port 4003"),
                Pair(Level.TRACE, "Replying to client on port null: ERROR: No connection on port 4003")
            )
        )
        stopLogCapture()


        // connections stop

        startLogCapture()
        testServer.parseTextCommand("connections list", null)
        assertLogs(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: Port 4001: Stopped"),
                Pair(Level.TRACE, "Replying to client on port null: Port 4002: Waiting")
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections stop", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Port must be specified")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections stop x", null)
        assertLogs(setOf(Pair(Level.TRACE, """Replying to client on port null: Invalid port: "x"""")))
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections list", null)
        assertLogs(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: Port 4001: Stopped"),
                Pair(Level.TRACE, "Replying to client on port null: Port 4002: Waiting")
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections stop 4002", null)
        assertLogs(
            setOf(
                Pair(Level.DEBUG, "Manually stopping connection at port 4002"),
                Pair(Level.TRACE, "Replying to client on port null: Stopping port 4002")
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections list", null)
        assertLogs(
            setOf(
                Pair(Level.TRACE, "Replying to client on port null: Port 4001: Stopped"),
                Pair(Level.TRACE, "Replying to client on port null: Port 4002: Stopped")
            )
        )
        stopLogCapture()

        startLogCapture()
        testServer.parseTextCommand("connections stop 4003", null)
        assertLogs(
            setOf(
                Pair(Level.DEBUG, "Manually stopping connection at port 4003"),
                Pair(Level.TRACE, "Replying to client on port null: Stopping port 4003"),
                Pair(Level.TRACE, "Replying to client on port null: ERROR: No connection on port 4003")
            )
        )
        stopLogCapture()

    }

    @Test
    fun testNonCommand() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        startLogCapture()
        testServer.parseTextCommand("imnotacommand", null)
        assertLogs(setOf(Pair(Level.TRACE, "Replying to client on port null: Bad Command: imnotacommand")))
        stopLogCapture()
    }
}
