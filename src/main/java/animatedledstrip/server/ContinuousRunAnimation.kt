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
import animatedledstrip.leds.AnimatedLEDStrip
import kotlinx.coroutines.*
import org.tinylog.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Class for running an animation that repeats until stopped.
 *
 * @param id A string to identify the animation, usually a random number
 * @param params An AnimationData instance containing data about the animation
 * to be run
 */
internal class ContinuousRunAnimation(
    private val id: String,
    val params: AnimationData,
    private val leds: AnimatedLEDStrip,
    private val handler: AnimationHandler
) {

    /**
     * Variable controlling if the animation will repeat
     */
    private var continueAnimation = true

    private var job: Job? = null

    private val fileName = "$id.anim"


    init {
        sendStartAnimation()                 // Send animation to GUI
    }


    /**
     * Run the animation in a new thread
     */
    fun runAnimation() {
        job = GlobalScope.launch(handler.animationThreadPool) {
            launch {
                withContext(Dispatchers.IO) {
                    ObjectOutputStream(FileOutputStream(".animations/$fileName")).apply {
                        writeObject(params)
                        close()
                    }
                }
            }
            Logger.trace { "params: $params" }
            while (continueAnimation) leds.run(params)
            sendEndAnimation()
            handler.continuousAnimations.remove(id)
        }
    }


    /**
     * Stop animation by setting the loop guard to false
     */
    fun endAnimation() {
        Logger.debug { "Animation $id ending" }
        if (continueAnimation) continueAnimation = false
        else job?.cancel()
        if (File(".animations/$id").exists())
            Files.delete(Paths.get(".animations/$fileName"))
    }


    /**
     *  Send message to client(s) that animation has started
     */
    fun sendStartAnimation(connection: SocketConnections.Connection? = null) {
        Logger.trace { "Sending animation start to client(s)" }
        SocketConnections.sendAnimation(params, id, connection)
    }

    /**
     * Send message to client(s) that animation has ended
     *
     * @param connection
     */
    fun sendEndAnimation(connection: SocketConnections.Connection? = null) {
        Logger.trace { "Sending animation end to client(s)" }
        SocketConnections.sendAnimation(params.copy(animation = Animation.ENDANIMATION), id, connection)
    }

}
