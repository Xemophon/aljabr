package com.xemophon.aljabr.modules.series.fourier

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.input.MathInputHandler
import com.xemophon.aljabr.ui.components.screens.FourierResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FourierFocus {
    BRANCH1, BRANCH2, LIMIT_A, LIMIT_B, LIMIT_C
}

class FourierViewModel(application: Application) : AndroidViewModel(application) {

    var isTwoBranch by mutableStateOf(value = false)
        private set

    fun setTwoBranchMode(twoBranch: Boolean) {
        isTwoBranch = twoBranch
        if ((!twoBranch) && (currentFocus in listOf(FourierFocus.BRANCH2, FourierFocus.LIMIT_B))) {
            currentFocus = FourierFocus.BRANCH1
        }
    }
    
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

    private val activeFocuses: List<FourierFocus>
        get() = if (isTwoBranch) {
            listOf(FourierFocus.BRANCH1, FourierFocus.LIMIT_A, FourierFocus.LIMIT_B, FourierFocus.BRANCH2, FourierFocus.LIMIT_C)
        } else {
            listOf(FourierFocus.BRANCH1, FourierFocus.LIMIT_A, FourierFocus.LIMIT_C)
        }

    fun onFocusChange(focus: FourierFocus) {
        if ((!isTwoBranch) && (focus in listOf(FourierFocus.BRANCH2, FourierFocus.LIMIT_B))) {
            return
        }
        currentFocus = focus
        isFocusedMode = true
    }

    fun dismissFocus() {
        isFocusedMode = false
    }

    fun nextFocus() {
        val list = activeFocuses
        val currentIndex = list.indexOf(currentFocus)
        currentFocus = if (currentIndex == -1) {
            list.first()
        } else {
            list[(currentIndex + 1) % list.size]
        }
    }

    fun prevFocus() {
        val list = activeFocuses
        val currentIndex = list.indexOf(currentFocus)
        currentFocus = if (currentIndex == -1) {
            list.first()
        } else {
            val prevIndex = if (currentIndex == 0) list.size - 1 else currentIndex - 1
            list[prevIndex]
        }
    }

    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Symbol -> {
                if (action.formula == "( )" || action.formula == "()") {
                    handleBrackets()
                } else {
                    handleSymbol(action.formula)
                }
            }
            is CalcButtonAction.Scientific -> {
                updateCurrentField { text ->
                    MathInputHandler.handleScientific(text, text.length, action).text
                }
            }
            is CalcButtonAction.Constant -> {
                updateCurrentField { text ->
                    MathInputHandler.handleConstant(text, text.length, action).text
                }
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
        updateCurrentField { text ->
            MathInputHandler.handleSymbol(text, text.length, symbol).text
        }
    }

