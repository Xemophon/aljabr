package com.xemophon.aljabr.modules.calculus.integrate.geometric

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.input.MathInputHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GeoIntegMode(val title: String) {
    SCALAR_LINE("Scalar Line"),
    ARC_LINE("Arc Line"),
    VECTOR_LINE("Vector Line"),
    SURFACE("Surface"),
    VOLUME("Volume")
}

enum class GeoIntegFocus {
    NONE,
    F_XY,
    P_XY,
    Q_XY,
    F_XYZ,
    PARAM_X,
    PARAM_Y,
    UPPER_T,
    LOWER_T,
    UPPER_X,
    LOWER_X,
    UPPER_Y,
    LOWER_Y,
    UPPER_Z,
    LOWER_Z
}

class GeoIntegViewModel : ViewModel() {
    var mode by mutableStateOf(GeoIntegMode.SCALAR_LINE)
        private set

    var currentFocus by mutableStateOf(GeoIntegFocus.NONE)
        private set

    var fXYText by mutableStateOf("x*y")
        private set

    var pXYText by mutableStateOf("x^2 + y")
        private set

    var qXYText by mutableStateOf("x - y^2")
        private set

    var fXYZText by mutableStateOf("x*y*z")
        private set

    var paramXText by mutableStateOf("cos(t)")
        private set

    var paramYText by mutableStateOf("sin(t)")
        private set

    var upperTText by mutableStateOf("2*pi")
        private set

    var lowerTText by mutableStateOf("0")
        private set

    var upperXText by mutableStateOf("1")
        private set

    var lowerXText by mutableStateOf("0")
        private set

    var upperYText by mutableStateOf("1")
        private set

    var lowerYText by mutableStateOf("0")
        private set

    var upperZText by mutableStateOf("1")
        private set

    var lowerZText by mutableStateOf("0")
        private set

    var orderIndex by mutableIntStateOf(0)
        private set

    val orderOptions = listOf(
        "dx dy dz",
        "dz dy dx",
        "dy dx dz",
        "dz dx dy",
        "dx dz dy",
        "dy dz dx"
    )

    var cursorIndex by mutableIntStateOf(-1)
        private set

    var resultText by mutableStateOf("")
        private set

    var geoIntegResult by mutableStateOf<GeoIntegResult?>(null)
        private set

    var isCalculating by mutableStateOf(false)
        private set

    fun selectMode(newMode: GeoIntegMode) {
        mode = newMode
        resultText = ""
        geoIntegResult = null
        currentFocus = GeoIntegFocus.NONE
        cursorIndex = -1
    }

    fun setFocus(focus: GeoIntegFocus) {
        currentFocus = focus
        cursorIndex = getActiveText().length
    }

    fun dismissFocus() {
        currentFocus = GeoIntegFocus.NONE
        cursorIndex = -1
    }

    fun getFocusListForMode(m: GeoIntegMode = mode): List<GeoIntegFocus> {
        return when (m) {
            GeoIntegMode.SCALAR_LINE -> listOf(GeoIntegFocus.F_XY, GeoIntegFocus.PARAM_X, GeoIntegFocus.PARAM_Y, GeoIntegFocus.UPPER_T, GeoIntegFocus.LOWER_T)
            GeoIntegMode.ARC_LINE -> listOf(GeoIntegFocus.PARAM_X, GeoIntegFocus.PARAM_Y, GeoIntegFocus.UPPER_T, GeoIntegFocus.LOWER_T)
            GeoIntegMode.VECTOR_LINE -> listOf(GeoIntegFocus.P_XY, GeoIntegFocus.Q_XY, GeoIntegFocus.PARAM_X, GeoIntegFocus.PARAM_Y, GeoIntegFocus.UPPER_T, GeoIntegFocus.LOWER_T)
            GeoIntegMode.SURFACE -> listOf(GeoIntegFocus.F_XY, GeoIntegFocus.UPPER_X, GeoIntegFocus.UPPER_Y, GeoIntegFocus.LOWER_X, GeoIntegFocus.LOWER_Y)
            GeoIntegMode.VOLUME -> listOf(GeoIntegFocus.F_XYZ, GeoIntegFocus.UPPER_X, GeoIntegFocus.UPPER_Y, GeoIntegFocus.UPPER_Z, GeoIntegFocus.LOWER_X, GeoIntegFocus.LOWER_Y, GeoIntegFocus.LOWER_Z)
        }
    }

