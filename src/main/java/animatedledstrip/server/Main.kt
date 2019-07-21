package animatedledstrip.server

import animatedledstrip.animationutils.Animation
import animatedledstrip.animationutils.AnimationData
import animatedledstrip.colors.ccpresets.CCBlack
import animatedledstrip.colors.ccpresets.CCBlue
import animatedledstrip.leds.AnimatedLEDStrip
import animatedledstrip.leds.AnimatedLEDStripKotlinPi
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import com.diozero.ws281xj.PixelAnimations.delay
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import tornadofx.*
import java.io.FileInputStream
import java.util.*

val properties = Properties()

class EmulatedLEDStripViewer : App(WS281xEmulator::class)

lateinit var leds: AnimatedLEDStrip       // Our LED strip instance - will be initialized in main

val animationQueue = mutableListOf<String>("C 0")

var quit = false    // Tracks if server should remain alive

var socketPort1 = 5

var socketPort2 = 6

var hostIP: String? = null

@Suppress("EXPERIMENTAL_API_USAGE")
fun main(args: Array<String>) {

    val options = Options()
    options.addOption("d", "Enable debugging")
    options.addOption("t", "Enable trace debugging")
    options.addOption("v", "Enable verbose log statements")
    options.addOption("q", "Disable log outputs")
    options.addOption("e", "Emulate LED strip and launch emulator")
    options.addOption("E", "Emulate LED strip but do NOT launch emulator")
    options.addOption("i", "Enable image debugging")
    options.addOption("T", "Run tests")

    val cmdline = DefaultParser().parse(options, args)

    val pattern =
        if (cmdline.hasOption("v")) "{date:yyyy-MM-dd HH:mm:ss} [{thread}] {class}.{method}()\n{level}: {message}"
        else "{{level}:|min-size=8} {message}"

    val level =
        when {
            cmdline.hasOption("t") -> Level.TRACE
            cmdline.hasOption("d") -> Level.DEBUG
            cmdline.hasOption("q") -> Level.OFF
            else -> Level.INFO
        }

    Configurator.defaultConfig().formatPattern(pattern).level(level).activate()

    try {
        Logger.debug("Loading led.config")
        properties.load(FileInputStream("led.config"))          // Load config file
    } catch (e: Exception) {
        Logger.warn("No led.config found")
    }

    leds = when (cmdline.hasOption("e") || cmdline.hasOption("E")) {
        false -> {
            AnimatedLEDStripKotlinPi(
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
                imageDebugging = cmdline.hasOption("i")
            )
        }
        true -> {
            EmulatedAnimatedLEDStrip(
                try {
                    Logger.trace("Trying to load numLEDs from led.config")
                    properties.getProperty("numLEDs").toInt()           // If config file has numLEDs property
                } catch (e: Exception) {
                    Logger.warn("No numLEDs in led.config or led.config does not exist")
                    240                                                 // Else default
                },
                imageDebugging = cmdline.hasOption("i")
            )
        }

    }

//    leds.addCustomAnimation(
//        """
//            import animatedledstrip.leds.*
//            val leds = bindings["leds"]!! as AnimatedLEDStrip
//            var animation = bindings["animation"] as AnimationData
//            with(leds){
//                setStripColor(animation.color1)
//                Thread.sleep(1000)
//                setStripColor(animation.color2)
//                run(AnimationData(mapOf("Animation" to "WIP", "Color1" to animation.color3.hex, "Direction" to 'F')))
//            }""".trimIndent(),
//        "COL2"
//    )

    Logger.trace("Initializing AnimationHandler")
    AnimationHandler                                            // Initialize AnimationHandler object

    AnimationHandler.addAnimation(AnimationData().animation(Animation.COLOR).color(CCBlack))
    /*  Launch loop to read from local terminal, mainly for a 'q' from the user */
    Logger.trace("Launching local terminal tread")
    GlobalScope.launch(newSingleThreadContext("Local Terminal")) {
        while (!quit) {
            Logger.trace("Local terminal waiting for input")
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
        SocketConnections.add(socketPort1).apply {
            openSocket()
        }
    }

    GlobalScope.launch(newSingleThreadContext("AppConnection")) {
        SocketConnections.add(socketPort2).apply {
            openSocket()
        }
    }

    if (cmdline.hasOption("T")) AnimationHandler.addAnimation(AnimationData().animation(Animation.COLOR).color(CCBlue))

    /*  If we told the LEDs to use EmulatedWS281x as their superclass, start the emulation GUI */
    if (leds is EmulatedAnimatedLEDStrip && !cmdline.hasOption("E")) {
        Logger.trace("Starting emulated LED strip GUI")
        GlobalScope.launch {
//            launch<EmulatedLEDStripViewer>(args)
        }
    }

    /* Endless loop that will break when the server is told to quit */
    while (!quit) {
        runBlocking { delay(1) }
    }

    Logger.debug("Quit has become true")

    /**
     *  Turns off LEDs and sends a 'Q' to the GUI
     */
    fun shutdownServer() {
        leds.setStripColor(0)
        delay(500)
        leds.toggleRender()
        delay(2000)
        return
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

