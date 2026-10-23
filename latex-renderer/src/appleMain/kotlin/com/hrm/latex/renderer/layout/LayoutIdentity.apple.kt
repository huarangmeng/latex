package com.hrm.latex.renderer.layout

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

@OptIn(ExperimentalNativeApi::class)
internal actual fun layoutIdentityHashCode(value: Any): Int = value.identityHashCode()
