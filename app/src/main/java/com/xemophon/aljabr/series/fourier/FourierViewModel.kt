package com.xemophon.aljabr.series.fourier

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.Constants
import com.xemophon.aljabr.ui.components.FourierResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FourierFocus {
    BRANCH1, BRANCH2, LIMIT_A, LIMIT_B, LIMIT_C
}

class FourierViewModel(application: Application) : AndroidViewModel(application) {

    var isTwoBranch by mutableStateOf(false)
    
    var f1 by mutableStateOf("")
    var f2 by mutableStateOf("")
    
    var a by mutableStateOf("-π")
    var b by mutableStateOf("0")
    var c by mutableStateOf("π")

    var currentFocus by mutableStateOf(FourierFocus.BRANCH1)
    var isFocusedMode by mutableStateOf(false)
    
    var resultText by mutableStateOf("")
    var fourierResult by mutableStateOf<FourierResult?>(null)
        private set

    var isCalculating by mutableStateOf(false)
        private set

    fun onFocusChange(focus: FourierFocus) {
        currentFocus = focus
        isFocusedMode = true
    }

    fun dismissFocus() {
        isFocusedMode = false
    }

    fun nextFocus() {
        val values = FourierFocus.entries
        val nextIndex = (currentFocus.ordinal + 1) % values.size
        // Skip BRANCH2 if not in two-branch mode
        if (!isTwoBranch && values[nextIndex] == FourierFocus.BRANCH2) {
            currentFocus = values[(nextIndex + 1) % values.size]
        } else {
            currentFocus = values[nextIndex]
        }
    }

