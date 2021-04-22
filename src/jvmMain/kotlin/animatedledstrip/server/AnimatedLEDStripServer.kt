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

package animatedledstrip.server

import animatedledstrip.communication.decodeJson
import animatedledstrip.communication.toUTF8String
import animatedledstrip.leds.animationmanagement.AnimationToRunParams
import animatedledstrip.leds.animationmanagement.RunningAnimationParams
import animatedledstrip.leds.animationmanagement.startAnimation
import animatedledstrip.leds.colormanagement.clear
import animatedledstrip.leds.locationmanagement.Location
import animatedledstrip.leds.stripmanagement.LEDStrip
import animatedledstrip.leds.stripmanagement.NativeLEDStrip
import animatedledstrip.leds.stripmanagement.StripInfo
import animatedledstrip.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

actual class AnimatedLEDStripServer<T : NativeLEDStrip> actual constructor(
    args: Array<String>,
    ledClass: KClass<T>,
    internal val pixelLocations: List<Location>?,
) {

    /** Is the server running */
    internal actual var running: Boolean = false

    /* Arguments for creating the AnimatedLEDStrip instance */

    internal actual var persistAnimations: Boolean = false

    internal actual var persistentAnimationDirectory: String = "."  // Will become ./.animations
        set(value) {
            field = "${if (value.endsWith("/")) value.removeSuffix("/") else value}/.animations"
        }

    /* Create strip info */

    actual var stripInfo: StripInfo by Delegates.notNull()

    init {
        parseOptions(args)
    }

    /* Create strip instance */

    actual val leds: LEDStrip

    init {
        /*
          Check validity of constructor before calling so user can be notified about
          what changes need to be made rather than just throwing an IllegalArgumentException
          without any explanation
        */
        val ledConstructor = ledClass.primaryConstructor
        requireNotNull(ledConstructor) { "LED class must have primary constructor" }
        require(ledConstructor.parameters.size == 1) { "LED class primary constructor must have only one argument" }
        require(ledConstructor.parameters[0].type.classifier == StripInfo::class) {
            "LED class primary constructor argument must be of type StripInfo"
        }

        leds = LEDStrip(stripInfo, ledConstructor.call(stripInfo))

        leds.startAnimationCallback = {
            if (persistAnimations) savePersistentAnimation(it)
        }
        leds.endAnimationCallback = {
            if (persistAnimations) deletePersistentAnimation(it)
        }
    }

    internal fun savePersistentAnimation(newAnim: RunningAnimationParams) {
        GlobalScope.launch(Dispatchers.IO) {
            FileOutputStream("$persistentAnimationDirectory/${newAnim.fileName}").apply {
                write(newAnim.sourceParams.json())
                close()
            }
        }
    }

    internal fun deletePersistentAnimation(anim: RunningAnimationParams) {
        if (File("$persistentAnimationDirectory/${anim.fileName}").exists())
            Files.delete(Paths.get("$persistentAnimationDirectory/${anim.fileName}"))
    }

    internal fun loadPersistentAnimations() {
        val dir = File(persistentAnimationDirectory)
        when {
            !dir.exists() -> dir.mkdirs()
            !dir.isDirectory -> Logger.w("Persist Animation Handler") { "$persistentAnimationDirectory should be a directory" }
            else -> GlobalScope.launch {
                File(persistentAnimationDirectory).walk().forEach {
                    if (!it.isDirectory && it.name.endsWith(".json"))
                        try {
                            FileInputStream(it).apply {
                                val obj = readAllBytes().toUTF8String().decodeJson() as AnimationToRunParams
                                leds.animationManager.startAnimation(obj, obj.id)
                                close()
                            }
                        } catch (e: FileNotFoundException) {
                        }
                }
            }
        }
    }

    val httpServer = httpServer(this)

    /* Start and stop methods */

    /**
     * Start the server
     *
     * @return This server
     */
    fun start(): AnimatedLEDStripServer<T> {
        leds.renderer.startRendering()

        if (persistAnimations) loadPersistentAnimations()

        httpServer.start(wait = false)

        running = true
        return this
    }

    /** Stop the server */
    fun stop() {
        if (!running) return
        httpServer.stop(10000, 30000)
        leds.clear()
        Thread.sleep(500) // Give renderer time to render the clear
        leds.renderer.stopRendering()
        running = false
    }

}
