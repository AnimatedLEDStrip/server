package server

import animatedledstrip.leds.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.apache.commons.logging.LogFactory
import tornadofx.App
import tornadofx.launch
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.*
import kotlin.Exception

val properties = Properties()

class EmulatedLEDStripViewer : App(WS281xEmulator::class)

lateinit var leds: AnimatedLEDStripConcurrent       // Our LED strip instance - will be initialized in main

val animationQueue = mutableListOf<String>("C 0")

var quit = false    // Tracks if loops should continue

fun main(args: Array<String>) {



    val log = LogFactory.getLog("Main")

    try {
        log.debug("Loading led.config")
        properties.load(FileInputStream("led.config"))          // Load config file
    } catch (e: Exception) {
        log.warn("No led.config found")
    }

    leds = AnimatedLEDStripConcurrent(
        try {
            log.debug("Trying to load numLEDs from led.config")
            properties.getProperty("numLEDs").toInt()           // If config file has numLEDs property
        } catch (e: Exception) {
            log.warn("No numLEDs in led.config or led.config does not exist")
            240                                                 // Else default
        },
        try {
            log.debug("Trying to load pin from led.config")
            properties.getProperty("pin").toInt()               // If config file has pin property
        } catch (e: Exception) {
            log.warn("No pin in led.config or led.config does not exist")
            10                                                  // Else default
        },
        emulated = try {
            log.debug("Checking command line arguments for 'emulate'")
            when (args[0].toUpperCase()) {
                "EMULATE" -> {
                    log.info("Emulating LED strip")
                    true
                }                               // If first command line argument is emulate, true
                else -> false                                   // Else if argument exists but is different, false
            }
        } catch (e: Exception) {
            false                                               // Else false
        }
    )

    log.debug("Initializing AnimationHandler")
    AnimationHandler                                            // Initialize AnimationHandler object

    AnimationHandler.addAnimation(mapOf("Animation" to Animations.COLOR1, "Color1" to 0x0.toLong()))
    /*  Launch loop to read from local terminal, mainly for a 'q' from the user */
    GlobalScope.launch(newSingleThreadContext("Local Terminal")){
        val log = LogFactory.getLog("Local Terminal Thread")
        log.debug("Launching local terminal tread")
        while (!quit) {
            val strIn = readLine()
            log.debug("Read line")
            if (strIn?.toUpperCase() == "Q") {
                log.debug("'Q' received, shutting down server")
                leds.setStripColor(0)
                quit = true
            }
        }
    }

    /*  Start GUI Socket in separate thread */
    GlobalScope.launch(newSingleThreadContext("GUIConnection")) {
        val log = LogFactory.getLog("GUISocket thread")
        log.debug("Launching GUISocket thread")
        GUISocket.openSocket()
    }

    /*  Start Command Line Socket in separate thread */
//    GlobalScope.launch {
//        CommandLineSocket.openSocket()
//    }

    /*  If we told the LEDs to use EmulatedWS281x as their superclass, start the emulation GUI */
    if (leds.isEmulated()) {
        log.debug("Starting emulated LED strip GUI")
        launch<EmulatedLEDStripViewer>(args)
    }

    /*  Legacy code that might be able to be removed */
    var taskList: MutableList<String>
    var out: PrintWriter? = null
//    GlobalScope.launch(newSingleThreadContext(random().toString())) {
//        while (out == null) {
//            try {
//                out = PrintWriter(GUISocket.clientSocketOut?.getOutputStream(), true)
//            } catch (e: Exception) {
//            }
//        }
//    }

    /*  Checks for new animation in queue and if it exists, runs it */
    while (!quit) {
        val taskString: String = animationQueue[0]
        taskList = taskString.split(" ").toMutableList()
        runAnimation(taskList)
        out?.println("C")
        if (animationQueue.size > 1)
            animationQueue.removeAt(0) // If there are more animations waiting, remove the first one
    }

    /**
     *  Turns off LEDs and sends a 'Q' to the GUI
     */
    fun shutdownServer() {
        leds.setStripColor(0)
        if (!GUISocket.isDisconnected()) out?.println("Q")
        System.exit(0)
    }

    shutdownServer()
}


/**
 * Checks if a String? is null or 'Q', if not adds it to the list
 *
 * @param input The nullable string to be checked
 * @return Boolean representing if input was null or 'Q'
 */
fun MutableList<String>.checkForNullAndAdd(input: String?): Boolean {
    if (input == null || input.toUpperCase() == "Q") return true
    this.add(input)
    return false
}

