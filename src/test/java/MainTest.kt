package animatedledstrip.test

import animatedledstrip.leds.EmulatedAnimatedLEDStrip
import animatedledstrip.server.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.junit.Test

class MainTest {

    private fun setSocketPorts() {
        socketPort1 = 1105
        socketPort2 = 1106
    }

    @Test
    fun testMainStart() {

        setSocketPorts()

        @Suppress("EXPERIMENTAL_API_USAGE")
        GlobalScope.launch(newSingleThreadContext("Test")) {
            delay(2000)
            checkAllPixels(leds as EmulatedAnimatedLEDStrip, 0)
            quit = true
        }

        main(arrayOf("-e"))
    }

}