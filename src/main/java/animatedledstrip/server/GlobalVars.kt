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

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

val options = Options().apply {
    addOption("d", "debug", false, "Enable debug level logging")
    addOption("E", "emulate", false, "Emulate the LED strip")
    addOption("f", "prop-file", true, "Specify properties file")
    addOption("h", "help", false, "Show help message")
    addOption("i", "image-debug", false, "Enable image debugging")
    addOption("n", "numleds", true, "Specify number of LEDs")
    addLongOption("no-persist", false, "Don't persist animations (overrides --persist and persist=true)")
    addOption("o", "outfile", true, "Specify the output file name for image debugging")
    addOption("p", "pin", true, "Specify pin")
    addOption("P", "port", true, "Add a port for clients to connect to")
    addLongOption("persist", false, "Persist animations across restarts")
    addOption("q", "quiet", false, "Disable log outputs")
    addOption("r", "renders", true, "Specify the number of renders between saves")
    addOption("t", "trace", false, "Enable trace level logging")
    addOption("T", "Run test animation")
    addOption("v", "verbose", false, "Enable verbose logging statements")
}

fun Options.addLongOption(longOpt: String, hasArg: Boolean, description: String) {
    addOption(Option.builder().longOpt(longOpt).desc(description).hasArg(hasArg).build())
}
