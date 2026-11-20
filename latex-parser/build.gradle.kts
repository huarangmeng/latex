import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
}

val karmaConfig = layout.projectDirectory.dir("karma.config.d")

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.hrm.latex.parser"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {}

        // 发布消费方 R8 / ProGuard 规则，随 AAR 一起分发给下游使用方
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.file(file("consumer-rules.pro"))
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LatexParser"
            isStatic = true
        }
    }

    macosArm64()

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xjdk-release=11")
        }
    }

    js {
        browser {
            testTask {
                inputs.dir(karmaConfig).withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                inputs.dir(karmaConfig).withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.latexBase)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.named<Test>("jvmTest") {
    inputs.dir(rootProject.file("test-corpus")).withPathSensitivity(PathSensitivity.RELATIVE)
    if (providers.environmentVariable("UPDATE_CORPUS_BASELINE").orNull == "1") {
        doNotTrackState("更新语料基线会写入测试输入目录")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

mavenPublishing {
    publishToMavenCentral(true)

    signAllPublications()

    coordinates("io.github.huarangmeng", "latex-parser", rootProject.version.toString())

    pom {
        name.set("Kotlin Multiplatform LaTeX Parser")
        description.set("""
            Cross-platform LaTeX math parsing solution with:
            - Full LaTeX syntax support (math mode)
            - Custom command definitions
            - Chemical formula rendering
            - Compose Multiplatform UI integration
            - Multi-module architecture (base/parser/renderer)
        """.trimIndent())
        inceptionYear.set("2026")
        url.set("https://github.com/huarangmeng/latex")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("huarangmeng")
                name.set("Kotlin Multiplatform Specialist")
                url.set("https://github.com/huarangmeng/")
            }
        }
        scm {
            url.set("https://github.com/huarangmeng/latex")
            connection.set("scm:git:git://github.com/huarangmeng/latex.git")
            developerConnection.set("scm:git:ssh://git@github.com/huarangmeng/latex.git")
        }
    }
}
