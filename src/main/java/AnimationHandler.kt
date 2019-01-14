package server

import animatedledstrip.leds.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.pmw.tinylog.Logger
import java.lang.Math.random


/**
 * An object that creates SingleRunAnimation and ContinuousRunAnimation
 * instances for animations and keeps track of currently running animations
 */
object AnimationHandler {

    /**
     * Map tracking what continuous animations are currently running
     *
     * key = id of animation,
     * value = ContinuousRunAnimation instance
     */
    val continuousAnimations = mutableMapOf<String, ContinuousRunAnimation>()


    /**
     * Adds a new animation.
     *
     * If params&#91;"Continuous"&#93; is false or null:
     *      Creates a SingleRunAnimation instance in a new thread.
     *
     * If params&#91;"Continuous"&#93; is true:
     *      Creates a ContinuousRunAnimation instance in a new thread,
     *      Adds pair with the animation ID and ContinuousRunAnimation instance
     *      to continuousAnimations.
     *
     * @param params A Map<String, Any?> containing data about the animation to be run
     */
    fun addAnimation(params: Map<*, *>) {
        Logger.trace("Launching new thread for new animation")
        GlobalScope.launch(newSingleThreadContext("Thread ${random()}")) {
            Logger.trace("Decomposing params map")
            val (animation, _, _, _, _, _, _, _, _, _,
                    continuous, ID) = params

            Logger.debug(params)
            when (animation) {
                /*  Animations that are only run once because they change the color of the strip */
                Animation.COLOR,
                Animation.MULTICOLOR,
                Animation.MULTIPIXELRUNTOCOLOR,
                Animation.SPARKLETOCOLOR,
                Animation.STACK,
                Animation.WIPE -> {
                    Logger.trace("Calling Single Run Animation")
                    SingleRunAnimation(params)
                    Logger.debug("${Thread.currentThread().name} complete")
                }
                /*  Animations that can be run repeatedly */
                Animation.ALTERNATE,
                Animation.MULTIPIXELRUN,
                Animation.PIXELRUN,
                Animation.PIXELRUNWITHTRAIL,
                Animation.PIXELMARATHON,
                Animation.SMOOTHCHASE,
                Animation.SPARKLE,
                Animation.STACKOVERFLOW -> {
                    if (continuous == true) {
                        Logger.trace("Calling Continuous Animation")
                        val id = random().toString()
                        continuousAnimations[id] =
                                ContinuousRunAnimation(id, params)
                        continuousAnimations[id]?.startAnimation()
                        Logger.debug(continuousAnimations)
                        Logger.debug("${Thread.currentThread().name} complete")
                    } else {
                        Logger.trace("Calling Single Run Animation")
                        SingleRunAnimation(params)
                        Logger.debug("${Thread.currentThread().name} complete")
                    }
                }
                /*  Special "Animation" type that the GUI sends to end an animation */
                Animation.ENDANIMATION -> {
                    Logger.debug("Ending an animation")
                    continuousAnimations[ID]?.endAnimation()        // End animation
                    continuousAnimations.remove(ID)                 // Remove it from the continuousAnimations map
                }
                Animation.COLOR1,
                Animation.COLOR2,
                Animation.COLOR3,
                Animation.COLOR4 -> Logger.warn("COLOR1, COLOR2, COLOR3 and COLOR4 are deprecated")
                else -> Logger.warn("Animation $animation not supported by server")
            }
        }
    }
}

