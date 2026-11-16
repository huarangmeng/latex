package com.hrm.latex.renderer.utils

import com.hrm.latex.parser.model.LatexNode

/** TeX bin cancellation, with constant-time classification for any line range. */
internal class MathAtomSequence(nodes: List<LatexNode>) {
    val ordinalAt = IntArray(nodes.size) { -1 }
    val indices: IntArray
    private val raw: Array<AtomType>
    private val runStart: IntArray
    private val runEnd: IntArray
    private val firstAt = IntArray(nodes.size + 1)
    private val lastAt = IntArray(nodes.size + 1) { -1 }

    init {
        indices = nodes.indices.filter { !MathSpacing.isNonAtom(nodes[it]) }.toIntArray()
        raw = Array(indices.size) { MathSpacing.classifyNode(nodes[indices[it]]) }
        runStart = IntArray(indices.size)
        runEnd = IntArray(indices.size)
        for (ordinal in indices.indices) {
            ordinalAt[indices[ordinal]] = ordinal
            runStart[ordinal] = if (raw[ordinal] == AtomType.BIN && ordinal > 0 && raw[ordinal - 1] == AtomType.BIN)
                runStart[ordinal - 1] else ordinal
        }
        for (ordinal in indices.indices.reversed()) {
            runEnd[ordinal] = if (raw[ordinal] == AtomType.BIN && ordinal + 1 < raw.size && raw[ordinal + 1] == AtomType.BIN)
                runEnd[ordinal + 1] else ordinal + 1
        }
        firstAt[nodes.size] = indices.size
        for (i in nodes.indices.reversed()) firstAt[i] = ordinalAt[i].takeIf { it >= 0 } ?: firstAt[i + 1]
        for (i in nodes.indices) lastAt[i + 1] = ordinalAt[i].takeIf { it >= 0 } ?: lastAt[i]
    }

    fun first(start: Int): Int = firstAt[start]
    fun last(end: Int): Int = lastAt[end]
    fun afterLeadingBins(start: Int): Int = first(start).let { if (it < raw.size) runEnd[it] else it }
    fun isBin(ordinal: Int): Boolean = ordinal in raw.indices && raw[ordinal] == AtomType.BIN

    fun typeAt(index: Int, start: Int = 0, end: Int = ordinalAt.size): AtomType {
        val ordinal = ordinalAt[index]
        if (ordinal < 0) return AtomType.ORD
        if (raw[ordinal] != AtomType.BIN) return raw[ordinal]
        val first = first(start)
        val run = maxOf(first, runStart[ordinal])
        val beginsAsOrd = run == first || raw[run - 1] in LEFT_CANCELLERS
        val isOrd = (ordinal - run) % 2 == if (beginsAsOrd) 0 else 1
        if (isOrd || ordinal == last(end) || raw.getOrNull(ordinal + 1) in RIGHT_CANCELLERS) return AtomType.ORD
        return AtomType.BIN
    }

    companion object {
        private val LEFT_CANCELLERS = setOf(AtomType.OP, AtomType.REL, AtomType.OPEN, AtomType.PUNCT)
        private val RIGHT_CANCELLERS = setOf(AtomType.REL, AtomType.CLOSE, AtomType.PUNCT)
    }
}
