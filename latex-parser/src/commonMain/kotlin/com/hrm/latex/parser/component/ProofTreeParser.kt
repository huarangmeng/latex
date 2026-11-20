package com.hrm.latex.parser.component

import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.ParseSession
import com.hrm.latex.parser.model.LatexNode
import com.hrm.latex.parser.model.LatexNode.ProofTree
import com.hrm.latex.parser.model.SourceRange
import com.hrm.latex.parser.tokenizer.LatexToken

private val inferenceArity = mapOf(
    "UnaryInfC" to 1, "UIC" to 1, "BinaryInfC" to 2, "BIC" to 2,
    "TrinaryInfC" to 3, "TIC" to 3, "QuaternaryInfC" to 4, "QuinaryInfC" to 5,
)

private val ruleCommands = mapOf(
    "singleLine" to ProofTree.Rule.SOLID, "solidLine" to ProofTree.Rule.SOLID,
    "dashedLine" to ProofTree.Rule.DASHED, "dottedLine" to ProofTree.Rule.DOTTED,
    "doubleLine" to ProofTree.Rule.DOUBLE, "noLine" to ProofTree.Rule.NONE,
)

private data class Branch(val proof: ProofTree, val depth: Int)

/** 推理命令按后缀顺序给出。每次从栈尾取出所需前提，并用新推理替换它们。 */
internal fun parseProofTree(context: LatexParserContext): LatexNode {
    val stream = context.tokenStream
    val stack = mutableListOf<Branch>()
    var defaultRule = ProofTree.Rule.SOLID
    var defaultRootAtTop = false
    var rule = defaultRule
    var rootAtTop = defaultRootAtTop
    var leftLabel: LatexNode? = null
    var rightLabel: LatexNode? = null

    fun failure(message: String, range: SourceRange, category: ParseDiagnostic.Category): LatexNode.Error {
        context.diagnostics.add(ParseDiagnostic(range, message, ParseDiagnostic.Severity.ERROR, category))
        context.accountForTreeDepth(stack.maxOfOrNull { it.depth } ?: 0)
        return LatexNode.Error(message, stack.map { it.proof }, range)
    }

    fun argument(command: LatexToken.Command): LatexNode {
        stream.skipWhitespace()
        if (!stream.isEOF() && stream.peek() !is LatexToken.EndEnvironment) {
            context.parseArgument()?.let { return it }
        }
        context.diagnostics.add(ParseDiagnostic(command.range, "Missing ${command.name} argument",
            ParseDiagnostic.Severity.ERROR, ParseDiagnostic.Category.INVALID_ARGUMENT))
        return LatexNode.Text("")
    }

    while (!stream.isEOF()) {
        val token = stream.peek() ?: break
        when (token) {
            is LatexToken.EndEnvironment -> {
                if (token.name != "prooftree") break
                stream.advance()
                val branch = stack.singleOrNull()
                if (branch != null && leftLabel == null && rightLabel == null) {
                    context.accountForTreeDepth(branch.depth)
                    return branch.proof
                }
                return failure("Proof tree must end with one conclusion and no unused labels", token.range,
                    ParseDiagnostic.Category.INVALID_ARGUMENT)
            }
            is LatexToken.Whitespace, is LatexToken.NewLine -> stream.advance()
            is LatexToken.Command -> {
                stream.advance()
                val baseName = token.name.removePrefix("always").replaceFirstChar { it.lowercaseChar() }
                val newRule = ruleCommands[baseName]
                val arity = inferenceArity[token.name]
                when {
                    newRule != null -> {
                        rule = newRule
                        if (token.name.startsWith("always")) defaultRule = newRule
                    }
                    baseName == "rootAtTop" || baseName == "rootAtBottom" -> {
                        rootAtTop = baseName == "rootAtTop"
                        if (token.name.startsWith("always")) defaultRootAtTop = rootAtTop
                    }
                    token.name == "AxiomC" || token.name == "AXC" -> {
                        val conclusion = argument(token)
                        stack.add(Branch(ProofTree(conclusion, rule = ProofTree.Rule.NONE,
                            sourceRange = stream.rangeFrom(token.range.start)), 1))
                    }
                    token.name == "LeftLabel" || token.name == "LL" -> leftLabel = argument(token)
                    token.name == "RightLabel" || token.name == "RL" -> rightLabel = argument(token)
                    arity != null && stack.size >= arity -> {
                        val conclusion = argument(token)
                        val lastBranches = stack.subList(stack.size - arity, stack.size)
                        val premises = lastBranches.map { it.proof }
                        val depth = lastBranches.maxOf { it.depth } + 1
                        if (depth > ParseSession.MAX_TREE_DEPTH) context.accountForTreeDepth(depth)
                        lastBranches.clear()
                        val range = stream.rangeFrom(premises.first().sourceRange?.start ?: token.range.start)
                        stack.add(Branch(ProofTree(conclusion, premises, leftLabel, rightLabel, rule, rootAtTop, range), depth))
                        leftLabel = null
                        rightLabel = null
                        rule = defaultRule
                        rootAtTop = defaultRootAtTop
                    }
                    else -> {
                        val message = if (arity != null) "${token.name} requires $arity premises"
                            else "Unexpected proof tree command ${token.name}"
                        context.diagnostics.add(ParseDiagnostic(token.range, message, ParseDiagnostic.Severity.ERROR,
                            ParseDiagnostic.Category.INVALID_ARGUMENT))
                    }
                }
            }
            else -> {
                stream.advance()
                context.diagnostics.add(ParseDiagnostic(token.range, "Expected proof tree command", ParseDiagnostic.Severity.ERROR,
                    ParseDiagnostic.Category.UNEXPECTED_TOKEN))
            }
        }
    }
    return failure("Missing \\end{prooftree}", stream.peek()?.range ?: SourceRange.EMPTY,
        ParseDiagnostic.Category.MISMATCHED_ENVIRONMENT)
}
