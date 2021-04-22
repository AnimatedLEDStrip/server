/*
 * Copyright (c) 2018-2021 AnimatedLEDStrip
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package animatedledstrip.test

import animatedledstrip.animations.parameters.PercentDistance
import animatedledstrip.colors.ColorContainer
import animatedledstrip.colors.ccpresets.randomColorList
import animatedledstrip.leds.animationmanagement.AnimationToRunParams
import animatedledstrip.leds.stripmanagement.NativeLEDStrip
import animatedledstrip.server.AnimatedLEDStripServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import kotlinx.coroutines.delay
import java.nio.file.Paths

class AnimatedLEDStripServerTest : StringSpec(
    {
        "test start stop" {
            val server = newTestServer()
            server.start()
            server.running.shouldBeTrue()
            server.leds.renderer.isRendering.shouldBeTrue()
            server.stop()
            server.running.shouldBeFalse()
            server.leds.renderer.isRendering.shouldBeFalse()
        }

        "test bad NativeLEDStrip constructor" {
            class BadStrip : NativeLEDStrip {
                override val numLEDs: Int = 0
                override fun close() {}
                override fun render() {}
                override fun setPixelColor(pixel: Int, color: Int) {}
            }

            shouldThrow<IllegalArgumentException> {
                AnimatedLEDStripServer(arrayOf(), BadStrip::class)
            }

            class BadStrip2(override val numLEDs: Int) : NativeLEDStrip {
                override fun close() {}
                override fun render() {}
                override fun setPixelColor(pixel: Int, color: Int) {}
            }

            shouldThrow<IllegalArgumentException> {
                AnimatedLEDStripServer(arrayOf(), BadStrip2::class)
            }
        }

        "test save persistent animation".config(enabled = false) {
            val server = newTestServer("--persist", "--persist-dir", "src/jvmTest/resources/persist-save")

            val anim = AnimationToRunParams("Runway Lights", ColorContainer.randomColorList(), id = "test",
                                            doubleParams = mutableMapOf("maximumInfluence" to 3.0, "spacing" to 10.0),
                                            distanceParams = mutableMapOf("offset" to PercentDistance(0.0,
                                                                                                      50.0,
                                                                                                      0.0))).prepare(
                server.leds.sectionManager.getSection(""))

            server.savePersistentAnimation(anim)
            delay(500)
            Paths.get("src/jvmTest/resources/persist-save/.animations/test.json").shouldExist()
        }

        "test delete persistent animation".config(enabled = false) {
            val server = newTestServer("--persist", "--persist-dir", "src/jvmTest/resources/persist-delete")

            val anim = AnimationToRunParams("Runway Lights", ColorContainer.randomColorList(), id = "test2",
                                            doubleParams = mutableMapOf("maximumInfluence" to 3.0, "spacing" to 10.0),
                                            distanceParams = mutableMapOf("offset" to PercentDistance(0.0,
                                                                                                      50.0,
                                                                                                      0.0)))
                .prepare(server.leds.sectionManager.getSection(""))

            server.savePersistentAnimation(anim)
            delay(500)
            Paths.get("src/jvmTest/resources/persist-delete/.animations/test2.json").shouldExist()
            delay(500)
            server.deletePersistentAnimation(anim)
            delay(500)
            Paths.get("src/jvmTest/resources/persist-delete/.animations/test2.json").shouldNotExist()
        }

        "test load persistent animations" {
            val server = newTestServer("--persist", "--persist-dir", "src/jvmTest/resources/persist")

            server.loadPersistentAnimations()
            delay(1000)
            server.leds.animationManager.runningAnimations.shouldContainKeys("23602685",
                                                                             "40202146",
                                                                             "44470329",
                                                                             "51140794",
                                                                             "65067451",
                                                                             "84029121")
        }
    }
)
