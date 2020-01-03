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
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.delayBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnimatedLEDStripServerTest {

    init {
        SocketConnections.hostIP = "0.0.0.0"
    }

    @Test
    fun testStartStop() = runBlocking {
        val server =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        server.start()
        delay(5000)
        server.stop()
        Unit
    }

    @Test
    fun testPropertyFileName() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer1.propertyFileName == "led.config" }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.propertyFileName == "src/test/resources/led.config" }
    }

    @Test
    fun testOutputFileName() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertNull(testServer1.outputFileName)

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-qio", "out.csv"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.outputFileName == "out.csv" }

//        assertFailsWith<InvocationTargetException> {
//            AnimatedLEDStripServer(arrayOf("-qo", "out.csv"), EmulatedAnimatedLEDStrip::class)
//        }
    }

    @Test
    fun testLoggingPattern() {
        AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        AnimatedLEDStripServer(arrayOf("-qv"), EmulatedAnimatedLEDStrip::class)
    }

    @Test
    fun testLoggingLevels() {
        AnimatedLEDStripServer(arrayOf("-tf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.TRACE }
        AnimatedLEDStripServer(arrayOf("-tqf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.TRACE }
        AnimatedLEDStripServer(arrayOf("-dtf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.TRACE }
        AnimatedLEDStripServer(arrayOf("-qdtf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.TRACE }

        AnimatedLEDStripServer(arrayOf("-df", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.DEBUG }
        AnimatedLEDStripServer(arrayOf("-qdf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.DEBUG }

        AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.OFF }

        AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.INFO }
    }

    @Test
    fun testLoadProperties() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer1.properties.isEmpty }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.properties.isNotEmpty() }
    }

    @Test
    fun testImageDebuggingEnabled() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertFalse { testServer1.imageDebuggingEnabled }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-qi"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.imageDebuggingEnabled }
    }

    @Test
    fun testNumLEDs() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer1.numLEDs == 240 }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-qn", "50"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.numLEDs == 50 }

        val testServer3 =
            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer3.numLEDs == 120 }

        val testServer4 =
            AnimatedLEDStripServer(
                arrayOf("-qn", "100", "-f", "src/test/resources/led.config"),
                EmulatedAnimatedLEDStrip::class
            )
        assertTrue { testServer4.numLEDs == 100 }

        val testServer5 =
            AnimatedLEDStripServer(arrayOf("-qn", "x"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer5.numLEDs == 240 }
    }


    @Test
    fun testPin() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer1.pin == 12 }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-qp", "20"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.pin == 20 }

        val testServer3 =
            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer3.pin == 15 }

        val testServer4 =
            AnimatedLEDStripServer(
                arrayOf("-qp", "10", "-f", "src/test/resources/led.config"),
                EmulatedAnimatedLEDStrip::class
            )
        assertTrue { testServer4.pin == 10 }

        val testServer5 =
            AnimatedLEDStripServer(arrayOf("-qp", "x"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer5.pin == 12 }
    }

    @Test
    fun testPorts() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer1.ports.isEmpty() }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/ports.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.ports.contains(3005) }
        assertTrue { testServer2.ports.contains(3006) }
        assertTrue { testServer2.ports.contains(3007) }

        assertFailsWith<IllegalArgumentException> {
            AnimatedLEDStripServer(
                arrayOf("-qf", "src/test/resources/ports.badconfig"),
                EmulatedAnimatedLEDStrip::class
            )
        }
    }

    @Test
    fun testRendersBeforeSave() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer1.rendersBeforeSave == 1000 }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-qr", "500"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.rendersBeforeSave == 500 }

        val testServer3 =
            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer3.rendersBeforeSave == 2000 }

        val testServer4 =
            AnimatedLEDStripServer(
                arrayOf("-qr", "750", "-f", "src/test/resources/led.config"),
                EmulatedAnimatedLEDStrip::class
            )
        assertTrue { testServer4.rendersBeforeSave == 750 }

        val testServer5 =
            AnimatedLEDStripServer(arrayOf("-qr", "x"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer5.rendersBeforeSave == 1000 }
    }

    @Test
    fun testPersistAnimations() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertFalse { testServer1.persistAnimations }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-q", "--persist"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer2.persistAnimations }

        val testServer3 =
            AnimatedLEDStripServer(arrayOf("-q", "--persist", "--no-persist"), EmulatedAnimatedLEDStrip::class)
        assertFalse { testServer3.persistAnimations }

        val testServer4 =
            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { testServer4.persistAnimations }

        val testServer5 =
            AnimatedLEDStripServer(
                arrayOf("-q", "--persist", "-f", "src/test/resources/led.config"),
                EmulatedAnimatedLEDStrip::class
            )
        assertTrue { testServer5.persistAnimations }

        val testServer6 =
            AnimatedLEDStripServer(
                arrayOf("-q", "--persist", "-f", "src/test/resources/led.config", "--no-persist"),
                EmulatedAnimatedLEDStrip::class
            )
        assertFalse { testServer6.persistAnimations }

        val testServer7 =
            AnimatedLEDStripServer(
                arrayOf("-qf", "src/test/resources/no-persist.config"),
                EmulatedAnimatedLEDStrip::class
            )
        assertFalse { testServer7.persistAnimations }

        val testServer8 =
            AnimatedLEDStripServer(
                arrayOf("-q", "--persist", "-f", "src/test/resources/no-persist.config"),
                EmulatedAnimatedLEDStrip::class
            )
        assertTrue { testServer8.persistAnimations }

        val testServer9 =
            AnimatedLEDStripServer(
                arrayOf("-q", "--persist", "f", "src/test/resources/no-persist.config", "--no-persist"),
                EmulatedAnimatedLEDStrip::class
            )
        assertFalse { testServer9.persistAnimations }
    }

    @Test
    fun testHelp() {
        redirectOutput()

        AnimatedLEDStripServer(arrayOf("-hq"), EmulatedAnimatedLEDStrip::class)

        checkOutput(
            expected =
            "usage: ledserver.jar\n" +
                    " -C,--command-line      Connect to a running server with a command line\n" +
                    " -d,--debug             Enable debug level logging\n" +
                    " -E,--emulate           Emulate the LED strip\n" +
                    " -f,--prop-file <arg>   Specify properties file\n" +
                    " -h,--help              Show help message\n" +
                    " -i,--image-debug       Enable image debugging\n" +
                    " -n,--numleds <arg>     Specify number of LEDs\n" +
                    "    --no-persist        Don't persist animations (overrides --persist and\n" +
                    "                        persist=true)\n" +
                    " -o,--outfile <arg>     Specify the output file name for image debugging\n" +
                    " -p,--pin <arg>         Specify pin\n" +
                    " -P,--port <arg>        Add a port for clients to connect to\n" +
                    "    --persist           Persist animations across restarts\n" +
                    " -q,--quiet             Disable log outputs\n" +
                    " -r,--renders <arg>     Specify the number of renders between saves\n" +
                    " -t,--trace             Enable trace level logging\n" +
                    " -T                     Run test animation\n" +
                    " -v,--verbose           Enable verbose logging statements\n"
        )

    }

    @Test
    fun testTestAnimation() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-qT"), EmulatedAnimatedLEDStrip::class).start()
        delayBlocking(500)
        checkAllPixels(testServer.leds as EmulatedAnimatedLEDStrip, 0xFF)
        testServer.stop()
    }

    @Test
    fun testPrimaryConstructor() {
        AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
    }

    @Test
    fun testSetLoggingLevel() {
        redirectOutput()

        val testServer =
            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.OFF }

        testServer.parseTextCommand("trace", null)
        assertTrue { Logger.getLevel() == Level.TRACE }
        checkOutput(expected = "TRACE:   Set logging level to trace\n")

        testServer.parseTextCommand("debug", null)
        assertTrue { Logger.getLevel() == Level.DEBUG }
        checkOutput(expected = "TRACE:   Parsing \"debug\"\nDEBUG:   Set logging level to debug\n")

        testServer.parseTextCommand("info", null)
        assertTrue { Logger.getLevel() == Level.INFO }
        checkOutput(expected = "INFO:    Set logging level to info\n")

        Configurator.currentConfig().level(Level.OFF).activate()
    }
}