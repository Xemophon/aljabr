package com.xemophon.aljabr.series

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.FourierResult

object SeriesFuncs {

    private fun String.prepare(): String = SymjaUtils.prepareForSymja(this)

    /**
     * Calculates the Fourier series expansion for a function (possibly piecewise).
     * 
     * If f2 is null/blank, calculates for f1 on [a, c].
     * If f2 is present, calculates for f1 on [a, b] and f2 on [b, c].
     */
    fun parseFourierElements(
        f1: String,
        f2: String?,
        aStr: String,
        bStr: String,
        cStr: String?,
        numTerms: Int = 3
    ): FourierResult {
        return synchronized(SymjaUtils.evaluator) {
            try {
                val a = aStr.prepare()
                val b = bStr.prepare()
                val c = cStr?.prepare() ?: b
                
                val f1Clean = f1.prepare()
                val f2Clean = f2?.takeIf { it.isNotBlank() }?.prepare()
                
                // 1. Calculate L (Half-period)
                // L = (c - a) / 2
                val bigLRaw = SymjaUtils.evaluator.eval("Simplify[($c - ($a)) / 2]").toString()
                val bigLFormatted = SymjaUtils.formatResult(bigLRaw)
                
                // 2. Integration helper
                fun integral(func: String, lower: String, upper: String): String {
                    return "Integrate[$func, {x, $lower, $upper}]"
                }
                
                // a0 = (1/L) * integral of f(x) from a to c
                val a0Expr = if (f2Clean == null) {
                    "(1/($bigLRaw)) * (${integral(f1Clean, a, c)})"
                } else {
                    "(1/($bigLRaw)) * (${integral(f1Clean, a, b)} + ${integral(f2Clean, b, c)})"
                }
                
                val a0Val = SymjaUtils.evaluator.eval("Simplify[$a0Expr]").toString()
                val a0Formatted = SymjaUtils.formatResult(a0Val)
                
                val terms = mutableListOf<String>()
                val anList = mutableListOf<String>()
                val bnList = mutableListOf<String>()
                
                // First term is a0 / 2
                val a0Half = SymjaUtils.evaluator.eval("Simplify[$a0Val / 2]").toString()
                val a0HalfFormatted = SymjaUtils.formatResult(a0Half)
                if (a0HalfFormatted != "0") {
                    terms.add(a0HalfFormatted)
                }
                
                // Calculate an and bn for n = 1 to numTerms
                for (n in 1..numTerms) {
                    val arg = "($n * Pi * x) / ($bigLRaw)"
                    val cosTerm = "Cos[$arg]"
                    val sinTerm = "Sin[$arg]"
                    
                    // an = (1/L) * integral of f(x)*cos(arg)
                    val anExpr = if (f2Clean == null) {
                        "(1/($bigLRaw)) * (${integral("$f1Clean * $cosTerm", a, c)})"
                    } else {
                        "(1/($bigLRaw)) * (${integral("$f1Clean * $cosTerm", a, b)} + ${integral("$f2Clean * $cosTerm", b, c)})"
                    }
                    
                    // bn = (1/L) * integral of f(x)*sin(arg)
                    val bnExpr = if (f2Clean == null) {
                        "(1/($bigLRaw)) * (${integral("$f1Clean * $sinTerm", a, c)})"
                    } else {
                        "(1/($bigLRaw)) * (${integral("$f1Clean * $sinTerm", a, b)} + ${integral("$f2Clean * $sinTerm", b, c)})"
                    }
                    
                    val anVal = SymjaUtils.evaluator.eval("Simplify[$anExpr]").toString()
                    val bnVal = SymjaUtils.evaluator.eval("Simplify[$bnExpr]").toString()
                    
                    val formattedAn = SymjaUtils.formatResult(anVal)
                    val formattedBn = SymjaUtils.formatResult(bnVal)
                    
                    anList.add(formattedAn)
                    bnList.add(formattedBn)
                    
                    if (formattedAn != "0" && !anVal.contains("Integrate")) {
                        val formattedCos = SymjaUtils.formatResult(cosTerm)
                        terms.add("($formattedAn)$formattedCos")
                    }
                    
                    if (formattedBn != "0" && !bnVal.contains("Integrate")) {
                        val formattedSin = SymjaUtils.formatResult(sinTerm)
                        terms.add("($formattedBn)$formattedSin")
                    }
                }
                
                val fullSeries = if (terms.isEmpty()) "0" else terms.joinToString(" + ").replace("+ -", "- ")
                
                FourierResult(
                    l = bigLFormatted,
                    a0 = a0Formatted,
                    an = anList,
                    bn = bnList,
                    fullSeries = fullSeries
                )
            } catch (e: Exception) {
                FourierResult("", "", emptyList(), emptyList(), "", error = e.message ?: "Calculation failed")
            }
        }
    }
}
