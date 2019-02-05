package animatedledstrip.server

import animatedledstrip.leds.*
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

val options: Options = Options()
    .addOption("a", true, "Animation")
    .addOption("c1", true, "Color1")
    .addOption("c2", true, "Color2")
    .addOption("c3", true, "Color3")
    .addOption("c4", true, "Color4")
    .addOption("c5", true, "Color5")
    .addOption(Option("clist", true, "ColorList").apply {
        args = Option.UNLIMITED_VALUES
    })
    .addOption("d", true, "Direction")
    .addOption("delay", true, "Delay")
    .addOption("dmod", true, "DelayMod")
    .addOption("s", true, "Spacing")
    .addOption("e", true, "End an animation")

fun main(){
//    AnimationData().parseText(readLine()!!)
//    AnimationData().parseText(readLine()!!)

    val x = mapOf("Test" to Animation.ENDANIMATION) as Map<*, *>
    println(x["Test"] is String)
}


fun AnimationData.parseText(argString: String) {

    val args = argString.split(" ").toTypedArray()
    val parser = DefaultParser()
    val cmd = parser.parse(options, args)

    if(cmd.getOptionValue("e") != null) {
        this.animation = Animation.ENDANIMATION
        this.id = cmd.getOptionValue("e")
        println(this)
        return
    }


    this.animation = when(cmd.getOptionValue("a").toUpperCase()) {
        "COL1", "C1", "C", "COLOR" -> Animation.COLOR
        "MCOL", "MULTICOLOR" -> Animation.MULTICOLOR
        "ALT", "ALTERNATE" -> Animation.ALTERNATE
        "BNC", "BOUNCE" -> Animation.BOUNCE
        "BTC", "BOUNCETOCOLOR" -> Animation.BOUNCETOCOLOR
        "MPR", "MULTIPIXELRUN" -> Animation.MULTIPIXELRUN
        "MTC", "MULTIPIXELRUNTOCOLOR" -> Animation.MULTIPIXELRUNTOCOLOR
        "PXM", "PIXELMARATHON" -> Animation.PIXELMARATHON
        "PXR", "PIXELRUN" -> Animation.PIXELRUN
        "PXRT", "PIXELRUNWITHTRAIL" -> Animation.PIXELRUNWITHTRAIL
        "SCH", "SMOOTHCHASE" -> Animation.SMOOTHCHASE
        "SMF", "SMOOTHFADE" -> Animation.SMOOTHFADE
        "SPK", "SPARKLE" -> Animation.SPARKLE
        "SPF", "SPARKLEFADE" -> Animation.SPARKLEFADE
        "STC", "SPARKLETOCOLOR" -> Animation.SPARKLETOCOLOR
        "STO", "STACKOVERFLOW" -> Animation.STACKOVERFLOW
        "STK", "STACK" -> Animation.STACK
        "WIP", "WIPE" -> Animation.WIPE
        else -> throw IllegalArgumentException()
    }

    this.color1 = ColorContainer(parseHex(cmd.getOptionValue("c1") ?: "0"))
    this.color2 = ColorContainer(parseHex(cmd.getOptionValue("c2") ?: "0"))
    this.color3 = ColorContainer(parseHex(cmd.getOptionValue("c3") ?: "0"))
    this.color4 = ColorContainer(parseHex(cmd.getOptionValue("c4") ?: "0"))
    this.color5 = ColorContainer(parseHex(cmd.getOptionValue("c5") ?: "0"))

    (cmd.getOptionValues("clist") as Array<*>?)?.forEach {
        c -> colorList.add(ColorContainer(parseHex(c as String? ?: "0")))
    }

    this.delay = cmd.getOptionValue("delay")?.toLong() ?: when (animationInfoMap[animation]?.delay) {
        ReqLevel.REQUIRED -> throw Exception("Animation delay required for $animation")
        else -> 0L
    }

    this.delayMod = cmd.getOptionValue("dmod")?.toDouble() ?: 1.0

    this.direction = when (cmd.getOptionValue("d")?.toUpperCase()) {
        "F" -> Direction.FORWARD
        "B" -> Direction.BACKWARD
        else -> Direction.FORWARD
    }

    this.spacing = cmd.getOptionValue("s")?.toInt() ?: 3

    println(this)

}
