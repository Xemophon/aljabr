package com.xemophon.aljabr.algebra.matrices

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.Constants
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

    init {
        viewModelScope.launch {
            settingsRepository.useRadiansFlow.collectLatest {
                useRadians = it
            }
        }
    }

    var mode by mutableStateOf(MatrixMode.ADDITION)
        private set

    var activeMatrix by mutableStateOf(MatrixName.A)
        private set

    // State for Matrix A
    var rowsA by mutableIntStateOf(3)
    var columnsA by mutableIntStateOf(3)
    private val _matrixDataA = mutableStateListOf<String>().apply { repeat(9) { add("") } }

    // State for Matrix B
    var rowsB by mutableIntStateOf(3)
    var columnsB by mutableIntStateOf(3)
    private val _matrixDataB = mutableStateListOf<String>().apply { repeat(9) { add("") } }

    // Helpers to get current active matrix state
    val rows: Int get() = if (activeMatrix == MatrixName.A) rowsA else rowsB
    val columns: Int get() = if (activeMatrix == MatrixName.A) columnsA else columnsB
    val matrixData: List<String> get() = if (activeMatrix == MatrixName.A) _matrixDataA else _matrixDataB

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
                updateRowsB(rowsA)
                updateColumnsB(columnsA)
            }
            MatrixMode.MULTIPLICATION -> {
                updateRowsB(columnsA)
            }
            MatrixMode.LINEARSOLVE -> {
                updateRowsB(rowsA)
                updateColumnsB(1)
            }
            in SquareMatrixModes -> {
                updateColumnsA(rowsA)
            }
            else -> {}
        }
    }

    private fun updateColumnsA(newCols: Int) {
        if (newCols in 1..10 && columnsA != newCols) {
            val old = columnsA
            columnsA = newCols
            resizeMatrix(_matrixDataA, rowsA, old, rowsA, newCols)
        }
    }

    fun toggleMatrix() {
        activeMatrix = if (activeMatrix == MatrixName.A) MatrixName.B else MatrixName.A
        // Reset selection when switching matrices to avoid index out of bounds
        if (selectedIndex >= matrixData.size) {
            selectedIndex = 0
        }
    }

    fun updateRows(newRows: Int) {
        if (newRows in 1..10) {
            if (activeMatrix == MatrixName.A) {
                val old = rowsA
                rowsA = newRows
                resizeMatrix(_matrixDataA, old, columnsA, newRows, columnsA)
                applyAlgebraicConstraints()
            } else {
                updateRowsB(newRows)
            }
        }
    }

    private fun updateRowsB(newRows: Int) {
        if (newRows in 1..10 && rowsB != newRows) {
            val old = rowsB
            rowsB = newRows
            resizeMatrix(_matrixDataB, old, columnsB, newRows, columnsB)
        }
    }

    fun updateColumns(newColumns: Int) {
        if (newColumns in 1..10) {
            if (activeMatrix == MatrixName.A) {
                val old = columnsA
                columnsA = newColumns
                resizeMatrix(_matrixDataA, rowsA, old, rowsA, newColumns)
                applyAlgebraicConstraints()
            } else {
                updateColumnsB(newColumns)
            }
        }
    }

    private fun updateColumnsB(newColumns: Int) {
        if (newColumns in 1..10 && columnsB != newColumns) {
            val old = columnsB
            columnsB = newColumns
            resizeMatrix(_matrixDataB, rowsB, old, rowsB, newColumns)
        }
    }

    private fun resizeMatrix(data: MutableList<String>, oldR: Int, oldC: Int, newR: Int, newC: Int) {
        val newData = MutableList(newR * newC) { "" }
        for (r in 0 until minOf(oldR, newR)) {
            for (c in 0 until minOf(oldC, newC)) {
                val oldIndex = r * oldC + c
                if (oldIndex < data.size) {
                    newData[r * newC + c] = data[oldIndex]
                }
            }
        }
        data.clear()
        data.addAll(newData)
        
        if (selectedIndex >= newR * newC) {
            selectedIndex = 0
        }
    }

    fun onElementClick(index: Int) {
        selectedIndex = index
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
            selectedIndex = matrixData.size - 1
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
            else -> {}
        }
    }

    fun handleSymbol(symbol: String) {
        val data = if (activeMatrix == MatrixName.A) _matrixDataA else _matrixDataB
        val current = data[selectedIndex]
        data[selectedIndex] = current + symbol
    }

    fun handleBackspace() {
        val data = if (activeMatrix == MatrixName.A) _matrixDataA else _matrixDataB
        val current = data[selectedIndex]
        if (current.isNotEmpty()) {
            data[selectedIndex] = current.dropLast(1)
        }
    }

    fun handleClear() {
        val data = if (activeMatrix == MatrixName.A) _matrixDataA else _matrixDataB
        data[selectedIndex] = ""
    }

    fun clearCurrentMatrix() {
        val data = if (activeMatrix == MatrixName.A) _matrixDataA else _matrixDataB
        for (i in data.indices) {
            data[i] = ""
        }
    }

    fun getSymjaMatrix(name: MatrixName): List<List<String>> {
        val data = if (name == MatrixName.A) _matrixDataA else _matrixDataB
        val r = if (name == MatrixName.A) rowsA else rowsB
        val c = if (name == MatrixName.A) columnsA else columnsB

        return List(r) { rowIndex ->
            List(c) { colIndex ->
                val raw = data.getOrNull(rowIndex * c + colIndex) ?: "0"
                SymjaUtils.prepareForSymja(if (raw.isBlank()) "0" else raw, useRadians = useRadians)
            }
        }
    }

    fun clearResult() {
        resultText = ""
    }

    fun loadResultIntoMatrix(name: MatrixName) {
        val result = resultText
        if (result.isBlank()) return

        try {
            // Very simple parser for Symja matrix strings like {{a,b},{c,d}}
            // or vector strings like {a,b,c}
            
            // Remove outer braces if they exist
            val content = result.trim()
            if (!content.startsWith("{")) return

            // If it's a matrix {{...},{...}}
            if (content.startsWith("{{")) {
                val rowsRaw = content.removeSurrounding("{", "}").split("},{")
                    .map { it.removeSurrounding("{", "}").split(",") }
                
                val newRows = rowsRaw.size
                val newCols = rowsRaw.firstOrNull()?.size ?: 0
                
                if (newRows in 1..10 && newCols in 1..10) {
                    if (name == MatrixName.A) {
                        rowsA = newRows
                        columnsA = newCols
                        _matrixDataA.clear()
                        rowsRaw.flatten().forEach { _matrixDataA.add(it.trim()) }
                    } else {
                        rowsB = newRows
                        columnsB = newCols
                        _matrixDataB.clear()
                        rowsRaw.flatten().forEach { _matrixDataB.add(it.trim()) }
                    }
                    clearResult()
                    activeMatrix = name
                }
            } else {
                // It's a vector {a,b,c} - load as a column or row matrix
                val elements = content.removeSurrounding("{", "}").split(",")
                val count = elements.size
                if (count in 1..10) {
                    if (name == MatrixName.A) {
                        rowsA = count
                        columnsA = 1
                        _matrixDataA.clear()
                        elements.forEach { _matrixDataA.add(it.trim()) }
                    } else {
                        rowsB = count
                        columnsB = 1
                        _matrixDataB.clear()
                        elements.forEach { _matrixDataB.add(it.trim()) }
                    }
                    clearResult()
                    activeMatrix = name
                }
            }
        } catch (_: Exception) {
            // If parsing fails, just leave it as is
        }
    }

    fun calculateResult() {
        val matrixA = getSymjaMatrix(MatrixName.A)
        val matrixB = if (mode !in SingleMatrixModes) {
            getSymjaMatrix(MatrixName.B)
        } else null

        resultText = MatrixFunc.calculate(mode, matrixA, matrixB)
    }
}
