package com.xemophon.aljabr.modules.algebra.matrices

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.data.StorageUtils
import com.xemophon.aljabr.modules.graphMaker.GraphGenerator
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.input.MathInputHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class MatrixMode {
    ADDITION, SUBTRACTION, MULTIPLICATION, DETERMINANT, INVERSE, TRANSPOSE, RANK, EIGENVALUES, EIGENVECTORS, LINEARSOLVE
}

val SingleMatrixModes = listOf(
    MatrixMode.DETERMINANT,
    MatrixMode.INVERSE,
    MatrixMode.TRANSPOSE,
    MatrixMode.RANK,
    MatrixMode.EIGENVALUES,
    MatrixMode.EIGENVECTORS
)

val SquareMatrixModes = listOf(
    MatrixMode.DETERMINANT,
    MatrixMode.INVERSE,
    MatrixMode.EIGENVALUES,
    MatrixMode.EIGENVECTORS
)

enum class MatrixName { A, B }

class MatrixViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    var useRadians by mutableStateOf(true)
        private set
    var autoClearCache by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            settingsRepository.useRadiansFlow.collectLatest {
                useRadians = it
            }
        }
        viewModelScope.launch {
            settingsRepository.autoClearCacheFlow.collectLatest {
                autoClearCache = it
            }
        }
    }

    var mode by mutableStateOf(MatrixMode.ADDITION)
        private set

    var activeMatrix by mutableStateOf(MatrixName.A)
        private set

    // State for Matrix A & B using MatrixData domain model
    var matrixA by mutableStateOf(MatrixData(3, 3))
        private set
    var matrixB by mutableStateOf(MatrixData(3, 3))
        private set

    // Helpers to get current active matrix state
    val rows: Int get() = if (activeMatrix == MatrixName.A) matrixA.rows else matrixB.rows
    val columns: Int get() = if (activeMatrix == MatrixName.A) matrixA.columns else matrixB.columns
    val matrixData: List<String> get() = if (activeMatrix == MatrixName.A) matrixA.elements else matrixB.elements

    var selectedIndex by mutableIntStateOf(0)
        private set

    var isFocusedMode by mutableStateOf(false)
        private set

    var resultText by mutableStateOf("")
        private set

    fun onModeChange(newMode: MatrixMode) {
        mode = newMode
        if (newMode in SingleMatrixModes) {
            activeMatrix = MatrixName.A
        }
        applyAlgebraicConstraints()
    }

    private fun applyAlgebraicConstraints() {
        when (mode) {
            MatrixMode.ADDITION, MatrixMode.SUBTRACTION -> {
                updateRowsB(matrixA.rows)
                updateColumnsB(matrixA.columns)
            }
            MatrixMode.MULTIPLICATION -> {
                updateRowsB(matrixA.columns)
            }
            MatrixMode.LINEARSOLVE -> {
                updateRowsB(matrixA.rows)
                updateColumnsB(1)
            }
            in SquareMatrixModes -> {
                updateColumnsA(matrixA.rows)
            }
            else -> {}
        }
    }

    private fun updateColumnsA(newCols: Int) {
        if (newCols in 1..10 && matrixA.columns != newCols) {
            matrixA = matrixA.resize(matrixA.rows, newCols)
            clampSelection()
        }
    }

    fun toggleMatrix() {
        activeMatrix = if (activeMatrix == MatrixName.A) MatrixName.B else MatrixName.A
        clampSelection()
    }

    fun updateRows(newRows: Int) {
        if (newRows in 1..10) {
            if (activeMatrix == MatrixName.A) {
                matrixA = matrixA.resize(newRows, matrixA.columns)
                applyAlgebraicConstraints()
            } else {
                updateRowsB(newRows)
            }
            clampSelection()
        }
    }

    private fun updateRowsB(newRows: Int) {
        if (newRows in 1..10 && matrixB.rows != newRows) {
            matrixB = matrixB.resize(newRows, matrixB.columns)
            clampSelection()
        }
    }

    fun updateColumns(newColumns: Int) {
        if (newColumns in 1..10) {
            if (activeMatrix == MatrixName.A) {
                matrixA = matrixA.resize(matrixA.rows, newColumns)
                applyAlgebraicConstraints()
            } else {
                updateColumnsB(newColumns)
            }
            clampSelection()
        }
    }

    private fun updateColumnsB(newColumns: Int) {
        if (newColumns in 1..10 && matrixB.columns != newColumns) {
            matrixB = matrixB.resize(matrixB.rows, newColumns)
            clampSelection()
        }
    }

    private fun clampSelection() {
        val total = matrixData.size
        if (selectedIndex >= total) {
            selectedIndex = (total - 1).coerceAtLeast(0)
        }
    }

    fun onElementClick(index: Int) {
        selectedIndex = index.coerceIn(0, (matrixData.size - 1).coerceAtLeast(0))
        isFocusedMode = true
    }

    fun dismissFocus() {
        isFocusedMode = false
    }

    fun nextElement() {
        if (selectedIndex < matrixData.size - 1) {
            selectedIndex++
        } else {
            selectedIndex = 0
        }
    }

    fun prevElement() {
        if (selectedIndex > 0) {
            selectedIndex--
        } else {
            selectedIndex = (matrixData.size - 1).coerceAtLeast(0)
        }
    }

    fun handleAction(action: CalcButtonAction) {
        when (action) {
            is CalcButtonAction.Symbol -> handleSymbol(action.text)
            is CalcButtonAction.Scientific -> {
                val current = matrixData.getOrElse(selectedIndex) { "" }
                val updated = MathInputHandler.handleScientific(current, current.length, action).text
                updateActiveElement(updated)
            }
            is CalcButtonAction.Constant -> {
                val current = matrixData.getOrElse(selectedIndex) { "" }
                val updated = MathInputHandler.handleConstant(current, current.length, action).text
                updateActiveElement(updated)
            }
            is CalcButtonAction.Variable -> handleSymbol(action.text)
            is CalcButtonAction.Clear -> handleClear()
            is CalcButtonAction.Backspace -> handleBackspace()
            CalcButtonAction.Calculate -> calculateResult()
            else -> {}
        }
    }

    fun handleSymbol(symbol: String) {
        val current = matrixData.getOrElse(selectedIndex) { "" }
        val updated = MathInputHandler.handleSymbol(current, current.length, symbol).text
        updateActiveElement(updated)
    }

    fun handleBackspace() {
        val current = matrixData.getOrElse(selectedIndex) { "" }
        val updated = MathInputHandler.handleBackspace(current, current.length).text
        updateActiveElement(updated)
    }

    private fun updateActiveElement(newValue: String) {
        if (activeMatrix == MatrixName.A) {
            matrixA = matrixA.updateElement(selectedIndex, newValue)
        } else {
            matrixB = matrixB.updateElement(selectedIndex, newValue)
        }
    }

    fun handleClear() {
        updateActiveElement("")

        GraphGenerator.clearCache()
        if (autoClearCache) {
            StorageUtils.clearAppCache(getApplication())
        }
    }

    fun clearCurrentMatrix() {
        if (activeMatrix == MatrixName.A) {
            matrixA = matrixA.clear()
        } else {
            matrixB = matrixB.clear()
        }

        if (autoClearCache) {
            GraphGenerator.clearCache()
            StorageUtils.clearAppCache(getApplication())
        }
    }

    fun getSymjaMatrix(name: MatrixName): List<List<String>> {
        val mat = if (name == MatrixName.A) matrixA else matrixB
        return mat.toSymjaGrid(useRadians = useRadians)
    }

    fun clearResult() {
        resultText = ""

        if (autoClearCache) {
            GraphGenerator.clearCache()
            StorageUtils.clearAppCache(getApplication())
        }
    }

    fun loadResultIntoMatrix(name: MatrixName) {
        val result = resultText
        if (result.isBlank()) return

        try {
            val content = result.trim()
            if (!content.startsWith("{")) return

            val rowsRaw = mutableListOf<List<String>>()

            if (content.startsWith("{{")) {
                val inner = content.removeSurrounding("{", "}")
                var depth = 0
                var current = StringBuilder()
                for (char in inner) {
                    if (char == '{') depth++
                    if (char == '}') depth--
                    current.append(char)
                    if (depth == 0 && char == '}') {
                        val rowStr = current.toString().removeSurrounding("{", "}")
                        rowsRaw.add(rowStr.split(",").map { it.trim() })
                        current = StringBuilder()
                    } else if (depth == 0 && char == ',') {
                        current = StringBuilder()
                    }
                }
            } else {
                val elements = content.removeSurrounding("{", "}").split(",").map { it.trim() }
                elements.forEach { rowsRaw.add(listOf(it)) }
            }

            if (rowsRaw.isNotEmpty()) {
                val newRows = rowsRaw.size
                val newCols = rowsRaw.first().size

                if (newRows in 1..10 && newCols in 1..10) {
                    val elements = rowsRaw.flatten()
                    val newMatrix = MatrixData(newRows, newCols, elements)
                    if (name == MatrixName.A) {
                        matrixA = newMatrix
                    } else {
                        matrixB = newMatrix
                    }
                    applyAlgebraicConstraints()
                }
            }
        } catch (_: Exception) {}
    }

    fun calculateResult() {
        try {
            val res = MatrixFunc.calculate(
                mode = mode,
                matrixA = getSymjaMatrix(MatrixName.A),
                matrixB = getSymjaMatrix(MatrixName.B)
            )
            resultText = res
        } catch (_: Exception) {
            resultText = "Error"
        }
    }
}
