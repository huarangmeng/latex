@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.hrm.latex.renderer.export

import kotlinx.coroutines.await

internal actual suspend fun awaitGraphicsReady() {
    awaitSkiko.await()
}
