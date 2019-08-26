package animatedledstrip.server

import animatedledstrip.animationutils.AnimationData
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
    fun add(port: Int): Connection {
        val connection = Connection(port)
        connections[port] = connection
        return connection
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    private val connectionThreadPool = newFixedThreadPoolContext(250, "Connections")

    class Connection(val port: Int) {
        private val serverSocket = ServerSocket(
            port,
            0,
            if (hostIP == null) null else InetAddress.getByName(hostIP)
        )
        var clientSocket: Socket? = null
        private var disconnected = true
        private var socOut: ObjectOutputStream? = null
        var textBased = false

        val isDisconnected: Boolean
            get() = disconnected

        /**
         * Open the connection
         */
        fun open() {
            GlobalScope.launch(connectionThreadPool) {
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
                while (!quit) {
                    try {
                        clientSocket = serverSocket.accept()
                        Logger.trace("Accepted new connection on port $port")
                        Logger.trace("Initializing input stream")
                        val socIn = ObjectInputStream(clientSocket!!.getInputStream())
                        Logger.trace("Initializing output stream")
                        socOut = ObjectOutputStream(clientSocket!!.getOutputStream())
                        Logger.debug("Sending currently running animations to GUI")
                        // Send all current running continuous animations to newly connected client
                        AnimationHandler.continuousAnimations.forEach {
                            it.value.sendAnimation(this@Connection)
                        }
                        disconnected = false
                        Logger.info("Connection on port $port Established")
                        var input: Any?
                        while (!disconnected) {
                            Logger.trace("Waiting for input")
                            try {
                                input = socIn.readObject() as AnimationData
                                Logger.trace("Input received")
                                AnimationHandler.addAnimation(input)
                            } catch (e: ClassCastException) {
                                Logger.error("Could not cast input to AnimationData")
                                continue
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
         * @param animation An AnimationData containing data about the animation
         * @param id The ID for the animation
         */
        fun sendAnimation(animation: AnimationData, id: String) {
            if (!isDisconnected) {
                Logger.trace("Animation to send: $animation")
                runBlocking {
                    withTimeout(5000) {
                        withContext(Dispatchers.IO) {
                            socOut?.writeObject(animation.id(if (animation.id == "") id else "${animation.id} $id"))
                                ?: Logger.debug("Could not send animation $id: Connection socket null")
                            Logger.debug("Sent animation $id")
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
            Logger.trace("Sent animation to client on port ${it.key}")
        }
    }

}