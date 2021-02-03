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

import animatedledstrip.utils.ALSLogger
import animatedledstrip.utils.TestLogger
import co.touchlab.kermit.Severity
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class ConfigParsingTests : StringSpec(
    {
        afterEach {
            ALSLogger.minSeverity = Severity.Warn
        }

        "config file not found" {
            TestLogger.startLogCapture()
            val server = newTestServer(arrayOf("-f", "src/jvmTest/resources/notaconfig.notaconfig"))
            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn, "File src/jvmTest/resources/notaconfig.notaconfig not found", "Config Parser"))
            TestLogger.stopLogCapture()
            server.leds.renderer.close()
        }

        "command line log level verbose" {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer(arrayOf("--log-level", "verbose"))
            ALSLogger.minSeverity shouldBe Severity.Verbose
        }

        "config file log level verbose" {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer(arrayOf("-f", "src/jvmTest/resources/log-verbose.config"))
            ALSLogger.minSeverity shouldBe Severity.Verbose
        }

        "command line log level debug" {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer(arrayOf("--log-level", "debug"))
            ALSLogger.minSeverity shouldBe Severity.Debug
        }

        "config file log level debug" {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer(arrayOf("-f", "src/jvmTest/resources/log-debug.config"))
            ALSLogger.minSeverity shouldBe Severity.Debug
        }

        "command line log level info" {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer(arrayOf("--log-level", "info"))
            ALSLogger.minSeverity shouldBe Severity.Info
        }

        "config file log level info" {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer(arrayOf("-f", "src/jvmTest/resources/log-info.config"))
            ALSLogger.minSeverity shouldBe Severity.Info
        }

        "command line log level warn" {
            ALSLogger.minSeverity = Severity.Error
            ALSLogger.minSeverity shouldBe Severity.Error
            newTestServer(arrayOf("--log-level", "warn"))
            ALSLogger.minSeverity shouldBe Severity.Warn
        }

        "config file log level warn" {
            ALSLogger.minSeverity = Severity.Error
            ALSLogger.minSeverity shouldBe Severity.Error
            newTestServer(arrayOf("-f", "src/jvmTest/resources/log-warn.config"))
            ALSLogger.minSeverity shouldBe Severity.Warn
        }

        "command line log level error" {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer(arrayOf("--log-level", "error"))
            ALSLogger.minSeverity shouldBe Severity.Error
        }

        "config file log level error" {
            ALSLogger.minSeverity shouldBe Severity.Warn
            newTestServer(arrayOf("-f", "src/jvmTest/resources/log-error.config"))
            ALSLogger.minSeverity shouldBe Severity.Error
        }

//        "command line ports error" {
//            TestLogger.startLogCapture()
//            newTestServer(arrayOf("-p", "x"))
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Error, "Could not parse port \"x\"", "Argument Parser"))
//            TestLogger.stopLogCapture()
//        }
//
//        "command line ports already exists" {
//            TestLogger.startLogCapture()
//            newTestServer(arrayOf("-p", "5 5"))
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn, "Port 5 already added", "Argument Parser"))
//            TestLogger.stopLogCapture()
//        }

//        "command line ports success" {
//            val server = newTestServer(arrayOf("-p", "5 6 7"))
//            server.ports.shouldContainExactly(5, 6, 7)
//        }

//        "config file ports error" {
//            TestLogger.startLogCapture()
//            newTestServer(arrayOf("-p", "x"))
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Error, "Could not parse port \"x\"", "Argument Parser"))
//            TestLogger.stopLogCapture()
//        }

//        "config file ports already exists" {
//            TestLogger.startLogCapture()
//            newTestServer(arrayOf("-p", "5 5"))
//            TestLogger.logs.shouldContain(TestLogger.Log(Severity.Warn, "Port 5 already added", "Argument Parser"))
//            TestLogger.stopLogCapture()
//        }
//
//        "config file ports success" {
//            val server = newTestServer(arrayOf("-p", "5 6 7"))
//            server.ports.shouldContainExactly(5, 6, 7)
//        }
    }
)
