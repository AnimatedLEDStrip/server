package server

import animatedledstrip.leds.*

/**
 * Class for running an animation that runs once before stopping
 *
 * @param params A Map<String, Any?> containing data about the animation to be run
 */
class SingleRunAnimation(private val params: AnimationData) {

    init {
        leds.run(params)
    }

}