    fun prevFocus() {
        val values = FourierFocus.entries
        val prevIndex = if (currentFocus.ordinal == 0) values.size - 1 else currentFocus.ordinal - 1
        // Skip BRANCH2 if not in two-branch mode
        if (!isTwoBranch && values[prevIndex] == FourierFocus.BRANCH2) {
            currentFocus = values[if (prevIndex == 0) values.size - 1 else prevIndex - 1]
        } else {
            currentFocus = values[prevIndex]
        }
    }

    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Symbol -> handleSymbol(action.text)
            is CalcButtonAction.Scientific -> handleSymbol(action.text)
            is CalcButtonAction.Constant -> {
                val toInsert = when (action.type) {
                    Constants.PI -> "π"
                    Constants.E -> "e"
                    Constants.I -> "j"
                    Constants.PHI -> "φ"
                    Constants.INF -> "∞"
                }
                handleSymbol(toInsert)
            }
            is CalcButtonAction.Variable -> handleSymbol(action.text)
            is CalcButtonAction.Clear -> handleClear()
            is CalcButtonAction.Backspace -> handleBackspace()
            CalcButtonAction.Calculate -> calculateResult()
            CalcButtonAction.Done -> dismissFocus()
            else -> {}
        }
    }

    private fun handleSymbol(symbol: String) {
        updateCurrentField { it + symbol }
    }

    private fun handleBackspace() {
        updateCurrentField { if (it.isNotEmpty()) it.dropLast(1) else "" }
    }

    private fun handleClear() {
        updateCurrentField { "" }
    }

    private fun updateCurrentField(update: (String) -> String) {
        when (currentFocus) {
            FourierFocus.BRANCH1 -> f1 = update(f1)
            FourierFocus.BRANCH2 -> f2 = update(f2)
            FourierFocus.LIMIT_A -> a = update(a)
            FourierFocus.LIMIT_B -> b = update(b)
            FourierFocus.LIMIT_C -> c = update(c)
        }
    }

    fun calculateResult() {
        if (f1.isBlank()) return
        
        viewModelScope.launch {
            isCalculating = true
            fourierResult = FourierResult("", "", mutableListOf(), mutableListOf(), null, null, "")
            
            try {
                withContext(Dispatchers.Default) {
                    val aClean = a.prepare()
                    val bClean = b.prepare()
                    val cClean = c.prepare()
                    val f1Clean = f1.prepare()
                    val f2Clean = if (isTwoBranch) f2.prepare() else null
                    
                    // 1. L (Half-period)
                    val bigLRaw = SymjaUtils.evaluator.eval("Simplify[($cClean - ($aClean)) / 2]").toString()
                    val bigL = SymjaUtils.formatResult(bigLRaw)
                    
                    withContext(Dispatchers.Main) {
                        fourierResult = fourierResult?.copy(l = bigL)
                    }

                    // 2. a0
                    fun integral(func: String, lower: String, upper: String) = "Integrate[$func, {x, $lower, $upper}]"
                    val a0Expr = if (f2Clean == null) {
                        "(1/($bigLRaw)) * (${integral(f1Clean, aClean, cClean)})"
                    } else {
                        "(1/($bigLRaw)) * (${integral(f1Clean, aClean, bClean)} + ${integral(f2Clean, bClean, cClean)})"
                    }
                    val a0Val = SymjaUtils.evaluator.eval("Simplify[$a0Expr]").toString()
                    val a0 = SymjaUtils.formatResult(a0Val)
                    
                    val a0HalfVal = SymjaUtils.evaluator.eval("Simplify[$a0Val / 2]").toString()
                    val a0Half = SymjaUtils.formatResult(a0HalfVal)

                    withContext(Dispatchers.Main) {
                        fourierResult = fourierResult?.copy(a0 = a0)
                        if (a0Half != "0") resultText = a0Half
                    }

                    // 3. General coefficients an, bn (symbolic n)
                    val genArg = "(n * Pi * x) / ($bigLRaw)"
                    val anGenExpr = if (f2Clean == null) {
                        "(1/($bigLRaw)) * (${integral("$f1Clean * Cos[$genArg]", aClean, cClean)})"
                    } else {
                        "(1/($bigLRaw)) * (${integral("$f1Clean * Cos[$genArg]", aClean, bClean)} + ${integral("$f2Clean * Cos[$genArg]", bClean, cClean)})"
                    }
                    val bnGenExpr = if (f2Clean == null) {
                        "(1/($bigLRaw)) * (${integral("$f1Clean * Sin[$genArg]", aClean, cClean)})"
                    } else {
                        "(1/($bigLRaw)) * (${integral("$f1Clean * Sin[$genArg]", aClean, bClean)} + ${integral("$f2Clean * Sin[$genArg]", bClean, cClean)})"
                    }

                    val anGenVal = try { SymjaUtils.evaluator.eval("Simplify[$anGenExpr]").toString() } catch(e:Exception) { null }
                    val bnGenVal = try { SymjaUtils.evaluator.eval("Simplify[$bnGenExpr]").toString() } catch(e:Exception) { null }

                    withContext(Dispatchers.Main) {
                        fourierResult = fourierResult?.copy(
                            anGeneral = if (anGenVal != null && !anGenVal.contains("Integrate")) SymjaUtils.formatResult(anGenVal) else null,
                            bnGeneral = if (bnGenVal != null && !bnGenVal.contains("Integrate")) SymjaUtils.formatResult(bnGenVal) else null
                        )
                    }

                    // 4. Numerical Coefficients n=1 to 2
                    val terms = if (a0Half != "0") mutableListOf<String>(a0Half) else mutableListOf<String>()
                    
                    for (n in 1..2) {
                        val arg = "($n * Pi * x) / ($bigLRaw)"
                        val cosTerm = "Cos[$arg]"
                        val sinTerm = "Sin[$arg]"
                        
                        val anExpr = if (f2Clean == null) {
                            "(1/($bigLRaw)) * (${integral("$f1Clean * $cosTerm", aClean, cClean)})"
                        } else {
                            "(1/($bigLRaw)) * (${integral("$f1Clean * $cosTerm", aClean, bClean)} + ${integral("$f2Clean * $cosTerm", bClean, cClean)})"
                        }
                        
                        val bnExpr = if (f2Clean == null) {
                            "(1/($bigLRaw)) * (${integral("$f1Clean * $sinTerm", aClean, cClean)})"
                        } else {
                            "(1/($bigLRaw)) * (${integral("$f1Clean * $sinTerm", aClean, bClean)} + ${integral("$f2Clean * $sinTerm", bClean, cClean)})"
                        }

                        val anVal = SymjaUtils.evaluator.eval("Simplify[$anExpr]").toString()
                        val bnVal = SymjaUtils.evaluator.eval("Simplify[$bnExpr]").toString()
                        
                        val an = SymjaUtils.formatResult(anVal)
                        val bn = SymjaUtils.formatResult(bnVal)

                        withContext(Dispatchers.Main) {
                            val current = fourierResult!!
                            fourierResult = current.copy(
                                an = current.an + an,
                                bn = current.bn + bn
                            )
                            
                            if (an != "0" && !anVal.contains("Integrate")) {
                                terms.add("($an)${SymjaUtils.formatResult(cosTerm)}")
                            }
                            if (bn != "0" && !bnVal.contains("Integrate")) {
                                terms.add("($bn)${SymjaUtils.formatResult(sinTerm)}")
                            }
                            resultText = terms.joinToString(" + ").replace("+ -", "- ")
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        fourierResult = fourierResult?.copy(fullSeries = "f(x) = $resultText")
                    }
                }
            } catch (e: Exception) {
                resultText = "Error"
            } finally {
                isCalculating = false
            }
        }
    }
    
    private fun String.prepare() = SymjaUtils.prepareForSymja(this)

    fun clearResult() {
        fourierResult = null
        resultText = ""
    }
}
