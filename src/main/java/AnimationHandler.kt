package server

import animatedledstrip.leds.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.pmw.tinylog.Logger
import java.lang.Math.random


/**
 * An object that creates ContinuousRunAnimation instances for animations and
 * keeps track of currently running animations.
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
     * If animation.continuous is false:
     *      Runs the animation in a new thread once.
     *
     * If animation.continuous is true:
     *      Creates a ContinuousRunAnimation instance in a new thread,
     *      Adds pair with the animation ID and ContinuousRunAnimation instance
     *      to continuousAnimations.
     *
     * @param params An AnimationData instance containing data about the animation to be run
     */
    fun addAnimation(params: AnimationData) {

        /*  Special "Animation" type that the GUI sends to end an animation */
        if (params.animation == Animation.ENDANIMATION) {
            Logger.debug("Ending an animation")
            continuousAnimations[params.id]?.endAnimation()        // End animation
            continuousAnimations.remove(params.id)                 // Remove it from the continuousAnimations map
            return
        }

        Logger.trace("Launching new thread for new animation")
        GlobalScope.launch(newSingleThreadContext("Thread ${random()}")) {
            Logger.trace("Decomposing params map")
            Logger.debug(params)

            when (params.animation::class.java.fields[params.animation.ordinal].annotations.find { it is NonRepetitive } is NonRepetitive) {
                /*  Animations that are only run once because they change the color of the strip */
                true -> {
                    Logger.trace("Calling Single Run Animation")
                    leds.run(params)
                    Logger.debug("${Thread.currentThread().name} complete")
                }
                /*  Animations that can be run repeatedly */
                false -> {
                    if (params.continuous) {
                        Logger.trace("Calling Continuous Animation")
                        val id = random().toString().removePrefix("0.")
                        continuousAnimations[id] =
                                ContinuousRunAnimation(id, params)
                        continuousAnimations[id]?.startAnimation()
                        Logger.debug(continuousAnimations)
                        Logger.debug("${Thread.currentThread().name} complete")
                    } else {
                        Logger.trace("Calling Single Run Animation")
                        leds.run(params)
                        Logger.debug("${Thread.currentThread().name} complete")
                    }
                }
            }
        }
    }
}

