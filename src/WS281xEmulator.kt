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
    private var stopAnimation = false
//    private val ROWSIZE = 20
    private val CENTERX = scale * pow(leds.numLEDs.toDouble(), 0.5) + 25.0
    private val CENTERY = scale * pow(leds.numLEDs.toDouble(), 0.5) + 25.0

    init {
//        DefaultsView().openWindow()
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
                println("i: $i r: $r t: $t centerX: $centerX centerY: $centerY")
                id = i.toString()
                style {
                    fill = Color.color(
                        ((pixelList[i] shr 16 and 0xFF) / 255.0),
                        ((pixelList[i] shr 8 and 0xFF) / 255.0),
                        ((pixelList[i] and 0xFF) / 255.0), 1.0
                    )
                }

//                GlobalScope.launch(newSingleThreadContext(id)) {
//                    while(true) {
//                        val color = leds.getPixelColor(id.toInt())
//                        this@apply.fill = Color.color(
//                            ((color.hex shr 8 and 0xFF) / 255.0),
//                            ((color.hex shr 4 and 0xFF) / 255.0),
//                            ((color.hex and 0xFF) / 255.0)
//                        )
//                        Thread.sleep(10)
////                        println("$id: ${color.hex}")
//                    }
//                }
                onMouseClicked = EventHandler {
                    println("${this.id} ${this.style} ${this.centerX} ${this.centerY}")
                }
            })
        }
        GlobalScope.launch {
            while (true) {
                var pixels = leds.getPixelColorList()
                for (i in 0 until leds.numLEDs) {
                    circleList[i].style {
                        fill = Color.color(
                            ((pixels[i] shr 16 and 0xFF) / 255.0),
                            ((pixels[i] shr 8 and 0xFF) / 255.0),
                            ((pixels[i] and 0xFF) / 255.0), 1.0
                        )
                    }
//                    if (i == 0) GlobalScope.launch {
//                        pixels = leds.getPixelColorList()
//                    }
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
                setMinSize(scale * pow(leds.numLEDs.toDouble(), 0.5) * 2 + 50.0, scale * pow(leds.numLEDs.toDouble(), 0.5) * 2 + 50.0)
            }
//            circle {
//                centerX = 50.0
//                centerY = 50.0
//                radius = 25.0
//            }
            pane {
                for (i in 0 until leds.numLEDs) {
                    this += circleList[i]
                }
            }
//            gridpane {
//                val rows = leds.numLEDs / ROWSIZE
//                println(rows)
//                for (i in 0 until rows) {
//                    row {
//                        for (c in 0 until ROWSIZE) {
//                            this += circleList[i * ROWSIZE + c]
////                            println("$i:$c = ${i * 15 + c}")
//                        }
//                    }
//                    row { label {} }
//                }
//                if (leds.numLEDs % ROWSIZE != 0) {
//                    row {
//                        for (c in 0 until leds.numLEDs % 15) this += circleList[rows * ROWSIZE + c]
//                    }
//                }
//                row { label {} }
//            }
        }
//        bottom {
//            gridpane {
//                row {
//                    button("MultiPixelRun") {
//                        action {
//                            GlobalScope.launch {
//                                while (!stopAnimation) leds.multiPixelRun(3, Direction.FORWARD, ColorContainer(0xFF))
//                                stopAnimation = false
//                            }
//                        }
//                    }
//                    button("Wipe") {
//                        action {
//                            GlobalScope.launch {
//                                stopAnimation = true
//                                Thread.sleep(200)
//                                leds.wipe(ColorContainer(0xFF), Direction.FORWARD)
//                                stopAnimation = false
//                            }
//                        }
//                    }
//                    button("Pixel Marathon") {
//                        action {
//                            GlobalScope.launch {
//                                stopAnimation = true
//                                Thread.sleep(200)
//                                leds.pixelMarathon(
//                                    ColorContainer(0xFF),
//                                    ColorContainer(0xFF00),
//                                    ColorContainer(0xFF0000),
//                                    ColorContainer(0xFF00FF),
//                                    ColorContainer(0x00FFFF), 50
//                                )
//                                stopAnimation = false
//                            }
//                        }
//                    }
//                    button("Multi-Alternate") {
//                        action {
//                            GlobalScope.launch {
//                                stopAnimation = true
//                                Thread.sleep(200)
//                                leds.multiAlternate(5, 10, ColorContainer(0xFF), ColorContainer(0xFF00))
//                                stopAnimation = false
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    init {

    }
}
