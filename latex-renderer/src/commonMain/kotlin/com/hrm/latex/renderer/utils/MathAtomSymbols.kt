package com.hrm.latex.renderer.utils

import com.hrm.latex.parser.SymbolMap

/** Math-atom families of supported symbols, audited against KaTeX 0.16.22. */
internal object MathAtomSymbols {
    private val byName: Map<String, AtomType> = buildMap {
        listOf(
            "Alpha", "Bbbk", "Beta", "Chi", "Delta", "Epsilon",
            "Eta", "Finv", "Game", "Gamma", "Im", "Iota",
            "Kappa", "Lambda", "Mu", "Nu", "Omega", "Omicron",
            "P", "Phi", "Pi", "Psi", "Re", "Rho",
            "S", "Sigma", "Tau", "Theta", "Upsilon", "Vert",
            "Xi", "Zeta", "aleph", "alpha", "angle", "backprime",
            "backslash", "beta", "beth", "bigstar", "blacklozenge", "blacksquare",
            "blacktriangle", "blacktriangledown", "bot", "checkmark", "chi", "circledS",
            "clubsuit", "complement", "daleth", "degree", "delta", "diagdown",
            "diagup", "diamondsuit", "digamma", "ell", "emptyset", "epsilon",
            "eta", "eth", "exists", "flat", "forall", "gamma",
            "gimel", "hbar", "heartsuit", "hslash", "imath", "infty",
            "iota", "jmath", "kappa", "lambda", "lnot", "lozenge",
            "measuredangle", "mho", "mu", "nabla", "natural", "neg",
            "nexists", "nu", "omega", "omicron", "partial", "phi",
            "pi", "prime", "psi", "rho", "sharp", "sigma",
            "spadesuit", "sphericalangle", "square", "tau", "theta", "top",
            "triangle", "upsilon", "varDelta", "varGamma", "varLambda", "varOmega",
            "varPhi", "varPi", "varPsi", "varSigma", "varTheta", "varUpsilon",
            "varXi", "varepsilon", "varkappa", "varnothing", "varphi", "varpi",
            "varrho", "varsigma", "vartheta", "vdots", "vert", "wp",
            "xi", "zeta", "surd",
        ).forEach { put(it, AtomType.ORD) }
        listOf(
            "bigcap", "bigcup", "bigodot", "bigoplus", "bigotimes", "bigsqcup",
            "biguplus", "bigvee", "bigwedge", "coprod", "iiint", "iint",
            "int", "oiiint", "oiint", "oint", "prod", "sum", "smallint", "intop", "ointop",
        ).forEach { put(it, AtomType.OP) }
        listOf(
            "Cap", "Cup", "amalg", "ast", "barwedge", "bigcirc",
            "bigtriangledown", "bigtriangleup", "boxdot", "boxminus", "boxplus", "boxtimes",
            "bullet", "cap", "cdot", "centerdot", "circ", "circledast",
            "circledcirc", "circleddash", "cup", "curlyvee", "curlywedge", "dagger",
            "ddagger", "diamond", "div", "divideontimes", "dotplus", "doublebarwedge",
            "gtrdot", "intercal", "land", "leftthreetimes", "lessdot", "lor",
            "ltimes", "minus", "mp", "odot", "ominus", "oplus",
            "oslash", "otimes", "plus", "pm", "rightthreetimes", "rtimes",
            "setminus", "smallsetminus", "sqcap", "sqcup", "star", "times",
            "triangleleft", "triangleright", "uplus", "vee", "veebar", "wedge",
            "wr",
        ).forEach { put(it, AtomType.BIN) }
        listOf(
            "Bumpeq", "Downarrow", "Leftarrow", "Leftrightarrow", "Lleftarrow", "Longleftarrow",
            "Longleftrightarrow", "Longrightarrow", "Lsh", "Rightarrow", "Rrightarrow", "Rsh",
            "Subset", "Supset", "Uparrow", "Updownarrow", "Vdash", "Vvdash",
            "approx", "asymp", "backepsilon", "backsim", "backsimeq", "because",
            "between", "blacktriangleleft", "blacktriangleright", "bowtie", "bumpeq", "circeq",
            "circlearrowleft", "circlearrowright", "coloneq", "coloneqq", "cong", "curvearrowleft",
            "curvearrowright", "dashleftarrow", "dashrightarrow", "dashv", "dblcolon", "doteqdot",
            "downarrow", "downharpoonleft", "downharpoonright", "eqcirc", "eqcolon", "eqqcolon",
            "eqslantgtr", "eqslantless", "equals", "equiv", "fallingdotseq", "frown",
            "ge", "geq", "geqslant", "gets", "gg", "ggg",
            "gneq", "gnsim", "gtrapprox", "gtreqless", "gtrless", "gtrsim",
            "hookleftarrow", "hookrightarrow", "iff", "implies", "in", "le",
            "leftarrow", "leftarrowtail", "leftharpoondown", "leftharpoonup", "leftleftarrows", "leftrightarrow",
            "leftrightarrows", "leftrightharpoons", "leftrightsquigarrow", "leq", "leqq", "leqslant",
            "lessapprox", "lessgtr", "lesssim", "ll", "lll", "lneq",
            "lnsim", "longleftarrow", "longleftrightarrow", "longmapsto", "longrightarrow", "looparrowleft",
            "looparrowright", "lvertneqq", "mapsto", "mid", "models", "multimap",
            "nLeftarrow", "nLeftrightarrow", "nRightarrow", "nVDash", "nVdash", "ncong",
            "ne", "nearrow", "neq", "ngeq", "ngeqslant", "ngtr",
            "ni", "nleftarrow", "nleftrightarrow", "nleq", "nleqslant", "nless",
            "nmid", "notin", "nparallel", "nprec", "npreceq", "nrightarrow",
            "nsim", "nsubseteq", "nsubseteqq", "nsucc", "nsupseteq", "nsupseteqq",
            "ntriangleleft", "ntrianglelefteq", "ntriangleright", "ntrianglerighteq", "nvDash", "nvdash",
            "nwarrow", "owns", "parallel", "perp", "pitchfork", "prec",
            "precapprox", "preceq", "precnsim", "precsim", "propto", "rightarrow",
            "rightarrowtail", "rightharpoondown", "rightharpoonup", "rightleftarrows", "rightleftharpoons", "rightrightarrows",
            "rightsquigarrow", "risingdotseq", "searrow", "sim", "simeq", "smallfrown",
            "smallsmile", "smile", "sqsubset", "sqsubseteq", "sqsupset", "sqsupseteq",
            "subset", "subseteq", "subsetneq", "subsetneqq", "succ", "succapprox",
            "succeq", "succnsim", "succsim", "supset", "supseteq", "supsetneq",
            "supsetneqq", "swarrow", "therefore", "to", "trianglelefteq", "triangleq",
            "trianglerighteq", "twoheadleftarrow", "twoheadrightarrow", "uparrow", "updownarrow", "upharpoonleft",
            "upharpoonright", "upuparrows", "vDash", "varpropto", "varsubsetneq", "varsubsetneqq",
            "varsupsetneq", "varsupsetneqq", "vartriangleleft", "vartriangleright", "vdash",
            "doteq", "relbar", "Relbar",
        ).forEach { put(it, AtomType.REL) }
        listOf(
            "lVert", "langle", "lbrace", "lbrack", "lceil", "lfloor",
            "lgroup", "llbracket", "lmoustache", "lvert",
        ).forEach { put(it, AtomType.OPEN) }
        listOf(
            "rVert", "rangle", "rbrace", "rbrack", "rceil", "rfloor",
            "rgroup", "rmoustache", "rrbracket", "rvert",
        ).forEach { put(it, AtomType.CLOSE) }
        listOf(
            "cdotp", "ldotp", "colon",
        ).forEach { put(it, AtomType.PUNCT) }
        listOf(
            "cdots", "ddots", "dots", "ldots",
        ).forEach { put(it, AtomType.INNER) }
    }

    private val byUnicode: Map<String, AtomType> = buildMap {
        for ((name, type) in byName) {
            if (type != AtomType.ORD) SymbolMap.getSymbol(name)?.let { if (it !in this) put(it, type) }
        }
        put("−", AtomType.BIN)
    }

    fun classify(name: String, unicode: String): AtomType = byName[name] ?: byUnicode[unicode] ?: AtomType.ORD
}
