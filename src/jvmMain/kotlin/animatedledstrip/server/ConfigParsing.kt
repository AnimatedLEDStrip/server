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

import animatedledstrip.leds.stripmanagement.StripInfo
import animatedledstrip.utils.ALSLogger
import animatedledstrip.utils.Logger
import co.touchlab.kermit.Severity
import org.apache.commons.cli.*
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.system.exitProcess

val options = Options().apply {
    addOption("f", "config", true, "Specify a config to load instead of /etc/leds/led.config")

    addLongOption("log-level", true, "Set the minimum severity level for logs that will be printed")
    addOption("p", "ports", true, "Add ports for clients to connect to")

    addLongOption("persist", false, "Persist animations across restarts")
    addLongOption("nopersist",
                  false,
                  "Don't persist animations across restarts (overrides --persist and persist=true in config)")

    addOption("n", "numleds", true, "Specify number of LEDs in the strip (default 240)")
    addOption("P", "pin", true, "Specify pin number the LED strip is connected to (default 12)")
    addLongOption("render-delay",
                  false,
                  "Specify the time in milliseconds between renders of the LED strip (default 10)")

    addLongOption("log-renders", false, "Enable strip color logging")
    addLongOption("log-file", true, "Specify the output file name for strip color logging")
    addLongOption("log-render-count", true, "Specify number of renders between saves to log file (default 1000)")

    addOption("1", "1d", false, "Support one-dimensional animations")
    addLongOption("no1d", false, "Don't support one-dimensional animations")
    addOption("2", "2d", false, "Support two-dimensional animations")
    addLongOption("no2d", false, "Don't support two-dimensional animations")
    addOption("3", "3d", false, "Support three-dimensional animations")
    addLongOption("no3d", false, "Don't support three-dimensional animations")

    addOption("h", "help", false, "Show help message")
}

fun Options.addLongOption(longOpt: String, hasArg: Boolean, description: String) {
    addOption(Option.builder().longOpt(longOpt).desc(description).hasArg(hasArg).build())
}

