import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.hrm.latex.fonts"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LatexFontsKatex"
            isStatic = true
        }
    }

    macosArm64()

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
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    packageOfResClass = "com.hrm.latex.fonts.katex.resources"
}

mavenPublishing {
    publishToMavenCentral(true)

    signAllPublications()

    coordinates(
        "io.github.huarangmeng",
        "latex-fonts-katex",
        rootProject.property("VERSION").toString()
    )

    pom {
        name.set("KaTeX fonts for the Kotlin Multiplatform LaTeX renderer")
        description.set(
            "Optional bundled KaTeX TTF fonts for latex-renderer. " +
                "Resource-only module: fonts are loaded automatically when present; " +
                "skip it to ship a smaller app and render with system fonts."
        )
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
