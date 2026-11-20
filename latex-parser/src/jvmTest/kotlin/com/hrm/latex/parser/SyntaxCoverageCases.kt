package com.hrm.latex.parser

import com.hrm.latex.parser.component.CommandParser
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.Accent.AccentType
import com.hrm.latex.parser.model.LatexNode.Binomial.BinomialStyle
import com.hrm.latex.parser.model.LatexNode.ExtensibleArrow.Direction
import com.hrm.latex.parser.model.LatexNode.FontSize.SizeType
import com.hrm.latex.parser.model.LatexNode.Fraction.FractionStyle
import com.hrm.latex.parser.model.LatexNode.MathStyle.MathStyleType
import com.hrm.latex.parser.model.LatexNode.Style.StyleType
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val syntaxFixtureSources: Map<String, String> by lazy {
    val text = testCorpusDirectory.resolve("syntax.txt").readText().replace("\r\n", "\n").removeSuffix("\n")
    check(text.startsWith("@@ ")) { "syntax.txt 必须以用例 ID 开头" }
    buildMap {
        // 分隔符换行属于文件格式，用例内的换行和空格都是 LaTeX 源码。
        for (entry in text.removePrefix("@@ ").split("\n@@ ")) {
            val id = entry.substringBefore('\n')
            val source = entry.substringAfter('\n', "")
            check(id.isNotBlank() && source.isNotEmpty()) { "syntax.txt 用例缺少 ID 或公式 $id" }
            check(put(id, source) == null) { "syntax.txt 用例 ID 重复 $id" }
        }
    }
}

internal data class SyntaxCoverageCase(
    val id: String,
    val source: String = syntaxFixtureSources.getValue(id),
    val commands: Set<String> = emptySet(),
    val environment: String? = null,
    val error: ParseDiagnostic.Category? = null,
    val verify: (ParseResult) -> Unit = {},
)

/**
 * 通过用例 ID 关联 syntax.txt 中的公式和这里的 AST、诊断断言。
 * 参数相同的命令共用断言，状态相关的用法单独验证。
 */
internal fun syntaxCoverageCases(): List<SyntaxCoverageCase> = buildList {
    fractionsAndDelimiters()
    fontsAndText()
    ornamentsAndSpacing()
    definitionsAndExpansion()
    referencesAndPackages()
    environmentsAndGrammar()
    assertEquals(map { it.id }.toSet(), syntaxFixtureSources.keys, "语法样例和断言的 ID 必须对应")

    // 字符表提供 Unicode 预期；与处理器同名的入口使用处理器的语义用例。
    val registered = CommandParser.syntaxCommands()
    for ((name, glyph) in SymbolMap.getAllSymbols()) {
        if (name !in registered) command<LatexNode.Symbol>(name, source = "\\$name") {
            assertEquals(glyph, it.unicode)
        }
    }
}

private fun MutableList<SyntaxCoverageCase>.fractionsAndDelimiters() {
    values("frac=NORMAL dfrac=DISPLAY tfrac=TEXT cfrac=CONTINUED splitfrac=RULELESS") { name, style ->
        command<LatexNode.Fraction>(name) { it.verifyFraction(FractionStyle.valueOf(style)) }
    }
    command<LatexNode.MathStyle>("splitdfrac") {
        assertEquals(MathStyleType.DISPLAY, it.mathStyleType)
        assertIs<LatexNode.Fraction>(it.content.single()).verifyFraction(FractionStyle.RULELESS)
    }
    command<LatexNode.Fraction>("genfrac") {
        it.verifyFraction(FractionStyle.DISPLAY)
    }
    values("binom=NORMAL tbinom=TEXT dbinom=DISPLAY") { name, style ->
        command<LatexNode.Binomial>(name) {
            assertEquals(BinomialStyle.valueOf(style), it.style)
            assertEquals("n", it.top.text())
            assertEquals("k", it.bottom.text())
        }
    }
    command<LatexNode.Root>("sqrt") {
        assertEquals("3", it.index?.text())
        assertIs<LatexNode.Superscript>(assertIs<LatexNode.Group>(it.content).children.single())
    }
    commands<LatexNode.BigOperator>(
        "sum prod int oint iint iiint smallint intop ointop bigcup bigcap bigvee bigwedge coprod " +
            "bigoplus bigotimes bigsqcup bigodot biguplus lim max min sup inf limsup liminf det gcd argmax " +
            "argmin deg dim ker arg hom Pr injlim projlim plim"
    ) { assertTrue(it.subscript != null && it.superscript != null) }
    commands<LatexNode.MathClass>("varlimsup varliminf varinjlim varprojlim") {
        assertEquals(LatexNode.MathClass.AtomClass.OP, it.atomClass)
    }
    commands<LatexNode.ManualSizedDelimiter>("llbracket rrbracket")
    commands<LatexNode.Delimited>("Set")
    commands<LatexNode.Group>("set")
    example("delimiter/left-middle-right", "left middle") {
        val node = it.single<LatexNode.Delimited>()
        assertEquals("⟨", node.left)
        assertEquals("⟩", node.right)
        assertTrue(node.content.any { child -> child is LatexNode.ManualSizedDelimiter && child.isMiddle })
    }
    for ((size, names) in listOf(
        1.2f to "big bigl bigr bigm", 1.8f to "Big Bigl Bigr Bigm",
        2.4f to "bigg biggl biggr biggm", 3.0f to "Bigg Biggl Biggr Biggm",
    )) commands<LatexNode.ManualSizedDelimiter>(names) {
        assertEquals(size, it.size)
        assertEquals("(", it.delimiter)
    }
}

