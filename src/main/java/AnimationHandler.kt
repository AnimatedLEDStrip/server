package server

import animatedledstrip.leds.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.apache.commons.logging.LogFactory
import java.lang.Math.random


/**
 * An object that creates SingleRunAnimation and ContinuousRunAnimation
 * instances for animations and keeps track of currently running animations
 */
object AnimationHandler {

    private val log = LogFactory.getLog(this::class.java)

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
        GlobalScope.launch(newSingleThreadContext("Thread ${random()}")) {
            log.debug("Launching new thread for new animation")
            log.debug("Decomposing params map")
            val (animation, _, _, _, _, _, _, _, _, _,
                    continuous, ID) = params

            log.info(params)
            when (animation) {
                /*  Animations that are only run once because they change the color of the strip */
                Animations.COLOR1,
                Animations.COLOR2,
                Animations.COLOR3,
                Animations.COLOR4,
                Animations.MULTIPIXELRUNTOCOLOR,
                Animations.SPARKLETOCOLOR,
                Animations.STACK,
                Animations.WIPE -> {
                    log.info("Single Run Animation called")
                    SingleRunAnimation(params)
                    println("${Thread.currentThread().name} complete")
                }
                /*  Animations that can be run repeatedly */
                Animations.ALTERNATE,
                Animations.MULTIPIXELRUN,
                Animations.PIXELRUN,
                Animations.PIXELRUNWITHTRAIL,
                Animations.PIXELMARATHON,
                Animations.SMOOTHCHASE,
                Animations.SPARKLE,
                Animations.STACKOVERFLOW -> {
                    if (continuous == true) {
                        log.info("Continuous Animation called")
                        val id = random().toString()
                        continuousAnimations[id] =
                                ContinuousRunAnimation(id, params)
                        continuousAnimations[id]?.startAnimation()
                        println(continuousAnimations)
                        println("${Thread.currentThread().name} complete")
                    } else {
                        log.info("Single Run Animation called")
                        SingleRunAnimation(params)
                        println("${Thread.currentThread().name} complete")
                    }
                }
                /*  Special "Animation" type that the GUI sends to end an animation */
                Animations.ENDANIMATION -> {
                    log.info("Ending an animation")
                    continuousAnimations[ID]?.endAnimation()        // End animation
                    continuousAnimations.remove(ID)                 // Remove it from the continuousAnimations map
                }
                else -> log.warn("Animation $animation not supported by server")
            }
        }
    }
}

