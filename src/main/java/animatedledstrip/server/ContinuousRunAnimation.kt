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
