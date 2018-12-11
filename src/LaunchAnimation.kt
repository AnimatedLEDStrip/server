import java.lang.Long.parseLong

fun runAnimation(args: List<String>) {
    when (args[0].toUpperCase()) {
        "COL1", "C1", "C" -> staticColor1(args)
        "COL2", "C2" -> staticColor2(args)
        "COL3", "C3" -> staticColor3(args)
        "COL4", "C4" -> staticColor4(args)
        "ALT" -> callAlternate(args)
        "FDP" -> callFadePixel(args)
        "MPR" -> callMultiPixelRun(args)
        "MTC" -> callMultiPixelRunToColor(args)
        "PXR" -> callPixelRun(args)
        "SCH" -> callSmoothChase(args)
        "SPK" -> callSparkle(args)
        "STC" -> callSparkleToColor(args)
        "STK" -> callStack(args)
        "WIP" -> callWipe(args)
        "Q" -> return
        "NONE", "ERROR" -> {
            Thread.sleep(1000)
            return
        }
        else -> {
            animationQueue[0] = "ERROR"
            Thread.sleep(1000)
            return
        }
    }

}


fun parseHex(string: String): Long = parseLong(string, 16)

fun callWipe(args: List<String>) = try {
    val direction = try {
        when (args[2].toUpperCase()) {
            "F" -> Direction.FORWARD
            "B" -> Direction.BACKWARD
            else -> Direction.FORWARD
        }
    } catch (e: Exception) {
        Direction.FORWARD
    }

    val color = ColorContainer(parseHex(args[1]))

    val animationMap = mapOf("Animation" to Animations.WIPE, "Color1" to color.getColorHex(), "Direction" to direction)
    AnimationHandler.addAnimation(animationMap)
//    leds.wipe(color, direction)
    animationQueue[0] = "NONE"
} catch (e: Exception) {
    println("Error - Wipe Animation: $e")
}

fun callStack(args: List<String>) = try {
    val direction = try {
        when (args[2].toUpperCase()) {
            "F" -> 'F'
            "B" -> 'B'
            else -> 'F'
        }
    } catch (e: Exception) {
        'F'
    }

    val color = ColorContainer(parseHex(args[1]))

//    leds.stack(direction, color)
    val animationMap = mapOf("Animation" to Animations.STACK, "Color1" to color.getColorHex(), "Direction" to direction)
    AnimationHandler.addAnimation(animationMap)
    animationQueue[0] = "NONE"
} catch (e: Exception) {
    println("Error - Wipe Animation: $e")
}


fun callSparkleToColor(args: List<String>) = try {
    val color = ColorContainer(parseHex(args[1]))
    val animationMap = mapOf("Animation" to Animations.SPARKLETOCOLOR, "Color`" to color.getColorHex())
    AnimationHandler.addAnimation(animationMap)
//    leds.sparkleToColor(color)
    animationQueue[0] = "NONE"
} catch (e: Exception) {
    println("Error - Sparkle to Color Animation: $e")
}

fun callSparkle(args: List<String>) = try {
    val color = ColorContainer(parseHex(args[1]))
//    leds.sparkleCC(color)
    val animationMap = mapOf("Animation" to Animations.SPARKLE, "Color1" to color)
    AnimationHandler.addAnimation(animationMap)
    animationQueue[0] = "NONE"

} catch (e: Exception) {
    println("Error - Sparkle Animation: $e")
}

fun callSmoothChase(args: List<String>) = try {

    val direction = try {
        when (args[1].toUpperCase()) {
            "F" -> Direction.FORWARD
            "B" -> Direction.BACKWARD
            else -> Direction.FORWARD
        }
    } catch (e: Exception) {
        Direction.FORWARD
    }

//    leds.smoothChase(palette1, direction)

//    animationQueue[0] = "NONE"

    animationQueue[0] = "NONE"
} catch (e: Exception) {
    println("Error - Smooth Chase Animation: $e")
}

