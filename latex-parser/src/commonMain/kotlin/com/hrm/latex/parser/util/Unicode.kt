package com.hrm.latex.parser.util

/** 完整代理对一起消费，未配对的 UTF-16 单元由调用方容错。 */
internal fun String.codePointWidthAt(index: Int): Int =
    if (this[index].isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()) 2 else 1
