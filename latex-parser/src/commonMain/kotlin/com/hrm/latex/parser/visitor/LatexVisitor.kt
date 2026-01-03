package com.hrm.latex.parser.visitor

import com.hrm.latex.parser.model.LatexNode

/**
 * LaTeX AST 访问者接口
 * 用于遍历和处理 LaTeX 语法树
 */
interface LatexVisitor<T> {
    fun visitDocument(node: LatexNode.Document): T
    fun visitText(node: LatexNode.Text): T
    fun visitCommand(node: LatexNode.Command): T
    fun visitEnvironment(node: LatexNode.Environment): T
    fun visitGroup(node: LatexNode.Group): T
    fun visitSuperscript(node: LatexNode.Superscript): T
    fun visitSubscript(node: LatexNode.Subscript): T
    fun visitFraction(node: LatexNode.Fraction): T
    fun visitRoot(node: LatexNode.Root): T
    fun visitMatrix(node: LatexNode.Matrix): T
    fun visitArray(node: LatexNode.Array): T
    fun visitSpace(node: LatexNode.Space): T
    fun visitHSpace(node: LatexNode.HSpace): T
    fun visitNewLine(node: LatexNode.NewLine): T
    fun visitSymbol(node: LatexNode.Symbol): T
    fun visitOperator(node: LatexNode.Operator): T
    fun visitDelimited(node: LatexNode.Delimited): T
    fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): T
    fun visitAccent(node: LatexNode.Accent): T
    fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): T
    fun visitStack(node: LatexNode.Stack): T
    fun visitStyle(node: LatexNode.Style): T
    fun visitColor(node: LatexNode.Color): T
    fun visitMathStyle(node: LatexNode.MathStyle): T
    fun visitBigOperator(node: LatexNode.BigOperator): T
    fun visitAligned(node: LatexNode.Aligned): T
    fun visitCases(node: LatexNode.Cases): T
    fun visitSplit(node: LatexNode.Split): T
    fun visitMultline(node: LatexNode.Multline): T
    fun visitEqnarray(node: LatexNode.Eqnarray): T
    fun visitSubequations(node: LatexNode.Subequations): T
    fun visitBinomial(node: LatexNode.Binomial): T
    fun visitTextMode(node: LatexNode.TextMode): T
}

/**
 * 默认访问者实现，提供默认行为
 */
abstract class BaseLatexVisitor<T> : LatexVisitor<T> {
    
    protected abstract fun defaultVisit(node: LatexNode): T
    
