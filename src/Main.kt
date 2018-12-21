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

lateinit var leds: AnimatedLEDStripConcurrent

val animationQueue = mutableListOf<String>("C 0")

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
    GlobalScope.launch {
        GUISocket.openSocket()
    }
    GlobalScope.launch {
        CommandLineSocket.openSocket()
    }

    if (leds.isEmulated()) launch<EmulatedLEDStripViewer>(args)

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
        val taskString: String = animationQueue[0]
        taskList = taskString.split(" ").toMutableList()
        runAnimation(taskList)
        out?.println("C")
        if (animationQueue.size > 1) animationQueue.removeAt(0)
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

