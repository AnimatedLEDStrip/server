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
import animatedledstrip.animationutils.isNonRepetitive
import animatedledstrip.leds.AnimatedLEDStrip
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import org.pmw.tinylog.Logger
import java.io.File
import java.io.FileInputStream
import java.io.InvalidClassException
import java.io.ObjectInputStream
import java.lang.Math.random

/**
 * An object that creates ContinuousRunAnimation instances for animations and
 * keeps track of currently running animations.
 */
internal class AnimationHandler(
    private val leds: AnimatedLEDStrip,
    threadCount: Int = 100,
    internal val persistAnimations: Boolean = false
) {

    @Suppress("EXPERIMENTAL_API_USAGE")
    val animationThreadPool = newFixedThreadPoolContext(threadCount, "AnimationThreads")

    /**
     * Map tracking what continuous animations are currently running
     *
     * key = id of animation,
     * value = ContinuousRunAnimation instance
     */
    val continuousAnimations = mutableMapOf<String, ContinuousRunAnimation>()


    init {
        if (persistAnimations)
            GlobalScope.launch {
                File(".animations/").walk().forEach {
                    if (!it.isDirectory && it.name.endsWith(".anim")) try {
                        ObjectInputStream(FileInputStream(it)).apply {
                            val obj = readObject() as AnimationData
                            addAnimation(obj, obj.id)
                            close()
                        }
                    } catch (e: ClassCastException) {
                        it.delete()
                    } catch (e: InvalidClassException) {
                        it.delete()
                    }
                }
            }
    }

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
     * If animation.continuous is null:
     *      Determine if the animation is continuous based on the presence of a
     *      NonRepetitive annotation
     *
     * @param params An AnimationData instance containing data about the animation to be run
     */
    fun addAnimation(params: AnimationData, animId: String? = null) {

        // Special "Animation" type that the client sends to end an animation
        if (params.animation == Animation.ENDANIMATION)
            endAnimation(params)
        else
            when (params.continuous) {
                true -> continuousRunAnimation(params, animId)
                false -> singleRunAnimation(params)
                // If continuous has not been set, check what type of animation is being run and use that
                null -> {
                    when (params.animation.isNonRepetitive()) {
                        // Animations that are only run once because they change the prolonged color of the strip
                        true -> {
                            singleRunAnimation(params)
                        }
                        // Animations that can be run repeatedly
                        false -> {
                            continuousRunAnimation(params, animId)
                        }
                    }
                }
            }
    }

    /**
     * Run an animation once without repeating
     *
     * @param params An AnimationData instance containing data about the animation to be run
     */
    private fun singleRunAnimation(params: AnimationData) {
        GlobalScope.launch(animationThreadPool) {
            leds.run(params)
        }
    }

    /**
     * Start an animation that will be run continuously until stopped
     *
     * @param params An AnimationData instance containing data about the animation to be run
     * @param animId The ID of the animation (if applicable)
     */
    private fun continuousRunAnimation(params: AnimationData, animId: String? = null) {
        val id = animId ?: (random() * 100000000).toInt().toString()
        params.id = id
        continuousAnimations[id] =
            ContinuousRunAnimation(id, params, leds, this)
        Logger.trace(continuousAnimations)
        continuousAnimations[id]?.runAnimation() ?: Logger.warn("Animation $id cannot be run")
    }

    /**
     * End an animation
     */
    fun endAnimation(params: AnimationData?) {
        continuousAnimations[params?.id ?: "NONE"]?.endAnimation()       // End animation
            ?: run { Logger.warn { "Animation ${params?.id} not running" }; continuousAnimations.remove(params?.id); return }
    }

    /**
     * End an animation
     */
    fun endAnimation(animation: ContinuousRunAnimation?) {
        if (animation == null) return
        else endAnimation(animation.params)
    }
}

