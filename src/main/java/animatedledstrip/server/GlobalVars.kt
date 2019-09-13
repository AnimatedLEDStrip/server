package animatedledstrip.server

import org.apache.commons.cli.Options

var server: AnimatedLEDStripServer<*>? = null

val options = Options().apply {
    addOption("d", "Enable debugging")
    addOption("t", "Enable trace debugging")
    addOption("v", "Enable verbose log statements")
    addOption("q", "Disable log outputs")
    addOption("E", "Emulate LED strip but do NOT launch emulator")
    addOption("f", true, "Specify properties file")
    addOption("o", true, "Specify output file name for image debugging")
    addOption("r", true, "Specify number of renders between saves")
    addOption("i", "Enable image debugging")
    addOption("T", "Run test")
    addOption("C", "Connect to a running server with a command line")
}