@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// F-Droid doesn't support foojay-resolver plugin
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
// }

rootProject.name = "M3-Play"

include(":app")
include(":innertube")
include(":kugou")
include(":lrclib")
include(":lastfm")
include(":simpmusic")
include(":betterlyrics")
include(":kizzy")
// ❌ canvas removed (was causing build error)
include(":shazamkit")

// Optional: Local NewPipe Extractor (keep commented)
// includeBuild("../NewPipeExtractor") {
//     dependencySubstitution {
//         substitute(module("com.github.teamnewpipe:NewPipeExtractor"))
//             .using(project(":extractor"))
//     }
// }
