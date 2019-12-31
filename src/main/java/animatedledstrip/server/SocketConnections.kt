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

package animatedledstrip.server

import animatedledstrip.animationutils.Animation
import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.id
import animatedledstrip.utils.getDataTypePrefix
import animatedledstrip.utils.json
import animatedledstrip.utils.jsonToAnimationData
import animatedledstrip.utils.toUTF8
import kotlinx.coroutines.*
import org.pmw.tinylog.Logger
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.nio.charset.Charset

/**
 * An object for creating, tracking and using connections that clients can connect to
 */
object SocketConnections {

    var hostIP: String? = null

    /**
     * A Map<Int, Connection> of all opened ports mapped to their respective
     * Connection instances.
     */
    val connections = mutableMapOf<Int, Connection>()

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
        connections[port] = connection
        return connection
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private val connectionThreadPool = newFixedThreadPoolContext(250, "Connections")

    /**
     * Represents a single port that a client can connect to.
     *
     * @property port The port to use
     * @property server The server creating the connection
     */
    class Connection(val port: Int, private val server: AnimatedLEDStripServer<*>) {

        private val serverSocket = ServerSocket(
            port,
            0,
            if (hostIP == null) null else InetAddress.getByName(hostIP)
        )
        var clientSocket: Socket? = null
        val connected: Boolean
            get() = clientSocket?.isConnected ?: false

        var sendLogs = false

        private var socOut: OutputStream? = null
        var job: Job? = null

        fun status(): String {
            return when {
                job == null -> "Stopped"
                !connected -> "Waiting"
                connected -> "Connected"
                else -> "Unknown"
            }
        }

        /**
         * Open the connection
         */
        fun open() {
            if (job?.isActive != true)
                job = GlobalScope.launch(connectionThreadPool) {
                    Logger.debug("Starting port $port")
                    openSocket()
                }
            else Logger.warn("Port $port already running")
        }

        fun close() {
            job?.cancel()
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
            while (server.running) {
                try {
                    Logger.debug("Socket at port $port started")
                    var socIn: InputStream? = null
                    withContext(Dispatchers.IO) {
                        clientSocket = serverSocket.accept()
                        yield()
                        socIn = clientSocket?.getInputStream() ?: error("Could not create input stream")
                        socOut = clientSocket?.getOutputStream()
                        clientSocket?.soTimeout = 1000
                    }
                    Logger.info("Connection on port $port Established")
                    // Send info about this strip and all current running continuous animations
                    // to newly connected client
                    sendInfo()
                    server.leds.runningAnimations.animations.forEach {
                        sendAnimation(it.animation, client = this@Connection)
                    }
                    var input = ByteArray(10000)
                    var count = -1
                    while (connected) {
                        while (true)
                            try {
                                withContext(Dispatchers.IO) {
                                    count = socIn?.read(input) ?: throw SocketException("Socket null")
                                }
                                break
                            } catch (e: SocketTimeoutException) {
                                yield()
                                continue
                            }

                        if (count == -1) throw SocketException("Connection closed")
                        Logger.debug(input.toString(Charset.forName("utf-8")).take(count))
                        Logger.debug("Bytes: ${input.toString(Charset.forName("utf-8")).take(count).toByteArray().map { it.toString() }}")
                        when (input.toUTF8(count).getDataTypePrefix()) {
                            "DATA" -> server.leds.addAnimation(input.toUTF8(count).jsonToAnimationData())
                            "CMD " -> server.parseTextCommand(input.toUTF8(count), client = this)
                            else -> Logger.warn("Incorrect data type")
                        }
                        input = ByteArray(1000)
                    }
                } catch (e: SocketException) {  // Catch disconnections
                    Logger.warn("Connection on port $port lost: $e")
                }
            }
        }

        /**
         * Send animation data to the client along with an ID.
         *
         * @param animation An AnimationData containing data about the animation
         * @param id The ID for the animation
         */
        fun sendAnimation(animation: AnimationData, id: String = animation.id) {
            if (!connected) {
                Logger.debug("Could not send animation on port $port: Not Connected")
                return
            }
            runBlocking {
                withContext(Dispatchers.IO) {
                    socOut?.write(
                        animation
                            .id(
                                if ((animation.animation == Animation.CUSTOMANIMATION ||
                                            animation.animation == Animation.CUSTOMREPETITIVEANIMATION) &&
                                    animation.id.length == 1
                                )
                                    "${animation.id} $id"
                                else id
                            ).json()
                    )
                        ?: Logger.debug("Could not send animation $id: Connection socket null")
                    if (animation.animation == Animation.ENDANIMATION) Logger.debug("Sent end of animation $id")
                    else Logger.debug("Sent animation $id")
                }
            }
        }

        /**
         * Send strip info to the client.
         */
        fun sendInfo() {
            if (!connected) {
                Logger.debug("Could not send info on port $port: Not Connected")
                return
            }
            runBlocking {
                withContext(Dispatchers.IO) {
                    socOut?.write(server.stripInfo.json())
                }
            }
        }

        /**
         * Send a string to the client.
         */
        fun sendString(str: String) {
            // Note: Don't include any logging statements in this function
            // (this will create an endless loop)
            if (!connected) {
                return
            }
            runBlocking {
                withContext(Dispatchers.IO) {
                    socOut?.write(str.toByteArray(Charset.forName("utf-8")))
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
     * @param animation An AnimationData instance containing info about the animation
     * @param id The ID for the animation
     * @param client Used to specify which client should receive the data. If
     * null, data is sent to all clients
     */
    fun sendAnimation(animation: AnimationData, id: String = animation.id, client: Connection? = null) {
        if (client != null) client.sendAnimation(animation, id)
        else connections.forEach {
            it.value.sendAnimation(animation, id)
        }
    }

    /**
     * Send a string to one or all terminal client(s).
     *
     * @param str The string to send to the terminal client
     * @param client Used to specify which client should receive the string. If
     * null, string is sent to all terminal clients
     */
    fun sendLog(str: String, client: Connection? = null) {
        // Note: Don't include any logging statements in this function
        // (this will create an endless loop)
        if (client != null) client.sendString(str)
        else connections.forEach {
            if (it.value.sendLogs) it.value.sendString(str)
        }
    }

}