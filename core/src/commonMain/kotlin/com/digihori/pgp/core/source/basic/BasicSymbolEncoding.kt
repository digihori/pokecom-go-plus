package com.digihori.pgp.core.source.basic

/** Machine character codes, independent of BASIC keyword tokens. */
internal object BasicSymbolEncoding {
    val old = mapOf(BasicSpecialSymbol.PI to 0x19, BasicSpecialSymbol.SQUARE_ROOT to 0x1a,
        BasicSpecialSymbol.EXPONENT to 0x4b, BasicSpecialSymbol.BLOCK to 0x4c)
    val smallS1 = mapOf(BasicSpecialSymbol.PI to 0xfb, BasicSpecialSymbol.SQUARE_ROOT to 0xfc,
        BasicSpecialSymbol.INSERT to 0xfd, BasicSpecialSymbol.BLOCK to 0xfe)
    val later = mapOf(BasicSpecialSymbol.PI to 0xfb, BasicSpecialSymbol.SQUARE_ROOT to 0xfc,
        BasicSpecialSymbol.INSERT to 0xfa, BasicSpecialSymbol.BLOCK to 0xf9)
    fun names(codes: Map<BasicSpecialSymbol, Int>): Map<Int, String> =
        codes.entries.associate { (symbol, code) -> code to text(symbol) }
    fun text(symbol: BasicSpecialSymbol): String = "\\" + when (symbol) {
        BasicSpecialSymbol.PI -> "PI"
        BasicSpecialSymbol.SQUARE_ROOT -> "SQR"
        BasicSpecialSymbol.EXPONENT -> "EX"
        BasicSpecialSymbol.BLOCK -> "BX"
        BasicSpecialSymbol.INSERT -> "INS"
    }
}
