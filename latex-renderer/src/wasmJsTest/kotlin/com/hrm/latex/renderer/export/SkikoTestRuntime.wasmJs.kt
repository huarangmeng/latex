@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@file:JsModule("./skiko.mjs")

package com.hrm.latex.renderer.export

import kotlin.js.JsAny
import kotlin.js.JsModule
import kotlin.js.Promise

// Skiko 未公开 Wasm Kotlin 就绪接口，直接等待其 JS 运行时导出的 Promise。
internal external val awaitSkiko: Promise<JsAny?>
