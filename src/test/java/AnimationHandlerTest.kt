package animatedledstrip.test

import animatedledstrip.leds.EmulatedAnimatedLEDStrip
import org.junit.Test
import server.AnimationHandler
import server.leds
import kotlin.test.assertTrue

class AnimationHandlerTest {


    init {
        leds = EmulatedAnimatedLEDStrip(50)
    }
    @Test
    fun testInit() {
        AnimationHandler
        assertTrue { AnimationHandler.continuousAnimations.isEmpty() }
    }

}