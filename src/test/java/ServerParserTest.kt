package animatedledstrip.test

import animatedledstrip.animationutils.AnimationData
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.delayBlocking
import org.junit.Test
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertTrue

class ServerParserTest {

    init {
        SocketConnections.hostIP = "0.0.0.0"
    }

    @Test
    fun testSetLoggingLevel() {
        val testServer =
            AnimatedLEDStripServer(arrayOf("-q"), EmulatedAnimatedLEDStrip::class)
        assertTrue { Logger.getLevel() == Level.OFF }
        testServer.parseTextCommand("trace")
        assertTrue { Logger.getLevel() == Level.TRACE }
        testServer.parseTextCommand("debug")
        assertTrue { Logger.getLevel() == Level.DEBUG }
        testServer.parseTextCommand("info")
        assertTrue { Logger.getLevel() == Level.INFO }
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

        System.setOut(stdout)
    }

}