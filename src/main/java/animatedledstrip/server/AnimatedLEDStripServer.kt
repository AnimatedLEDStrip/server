/*
 *  Copyright (c) 2018-2020 AnimatedLEDStrip
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

import animatedledstrip.animationutils.AnimationData
import animatedledstrip.animationutils.animation
import animatedledstrip.animationutils.color
import animatedledstrip.animationutils.findAnimationOrNull
import animatedledstrip.colors.ccpresets.CCBlue
import animatedledstrip.leds.AnimatedLEDStrip
import animatedledstrip.leds.StripInfo
import animatedledstrip.utils.DELIMITER
import animatedledstrip.utils.delayBlocking
import animatedledstrip.utils.endAnimation
import animatedledstrip.utils.jsonToAnimationData
import io.github.maxnz.parser.CommandParser
import io.github.maxnz.parser.action
import io.github.maxnz.parser.command
import io.github.maxnz.parser.commandGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.BindException
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
            ?: "/etc/leds/led.config"

    internal val properties =
        Properties().apply {
            try {
                load(FileInputStream(propertyFileName))
            } catch (e: FileNotFoundException) {
                Logger.warn("File $propertyFileName not found")
            }
        }

    /* Get port numbers */
    internal val ports =
        mutableListOf<Int>().apply {
            cmdline.getOptionValue("P")?.split(' ')?.forEach {
                requireNotNull(it.toIntOrNull()) { "Could not parse port \"$it\"" }
                this += it.toInt()
            }

            properties.getProperty("ports")?.split(' ')?.forEach {
                requireNotNull(it.toIntOrNull()) { "Could not parse port \"$it\"" }
                this += it.toInt()
            }
        }


    /* Arguments for creating the AnimatedLEDStrip instance */

    internal val persistAnimations: Boolean =
        !cmdline.hasOption("no-persist") &&
                (cmdline.hasOption("persist") ||
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

    init {
        stripInfo = StripInfo(
            numLEDs = numLEDs,
            pin = pin,
            imageDebugging = imageDebuggingEnabled,
            fileName = outputFileName,
            rendersBeforeSave = rendersBeforeSave,
            threadCount = threadCount
        )

        /*
          Check validity of constructor before calling so user can be notified about
          what changes need to be made rather than just throwing an IllegalArgumentException
          without any explanation
        */
        val ledConstructor = ledClass.primaryConstructor
        requireNotNull(ledConstructor)
        require(ledConstructor.parameters.size == 1)
        require(ledConstructor.parameters[0].type.classifier == StripInfo::class)

        leds = ledConstructor.call(
            stripInfo
        )

        leds.startAnimationCallback = {
            SocketConnections.sendAnimation(it)
            if (persistAnimations)
                GlobalScope.launch(Dispatchers.IO) {
                    FileOutputStream(".animations/${it.fileName}").apply {
                        write(it.json())
                        close()
                    }
                }
        }
        leds.endAnimationCallback = {
            SocketConnections.sendEndAnimation(it.endAnimation())
            if (File(".animations/${it.fileName}").exists())
                Files.delete(Paths.get(".animations/${it.fileName}"))
        }
        leds.newSectionCallback = {
            SocketConnections.sendData(it)
        }
    }

    val commandParser =
        CommandParser<AnimatedLEDStripServer<T>, SocketConnections.Connection?>(
            this
        )

    init {
        commandParser.apply {
            fun reply(str: String, client: SocketConnections.Connection?) {
                Logger.trace("Replying to client on port ${client?.port}: $str")
                client?.sendString(str + DELIMITER)
            }

            val replyAction: AnimatedLEDStripServer<T>.(SocketConnections.Connection?, String) -> Unit =
                { client, msg ->
                    if (client != null) reply(msg, client)
                    else println(msg)
                }

            badCommandAction = replyAction
            commandNeedsSubCommandAction = replyAction
            helpMessageAction = replyAction
            tooManyMatchingCommandsAction = replyAction

            command("quit") {
                description = "Stop the server"

                action { _, _ ->
                    Logger.warn("Shutting down server")
                    stop()
                }
            }

            commandGroup("logs") {
                description = "Modify logging settings"

                command("on") {
                    description = "Turn on logs to this port"

                    action { client, _ ->
                        client?.sendLogs = true
                        reply("Enabled logs to port ${client?.port}", client)
                    }
                }

                command("off") {
                    description = "Turn off logs to this port"

                    action { client, _ ->
                        client?.sendLogs = false
                        reply("Disabled logs to port ${client?.port}", client)
                    }
                }

                commandGroup("level") {
                    description = "Get or set the global logging level"
                    commandGroup("set") {
                        description = "Set the global logging level"

                        command("info") {
                            description = "Set global logging level to info"

                            action { _, _ ->
                                setLoggingLevel(Level.INFO)
                                Logger.info("Set logging level to info")
                            }
                        }

                        command("debug") {
                            description = "Set global logging level to debug"

                            action { _, _ ->
                                setLoggingLevel(Level.DEBUG)
                                Logger.debug("Set logging level to debug")
                            }
                        }

                        command("trace") {
                            description = "Set global logging level to trace"

                            action { _, _ ->
                                setLoggingLevel(Level.TRACE)
                                Logger.trace("Set logging level to trace")
                            }
                        }
                    }

                    command("get") {
                        description = "Get the global logging level"

                        action { client, _ ->
                            reply("Logging level is ${Logger.getLevel()}", client)
                        }
                    }
                }
            }

            commandGroup("strip") {
                command("clear") {
                    description = "Clear the LED strip (i.e. turn off all pixels)"

                    action { client, _ ->
                        leds.clear()
                        reply("Cleared strip", client)
                    }
                }

                command("info") {
                    description = "Get information about the server"

                    action { client, _ ->
                        client?.sendInfo()
                    }
                }
            }

            commandGroup("running") {
                description = "Get information about currently running animations"

                command("list") {
                    description = "Print a list of all running animations"

                    action { client, _ ->
                        reply("Running Animations: ${leds.runningAnimations.ids}", client)
                    }
                }

                command("info") {
                    description = "Print info about a running animation"
                    argHelpStr = "ID"

                    action { client, args ->
                        val anim = args.firstOrNull()
                            ?: run { reply("ID of animation required", client); return@action }
                        reply(
                            leds.runningAnimations.entries.toMap()[anim]?.data?.toHumanReadableString()
                                ?: "$anim: NOT FOUND",
                            client
                        )
                    }
                }
            }

//            command("end") {
//                description = "End one or more running animations"
//                argHelpStr = "ID..."
//
//                subCommand("all") {
//                    description = "End all running animations"
//
//                    action { client, _ ->
//                        leds.runningAnimations.ids.toList().forEach {
//                            reply("Ending animation $it", client)
//                            leds.endAnimation(it)
//                        }
//                    }
//                }
//
//                action { client, args ->
//                    args.forEach {
//                        reply("Ending animation $it", client)
//                        leds.endAnimation(it)
//                    }
//                }
//            }

            command("animation") {
                description = "Get information about a defined animation"
                argHelpStr = "NAME"

                action { client, args ->
                    val animName = args.firstOrNull() ?: run {
                        reply("Name of animation required", client)
                        return@action
                    }
                    val anim = findAnimationOrNull(animName) ?: run {
                        reply("Animation $animName not found", client)
                        return@action
                    }

                    client?.sendData(anim.info)
                }

            }

            commandGroup("connections") {
                description = "Manage the server's socket connections"

                command("list") {
                    description = "Show a list of all connections associated with this server"

                    action { client, _ ->
                        for (port in ports.sorted()) {
                            reply("Port $port: ${SocketConnections.connections[port]?.status()}", client)
                        }
                    }
                }

                command("add") {
                    description = "Add a new connection"
                    argHelpStr = "PORT"

                    action { client, args ->
                        when (val port = args.firstOrNull()) {
                            null -> reply("Port must be specified", client)
                            else -> when (val portNum = port.toIntOrNull()) {
                                null -> reply("""Invalid port: "$port"""", client)
                                else -> {
                                    Logger.debug("Adding port $portNum")
                                    when (SocketConnections.connections.containsKey(portNum)) {
                                        true -> reply("ERROR: Port $portNum already has a connection", client)
                                        false -> {
                                            SocketConnections.add(portNum, server = this)
                                            ports += portNum
                                            reply("Added port $portNum", client)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                command("start") {
                    description = "Start the server connection on the specified port"
                    argHelpStr = "PORT"

                    action { client, args ->
                        when (val port = args.firstOrNull()) {
                            null -> reply("Port must be specified", client)
                            else -> when (val portNum = port.toIntOrNull()) {
                                null -> reply("""Invalid port: "$port"""", client)
                                else -> {
                                    Logger.debug("Manually starting connection at port $portNum")
                                    reply("Starting port $portNum", client)
                                    SocketConnections.connections.getOrDefault(portNum, null)?.open()
                                        ?: reply("ERROR: No connection on port $port", client)
                                }
                            }
                        }
                    }
                }

                command("stop") {
                    description = "Start the server connection on the specified port"
                    argHelpStr = "PORT"

                    action { client, args ->
                        when (val port = args.firstOrNull()) {
                            null -> reply("Port must be specified", client)
                            else -> when (val portNum = port.toIntOrNull()) {
                                null -> reply("""Invalid port: "$port"""", client)
                                else -> {
                                    Logger.debug("Manually stopping connection at port $portNum")
                                    reply("Stopping port $portNum", client)
                                    SocketConnections.connections.getOrDefault(portNum, null)?.close()
                                        ?: reply("ERROR: No connection on port $portNum", client)
                                }
                            }
                        }
                    }
                }
            }

            Unit
        }
    }

    /**
     * The test animation
     */
    var testAnimation =
        AnimationData().animation("Color").color(CCBlue)

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
                            FileInputStream(it).apply {
                                val obj = readAllBytes().toString().jsonToAnimationData()
                                leds.startAnimation(obj)
                                close()
                            }
                        } catch (e: FileNotFoundException) {
                        }

                    }
                }
            }
        }

        running = true
        ports.forEach {
            try {
                SocketConnections.add(it, server = this)
                SocketConnections.connections[it]?.open()
            } catch (e: BindException) {
                Logger.error("Could not bind to port $it")
            }
        }
        if (cmdline.hasOption("T")) leds.startAnimation(testAnimation)
        return this
    }

    /** Stop the server */
    fun stop() {
        leds.wholeStrip.setProlongedStripColor(0)
        delayBlocking(500)
        leds.toggleRender()
        delayBlocking(2000)
        running = false
    }

    internal fun parseTextCommand(command: String, client: SocketConnections.Connection?) =
        commandParser.parseCommand(command.removePrefix("CMD :"), client)

    private fun setLoggingLevel(level: Level) {
        Configurator.currentConfig().level(level).activate()
    }

}