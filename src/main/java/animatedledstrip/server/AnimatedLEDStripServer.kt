package animatedledstrip.server

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


import animatedledstrip.animationutils.Animation
import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.animation
import animatedledstrip.animationutils.color
import animatedledstrip.colors.ccpresets.CCBlue
import animatedledstrip.leds.AnimatedLEDStrip
import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.utils.delayBlocking
import org.apache.commons.cli.DefaultParser
import org.tinylog.Logger
import org.tinylog.configuration.Configuration
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class AnimatedLEDStripServer<T : AnimatedLEDStrip>(
    args: Array<String>,
    ledClass: KClass<T>
) {
    /**
     * Is the server running
     */
    internal var running = false

    /* Command line options and properties file */

    private val cmdline = DefaultParser().parse(options, args)

    private var propertyFileName = cmdline.getOptionValue("f") ?: "led.config"

    private var outputFileName: String? = cmdline.getOptionValue("o")

    /* Set logging levels based on command line */
    init {
        val loggingPattern =
            if (cmdline.hasOption("v")) "{date:yyyy-MM-dd HH:mm:ss} [{thread}] {class}.{method}()\n{level}: {message}"
            else "{{level}:|min-size=8} {message}"

        val loggingLevel =
            when {
                cmdline.hasOption("t") -> "trace"
                cmdline.hasOption("d") -> "debug"
                cmdline.hasOption("q") -> "off"
                else -> "info"
            }

        Configuration.set("level", loggingLevel)
        Configuration.set("format", loggingPattern)
    }
    private val properties = Properties().apply {
        try {
            load(FileInputStream(propertyFileName))
        } catch (e: FileNotFoundException) {
            Logger.warn { "File $propertyFileName not found" }
        }
    }

    /* Arguments for creating the AnimatedLEDStrip instance */

    private val emulated: Boolean = cmdline.hasOption("e") || cmdline.hasOption("E")

    private val numLEDs: Int = properties.getProperty("numLEDs", "240").toInt()

    private val pin: Int = properties.getProperty("pin", "12").toInt()

    private val imageDebuggingEnabled: Boolean = cmdline.hasOption("i")

    private val ports = mutableListOf<Int>().apply {
        properties.getProperty("ports")?.split(' ')?.forEach {
            requireNotNull(it.toIntOrNull())
            this.add(it.toInt())
        }
        if (!emulated) this += 1118            // local port
    }

    private val rendersBeforeSave =
        properties.getProperty("renders")?.toIntOrNull() ?: cmdline.getOptionValue("r")?.toIntOrNull() ?: 1000

    private val leds = when (emulated) {
        false -> ledClass.primaryConstructor!!.call(
            numLEDs,
            pin,
            imageDebuggingEnabled,
            outputFileName,
            rendersBeforeSave
        )
        true -> EmulatedAnimatedLEDStrip(
            numLEDs,
            imageDebugging = imageDebuggingEnabled,
            fileName = outputFileName
        )
    }

    internal val animationHandler = AnimationHandler(leds)

    var testAnimation: AnimationData =
        AnimationData().animation(Animation.COLOR).color(CCBlue)

    /* Start and stop methods */

    fun start(): AnimatedLEDStripServer<T> {
        running = true
        Logger.debug { "Ports: $ports" }
        ports.forEach {
            SocketConnections.add(it, server = this).open()
        }
        if (cmdline.hasOption("T")) animationHandler.addAnimation(testAnimation)
        return this
    }

    fun stop() {
        leds.setStripColor(0)
        delayBlocking(500)
        leds.toggleRender()
        delayBlocking(2000)
        running = false
    }

    internal fun parseTextCommand(command: String) {
        Logger.info { command }
        val line = command.toUpperCase().split(" ")
        return when (line[0]) {
            "QUIT", "Q", "EXIT" -> {
                Logger.info { "Shutting down server" }
                stop()
            }
            "CLEAR" -> {
                animationHandler.addAnimation(AnimationData().animation(Animation.COLOR))
            }
            "SHOW" -> {
                if (line.size > 1) Logger.info {
                    "${line[1]}: ${animationHandler.continuousAnimations[line[1]]?.params ?: "NOT FOUND"}"
                }
                else Logger.info { "Running Animations: ${animationHandler.continuousAnimations.keys}" }
            }
            "END" -> {
                if (line.size > 1) {
                    if (line[1].toUpperCase() == "ALL") {
                        val animations = animationHandler.continuousAnimations
                        animations.forEach {
                            animationHandler.endAnimation(it.value)
                        }
                    } else for (i in 1 until line.size)
                        animationHandler.endAnimation(animationHandler.continuousAnimations[line[i]])
                } else Logger.warn { "Animation ID must be specified" }
            }
            else -> Logger.warn { "Not a valid command" }
        }
    }

    /* Helper methods */




}