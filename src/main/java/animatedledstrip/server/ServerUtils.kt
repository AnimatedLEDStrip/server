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

package animatedledstrip.server

import animatedledstrip.animationutils.AnimationData
import animatedledstrip.cmdline.CommandLine
import animatedledstrip.leds.AnimatedLEDStrip
import animatedledstrip.utils.delayBlocking
import org.apache.commons.cli.DefaultParser
import kotlin.reflect.KClass

fun <T : AnimatedLEDStrip> startServer(args: Array<String>, ledClass: KClass<T>) {
    val cmdline = DefaultParser().parse(options, args)
    when (cmdline.hasOption("C")) {
        false -> AnimatedLEDStripServer(args, ledClass).start().waitUntilStop()
        true -> CommandLine(
            port = cmdline.getOptionValue("P")?.toIntOrNull() ?: 1118,
            quiet = cmdline.hasOption("q")
        ).start()
    }
}

fun AnimatedLEDStripServer<*>.waitUntilStop() {
    while (running) {
        delayBlocking(1)
    }
}

val AnimationData.fileName: String
    get() = "$id.anim"