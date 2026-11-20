package com.hrm.latex.renderer.layout

import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.renderer.model.RenderContext
import com.hrm.latex.renderer.model.MathStyle
import com.hrm.latex.renderer.utils.AtomType
import com.hrm.latex.renderer.utils.MathAtomSequence
import com.hrm.latex.renderer.utils.MathSpacing
import kotlin.test.Test
import kotlin.test.assertEquals

class MathSpacingAuditTest {
    // Rows/columns: Ord, Op, Bin, Rel, Open, Close, Punct, Inner.
    // KaTeX 0.16.22 src/spacingData.js, normal and tight spacing tables.
    private val normalMu = arrayOf(
        intArrayOf(0, 3, 4, 5, 0, 0, 0, 3),
        intArrayOf(3, 3, 0, 5, 0, 0, 0, 3),
        intArrayOf(4, 4, 0, 0, 4, 0, 0, 4),
        intArrayOf(5, 5, 0, 0, 5, 0, 0, 5),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 3, 4, 5, 0, 0, 0, 3),
        intArrayOf(3, 3, 0, 5, 3, 3, 3, 3),
        intArrayOf(3, 3, 4, 5, 3, 0, 3, 3),
    )
    private val tightMu = arrayOf(
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0),
        intArrayOf(3, 3, 0, 0, 0, 0, 0, 0),
        IntArray(8), IntArray(8), IntArray(8),
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0),
        IntArray(8), intArrayOf(0, 3, 0, 0, 0, 0, 0, 0),
    )

    @Test
    fun allAtomPairsMatchNormalAndScriptTables() {
        for (left in AtomType.entries) for (right in AtomType.entries) {
            assertEquals(normalMu[left.ordinal][right.ordinal] / 18f,
                MathSpacing.spaceBetween(left, right, false), 0.00001f, "$left/$right normal")
            assertEquals(tightMu[left.ordinal][right.ordinal] / 18f,
                MathSpacing.spaceBetween(left, right, true), 0.00001f, "$left/$right tight")
        }
    }

    @Test
    fun parserAndMathListMatchKatexAtomFamilies() {
        val context = RenderContext(fontSize = 18.sp, color = Color.Black)
        for ((source, expected, _) in mathSpacingReference) {
            val list = InlineMathList.prepare(LatexParser().parse(source).children, context)
            val sequence = MathAtomSequence(list.nodes)
            val actual = sequence.indices.joinToString(" ") { sequence.typeAt(it).name }
            // KaTeX emits a zero-width relational slash plus the relation;
            // our Negation node combines those two adjacent relations.
            val families = if (source.contains("\\not")) expected.replace("REL REL", "REL") else expected
            assertEquals(families, actual, source)
        }
    }

    @Test
    fun compatibilitySymbolsKeepCommandSpecificAtomTypes() {
        val source = """a \lhd b \vartriangleleft c \vartriangle d \triangle e"""
        val context = RenderContext(fontSize = 18.sp, color = Color.Black)
        val list = InlineMathList.prepare(LatexParser().parse(source).children, context)
        val sequence = MathAtomSequence(list.nodes)
        assertEquals("ORD BIN ORD REL ORD REL ORD ORD ORD", sequence.indices.joinToString(" ") { sequence.typeAt(it).name })
    }

    @Test
    fun referenceListsHaveExpectedImplicitAndExplicitGlue() {
        val context = RenderContext(fontSize = 18.sp, color = Color.Black)
        for ((source, _, mu) in mathSpacingReference) {
            // Script tables are checked independently above; DOM em margins are
            // relative to KaTeX's optical-size font metrics rather than this font.
            if (source.startsWith("\\scriptstyle")) continue
            val list = InlineMathList.prepare(LatexParser().parse(source).children, context)
            val layouts = list.nodes.map { node ->
                val width = when (node) {
                    is LatexNode.Space -> when (node.type) {
                        LatexNode.Space.SpaceType.SOURCE -> 0f
                        LatexNode.Space.SpaceType.THIN -> 3f
                        LatexNode.Space.SpaceType.NEGATIVE_THIN -> -3f
                        LatexNode.Space.SpaceType.QUAD -> 18f
                        else -> error("Unexpected spacing fixture: $source")
                    }
                    else -> 0f
                }
                NodeLayout(width, 0f, 0f) { _, _ -> }
            }
            val metrics = InlineRowMetrics(list.nodes, layouts, list.glueContext, Density(1f), list.contexts)
            assertEquals(mu, metrics.width(0, list.nodes.size), 0.005f, source)
        }
    }

    @Test
    fun everySubrowMatchesIndependentSequentialBinCancellation() {
        // All 512 triples, with explicit glue between atoms, test every break
        // boundary. Long bin chains exercise parity changes at a new line start.
        val sequences = buildList {
            for (a in AtomType.entries) for (b in AtomType.entries) for (c in AtomType.entries) add(listOf(a, b, c))
            for (length in 1..32) add(listOf(AtomType.ORD) + List(length) { AtomType.BIN } + AtomType.ORD)
        }
        for (script in listOf(false, true)) for (types in sequences) {
            val context = RenderContext(fontSize = 18.sp, color = Color.Black, mathStyle = if (script) MathStyle.SCRIPT else MathStyle.TEXT)
            val nodes = types.flatMap { type -> listOf(
                LatexNode.HSpace("1mu"),
                LatexNode.MathClass(emptyList(), LatexNode.MathClass.AtomClass.valueOf(type.name)),
            ) } + LatexNode.HSpace("1mu")
            val layouts = nodes.map { NodeLayout(1f, 0f, 0f) { _, _ -> } }
            val metrics = InlineRowMetrics(nodes, layouts, context, Density(1f))
            for (start in nodes.indices) for (end in start + 1..nodes.size) {
                val atoms = nodes.subList(start, end).filterIsInstance<LatexNode.MathClass>()
                val effective = atoms.map { AtomType.valueOf(it.atomClass.name) }.toMutableList()
                for (i in effective.indices) {
                    val current = effective[i]
                    val previous = effective.getOrNull(i - 1)
                    if (previous == AtomType.BIN && current in listOf(AtomType.REL, AtomType.CLOSE, AtomType.PUNCT)) effective[i - 1] = AtomType.ORD
                    else if (current == AtomType.BIN && (previous == null || previous in listOf(AtomType.BIN, AtomType.OP, AtomType.REL, AtomType.OPEN, AtomType.PUNCT))) effective[i] = AtomType.ORD
                }
                if (effective.lastOrNull() == AtomType.BIN) effective[effective.lastIndex] = AtomType.ORD
                val table = if (script) tightMu else normalMu
                val expected = end - start + effective.zipWithNext().sumOf { (l, r) -> table[l.ordinal][r.ordinal] }
                assertEquals(expected.toFloat(), metrics.width(start, end), 0.001f, "$types/$start:$end/script=$script")
            }
        }
    }
}