fun callPixelRun(args: List<String>) = try {
    val direction = try {
        when (args[3].toUpperCase()) {
            "F" -> 'F'
            "B" -> 'B'
            else -> 'F'
        }
    } catch (e: Exception) {
        'F'
    }

    val color1 = ColorContainer(parseHex(args[1]))
    val color2 = ColorContainer(parseHex(args[2]))

    val animationMap = mapOf("Animation" to Animations.PIXELRUN, "Color1" to color1, "Color2" to color2)
    AnimationHandler.addAnimation(animationMap)
    animationQueue[0] = "NONE"

//    try {
//        when (args[4].toUpperCase()) {
//            "T" -> leds.pixelRunWithTrail(direction, color1, color2)
//            else -> leds.pixelRun(direction, color1, color2)
//        }
//    } catch (f: Exception) {
//        leds.pixelRun(direction, color1, color2)
//    }
} catch (e: Exception) {
    println("Error - Pixel Run Animation: $e")
}

fun callMultiPixelRunToColor(args: List<String>) = try {
    val spacing = args[2].toInt()
    val direction = try {
        when (args[3].toUpperCase()) {
            "F" -> 'F'
            "B" -> 'B'
            else -> 'F'
        }
    } catch (e: Exception) {
        'F'
    }

    val color1 = ColorContainer(parseHex(args[1]))

//    leds.multiPixelRunToColor(spacing, direction, color1)
    animationQueue[0] = "NONE"
    val animationMap = mapOf("Animation" to Animations.MULTIPIXELRUNTOCOLOR, "Color1" to color1, "Spacing" to spacing, "Direction" to direction)
    AnimationHandler.addAnimation(animationMap)
} catch (e: Exception) {
    println("Error - Multi-Pixel Run To Color Animation: $e")
}

fun callMultiPixelRun(args: List<String>) = try {
    val spacing = args[3].toInt()
    val direction = try {
        when (args[4].toUpperCase()) {
            "F" -> 'F'
            "B" -> 'B'
            else -> 'F'
        }
    } catch (e: Exception) {
        'F'
    }

    val color1 = ColorContainer(parseHex(args[1]))
    val color2 = ColorContainer(parseHex(args[2]))

    val animationMap = mapOf("Animation" to Animations.MULTIPIXELRUN, "Color1" to color1, "Color2" to color2, "Spacing" to spacing, "Direction" to direction)
    AnimationHandler.addAnimation(animationMap)

//    leds.multiPixelRun(spacing, direction, color1, color2)
    animationQueue[0] = "NONE"

} catch (e: Exception) {
    println("Error - Multi-Pixel Run Animation: $e")
}

fun callFadePixel(args: List<String>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.


}

fun callAlternate(args: List<String>) = try {
    val color1 = ColorContainer(parseHex(args[1]))
    val color2 = ColorContainer(parseHex(args[2]))

    val animationMap = mapOf("Animation" to Animations.ALTERNATE, "Color1" to color1, "Color2" to color2)
    AnimationHandler.addAnimation(animationMap)
    animationQueue[0] = "NONE"

//    leds.alternate(color1, color2, args[3].toInt())
} catch (e: Exception) {
    println("Error - Alternate Animation: $e")
}

fun staticColor4(args: List<String>) = try {
    val color1 = ColorContainer(parseHex(args[1]))
    val color2 = ColorContainer(parseHex(args[2]))
    val color3 = ColorContainer(parseHex(args[3]))
    val color4 = ColorContainer(parseHex(args[4]))

    leds.setStripWithGradient(color1, color2, color3, color4)
    Thread.sleep(1000)
    animationQueue[0] = "NONE"
} catch (e: Exception) {
    println("Error - Static Color 4: $e")
}

fun staticColor3(args: List<String>) = try {
    val color1 = ColorContainer(parseHex(args[1]))
    val color2 = ColorContainer(parseHex(args[2]))
    val color3 = ColorContainer(parseHex(args[3]))

    leds.setStripWithGradient(color1, color2, color3)
    Thread.sleep(1000)

    animationQueue[0] = "NONE"
} catch (e: Exception) {
    println("Error - Static Color 3: $e")
}

fun staticColor2(args: List<String>) = try {
    val color1 = ColorContainer(parseHex(args[1]))
    val color2 = ColorContainer(parseHex(args[2]))

    leds.setStripWithGradient(color1, color2)
    Thread.sleep(1000)
    animationQueue[0] = "NONE"
} catch (e: Exception) {
    println("Error - Static Color 2: $e")
}

fun staticColor1(args: List<String>) = try {
    leds.setStripColor(parseHex(args[1]))
    Thread.sleep(1000)
    animationQueue[0] = "NONE"
} catch (e: Exception) {
    println("Error - Static Color 1: $e")
}
