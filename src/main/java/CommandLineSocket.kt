package server

import org.pmw.tinylog.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

/**
 * An object that handles communication with a remote terminal via port 6
 */
object CommandLineSocket {
    private val serverSocket = ServerSocket(6)
    private var disconnected = true
    private var clientSocket: Socket? = null

    fun isDisconnected() = !disconnected


    /**
     * Accept communication and start loop that listens for input
     * If the server is not shutting down (quit == false)
     *      Accept a new connection
     *      While connected, get input and pass on to the animationQueue
     * (If there is a disconnection and the server is not shutting down, wait for a new connection)
     */
    fun openSocket() {
        while (!quit) {
            clientSocket = serverSocket.accept()
            Logger.trace("Accepted new connection on port 6")
            Logger.trace("Initializing input stream")
            val socIn = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
            disconnected = false
            Logger.info("Command Line Connection Established")
            var input: String?
            while (!disconnected) {
                Logger.trace("Waiting for input")
                input = socIn.readLine()
                Logger.trace("Input received")
                /*  If checkForNullAndAdd returns true, then the remote client has disconnected or is about to */
                if (animationQueue.checkForNullAndAdd(input)) disconnected = true
            }
            Logger.warn("Command Line Connection Lost")
        }
    }
}
