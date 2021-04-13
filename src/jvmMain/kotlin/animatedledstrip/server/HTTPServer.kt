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

import animatedledstrip.animations.groups.AnimationGroup
import animatedledstrip.communication.serializer
import animatedledstrip.leds.animationmanagement.AnimationToRunParams
import animatedledstrip.leds.animationmanagement.RunningAnimationParams
import animatedledstrip.leds.animationmanagement.endAnimation
import animatedledstrip.leds.animationmanagement.startAnimation
import animatedledstrip.leds.colormanagement.clear
import animatedledstrip.leds.colormanagement.pixelActualColorList
import animatedledstrip.leds.sectionmanagement.Section
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun httpServer(ledServer: AnimatedLEDStripServer<*>) {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(serializer)
        }
        install(CORS) {
            method(HttpMethod.Options)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
            method(HttpMethod.Patch)
            header(HttpHeaders.Authorization)
            allowCredentials = true
            allowNonSimpleContentTypes = true
            anyHost()
        }
        routing {
//            trace { application.log.debug(it.buildText()) }
            get("/") {
                call.respondText("Success")
            }
            animationRoute(ledServer)
            runningRoute(ledServer)
            sectionRoute(ledServer)
            startRoute(ledServer)
            stripRoute(ledServer)
        }
    }.start(true)
}

fun Route.animationRoute(ledServer: AnimatedLEDStripServer<*>) {
    route("/animation") {
        get {
            call.respondRedirect("/animations", permanent = true)
        }
        get("{name}") {
            val name: String = call.parameters["name"] ?: return@get call.respondText("Name of animation required")
            val anim = ledServer.leds.animationManager.findAnimationOrNull(name.toString())
                       ?: return@get call.respondText("Animation $name not found",
                                                      status = HttpStatusCode.NotFound)
            call.respond(anim.info)
        }
    }
    route("/animations") {
        get {
            call.respond(ledServer.leds.animationManager.supportedAnimations.values.map { it.info })
        }
        get("/names") {
            call.respond(ledServer.leds.animationManager.supportedAnimations.values.map { it.info.name })
        }
        get("/map") {
            call.respond(ledServer.leds.animationManager.supportedAnimations.map { it.key to it.value.info }.toMap())
        }
        get("{name}") {
            val name = call.parameters["name"] ?: return@get call.respondText("Name of animation required")
            call.respondRedirect("/animation/$name", permanent = true)
        }
        post("/newGroup") {
            val newGroup = call.receive<AnimationGroup.NewAnimationGroupInfo>()
            ledServer.leds.animationManager.addNewGroup(newGroup)
        }
    }
}

fun Route.runningRoute(ledServer: AnimatedLEDStripServer<*>) {
    route("/running") {
        get {
            call.respond(ledServer.leds.animationManager.runningAnimations.map { it.key to it.value.params }.toMap())
        }
        get("ids") {
            call.respond(ledServer.leds.animationManager.runningAnimations.keys.toList())
        }
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("Animation ID required",
                                                                          status = HttpStatusCode.BadRequest)
            val animParams: RunningAnimationParams =
                ledServer.leds.animationManager.runningAnimations[id]?.params
                ?: return@get call.respondText("Animation $id not running",
                                               status = HttpStatusCode.NotFound)
            call.respond(animParams)
        }
        delete("{id}") {
            val id = call.parameters["id"] ?: return@delete call.respondText("Animation ID required",
                                                                             status = HttpStatusCode.BadRequest)
            val animParams: RunningAnimationParams =
                ledServer.leds.animationManager.runningAnimations[id]?.params
                ?: return@delete call.respondText("Animation $id not running",
                                                  status = HttpStatusCode.NotFound)
            ledServer.leds.animationManager.endAnimation(animParams.id)
            call.respond(animParams)
        }
    }
}

fun Route.sectionRoute(ledServer: AnimatedLEDStripServer<*>) {
    route("/section") {
        get {
            call.respondRedirect("/sections", permanent = true)
        }
        get("{name}") {
            val name: String = call.parameters["name"] ?: return@get call.respondText("Name of section required")
            val section = ledServer.leds.sectionManager.getSectionOrNull(name)
                          ?: return@get call.respondText("Section $name not found",
                                                         status = HttpStatusCode.NotFound)
            call.respond(section)
        }
    }
    route("/sections") {
        get {
            call.respond(ledServer.leds.sectionManager.sections.values.toList())
        }
        get("/map") {
            call.respond(ledServer.leds.sectionManager.sections)
        }
        get("{name}") {
            val name: String = call.parameters["name"] ?: return@get call.respondText("Name of section required")
            call.respondRedirect("/section/$name", permanent = true)
        }
        post {
            val newSection = call.receive<Section>()
            if (ledServer.leds.sectionManager.getSectionOrNull(newSection.name) != null)
                return@post call.respondText("Section ${newSection.name} already exists",
                                             status = HttpStatusCode.Conflict)
            call.respond(ledServer.leds.sectionManager.createSection(newSection))
        }
    }
}

fun Route.startRoute(ledServer: AnimatedLEDStripServer<*>) {
    route("/start") {
        post {
            val animParams = call.receive<AnimationToRunParams>()
            call.respond(ledServer.leds.animationManager.startAnimation(animParams).params)
        }
    }
}

fun Route.stripRoute(ledServer: AnimatedLEDStripServer<*>) {
    route("/strip") {
        get("/color") {
            call.respond(ledServer.leds.pixelActualColorList)
        }
        get("/info") {
            call.respond(ledServer.leds.stripInfo)
        }
        post("/clear") {
            ledServer.leds.clear()
        }
    }
}
