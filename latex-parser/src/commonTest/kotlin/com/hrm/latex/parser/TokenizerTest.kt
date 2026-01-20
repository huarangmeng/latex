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


package com.hrm.latex.parser

import com.hrm.latex.parser.tokenizer.LatexToken
import com.hrm.latex.parser.tokenizer.LatexTokenizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenizerTest {
    
    @Test
    fun testSimpleText() {
        val tokenizer = LatexTokenizer("hello")
        val tokens = tokenizer.tokenize()
        
        assertEquals(2, tokens.size) // text + EOF
        assertTrue(tokens[0] is LatexToken.Text)
        assertTrue(tokens[1] is LatexToken.EOF)
    }
    
    @Test
    fun testCommand() {
        val tokenizer = LatexTokenizer("\\alpha")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens[0] is LatexToken.Command)
        assertEquals("alpha", (tokens[0] as LatexToken.Command).name)
    }
    
    @Test
    fun testBraces() {
        val tokenizer = LatexTokenizer("{}")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens[0] is LatexToken.LeftBrace)
        assertTrue(tokens[1] is LatexToken.RightBrace)
    }
    
    @Test
    fun testBrackets() {
        val tokenizer = LatexTokenizer("[]")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens[0] is LatexToken.LeftBracket)
        assertTrue(tokens[1] is LatexToken.RightBracket)
    }
    
    @Test
    fun testSuperscript() {
        val tokenizer = LatexTokenizer("x^2")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens.any { it is LatexToken.Superscript })
    }
    
    @Test
    fun testSubscript() {
        val tokenizer = LatexTokenizer("x_i")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens.any { it is LatexToken.Subscript })
    }
    
    @Test
    fun testAmpersand() {
        val tokenizer = LatexTokenizer("a & b")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens.any { it is LatexToken.Ampersand })
    }
    
    @Test
    fun testBeginEnvironment() {
        val tokenizer = LatexTokenizer("\\begin{matrix}")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens[0] is LatexToken.BeginEnvironment)
        assertEquals("matrix", (tokens[0] as LatexToken.BeginEnvironment).name)
    }
    
    @Test
    fun testEndEnvironment() {
        val tokenizer = LatexTokenizer("\\end{matrix}")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens[0] is LatexToken.EndEnvironment)
        assertEquals("matrix", (tokens[0] as LatexToken.EndEnvironment).name)
    }
    
    @Test
    fun testNewLine() {
        val tokenizer = LatexTokenizer("\\\\")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens[0] is LatexToken.NewLine)
    }
    
    @Test
    fun testWhitespace() {
        val tokenizer = LatexTokenizer("a b")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens.any { it is LatexToken.Whitespace })
    }
    
    @Test
    fun testComplexExpression() {
        val tokenizer = LatexTokenizer("\\frac{a}{b}")
        val tokens = tokenizer.tokenize()
        
        // \frac { a } { b } EOF
        assertTrue(tokens[0] is LatexToken.Command)
        assertTrue(tokens[1] is LatexToken.LeftBrace)
        assertTrue(tokens[2] is LatexToken.Text)
        assertTrue(tokens[3] is LatexToken.RightBrace)
        assertTrue(tokens[4] is LatexToken.LeftBrace)
        assertTrue(tokens[5] is LatexToken.Text)
        assertTrue(tokens[6] is LatexToken.RightBrace)
        assertTrue(tokens[7] is LatexToken.EOF)
    }
    
    @Test
    fun testMultipleCommands() {
        val tokenizer = LatexTokenizer("\\alpha\\beta")
        val tokens = tokenizer.tokenize()
        
        val commands = tokens.filterIsInstance<LatexToken.Command>()
        assertEquals(2, commands.size)
        assertEquals("alpha", commands[0].name)
        assertEquals("beta", commands[1].name)
    }
    
    @Test
    fun testEscapedCharacters() {
        val tokenizer = LatexTokenizer("\\{ \\}")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens[0] is LatexToken.Command)
        assertTrue(tokens[2] is LatexToken.Command)
    }
    
    @Test
    fun testEmptyString() {
        val tokenizer = LatexTokenizer("")
        val tokens = tokenizer.tokenize()
        
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is LatexToken.EOF)
    }
    
    @Test
    fun testMixedContent() {
        val tokenizer = LatexTokenizer("E = mc^2")
        val tokens = tokenizer.tokenize()
        
        assertTrue(tokens.any { it is LatexToken.Text })
        assertTrue(tokens.any { it is LatexToken.Superscript })
        assertTrue(tokens.last() is LatexToken.EOF)
    }
}
