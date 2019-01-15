package server

import animatedledstrip.leds.*
import com.diozero.ws281xj.PixelAnimations.delay
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import tornadofx.App
import tornadofx.launch
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.*
import kotlin.Exception

val properties = Properties()

class EmulatedLEDStripViewer : App(WS281xEmulator::class)

lateinit var leds: AnimatedLEDStrip       // Our LED strip instance - will be initialized in main

val animationQueue = mutableListOf<String>("C 0")

var quit = false    // Tracks if loops should continue

fun main(args: Array<String>) {

    val options = Options()
    options.addOption("d", "Enable debugging")
    options.addOption("t", "Enable trace debugging")
    options.addOption("v", "Enable verbose log statements")
    options.addOption("e", "Emulate LED strip and launch emulator")
    options.addOption("i", "Enable image debugging")

    val cmdline = DefaultParser().parse(options, args)

    val pattern =
        if (cmdline.hasOption("v")) "{date:yyyy-MM-dd HH:mm:ss} [{thread}] {class}.{method}()\n{level}: {message}"
        else "{{level}:|min-size=8} {message}"

    val level =
        when {
            cmdline.hasOption("t") -> Level.TRACE
            cmdline.hasOption("d") -> Level.DEBUG
            cmdline.hasOption("i") -> Level.OFF
            else -> Level.INFO
        }

    Configurator.defaultConfig().formatPattern(pattern).level(level).activate()

    try {
        Logger.debug("Loading led.config")
        properties.load(FileInputStream("led.config"))          // Load config file
    } catch (e: Exception) {
        Logger.warn("No led.config found")
    }

    leds = AnimatedLEDStrip(
        try {
            Logger.trace("Trying to load numLEDs from led.config")
            properties.getProperty("numLEDs").toInt()           // If config file has numLEDs property
        } catch (e: Exception) {
            Logger.warn("No numLEDs in led.config or led.config does not exist")
            240                                                 // Else default
        },
        try {
            Logger.trace("Trying to load pin from led.config")
            properties.getProperty("pin").toInt()               // If config file has pin property
        } catch (e: Exception) {
            Logger.warn("No pin in led.config or led.config does not exist")
            10                                                  // Else default
        },
        emulated = cmdline.hasOption("e")
    )

    Logger.trace("Initializing AnimationHandler")
    AnimationHandler                                            // Initialize AnimationHandler object

    AnimationHandler.addAnimation(AnimationData(mapOf("Animation" to Animation.COLOR1, "Color1" to 0x0.toLong())))
    /*  Launch loop to read from local terminal, mainly for a 'q' from the user */
    Logger.trace("Launching local terminal tread")
    GlobalScope.launch(newSingleThreadContext("Local Terminal")) {
        while (!quit) {
            val strIn = readLine()
            Logger.trace("Read line")
            if (strIn?.toUpperCase() == "Q") {
                Logger.trace("'Q' received, shutting down server")
                leds.setStripColor(0)
                quit = true
            }
        }
    }

    /*  Start GUI Socket in separate thread */
    Logger.trace("Launching GUISocket thread")
    GlobalScope.launch(newSingleThreadContext("GUIConnection")) {
        GUISocket.openSocket()
    }

    /*  Start Command Line Socket in separate thread */
//    GlobalScope.launch {
//        CommandLineSocket.openSocket()
//    }

    /*  If we told the LEDs to use EmulatedWS281x as their superclass, start the emulation GUI */
    if (leds.isEmulated()) {
        Logger.trace("Starting emulated LED strip GUI")
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
        delay(500)
        leds.stopRender = true
        delay(2000)
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

