package animatedledstrip.server

import animatedledstrip.leds.AnimationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.pmw.tinylog.Logger
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException


object SocketConnections {

    val connections = mutableMapOf<Int, Connection>()

    fun add(port: Int): Connection {
        val connection = Connection(port)
        connections[port] = connection
        return connection
    }

    class Connection(val port: Int) {
        private val serverSocket = ServerSocket(
            port,
            0,
            if (!isTest) null else InetAddress.getByName("0.0.0.0")
        )
        var clientSocket: Socket? = null
        private var disconnected = true
        private var socOut: ObjectOutputStream? = null
        var textBased = false

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
        suspend fun openSocket() {
            withContext(Dispatchers.IO) {
                Logger.debug("Socket at port $port started")
                while (!quit) {
                    try {
                        clientSocket = serverSocket.accept()
                        Logger.trace("Accepted new connection on port $port")
                        Logger.trace("Initializing input stream")
                        val socIn = ObjectInputStream(BufferedInputStream(clientSocket!!.getInputStream()))
                        Logger.trace("Initializing output stream")
                        socOut = ObjectOutputStream(clientSocket!!.getOutputStream())
                        Logger.debug("Sending currently running animations to GUI")
                        AnimationHandler.continuousAnimations.forEach {
                            it.value.sendAnimation(this@Connection)    // Send all current running continuous animations to newly connected GUI
                        }
                        disconnected = false
                        Logger.info("Connection on port $port Established")
                        var input: Any?
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
                            Logger.debug("Sent animation to GUI:\n$animation : $id")
                        }
                    }
                }
            }
        }


        override fun toString(): String {
            return "Connection@${serverSocket.inetAddress.toString().removePrefix("/")}:$port"
        }
    }

    fun sendAnimation(animation: Map<*, *>, id: String, connection: Connection? = null) {
        if (connection != null) connection.sendAnimation(animation, id)
        else connections.forEach {
            it.value.sendAnimation(animation, id)
            Logger.trace("Sent animation to connection on port ${it.key}")
        }
    }

}