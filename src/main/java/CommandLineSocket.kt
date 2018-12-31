package server

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
    fun getClientIP() = clientSocket!!.remoteSocketAddress


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
            val socIn = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
            disconnected = false
            println("Command Line Connection Established")
            var input: String?
            while (!disconnected) {
                input = socIn.readLine()
                /*  If checkForNullAndAdd returns true, then the remote client has disconnected or is about to */
                if (animationQueue.checkForNullAndAdd(input)) disconnected = true
            }
            println("Command Line Connection Lost")
        }
    }
}
