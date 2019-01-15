package server

import animatedledstrip.leds.*
import animatedledstrip.ccpresets.*
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
        when (params.animation) {
            Animation.ALTERNATE ->
                leds.alternate(params)
            Animation.MULTIPIXELRUN ->
                leds.multiPixelRun(params)
            Animation.PIXELMARATHON ->
                leds.pixelMarathon(params)
            Animation.PIXELRUN ->
                leds.pixelRun(params)
            Animation.PIXELRUNWITHTRAIL ->
                leds.pixelRunWithTrail(params)
            Animation.SMOOTHCHASE ->
                leds.smoothChase(params)
            Animation.SPARKLE ->
                leds.sparkle(params)
            Animation.STACKOVERFLOW ->
                leds.stackOverflow(params)
        }
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
    fun sendAnimation() {
        Logger.trace("Sending animation to GUI")
        GUISocket.sendAnimation(params.toMap(), id)
    }

}
