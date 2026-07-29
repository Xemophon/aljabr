package com.xemophon.aljabr.conversions

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.basicCalc.CalcFuncs
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.Constants
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.*

enum class ConversionMode { ANGLE, COMPLEX }
enum class SelectedField { PRIMARY, SECONDARY }
enum class SubField { MAIN, EXTRA }

class ConvertorViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    var precision by mutableIntStateOf(4)
        private set

    init {
        viewModelScope.launch {
            settingsRepository.precisionFlow.collectLatest {
                precision = it
                performInstantConversion()
            }
        }
    }

    var mode by mutableStateOf(ConversionMode.ANGLE)
    var isSwapped by mutableStateOf(false)

    fun onModeChanged(newMode: ConversionMode) {
        if (mode != newMode) {
            mode = newMode
            isSwapped = false
            primaryValue = ""
            primaryValue2 = ""
            secondaryValue = ""
            secondaryValue2 = ""
            primaryCursor = 0
            primaryCursor2 = 0
            secondaryCursor = 0
            secondaryCursor2 = 0
            selectedSubField = SubField.MAIN
        }
    }

    var selectedField by mutableStateOf(SelectedField.PRIMARY)
    var selectedSubField by mutableStateOf(SubField.MAIN)

    var primaryValue by mutableStateOf("")
    var primaryValue2 by mutableStateOf("")
    var secondaryValue by mutableStateOf("")
    var secondaryValue2 by mutableStateOf("")

    var primaryCursor by mutableIntStateOf(0)
    var primaryCursor2 by mutableIntStateOf(0)
    var secondaryCursor by mutableIntStateOf(0)
    var secondaryCursor2 by mutableIntStateOf(0)

    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Clear -> {
                primaryValue = ""
                primaryValue2 = ""
                secondaryValue = ""
                secondaryValue2 = ""
                primaryCursor = 0
                primaryCursor2 = 0
                secondaryCursor = 0
                secondaryCursor2 = 0
            }

            is CalcButtonAction.Backspace -> handleBackspace()
            is CalcButtonAction.Symbol -> handleSymbol(action.text)
            is CalcButtonAction.Scientific -> handleScientific(action)
            is CalcButtonAction.Constant -> handleConstant(action)
            is CalcButtonAction.Variable -> handleSymbol(action.text)
            CalcButtonAction.Calculate -> performConversion()
            else -> {}
        }
    }

    private fun handleSymbol(symbol: String) {
        if (selectedField == SelectedField.PRIMARY) {
            if (selectedSubField == SubField.MAIN) {
                primaryValue = insertAtCursor(primaryValue, primaryCursor, symbol)
                primaryCursor += symbol.length
            } else {
                primaryValue2 = insertAtCursor(primaryValue2, primaryCursor2, symbol)
                primaryCursor2 += symbol.length
            }
        } else {
            if (selectedSubField == SubField.MAIN) {
                secondaryValue = insertAtCursor(secondaryValue, secondaryCursor, symbol)
                secondaryCursor += symbol.length
            } else {
                secondaryValue2 = insertAtCursor(secondaryValue2, secondaryCursor2, symbol)
                secondaryCursor2 += symbol.length
            }
        }
        performInstantConversion()
    }

    private fun handleScientific(action: CalcButtonAction.Scientific) {
        handleSymbol(action.text)
    }

    private fun handleConstant(action: CalcButtonAction.Constant) {
        val toInsert = when (action.type) {
            Constants.PI -> "π"
            Constants.E -> "e"
            Constants.I -> "j"
            Constants.PHI -> "φ"
        }
        handleSymbol(toInsert)
    }

    private fun handleBackspace() {
        if (selectedField == SelectedField.PRIMARY) {
            if (selectedSubField == SubField.MAIN) {
                if (primaryCursor > 0) {
                    primaryValue = primaryValue.removeRange(primaryCursor - 1, primaryCursor)
                    primaryCursor--
                }
            } else {
                if (primaryCursor2 > 0) {
                    primaryValue2 = primaryValue2.removeRange(primaryCursor2 - 1, primaryCursor2)
                    primaryCursor2--
                }
            }
        } else {
            if (selectedSubField == SubField.MAIN) {
                if (secondaryCursor > 0) {
                    secondaryValue = secondaryValue.removeRange(secondaryCursor - 1, secondaryCursor)
                    secondaryCursor--
                }
            } else {
                if (secondaryCursor2 > 0) {
                    secondaryValue2 = secondaryValue2.removeRange(secondaryCursor2 - 1, secondaryCursor2)
                    secondaryCursor2--
                }
            }
        }
        performInstantConversion()
    }

    fun getLabels(): Pair<String, String> {
        val base = when (mode) {
            ConversionMode.ANGLE -> "Degrees" to "Radians"
            ConversionMode.COMPLEX -> "Cartesian" to "Polar"
        }
        return if (isSwapped) base.second to base.first else base
    }

    fun getSubLabels(): Pair<Pair<String, String>, Pair<String, String>> {
        val cart = "Expression" to ""
        val polar = "Modulus" to "Angle"
        val angle = "Value" to ""

        val base = when (mode) {
            ConversionMode.ANGLE -> angle to angle
            ConversionMode.COMPLEX -> cart to polar
        }
        return if (isSwapped) base.second to base.first else base
    }

    private fun insertAtCursor(text: String, cursor: Int, toInsert: String): String {
        return text.substring(0, cursor) + toInsert + text.substring(cursor)
    }

    fun swapFields() {
        isSwapped = !isSwapped
        val tempVal = primaryValue
        val tempVal2 = primaryValue2
        primaryValue = secondaryValue
        primaryValue2 = secondaryValue2
        secondaryValue = tempVal
        secondaryValue2 = tempVal2
        
        val tempCursor = primaryCursor
        val tempCursor2 = primaryCursor2
        primaryCursor = secondaryCursor
        primaryCursor2 = secondaryCursor2
        secondaryCursor = tempCursor
        secondaryCursor2 = tempCursor2
        
        performInstantConversion()
    }

    private fun performInstantConversion() {
        if (selectedField == SelectedField.PRIMARY) {
            if (primaryValue.isBlank() && primaryValue2.isBlank()) {
                secondaryValue = ""
                secondaryValue2 = ""
                return
            }
            try {
                val res = convert(primaryValue, primaryValue2, fromPrimary = true)
                secondaryValue = res.first
                secondaryValue2 = res.second
            } catch (_: Exception) {
                secondaryValue = "Error"
                secondaryValue2 = ""
            }
        } else {
            if (secondaryValue.isBlank() && secondaryValue2.isBlank()) {
                primaryValue = ""
                primaryValue2 = ""
                return
            }
            try {
                val res = convert(secondaryValue, secondaryValue2, fromPrimary = false)
                primaryValue = res.first
                primaryValue2 = res.second
            } catch (_: Exception) {
                primaryValue = "Error"
                primaryValue2 = ""
            }
        }
    }

    private fun performConversion() {
        performInstantConversion()
    }

    private fun convert(val1: String, val2: String, fromPrimary: Boolean): Pair<String, String> {
        return when (mode) {
            ConversionMode.ANGLE -> convertAngle(val1, fromPrimary) to ""
            ConversionMode.COMPLEX -> convertComplexCartPolar(val1, val2, fromPrimary)
        }
    }

    private fun convertAngle(input: String, fromPrimary: Boolean): String {
        val value = CalcFuncs.calculateExpression(input)
        if (value.isNaN()) return "Error"
        
        val toRad = if (isSwapped) !fromPrimary else fromPrimary
        
        val result = if (toRad) {
            // Deg to Rad
            (value * PI) / 180.0
        } else {
            // Rad to Deg
            (value * 180.0) / PI
        }
        return CalcFuncs.formatResult(result, precision)
    }

    private fun convertComplexCartPolar(val1: String, val2: String, fromPrimary: Boolean): Pair<String, String> {
        val toPolar = if (isSwapped) !fromPrimary else fromPrimary
        
        return if (toPolar) {
            // Cartesian to Polar
            val (real, imag) = parseCartesian(val1) ?: return "Error" to ""
            val r = sqrt(real * real + imag * imag)
            val theta = atan2(imag, real) // in radians
            val thetaDeg = theta * 180.0 / PI
            CalcFuncs.formatResult(r, precision) to "${CalcFuncs.formatResult(thetaDeg, precision)}°"
        } else {
            // Polar to Cartesian
            val r = CalcFuncs.calculateExpression(val1)
            val thetaDeg = CalcFuncs.calculateExpression(val2.replace("°", ""))
            if (r.isNaN() || thetaDeg.isNaN()) return "Error" to ""
            
            val theta = thetaDeg * PI / 180.0
            val real = r * cos(theta)
            val imag = r * sin(theta)
            formatCartesian(real, imag) to ""
        }
    }

    private fun parseCartesian(input: String): Pair<Double, Double>? {
        // Simple parser for a+bj
        val cleaned = input.replace(" ", "")
        if (cleaned.isBlank()) return null
        
        return try {
            if (cleaned.endsWith("j") || cleaned.endsWith("i")) {
                val withoutJ = cleaned.dropLast(1)
                if (withoutJ.isEmpty() || withoutJ == "+") Pair(0.0, 1.0)
                else if (withoutJ == "-") Pair(0.0, -1.0)
                else {
                    val lastPlus = withoutJ.lastIndexOf('+')
                    val lastMinus = withoutJ.lastIndexOf('-')
                    val splitIdx = max(lastPlus, lastMinus)
                    
                    if (splitIdx > 0) {
                        val a = CalcFuncs.calculateExpression(withoutJ.substring(0, splitIdx))
                        val bStr = withoutJ.substring(splitIdx)
                        val b = if (bStr == "+") 1.0 else if (bStr == "-") -1.0 else CalcFuncs.calculateExpression(bStr)
                        Pair(a, b)
                    } else {
                        // Just bj
                        val b = CalcFuncs.calculateExpression(withoutJ)
                        Pair(0.0, b)
                    }
                }
            } else {
                // Just a
                Pair(CalcFuncs.calculateExpression(cleaned), 0.0)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatCartesian(real: Double, imag: Double): String {
        val rStr = CalcFuncs.formatResult(real, precision)
        val iStr = CalcFuncs.formatResult(abs(imag), precision)
        return when {
            imag == 0.0 -> rStr
            real == 0.0 -> "${if (imag < 0) "-" else ""}${if (iStr == "1") "" else iStr}j"
            else -> "$rStr ${if (imag < 0) "-" else "+"} ${if (iStr == "1") "" else iStr}j"
        }
    }
}
