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

package animatedledstrip.test

import animatedledstrip.leds.emulated.EmulatedAnimatedLEDStrip
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
    testLEDs.pixelColorList.forEach {
        assertTrue { it == color }
    }
}

private fun ByteArrayOutputStream.toCleanedString(): String {
    return this
        .toByteArray().filter { it != 0.toByte() }.toByteArray().toUTF8()   // remove excess null bytes
        .replace("\r\n", "\n")
        .replace("Welcome to the AnimatedLEDStrip Server console\nConnected\n", "")
        .replace(Regex("INFO:\\{.*};\n"), "")
        .replace(Regex("DATA:\\{.*};\n"), "")
}

fun checkOutput(expected: String) {
    assertTrue { outStream.toCleanedString() == expected }
    outStream.reset()
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