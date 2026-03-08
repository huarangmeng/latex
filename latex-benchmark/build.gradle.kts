plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxBenchmark)
    alias(libs.plugins.allopen)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// JMH 要求 benchmark 类非 final，allopen 自动处理
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("kotlinx.benchmark.State")
}

kotlin {
    jvmToolchain(21)
}

// 让 benchmark 模块能访问 latex-renderer 的 internal API（仅用于性能基准测试）
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    val rendererJar = project(":latex-renderer").tasks.named("jvmJar")
    dependsOn(rendererJar)
    val jarFile = rendererJar.map { (it as Jar).archiveFile.get().asFile.absolutePath }
    compilerOptions {
        freeCompilerArgs.addAll(jarFile.map { listOf("-Xfriend-paths=$it") })
    }
}

dependencies {
    implementation(project(":latex-parser"))
    implementation(project(":latex-renderer"))
    implementation(libs.kotlinx.benchmark.runtime)

    // Compose Desktop 依赖（用于 TextMeasurer, Density 等）
    implementation(compose.desktop.currentOs)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.runtime)
}

benchmark {
    configurations {
        named("main") {
            warmups = 5
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            outputTimeUnit = "ms"
            mode = "avgt"
        }
    }
    targets {
        register("main")
    }
}
