package server

import animatedledstrip.leds.*
import animatedledstrip.ccpresets.*
import org.pmw.tinylog.Logger

/**
 * Class for running an animation that runs once before stopping
 *
 * @param params A Map<String, Any?> containing data about the animation to be run
 */
class SingleRunAnimation(private val params: AnimationData) {

    init {

        when (params.animation) {
            Animation.ALTERNATE ->
                leds.alternate(params)
            Animation.MULTIPIXELRUN ->
                leds.multiPixelRun(params)
            Animation.MULTIPIXELRUNTOCOLOR ->
                leds.multiPixelRunToColor(params)
            Animation.PIXELMARATHON ->
                leds.pixelMarathon(params)
            Animation.PIXELRUN ->
                leds.pixelRun(params)
            Animation.PIXELRUNWITHTRAIL ->
                leds.pixelRunWithTrail(params)
            Animation.SMOOTHCHASE -> TODO()
            Animation.SPARKLE ->
                leds.sparkle(params)
            Animation.SPARKLETOCOLOR ->
                leds.sparkleToColor(params)
            Animation.STACK ->
                leds.stack(params)
            Animation.STACKOVERFLOW ->
                leds.stackOverflow(params)
            Animation.WIPE ->
                leds.wipe(params)
        }
    }

}