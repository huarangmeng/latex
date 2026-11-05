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
    runtimeOnly(project(":latex-fonts-katex"))
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlinx.coroutines.core)

    // Compose Desktop 依赖（用于 TextMeasurer, Density 等）
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
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

// kotlinx-benchmark 的 runner 不支持透传 JMH profiler；直接复用其生成物运行标准 JMH。
tasks.register<JavaExec>("performanceBenchmark") {
    group = "benchmark"
    description = "运行全链路 JVM 性能基准并输出 JSON 与 GC 分配指标"
    dependsOn("mainBenchmarkCompile")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    mainClass.set("org.openjdk.jmh.Main")
    // 固定堆大小，避免不同机器的默认堆策略干扰分配与 GC 对比。
    jvmArgs("-Xms256m", "-Xmx1g")
    classpath(
        layout.buildDirectory.dir("benchmarks/main/classes"),
        layout.buildDirectory.dir("benchmarks/main/resources"),
        sourceSets.main.get().runtimeClasspath
    )
    val profile = providers.gradleProperty("benchmarkProfile").orElse("full").get()
    require(profile == "full" || profile == "ci") { "benchmarkProfile 只支持 full 或 ci" }
    val isCi = profile == "ci"
    // CI 保留所有测量阶段，每个参数维度选一组代表值，减少独立 JVM 的启动次数。
    if (isCi) {
        args(
            "-p", "input=large", "-p", "edit=replace", "-p", "scenario=streaming",
            "-p", "chunkSize=16", "-p", "entries=1000"
        )
    }
    val report = providers.gradleProperty("benchmarkReport")
        .map { rootProject.file(it) }
        .orElse(layout.buildDirectory.file("reports/performance/jmh.json").map { it.asFile })
    args(
        providers.gradleProperty("benchmarkInclude").orElse(
            "com.hrm.latex.benchmark.*Benchmark.*"
        ).get(),
        "-wi", providers.gradleProperty("benchmarkWarmups").orElse(if (isCi) "1" else "3").get(),
        "-i", providers.gradleProperty("benchmarkIterations").orElse(if (isCi) "2" else "5").get(),
        "-w", providers.gradleProperty("benchmarkIterationMs").orElse(if (isCi) "100" else "500").get() + "ms",
        "-r", providers.gradleProperty("benchmarkIterationMs").orElse(if (isCi) "100" else "500").get() + "ms",
        "-f", providers.gradleProperty("benchmarkForks").orElse("1").get(),
        "-t", "1", "-tu", "ms", "-rf", "json", "-rff", report.get().absolutePath,
        "-foe", "true"
    )
    if (providers.gradleProperty("benchmarkGc").orElse("true").get().toBoolean()) {
        args("-prof", "gc")
    }
    doFirst { report.get().parentFile.mkdirs() }
}
