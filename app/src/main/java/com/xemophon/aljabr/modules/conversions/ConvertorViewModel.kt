package com.xemophon.aljabr.modules.conversions

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.modules.basicCalc.CalcFuncs
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.input.InputState
import com.xemophon.aljabr.ui.components.input.MathInputHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigInteger
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ConversionMode { ANGLE, COMPLEX, NUMSYS }
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
            selectedField = SelectedField.PRIMARY
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
            is CalcButtonAction.Symbol -> handleSymbol(action.formula)
            is CalcButtonAction.Scientific -> handleScientific(action)
            is CalcButtonAction.Constant -> handleConstant(action)
            is CalcButtonAction.Variable -> handleSymbol(action.text)
            CalcButtonAction.Calculate -> performConversion()
            else -> {}
        }
    }

    private fun updateActiveFieldValue(update: (text: String, cursor: Int) -> InputState) {
        if (selectedField == SelectedField.PRIMARY) {
            if (selectedSubField == SubField.MAIN) {
                val newState = update(primaryValue, primaryCursor)
                primaryValue = newState.text
                primaryCursor = newState.cursorIndex
            } else {
                val newState = update(primaryValue2, primaryCursor2)
                primaryValue2 = newState.text
                primaryCursor2 = newState.cursorIndex
            }
        } else {
            if (selectedSubField == SubField.MAIN) {
                val newState = update(secondaryValue, secondaryCursor)
                secondaryValue = newState.text
                secondaryCursor = newState.cursorIndex
            } else {
                val newState = update(secondaryValue2, secondaryCursor2)
                secondaryValue2 = newState.text
                secondaryCursor2 = newState.cursorIndex
            }
        }
        performInstantConversion()
    }

    private fun handleSymbol(symbol: String) {
        updateActiveFieldValue { text, cursor ->
            MathInputHandler.handleSymbol(text, cursor, symbol)
        }
    }

    private fun handleScientific(action: CalcButtonAction.Scientific) {
        updateActiveFieldValue { text, cursor ->
            MathInputHandler.handleScientific(text, cursor, action)
        }
    }

    private fun handleConstant(action: CalcButtonAction.Constant) {
        updateActiveFieldValue { text, cursor ->
            MathInputHandler.handleConstant(text, cursor, action)
        }
    }

    private fun handleBackspace() {
        updateActiveFieldValue { text, cursor ->
            MathInputHandler.handleBackspace(text, cursor)
        }
    }

    fun getLabels(): Pair<String, String> {
        val base = when (mode) {
            ConversionMode.ANGLE -> "Degrees" to "Radians"
            ConversionMode.COMPLEX -> "Cartesian" to "Polar"
            ConversionMode.NUMSYS -> "Decimal" to "HEX / BIN"
        }
        return if (isSwapped) base.second to base.first else base
    }

    fun getSubLabels(): Pair<Pair<String, String>, Pair<String, String>> {
        val cart = "Expression" to ""
        val polar = "Modulus" to "Angle"
        val angle = "Value" to ""
        val dec = "Decimal" to ""
        val hexBin = "Hex" to "Binary"

        val base = when (mode) {
            ConversionMode.ANGLE -> angle to angle
            ConversionMode.COMPLEX -> cart to polar
            ConversionMode.NUMSYS -> dec to hexBin
        }
        return if (isSwapped) base.second to base.first else base
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
        if (mode == ConversionMode.NUMSYS) {
            performNumSysConversion()
            return
        }

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

    private fun performNumSysConversion() {
        val isDecimalSelected = if (!isSwapped) {
            selectedField == SelectedField.PRIMARY
        } else {
            selectedField == SelectedField.SECONDARY
        }

        val isHexSelected = if (!isSwapped) {
            selectedField == SelectedField.SECONDARY && selectedSubField == SubField.MAIN
        } else {
            selectedField == SelectedField.PRIMARY && selectedSubField == SubField.MAIN
        }

        val isBinSelected = if (!isSwapped) {
            selectedField == SelectedField.SECONDARY && selectedSubField == SubField.EXTRA
        } else {
            selectedField == SelectedField.PRIMARY && selectedSubField == SubField.EXTRA
        }

        if (isDecimalSelected) {
            val decInput = if (!isSwapped) primaryValue else secondaryValue
            if (decInput.isBlank()) {
                if (!isSwapped) { secondaryValue = ""; secondaryValue2 = "" }
                else { primaryValue = ""; primaryValue2 = "" }
                return
            }
            val (hexRes, binRes) = convertDecimalToHexAndBin(decInput)
            if (!isSwapped) {
                secondaryValue = hexRes
                secondaryValue2 = binRes
            } else {
                primaryValue = hexRes
                primaryValue2 = binRes
            }
        } else if (isHexSelected) {
            val hexInput = if (!isSwapped) secondaryValue else primaryValue
            if (hexInput.isBlank()) {
                if (!isSwapped) { primaryValue = ""; secondaryValue2 = "" }
                else { secondaryValue = ""; primaryValue2 = "" }
                return
            }
            val (decRes, binRes) = convertHexToDecAndBin(hexInput)
            if (!isSwapped) {
                primaryValue = decRes
                secondaryValue2 = binRes
            } else {
                secondaryValue = decRes
                primaryValue2 = binRes
            }
        } else if (isBinSelected) {
            val binInput = if (!isSwapped) secondaryValue2 else primaryValue2
            if (binInput.isBlank()) {
                if (!isSwapped) { primaryValue = ""; secondaryValue = "" }
                else { secondaryValue = ""; primaryValue = "" }
                return
            }
            val (decRes, hexRes) = convertBinToDecAndHex(binInput)
            if (!isSwapped) {
                primaryValue = decRes
                secondaryValue = hexRes
            } else {
                secondaryValue = decRes
                primaryValue = hexRes
            }
        }
    }

    private fun convertDecimalToHexAndBin(input: String): Pair<String, String> {
        val clean = input.trim()
        if (clean.isBlank()) return "" to ""
        val bigInt = try {
            BigInteger(clean)
        } catch (_: Exception) {
            val exprVal = CalcFuncs.calculateExpression(clean)
            if (exprVal.isNaN() || exprVal.isInfinite()) return "Error" to ""
            try {
                BigInteger.valueOf(exprVal.toLong())
            } catch (_: Exception) {
                return "Error" to ""
            }
        }
        val hex = bigInt.toString(16).uppercase()
        val bin = bigInt.toString(2)
        return hex to bin
    }

    private fun convertHexToDecAndBin(input: String): Pair<String, String> {
        val clean = input.trim().removePrefix("0x").removePrefix("0X").replace(" ", "")
        if (clean.isBlank()) return "" to ""
        val bigInt = try {
            BigInteger(clean, 16)
        } catch (_: Exception) {
            return "Error" to ""
        }
        val dec = bigInt.toString(10)
        val bin = bigInt.toString(2)
        return dec to bin
    }

    private fun convertBinToDecAndHex(input: String): Pair<String, String> {
        val clean = input.trim().removePrefix("0b").removePrefix("0B").replace(" ", "")
        if (clean.isBlank()) return "" to ""
        val bigInt = try {
            BigInteger(clean, 2)
        } catch (_: Exception) {
            return "Error" to ""
        }
        val dec = bigInt.toString(10)
        val hex = bigInt.toString(16).uppercase()
        return dec to hex
    }

    private fun performConversion() {
        performInstantConversion()
    }

    private fun convert(val1: String, val2: String, fromPrimary: Boolean): Pair<String, String> {
        return when (mode) {
            ConversionMode.ANGLE -> convertAngle(val1, fromPrimary) to ""
            ConversionMode.COMPLEX -> convertComplexCartPolar(val1, val2, fromPrimary)
            ConversionMode.NUMSYS -> "" to ""
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
        val cleanedInput = input.replace(" ", "")
        if (cleanedInput.isBlank()) return null

        return try {
            val cleaned = SymjaUtils.prepareForSymja(input)
            if (cleaned.isBlank()) return null

            SymjaUtils.evaluate { eval ->
                val realStr = eval.eval("N[Re[$cleaned]]").toString()
                val imagStr = eval.eval("N[Im[$cleaned]]").toString()

                val real = realStr.toDoubleOrNull()
                val imag = imagStr.toDoubleOrNull()

                if (real != null && imag != null && !real.isNaN() && !real.isInfinite() && !imag.isNaN() && !imag.isInfinite()) {
                    Pair(real, imag)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatCartesian(real: Double, imag: Double): String {
        return SymjaUtils.formatComplexNumber(real, imag, precision)
    }
}
