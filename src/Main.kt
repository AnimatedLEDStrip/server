import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.lang.Exception

val leds = AnimatedLEDStripConcurrent(240, 12)

val animationQueue = mutableListOf<String>("C 0")

val palette1 = RGBPalette16(
        CCRed,
        ColorContainer(0xD52A00),
        ColorContainer(0xAB5500),
        ColorContainer(0xAB7F00),
        ColorContainer(0xABAB00),
        ColorContainer(0x56D500),
        CCGreen,
        ColorContainer(0x00D52A),
        ColorContainer(0x00AB55),
        ColorContainer(0x0056AA),
        CCBlue,
        ColorContainer(0x2A00D5),
        ColorContainer(0x5500AB),
        ColorContainer(0x7F0081),
        ColorContainer(0xAB0055),
        ColorContainer(0xD5002B)
)

//var disconnected = false
var quit = false

fun main(args: Array<String>) {
    AnimationHandler
    GlobalScope.launch {
        while (!quit) {
            val strIn = readLine()
            if (strIn?.toUpperCase() == "Q") {
                leds.setStripColor(0)
                quit = true
            }
        }
    }
    GlobalScope.launch {
        LocalSocket.openSocket()
    }
    GlobalScope.launch {
        RemoteSocket.openSocket()
    }
//    val serverSocket = ServerSocket(5)
//    val remoteServerSocket = ServerSocket(6)
    var taskList: MutableList<String>
    var out: PrintWriter? = null
    GlobalScope.launch {
        while (out == null) {
            try {
                out = PrintWriter(LocalSocket.clientSocket?.getOutputStream(), true)
            } catch (e: Exception) {}
        }
    }

    while (!quit) {
//        val clientSocket = serverSocket.accept()
//        val remoteClientSocket = remoteServerSocket.accept()
//        disconnected = false
//        println("Connection Established")


//        val socIn = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
//        var input: String?
//            GlobalScope.launch {
//                while (!disconnected) {
//                    input = socIn.readLine()
//                    if (animationQueue.checkForNullAndAdd(input)) disconnected = true
//                }
//            }
        val taskString: String = animationQueue[0]
        taskList = taskString.split(" ").toMutableList()
        runAnimation(taskList)
        out?.println("C")
        if (animationQueue.size > 1) animationQueue.removeAt(0)

//        try {
//            val header = "${when(LocalSocket.isDisconnected()){
//                true -> "L"
//                false -> "-"
//            }}${when(RemoteSocket.isDisconnected()){
//                true -> "R: ${RemoteSocket.getClientIP()}"
//                false -> "-"
//            }}"
//            println(header + animationQueue)
//        } catch (e: ConcurrentModificationException) {
//        }
//        println("Connection Lost")
    }

    fun shutdownServer() {
        leds.setStripColor(0)
        if (!LocalSocket.isDisconnected()) out?.println("Q")
        System.exit(0)
    }

    shutdownServer()
}


fun MutableList<String>.checkForNullAndAdd(input: String?): Boolean {
    if (input == null || input.toUpperCase() == "Q") return true
    this.add(input)
    return false
}

