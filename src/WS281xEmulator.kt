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
    private val CENTERX = scale * pow(leds.numLEDs.toDouble(), 0.5) + 25.0
    private val CENTERY = scale * pow(leds.numLEDs.toDouble(), 0.5) + 25.0

    init {
        val pixelList = leds.getPixelColorList()
        var r = 0.0
        var t = 0.0
        for (i in 0 until pixelList.size) {
            circleList.add(Circle().apply {
                radius = 25.0
                centerX = r * cos(t) + CENTERX
                centerY = r * sin(t) + CENTERY
                t = 2.5 * pow(i.toDouble(), 0.5)
                r = scale * pow(i.toDouble(), 0.5)
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

    override val root = borderpane {

        style {
            backgroundColor += Color.BLACK
        }

        center {
            style {
                backgroundColor += Color.BLACK
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
                                leds.multiPixelRun(
                                    3,
                                    Direction.FORWARD,
                                    ColorContainer(0xFF)
                                )

                            }
                        }
                    }
                }
            }
        }
    }
}
