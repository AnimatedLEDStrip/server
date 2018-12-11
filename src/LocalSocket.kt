import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.net.ServerSocket
import java.net.Socket

object LocalSocket {
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
            println("Local Connection Established")
//            while (!disconnected) {
//                GlobalScope.launch {
//            var input: String?
                while (!disconnected) {
                    AnimationHandler.addAnimation(socIn.readObject() as Map<*, *>)
//                    input = socIn.readLine()
//                    if (animationQueue.checkForNullAndAdd(input)) disconnected = true
//                    }
                }
//            }
            println("Local Connection Lost")
        }
    }

}