    override fun visitDocument(node: LatexNode.Document): T {
        node.children.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitText(node: LatexNode.Text): T = defaultVisit(node)
    
    override fun visitCommand(node: LatexNode.Command): T {
        node.arguments.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitEnvironment(node: LatexNode.Environment): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitGroup(node: LatexNode.Group): T {
        node.children.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitSuperscript(node: LatexNode.Superscript): T {
        visit(node.base)
        visit(node.exponent)
        return defaultVisit(node)
    }
    
    override fun visitSubscript(node: LatexNode.Subscript): T {
        visit(node.base)
        visit(node.index)
        return defaultVisit(node)
    }
    
    override fun visitFraction(node: LatexNode.Fraction): T {
        visit(node.numerator)
        visit(node.denominator)
        return defaultVisit(node)
    }
    
    override fun visitRoot(node: LatexNode.Root): T {
        visit(node.content)
        node.index?.let { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitMatrix(node: LatexNode.Matrix): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }
    
    override fun visitArray(node: LatexNode.Array): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }
    
    override fun visitSpace(node: LatexNode.Space): T = defaultVisit(node)
    
    override fun visitHSpace(node: LatexNode.HSpace): T = defaultVisit(node)

    override fun visitNewLine(node: LatexNode.NewLine): T = defaultVisit(node)
    
    override fun visitSymbol(node: LatexNode.Symbol): T = defaultVisit(node)
    
    override fun visitOperator(node: LatexNode.Operator): T = defaultVisit(node)
    
    override fun visitDelimited(node: LatexNode.Delimited): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitManualSizedDelimiter(node: LatexNode.ManualSizedDelimiter): T = defaultVisit(node)
    
    override fun visitAccent(node: LatexNode.Accent): T {
        visit(node.content)
        return defaultVisit(node)
    }
    
    override fun visitExtensibleArrow(node: LatexNode.ExtensibleArrow): T {
        visit(node.content)
        node.below?.let { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitStack(node: LatexNode.Stack): T {
        visit(node.base)
        node.above?.let { visit(it) }
        node.below?.let { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitStyle(node: LatexNode.Style): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitColor(node: LatexNode.Color): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitMathStyle(node: LatexNode.MathStyle): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitBigOperator(node: LatexNode.BigOperator): T {
        node.subscript?.let { visit(it) }
        node.superscript?.let { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitAligned(node: LatexNode.Aligned): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }
    
    override fun visitCases(node: LatexNode.Cases): T {
        node.cases.forEach { (expr, cond) ->
            visit(expr)
            visit(cond)
        }
        return defaultVisit(node)
    }
    
    override fun visitSplit(node: LatexNode.Split): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }
    
    override fun visitMultline(node: LatexNode.Multline): T {
        node.lines.forEach { line -> visit(line) }
        return defaultVisit(node)
    }
    
    override fun visitEqnarray(node: LatexNode.Eqnarray): T {
        node.rows.forEach { row ->
            row.forEach { cell -> visit(cell) }
        }
        return defaultVisit(node)
    }
    
    override fun visitSubequations(node: LatexNode.Subequations): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    override fun visitBinomial(node: LatexNode.Binomial): T {
        visit(node.top)
        visit(node.bottom)
        return defaultVisit(node)
    }
    
    override fun visitTextMode(node: LatexNode.TextMode): T = defaultVisit(node)
    
    open fun visitBoxed(node: LatexNode.Boxed): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    open fun visitPhantom(node: LatexNode.Phantom): T {
        node.content.forEach { visit(it) }
        return defaultVisit(node)
    }
    
    /**
     * 访问任意节点
     */
    fun visit(node: LatexNode): T = when (node) {
        is LatexNode.Document -> visitDocument(node)
        is LatexNode.Text -> visitText(node)
        is LatexNode.Command -> visitCommand(node)
        is LatexNode.Environment -> visitEnvironment(node)
        is LatexNode.Group -> visitGroup(node)
        is LatexNode.Superscript -> visitSuperscript(node)
        is LatexNode.Subscript -> visitSubscript(node)
        is LatexNode.Fraction -> visitFraction(node)
        is LatexNode.Root -> visitRoot(node)
        is LatexNode.Matrix -> visitMatrix(node)
        is LatexNode.Array -> visitArray(node)
        is LatexNode.Space -> visitSpace(node)
        is LatexNode.HSpace -> visitHSpace(node)
        is LatexNode.NewLine -> visitNewLine(node)
        is LatexNode.Symbol -> visitSymbol(node)
        is LatexNode.Operator -> visitOperator(node)
        is LatexNode.Delimited -> visitDelimited(node)
        is LatexNode.ManualSizedDelimiter -> visitManualSizedDelimiter(node)
        is LatexNode.Accent -> visitAccent(node)
        is LatexNode.ExtensibleArrow -> visitExtensibleArrow(node)
        is LatexNode.Stack -> visitStack(node)
            is LatexNode.Style -> visitStyle(node)
            is LatexNode.Color -> visitColor(node)
            is LatexNode.MathStyle -> visitMathStyle(node)
            is LatexNode.BigOperator -> visitBigOperator(node)
        is LatexNode.Aligned -> visitAligned(node)
        is LatexNode.Cases -> visitCases(node)
        is LatexNode.Split -> visitSplit(node)
        is LatexNode.Multline -> visitMultline(node)
        is LatexNode.Eqnarray -> visitEqnarray(node)
        is LatexNode.Subequations -> visitSubequations(node)
        is LatexNode.Binomial -> visitBinomial(node)
        is LatexNode.TextMode -> visitTextMode(node)
        is LatexNode.Boxed -> visitBoxed(node)
        is LatexNode.Phantom -> visitPhantom(node)
    }
}
