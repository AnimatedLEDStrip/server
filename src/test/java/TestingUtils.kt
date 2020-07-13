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

package animatedledstrip.test

import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
import animatedledstrip.server.SocketConnections
import animatedledstrip.utils.toUTF8
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertTrue

private val exitStream = ByteArrayInputStream("exit\n".toByteArray())
private var stream = ByteArrayInputStream("".toByteArray())
val outStream = ByteArrayOutputStream()

fun checkAllPixels(testLEDs: EmulatedAnimatedLEDStrip, color: Long) {
    testLEDs.pixelProlongedColorList.forEach {
        assertTrue { it == color }
    }
}

private fun ByteArrayOutputStream.toCleanedString(removeInfo: Boolean = true, removeData: Boolean = true): String {
    var returnStr = this
        .toByteArray().filter { it != 0.toByte() }.toByteArray().toUTF8()   // remove excess null bytes
        .replace("\r\n", "\n")                                              // Allow CRLF and LF terminals to test
        .replace("\n", "")                                                 // Remove newlines
        .replace("Welcome to the AnimatedLEDStrip Server consoleConnected", "")
    if (removeInfo) returnStr = returnStr.replace(Regex("Strip Info:[\\s\\S]*End Strip Info"), "")
    if (removeData) returnStr = returnStr.replace(Regex("AnimationData \\d*:[\\s\\S]*End AnimationData"), "")
    return returnStr
}

fun checkOutput(expected: String, removeInfo: Boolean = true, removeData: Boolean = true) {
    try {
        assertTrue {
            outStream.toCleanedString(removeInfo, removeData) ==
                    expected.filterNot { it == '\n' || it == '\r' }
        }
        outStream.reset()
    } catch (e: AssertionError) {
        System.err.println("expected: " + expected.filterNot { it == '\n' || it == '\r' })
        System.err.println()
        System.err.println("actual:   " + outStream.toCleanedString())
        throw e
    }
}

fun newCommandStream(newString: String) {
    stream = ByteArrayInputStream(newString.toByteArray())
    System.setIn(stream)

    exitStream.reset()
    GlobalScope.launch {
        delay(4000)
        System.setIn(exitStream)
    }
}

fun redirectOutput() {
    outStream.reset()
    System.setOut(PrintStream(outStream))
}

fun SocketConnections.Connection.reset() {
    close()
    open()
}