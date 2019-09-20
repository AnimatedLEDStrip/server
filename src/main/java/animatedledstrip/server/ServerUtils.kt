package animatedledstrip.server

import animatedledstrip.utils.delayBlocking

fun AnimatedLEDStripServer<*>.waitUntilStop() {
    while (running) {
        delayBlocking(1)
    }
}