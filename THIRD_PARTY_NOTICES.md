# Third-Party Notices

This project bundles third-party assets. The project itself remains licensed
under the terms in [`LICENSE`](./LICENSE).

## KaTeX fonts

- Component: KaTeX font files
- Version: 0.16.11
- Source: https://github.com/KaTeX/KaTeX/tree/v0.16.11/src/fonts
- Font source project: https://github.com/KaTeX/katex-fonts
- Bundled location: `latex-renderer/src/commonMain/composeResources/font`
- License: MIT License
- Copyright: Copyright (c) 2013-2020 Khan Academy and other contributors;
  font source project copyright (c) 2018 Khan Academy

The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## RaTeX test corpus

- Component: RaTeX formula lists (test data only, not shipped)
- Version: commit c98ae9d128a3c0a26176b3f3541f3411c4e7c3d6 (`main` on 2026-11-16; v0.1.16 was the latest tag)
- Source: https://github.com/erweixin/RaTeX
- Bundled location: `test-corpus/ratex`
- License: MIT License
- Copyright: Copyright (c) erweixin
- `main.txt` and `parser.txt` include cases RaTeX adapted from KaTeX's tests (such as
  `test/katex-spec.ts` and `test/unicode-spec.ts`) and documentation (MIT License, Copyright (c)
  2013-2020 Khan Academy and other contributors; license text in the KaTeX fonts section above),
  and `mhchem.txt` includes examples from the mhchem manual

MIT License

Copyright (c) erweixin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
