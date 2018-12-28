import java.io.BufferedInputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket

object GUISocket {
    private val serverSocket = ServerSocket(5)
    var clientSocket: Socket? = null
    private var disconnected = true
    private var socOut: ObjectOutputStream? = null

    fun isDisconnected() = disconnected

    fun openSocket() {
        while (!quit) {
            clientSocket = serverSocket.accept()
            val socIn = ObjectInputStream(BufferedInputStream(clientSocket!!.getInputStream()))
            socOut = ObjectOutputStream(clientSocket!!.getOutputStream())
            disconnected = false
            println("GUI Connection Established")
            var input: Map<*, *>
            try {
                while (!disconnected) {
                    input = socIn.readObject() as Map<*, *>
                    val remoteQuit = input["Quit"] as Boolean? ?: false
                    if (remoteQuit) disconnected = true
                    else AnimationHandler.addAnimation(input)
                }
            } catch (e: Exception) {
                println("GUI Connection Lost : $e")
            }
        }
    }

    fun sendAnimation(animation: Map<*, *>, id: String) {
        if (!isDisconnected()) {
            socOut!!.writeObject(mapOf("Animation" to animation, "ID" to id))
        }
    }
}
