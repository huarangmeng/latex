plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxBenchmark)
    alias(libs.plugins.allopen)
}

// JMH 要求 benchmark 类非 final，allopen 自动处理
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("kotlinx.benchmark.State")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":latex-parser"))
    implementation(libs.kotlinx.benchmark.runtime)
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
