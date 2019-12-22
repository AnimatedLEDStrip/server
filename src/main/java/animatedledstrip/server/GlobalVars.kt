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

import animatedledstrip.cmdline.CommandLine
import animatedledstrip.leds.AnimatedLEDStrip
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import kotlin.reflect.KClass

val options = Options().apply {
    addOption("h", "help", false, "Show help message")
    addOption("d", "debug", false, "Enable debug level logging")
    addOption("t", "trace", false, "Enable trace level logging")
    addOption("v", "verbose", false, "Enable verbose logging statements")
    addOption("q", "quiet", false, "Disable log outputs")
    addOption("E", "emulate", false, "Emulate the LED strip")
    addOption("f", "prop-file", true, "Specify properties file")
    addOption("i", "image-debug", false, "Enable image debugging")
    addOption("o", "outfile", true, "Specify the output file name for image debugging")
    addOption("n", "numleds", true, "Specify number of LEDs")
    addOption("p", "pin", true, "Specify pin")
    addOption("r", "renders", true, "Specify the number of renders between saves")
    addOption("P", "persist", false, "Persist animations across restarts")
    addOption(
        Option.builder().longOpt("no-persist").desc("Don't persist animations (default true)").build()
    )
    addOption("T", "Run test animation")
    addOption("L", "local-port", true, "Specify local connection port number")
    addOption("C", "command-line", false, "Connect to a running server with a command line")
}

fun <T : AnimatedLEDStrip> startServer(args: Array<String>, ledClass: KClass<T>) {
    val cmdline = DefaultParser().parse(options, args)
    when (cmdline.hasOption("C")) {
        false -> AnimatedLEDStripServer(args, ledClass).start().waitUntilStop()
        true -> CommandLine(
            port = cmdline.getOptionValue("L")?.toIntOrNull() ?: 1118,
            quiet = cmdline.hasOption("q")
        ).start()
    }
}