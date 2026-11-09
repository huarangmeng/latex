/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.renderer.layout

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.model.RenderContext

/**
 * 单个渲染器持有的有界布局缓存，复用不可变 AST 节点实例。
 *
 * 缓存键按引用身份比较 AST 和字体资源，不递归哈希节点或字体字节。
 * 结构相同的新 AST 实例会重新测量，增量解析保留的实例可继续复用。
 * 所属组合在多轮测量间保留缓存；[beginMeasure] 将像素布局绑定到
 * TextMeasurer、屏幕密度和字体缩放，环境变化时清空缓存。
 *
 * 编号环境、引用及其祖先依赖文档状态，每轮重新测量，保证编号计数推进。
 * 这些节点的纯子节点仍可复用。每轮重置可缓存性分类，避免保留历次编辑的 AST。
 *
 * 仅在组合线程使用，无需同步。缓存存续期间，AST 列表和字体字节必须保持不变。
 */
internal class LayoutCache(maxSize: Int = DEFAULT_MAX_SIZE) {
    private val enabled = maxSize > 0
    private val nodeCache = LruCache<CacheKey, NodeLayout>(maxSize)
    private val cacheability = HashMap<NodeIdentity, Boolean>()
    private var textMeasurer: TextMeasurer? = null
    private var density: Float? = null
    private var fontScale: Float? = null

    var hits: Int = 0
        private set
    var misses: Int = 0
        private set
    /** 因容量为零或子树依赖文档状态而跳过的查询，不计入命中或未命中。 */
    var bypasses: Int = 0
        private set

    /** 每轮完整测量前调用，包括 AST 已发生变化的情况。 */
    fun beginMeasure(measurer: TextMeasurer, density: Density) {
        if (textMeasurer !== measurer || this.density != density.density || fontScale != density.fontScale) {
            clear()
            textMeasurer = measurer
            this.density = density.density
            fontScale = density.fontScale
        }
        cacheability.clear()
    }

    fun getNode(node: LatexNode, context: RenderContext): NodeLayout? {
        if (!enabled) {
            bypasses++
            return null
        }
        val cached = nodeCache[CacheKey(node, context)]
        // 缓存只保存纯节点；同一不可变实例命中后，无需再次遍历子树进行分类。
        if (cached != null) {
            hits++
            return cached
        }
        if (!isCacheable(node)) {
            bypasses++
            return null
        }
        misses++
        return null
    }

    fun putNode(node: LatexNode, context: RenderContext, layout: NodeLayout) {
        if (enabled && isCacheable(node)) nodeCache[CacheKey(node, context)] = layout
    }

    private fun isCacheable(node: LatexNode): Boolean {
        val identity = NodeIdentity(node)
        cacheability[identity]?.let { return it }
        val dependsOnDocument = when (node) {
            is LatexNode.Ref, is LatexNode.EqRef, is LatexNode.Subequations -> true
            is LatexNode.Environment -> EquationNumbering.isNumberedEnvName(node.name)
            is LatexNode.Aligned -> node.envName?.let(EquationNumbering::isNumberedEnvName) == true
            is LatexNode.Multline -> node.envName?.let(EquationNumbering::isNumberedEnvName) == true
            is LatexNode.Eqnarray -> node.envName?.let(EquationNumbering::isNumberedEnvName) == true
            else -> false
        }
        // 不短路子节点遍历，在同一轮递归中完成后代分类，后续查询可直接复用。
        var cacheable = !dependsOnDocument
        for (child in node.children()) {
            if (!isCacheable(child)) cacheable = false
        }
        cacheability[identity] = cacheable
        return cacheable
    }

    fun clear() {
        nodeCache.clear()
        cacheability.clear()
        hits = 0
        misses = 0
        bypasses = 0
    }

    val size: Int get() = nodeCache.size

    companion object {
        const val DEFAULT_MAX_SIZE = 2048
    }
}

private class NodeIdentity(private val node: LatexNode) {
    private val hash = layoutIdentityHashCode(node)
    override fun hashCode(): Int = hash
    override fun equals(other: Any?): Boolean = other is NodeIdentity && node === other.node
}

