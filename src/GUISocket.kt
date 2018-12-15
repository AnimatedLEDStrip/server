import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.net.ServerSocket
import java.net.Socket

object GUISocket {
    private val serverSocket = ServerSocket(5)
    var clientSocket: Socket? = null
    private var disconnected = true

    fun isDisconnected() = !disconnected

    fun openSocket() {
        while (!quit) {
            clientSocket = serverSocket.accept()
            val socIn = ObjectInputStream(BufferedInputStream(clientSocket!!.getInputStream()))
//            val socIn = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
            disconnected = false
            println("GUI Connection Established")
//            while (!disconnected) {
//                GlobalScope.launch {
//            var input: String?
            var input: Map<*, *>
                while (!disconnected) {
                    input = socIn.readObject() as Map<*, *>
                    val remoteQuit = input["Quit"] as Boolean? ?: false
                    if (remoteQuit) disconnected = true
                    else AnimationHandler.addAnimation(input)
//                    input = socIn.readLine()
//                    if (animationQueue.checkForNullAndAdd(input)) disconnected = true
//                    }
                }
//            }
            println("GUI Connection Lost")
        }
    }

}
