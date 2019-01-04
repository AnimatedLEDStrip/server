package server

import org.apache.commons.logging.LogFactory
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
    private val log = LogFactory.getLog(this::class.java)
    private val serverSocketOut = ServerSocket(5)
    private val serverSocketIn = ServerSocket(4)
    var clientSocketOut: Socket? = null
    var clientSocketIn: Socket? = null
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
            clientSocketOut = serverSocketOut.accept()
            clientSocketIn = serverSocketIn.accept()
            log.debug("Accepted new connection")
            log.debug("Initializing input stream")
            val socIn = ObjectInputStream(BufferedInputStream(clientSocketIn!!.getInputStream()))
            log.debug("Initializing output stream")
            socOut = ObjectOutputStream(clientSocketOut!!.getOutputStream())
            log.debug("Sending currently running animations to GUI")
            AnimationHandler.continuousAnimations.forEach {
                it.value.sendAnimation()    // Send all current running continuous animations to newly connected GUI
            }
            disconnected = false
            log.info("GUI Connection Established")
            var input: Map<*, *>
            try {
                while (!disconnected) {
                    log.debug("Waiting for input")
                    input = socIn.readObject() as Map<*, *>
                    log.debug("Input received")
                    /*  Check if GUI is sending Quit command */
                    val remoteQuit = input["Quit"] as Boolean? ?: false
                    if (remoteQuit)
                        disconnected = true
                    else    // Else send animation data to the AnimationHandler
                        AnimationHandler.addAnimation(input)
                }
            } catch (e: SocketException) {  // Catch disconnections
                log.warn("GUI Connection Lost: $e")
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
            socOut!!.writeObject(mapOf("Animation" to animation, "ID" to id))
        }
    }
}
