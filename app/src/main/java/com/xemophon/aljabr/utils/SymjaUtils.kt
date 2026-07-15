package com.xemophon.aljabr.utils

import org.matheclipse.core.eval.ExprEvaluator

object SymjaUtils {
    // Shared evaluator to avoid expensive re-initialization.
    val evaluator by lazy {
        ExprEvaluator().apply {
            evalEngine.isRelaxedSyntax = true
            evalEngine.recursionLimit = 128
            evalEngine.iterationLimit = 500
        }
    }

    fun prepareForSymja(expression: String): String {
        return expression
            .replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("sin", "Sin", ignoreCase = true)
            .replace("cos", "Cos", ignoreCase = true)
            .replace("tan", "Tan", ignoreCase = true)
            .replace("asin", "ArcSin", ignoreCase = true)
            .replace("acos", "ArcCos", ignoreCase = true)
            .replace("atan", "ArcTan", ignoreCase = true)
            .replace("ln", "Log", ignoreCase = true)
            .replace("log", "Log10", ignoreCase = true)
            .replace("sqrt", "Sqrt", ignoreCase = true)
            .replace("π", "Pi")
            .replace("e", "E")
            .replace("√", "Sqrt")
    }

    fun formatResult(resStr: String): String {
        return resStr
            .replace("Log10", "log")
            .replace("Log", "ln")
            .replace("ArcSin", "asin")
            .replace("ArcCos", "acos")
            .replace("ArcTan", "atan")
            .replace("Sin", "sin")
            .replace("Cos", "cos")
            .replace("Tan", "tan")
            .replace("Pi", "π")
            .replace("E", "e")
            .replace("Sqrt", "√")
            .replace("*x", "x")
            .replace("*(", "(")
            .replace("[", "(")
            .replace("]", ")")
            .replace(",", ", ")
    }
}