    fun nextFocus() {
        val list = getFocusListForMode(mode)
        val idx = list.indexOf(currentFocus)
        if (idx != -1) {
            currentFocus = list[(idx + 1) % list.size]
            cursorIndex = getActiveText().length
        } else if (list.isNotEmpty()) {
            currentFocus = list.first()
            cursorIndex = getActiveText().length
        }
    }

    fun prevFocus() {
        val list = getFocusListForMode(mode)
        val idx = list.indexOf(currentFocus)
        if (idx != -1) {
            val prevIdx = if (idx - 1 < 0) list.size - 1 else idx - 1
            currentFocus = list[prevIdx]
            cursorIndex = getActiveText().length
        } else if (list.isNotEmpty()) {
            currentFocus = list.last()
            cursorIndex = getActiveText().length
        }
    }

    fun getFocusedTitle(): String {
        return when (currentFocus) {
            GeoIntegFocus.NONE -> ""
            GeoIntegFocus.F_XY -> "f(x, y)"
            GeoIntegFocus.P_XY -> "P(x, y)"
            GeoIntegFocus.Q_XY -> "Q(x, y)"
            GeoIntegFocus.F_XYZ -> "f(x, y, z)"
            GeoIntegFocus.PARAM_X -> "x(t)"
            GeoIntegFocus.PARAM_Y -> "y(t)"
            GeoIntegFocus.UPPER_T -> "Upper t bound"
            GeoIntegFocus.LOWER_T -> "Lower t bound"
            GeoIntegFocus.UPPER_X -> "Upper x bound"
            GeoIntegFocus.LOWER_X -> "Lower x bound"
            GeoIntegFocus.UPPER_Y -> "Upper y bound"
            GeoIntegFocus.LOWER_Y -> "Lower y bound"
            GeoIntegFocus.UPPER_Z -> "Upper z bound"
            GeoIntegFocus.LOWER_Z -> "Lower z bound"
        }
    }

    fun getFocusedPlaceholder(): String {
        return when (currentFocus) {
            GeoIntegFocus.NONE -> ""
            GeoIntegFocus.F_XY -> "f(x, y)"
            GeoIntegFocus.P_XY -> "P(x, y)"
            GeoIntegFocus.Q_XY -> "Q(x, y)"
            GeoIntegFocus.F_XYZ -> "f(x, y, z)"
            GeoIntegFocus.PARAM_X -> "x(t)"
            GeoIntegFocus.PARAM_Y -> "y(t)"
            GeoIntegFocus.UPPER_T -> "b"
            GeoIntegFocus.LOWER_T -> "a"
            GeoIntegFocus.UPPER_X -> "b"
            GeoIntegFocus.LOWER_X -> "a"
            GeoIntegFocus.UPPER_Y -> "d"
            GeoIntegFocus.LOWER_Y -> "c"
            GeoIntegFocus.UPPER_Z -> "f"
            GeoIntegFocus.LOWER_Z -> "e"
        }
    }

    fun updateCursorIndex(index: Int) {
        cursorIndex = index.coerceIn(0, getActiveText().length)
    }

    fun cycleOrder() {
        orderIndex = (orderIndex + 1) % orderOptions.size
    }

    val currentOrder: String
        get() = orderOptions[orderIndex]

    fun getActiveText(): String {
        return when (currentFocus) {
            GeoIntegFocus.NONE -> ""
            GeoIntegFocus.F_XY -> fXYText
            GeoIntegFocus.P_XY -> pXYText
            GeoIntegFocus.Q_XY -> qXYText
            GeoIntegFocus.F_XYZ -> fXYZText
            GeoIntegFocus.PARAM_X -> paramXText
            GeoIntegFocus.PARAM_Y -> paramYText
            GeoIntegFocus.UPPER_T -> upperTText
            GeoIntegFocus.LOWER_T -> lowerTText
            GeoIntegFocus.UPPER_X -> upperXText
            GeoIntegFocus.LOWER_X -> lowerXText
            GeoIntegFocus.UPPER_Y -> upperYText
            GeoIntegFocus.LOWER_Y -> lowerYText
            GeoIntegFocus.UPPER_Z -> upperZText
            GeoIntegFocus.LOWER_Z -> lowerZText
        }
    }

