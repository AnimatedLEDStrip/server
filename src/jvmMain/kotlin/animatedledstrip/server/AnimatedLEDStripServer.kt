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
import animatedledstrip.leds.animationmanagement.AnimationToRunParams
import animatedledstrip.leds.animationmanagement.RunningAnimationParams
import animatedledstrip.leds.animationmanagement.startAnimation
import animatedledstrip.leds.colormanagement.clear
import animatedledstrip.leds.locationmanagement.Location
import animatedledstrip.leds.stripmanagement.LEDStrip
import animatedledstrip.leds.stripmanagement.NativeLEDStrip
import animatedledstrip.leds.stripmanagement.StripInfo
import animatedledstrip.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class AnimatedLEDStripServer<T : NativeLEDStrip>(
    args: Array<String>,
    ledClass: KClass<T>,
    internal val pixelLocations: List<Location>? = null,
) {

    /** Is the server running */
    internal var running: Boolean = false

    /* Arguments for creating the AnimatedLEDStrip instance */

    internal var persistAnimations: Boolean = false

    internal var storedAnimationsDirectory: String = "."  // Will become ./.animations
        set(value) {
            field = "${if (value.endsWith("/")) value.removeSuffix("/") else value}/.animations"
        }

    internal val persistentAnimationDirectory: String
        get() = "$storedAnimationsDirectory/persistent"

    internal val savedAnimationDirectory: String
        get() = "$storedAnimationsDirectory/saved"


    /* Create strip info */

    var stripInfo: StripInfo by Delegates.notNull()

    init {
        parseOptions(args)
    }

    /* Create strip instance */

    val leds: LEDStrip

    val savedAnimations: MutableMap<String, AnimationToRunParams> = mutableMapOf()

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

        loadSavedAnimations()
    }

    internal fun savePersistentAnimation(newAnim: RunningAnimationParams) {
        val scope = CoroutineScope(EmptyCoroutineContext)
        scope.launch(Dispatchers.IO) {
            FileOutputStream("$persistentAnimationDirectory/${newAnim.fileName}").apply {
                write(newAnim.sourceParams.json())
                close()
            }
        }
        scope.cancel()
    }

    internal fun deletePersistentAnimation(anim: RunningAnimationParams) {
        if (File("$persistentAnimationDirectory/${anim.fileName}").exists())
            Files.delete(Paths.get("$persistentAnimationDirectory/${anim.fileName}"))
    }

    internal fun loadPersistentAnimations() {
        val dir = File(persistentAnimationDirectory)
        val scope = CoroutineScope(EmptyCoroutineContext)
        when {
            !dir.exists() -> dir.mkdirs()
            !dir.isDirectory -> Logger.w("$persistentAnimationDirectory should be a directory")
            else ->
                dir.walk().forEach {
                    if (!it.isDirectory && it.name.endsWith(".json"))
                        try {
                            val obj = it.readText().decodeJson() as AnimationToRunParams
                            leds.animationManager.startAnimation(obj, obj.id)
                        } catch (e: Exception) {
                            Logger.e("Failed to decode ${it.name}: $e")
                        }
                }
        }
        scope.cancel()
    }

    fun addSavedAnimation(newAnim: AnimationToRunParams) {
        Logger.i(newAnim.toString())
        savedAnimations[newAnim.id] = newAnim
        FileOutputStream("$savedAnimationDirectory/${newAnim.fileName}").apply {
            write(newAnim.json())
            close()
        }
    }

    fun deleteSavedAnimation(id: String) {
        savedAnimations.remove(id)
        if (File("$savedAnimationDirectory/$id.json").exists())
            Files.delete(Paths.get("$savedAnimationDirectory/$id.json"))
    }

    internal fun loadSavedAnimations() {
        val dir = File(savedAnimationDirectory)
        val scope = CoroutineScope(EmptyCoroutineContext)
        when {
            !dir.exists() -> dir.mkdirs()
            !dir.isDirectory -> Logger.w("$savedAnimationDirectory should be a directory")
            else -> scope.launch {
                File(savedAnimationDirectory).walk().forEach {
                    if (!it.isDirectory && it.name.endsWith(".json"))
                        try {
                            val obj = it.readText().decodeJson() as AnimationToRunParams
                            savedAnimations[obj.id] = obj
                        } catch (e: Exception) {
                            Logger.e("Failed to decode ${it.name}: $e")
                        }
                }
            }
        }
        scope.cancel()
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
