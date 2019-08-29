package animatedledstrip.server

/*
 *  Copyright (c) 2019 AnimatedLEDStrip
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */


import animatedledstrip.animationutils.Animation
import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.NonRepetitive
import animatedledstrip.leds.AnimatedLEDStrip
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import org.pmw.tinylog.Logger
import java.lang.Math.random


/**
 * An object that creates ContinuousRunAnimation instances for animations and
 * keeps track of currently running animations.
 */
internal class AnimationHandler(private val leds: AnimatedLEDStrip) {

    @Suppress("EXPERIMENTAL_API_USAGE")
    val animationThreadPool = newFixedThreadPoolContext(100, "AnimationThreads")

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
            continuousAnimations[params.id]?.endAnimation()       // End animation
                ?: throw Exception("Animation ${params.id} not running")
            continuousAnimations.remove(params.id)                 // Remove it from the continuousAnimations map
            return
        }

        Logger.trace("Launching new thread for new animation")
        GlobalScope.launch(animationThreadPool) {
            Logger.debug(params)

            when (params.animation::class.java.fields[params.animation.ordinal].annotations.find { it is NonRepetitive } is NonRepetitive) {
                /*  Animations that are only run once because they change the color of the strip */
                true -> {
                    Logger.trace("Calling Single Run Animation")
                    leds.run(params)
                    Logger.trace("Single Run Animation on ${Thread.currentThread().name} complete")
                }
                /*  Animations that can be run repeatedly */
                false -> {
                    if (params.continuous) {
                        Logger.trace("Calling Continuous Animation")
                        val id = random().toString().removePrefix("0.")
                        continuousAnimations[id] =
                            ContinuousRunAnimation(id, params, leds)
                        Logger.trace(continuousAnimations)
                        continuousAnimations[id]!!.startAnimation()
                        Logger.debug("$id complete")
                    } else {
                        Logger.trace("Calling Single Run Animation")
                        leds.run(params)
                        Logger.trace("Single Run Animation on ${Thread.currentThread().name} complete")
                    }
                }
            }
        }
    }
}

