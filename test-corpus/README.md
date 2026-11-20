# Test corpus

Formula lists read by `CorpusTest` in `latex-parser/src/jvmTest`. Nothing here ships in a
published artifact.

One formula per line. Blank lines and lines whose first non-whitespace character is `#` are
comments.

## `ratex/`

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

`unsupported.txt` records formulas that parse with an error diagnostic, an `Error` node
or an unrecognized command. A test fails when a formula enters or leaves this list.
Regenerate the baseline after intentional changes.

```bash
UPDATE_CORPUS_BASELINE=1 ./gradlew :latex-parser:jvmTest --tests "*CorpusTest*"
```
