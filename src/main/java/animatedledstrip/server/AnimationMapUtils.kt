package animatedledstrip.server

import animatedledstrip.leds.Animation

/*  Operator functions for simplifying decomposition of a Map */

operator fun Map<*, *>.component1() = this["Animation"] as Animation
operator fun Map<*, *>.component2() = this["Color1"] as Long
operator fun Map<*, *>.component3() = this["Color2"] as Long?
operator fun Map<*, *>.component4() = this["Color3"] as Long?
operator fun Map<*, *>.component5() = this["Color4"] as Long?
operator fun Map<*, *>.component6() = this["Color5"] as Long?
operator fun Map<*, *>.component7() = this["ColorList"] as List<*>?
operator fun Map<*, *>.component8() = this["Direction"] as Char?
operator fun Map<*, *>.component9() = this["Spacing"] as Int?
operator fun Map<*, *>.component10() = this["Delay"] as Int?
operator fun Map<*, *>.component11() = this["Continuous"] as Boolean?
operator fun Map<*, *>.component12() = this["ID"] as String?
