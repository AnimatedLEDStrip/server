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

tasks.wrapper {
    gradleVersion = "8.7"
}

plugins {
    kotlin("multiplatform") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.23"
    id("org.jetbrains.dokka") version "1.9.20"
    id("io.kotest") version "0.3.9"
    id("org.jetbrains.kotlinx.kover") version "0.7.6"
    id("java-library")
    signing
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "io.github.animatedledstrip"
version = "1.1.2"
description = "A library for creating an AnimatedLEDStrip server"

kotlin {
    jvmToolchain(8)

    jvm {
//        compilations.all {
//            kotlinOptions.jvmTarget = "1.8"
//        }
    }
//    js(LEGACY) {
//        browser {
//            testTask {
//                useKarma {
//                    useChromeHeadless()
//                    webpackConfig.cssSupport.enabled = true
//                }
//            }
//        }
//    }
//    val hostOs = System.getProperty("os.name")
//    val isMingwX64 = hostOs.startsWith("Windows")
//    val nativeTarget = when {
//        hostOs == "Mac OS X" -> macosX64("native")
//        hostOs == "Linux" -> linuxX64("native")
//        isMingwX64 -> mingwX64("native")
//        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
//    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlin("reflect"))
                api("io.github.animatedledstrip:animatedledstrip-core:1.0.5")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.kotest:kotest-assertions-core:5.9.0")
                implementation("io.kotest:kotest-property:5.9.0")
            }
        }
        val jvmMain by getting {
            dependencies {
                api("io.github.animatedledstrip:animatedledstrip-core-jvm:1.0.5")

                api("commons-cli:commons-cli:1.8.0")
                implementation("io.github.maxnz:interactive-command-parser:0.1")
                implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.3")

                api("io.ktor:ktor-server-core:1.6.8")
                api("io.ktor:ktor-server-netty:1.6.8")
                api("io.ktor:ktor-serialization:1.6.8")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:0.20.0")
//                api("ch.qos.logback:logback-classic:1.5.6")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("io.mockk:mockk:1.13.11")
                implementation("io.kotest:kotest-runner-junit5:5.9.0")
                implementation("io.kotest:kotest-framework-engine-jvm:5.9.0")
            }
        }
//        val jsMain by getting
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
//        val nativeMain by getting
//        val nativeTest by getting
    }

}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
//    finalizedBy(tasks.jacocoTestReport)
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                       org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                       org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED)
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
//    systemProperties = System.getProperties().map { it.key.toString() to it.value }.toMap()
}

//tasks.jacocoTestReport {
//    val coverageSourceDirs = arrayOf(
//        "${projectDir}/src/commonMain/kotlin",
//        "${projectDir}/src/jvmMain/kotlin"
//    )
//
//    val classFiles = File("${buildDir}/classes/kotlin/jvm/main/")
//        .walkBottomUp()
//        .toSet()
//
//
//    classDirectories.setFrom(classFiles)
//    sourceDirectories.setFrom(files(coverageSourceDirs))
//
//    executionData.setFrom(files("${buildDir}/jacoco/jvmTest.exec"))
//
//    reports {
//        xml.isEnabled = true
//        csv.isEnabled = true
//        html.isEnabled = true
//    }
//}

val javadoc = tasks.named("javadoc")

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(javadoc)
}

publishing {
    publications.withType<MavenPublication>().forEach {
        it.apply {
            artifact(javadocJar)
            pom {
                name.set("AnimatedLEDStrip Server")
                description.set("A library designed to simplify running animations on WS281x strips")
                url.set("https://github.com/AnimatedLEDStrip/server")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }

                developers {
                    developer {
                        name.set("Max Narvaez")
                        email.set("mnmax.narvaez3@gmail.com")
                        organization.set("AnimatedLEDStrip")
                        organizationUrl.set("https://animatedledstrip.github.io")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/AnimatedLEDStrip/server.git")
                    developerConnection.set("scm:git:https://github.com/AnimatedLEDStrip/server.git")
                    url.set("https://github.com/AnimatedLEDStrip/server")
                }
            }
        }

    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

nexusPublishing {
    repositories {
        sonatype {
            val nexusUsername: String? by project
            val nexusPassword: String? by project
            username.set(nexusUsername)
            password.set(nexusPassword)
        }
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(projectDir.resolve("dokka"))
}
