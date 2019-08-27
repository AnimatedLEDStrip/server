package animatedledstrip.server

import animatedledstrip.leds.AnimatedLEDStripPi

fun main(args: Array<String>) {
    AnimatedLEDStripServer(args, AnimatedLEDStripPi::class).start().waitUntilStop()
}

