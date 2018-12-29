import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.lang.Math.random

object AnimationHandler {

    val continuousAnimations = mutableMapOf<String, ContinuousRunAnimation>()

    fun addAnimation(params: Map<*, *>) {
        println(params)
        GlobalScope.launch(newSingleThreadContext("Thread ${random()}")) {
            val (animation, _, _, _, _, _, _, _, _,
                    continuous, ID) = params

            when (animation) {
                Animations.COLOR1,
                Animations.COLOR2,
                Animations.COLOR3,
                Animations.COLOR4,
                Animations.MULTIPIXELRUNTOCOLOR,
                Animations.SPARKLETOCOLOR,
                Animations.STACK,
                Animations.WIPE -> {
                    SingleRunAnimation(params)
                    println("${Thread.currentThread().name} complete")
                }
                Animations.ALTERNATE,
                Animations.MULTIPIXELRUN,
                Animations.PIXELRUN,
                Animations.PIXELRUNWITHTRAIL,
                Animations.PIXELMARATHON,
                Animations.SMOOTHCHASE,
                Animations.SPARKLE,
                Animations.STACKOVERFLOW -> {
                    if (continuous == true) {
                        val id = random().toString()
                        continuousAnimations[id] =
                                ContinuousRunAnimation(id, params)
                        continuousAnimations[id]?.startAnimation()
                        println(continuousAnimations)
                        println("${Thread.currentThread().name} complete")
                    } else {
                        SingleRunAnimation(params)
                        println("${Thread.currentThread().name} complete")
                    }
                }
                Animations.ENDANIMATION -> {
                    continuousAnimations[ID]?.endAnimation()
                    continuousAnimations.remove(ID)
                }
                else -> println("Animation $animation not supported by server")
            }
        }
    }
}

