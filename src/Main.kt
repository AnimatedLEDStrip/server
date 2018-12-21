import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.App
import tornadofx.launch
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.*
import kotlin.Exception

val properties = Properties()

class EmulatedLEDStripViewer : App(WS281xEmulator::class)

//val leds = AnimatedLEDStripConcurrent(50, 12, emulated = true)
lateinit var leds: AnimatedLEDStripConcurrent

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
    try {
        properties.load(FileInputStream("led.config"))
    } catch (e: Exception) {
        println("No led.config found")
    }

    leds = AnimatedLEDStripConcurrent(
        try {
            properties.getProperty("numLEDs").toInt()
        } catch (e: Exception) {
            240
        },
        try {
            properties.getProperty("pin").toInt()
        } catch (e: Exception) {
            12
        },
        emulated = try {
            when (args[0].toUpperCase()) {
                "EMULATE" -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    )



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
//    println(leds.getByteBuffer())
    GlobalScope.launch {
        GUISocket.openSocket()
    }
    GlobalScope.launch {
        CommandLineSocket.openSocket()
    }
//    while (!::leds.isInitialized) {
//    }
//    launch<EmulatedGUI>(args)
    if (leds.isEmulated()) launch<EmulatedLEDStripViewer>(args)
//    val serverSocket = ServerSocket(5)
//    val remoteServerSocket = ServerSocket(6)
    var taskList: MutableList<String>
    var out: PrintWriter? = null
    GlobalScope.launch {
        while (out == null) {
            try {
                out = PrintWriter(GUISocket.clientSocket?.getOutputStream(), true)
            } catch (e: Exception) {
            }
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
//            val header = "${when(GUISocket.isDisconnected()){
//                true -> "L"
//                false -> "-"
//            }}${when(CommandLineSocket.isDisconnected()){
//                true -> "R: ${CommandLineSocket.getClientIP()}"
//                false -> "-"
//            }}"
//            println(header + animationQueue)
//        } catch (e: ConcurrentModificationException) {
//        }
//        println("Connection Lost")
    }

    fun shutdownServer() {
        leds.setStripColor(0)
        if (!GUISocket.isDisconnected()) out?.println("Q")
        System.exit(0)
    }

    shutdownServer()
}


fun MutableList<String>.checkForNullAndAdd(input: String?): Boolean {
    if (input == null || input.toUpperCase() == "Q") return true
    this.add(input)
    return false
}

