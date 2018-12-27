import javafx.event.EventHandler
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import kotlinx.coroutines.*
import tornadofx.*
import java.lang.Math.pow
import kotlin.math.*

class WS281xEmulator : View("WS281x Emulator") {
    private val scale = 22.0
    private val circleList = mutableListOf<Circle>()
    private val CENTER_X = scale * pow(leds.numLEDs.toDouble(), 0.5) + 20.0
    private val CENTER_Y = scale * pow(leds.numLEDs.toDouble(), 0.5) + 20.0

    init {
        val pixelList = leds.getPixelColorList()
        var r: Double
        var t: Double
        for (i in 0 until pixelList.size) {
            circleList.add(Circle().apply {
                radius = 20.0
                t = 2.5 * pow((i + 3).toDouble(), 0.5)
                r = scale * pow((i + 3).toDouble(), 0.5)
                centerX = r * cos(t) + CENTER_X
                centerY = r * sin(t) + CENTER_Y
                id = i.toString()
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
        GlobalScope.launch {
            while (true) {
                val pixels = leds.getPixelColorList()
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

    private val backColor = ColorContainer(0x0)
    override val root = borderpane {

        style {
            backgroundColor += backColor.toColor()
        }

        center {
            style {
                backgroundColor += backColor.toColor()
                setMinSize(
                    scale * pow(leds.numLEDs.toDouble(), 0.5) * 2 + 50.0,
                    scale * pow(leds.numLEDs.toDouble(), 0.5) * 2 + 50.0
                )
            }
            pane {
                for (i in 0 until leds.numLEDs) {
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

                                // Replace with animation call to test

                            }
                        }
                    }
                }
            }
        }
    }
}
