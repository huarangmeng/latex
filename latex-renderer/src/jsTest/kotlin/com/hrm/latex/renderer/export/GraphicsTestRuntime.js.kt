package com.hrm.latex.renderer.export

import kotlinx.coroutines.await
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.wasm.awaitSkiko

@OptIn(InternalSkikoApi::class)
internal actual suspend fun awaitGraphicsReady() {
    awaitSkiko.await()
}
