plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("signing")
}

kotlin {
    val iosTargets = listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )

    iosTargets.forEach {
        it.binaries {
            framework { baseName = "library" }
        }
    }

    val nativeTargets = listOf(
        macosX64(),
        macosArm64(),
        linuxX64(),
        mingwX64(),
    ) + iosTargets

    nativeTargets.forEach {
        it.binaries {
            staticLib()
            sharedLib()
        }
        val rtmidi by it.compilations.getByName("main").cinterops.creating {
            packageName("dev.atsushieno.rtmidicinterop")
            includeDirs.allHeaders("../external/rtmidi/dist-shared/include")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":ktmidi"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}

afterEvaluate {

    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications.withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("ktmidi")
                description.set("Kotlin Multiplatform library for MIDI 1.0 and MIDI 2.0 - Native specific")
                url.set("https://github.com/atsushieno/ktmidi")
                scm {
                    url.set("https://github.com/atsushieno/ktmidi")
                }
                licenses {
                    license {
                        name.set("the MIT License")
                        url.set("https://github.com/atsushieno/ktmidi/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("atsushieno")
                        name.set("Atsushi Eno")
                        email.set("atsushieno@gmail.com")
                    }
                }
            }
        }

        repositories {
            /*
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/atsushieno/ktmidi")
                    credentials {
                        username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
                        password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
                    }
                }
                */
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSSRH_USERNAME")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

    // keep it as is. It is replaced by CI release builds
    signing {}
}
