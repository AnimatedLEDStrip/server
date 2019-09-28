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
import animatedledstrip.animationutils.id
import kotlinx.coroutines.*
import org.pmw.tinylog.Logger
import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

object SocketConnections {

    var hostIP: String? = null

    /**
     * A Map<Int, Connection> of all opened ports mapped to their respective
     * Connection instances.
     */
    val connections = mutableMapOf<Int, Connection>()

    var localConnection: Connection? = null

    /**
     * Initialize a new connection. Creates a Connection instance with the
     * specified port, then adds the port and Connection instance to
     * [connections] before returning the Connection instance.
     *
     * @param port The port to use when creating the ServerSocket in the
     * connection
     */
    fun add(port: Int, server: AnimatedLEDStripServer<*>): Connection {
        val connection = Connection(port, server)
        if (port == 1118) localConnection = connection
        else connections[port] = connection
        return connection
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private val connectionThreadPool = newFixedThreadPoolContext(250, "Connections")

    class Connection(val port: Int, private val server: AnimatedLEDStripServer<*>) {
        private val serverSocket = ServerSocket(
            port,
            0,
            if (hostIP == null) null else InetAddress.getByName(hostIP)
        )
        var clientSocket: Socket? = null
        var connected = false
            private set
        private var socOut: ObjectOutputStream? = null

        /**
         * Open the connection
         */
        fun open() {
            GlobalScope.launch(connectionThreadPool) {
                Logger.debug("Starting port $port")
                openSocket()
            }
        }

        /**
         * Accept communication and start loop that listens for input
         *
         * If the server is not shutting down (quit == false):
         *      Accept a new connection,
         *      While connected, get input and pass on to the animationQueue.
         *      If there is a disconnection and the server is not shutting down, wait for a new connection
         */
        private suspend fun openSocket() {
            withContext(Dispatchers.IO) {
                Logger.debug("Socket at port $port started")
                while (server.running) {
                    try {
                        clientSocket = serverSocket.accept()
                        val socIn = ObjectInputStream(clientSocket!!.getInputStream())
                        socOut = ObjectOutputStream(clientSocket!!.getOutputStream())
                        Logger.info("Connection on port $port Established")
                        connected = true
                        // Send all current running continuous animations to newly connected client
                        if (port != 1118)
                            server.animationHandler.continuousAnimations.forEach {
                                it.value.sendStartAnimation(this@Connection)
                            }
                        var input: Any?
                        while (connected) {
                            try {
                                input = when (port) {
                                    1118 -> socIn.readObject() as String
                                    else -> socIn.readObject() as AnimationData
                                }
                                when (input) {
                                    is AnimationData -> server.animationHandler.addAnimation(input)
                                    is String -> server.parseTextCommand(input)
                                }
                            } catch (e: ClassCastException) {
                                Logger.error("Could not cast input to ${if (port == 1118) "String" else "AnimationData"}")
                                continue
                            }
                        }
                    } catch (e: SocketException) {
                        // Catch disconnections
                        Logger.warn("Connection on port $port ${if (port == 1118) "(Local) " else ""}Lost: $e")
                        connected = false
                    } catch (e: EOFException) {
                        Logger.warn("Connection on port $port ${if (port == 1118) "(Local) " else ""}Lost: $e")
                        connected = false
                    }
                }
            }
        }

        /**
         * Send animation data to the client along with an ID.
         * Does not work for port 1118 (local connection).
         *
         * @param animation An AnimationData containing data about the animation
         * @param id The ID for the animation
         */
        fun sendAnimation(animation: AnimationData, id: String) {
            check(port != 1118) { "Cannot send animation to local port" }
            if (connected) {
                runBlocking {
                    withTimeout(5000) {
                        withContext(Dispatchers.IO) {
                            socOut?.writeObject(
                                animation
                                    .id(
                                        if ((animation.animation == Animation.CUSTOMANIMATION ||
                                                    animation.animation == Animation.CUSTOMREPETITIVEANIMATION) &&
                                            animation.id.length == 1
                                        )
                                            "${animation.id} $id"
                                        else id
                                    )
                            )
                                ?: Logger.debug("Could not send animation $id: Connection socket null")
                            if (animation.animation == Animation.ENDANIMATION) Logger.debug("Sent end of animation $id")
                            else Logger.debug("Sent animation $id")
                        }
                    }
                }
            }
        }

        /**
         * Send a string to the local port.
         * Only works for a connection with port 1118 (local connection)
         */
        fun sendString(str: String) {
            check(port == 1118) { "Cannot send string to non-local port" }
            if (connected) {
                runBlocking {
                    withTimeout(5000) {
                        withContext(Dispatchers.IO) {
                            socOut?.writeObject(str)
                        }
                    }
                }
            }
        }


        override fun toString(): String {
            return "Connection@${serverSocket.inetAddress.toString().removePrefix("/")}:$port"
        }
    }


    /**
     * Send animation data to one or all client(s).
     *
     * @param animation A Map<*, *> containing info about the animation
     * @param id The ID for the animation
     * @param client Used to specify which client should receive the data. If
     * null, data is sent to all clients
     */
    fun sendAnimation(animation: AnimationData, id: String, client: Connection? = null) {
        if (client != null) client.sendAnimation(animation, id)
        else connections.forEach {
            it.value.sendAnimation(animation, id)
        }
    }

}