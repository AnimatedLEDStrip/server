package animatedledstrip.test

import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.addColor
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.delayBlocking
import org.junit.Test
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
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

        testServer.animationHandler.addAnimation(AnimationData().addColor(0xFF))
        delayBlocking(500)
        checkAllPixels(testServer.leds as EmulatedAnimatedLEDStrip, 0xFF)

        testServer.parseTextCommand("clear")
        delayBlocking(500)
        checkAllPixels(testServer.leds, 0x0)
    }

    @Test
    fun testEnd() {
        val stderr: PrintStream = System.err
        val tempOut = ByteArrayOutputStream()
        System.setErr(PrintStream(tempOut))

        val testServer =
            AnimatedLEDStripServer(arrayOf("-f", "src/test/resources/empty.config"), EmulatedAnimatedLEDStrip::class)

        testServer.animationHandler.addAnimation(AnimationData(continuous = true), "1234")
        testServer.animationHandler.addAnimation(AnimationData(continuous = true), "1357")
        testServer.animationHandler.addAnimation(AnimationData(continuous = true), "2431")
        testServer.animationHandler.addAnimation(AnimationData(continuous = true), "7654")
        testServer.animationHandler.addAnimation(AnimationData(continuous = true), "2653")
        testServer.animationHandler.addAnimation(AnimationData(continuous = true), "2521")

        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("1234") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("1357") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("2431") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("7654") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("2653") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("2521") }

        testServer.parseTextCommand("end 1234")
        delayBlocking(200)
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("1234") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("1357") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("2431") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("7654") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("2653") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("2521") }

        testServer.parseTextCommand("end 1357 2431")
        delayBlocking(200)
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("1234") }
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("1357") }
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("2431") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("7654") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("2653") }
        assertTrue { testServer.animationHandler.continuousAnimations.containsKey("2521") }

        testServer.parseTextCommand("end all")
        delayBlocking(200)
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("1234") }
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("1357") }
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("2431") }
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("7654") }
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("2653") }
        assertFalse { testServer.animationHandler.continuousAnimations.containsKey("2521") }

        tempOut.reset()

        testServer.parseTextCommand("end")
        assertTrue {
            tempOut
                .toString("utf-8")
                .replace("\r\n", "\n") ==
                    "WARNING: Animation ID must be specified\n"
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

        testServer.animationHandler.addAnimation(AnimationData(continuous = true), "5678")
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
                    "INFO:    5678: AnimationData(animation=COLOR, colors=[0], center=120, continuous=true, delay=50, delayMod=1.0, direction=FORWARD, distance=240, endPixel=239, id=5678, spacing=3, startPixel=0)\n"
        }
        tempOut.reset()
        testServer.parseTextCommand("end 5678")

        System.setOut(stdout)
    }

}