    private fun handleBrackets() {
        updateCurrentField { text ->
            MathInputHandler.handleBrackets(text, text.length).text
        }
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
            fourierResult = FourierResult("", "", null, null, rawL = "", rawA0 = "", rawAnGeneral = null, rawBnGeneral = null, fullSeries = "")
            
            try {
                withContext(Dispatchers.Default) {
                    val aClean = a.prepare()
                    val bClean = b.prepare()
                    val cClean = c.prepare()
                    val f1Clean = f1.prepare()
                    val f2Clean = if (isTwoBranch) f2.prepare() else null
                    
                    // 1. L (Half-period)
                    val bigLRaw = SymjaUtils.evaluate { eval ->
                        eval.eval("Clear[n, x]")
                        eval.eval("Simplify[($cClean - ($aClean)) / 2]").toString()
                    }
                    if (bigLRaw.contains("Infinity") || bigLRaw.contains("Indeterminate") || bigLRaw.contains("ComplexInfinity")) {
                        withContext(Dispatchers.Main) {
                            fourierResult = FourierResult("", "", error = "Invalid interval limits: [$a, $c]")
                        }
                        return@withContext
                    }
                    val bigL = SymjaUtils.formatResult(bigLRaw)
                    
                    withContext(Dispatchers.Main) {
                        fourierResult = fourierResult?.copy(l = bigL, rawL = bigLRaw)
                    }

                    // 2. a0
                    fun integral(func: String, lower: String, upper: String) = "Integrate[($func), {x, $lower, $upper}]"
                    val a0Expr = if (f2Clean == null) {
                        "(1/($bigLRaw)) * (${integral(f1Clean, aClean, cClean)})"
                    } else {
                        "(1/($bigLRaw)) * (${integral(f1Clean, aClean, bClean)} + ${integral(f2Clean, bClean, cClean)})"
                    }
                    val a0Raw = SymjaUtils.evaluate { eval ->
                        eval.eval("Clear[n, x]")
                        eval.eval("FullSimplify[$a0Expr]").toString()
                    }
                    
                    val a0HasError = a0Raw.contains("Integrate") || a0Raw.contains("Infinity") || a0Raw.contains("Indeterminate") || a0Raw.contains("ComplexInfinity")
                    if (a0HasError) {
                        val fnDisplay = if (isTwoBranch) "f₁(x)=$f1, f₂(x)=$f2" else "f(x)=$f1"
                        withContext(Dispatchers.Main) {
                            fourierResult = FourierResult(l = bigL, a0 = "", error = "Function $fnDisplay is singular or non-integrable on the interval [$a, $c].")
                        }
                        return@withContext
                    }

                    val a0 = SymjaUtils.formatResult(a0Raw)
                    withContext(Dispatchers.Main) {
                        fourierResult = fourierResult?.copy(a0 = a0, rawA0 = a0Raw)
                    }

                    // 3. General coefficients
                    val genArg = "(n * Pi * x) / ($bigLRaw)"
                    val anGenExpr = if (f2Clean == null) {
                        "(1/($bigLRaw)) * (${integral("($f1Clean) * Cos[$genArg]", aClean, cClean)})"
                    } else {
                        "(1/($bigLRaw)) * (${integral("($f1Clean) * Cos[$genArg]", aClean, bClean)} + ${integral("($f2Clean) * Cos[$genArg]", bClean, cClean)})"
                    }
                    val bnGenExpr = if (f2Clean == null) {
                        "(1/($bigLRaw)) * (${integral("($f1Clean) * Sin[$genArg]", aClean, cClean)})"
                    } else {
                        "(1/($bigLRaw)) * (${integral("($f1Clean) * Sin[$genArg]", aClean, bClean)} + ${integral("($f2Clean) * Sin[$genArg]", bClean, cClean)})"
                    }

                    val anGenVal = try {
                        SymjaUtils.evaluate { eval ->
                            eval.eval("Clear[n, x]")
                            eval.eval("FullSimplify[$anGenExpr, n > 0 && Element[n, Integers]]").toString()
                        }
                    } catch (_: Exception) { null }

                    val bnGenVal = try {
                        SymjaUtils.evaluate { eval ->
                            eval.eval("Clear[n, x]")
                            eval.eval("FullSimplify[$bnGenExpr, n > 0 && Element[n, Integers]]").toString()
                        }
                    } catch (_: Exception) { null }

                    val anHasError = anGenVal != null && (anGenVal.contains("Integrate") || anGenVal.contains("Infinity") || anGenVal.contains("Indeterminate") || anGenVal.contains("ComplexInfinity"))
                    val bnHasError = bnGenVal != null && (bnGenVal.contains("Integrate") || bnGenVal.contains("Infinity") || bnGenVal.contains("Indeterminate") || bnGenVal.contains("ComplexInfinity"))

                    if (anHasError || bnHasError) {
                        val fnDisplay = if (isTwoBranch) "f₁(x)=$f1, f₂(x)=$f2" else "f(x)=$f1"
                        withContext(Dispatchers.Main) {
                            fourierResult = FourierResult(l = bigL, a0 = a0, rawA0 = a0Raw, error = "Function $fnDisplay has non-integrable or divergent harmonic coefficients on [$a, $c].")
                        }
                        return@withContext
                    }

                    withContext(Dispatchers.Main) {
                        fourierResult = fourierResult?.copy(
                            anGeneral = if (anGenVal != null) SymjaUtils.formatResult(anGenVal) else null,
                            bnGeneral = if (bnGenVal != null) SymjaUtils.formatResult(bnGenVal) else null,
                            rawAnGeneral = anGenVal,
                            rawBnGeneral = bnGenVal
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    fourierResult = FourierResult("", "", error = e.message ?: "Calculation error")
                }
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
