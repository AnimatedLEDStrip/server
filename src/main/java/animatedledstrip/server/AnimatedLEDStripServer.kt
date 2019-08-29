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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class AnimatedLEDStripServer <T: AnimatedLEDStrip> (
    args: Array<String>,
    ledClass: KClass<T>
) {
    internal var running = false

    private val options = Options().apply {
        addOption("d", "Enable debugging")
        addOption("t", "Enable trace debugging")
        addOption("v", "Enable verbose log statements")
        addOption("q", "Disable log outputs")
        addOption("E", "Emulate LED strip but do NOT launch emulator")
        addOption("f", true, "Specify properties file")
        addOption("i", "Enable image debugging")
        addOption("T", "Run test")
    }

    private val cmdline = DefaultParser().parse(options, args)

    var defaultPropertyFileName = "led.config"

    var defaultOutputFileName: String? = null

    private val properties = Properties().apply {
        try {
            load(FileInputStream(cmdline.getOptionValue("f") ?: defaultPropertyFileName))
        } catch (e: FileNotFoundException) {
            Logger.warn("File ${cmdline.getOptionValue("f") ?: defaultPropertyFileName} not found")
        }
    }

    val ports = mutableListOf<Int>().apply {
        Logger.debug(properties.getProperty("ports"))
        properties.getProperty("ports")?.split(' ')?.forEach {
            Logger.debug(it)
            requireNotNull(it.toIntOrNull())
            this.add(it.toInt())
        }
    }

    private val leds = when (cmdline.hasOption("e") || cmdline.hasOption("E")) {
        false -> ledClass.primaryConstructor!!.call(
            properties.getProperty("numLEDs", "240").toInt(),
            properties.getProperty("pin", "12").toInt(),
            cmdline.hasOption("i"),
            cmdline.getOptionValue("o") ?: defaultOutputFileName
        )
        true -> EmulatedAnimatedLEDStrip(
            properties.getProperty("numLEDs", "240").toInt(),
            imageDebugging = cmdline.hasOption("i"),
            fileName = cmdline.getOptionValue("o") ?: defaultOutputFileName
        )
    }

    internal val animationHandler = AnimationHandler(leds)

    var testAnimation: AnimationData =
        AnimationData().animation(Animation.COLOR).color(CCBlue)

    init {
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
    }


    fun start(): AnimatedLEDStripServer<T> {
        running = true
        startLocalTerminalReader()
        Logger.debug("Ports = $ports")
        ports.forEach {
            SocketConnections.add(it, server = this).open()
        }
        if (cmdline.hasOption("T")) animationHandler.addAnimation(testAnimation)
        return this
    }

    fun waitUntilStop() {
        while (running) {
            delayBlocking(1)
        }
    }

    fun stop() {
        leds.setStripColor(0)
        delayBlocking(500)
        leds.toggleRender()
        delayBlocking(2000)
        running = false
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    val localThread = newSingleThreadContext("Local Terminal")

    private fun startLocalTerminalReader() {
        GlobalScope.launch(localThread) {
            while (this@AnimatedLEDStripServer.running) {
                Logger.trace("Local terminal waiting for input")
                val strIn = readLine()
                Logger.trace("Read line")
                if (strIn?.toUpperCase() == "Q") {
                    Logger.trace("'Q' received, shutting down server")
                    stop()
                }
            }
        }
    }
}