    private fun updateActiveText(newText: String, newCursor: Int) {
        when (currentFocus) {
            GeoIntegFocus.NONE -> {}
            GeoIntegFocus.F_XY -> fXYText = newText
            GeoIntegFocus.P_XY -> pXYText = newText
            GeoIntegFocus.Q_XY -> qXYText = newText
            GeoIntegFocus.F_XYZ -> fXYZText = newText
            GeoIntegFocus.PARAM_X -> paramXText = newText
            GeoIntegFocus.PARAM_Y -> paramYText = newText
            GeoIntegFocus.UPPER_T -> upperTText = newText
            GeoIntegFocus.LOWER_T -> lowerTText = newText
            GeoIntegFocus.UPPER_X -> upperXText = newText
            GeoIntegFocus.LOWER_X -> lowerXText = newText
            GeoIntegFocus.UPPER_Y -> upperYText = newText
            GeoIntegFocus.LOWER_Y -> lowerYText = newText
            GeoIntegFocus.UPPER_Z -> upperZText = newText
            GeoIntegFocus.LOWER_Z -> lowerZText = newText
        }
        cursorIndex = newCursor
    }

    fun handleAction(action: CalcButtonAction) {
        if (resultText.isNotEmpty()) resultText = ""

        when (action) {
            is CalcButtonAction.Clear -> clearActiveField()
            is CalcButtonAction.Backspace -> handleBackspace()
            is CalcButtonAction.Symbol -> handleSymbol(action.formula)
            is CalcButtonAction.Variable -> handleInsert(action.text)
            is CalcButtonAction.Constant -> handleConstant(action)
            is CalcButtonAction.Scientific -> handleScientific(action)
            is CalcButtonAction.Done, is CalcButtonAction.Calculate -> computeResult()
            else -> {}
        }
    }

    fun clearActiveField() {
        updateActiveText("", 0)
    }

    fun clearAllFields() {
        fXYText = ""
        pXYText = ""
        qXYText = ""
        fXYZText = ""
        paramXText = ""
        paramYText = ""
        upperTText = ""
        lowerTText = ""
        upperXText = ""
        lowerXText = ""
        upperYText = ""
        lowerYText = ""
        upperZText = ""
        lowerZText = ""
        resultText = ""
        geoIntegResult = null
        cursorIndex = 0
    }

    fun clearResult() {
        geoIntegResult = null
    }

    private fun handleBackspace() {
        val currentText = getActiveText()
        val state = MathInputHandler.handleBackspace(currentText, cursorIndex)
        updateActiveText(state.text, state.cursorIndex)
    }

    private fun handleSymbol(symbol: String) {
        val currentText = getActiveText()
        val state = MathInputHandler.handleSymbol(currentText, cursorIndex, symbol)
        updateActiveText(state.text, state.cursorIndex)
    }

    private fun handleInsert(toInsert: String) {
        val currentText = getActiveText()
        val state = MathInputHandler.insertText(currentText, cursorIndex, toInsert, applyImplicitMultiplication = true)
        updateActiveText(state.text, state.cursorIndex)
    }

    private fun handleConstant(action: CalcButtonAction.Constant) {
        val currentText = getActiveText()
        val state = MathInputHandler.handleConstant(currentText, cursorIndex, action)
        updateActiveText(state.text, state.cursorIndex)
    }

    private fun handleScientific(action: CalcButtonAction.Scientific) {
        val currentText = getActiveText()
        val state = MathInputHandler.handleScientific(currentText, cursorIndex, action)
        updateActiveText(state.text, state.cursorIndex)
    }

    fun computeResult() {
        dismissFocus()
        viewModelScope.launch {
            isCalculating = true
            val res = withContext(Dispatchers.Default) {
                GeoIntegFunc.calculateGeoInteg(
                    mode = mode,
                    fXY = fXYText,
                    pXY = pXYText,
                    qXY = qXYText,
                    fXYZ = fXYZText,
                    paramX = paramXText,
                    paramY = paramYText,
                    upperT = upperTText,
                    lowerT = lowerTText,
                    upperX = upperXText,
                    lowerX = lowerXText,
                    upperY = upperYText,
                    lowerY = lowerYText,
                    upperZ = upperZText,
                    lowerZ = lowerZText,
                    orderStr = currentOrder
                )
            }
            geoIntegResult = res
            isCalculating = false
        }
    }
}
