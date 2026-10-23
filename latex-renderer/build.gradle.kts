import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

// The optional font module is a test input only, not a renderer runtime dependency.
evaluationDependsOn(":latex-fonts-katex")

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.hrm.latex.renderer"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true

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
            baseName = "LatexRenderer"
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.graphics.path)
        }
        commonMain.dependencies {
            api(projects.latexParser)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinx.coroutines.get()}")
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
        }
        val skiaTest by creating {
            dependsOn(commonTest.get())
            dependencies {
                implementation(projects.latexFontsKatex)
            }
        }
        macosTest {
            dependsOn(skiaTest)
            // Compose 1.10.3 aggregates production dependencies, not test-only font resources.
            // Pass the assembled output to its Native test-bundle copier.
            // https://github.com/JetBrains/compose-multiplatform/blob/v1.10.3/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/resources/IosResources.kt
            resources.srcDir(
                project(":latex-fonts-katex").tasks.named("assembleMacosArm64MainResources")
            )
        }
        jvmTest {
            dependsOn(skiaTest)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(true)

    signAllPublications()

    coordinates(
        "io.github.huarangmeng",
        "latex-renderer",
        rootProject.property("VERSION").toString()
    )

    pom {
        name.set("Kotlin Multiplatform LaTeX Rendering Engine")
        description.set(
            """
            Cross-platform LaTeX math rendering solution with:
            - Full LaTeX syntax support (math mode)
            - Custom command definitions
            - Chemical formula rendering
            - Compose Multiplatform UI integration
            - Multi-module architecture (base/parser/renderer)
        """.trimIndent()
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