private fun LatexNode.Fraction.verifyFraction(expectedStyle: FractionStyle) {
    assertEquals(expectedStyle, style)
    assertEquals("1", numerator.text())
    assertEquals("2", denominator.text())
}

private fun MutableList<SyntaxCoverageCase>.fontsAndText() {
    val styles = listOf(
        StyleType.BOLD to "mathbf bold", StyleType.TEXT_BOLD to "textbf", StyleType.TEXT_MEDIUM to "textmd",
        StyleType.BOLD_SYMBOL to "boldsymbol bm symbf", StyleType.ITALIC to "mathit symit",
        StyleType.MATH_NORMAL to "mathnormal", StyleType.TEXT_ITALIC to "textit textsw",
        StyleType.TEXT_SLANTED to "textsl", StyleType.TEXT_EMPHASIS to "emph",
        StyleType.SMALL_CAPS to "textsc textssc", StyleType.ROMAN to "mathrm symrm",
        StyleType.TEXT_ROMAN to "textrm", StyleType.TEXT_NORMAL to "textnormal",
        StyleType.TEXT_UPRIGHT to "textup", StyleType.SANS_SERIF to "mathsf symsf",
        StyleType.TEXT_SANS_SERIF to "textsf", StyleType.MONOSPACE to "mathtt", StyleType.TEXT_MONOSPACE to "texttt",
        StyleType.BLACKBOARD_BOLD to "mathbb Bbb", StyleType.FRAKTUR to "mathfrak", StyleType.SCRIPT to "mathscr",
        StyleType.CALLIGRAPHIC to "mathcal", StyleType.TEXT_UPPER_LOWER_CASE to "textulc",
    )
    for ((style, names) in styles) {
        commands<LatexNode.Style>(names) { assertEquals(style, it.styleType) }
    }
    commands<LatexNode.Style>("N natnums Z R Reals reals Complex cnums") {
        assertEquals(StyleType.BLACKBOARD_BOLD, it.styleType)
    }
    values(
        "bf=LEGACY_BOLD it=LEGACY_ITALIC rm=LEGACY_ROMAN sf=LEGACY_SANS_SERIF tt=LEGACY_MONOSPACE " +
            "cal=CALLIGRAPHIC frak=FRAKTUR scr=SCRIPT sc=LEGACY_SMALL_CAPS sl=LEGACY_SLANTED em=TEXT_EMPHASIS " +
            "swshape=TEXT_ITALIC sscshape=SMALL_CAPS rmfamily=TEXT_ROMAN sffamily=TEXT_SANS_SERIF " +
            "ttfamily=TEXT_MONOSPACE bfseries=TEXT_BOLD mdseries=TEXT_MEDIUM itshape=TEXT_ITALIC " +
            "slshape=TEXT_SLANTED upshape=TEXT_UPRIGHT scshape=SMALL_CAPS ulcshape=TEXT_UPPER_LOWER_CASE " +
            "normalshape=TEXT_NORMAL_SHAPE normalfont=TEXT_NORMAL"
    ) { name, style ->
        command<LatexNode.Style>(name) {
            assertEquals(StyleType.valueOf(style), it.styleType)
            assertEquals("Ab", it.text())
        }
    }
    commands<LatexNode.MathChoice>("mathchoice")
    values(
        "displaystyle=DISPLAY textstyle=TEXT scriptstyle=SCRIPT scriptscriptstyle=SCRIPT_SCRIPT " +
            "crampeddisplaystyle=CRAMPED_DISPLAY crampedtextstyle=CRAMPED_TEXT " +
            "crampedscriptstyle=CRAMPED_SCRIPT crampedscriptscriptstyle=CRAMPED_SCRIPT_SCRIPT"
    ) { name, style ->
        command<LatexNode.MathStyle>(name) {
            assertEquals(MathStyleType.valueOf(style), it.mathStyleType)
            assertEquals("x", it.text())
        }
    }
    command<LatexNode.MathStyle>("cramped") {
        assertEquals(MathStyleType.CRAMPED_SCRIPT, it.mathStyleType)
    }
    values(
        "tiny=TINY sixptsize=SIX_POINT scriptsize=SCRIPT_SIZE footnotesize=FOOTNOTE_SIZE small=SMALL " +
            "normalsize=NORMAL_SIZE large=LARGE Large=LARGE_2 LARGE=LARGE_3 huge=HUGE Huge=HUGE_2"
    ) { name, size ->
        command<LatexNode.FontSize>(name) {
            assertEquals(SizeType.valueOf(size), it.sizeType)
            assertEquals("x", it.text())
        }
    }
    commands<LatexNode.Style>("TeX LaTeX KaTeX") { assertEquals(StyleType.TEXT_ROMAN, it.styleType) }
    commands<LatexNode.TextMode>("copyright textcopyright") { assertEquals("©", it.text) }
    values(
        "AA=Å aa=å AE=Æ ae=æ OE=Œ oe=œ O=Ø o=ø ss=ß i=ı j=ȷ l=ł L=Ł lq=‘ rq=’ dag=† ddag=‡ " +
            "textdegree=° textemdash=— textendash=– textbackslash=\\ textasciitilde=~ textasciicircum=^ " +
            "textbraceleft={ textbraceright=} textbar=| textbardbl=‖ textunderscore=_ textdollar=$ " +
            "textless=< textgreater=> textellipsis=… textquoteleft=‘ textquoteright=’ textquotedblleft=“ " +
            "textquotedblright=” textdagger=† textdaggerdbl=‡ textregistered=® textsterling=£"
    ) { name, value -> command<LatexNode.TextMode>(name) { assertEquals(value, it.text) } }
    for (kind in LatexNode.TextAccent.Kind.entries) {
        if (kind == LatexNode.TextAccent.Kind.TIE) {
            command<LatexNode.TextAccent>(kind.command) {
                assertEquals(kind, it.accent)
                assertEquals("oo", it.text())
            }
        } else {
            command<LatexNode.Group>(kind.command) {
                val accent = assertIs<LatexNode.TextAccent>(it.children.single())
                assertEquals(kind, accent.accent)
                assertEquals("a", accent.text())
            }
        }
    }
    commands<LatexNode.TextMode>("text mbox hbox intertext shortintertext") { assertEquals("Ab", it.text) }
    example("mode/TextOrMath-math", "TextOrMath") { assertEquals("math", it.text()) }
    example("mode/TextOrMath-text", "text TextOrMath") {
        assertEquals("text", it.single<LatexNode.TextMode>().text())
    }
    commands<LatexNode.TextDirection>("RLE textarabic texthebrew") {
        assertEquals(LatexNode.TextDirection.Direction.RTL, it.direction)
    }
    commands<LatexNode.TextDirection>("LRE") {
        assertEquals(LatexNode.TextDirection.Direction.LTR, it.direction)
    }
}

