package animatedledstrip.server

import animatedledstrip.animationutils.AnimationData
import animatedledstrip.leds.AnimatedLEDStrip
import org.pmw.tinylog.Logger

/**
 * Class for running an animation that repeats until stopped.
 *
 * @param id A string to identify the animation, usually a random number
 * @param params An AnimationData instance containing data about the animation
 * to be run
 */
class ContinuousRunAnimation(private val id: String, private val params: AnimationData, private val leds: AnimatedLEDStrip) {

    /**
     * Variable controlling while loops in animation functions.
     */
    private var continueAnimation = true


    init {
        sendAnimation()                 // Send animation to GUI
    }


    /**
     * Determine which animation is being called and call the corresponding function.
     */
    fun startAnimation() {
        Logger.trace("params: $params")
        while (continueAnimation) leds.run(params)
    }


    /**
     * Stop animation by setting the loop guard to false.
     */
    fun endAnimation() {
        Logger.debug("Animation $id ending")
        continueAnimation = false
    }


    /**
     *  Send animation data to GUI.
     */
    fun sendAnimation(connection: SocketConnections.Connection? = null) {
        Logger.trace("Sending animation to GUI")
        SocketConnections.sendAnimation(params, id, connection)
    }

}
