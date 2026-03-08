rootProject.name = "latex"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://maven.universablockchain.com/")
                }
            }
            filter {
                includeGroup("net.sergeych")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
include(":androidapp")

// SDK 模块
include(":latex-base")
include(":latex-parser")
include(":latex-renderer")

// Preview 模块（仅用于本地预览，不参与sdk打包）
include(":latex-preview")

// Benchmark 模块（仅用于性能基准测试，不参与sdk打包）
include(":latex-benchmark")