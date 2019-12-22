/*
 *  Copyright (c) 2019 AnimatedLEDStrip
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package animatedledstrip.server

import animatedledstrip.animationutils.Animation
import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.animation
import animatedledstrip.animationutils.color
import animatedledstrip.colors.ccpresets.CCBlue
import animatedledstrip.leds.AnimatedLEDStrip
import animatedledstrip.leds.StripInfo
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.utils.delayBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class AnimatedLEDStripServer<T : AnimatedLEDStrip>(
    args: Array<String>,
    ledClass: KClass<T>
) {

    /** Is the server running */
    internal var running = false


    /* Get command line options */

    private val cmdline: CommandLine =
        DefaultParser().parse(options, args)


    /* Set logging format and level based on command line options */

    init {
        val loggingPattern: String =
            if (cmdline.hasOption("v"))
                "{date:yyyy-MM-dd HH:mm:ss} [{thread}] {class}.{method}()\n{level}: {message}"
            else
                "{{level}:|min-size=8} {message}"

        assert(
            if (cmdline.hasOption("v"))
                loggingPattern == "{date:yyyy-MM-dd HH:mm:ss} [{thread}] {class}.{method}()\n{level}: {message}"
            else
                loggingPattern == "{{level}:|min-size=8} {message}"
        )

        val loggingLevel =
            when {
                cmdline.hasOption("t") -> Level.TRACE
                cmdline.hasOption("d") -> Level.DEBUG
                cmdline.hasOption("q") -> Level.OFF
                else -> Level.INFO
            }

        Configurator.defaultConfig()
            .formatPattern(loggingPattern)
            .level(loggingLevel)
            .addWriter(SocketWriter())
            .activate()
    }

    init {
        if (cmdline.hasOption("h")) {
            HelpFormatter().printHelp("ledserver.jar", options)
        }
    }

    /* Get properties file */

    internal val propertyFileName: String =
        cmdline.getOptionValue("f")
            ?: "led.config"

    internal val properties =
        Properties().apply {
            try {
                load(FileInputStream(propertyFileName))
            } catch (e: FileNotFoundException) {
                Logger.warn("File $propertyFileName not found")
            }
        }


    /* Determine if the local port should be created */
    internal val createLocalPort: Boolean =
        (ledClass != EmulatedAnimatedLEDStrip::class ||
                cmdline.hasOption("L"))

    /* Get port numbers */

    internal val localPort: Int =
        cmdline.getOptionValue("L")?.toIntOrNull()
            ?: properties.getProperty("local_port")?.toIntOrNull()
            ?: 1118

    internal val ports =
        mutableListOf<Int>().apply {
            properties.getProperty("ports")?.split(' ')?.forEach {
                requireNotNull(it.toIntOrNull()) { "Could not parse port \"$it\"" }
                this += it.toInt()
            }
            if (createLocalPort) {
                this += localPort            // local port
            }
        }


    /* Arguments for creating the AnimatedLEDStrip instance */

    internal val persistAnimations: Boolean =
        !cmdline.hasOption("no-persist") &&
                (cmdline.hasOption("P") ||
                        properties.getProperty("persist")?.toBoolean() == true)


    internal val numLEDs: Int =
        cmdline.getOptionValue("n")?.toIntOrNull()
            ?: properties.getProperty("numLEDs")?.toIntOrNull()
            ?: 240

    internal val pin: Int =
        cmdline.getOptionValue("p")?.toIntOrNull()
            ?: properties.getProperty("pin")?.toInt()
            ?: 12

    internal val imageDebuggingEnabled: Boolean =
        cmdline.hasOption("i")

    internal var outputFileName: String? =
        cmdline.getOptionValue("o")

    internal val rendersBeforeSave: Int =
        cmdline.getOptionValue("r")?.toIntOrNull()
            ?: properties.getProperty("renders")?.toIntOrNull()
            ?: 1000

    internal val threadCount: Int = 100

    /* Create strip instance and animation handler */

    val stripInfo: StripInfo
    val leds: AnimatedLEDStrip

    /*
      Check validity of constructor before calling so user can be notified about
      what changes need to be made rather than just throwing an IllegalArgumentException
      without any explanation
    */
    init {
        stripInfo = StripInfo(
            numLEDs = numLEDs,
            pin = pin,
            imageDebugging = imageDebuggingEnabled,
            fileName = outputFileName,
            rendersBeforeSave = rendersBeforeSave,
            threadCount = threadCount
        )

        val ledConstructor = ledClass.primaryConstructor
        requireNotNull(ledConstructor)
        require(ledConstructor.parameters.size == 1)
        require(ledConstructor.parameters[0].type.classifier == StripInfo::class)

        leds = ledConstructor.call(
            stripInfo
        )

        // TODO: Change saved files to use JSON
        leds.startAnimationCallback = {
            SocketConnections.sendAnimation(it)
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    ObjectOutputStream(FileOutputStream(".animations/${it.fileName}")).apply {
                        writeObject(it)
                        close()
                    }
                }
            }
        }
        leds.endAnimationCallback = {
            SocketConnections.sendAnimation(it.copy(animation = Animation.ENDANIMATION))
            if (File(".animations/${it.fileName}").exists())
                Files.delete(Paths.get(".animations/${it.fileName}"))
        }
    }