private fun MutableList<SyntaxCoverageCase>.ornamentsAndSpacing() {
    values(
        "hat=HAT tilde=TILDE widetilde=TILDE bar=OVERLINE overline=OVERLINE underline=UNDERLINE " +
            "dot=DOT ddot=DDOT dddot=DDDOT ddddot=DDDDOT centernot=CENTER_NOT slashed=SLASHED vec=VEC " +
            "overbrace=OVERBRACE underbrace=UNDERBRACE widehat=WIDEHAT overrightarrow=OVERRIGHTARROW " +
            "overleftarrow=OVERLEFTARROW cancel=CANCEL bcancel=BCANCEL xcancel=XCANCEL grave=GRAVE acute=ACUTE " +
            "check=CHECK breve=BREVE ring=RING mathring=RING overbracket=OVERBRACKET underbracket=UNDERBRACKET " +
            "widecheck=WIDECHECK overleftrightarrow=OVERLEFTRIGHTARROW underleftarrow=UNDERLEFTARROW " +
            "underrightarrow=UNDERRIGHTARROW overparen=OVERPAREN underparen=UNDERPAREN Overrightarrow=OVER_RIGHT_DOUBLE " +
            "overleftharpoon=OVER_LEFT_HARPOON overrightharpoon=OVER_RIGHT_HARPOON underleftrightarrow=UNDER_BOTH_ARROWS " +
            "overgroup=OVER_GROUP undergroup=UNDER_GROUP overlinesegment=OVER_SEGMENT underlinesegment=UNDER_SEGMENT " +
            "utilde=UNDER_TILDE underbar=UNDERLINE"
    ) { name, type ->
        command<LatexNode.Accent>(name) {
            assertEquals(AccentType.valueOf(type), it.accentType)
            assertEquals("x", it.content.text())
        }
    }
    command<LatexNode.Accent>("skew") { assertEquals(2f, it.skewMu) }
    values(
        "xrightarrow=RIGHT xleftarrow=LEFT xleftrightarrow=BOTH xrightleftarrows=RIGHT_LEFT_ARROWS " +
            "xtofrom=TO_FROM xhookrightarrow=HOOK_RIGHT xhookleftarrow=HOOK_LEFT xRightarrow=RIGHT_DOUBLE " +
            "xLeftarrow=LEFT_DOUBLE xLeftrightarrow=BOTH_DOUBLE xmapsto=MAPSTO xlongequal=EQUAL " +
            "xrightharpoonup=RIGHT_HARPOON_UP xrightharpoondown=RIGHT_HARPOON_DOWN xleftharpoonup=LEFT_HARPOON_UP " +
            "xleftharpoondown=LEFT_HARPOON_DOWN xrightleftharpoons=RIGHT_LEFT_HARPOONS " +
            "xleftrightharpoons=LEFT_RIGHT_HARPOONS xtwoheadrightarrow=TWO_HEAD_RIGHT xtwoheadleftarrow=TWO_HEAD_LEFT"
    ) { name, direction ->
        command<LatexNode.ExtensibleArrow>(name) {
            assertEquals(Direction.valueOf(direction), it.direction)
            assertEquals("above", it.content.text())
            assertEquals("below", it.below?.text())
        }
    }
    commands<LatexNode.Stack>("overset underset")
    commands<LatexNode.MathClass>("stackrel")
    command<LatexNode.MathClass>("@binrel") { assertEquals(LatexNode.MathClass.AtomClass.BIN, it.atomClass) }
    command<LatexNode.MathClass>("pmb") { assertEquals(LatexNode.MathClass.AtomClass.REL, it.atomClass) }
    commands<LatexNode.MathClass>(
        "ordinarycolon vcentcolon ratio thickapprox thicksim Coloneqq coloncolonequals Coloneq coloncolonminus Eqqcolon " +
            "equalscoloncolon Eqcolon minuscoloncolon colonapprox Colonapprox coloncolonapprox colonsim Colonsim " +
            "coloncolonsim approxcolon approxcoloncolon simcolon simcoloncolon joinrel"
    ) { assertEquals(LatexNode.MathClass.AtomClass.REL, it.atomClass) }
    for ((name, type) in listOf(
        "mathord" to LatexNode.MathClass.AtomClass.ORD, "mathbin" to LatexNode.MathClass.AtomClass.BIN,
        "mathrel" to LatexNode.MathClass.AtomClass.REL, "mathopen" to LatexNode.MathClass.AtomClass.OPEN,
        "mathclose" to LatexNode.MathClass.AtomClass.CLOSE, "mathpunct" to LatexNode.MathClass.AtomClass.PUNCT,
        "mathinner" to LatexNode.MathClass.AtomClass.INNER,
    )) command<LatexNode.MathClass>(name) { assertEquals(type, it.atomClass) }

    command<LatexNode.Space>(" ") { assertEquals(LatexNode.Space.SpaceType.NORMAL, it.type) }
    for ((type, names) in listOf(
        LatexNode.Space.SpaceType.NORMAL to "space", LatexNode.Space.SpaceType.NON_BREAKING to "nobreakspace",
        LatexNode.Space.SpaceType.THIN to ", thinspace", LatexNode.Space.SpaceType.MEDIUM to ": > medspace",
        LatexNode.Space.SpaceType.THICK to "; thickspace", LatexNode.Space.SpaceType.QUAD to "quad",
        LatexNode.Space.SpaceType.QQUAD to "qquad", LatexNode.Space.SpaceType.NEGATIVE_THIN to "! negthinspace",
    )) commands<LatexNode.Space>(names) { assertEquals(type, it.type) }
    commands<LatexNode.HSpace>("enspace enskip negmedspace negthickspace allowbreak nobreak")
    commands<LatexNode.HSpace>("hspace kern hskip") { assertEquals("1em", it.dimension) }
    commands<LatexNode.HSpace>("mkern mskip") { assertEquals("3mu", it.dimension) }
    commands<LatexNode.HSpace>("tmspace")
    commands<LatexNode.NewLine>("cr newline")
    example("color/definitions", "definecolor colorlet color") {
        assertEquals("#336699", it.document.children.filterIsInstance<LatexNode.Color>().single().color)
    }
    commands<LatexNode.Color>("textcolor")
    commands<LatexNode.Color>("blue orange pink red green purple")
    commands<LatexNode.ColorBox>("colorbox")
    commands<LatexNode.ColorBox>("fcolorbox")
    commands<LatexNode.VPhantom>("mathstrut")
    commands<LatexNode.Enclose>("circled textcircled sout angl phase")
    commands<LatexNode.Enclose>("angln")
    commands<LatexNode.Enclose>("enclose")
    commands<LatexNode.Enclose>("bbox")
    command<LatexNode.Layout>("vcenter") { assertEquals(LatexNode.Layout.LayoutType.V_CENTER, it.layoutType) }
    commands<LatexNode.Boxed>("boxed Aboxed fbox")
    commands<LatexNode.Phantom>("phantom")
    commands<LatexNode.Smash>("smash")
    commands<LatexNode.VPhantom>("vphantom")
    commands<LatexNode.HPhantom>("hphantom")
    commands<LatexNode.Negation>("not")
    commands<LatexNode.MathLap>("mathclap clap mathllap llap mathrlap rlap")
    commands<LatexNode.Layout>("raisebox")
    commands<LatexNode.Layout>("rule")
    commands<LatexNode.HSpace>("MoveEqLeft")
    commands<LatexNode.Html>("htmlClass htmlId")
    commands<LatexNode.Html>("htmlData")
    command<LatexNode.Html>("htmlStyle") { assertEquals("red", it.color) }
    command<LatexNode.InlineImage>("includegraphics") {
        assertEquals("a%20b.png", it.source)
        assertEquals("sample", it.alt)
    }
    commands<LatexNode.Hyperlink>("href")
    commands<LatexNode.Hyperlink>("url")
}

