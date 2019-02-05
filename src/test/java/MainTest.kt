package animatedledstrip.test

import animatedledstrip.leds.EmulatedAnimatedLEDStrip
import animatedledstrip.server.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.junit.Test

class MainTest {

    private fun setSocketPorts(port1: Int, port2: Int) {
        socketPort1 = port1
        socketPort2 = port2
        isTest = true
    }

    @Test
    fun testMainStart() {
        setSocketPorts(1105, 1106)

        @Suppress("EXPERIMENTAL_API_USAGE")
        GlobalScope.launch(newSingleThreadContext("Test")) {
            delay(2000)
            checkAllPixels(leds as EmulatedAnimatedLEDStrip, 0)
            quit = true
        }

        main(arrayOf("-Eq"))
    }

    @Test
    fun testLocalTerminalThread() {
        setSocketPorts(1107, 1108)

        @Suppress("EXPERIMENTAL_API_USAGE")
        GlobalScope.launch(newSingleThreadContext("Test")) {
            delay(5000)
            quit = true
        }

        main(arrayOf("-Eq"))
    }

}