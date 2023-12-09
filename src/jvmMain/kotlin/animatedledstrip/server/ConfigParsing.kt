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

import animatedledstrip.leds.locationmanagement.Location
import animatedledstrip.leds.stripmanagement.StripInfo
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.apache.commons.cli.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.system.exitProcess

//val ArgumentParserLogger = Logger.withTag("Argument Parser")
//
//val ConfigParserLogger = Logger.withTag("Config Parser")
//
//val LocationsParserLogger = Logger.withTag("LED Locations File Parser")


val options = Options().apply {
    addOption("f", "config", true, "Specify a config to load instead of /etc/leds/led.config")

    addLongOption("log-level", true, "Set the minimum severity level for logs that will be printed")

    addLongOption("persist", false, "Persist animations across restarts")
    addLongOption(
        "nopersist",
        false,
        "Don't persist animations across restarts (overrides --persist and persist=true in config)"
    )
    addLongOption("anim-dir", true, "Directory to store saved and persistent animations")

    addOption("n", "numleds", true, "Specify number of LEDs in the strip (default 240)")
    addOption("l", "locations-file", true, "Specify a file with the locations of all pixels")
    addOption("p", "pin", true, "Specify pin number the LED strip is connected to (default 12)")
    addLongOption(
        "render-delay",
        true,
        "Specify the time in milliseconds between renders of the LED strip (default 10)"
    )

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
//            /*ConfigParser*/Logger.w("File $configFile not found")
        }
    }

    // Parse for logging severity
    val loggingSeverity = when (val argL = argParser.getOptionValue("log-level")?.lowercase(Locale.getDefault())) {
        "verbose" -> Severity.Verbose
        "debug" -> Severity.Debug
        "info" -> Severity.Info
        "warn" -> Severity.Warn
        "error" -> Severity.Error
        else -> {
//            if (argL != null)
//            /*ArgumentParser*/ Logger.w("Could not parse --log-level \"$argL\" from command line (must be one of verbose, debug, info, warn, error)")
            when (val confL = configuration.getProperty("log-level")?.lowercase(Locale.getDefault())) {
                "verbose" -> Severity.Verbose
                "debug" -> Severity.Debug
                "info" -> Severity.Info
                "warn" -> Severity.Warn
                "error" -> Severity.Error
                else -> {
//                    if (confL != null)
//                    /*ConfigParser*/ Logger.w("Could not parse log-level \"$confL\" in config (must be one of verbose, debug, info, warn, error)")
                    Severity.Warn
                }
            }
        }
    }
//    Logger.setMinSeverity(loggingSeverity)

    persistAnimations = !argParser.hasOption("nopersist") &&
                        (argParser.hasOption("persist") || configuration.getProperty("persist")
                            ?.toBoolean() ?: persistAnimations)

    storedAnimationsDirectory = argParser.getOptionValue("anim-dir")
                                ?: configuration.getProperty("anim-dir")
                                ?: storedAnimationsDirectory

    fun warnArgParseError(flag: String, value: String) {
//        /*ArgumentParser*/Logger.w("Could not parse $flag \"$value\" from command line")
    }

    fun warnConfigParseError(prop: String, value: String) {
//        /*ConfigParser*/Logger.w("Could not parse $prop \"$value\" in config")
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
                    warnArgParseError("-n/--numleds", argLEDs)
                    defaultStripInfo.numLEDs
                }
                else -> argLEDNum
            }
        }

    val pin: Int? =
        when (val argPin = argParser.getOptionValue("p")) {
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
                    warnArgParseError("-p/--pin", argPin)
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
                        (argParser.hasOption("1d") || (configuration.getProperty("1d")?.toBoolean()
                                                       ?: defaultStripInfo.is1DSupported))

    val is2DSupported = !argParser.hasOption("no2d") &&
                        (argParser.hasOption("2d") || (configuration.getProperty("2d")?.toBoolean()
                                                       ?: defaultStripInfo.is2DSupported))

    val is3DSupported = !argParser.hasOption("no3d") &&
                        (argParser.hasOption("3d") || (configuration.getProperty("3d")?.toBoolean()
                                                       ?: defaultStripInfo.is3DSupported))

    val locationsFileName: String? =
        argParser.getOptionValue("locations-file") ?: configuration.getProperty("locations-file")

    val locationsFile: File? = if (locationsFileName != null) File(locationsFileName) else null

    val ledLocations = when {
        locationsFile?.exists() == false -> {
            /*LocationsParser*/Logger.w("File $locationsFileName does not exist")
            null
        }
        locationsFile != null -> {
            var discard = false
            val locations: MutableList<Location> = mutableListOf()
            for ((i, l) in csvReader().readAll(locationsFile).withIndex()) {
                val x = l.getOrNull(0)?.toDoubleOrNull() ?: run {
                    /*LocationsParser*/Logger.e("Could not parse first column of row ${i + 1} properly, aborting and using defaults")
                    discard = true
                    null
                }
                val y = l.getOrNull(1)?.toDoubleOrNull() ?: run {
                    /*LocationsParser*/Logger.e("Could not parse second column of row ${i + 1} properly, aborting and using defaults")
                    discard = true
                    null
                }
                val z = l.getOrNull(2)?.toDoubleOrNull() ?: run {
                    /*LocationsParser*/Logger.e("Could not parse third column of row ${i + 1} properly, aborting and using defaults")
                    discard = true
                    null
                }
                if (x != null && y != null && z != null) locations.add(Location(x, y, z))
                else break
            }
            if (discard) pixelLocations else locations
        }
        else -> pixelLocations
    }

    stripInfo = StripInfo(
        numLEDs = numLEDs,
        pin = pin,
        renderDelay = renderDelay,
        isRenderLoggingEnabled = isRenderLoggingEnabled,
        renderLogFile = renderLogFile,
        rendersBetweenLogSaves = rendersBetweenLogSaves,
        is1DSupported = is1DSupported,
        is2DSupported = is2DSupported,
        is3DSupported = is3DSupported,
        ledLocations = ledLocations
    )
}