private inline fun <reified T : LatexNode> MutableList<SyntaxCoverageCase>.command(
    name: String, source: String? = null, crossinline verify: (T) -> Unit = {},
) {
    val id = "command/" + if (name == " ") "control-space" else name
    add(SyntaxCoverageCase(id, source ?: syntaxFixtureSources.getValue(id), setOf(name)) { result ->
        val node = result.document.children.firstOrNull { it is T }
        verify(assertIs<T>(node, "预期顶层包含 " + T::class.simpleName + "，实际为 " + result.document.children))
    })
}

private inline fun <reified T : LatexNode> MutableList<SyntaxCoverageCase>.commands(
    names: String, crossinline verify: (T) -> Unit = {},
) {
    for (name in names.split(' ')) command<T>(name) { verify(it) }
}

private fun MutableList<SyntaxCoverageCase>.example(
    id: String, commands: String = "", verify: (ParseResult) -> Unit,
) {
    add(SyntaxCoverageCase(id, commands = commands.split(' ').filter(String::isNotEmpty).toSet(), verify = verify))
}

private inline fun values(table: String, write: (String, String) -> Unit) {
    for (entry in table.split(' ')) write(entry.substringBefore('='), entry.substringAfter('='))
}

private inline fun <reified T : LatexNode> ParseResult.single(): T = assertIs<T>(document.children.single())