//    internal val animationHandler =
//        AnimationHandler(leds, persistAnimations = persistAnimations)

    /**
     * The test animation
     */
    var testAnimation =
        AnimationData().animation(Animation.COLOR).color(CCBlue)

    /* Start and stop methods */

    /**
     * Start the server
     *
     * @return This server
     */
    fun start(): AnimatedLEDStripServer<T> {
        if (persistAnimations) {
            val dir = File(".animations")
            if (!dir.isDirectory)
                dir.mkdirs()
            else {
                GlobalScope.launch {
                    File(".animations/").walk().forEach {
                        if (!it.isDirectory && it.name.endsWith(".anim")) try {
                            ObjectInputStream(FileInputStream(it)).apply {
                                val obj = readObject() as AnimationData
                                leds.addAnimation(obj)
                                close()
                            }
                        } catch (e: ClassCastException) {
                            it.delete()
                        } catch (e: InvalidClassException) {
                            it.delete()
                        }
                    }
                }
            }
        }

        running = true
        ports.forEach {
            if (!SocketConnections.connections.containsKey(it))
                SocketConnections.add(it, server = this)
            SocketConnections.connections[it]?.open()
        }
        if (createLocalPort) {
            if (SocketConnections.localConnection == null)
                SocketConnections.add(localPort, server = this, local = true)
            SocketConnections.localConnection?.open()
        }
        if (cmdline.hasOption("T")) leds.addAnimation(testAnimation)
        return this
    }

    /** Stop the server */
    fun stop() {
        leds.setStripColor(0)
        delayBlocking(500)
        leds.toggleRender()
        delayBlocking(2000)
        running = false
    }

    internal fun parseTextCommand(command: String) {
        Logger.trace("Parsing \"$command\"")
        val line = command.toUpperCase().split(" ")
        return when (line[0]) {
            "QUIT", "Q" -> {
                Logger.info("Shutting down server")
                stop()
            }
            "DEBUG" -> {
                setLoggingLevel(Level.DEBUG)
                Logger.debug("Set logging level to debug")
            }
            "TRACE" -> {
                setLoggingLevel(Level.TRACE)
                Logger.trace("Set logging level to trace")
            }
            "INFO" -> {
                setLoggingLevel(Level.INFO)
                Logger.info("Set logging level to info")
            }
            "CLEAR" -> {
                leds.addAnimation(AnimationData().animation(Animation.COLOR))
                Unit
            }
            "SHOW", "S" -> {
                if (line.size > 1) Logger.info(
                    "${line[1]}: ${leds.runningAnimations.entries.toMap()[line[1]]?.animation ?: "NOT FOUND"}"
                )
                else Logger.info("Running Animations: ${leds.runningAnimations.ids}")
            }
            "CONNECTIONS", "C" -> {
                if (line.size > 1) {
                    when (line[1]) {
                        "LIST" -> {
                            for (port in ports) {
                                Logger.info(
                                    "Port $port: ${SocketConnections.connections[port]?.status() ?: "Not Found"}"
                                )
                            }
                        }
                        "START" -> {
                            if (line.size > 2) {
                                Logger.debug("Manually starting connection at port ${line[2]}")
                                SocketConnections.connections.getOrDefault(line[2].toIntOrNull(), null)?.open()
                                    ?: Logger.warn("No connection on port ${line[2]}")
                            } else Logger.warn("Port must be specified")
                        }
                        "STOP" -> {
                            if (line.size > 2) {
                                Logger.debug("Manually stopping connection at port ${line[2]}")
                                SocketConnections.connections.getOrDefault(line[2].toIntOrNull(), null)?.close()
                                    ?: Logger.warn("No connection on port ${line[2]}")
                            } else Logger.warn("Port must be specified")
                        }
                        else -> Logger.warn("Invalid sub-command")
                    }
                } else Logger.warn("Sub-command must be specified")
            }
            "END" -> {
                if (line.size > 1) {
                    if (line[1].toUpperCase() == "ALL") {
                        val animations = leds.runningAnimations.ids.toList()
                        animations.forEach {
                            leds.endAnimation(it)
                        }
                    } else for (i in 1 until line.size)
                        leds.endAnimation(line[i])
                } else Logger.warn("Animation ID or \"all\" must be specified")
            }
            else -> Logger.warn("$command is not a valid command")
        }
    }

    private fun setLoggingLevel(level: Level) {
        Configurator.currentConfig().level(level).activate()
    }

}