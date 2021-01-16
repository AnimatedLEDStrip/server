/*
 * Copyright (c) 2018-2021 AnimatedLEDStrip
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package animatedledstrip.server

import animatedledstrip.animations.Animation
import animatedledstrip.animations.definedAnimations
import animatedledstrip.communication.*
import animatedledstrip.leds.animationmanagement.*
import animatedledstrip.leds.colormanagement.CurrentStripColor
import animatedledstrip.leds.sectionmanagement.Section
import animatedledstrip.leds.stripmanagement.StripInfo
import animatedledstrip.server.SocketConnections.connections
import animatedledstrip.utils.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        connections.getOrPut(port) { Connection(port, server) }


    fun sendData(data: SendableData, client: Connection? = null) {
        if (client != null) client.sendData(data)
        else connections.forEach {
            it.value.sendData(data)
        }
    }

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

    /**
     * Represents a single port that a client can connect to
     *
     * @property server The server creating the connection
     */
    class Connection(val port: Int, private val server: AnimatedLEDStripServer<*>) {

        var clientParams: ClientParams? = null

        private val serverSocket = ServerSocket(
            port,
            0,
            if (hostIP == null) null else InetAddress.getByName(hostIP),
        )
        var clientSocket: Socket? = null
        val connected: Boolean
            get() = clientSocket?.isConnected ?: false

        var sendLogs = false

        private var socIn: InputStream? = null
        private var socOut: OutputStream? = null
        var job: Job? = null

        private var delayedSenderJob: Job? = null
        private val bufferedData: MutableList<SendableData> = mutableListOf()
        private val bufferedDataMutex: Mutex = Mutex()
        private val bufferedDataChannel: Channel<SendableData> = Channel()

        var bufferedDataHandler: Job? = null

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
                job = GlobalScope.launch {
                    Logger.d("Socket Connection") { "Starting port $port" }
                    openSocket()
                }
            else Logger.w("Socket Connection") { "Port $port already running" }
        }

        fun close() {
            job?.cancel()
            delayedSenderJob?.cancel()
            bufferedDataHandler?.cancel()
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
                Logger.d("Socket Connection") { "Socket at port $port started" }

                // Accept connection
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
                            socIn = clientSocket?.getInputStream()
                                    ?: throw SocketException("Could not create input stream")
                            socOut = clientSocket?.getOutputStream()
                                     ?: throw SocketException("Could not create output stream")
                            break           // Input and output streams have been created successfully
                        } catch (e: SocketException) {
                            continue        // Something happened when creating streams, start over with new connection
                        }
                    }
                }

                Logger.i("Socket Connection") { "Connection on port $port Established" }

                // Receive and process input
                try {
                    while (connected && server.running) processData(receiveData())
                } catch (e: SocketException) {  // Catch disconnections
                    Logger.w("Socket Connection") { "Connection on port $port lost: $e" }
                }
            }
        }

        private suspend fun receiveData(): String {
            val input = ByteArray(10000)
            var count: Int = -1

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

            // Output string and array of bytes received for debugging
            Logger.d("Socket Connection") { input.toUTF8String(count) }
            Logger.d("Socket Connection") {
                "Bytes: ${
                    input
                        .toUTF8String(count)
                        .toByteArray()
                        .map { it.toString() }
                }"
            }

            return input.toUTF8String(count)
        }

        private fun processData(input: String) {
            for (d in splitData(input)) {
                if (d.isEmpty()) continue
                when (val data = d.decodeJson()) {
                    is Animation.AnimationInfo -> Logger.w("Socket Connection") { "Receiving AnimationInfo is not supported by server" }
                    is AnimationToRunParams -> server.leds.animationManager.startAnimation(data)
                    is ClientParams -> {
                        // Send info about this strip, all current running animations,
                        // all supported animations, and all sections
                        // to newly connected client, if desired
                        if (data.sendStripInfoOnConnection) sendInfo()
                        if (data.sendRunningAnimationInfoOnConnection)
                            server.leds.animationManager.runningAnimations.values.forEach {
                                sendData(it.params)
                            }
                        if (data.sendDefinedAnimationInfoOnConnection)
                            definedAnimations.forEach {
                                sendData(it.value.info)
                            }
                        if (data.sendSectionInfoOnConnection)
                            server.leds.sectionManager.sections.forEach {
                                sendData(it.value)
                            }

                        if (data.sendAnimationStart == MessageFrequency.INTERVAL ||
                            data.sendAnimationEnd == MessageFrequency.INTERVAL ||
                            data.sendSectionCreation == MessageFrequency.INTERVAL
                        ) {
                            delayedSenderJob = GlobalScope.launch {
                                while (true) {
                                    delay(data.bufferedMessageInterval)
                                    bufferedDataMutex.withLock {
                                        bufferedData.forEach {
                                            send(it.json())
                                        }
                                        bufferedData.clear()
                                    }
                                }
                            }
                            bufferedDataHandler = GlobalScope.launch {
                                for (newData in bufferedDataChannel)
                                    bufferedDataMutex.withLock {
                                        bufferedData.add(newData)
                                    }
                            }
                        }

                        clientParams = data
                    }
                    is Command -> server.commandParser.parseCommand(data.command, arg = this)
                    is CurrentStripColor -> Logger.w("Socket Connection") { "Receiving CurrentStripColor is not supported by server" }
                    is EndAnimation -> server.leds.animationManager.endAnimation(data)
                    is Message -> Logger.w("Socket Connection") { "Receiving Message is not supported by server" }
                    is RunningAnimationParams -> Logger.w("Socket Connection") { "Receiving RunningAnimationParams is not supported by server" }
                    is Section -> server.leds.sectionManager.createSection(data)
                    is StripInfo -> Logger.w("Socket Connection") { "Receiving StripInfo is not supported by server" }
                    else -> Logger.w("Socket Connection") { "Unrecognized data type: $data" }
                }
            }
        }

        private val partialData = StringBuilder()

        private fun String.withPartialData(): String {
            val newStr = partialData.toString() + this
            partialData.clear()
            return newStr
        }

        private fun handlePartialData(inputData: List<String>): List<String> {
            partialData.append(inputData.last())
            return inputData.dropLast(1)
        }

        private fun splitData(input: String): List<String> {
            val inputData = input.withPartialData().split(DELIMITER)

            return if (!input.endsWith(DELIMITER))
                handlePartialData(inputData)
            else
                inputData
        }

        fun sendData(data: SendableData) {
            when (data) {
                is EndAnimation -> when (clientParams?.sendAnimationEnd) {
                    MessageFrequency.IMMEDIATE -> send(data.json())
                    MessageFrequency.INTERVAL -> GlobalScope.launch {
                        bufferedDataChannel.send(data)
                    }
                    else -> {
                    }
                }
                is RunningAnimationParams -> when (clientParams?.sendAnimationStart) {
                    MessageFrequency.IMMEDIATE -> send(data.json())
                    MessageFrequency.INTERVAL -> GlobalScope.launch {
                        bufferedDataChannel.send(data)
                    }
                    else -> {
                    }
                }
                is Section -> when (clientParams?.sendSectionCreation) {
                    MessageFrequency.IMMEDIATE -> send(data.json())
                    MessageFrequency.INTERVAL -> GlobalScope.launch {
                        bufferedDataChannel.send(data)
                    }
                    else -> {
                    }
                }
                else -> send(data.json())
            }
            when (data) {
                is AnimationToRunParams -> Logger.d("Socket Connection") { "Sent animation ${data.id}" }
                is EndAnimation -> Logger.d("Socket Connection") { "Sent end of animation ${data.id}" }
                else -> Logger.d("Socket Connection") { "Sent $data" }
            }
        }

        fun sendRequestedData(data: SendableData) {
            when (data) {
                is EndAnimation -> when (clientParams?.sendAnimationEnd) {
                    MessageFrequency.IMMEDIATE, MessageFrequency.REQUEST -> send(data.json())
                    MessageFrequency.INTERVAL -> GlobalScope.launch {
                        bufferedDataChannel.send(data)
                    }
                }
                is RunningAnimationParams -> when (clientParams?.sendAnimationStart) {
                    MessageFrequency.IMMEDIATE, MessageFrequency.REQUEST -> send(data.json())
                    MessageFrequency.INTERVAL -> GlobalScope.launch {
                        bufferedDataChannel.send(data)
                    }
                }
                is Section -> when (clientParams?.sendSectionCreation) {
                    MessageFrequency.IMMEDIATE, MessageFrequency.REQUEST -> send(data.json())
                    MessageFrequency.INTERVAL -> GlobalScope.launch {
                        bufferedDataChannel.send(data)
                    }
                }
                else -> send(data.json())
            }
            when (data) {
                is AnimationToRunParams -> Logger.d("Socket Connection") { "Sent animation ${data.id}" }
                is EndAnimation -> Logger.d("Socket Connection") { "Sent end of animation ${data.id}" }
                else -> Logger.d("Socket Connection") { "Sent $data" }
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
                Logger.d("Socket Connection") { "Could not send to port $port: Not Connected" }
                return
            }
            runBlocking {
                withContext(Dispatchers.IO) {
                    try {
                        socOut?.write(data)
                        ?: Logger.d("Socket Connection") { "Could not send to port $port: Connection socket null" }
                    } catch (e: SocketException) {
                        Logger.d("Socket Connection") { "Could not send to port $port: Disconnect" }
                    }
                }
            }
        }


        override fun toString(): String =
            "Connection@${serverSocket.inetAddress.toString().removePrefix("/")}:$port"
    }
}
