package animatedledstrip.server

import animatedledstrip.leds.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.pmw.tinylog.Logger
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException


object SocketConnections {

    private val connections = mutableListOf<Connection>()

    fun add(port: Int): Connection {
        val connection = Connection(port)
        connections.add(connection)
        return connection
    }

    class Connection(val port: Int) {
        private val serverSocket = ServerSocket(port)
        var clientSocket: Socket? = null
        private var disconnected = true
        private var socOut: ObjectOutputStream? = null
        private var textBased = false

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
                Logger.trace("Accepted new connection on port $port")
                Logger.trace("Initializing input stream")
                val socIn = ObjectInputStream(BufferedInputStream(clientSocket!!.getInputStream()))
                Logger.trace("Initializing output stream")
                socOut = ObjectOutputStream(clientSocket!!.getOutputStream())
                Logger.trace("Sending currently running animations to GUI")
                AnimationHandler.continuousAnimations.forEach {
                    it.value.sendAnimation(this)    // Send all current running continuous animations to newly connected GUI
                }
                disconnected = false
                Logger.info("Connection on port $port Established")
                var input: Any?
                try {
                    while (!disconnected) {
                        Logger.trace("Waiting for input")
                        input = socIn.readObject()
                        Logger.trace("Input received")
                        when (input) {
                            is Map<*, *> -> {
                                if (input["ClientData"] as Boolean? == true) {
                                    textBased = input["TextBased"] as Boolean? ?: textBased
                                    Logger.debug("Client info: $input")

//                                } else if (input["AnimationDefinition"] as Boolean? == true) {
//
//                                    if (input["AnimationCode"] as String? != null &&
//                                        input["CustomAnimationID"] as String? != null
//                                    ) {
//                                        leds.addCustomAnimation(
//                                            input["AnimationCode"] as String,
//                                            input["CustomAnimationID"] as String
//                                        )
//                                    }

                                } else AnimationHandler.addAnimation(AnimationData(input))
                            }
                        }
                    }
                } catch (e: SocketException) {
                    // Catch disconnections
                    Logger.warn("Connection on port $port Lost: $e")
                    disconnected = true
                } catch (e: EOFException) {
                    Logger.warn("Connection on port $port Lost: $e")
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
                runBlocking {
                    withTimeout(5000) {
                        withContext(Dispatchers.IO) {
                            socOut!!.writeObject(mapOf("Animation" to animation, "ID" to id))
                            Logger.trace("Sent animation to GUI:\n$animation : $id")
                        }
                    }
                }
            }
        }
    }

    fun sendAnimation(animation: Map<*, *>, id: String, connection: Connection? = null) {
        if (connection != null) connection.sendAnimation(animation, id)
        else connections.forEach {
            it.sendAnimation(animation, id)
            Logger.trace("Sent animation to connection on port ${it.port}")
        }
    }

}