package server

import animatedledstrip.leds.*
import org.pmw.tinylog.Logger

/**
 * Class for running an animation that repeats until stopped
 *
 * @param id A string to identify the animation, such as the thread it is running in
 * @param params A Map<String, Any?> containing data about the animation to be run
 */
class ContinuousRunAnimation(private val id: String, private val params: AnimationData) {

    /**
     * Variable controlling while loops in animation functions
     */
    private var continueAnimation = true


    init {
        sendAnimation()                 // Send animation to GUI
    }


    /**
     * Determine which animation is being called and call the corresponding function
     */
    fun startAnimation() {
        Logger.trace("params: $params")
        while (continueAnimation) leds.run(params)
    }


    /**
     * Stop animation
     */
    fun endAnimation() {
        Logger.debug("Animation $id ending")
        continueAnimation = false
    }


    /**
     *  Send animation data to GUI
     */
    fun sendAnimation(connection: SocketConnections.Connection? = null) {
        Logger.trace("Sending animation to GUI")
        SocketConnections.sendAnimation(params.toMap(), id, connection)
    }

}
