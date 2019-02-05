package animatedledstrip.test

import animatedledstrip.leds.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimationHandler
import animatedledstrip.server.leds
import org.junit.Test
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