/*
 * Copyright (c) 2018-2021 AnimatedLEDStrip
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package animatedledstrip.test

import animatedledstrip.leds.emulation.EmulatedWS281x
import animatedledstrip.server.AnimatedLEDStripServer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimatedLEDStripServerTest : StringSpec(
    {
        "test start stop" {
            val server = newTestServer()
            server.start()
            server.running.shouldBeTrue()
            server.leds.renderer.isRendering.shouldBeTrue()
            server.stop()
            server.running.shouldBeFalse()
            server.leds.renderer.isRendering.shouldBeFalse()
        }
    }
) {

//    init {
//        SocketConnections.hostIP = "0.0.0.0"
//    }

    @Test
    fun testStartStop() = runBlocking {
        val server =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
        server.start()
        delay(5000)
        server.stop()
        Unit
    }

//    @Test
//    fun testPropertyFileName() {
//        val testServer1 =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertTrue { testServer1.propertyFileName == "/etc/leds/led.config" }
//
//        val testServer2 =
//            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { testServer2.propertyFileName == "src/test/resources/led.config" }
//    }
//
//    @Test
//    fun testOutputFileName() {
//        val testServer1 =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertNull(testServer1.outputFileName)
//
//        val testServer2 =
//            AnimatedLEDStripServer(arrayOf("-qio", "out.csv"), EmulatedWS281x::class)
//        assertTrue { testServer2.outputFileName == "out.csv" }
//    }
//
//    @Test
//    fun testLoggingPattern() {
//        AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        AnimatedLEDStripServer(arrayOf("-qv"), EmulatedWS281x::class)
//    }
//
//    @Test
//    fun testLoggingLevels() {
//        AnimatedLEDStripServer(arrayOf("-tf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { Logger.getLevel() == Level.TRACE }
//        AnimatedLEDStripServer(arrayOf("-tqf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { Logger.getLevel() == Level.TRACE }
//        AnimatedLEDStripServer(arrayOf("-dtf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { Logger.getLevel() == Level.TRACE }
//        AnimatedLEDStripServer(arrayOf("-qdtf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { Logger.getLevel() == Level.TRACE }
//
//        AnimatedLEDStripServer(arrayOf("-df", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { Logger.getLevel() == Level.DEBUG }
//        AnimatedLEDStripServer(arrayOf("-qdf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { Logger.getLevel() == Level.DEBUG }
//
//        AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertTrue { Logger.getLevel() == Level.OFF }
//
//        AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { Logger.getLevel() == Level.INFO }
//    }
//
//    @Test
//    fun testLoadProperties() {
//        val testServer1 =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertTrue { testServer1.properties.isEmpty }
//
//        val testServer2 =
//            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { testServer2.properties.isNotEmpty() }
//    }
//
//    @Test
//    fun testImageDebuggingEnabled() {
//        val testServer1 =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertFalse { testServer1.colorLoggingEnabled }
//
//        val testServer2 =
//            AnimatedLEDStripServer(arrayOf("-qi"), EmulatedWS281x::class)
//        assertTrue { testServer2.colorLoggingEnabled }
//    }
//
//    @Test
//    fun testNumLEDs() {
//        val testServer1 =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertTrue { testServer1.numLEDs == 240 }
//
//        val testServer2 =
//            AnimatedLEDStripServer(arrayOf("-qn", "50"), EmulatedWS281x::class)
//        assertTrue { testServer2.numLEDs == 50 }
//
//        val testServer3 =
//            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { testServer3.numLEDs == 120 }
//
//        val testServer4 =
//            AnimatedLEDStripServer(
//                arrayOf("-qn", "100", "-f", "src/test/resources/led.config"),
//                EmulatedWS281x::class,
//            )
//        assertTrue { testServer4.numLEDs == 100 }
//
//        val testServer5 =
//            AnimatedLEDStripServer(arrayOf("-qn", "x"), EmulatedWS281x::class)
//        assertTrue { testServer5.numLEDs == 240 }
//    }
//
//
//    @Test
//    fun testPin() {
//        val testServer1 =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertTrue { testServer1.pin == 12 }
//
//        val testServer2 =
//            AnimatedLEDStripServer(arrayOf("-qp", "20"), EmulatedWS281x::class)
//        assertTrue { testServer2.pin == 20 }
//
//        val testServer3 =
//            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { testServer3.pin == 15 }
//
//        val testServer4 =
//            AnimatedLEDStripServer(
//                arrayOf("-qp", "10", "-f", "src/test/resources/led.config"),
//                EmulatedWS281x::class,
//            )
//        assertTrue { testServer4.pin == 10 }
//
//        val testServer5 =
//            AnimatedLEDStripServer(arrayOf("-qp", "x"), EmulatedWS281x::class)
//        assertTrue { testServer5.pin == 12 }
//    }
//
//    @Test
//    fun testPorts() {
//        val testServer1 =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertTrue { testServer1.ports.isEmpty() }
//
//        val testServer2 =
//            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/ports.config"), EmulatedWS281x::class)
//        assertTrue { testServer2.ports.contains(3005) }
//        assertTrue { testServer2.ports.contains(3006) }
//        assertTrue { testServer2.ports.contains(3007) }
//
//        assertFailsWith<IllegalArgumentException> {
//            AnimatedLEDStripServer(
//                arrayOf("-qf", "src/test/resources/ports.badconfig"),
//                EmulatedWS281x::class,
//            )
//        }
//    }
//
//    @Test
//    fun testRendersBeforeSave() {
//        val testServer1 =
//            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
//        assertTrue { testServer1.rendersBeforeSave == 1000 }
//
//        val testServer2 =
//            AnimatedLEDStripServer(arrayOf("-qr", "500"), EmulatedWS281x::class)
//        assertTrue { testServer2.rendersBeforeSave == 500 }
//
//        val testServer3 =
//            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedWS281x::class)
//        assertTrue { testServer3.rendersBeforeSave == 2000 }
//
//        val testServer4 =
//            AnimatedLEDStripServer(
//                arrayOf("-qr", "750", "-f", "src/test/resources/led.config"),
//                EmulatedWS281x::class,
//            )
//        assertTrue { testServer4.rendersBeforeSave == 750 }
//
//        val testServer5 =
//            AnimatedLEDStripServer(arrayOf("-qr", "x"), EmulatedWS281x::class)
//        assertTrue { testServer5.rendersBeforeSave == 1000 }
//    }

    @Test
    fun testPersistAnimations() {
        val testServer1 =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedWS281x::class)
        assertFalse { testServer1.persistAnimations }

        val testServer2 =
            AnimatedLEDStripServer(arrayOf("-q", "--persist"), EmulatedWS281x::class)
        assertTrue { testServer2.persistAnimations }

        val testServer3 =
            AnimatedLEDStripServer(arrayOf("-q", "--persist", "--no-persist"), EmulatedWS281x::class)
        assertFalse { testServer3.persistAnimations }

        val testServer4 =
            AnimatedLEDStripServer(arrayOf("-qf", "src/test/resources/led.config"), EmulatedWS281x::class)
        assertTrue { testServer4.persistAnimations }

        val testServer5 =
            AnimatedLEDStripServer(
                arrayOf("-q", "--persist", "-f", "src/test/resources/led.config"),
                EmulatedWS281x::class,
            )
        assertTrue { testServer5.persistAnimations }

        val testServer6 =
            AnimatedLEDStripServer(
                arrayOf("-q", "--persist", "-f", "src/test/resources/led.config", "--no-persist"),
                EmulatedWS281x::class,
            )
        assertFalse { testServer6.persistAnimations }

        val testServer7 =
            AnimatedLEDStripServer(
                arrayOf("-qf", "src/test/resources/no-persist.config"),
                EmulatedWS281x::class,
            )
        assertFalse { testServer7.persistAnimations }

        val testServer8 =
            AnimatedLEDStripServer(
                arrayOf("-q", "--persist", "-f", "src/test/resources/no-persist.config"),
                EmulatedWS281x::class,
            )
        assertTrue { testServer8.persistAnimations }

        val testServer9 =
            AnimatedLEDStripServer(
                arrayOf("-q", "--persist", "f", "src/test/resources/no-persist.config", "--no-persist"),
                EmulatedWS281x::class,
            )
        assertFalse { testServer9.persistAnimations }
    }

    @Test
    @Ignore
    fun testHelp() {
        redirectOutput()

        AnimatedLEDStripServer(arrayOf("-hq"), EmulatedWS281x::class)

        checkOutput(
            expected =
            "usage: ledserver.jar\n" +
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
}
