package animatedledstrip.test

import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.server.startServer
import animatedledstrip.utils.delayBlocking
import kotlinx.coroutines.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertTrue


class CommandLineTest {

    @Test
    fun testCommandLine() = runBlocking {
        withTimeout(60000) {
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
    }

    @Test
    fun testQuit() = runBlocking {
        withTimeout(60000) {
            GlobalScope.launch {
                delay(5000)

                val stream = ByteArrayInputStream("q".toByteArray())
                System.setIn(stream)
                startServer(arrayOf("-qCL", "3102"), EmulatedAnimatedLEDStrip::class)
            }
            startServer(arrayOf("-qL", "3102"), EmulatedAnimatedLEDStrip::class)
            Unit
        }
    }

    @Test
    fun testNoCommand() = runBlocking {
        withTimeout(60000) {
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
    }

    @Test
    fun testExit() = runBlocking {
        withTimeout(60000) {
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