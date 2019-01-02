package server

import animatedledstrip.leds.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
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
        GlobalScope.launch(newSingleThreadContext("Thread ${random()}")) {
            val (animation, _, _, _, _, _, _, _, _, _,
                    continuous, ID) = params

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
                        val id = random().toString()
                        continuousAnimations[id] =
                                ContinuousRunAnimation(id, params)
                        continuousAnimations[id]?.startAnimation()
                        println(continuousAnimations)
                        println("${Thread.currentThread().name} complete")
                    } else {
                        SingleRunAnimation(params)
                        println("${Thread.currentThread().name} complete")
                    }
                }
                /*  Special "Animation" type that the GUI sends to end an animation */
                Animations.ENDANIMATION -> {
                    continuousAnimations[ID]?.endAnimation()        // End animation
                    continuousAnimations.remove(ID)                 // Remove it from the continuousAnimations map
                }
                else -> println("Animation $animation not supported by server")
            }
        }
    }
}