/** 仅保存纯布局所需的输入快照，不持有文档编号状态。 */
private class CacheKey(private val node: LatexNode, context: RenderContext) {
    private val fontSize = context.fontSize
    private val baseFontSize = context.baseFontSize
    private val color = context.color
    private val errorColor = context.errorColor
    private val mathStyle = context.mathStyle
    private val isCramped = context.isCramped
    private val fontWeight = context.fontWeight
    private val fontStyle = context.fontStyle
    private val fontFamily = context.fontFamily
    private val fontVariant = context.fontVariant
    private val fontFamilies = context.fontFamilies
    private val isVariantFontFamily = context.isVariantFontFamily
    private val smallCaps = context.smallCaps
    private val layoutHints = context.layoutHints
    private val mathFontProvider = context.mathFontProvider
    private val textDirection = context.textDirection

    private val hash: Int = run {
        var result = layoutIdentityHashCode(node)
        result = 31 * result + fontSize.hashCode()
        result = 31 * result + baseFontSize.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + errorColor.hashCode()
        result = 31 * result + mathStyle.hashCode()
        result = 31 * result + isCramped.hashCode()
        result = 31 * result + (fontWeight?.hashCode() ?: 0)
        result = 31 * result + (fontStyle?.hashCode() ?: 0)
        result = 31 * result + identityHash(fontFamily)
        result = 31 * result + fontVariant.hashCode()
        result = 31 * result + identityHash(fontFamilies)
        result = 31 * result + isVariantFontFamily.hashCode()
        result = 31 * result + smallCaps.hashCode()
        result = 31 * result + layoutHints.hashCode()
        result = 31 * result + identityHash(mathFontProvider)
        result = 31 * result + textDirection.hashCode()
        result
    }

    override fun hashCode(): Int = hash

    override fun equals(other: Any?): Boolean = other is CacheKey &&
        node === other.node && fontSize == other.fontSize && baseFontSize == other.baseFontSize &&
        color == other.color && errorColor == other.errorColor && mathStyle == other.mathStyle &&
        isCramped == other.isCramped && fontWeight == other.fontWeight && fontStyle == other.fontStyle &&
        fontFamily === other.fontFamily && fontVariant == other.fontVariant &&
        fontFamilies === other.fontFamilies && isVariantFontFamily == other.isVariantFontFamily && smallCaps == other.smallCaps &&
        layoutHints == other.layoutHints && mathFontProvider === other.mathFontProvider &&
        textDirection == other.textDirection
}

private fun identityHash(value: Any?): Int = if (value == null) 0 else layoutIdentityHashCode(value)

/** 命中时只查询一次 HashMap，再原地移动双向链表节点，避免删除、重插及额外分配。 */
private class LruCache<K, V>(private val maxSize: Int) {
    init {
        require(maxSize >= 0) { "Cache capacity must not be negative" }
    }

    private class Entry<K, V>(val key: K, var value: V) {
        var previous: Entry<K, V>? = null
        var next: Entry<K, V>? = null
    }

    private val map = HashMap<K, Entry<K, V>>()
    private var oldest: Entry<K, V>? = null
    private var newest: Entry<K, V>? = null

    val size: Int get() = map.size

    operator fun get(key: K): V? {
        val entry = map[key] ?: return null
        moveToNewest(entry)
        return entry.value
    }

    operator fun set(key: K, value: V) {
        if (maxSize == 0) return
        val existing = map[key]
        if (existing != null) {
            existing.value = value
            moveToNewest(existing)
            return
        }
        val entry = Entry(key, value)
        map[key] = entry
        append(entry)
        if (map.size > maxSize) {
            val evicted = oldest ?: return
            detach(evicted)
            map.remove(evicted.key)
        }
    }

    private fun moveToNewest(entry: Entry<K, V>) {
        if (entry === newest) return
        detach(entry)
        append(entry)
    }

    private fun detach(entry: Entry<K, V>) {
        val previous = entry.previous
        val next = entry.next
        if (previous == null) oldest = next else previous.next = next
        if (next == null) newest = previous else next.previous = previous
        entry.previous = null
        entry.next = null
    }

    private fun append(entry: Entry<K, V>) {
        entry.previous = newest
        newest?.next = entry
        if (oldest == null) oldest = entry
        newest = entry
    }

    fun clear() {
        map.clear()
        oldest = null
        newest = null
    }
}
