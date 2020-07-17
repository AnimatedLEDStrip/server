[![KDoc](https://img.shields.io/badge/KDoc-read-green.svg)](https://animatedledstrip.github.io/server/animatedledstrip-server/index.html)
[![Build Status](https://travis-ci.com/AnimatedLEDStrip/server.svg?branch=master)](https://travis-ci.com/AnimatedLEDStrip/server)
[![codecov](https://codecov.io/gh/AnimatedLEDStrip/server/branch/master/graph/badge.svg)](https://codecov.io/gh/AnimatedLEDStrip/server)

# AnimatedLEDStripServer
An AnimatedLEDStripServer is designed to run animations on a LED strip in parallel.
Animations can be started by clients (using the [AnimatedLEDStripClient](https://github.com/AnimatedLEDStrip/AnimatedLEDStripClient) library).

## Creating a Server
The AnimatedLEDStripServer library is combined with a device library to run a server on that device.
See the [wiki](https://github.com/AnimatedLEDStrip/server/wiki/Creating-a-Server)
for instructions.

### Device Libraries
Currently the only device supported is the Raspberry Pi. This can be [expanded in the future](https://github.com/AnimatedLEDStrip/server/wiki/Device-Libraries) to more devices.
See the [wiki](https://github.com/AnimatedLEDStrip/server/wiki/Device-Libraries)
for details.

## Local Command Line Connection
A connection to the server can be created from a local command line by running the executable with the `-C` flag.
See the [wiki](https://github.com/AnimatedLEDStrip/server/wiki/Command-Line-Interface) for more details.

## Note About Building
Because we use the dokka plugin to generate our documentation, we must build using Java <=9
> https://www.oracle.com/technetwork/java/javase/downloads/java-archive-javase9-3934878.html
