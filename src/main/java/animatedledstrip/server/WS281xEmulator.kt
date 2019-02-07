package animatedledstrip.server

import animatedledstrip.leds.ColorContainer
import javafx.event.EventHandler
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*
import java.lang.Math.pow
import kotlin.math.cos
import kotlin.math.sin


/**
 * A GUI that shows an emulated LED strip using circles arranged in a spiral.
 *
 * The equation y = x^(1/2) is used to determine the polar coordinates for each circle to create a spiral.
 *
 * A button labeled "Test Animation" is located at the bottom of the window. This can be set to test different
 * animations by adding an animation call in the action lambda for the button (labeled with "Put animation
 * call to test here" in the code below).
 */
class WS281xEmulator : View("WS281x Emulator") {

    /**
     * Scaling for r component.
     */
    private val scale = 22.0

    /**
     * List of Circle instances (circle index == pixel index)
     */
    private val circleList = mutableListOf<Circle>()

    /**
     * X center of pane
     *
     * Takes furthest circle center (largest r) and adds 20
     */
    private val CENTER_X = scale * pow(leds.numLEDs.toDouble(), 0.5) + 20.0

    /**
     * Y center of pane
     *
     * Takes furthest circle center (largest r) and adds 20
     */
    private val CENTER_Y = scale * pow(leds.numLEDs.toDouble(), 0.5) + 20.0

    init {
        val pixelList = leds.pixelColorList
        var r: Double
        var t: Double
        for (i in 0 until pixelList.size) {
            circleList.add(Circle().apply {
                radius = 20.0                                   // Size of circle
                t = 2.5 * pow((i + 3).toDouble(), 0.5)          // t-component
                r = scale * pow((i + 3).toDouble(), 0.5)        // r-component
                centerX = r * cos(t) + CENTER_X                 // Convert polar coordinates to cartesian x
                centerY = r * sin(t) + CENTER_Y                 // Convert polar coordinates to cartesian y
                id = i.toString()                               // Create id for circle
                style {
                    fill = Color.color(
                        ((pixelList[i] shr 16 and 0xFF) / 255.0),
                        ((pixelList[i] shr 8 and 0xFF) / 255.0),
                        ((pixelList[i] and 0xFF) / 255.0), 1.0
                    )
                }

                onMouseClicked = EventHandler {
                    println("pixel: $i\t centerX: ~${centerX.toInt()}\t centerY: ~${centerY.toInt()}\t style: $style")
                }
            })
        }

        /*
        * Start continuous loop in a separate thread that constantly updates
        * colors of circles.
        *
        * Probably should be done differently - currently the source of various
        * glitches in GUI, especially when colors are updating quickly.
        * (Technically your aren't supposed to change things in the GUI from
        * a different thread)
        */
        GlobalScope.launch {
            while (true) {
                val pixels = leds.pixelColorList
                for (i in 0 until leds.numLEDs) {
                    circleList[i].style {
                        fill = Color.color(
                            ((pixels[i] shr 16 and 0xFF) / 255.0),
                            ((pixels[i] shr 8 and 0xFF) / 255.0),
                            ((pixels[i] and 0xFF) / 255.0), 1.0
                        )
                    }
                }
            }
        }
    }

    fun ColorContainer.toColor(): Color =
        Color.color((color shr 16 and 0xFF) / 255.0, (color shr 8 and 0xFF) / 255.0, (color and 0xFF) / 255.0)

    /**
     * Color of the pane background
     */
    private val backColor = ColorContainer(0x0)

    override val root = borderpane {

        style {
            backgroundColor += backColor.toColor()
        }

        center {
            style {
                backgroundColor += backColor.toColor()
                setMinSize(                                 // Set size of pane based on number of circles/pixels
                    scale * pow(leds.numLEDs.toDouble(), 0.5) * 2 + 50.0,
                    scale * pow(leds.numLEDs.toDouble(), 0.5) * 2 + 50.0
                )
            }
            pane {
                for (i in 0 until leds.numLEDs) {   // Add circles to pane
                    this += circleList[i]
                }
            }
        }

        bottom {
            gridpane {
                row {
                    button("Test Animation") {
                        action {
                            GlobalScope.launch {
                                // Put animation call to test here

                            }
                        }
                    }
                }
            }
        }
    }
}