fun AnimatedLEDStripServer<*>.parseOptions(args: Array<String>) {
    val argParser: CommandLine = DefaultParser().parse(options, args)

    if (argParser.hasOption("h")) {
        HelpFormatter().printHelp("ledserver.jar", options)
        exitProcess(0)
    }

    val configFile: String =
        argParser.getOptionValue("f")
        ?: "/etc/leds/led.config"

    val configuration: Properties = Properties().apply {
        try {
            load(FileInputStream(configFile))
        } catch (e: FileNotFoundException) {
            Logger.w("Config Parser") { "File $configFile not found" }
        }
    }

    // Parse for logging severity
    val loggingSeverity = when (argParser.getOptionValue("log-level")?.toLowerCase()) {
        "verbose" -> Severity.Verbose
        "debug" -> Severity.Debug
        "info" -> Severity.Info
        "warn" -> Severity.Warn
        "error" -> Severity.Error
        else -> when (configuration.getProperty("log-level")?.toLowerCase()) {
            "verbose" -> Severity.Verbose
            "debug" -> Severity.Debug
            "info" -> Severity.Info
            "warn" -> Severity.Warn
            "error" -> Severity.Error
            else -> Severity.Warn
        }
    }
    ALSLogger.minSeverity = loggingSeverity

    argParser.getOptionValue("p")?.split(' ')?.forEach {
        when (val port = it.toIntOrNull()) {
            null -> Logger.e("Argument Parser") { "Could not parse port \"$it\"" }
            in ports -> Logger.w("Argument Parser") { "Port $port already added" }
            else -> ports.add(port)
        }
    }

    configuration.getProperty("ports")?.split(' ')?.forEach {
        when (val port = it.toIntOrNull()) {
            null -> Logger.e("Config Parser") { "Could not parse port \"$it\"" }
            in ports -> Logger.w("Config Parser") { "Port $port already added" }
            else -> ports.add(port)
        }
    }

    persistAnimations = !argParser.hasOption("nopersist") &&
                        (argParser.hasOption("persist") || configuration.getProperty("persist")?.toBoolean() ?: false)

    fun warnArgParseError(flag: String, value: String) {
        Logger.w("Argument Parser") { "Could not parse $flag \"$value\" from command line" }
    }

    fun warnConfigParseError(prop: String, value: String) {
        Logger.w("Config Parser") { "Could not parse $prop \"$value\" in config" }
    }

    val defaultStripInfo = StripInfo()

    // Checks for command line -n or --numLEDs,
    // then checks for numLEDs in config,
    // then checks for numleds in config
    val numLEDs: Int =
        when (val argLEDs = argParser.getOptionValue("n")) {
            null -> when (val configLEDs1 = configuration.getProperty("numLEDs")) {
                null -> when (val configLEDs2 = configuration.getProperty("numleds")) {
                    null -> defaultStripInfo.numLEDs
                    else -> when (val configLEDNum = configLEDs2.toIntOrNull()) {
                        null -> {
                            warnConfigParseError("numleds", configLEDs2)
                            defaultStripInfo.numLEDs
                        }
                        else -> configLEDNum
                    }
                }
                else -> when (val configLEDNum = configLEDs1.toIntOrNull()) {
                    null -> {
                        warnConfigParseError("numLEDs", configLEDs1)
                        defaultStripInfo.numLEDs
                    }
                    else -> configLEDNum
                }
            }
            else -> when (val argLEDNum = argLEDs.toIntOrNull()) {
                null -> {
                    warnArgParseError("-n/--numLEDs", argLEDs)
                    defaultStripInfo.numLEDs
                }
                else -> argLEDNum
            }
        }

    val pin: Int? =
        when (val argPin = argParser.getOptionValue("P")) {
            null -> when (val configPin = configuration.getProperty("pin")) {
                null -> defaultStripInfo.pin
                else -> when (val configPinNum = configPin.toIntOrNull()) {
                    null -> {
                        warnConfigParseError("pin", configPin)
                        defaultStripInfo.pin
                    }
                    else -> configPinNum
                }
            }
            else -> when (val argPinNum = argPin.toIntOrNull()) {
                null -> {
                    warnArgParseError("-P/--pin", argPin)
                    defaultStripInfo.pin
                }
                else -> argPinNum
            }
        }

    val renderDelay: Long =
        when (val argDelay = argParser.getOptionValue("render-delay")) {
            null -> when (val configDelay = configuration.getProperty("render-delay")) {
                null -> defaultStripInfo.renderDelay
                else -> when (val configDelayNum = configDelay.toLongOrNull()) {
                    null -> {
                        warnConfigParseError("render-delay", configDelay)
                        defaultStripInfo.renderDelay
                    }
                    else -> configDelayNum
                }
            }
            else -> when (val argDelayNum = argDelay.toLongOrNull()) {
                null -> {
                    warnArgParseError("--render-delay", argDelay)
                    defaultStripInfo.renderDelay
                }
                else -> argDelayNum
            }
        }

    val isRenderLoggingEnabled: Boolean =
        argParser.hasOption("log-renders") || (configuration.getProperty("log-renders")
                                                   ?.toBoolean() ?: defaultStripInfo.isRenderLoggingEnabled)

    val renderLogFile: String? =
        argParser.getOptionValue("log-file") ?: configuration.getProperty("log-file") ?: defaultStripInfo.renderLogFile

    val rendersBetweenLogSaves: Int =
        when (val argCount = argParser.getOptionValue("log-render-count")) {
            null -> when (val configCount = configuration.getProperty("log-render-count")) {
                null -> defaultStripInfo.rendersBetweenLogSaves
                else -> when (val configCountNum = configCount.toIntOrNull()) {
                    null -> {
                        warnConfigParseError("log-render-count", configCount)
                        defaultStripInfo.rendersBetweenLogSaves
                    }
                    else -> configCountNum
                }
            }
            else -> when (val argCountNum = argCount.toIntOrNull()) {
                null -> {
                    warnArgParseError("--log-render-count", argCount)
                    defaultStripInfo.rendersBetweenLogSaves
                }
                else -> argCountNum
            }
        }

    val is1DSupported = !argParser.hasOption("no1d") &&
                        (argParser.hasOption("1") || (configuration.getProperty("1d")?.toBoolean()
                                                      ?: defaultStripInfo.is1DSupported))

    val is2DSupported = !argParser.hasOption("no2d") &&
                        (argParser.hasOption("2") || (configuration.getProperty("2d")?.toBoolean()
                                                      ?: defaultStripInfo.is2DSupported))

    val is3DSupported = !argParser.hasOption("no3d") &&
                        (argParser.hasOption("3") || (configuration.getProperty("3d")?.toBoolean()
                                                      ?: defaultStripInfo.is3DSupported))

    val ledLocations = pixelLocations // TODO: Support file input

    stripInfo = StripInfo(numLEDs = numLEDs,
                          pin = pin,
                          renderDelay = renderDelay,
                          isRenderLoggingEnabled = isRenderLoggingEnabled,
                          renderLogFile = renderLogFile,
                          rendersBetweenLogSaves = rendersBetweenLogSaves,
                          is1DSupported = is1DSupported,
                          is2DSupported = is2DSupported,
                          is3DSupported = is3DSupported,
                          ledLocations = ledLocations)
}