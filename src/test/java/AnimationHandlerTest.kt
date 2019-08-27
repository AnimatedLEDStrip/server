package animatedledstrip.test

import animatedledstrip.animationutils.*
import animatedledstrip.colors.ccpresets.CCBlue
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimationHandler
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.delayBlocking
import org.junit.Test
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import kotlin.test.assertFails
import kotlin.test.assertTrue

class AnimationHandlerTest {


    init {
        Configurator.defaultConfig().level(Level.OFF).activate()
        SocketConnections.connections.clear()
    }

    private val leds = EmulatedAnimatedLEDStrip(50)

    @Test
    fun testHandlerInit() {
        val handler = AnimationHandler(leds)
        assertTrue { handler.continuousAnimations.isEmpty() }
    }

    @Test
    fun testNonRepetitiveAnimation() {
        val handler = AnimationHandler(leds)

        handler.addAnimation(AnimationData().animation(Animation.COLOR).color(CCBlue))

        delayBlocking(1000)

        checkAllPixels(leds, 0xFF)
    }

    @Test
    fun testNonContinuousAnimation() {
        val handler = AnimationHandler(leds)
        handler.addAnimation(AnimationData().animation(Animation.MULTIPIXELRUN).continuous(false))
        delayBlocking(1000)
    }

    @Test
    fun testContinuousAnimation() {
        val handler = AnimationHandler(leds)
        handler.addAnimation(
            AnimationData()
                .animation(Animation.PIXELRUN)
                .color(0xFF)
                .direction(Direction.FORWARD)
        )

        delayBlocking(2000)

        for (key in handler.continuousAnimations.keys) {
            handler.addAnimation(AnimationData().animation(Animation.ENDANIMATION).id(key))
        }

        delayBlocking(1000)
        checkAllPixels(leds, 0x0)
    }

    @Test
    fun testRemoveNonExistentAnimation() {
        val handler = AnimationHandler(leds)
        assertFails {
            handler.addAnimation(AnimationData().animation(Animation.ENDANIMATION).id("TEST"))
        }
    }

}