private fun ParseResult.text(): String = document.text()

private fun LatexNode.text(): String = fold("") { text, node ->
    text + when (node) {
        is LatexNode.Text -> node.content
        is LatexNode.TextMode -> node.text
        is LatexNode.Symbol -> node.unicode
        else -> ""
    }
}

private fun MutableList<SyntaxCoverageCase>.definitionsAndExpansion() {
    for (name in listOf("newcommand", "renewcommand", "providecommand")) {
        example("definition/$name", name) {
            assertTrue(it.document.children.any { node -> node is LatexNode.NewCommand && node.numArgs == 1 })
            assertEquals("2+1", it.text())
        }
    }
    for (name in listOf("def", "gdef", "edef", "xdef")) {
        example("definition/$name", name) {
            assertEquals("2+1", it.text())
        }
    }
    example("definition/let", "let") { assertEquals("α", it.text()) }
    example("definition/futurelet", "futurelet") {
        assertEquals("12+1", it.text())
    }
    example("definition/global", "global") { assertEquals("x", it.text()) }
    example("definition/relax", "relax") { assertEquals("x", it.text()) }
    example("diagnostic/message", "message") {
        assertEquals(ParseDiagnostic.Severity.INFO, it.diagnostics.single().severity)
        assertEquals("hello", it.diagnostics.single().message)
        assertEquals("x", it.text())
    }
    add(SyntaxCoverageCase("diagnostic/errmessage", commands = setOf("errmessage"),
        error = ParseDiagnostic.Category.GENERAL) { assertEquals("hello", it.errors.single().message) })
    example("definition/operator", "DeclareMathOperator") {
        assertTrue(it.document.children.any { node -> node is LatexNode.OperatorName && node.name == "rank" })
    }
    example("definition/paired-delimiter", "DeclarePairedDelimiter") {
        assertTrue(it.document.children.any { node -> node is LatexNode.Delimited })
    }
    example("definition/paired-delimiter-with-arguments", "DeclarePairedDelimiterX") {
        assertEquals("x,y", it.text())
    }
    example("definition/environments",
        "newenvironment renewenvironment") { assertEquals("cxd", it.text()) }
    command<LatexNode.TextMode>("char") { assertEquals("☺", it.text) }
    example("definition/mathcode", "mathcode") {
        assertEquals("axb", it.text())
    }
    example("group/explicit", "begingroup endgroup bgroup egroup") {
        assertEquals("ab", it.text())
    }
    example("expansion/expandafter", "expandafter csname endcsname") {
        assertEquals("x", it.text())
    }
    example("expansion/noexpand", "noexpand") {
        assertEquals("y", it.text())
    }
    example("expansion/conditional", "ifdefined ifx else fi") {
        assertEquals("y", it.text())
    }
    example("expansion/mathpalette", "mathpalette") {
        assertTrue(it.document.children.any { node -> node is LatexNode.MathChoice })
    }
    example("expansion/helpers",
        "@firstoftwo @secondoftwo @ifnextchar @ifstar") { assertEquals("abczd", it.text()) }
    example("script/plain-aliases", "sp sb") {
        val sup = it.single<LatexNode.Superscript>()
        assertIs<LatexNode.Subscript>(sup.base)
    }
}

