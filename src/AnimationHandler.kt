import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import java.lang.Math.random

object AnimationHandler {

    fun addAnimation(params: Map<*, *>) {
        println(params)
        GlobalScope.launch(newSingleThreadContext("Thread ${random()}")) {
            SingleRunAnimation(params)
            println("${Thread.currentThread().name} complete")
        }
//        println("End addAnimation()")
    }


}

