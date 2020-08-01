/*
 *  Copyright (c) 2018-2020 AnimatedLEDStrip
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

import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.EndAnimation
import animatedledstrip.animationutils.definedAnimations
import animatedledstrip.leds.AnimatedLEDStrip
import animatedledstrip.server.SocketConnections.connections
import animatedledstrip.utils.*
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

    /**
     * Used in testing by setting it equal to "0.0.0.0"
     */
    var hostIP: String? = null

    /**
     * A Map with all added ports mapped to their respective
     * Connection instances.
     */
    val connections = mutableMapOf<Int, Connection>()

    /**
     * Initialize a new connection. If a connection for the port doesn't
     * already exist, creates a Connection instance with the
     * specified port, then adds the port and Connection instance to
     * [connections].
     *
     * @return The Connection instance associated with the port
     */
    fun add(port: Int, server: AnimatedLEDStripServer<*>): Connection =
        connections.getOrPut(port, { Connection(port, server) })

    /**
     * A pool of threads used for connections
     */
    @Suppress("EXPERIMENTAL_API_USAGE")
    private val connectionThreadPool =
        newFixedThreadPoolContext(250, "Connections")

    /**
     * Represents a single port that a client can connect to
     *
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
                job?.isActive != true -> "Stopped"
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
         *      While connected, get animations and commands and pass them
         *        on to the appropriate functions,
         *      If there is a disconnection and the server is not shutting
         *        down, wait for a new connection
         */
        private suspend fun openSocket() {
            while (server.running) {
                clientSocket?.soTimeout = 1000
                Logger.debug("Socket at port $port started")

                // Accept connection
                var socIn: InputStream? = null
                withContext(Dispatchers.IO) {
                    while (server.running) {
                        while (true)
                            try {
                                clientSocket = serverSocket.accept()
                                break       // A connection has been accepted
                            } catch (e: SocketTimeoutException) {
                                yield()     // On timeout, check if coroutine has been cancelled,
                                continue    // otherwise try again
                            }
                        try {
                            socIn =
                                clientSocket?.getInputStream() ?: throw SocketException("Could not create input stream")
                            socOut = clientSocket?.getOutputStream()
                                ?: throw SocketException("Could not create output stream")
                            break           // Input and output streams have been created successfully
                        } catch (e: SocketException) {
                            continue        // Something happened when creating streams, start over with new connection
                        }
                    }
                }

                Logger.info("Connection on port $port Established")

                // Send info about this strip, all current running animations,
                // all supported animations, and all sections
                // to newly connected client
                sendInfo()
                server.leds.runningAnimations.animations.forEach {
                    sendData(it.data)
                }
                definedAnimations.forEach {
                    sendData(it.value.info)
                }
                server.leds.sections.forEach {
                    sendData(it.value)
                }

                // Receive and process input
                try {
                    var input = ByteArray(10000)
                    var count = -1
                    while (connected && server.running) {
                        while (server.running)
                            try {
                                withContext(Dispatchers.IO) {
                                    count = socIn?.read(input) ?: throw SocketException("Socket null")
                                }
                                break       // Input has been received
                            } catch (e: SocketTimeoutException) {
                                yield()     // On timeout, check if coroutine has been cancelled,
                                continue    // otherwise try again
                            }

                        if (count == -1)    // There was no input
                            throw SocketException("Connection closed")

                        // Output string and array of bytes received for debugging
                        Logger.debug(input.toUTF8(count))
                        Logger.debug(
                            "Bytes: ${input
                                .toUTF8(count)
                                .toByteArray()
                                .map { it.toString() }}"
                        )

                        val data = input.toUTF8(count)

                        // Pass input to appropriate function
                        when (val dataType = data.getDataTypePrefix()) {
                            AnimationData.prefix -> server.leds.startAnimation(data.jsonToAnimationData())
                            "CMD " -> server.parseTextCommand(data, client = this)
                            EndAnimation.prefix -> server.leds.endAnimation(data.jsonToEndAnimation())
                            AnimatedLEDStrip.sectionPrefix -> server.leds.createSection(data.jsonToSection())
                            else -> Logger.warn("Incorrect data type: $dataType")
                        }

                        input = ByteArray(10000)    // Reset ByteArray
                    }
                } catch (e: SocketException) {  // Catch disconnections
                    Logger.warn("Connection on port $port lost: $e")
                }
            }
        }

        fun sendData(data: SendableData) {
            send(data.json())
            when (data) {
                is AnimationData -> Logger.debug("Sent animation ${data.id}")
                is EndAnimation -> Logger.debug("Sent end of animation ${data.id}")
                else -> Logger.debug("Sent $data")
            }
        }

        /**
         * Send strip info to the client
         */
        fun sendInfo() = send(server.stripInfo.json())

        /**
         * Send a string to the client
         */
        fun sendString(str: String) {
            // Note: Don't include any logging statements in this function
            // (this will create an endless loop)
            if (!connected) {
                return
            }
            // Note that this does not use the send method in order to prevent
            // loops caused by the log statements in that function
            runBlocking {
                withContext(Dispatchers.IO) {
                    try {
                        socOut?.write(str.toByteArray(Charset.forName("utf-8")))
                    } catch (e: SocketException) {
                    }
                }
            }
        }

        /**
         * Send an array of bytes to a client
         */
        private fun send(data: ByteArray) {
            if (!connected) {
                Logger.debug("Could not send to port $port: Not Connected")
                return
            }
            runBlocking {
                withContext(Dispatchers.IO) {
                    try {
                        socOut?.write(data)
                            ?: Logger.debug("Could not send to port $port: Connection socket null")
                    } catch (e: SocketException) {
                        Logger.debug("Could not send to port $port: Disconnect")
                    }
                }
            }
        }


        override fun toString(): String {
            return "Connection@${serverSocket.inetAddress.toString().removePrefix("/")}:$port"
        }
    }


    fun sendData(data: SendableData, client: Connection? = null) {
        if (client != null) client.sendData(data)
        else connections.forEach {
            it.value.sendData(data)
        }
    }

    /**
     * Send animation data to one or all client(s).
     *
     * @param client Used to specify which client should receive the data. If
     * null, data is sent to all clients
     */
    fun sendAnimation(animation: AnimationData, client: Connection? = null) =
        sendData(animation, client)

    fun sendEndAnimation(animation: EndAnimation, client: Connection? = null) =
        sendData(animation, client)

    /**
     * Send a string to one or all client(s).
     *
     * @param client Used to specify which client should receive the string. If
     * null, string is sent to all clients
     */
    fun sendLog(str: String, client: Connection? = null) {
        // Note: Don't include any logging statements in this function
        // (this will create an endless loop)
        if (client != null) client.sendString(str + DELIMITER)
        else connections.forEach {
            if (it.value.sendLogs) it.value.sendString(str + DELIMITER)
        }
    }

}