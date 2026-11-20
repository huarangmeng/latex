# Test corpus

The JVM parser tests read their formula fixtures from this directory.

## Project syntax examples

[`syntax.txt`](syntax.txt) stores raw LaTeX grouped by syntax family. Each `@@ <id>`
line starts a case. The formula follows and may span multiple lines.
Add the matching AST or diagnostic assertion in
[`SyntaxCoverageCases.kt`](../latex-parser/src/jvmTest/kotlin/com/hrm/latex/parser/SyntaxCoverageCases.kt).
The symbol table supplies cases for symbols handled by direct lookup.

`SyntaxCoverageTest` checks that the catalog covers the parser's command and symbol inventories
and all built-in environments. It also checks the examples' semantics and compares the AST
after streaming and editing with a batch parse. The RaTeX corpus below gives broader coverage
of combined inputs.

```bash
./gradlew :latex-parser:jvmTest --tests "*SyntaxCoverageTest*"
```

## `ratex/`

One formula per line. Blank lines and lines whose first non-whitespace character is `#` are
comments.

Copied unchanged from [RaTeX](https://github.com/erweixin/RaTeX) at commit
`c98ae9d128a3c0a26176b3f3541f3411c4e7c3d6` (`main` on 2026-11-16; v0.1.16 was the latest tag),
MIT License, Copyright (c) erweixin. `main.txt` and `parser.txt` include cases RaTeX adapted from
KaTeX's tests and documentation, and `mhchem.txt` examples from the mhchem manual.

| File | Source in RaTeX |
|------|-----------------|
| `main.txt` | `tests/golden/test_cases.txt` (KaTeX-aligned golden suite) |
| `parser.txt` | `tools/parser_compare/test_cases.txt` |
| `mhchem.txt` | `tests/golden/test_case_ce.txt` |
| `centernot.txt` | `tests/golden/test_cases_centernot.txt` |
| `prooftree.txt` | `tests/golden/test_cases_prooftree.txt` |
| `slashed.txt` | `tests/golden/test_cases_slashed.txt` |

## Incremental parsing

`CorpusTest` requires every formula's streamed tokens and AST to match a batch parse
for every tested chunk size, including source ranges. The edit test independently compares
insertion, replacement and deletion against batch parsing.

## `baselines/`

`expected-diagnostics.tsv` lists inputs that must produce an error of the recorded
category, including malformed input and `\errmessage`. The copied formula files
retain literal backslash sequences such as `\n`. Separate tests exercise actual
line breaks and tabs.

`unsupported.txt` records unexpected errors, error nodes, and unrecognized commands
in the remaining corpus. The test requires every entry in both files to match the
current parser. Regenerate `unsupported.txt` after intentional changes.

```bash
UPDATE_CORPUS_BASELINE=1 ./gradlew :latex-parser:jvmTest --tests "*CorpusTest*"
```
