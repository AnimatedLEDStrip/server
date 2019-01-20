package server

import animatedledstrip.leds.*
import org.pmw.tinylog.Logger
import java.io.BufferedInputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException


/**
 * An object that handles communication with the GUI via port 5
 */
object GUISocket {
    private val serverSocket = ServerSocket(5)
    var clientSocket: Socket? = null
    private var disconnected = true
    private var socOut: ObjectOutputStream? = null

    fun isDisconnected() = disconnected


    /**
     * Accept communication and start loop that listens for input
     *
     * If the server is not shutting down (quit == false):
     *      Accept a new connection,
     *      While connected, get input and pass on to the animationQueue.
     *
     * (If there is a disconnection and the server is not shutting down, wait for a new connection)
     */
    fun openSocket() {
        while (!quit) {
            clientSocket = serverSocket.accept()
            Logger.trace("Accepted new connection on port 5")
            Logger.trace("Initializing input stream")
            val socIn = ObjectInputStream(BufferedInputStream(clientSocket!!.getInputStream()))
            Logger.trace("Initializing output stream")
            socOut = ObjectOutputStream(clientSocket!!.getOutputStream())
            Logger.trace("Sending currently running animations to GUI")
            AnimationHandler.continuousAnimations.forEach {
                it.value.sendAnimation()    // Send all current running continuous animations to newly connected GUI
            }
            disconnected = false
            Logger.info("GUI Connection Established")
            var input: Map<*, *>
            try {
                while (!disconnected) {
                    Logger.trace("Waiting for input")
                    input = socIn.readObject() as Map<*, *>
                    Logger.trace("Input received")
                    AnimationHandler.addAnimation(AnimationData(input))
                }
            } catch (e: SocketException) {  // Catch disconnections
                Logger.warn("GUI Connection Lost: $e")
                disconnected = true
            }
        }
    }

    /**
     * Send animation data to the GUI along with an ID
     *
     * @param animation A Map<String, Any?> containing data about the animation
     * @param id The ID for the animation
     */
    fun sendAnimation(animation: Map<*, *>, id: String) {
        if (!isDisconnected()) {
            Logger.trace("Animation to send: $animation")
            socOut!!.writeObject(mapOf("Animation" to animation, "ID" to id))
            Logger.trace("Sent animation to GUI:\n$animation : $id")
        }
    }
}