private fun MutableList<SyntaxCoverageCase>.referencesAndPackages() {
    commands<LatexNode.HLine>("hline hdashline")
    example("array/spacing", "setlength arraycolsep") {
        val array = it.document.children.filterIsInstance<LatexNode.Array>().single()
        assertEquals(1, array.rows.size)
    }
    commands<LatexNode.CLine>("cline")
    commands<LatexNode.Multicolumn>("multicolumn")
    commands<LatexNode.Label>("label")
    commands<LatexNode.Ref>("ref")
    commands<LatexNode.EqRef>("eqref")
    command<LatexNode.Tag>("tag") { assertTrue(it.starred) }
    commands<LatexNode.Tag>("notag nonumber")
    commands<LatexNode.Substack>("substack")
    for (operator in ("sin cos tan cot sec csc arcsin arccos arctan sinh cosh tanh coth ln log exp lg sgn lcm " +
        "arctg arcctg cosec cotg ctg cth tg ch sh th").split(' ')) {
        command<LatexNode.Operator>(operator) { assertEquals(operator, it.op) }
    }
    commands<LatexNode.SideSet>("sideset")
    commands<LatexNode.Tensor>("tensor")
    commands<LatexNode.Tensor>("indices")
    commands<LatexNode.Prescript>("prescript")
    commands<LatexNode.OperatorName>("operatorname operatornamewithlimits")
    commands<LatexNode.BigOperator>("mathop")
    commands<LatexNode.ModOperator>("bmod mod")
    commands<LatexNode.ModOperator>("pmod pod")
    commands<LatexNode.Symbol>("dotsb dotsm") { assertEquals("⋯", it.unicode) }
    commands<LatexNode.Symbol>("dotsc dotso") { assertEquals("…", it.unicode) }
    command<LatexNode.Group>("dotsx") {
        assertEquals("…", assertIs<LatexNode.Symbol>(it.children[0]).unicode)
        assertEquals(LatexNode.Space.SpaceType.THIN, assertIs<LatexNode.Space>(it.children[1]).type)
    }
    command<LatexNode.Group>("dotsi") {
        assertEquals(LatexNode.Space.SpaceType.NEGATIVE_THIN, assertIs<LatexNode.Space>(it.children[0]).type)
        assertEquals("⋯", assertIs<LatexNode.Symbol>(it.children[1]).unicode)
    }
    command<LatexNode.Symbol>("dots") { assertEquals("…", it.unicode) }
    commands<LatexNode.SectionHeading>("section subsection subsubsection paragraph subparagraph")
    commands<LatexNode.Delimited>("abs norm bra ket braket Bra Ket Braket eval quantity")
    commands<LatexNode.Delimited>("comm anticomm")
    commands<LatexNode.Style>("vb")
    commands<LatexNode.Accent>("va")
    commands<LatexNode.Fraction>("dv pdv")
    commands<LatexNode.Group>("num")
    commands<LatexNode.Style>("si unit") {
        assertEquals(StyleType.ROMAN, it.styleType)
        assertEquals("m/s", it.text())
    }
    commands<LatexNode.Group>("SI qty")
    commands<LatexNode.Group>("numlist")
    commands<LatexNode.Group>("qtylist")
    commands<LatexNode.Group>("numrange")
    commands<LatexNode.Group>("qtyrange")
    commands<LatexNode.Group>("ang")
    commands<LatexNode.Group>("pu")
    command<LatexNode.Symbol>("bond") { assertEquals("≡", it.unicode) }
    values(
        "metre=m meter=m second=s gram=g kilogram=kg ampere=A kelvin=K mole=mol candela=cd hertz=Hz " +
            "newton=N pascal=Pa joule=J watt=W coulomb=C volt=V farad=F ohm=Ω siemens=S weber=Wb tesla=T " +
            "henry=H lumen=lm lux=lx becquerel=Bq gray=Gy sievert=Sv katal=kat litre=L liter=L degreeCelsius=°C " +
            "kilo=k mega=M giga=G centi=c milli=m micro=μ nano=n per=/ squared=² cubed=³"
    ) { name, value -> command<LatexNode.Text>(name) { assertEquals(value, it.content) } }
    commands<LatexNode.Group>("ce cf") {
        assertEquals("2", assertIs<LatexNode.Text>(assertIs<LatexNode.Subscript>(it.children.first()).index).content)
    }
    example("chemistry/circa", "ce") {
        val symbol = assertIs<LatexNode.Group>(it.single<LatexNode.Group>().children.first())
        assertEquals("∼", assertIs<LatexNode.Symbol>(symbol.children.single()).unicode)
    }
    example("chemistry/in-text", "text ce") {
        val text = it.single<LatexNode.TextMode>()
        val subscripts = text.fold(emptyList<LatexNode.Subscript>()) { nodes, node ->
            if (node is LatexNode.Subscript) nodes + node else nodes
        }
        assertEquals("H", subscripts.single().base.text())
        assertEquals("2", subscripts.single().index.text())
    }
}

