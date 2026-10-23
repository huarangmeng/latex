package com.hrm.latex.renderer.export

import kotlinx.coroutines.await
import org.jetbrains.skiko.wasm.awaitSkiko

internal actual suspend fun awaitGraphicsReady() {
    awaitSkiko.await()
}
