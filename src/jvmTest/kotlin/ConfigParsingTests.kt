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

package animatedledstrip.test

import animatedledstrip.leds.emulation.EmulatedWS281x
import animatedledstrip.leds.locationmanagement.Location
import animatedledstrip.server.AnimatedLEDStripServer
import animatedledstrip.utils.ALSLogger
import animatedledstrip.utils.TestLogger
import co.touchlab.kermit.Severity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ConfigParsingTests : StringSpec(
    {
        afterEach {
            ALSLogger.minSeverity = Severity.Warn
        }

        fun configPath(directory: String, file: String): String =
            "src/jvmTest/resources/configs/$directory/$file"

        "config file not found".config(enabled = false) {
            TestLogger.startLogCapture()
            val server = newTestServer("-f", "src/jvmTest/resources/configs/notaconfig.notaconfig")
            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
                                                         "File src/jvmTest/resources/configs/notaconfig.notaconfig not found",
                                                         "Config Parser"))
            TestLogger.stopLogCapture()
            server.leds.renderer.close()
        }

        "command line log level verbose".config(enabled = false) {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer("--log-level", "verbose")
            ALSLogger.minSeverity shouldBe Severity.Verbose
        }

        "config file log level verbose".config(enabled = false) {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer("-f", configPath("log-level", "log-verbose.config"))
            ALSLogger.minSeverity shouldBe Severity.Verbose
        }

        "command line log level debug".config(enabled = false) {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer("--log-level", "debug")
            ALSLogger.minSeverity shouldBe Severity.Debug
        }

        "config file log level debug".config(enabled = false) {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer("-f", configPath("log-level", "log-debug.config"))
            ALSLogger.minSeverity shouldBe Severity.Debug
        }

        "command line log level info".config(enabled = false) {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer("--log-level", "info")
            ALSLogger.minSeverity shouldBe Severity.Info
        }

        "config file log level info".config(enabled = false) {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer("-f", configPath("log-level", "log-info.config"))
            ALSLogger.minSeverity shouldBe Severity.Info
        }

        "command line log level warn".config(enabled = false) {
            ALSLogger.minSeverity = Severity.Error
            ALSLogger.minSeverity shouldBe Severity.Error
            newTestServer("--log-level", "warn")
            ALSLogger.minSeverity shouldBe Severity.Warn
        }

        "config file log level warn".config(enabled = false) {
            ALSLogger.minSeverity = Severity.Error
            ALSLogger.minSeverity shouldBe Severity.Error
            newTestServer("-f", configPath("log-level", "log-warn.config"))
            ALSLogger.minSeverity shouldBe Severity.Warn
        }

        "command line log level error".config(enabled = false) {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer("--log-level", "error")
            ALSLogger.minSeverity shouldBe Severity.Error
        }

        "config file log level error".config(enabled = false) {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer("-f", configPath("log-level", "log-error.config"))
            ALSLogger.minSeverity shouldBe Severity.Error
        }

        "command line log level parse error".config(enabled = false) {
            ALSLogger.minSeverity = Severity.Error
            ALSLogger.minSeverity shouldBe Severity.Error
            TestLogger.startLogCapture()
            newTestServer("--log-level", "bad")
            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
                                                         "Could not parse --log-level \"bad\" from command line (must be one of verbose, debug, info, warn, error)",
                                                         "Argument Parser"))
            TestLogger.stopLogCapture()
            ALSLogger.minSeverity shouldBe Severity.Warn
        }

        "config file log level parse error".config(enabled = false) {
            ALSLogger.minSeverity = Severity.Error
            ALSLogger.minSeverity shouldBe Severity.Error
            TestLogger.startLogCapture()
            newTestServer("-f", configPath("log-level", "log-bad.config"))
            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
                                                         "Could not parse log-level \"bad\" in config (must be one of verbose, debug, info, warn, error)",
                                                         "Config Parser"))
            TestLogger.stopLogCapture()
            ALSLogger.minSeverity shouldBe Severity.Warn
        }

        "command line persist" {
            newTestServer("--persist").persistAnimations.shouldBeTrue()
        }

        "config file persist" {
            newTestServer("-f", configPath("persist", "persist.config")).persistAnimations.shouldBeTrue()
        }

        "command line nopersist override" {
            newTestServer("--persist", "--nopersist").persistAnimations.shouldBeFalse()
            newTestServer("-f",
                          configPath("persist", "persist.config"),
                          "--nopersist").persistAnimations.shouldBeFalse()
        }

        "command line anim-dir" {
            newTestServer("--anim-dir", "src/jvmTest/resources/persist").storedAnimationsDirectory shouldBe
                    "src/jvmTest/resources/persist/.animations"
            newTestServer().storedAnimationsDirectory shouldBe "./.animations"
        }

        "config file anim-dir" {
            newTestServer("-f", configPath("persist", "anim-dir.config")).storedAnimationsDirectory shouldBe
                    "src/jvmTest/resources/persist/.animations"
        }

        "command line numLEDs" {
            AnimatedLEDStripServer(arrayOf("-n", "10"), EmulatedWS281x::class).stripInfo.numLEDs shouldBe 10
            AnimatedLEDStripServer(arrayOf("--numleds", "15"), EmulatedWS281x::class).stripInfo.numLEDs shouldBe 15
//            TestLogger.startLogCapture()
//            shouldThrow<IllegalArgumentException> {
//                AnimatedLEDStripServer(arrayOf("--numleds", "r"), EmulatedWS281x::class)
//            }
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
//                                                         "Could not parse -n/--numleds \"r\" from command line",
//                                                         "Argument Parser"))
//            TestLogger.stopLogCapture()
        }

        "config file numLEDs" {
            AnimatedLEDStripServer(arrayOf("-f", configPath("num-leds", "numLEDs.config")),
                                   EmulatedWS281x::class).stripInfo.numLEDs shouldBe 50
            AnimatedLEDStripServer(arrayOf("-f", configPath("num-leds", "numleds-lowercase.config")),
                                   EmulatedWS281x::class).stripInfo.numLEDs shouldBe 30
//            TestLogger.startLogCapture()
//            shouldThrow<IllegalArgumentException> {
//                AnimatedLEDStripServer(arrayOf("-f", configPath("num-leds", "numLEDs-bad.config")),
//                                       EmulatedWS281x::class)
//            }
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
//                                                         "Could not parse numLEDs \"t\" in config",
//                                                         "Config Parser"))
//            TestLogger.stopLogCapture()
        }

        "command line pin" {
            newTestServer("-p", "12").stripInfo.pin shouldBe 12
            newTestServer("--pin", "15").stripInfo.pin shouldBe 15
//            TestLogger.startLogCapture()
//            newTestServer("-p", "x").stripInfo.pin.shouldBeNull()
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
//                                                         "Could not parse -p/--pin \"x\" from command line",
//                                                         "Argument Parser"))
//            TestLogger.stopLogCapture()
        }

        "config file pin" {
            newTestServer("-f", configPath("pin", "pin.config")).stripInfo.pin shouldBe 10
//            TestLogger.startLogCapture()
//            newTestServer("-f", configPath("pin", "pin-bad.config")).stripInfo.pin.shouldBeNull()
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
//                                                         "Could not parse pin \"z\" in config",
//                                                         "Config Parser"))
//            TestLogger.stopLogCapture()
        }

        "command line render-delay" {
            newTestServer("--render-delay", "25").stripInfo.renderDelay shouldBe 25
//            TestLogger.startLogCapture()
//            newTestServer("--render-delay", "q").stripInfo.renderDelay shouldBe 10
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
//                                                         "Could not parse --render-delay \"q\" from command line",
//                                                         "Argument Parser"))
//            TestLogger.stopLogCapture()
        }

        "config file render-delay" {
            newTestServer("-f", configPath("render-delay", "render-delay.config")).stripInfo.renderDelay shouldBe 50
//            TestLogger.startLogCapture()
//            newTestServer("-f", configPath("render-delay", "render-delay-bad.config")).stripInfo.renderDelay shouldBe 10
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
//                                                         "Could not parse render-delay \"2cwas\" in config",
//                                                         "Config Parser"))
//            TestLogger.stopLogCapture()
        }

        "command line log-renders" {
            newTestServer("--log-renders").stripInfo.isRenderLoggingEnabled.shouldBeTrue()
            newTestServer().stripInfo.isRenderLoggingEnabled.shouldBeFalse()
        }

        "config file log-renders" {
            newTestServer("-f",
                          configPath("log-renders",
                                     "log-renders-true.config")).stripInfo.isRenderLoggingEnabled.shouldBeTrue()
            newTestServer("-f",
                          configPath("log-renders",
                                     "log-renders-false.config")).stripInfo.isRenderLoggingEnabled.shouldBeFalse()
            newTestServer("-f",
                          configPath("log-renders",
                                     "log-renders-bad.config")).stripInfo.isRenderLoggingEnabled.shouldBeFalse()
        }

        "log-file" {
            newTestServer("--log-file", "testfile.test").stripInfo.renderLogFile shouldBe "testfile.test"
            newTestServer("-f",
                          configPath("log-file", "log-file.config")).stripInfo.renderLogFile shouldBe "hsdfgs23vw1"
            newTestServer().stripInfo.renderLogFile.shouldBeNull()
        }

        "command line log-render-count" {
            newTestServer("--log-render-count", "42").stripInfo.rendersBetweenLogSaves shouldBe 42
//            TestLogger.startLogCapture()
//            newTestServer("--log-render-count", "gbdgs").stripInfo.rendersBetweenLogSaves shouldBe 1000
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
//                                                         "Could not parse --log-render-count \"gbdgs\" from command line",
//                                                         "Argument Parser"))
//            TestLogger.stopLogCapture()
        }

        "config file log-render-count" {
            newTestServer("-f",
                          configPath("log-render-count",
                                     "log-render-count.config")).stripInfo.rendersBetweenLogSaves shouldBe 504
//            TestLogger.startLogCapture()
//            newTestServer("-f",
//                          configPath("log-render-count",
//                                     "log-render-count-bad.config")).stripInfo.rendersBetweenLogSaves shouldBe 1000
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
//                                                         "Could not parse log-render-count \"gnwsr\" in config",
//                                                         "Config Parser"))
//            TestLogger.stopLogCapture()
        }

        "command line 1d" {
            newTestServer("-1").stripInfo.is1DSupported.shouldBeTrue()
            newTestServer("--1d").stripInfo.is1DSupported.shouldBeTrue()
        }

        "config file 1d" {
            newTestServer("-f", configPath("dimensions", "1d-supported.config")).stripInfo.is1DSupported.shouldBeTrue()
            newTestServer("-f",
                          configPath("dimensions", "1d-not-supported.config")).stripInfo.is1DSupported.shouldBeFalse()
        }

        "command line 1d override" {
            newTestServer("-1", "--no1d").stripInfo.is1DSupported.shouldBeFalse()
            newTestServer("-f",
                          configPath("dimensions", "1d-supported.config"),
                          "--no1d").stripInfo.is1DSupported.shouldBeFalse()
            newTestServer("-f",
                          configPath("dimensions", "1d-not-supported.config"),
                          "-1").stripInfo.is1DSupported.shouldBeTrue()
        }

        "command line 2d" {
            newTestServer("-2").stripInfo.is2DSupported.shouldBeTrue()
            newTestServer("--2d").stripInfo.is2DSupported.shouldBeTrue()
        }

        "config file 2d" {
            newTestServer("-f", configPath("dimensions", "2d-supported.config")).stripInfo.is2DSupported.shouldBeTrue()
            newTestServer("-f",
                          configPath("dimensions", "2d-not-supported.config")).stripInfo.is2DSupported.shouldBeFalse()
        }

        "command line 2d override" {
            newTestServer("-2", "--no2d").stripInfo.is2DSupported.shouldBeFalse()
            newTestServer("-f",
                          configPath("dimensions", "2d-supported.config"),
                          "--no2d").stripInfo.is2DSupported.shouldBeFalse()
            newTestServer("-f",
                          configPath("dimensions", "2d-not-supported.config"),
                          "-2").stripInfo.is2DSupported.shouldBeTrue()
        }

        "command line 3d" {
            newTestServer("-3").stripInfo.is3DSupported.shouldBeTrue()
            newTestServer("--3d").stripInfo.is3DSupported.shouldBeTrue()
        }

        "config file 3d" {
            newTestServer("-f", configPath("dimensions", "3d-supported.config")).stripInfo.is3DSupported.shouldBeTrue()
            newTestServer("-f",
                          configPath("dimensions", "3d-not-supported.config")).stripInfo.is3DSupported.shouldBeFalse()
        }

        "command line 3d override" {
            newTestServer("-3", "--no3d").stripInfo.is3DSupported.shouldBeFalse()
            newTestServer("-f",
                          configPath("dimensions", "3d-supported.config"),
                          "--no3d").stripInfo.is3DSupported.shouldBeFalse()
            newTestServer("-f",
                          configPath("dimensions", "3d-not-supported.config"),
                          "-3").stripInfo.is3DSupported.shouldBeTrue()
        }

        "command line locations-file" {
            newTestServer("-l",
                          "src/jvmTest/resources/locations/test1.csv").stripInfo.ledLocations.shouldContainExactly(
                Location(63.379, 52.074, 23.217),
                Location(38.28, 48.749, 26.131),
                Location(38.224, 99.991, 88.812),
                Location(59.044, 38.796, 89.937),
                Location(25.143, 24.141, 46.963),
                Location(87.758, 24.399, 56.33),
                Location(62.481, 96.821, 93.497),
                Location(30.69, 24.602, 99.471),
                Location(22.156, 17.049, 31.57),
                Location(96.502, 21.982, 77.334),
            )
            newTestServer("--locations-file",
                          "src/jvmTest/resources/locations/test2.csv").stripInfo.ledLocations.shouldContainExactly(
                Location(25.546, 2.922, 58.655),
                Location(33.142, 87.153, 77.244),
                Location(43.71, 95.472, 75.697),
                Location(16.485, 6.102, 95.918),
                Location(31.815, 94.161, 10.102),
                Location(31.842, 6.955, 49.322),
                Location(83.502, 27.646, 57.089),
                Location(54.27, 41.257, 73.514),
                Location(86.418, 64.481, 56.281),
                Location(49.262, 48.842, 96.996),
            )
            newTestServer().stripInfo.ledLocations.shouldBeNull()
        }

        "config file locations-file" {
            newTestServer("-f", configPath("locations-file", "locations-file.config")).stripInfo.ledLocations.shouldContainExactly(
                Location(15.869,78.812,85.113),
                Location(35.653,15.497,72.709),
                Location(94.686,2.497,70.808),
                Location(35.047,74.252,24.252),
                Location(57.564,19.283,95.993),
                Location(52.184,24.498,19.285),
                Location(66.901,8.861,82.898),
                Location(48.413,30.385,12.076),
                Location(2.003,37.389,22.114),
                Location(19.245,86.439,27.146),
            )
        }

        "locations file missing".config(enabled = false) {
            TestLogger.startLogCapture()
            newTestServer("-l", "src/jvmTest/resources/locations/test-missing.csv")
            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn,
                                                         "File src/jvmTest/resources/locations/test-missing.csv does not exist",
                                                         "LED Locations File Parser"))
            TestLogger.stopLogCapture()
        }

        "locations file parse error".config(enabled = false) {
            TestLogger.startLogCapture()
            newTestServer("-l", "src/jvmTest/resources/locations/bad-test1.csv")
            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Error,
                                                         "Could not parse first column of row 4 properly, aborting and using defaults",
                                                         "LED Locations File Parser"))
            TestLogger.stopLogCapture()

            TestLogger.startLogCapture()
            newTestServer("-l", "src/jvmTest/resources/locations/bad-test2.csv")
            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Error,
                                                         "Could not parse second column of row 7 properly, aborting and using defaults",
                                                         "LED Locations File Parser"))
            TestLogger.stopLogCapture()

            TestLogger.startLogCapture()
            newTestServer("-l", "src/jvmTest/resources/locations/bad-test3.csv")
            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Error,
                                                         "Could not parse third column of row 1 properly, aborting and using defaults",
                                                         "LED Locations File Parser"))
            TestLogger.stopLogCapture()
        }
    }
)