private fun MutableList<SyntaxCoverageCase>.environmentsAndGrammar() {
    environments<LatexNode.Matrix>(
        "matrix pmatrix bmatrix Bmatrix vmatrix Vmatrix matrix* pmatrix* bmatrix* Bmatrix* vmatrix* Vmatrix* smallmatrix smallmatrix*"
    ) { assertEquals(2, it.rows.size) }
    environments<LatexNode.Array>("array") { assertEquals(2, it.rows.size) }
    environments<LatexNode.Array>("subarray") { assertEquals(2, it.rows.size) }
    environments<LatexNode.Tabular>("tabular")
    environments<LatexNode.Aligned>("align aligned align* gather gathered gather* flalign flalign*")
    environments<LatexNode.Aligned>("alignat alignat* alignedat")
    environments<LatexNode.Split>("split")
    environments<LatexNode.Multline>("multline multline* multlined")
    environments<LatexNode.Eqnarray>("eqnarray eqnarray*")
    environments<LatexNode.Subequations>("subequations")
    environments<LatexNode.Cases>("cases cases* dcases dcases* rcases rcases*")
    environments<LatexNode.Environment>("equation equation* displaymath")
    environments<LatexNode.TextDirection>("RTL LTR")
    environments<LatexNode.ProofTree>(
        "prooftree"
    ) {
        assertEquals(2, it.premises.size)
        assertEquals(LatexNode.ProofTree.Rule.DASHED, it.rule)
    }
    for ((name, arity) in listOf(
        "UnaryInfC" to 1, "UIC" to 1, "BinaryInfC" to 2, "TrinaryInfC" to 3,
        "TIC" to 3, "QuaternaryInfC" to 4, "QuinaryInfC" to 5,
    )) {
        example("proof/$name") {
            val tree = it.single<LatexNode.ProofTree>()
            assertEquals(List(arity) { "P" }, tree.premises.map { premise ->
                assertIs<LatexNode.ProofTree>(premise).conclusion.text()
            })
            assertEquals("Q", tree.conclusion.text())
            assertEquals("L", tree.leftLabel?.text())
            assertEquals("R", tree.rightLabel?.text())
            assertTrue(tree.rootAtTop)
        }
    }

    example("syntax/frac-unbraced", "frac") {
        val node = it.single<LatexNode.Fraction>()
        assertEquals("1", assertIs<LatexNode.Text>(node.numerator).content)
        assertEquals("2", assertIs<LatexNode.Text>(node.denominator).content)
    }
    for ((args, style) in listOf("over" to FractionStyle.NORMAL, "atop" to FractionStyle.RULELESS,
        "above 1pt" to FractionStyle.NORMAL, "above 0pt" to FractionStyle.RULELESS,
    )) {
        example("syntax/infix-$args") {
            assertIs<LatexNode.Fraction>(it.single<LatexNode.Group>().children.single()).verifyFraction(style)
        }
    }
    example("syntax/infix-choose") {
        val binomial = assertIs<LatexNode.Binomial>(it.single<LatexNode.Group>().children.single())
        assertEquals("n", binomial.top.text())
        assertEquals("k", binomial.bottom.text())
    }
    for ((name, left, right) in listOf(Triple("brack", "[", "]"), Triple("brace", "{", "}"))) {
        example("syntax/infix-$name") {
            val pair = assertIs<LatexNode.Delimited>(it.single<LatexNode.Group>().children.single())
            assertEquals(left, pair.left)
            assertEquals(right, pair.right)
            assertIs<LatexNode.Fraction>(pair.content.single()).verifyFraction(FractionStyle.RULELESS)
        }
    }
    example("syntax/scripts") {
        val sup = it.single<LatexNode.Superscript>()
        assertIs<LatexNode.Subscript>(sup.base)
    }
    example("syntax/operator-limits") {
        assertEquals(LatexNode.BigOperator.LimitsMode.LIMITS, it.single<LatexNode.BigOperator>().limitsMode)
    }
    example("syntax/operator-nolimits") {
        assertEquals(LatexNode.BigOperator.LimitsMode.NOLIMITS, it.single<LatexNode.BigOperator>().limitsMode)
    }
    example("syntax/verb") { assertEquals("a_b%", it.single<LatexNode.TextMode>().text) }
    example("syntax/verb-star") { assertEquals("a␣b", it.single<LatexNode.TextMode>().text) }
    example("syntax/comment-newline") { assertEquals("ab", it.single<LatexNode.TextMode>().text) }
    example("syntax/inline-dollar") { it.single<LatexNode.InlineMath>() }
    example("syntax/inline-parentheses") { it.single<LatexNode.InlineMath>() }
    example("syntax/display-dollar") { it.single<LatexNode.DisplayMath>() }
    example("syntax/display-brackets") { it.single<LatexNode.DisplayMath>() }
    example("syntax/line-break") { assertTrue(it.document.children[1] is LatexNode.NewLine) }

    add(SyntaxCoverageCase("diagnostic/unknown-command", error = ParseDiagnostic.Category.UNKNOWN_COMMAND))
    add(SyntaxCoverageCase("diagnostic/missing-brace", error = ParseDiagnostic.Category.MISSING_BRACE))
    add(SyntaxCoverageCase("diagnostic/unexpected-environment", error = ParseDiagnostic.Category.MISMATCHED_ENVIRONMENT))
}

private inline fun <reified T : LatexNode> MutableList<SyntaxCoverageCase>.environments(
    names: String, crossinline verify: (T) -> Unit = {},
) {
    for (name in names.split(' ')) {
        add(SyntaxCoverageCase("environment/$name", environment = name) {
            verify(it.single<T>())
        })
    }
}
