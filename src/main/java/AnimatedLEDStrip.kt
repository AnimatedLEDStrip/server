package server

import animatedledstrip.leds.AnimatedLEDStrip
import animatedledstrip.leds.AnimationData
import animatedledstrip.leds.ColorContainer

fun AnimatedLEDStrip.alternate(data: AnimationData) =
    alternate(ColorContainer(data.color1), ColorContainer(data.color2), delay = data.delay)

fun AnimatedLEDStrip.multiPixelRun(data: AnimationData) = multiPixelRun(
    data.spacing,
    data.direction,
    ColorContainer(data.color1),
    ColorContainer(data.color2),
    data.delay
)

fun AnimatedLEDStrip.multiPixelRunToColor(data: AnimationData) =
    multiPixelRunToColor(data.spacing, data.direction, ColorContainer(data.color1), data.delay)

fun AnimatedLEDStrip.pixelMarathon(data: AnimationData) = pixelMarathon(
    ColorContainer(data.color1),
    ColorContainer(data.color2),
    ColorContainer(data.color3),
    ColorContainer(data.color4),
    ColorContainer(data.color5),
    data.delay
)

fun AnimatedLEDStrip.pixelRun(data: AnimationData) =
    pixelRun(data.direction, ColorContainer(data.color1), ColorContainer(data.color2), data.delay)

fun AnimatedLEDStrip.pixelRunWithTrail(data: AnimationData) =
    pixelRunWithTrail(data.direction, ColorContainer(data.color1), ColorContainer(data.color2), data.delay)

fun AnimatedLEDStrip.smoothChase(data: AnimationData) = smoothChase(data.colorList, data.direction, data.delay)

fun AnimatedLEDStrip.sparkle(data: AnimationData) = sparkle(ColorContainer(data.color1), data.delay)

fun AnimatedLEDStrip.sparkleToColor(data: AnimationData) = sparkleToColor(ColorContainer(data.color1), data.delay)

fun AnimatedLEDStrip.stack(data: AnimationData) = stack(data.direction, ColorContainer(data.color1), data.delay)

fun AnimatedLEDStrip.stackOverflow(data: AnimationData) = stackOverflow(ColorContainer(data.color1), ColorContainer(data.color2))

fun AnimatedLEDStrip.wipe(data: AnimationData) = wipe(ColorContainer(data.color1), data.direction, data.delay)