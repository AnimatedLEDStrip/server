package animatedledstrip.test

import animatedledstrip.animationutils.Animation
import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.Direction
import animatedledstrip.colors.ccpresets.CCBlue
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.AnimationHandler
import animatedledstrip.server.SocketConnections
import animatedledstrip.server.leds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import kotlin.test.assertFails
import kotlin.test.assertTrue

class AnimationHandlerTest {


    init {
        Configurator.defaultConfig().level(Level.OFF).activate()
        SocketConnections.connections.clear()
        leds = EmulatedAnimatedLEDStrip(50)
    }

    @Test
    fun testHandlerInit() {
        AnimationHandler
        assertTrue { AnimationHandler.continuousAnimations.isEmpty() }
    }

    @Test
    fun testNonRepetitiveAnimation() {
        AnimationHandler.addAnimation(AnimationData().animation(Animation.COLOR).color(CCBlue))

        runBlocking { delay(1000) }

        checkAllPixels(leds as EmulatedAnimatedLEDStrip, 0xFF)
    }

    @Test
    fun testNonContinuousAnimation() {
        AnimationHandler.addAnimation(AnimationData().animation(Animation.MULTIPIXELRUN).continuous(false))

        runBlocking { delay(1000) }
    }

    @Test
    fun testContinuousAnimation() {
        AnimationHandler.addAnimation(
            AnimationData()
                .animation(Animation.PIXELRUN)
                .color(0xFF)
                .direction(Direction.FORWARD)
        )

        runBlocking { delay(2000) }

        for (key in AnimationHandler.continuousAnimations.keys) {
            AnimationHandler.addAnimation(AnimationData().animation(Animation.ENDANIMATION).id(key))
        }

        runBlocking { delay(1000) }
        checkAllPixels(leds as EmulatedAnimatedLEDStrip, 0x0)
    }

    @Test
    fun testRemoveNonExistentAnimation() {
        assertFails {
            AnimationHandler.addAnimation(AnimationData().animation(Animation.ENDANIMATION).id("TEST"))
        }
    }

}