@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, org.jetbrains.compose.resources.InternalResourceApi::class)

package com.hrm.latex.renderer.utils

import com.hrm.latex.renderer.font.KATEX_RESOURCE_ROOT
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.readResourceBytes
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformMacosTest {
    @Test
    fun nativeMacosHasDesktopSemantics() {
        assertEquals(PlatformType.MACOS, getCurrentPlatform())
        assertFalse(isMobilePlatform())
    }

    @Test
    fun byteFontsAreStagedAtTheResourceReaderBundlePath() = runTest {
        val path = "${KATEX_RESOURCE_ROOT}katex_main_regular.ttf"
        val resourcePath = "${NSBundle.mainBundle.resourcePath}/compose-resources/$path"
        assertTrue(NSFileManager.defaultManager().fileExistsAtPath(resourcePath), resourcePath)
        assertEquals(listOf<Byte>(0, 1, 0, 0), readResourceBytes(path).take(4))
    }
}
