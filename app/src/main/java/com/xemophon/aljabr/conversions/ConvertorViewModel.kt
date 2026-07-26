package com.xemophon.aljabr.conversions

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.xemophon.aljabr.basicCalc.CalcFuncs
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.Constants
import kotlin.math.*

enum class ConversionMode { ANGLE, COMPLEX_CART_POLAR, COMPLEX_CART_EXP }
enum class SelectedField { PRIMARY, SECONDARY }

class ConvertorViewModel(application: Application) : AndroidViewModel(application) {

    var mode by mutableStateOf(ConversionMode.ANGLE)
    var isSwapped by mutableStateOf(false)

    fun onModeChanged(newMode: ConversionMode) {
        if (mode != newMode) {
            mode = newMode
            isSwapped = false
            primaryValue = ""
            secondaryValue = ""
            primaryCursor = 0
            secondaryCursor = 0
        }
    }
    var selectedField by mutableStateOf(SelectedField.PRIMARY)

    var primaryValue by mutableStateOf("")
    var secondaryValue by mutableStateOf("")

    var primaryCursor by androidx.compose.runtime.mutableIntStateOf(0)
    var secondaryCursor by androidx.compose.runtime.mutableIntStateOf(0)

    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Clear -> {
                primaryValue = ""
                secondaryValue = ""
                primaryCursor = 0
                secondaryCursor = 0
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
            primaryValue = insertAtCursor(primaryValue, primaryCursor, symbol)
            primaryCursor += symbol.length
        } else {
            secondaryValue = insertAtCursor(secondaryValue, secondaryCursor, symbol)
            secondaryCursor += symbol.length
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
            Constants.I -> "i"
            Constants.PHI -> "φ"
        }
        handleSymbol(toInsert)
    }

    private fun handleBackspace() {
        if (selectedField == SelectedField.PRIMARY) {
            if (primaryCursor > 0) {
                primaryValue = primaryValue.removeRange(primaryCursor - 1, primaryCursor)
                primaryCursor--
            }
        } else {
            if (secondaryCursor > 0) {
                secondaryValue = secondaryValue.removeRange(secondaryCursor - 1, secondaryCursor)
                secondaryCursor--
            }
        }
        performInstantConversion()
    }

    fun getLabels(): Pair<String, String> {
        val base = when (mode) {
            ConversionMode.ANGLE -> "Deg" to "Rad"
            ConversionMode.COMPLEX_CART_POLAR -> "Cartesian" to "Polar"
            ConversionMode.COMPLEX_CART_EXP -> "Cartesian" to "Exponential"
        }
        return if (isSwapped) base.second to base.first else base
    }

    private fun insertAtCursor(text: String, cursor: Int, toInsert: String): String {
        return text.substring(0, cursor) + toInsert + text.substring(cursor)
    }

    fun swapFields() {
        isSwapped = !isSwapped
        val tempVal = primaryValue
        primaryValue = secondaryValue
        secondaryValue = tempVal
        
        val tempCursor = primaryCursor
        primaryCursor = secondaryCursor
        secondaryCursor = tempCursor
        
        performInstantConversion()
    }

    private fun performInstantConversion() {
        if (selectedField == SelectedField.PRIMARY) {
            if (primaryValue.isBlank()) {
                secondaryValue = ""
                return
            }
            secondaryValue = try {
                convert(primaryValue, fromPrimary = true)
            } catch (_: Exception) {
                "Error"
            }
        } else {
            if (secondaryValue.isBlank()) {
                primaryValue = ""
                return
            }
            primaryValue = try {
                convert(secondaryValue, fromPrimary = false)
            } catch (_: Exception) {
                "Error"
            }
        }
    }

    private fun performConversion() {
        performInstantConversion()
    }

    private fun convert(input: String, fromPrimary: Boolean): String {
        return when (mode) {
            ConversionMode.ANGLE -> convertAngle(input, fromPrimary)
            ConversionMode.COMPLEX_CART_POLAR -> convertComplexCartPolar(input, fromPrimary)
            ConversionMode.COMPLEX_CART_EXP -> convertComplexCartExp(input, fromPrimary)
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
        return CalcFuncs.formatResult(result)
    }

    private fun convertComplexCartPolar(input: String, fromPrimary: Boolean): String {
        val toPolar = if (isSwapped) !fromPrimary else fromPrimary
        
        return if (toPolar) {
            // Cartesian to Polar
            val (real, imag) = parseCartesian(input) ?: return "Error"
            val r = sqrt(real * real + imag * imag)
            val theta = atan2(imag, real) // in radians
            // Result in (r, theta_deg)
            val thetaDeg = theta * 180.0 / PI
            "(${CalcFuncs.formatResult(r)}, ${CalcFuncs.formatResult(thetaDeg)}°)"
        } else {
            // Polar to Cartesian
            val (r, thetaDeg) = parsePolar(input) ?: return "Error"
            val theta = thetaDeg * PI / 180.0
            val real = r * cos(theta)
            val imag = r * sin(theta)
            formatCartesian(real, imag)
        }
    }

    private fun convertComplexCartExp(input: String, fromPrimary: Boolean): String {
        val toExp = if (isSwapped) !fromPrimary else fromPrimary
        
        return if (toExp) {
            // Cartesian to Exponential
            val (real, imag) = parseCartesian(input) ?: return "Error"
            val r = sqrt(real * real + imag * imag)
            val theta = atan2(imag, real)
            "${CalcFuncs.formatResult(r)}e^(i${CalcFuncs.formatResult(theta)})"
        } else {
            // Exponential to Cartesian
            val (r, theta) = parseExponential(input) ?: return "Error"
            val real = r * cos(theta)
            val imag = r * sin(theta)
            formatCartesian(real, imag)
        }
    }

    private fun parseCartesian(input: String): Pair<Double, Double>? {
        // Simple parser for a+bi
        // Remove spaces and 'i'
        val cleaned = input.replace(" ", "")
        if (cleaned.isBlank()) return null
        
        return try {
            if (cleaned.endsWith("i")) {
                val withoutI = cleaned.dropLast(1)
                if (withoutI.isEmpty() || withoutI == "+") Pair(0.0, 1.0)
                else if (withoutI == "-") Pair(0.0, -1.0)
                else {
                    // Try to find the last + or - that isn't at the start or inside parens
                    // For simplicity, handle a+bi where a and b are numbers
                    val lastPlus = withoutI.lastIndexOf('+')
                    val lastMinus = withoutI.lastIndexOf('-')
                    val splitIdx = max(lastPlus, lastMinus)
                    
                    if (splitIdx > 0) {
                        val a = CalcFuncs.calculateExpression(withoutI.substring(0, splitIdx))
                        val bStr = withoutI.substring(splitIdx)
                        val b = if (bStr == "+") 1.0 else if (bStr == "-") -1.0 else CalcFuncs.calculateExpression(bStr)
                        Pair(a, b)
                    } else {
                        // Just bi
                        val b = CalcFuncs.calculateExpression(withoutI)
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

    private fun parsePolar(input: String): Pair<Double, Double>? {
        // Format (r, theta)
        val regex = Regex("\\(([^,]+),([^)]+)°?\\)")
        val match = regex.find(input.replace(" ", "")) ?: return null
        return try {
            val r = CalcFuncs.calculateExpression(match.groupValues[1])
            val theta = CalcFuncs.calculateExpression(match.groupValues[2])
            Pair(r, theta)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseExponential(input: String): Pair<Double, Double>? {
        // Format r e^(i theta)
        val regex = Regex("([^e]+)e\\^\\(i?([^)]+)\\)")
        val match = regex.find(input.replace(" ", "")) ?: return null
        return try {
            val r = CalcFuncs.calculateExpression(match.groupValues[1])
            val theta = CalcFuncs.calculateExpression(match.groupValues[2])
            Pair(r, theta)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatCartesian(real: Double, imag: Double): String {
        val rStr = CalcFuncs.formatResult(real)
        val iStr = CalcFuncs.formatResult(abs(imag))
        return when {
            imag == 0.0 -> rStr
            real == 0.0 -> "${if (imag < 0) "-" else ""}${if (iStr == "1") "" else iStr}i"
            else -> "$rStr ${if (imag < 0) "-" else "+"} ${if (iStr == "1") "" else iStr}i"
        }
    }
}
