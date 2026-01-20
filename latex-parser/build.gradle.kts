import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.hrm.latex.parser"
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {}
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

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
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

    coordinates("io.github.huarangmeng", "latex-parser", rootProject.property("VERSION").toString())

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
                id.set("huaranmeng")
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
