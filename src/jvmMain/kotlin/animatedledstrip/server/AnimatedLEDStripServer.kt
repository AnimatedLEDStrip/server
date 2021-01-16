/*
 * Copyright (c) 2018-2021 AnimatedLEDStrip
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package animatedledstrip.server

import animatedledstrip.animations.findAnimationOrNull
import animatedledstrip.communication.DELIMITER
import animatedledstrip.communication.decodeJson
import animatedledstrip.leds.*
import animatedledstrip.leds.animationmanagement.*
import animatedledstrip.leds.colormanagement.clear
import animatedledstrip.leds.colormanagement.currentStripColor
import animatedledstrip.leds.locationmanagement.Location
import animatedledstrip.leds.stripmanagement.LEDStrip
import animatedledstrip.leds.stripmanagement.NativeLEDStrip
import animatedledstrip.leds.stripmanagement.StripInfo
import animatedledstrip.utils.ALSLogger
import animatedledstrip.utils.Logger
import co.touchlab.kermit.Severity
import io.github.maxnz.parser.CommandParser
import io.github.maxnz.parser.action
import io.github.maxnz.parser.command
import io.github.maxnz.parser.commandGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.BindException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

actual class AnimatedLEDStripServer<T : NativeLEDStrip> actual constructor(
    args: Array<String>,
    ledClass: KClass<T>,
    internal val pixelLocations: List<Location>?,
) {

    /** Is the server running */
    internal actual var running: Boolean = false

    internal actual val ports: MutableList<Int> = mutableListOf()

    /* Arguments for creating the AnimatedLEDStrip instance */

    internal actual var persistAnimations: Boolean by Delegates.notNull()

    /* Create strip info */

    actual var stripInfo: StripInfo by Delegates.notNull()

    init {
        parseOptions(args)
    }

    /* Create strip instance */

    actual val leds: LEDStrip

    init {
        /*
          Check validity of constructor before calling so user can be notified about
          what changes need to be made rather than just throwing an IllegalArgumentException
          without any explanation
        */
        val ledConstructor = ledClass.primaryConstructor
        requireNotNull(ledConstructor) { "LED class must have primary constructor" }
        require(ledConstructor.parameters.size == 1) { "LED class primary constructor must have only one argument" }
        require(ledConstructor.parameters[0].type.classifier == StripInfo::class) {
            "LED class primary constructor argument must be of type StripInfo"
        }

        leds = LEDStrip(stripInfo, ledConstructor.call(stripInfo))

        leds.startAnimationCallback = {
            SocketConnections.sendData(it)
            if (persistAnimations)
                GlobalScope.launch(Dispatchers.IO) {
                    FileOutputStream(".animations/${it.fileName}").apply {
                        write(it.json())
                        close()
                    }
                }
        }
        leds.endAnimationCallback = {
            SocketConnections.sendData(it.endAnimation())
            if (File(".animations/${it.fileName}").exists())
                Files.delete(Paths.get(".animations/${it.fileName}"))
        }
        leds.newSectionCallback = {
            SocketConnections.sendData(it)
        }
    }

    val commandParser =
        CommandParser<AnimatedLEDStripServer<T>, SocketConnections.Connection?>(this)

    init {
        commandParser.apply {
            fun reply(str: String, client: SocketConnections.Connection?) {
                Logger.v("Command Parser") { "Replying to client on port ${client?.port}: $str" }
                client?.sendString(str + DELIMITER)
            }

            val replyAction: AnimatedLEDStripServer<T>.(SocketConnections.Connection?, String) -> Unit =
                { client, msg ->
                    reply(msg, client)
                }

            badCommandAction = replyAction
            commandNeedsSubCommandAction = replyAction
            helpMessageAction = replyAction
            tooManyMatchingCommandsAction = replyAction

            command("quit") {
                description = "Stop the server"

                action { _, _ ->
                    Logger.w("Server") { "Shutting down server" }
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

                        command("error") {
                            description = "Set global logging level to error"

                            action { _, _ ->
                                ALSLogger.minSeverity = Severity.Error
                                Logger.e { "Set logging level to error" }
                            }
                        }

                        command("warn") {
                            description = "Set global logging level to warn"

                            action { _, _ ->
                                ALSLogger.minSeverity = Severity.Warn
                                Logger.w { "Set logging level to warn" }
                            }
                        }

                        command("info") {
                            description = "Set global logging level to info"

                            action { _, _ ->
                                ALSLogger.minSeverity = Severity.Info
                                Logger.i { "Set logging level to info" }
                            }
                        }

                        command("debug") {
                            description = "Set global logging level to debug"

                            action { _, _ ->
                                ALSLogger.minSeverity = Severity.Debug
                                Logger.d { "Set logging level to debug" }
                            }
                        }

                        command("verbose") {
                            description = "Set global logging level to verbose"

                            action { _, _ ->
                                ALSLogger.minSeverity = Severity.Verbose
                                Logger.v { "Set logging level to verbose" }
                            }
                        }
                    }

                    command("get") {
                        description = "Get the global logging level"

                        action { client, _ ->
                            reply("Logging level is ${ALSLogger.minSeverity}", client)
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

                command("color") {
                    description = "Get strip color"

                    action { client, _ ->
                        client?.sendData(leds.currentStripColor())
                    }
                }
            }

            commandGroup("running") {
                description = "Get information about currently running animations"

                command("list") {
                    description = "Return all running animations"

                    action { client, _ ->
                        for (anim in leds.animationManager.runningAnimations.values)
                            client?.sendRequestedData(anim.params)
                    }
                }

                command("ids") {
                    description = "Return a list of all running animation ids"

                    action { client, _ ->
                        reply("Running Animations: ${leds.animationManager.runningAnimations.keys}", client)
                    }
                }

                command("info") {
                    description = "Print info about a running animation"
                    argHelpStr = "ID [ID...]"

                    action { client, args ->
                        if (args.isEmpty()) reply("ID of animation required", client)
                        args.forEach {
                            reply(
                                leds.animationManager.runningAnimations.toMap()[it]?.params?.toString()
                                ?: "$it: NOT FOUND",
                                client,
                            )
                        }
                    }
                }

                command("end") {
                    description = "End a running animation"
                    argHelpStr = "ID [ID...]"

                    action { client, args ->
                        if (args.isEmpty()) reply("ID of animation required", client)
                        args.forEach {
                            reply("Ending animation $it", client)
                            leds.animationManager.endAnimation(it)
                        }
                    }
                }
            }

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
                                    Logger.d { "Adding port $portNum" }
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
                                    Logger.d { "Manually starting connection at port $portNum" }
                                    reply("Starting port $portNum", client)
                                    SocketConnections.connections.getOrDefault(portNum, null)?.open()
                                    ?: reply("ERROR: No connection on port $port", client)
                                }
                            }
                        }
                    }
                }

                command("stop") {
                    description = "Stop the server connection on the specified port"
                    argHelpStr = "PORT"

                    action { client, args ->
                        when (val port = args.firstOrNull()) {
                            null -> reply("Port must be specified", client)
                            else -> when (val portNum = port.toIntOrNull()) {
                                null -> reply("""Invalid port: "$port"""", client)
                                else -> {
                                    Logger.d { "Manually stopping connection at port $portNum" }
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
                                val obj = readAllBytes().toString().decodeJson() as AnimationToRunParams
                                leds.animationManager.startAnimation(obj)
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
                Logger.e { "Could not bind to port $it" }
            }
        }
        return this
    }

    /** Stop the server */
    fun stop() {
        if (!running) return
        leds.clear()
        Thread.sleep(500)
//        leds.toggleRender()
        Thread.sleep(2000)
        running = false
    }

    internal fun parseTextCommand(command: String, client: SocketConnections.Connection?) =
        commandParser.parseCommand(command, client)
}
