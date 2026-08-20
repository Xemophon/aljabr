package com.xemophon.aljabr.algebra.matrices

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalcButtonAction
import com.xemophon.aljabr.ui.components.Constants

enum class MatrixMode {
    ADDITION, SUBTRACTION, MULTIPLICATION, DETERMINANT, INVERSE, TRANSPOSE, RANK, EIGENVALUES, EIGENVECTORS, LINEARSOLVE
}

enum class MatrixName { A, B }

class MatrixViewModel : ViewModel() {

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
            } else {
                val old = rowsB
                rowsB = newRows
                resizeMatrix(_matrixDataB, old, columnsB, newRows, columnsB)
            }
        }
    }

    fun updateColumns(newColumns: Int) {
        if (newColumns in 1..10) {
            if (activeMatrix == MatrixName.A) {
                val old = columnsA
                columnsA = newColumns
                resizeMatrix(_matrixDataA, rowsA, old, rowsA, newColumns)
            } else {
                val old = columnsB
                columnsB = newColumns
                resizeMatrix(_matrixDataB, rowsB, old, rowsB, newColumns)
            }
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
                SymjaUtils.prepareForSymja(if (raw.isBlank()) "0" else raw)
            }
        }
    }

    fun calculateResult() {
        val matrixA = getSymjaMatrix(MatrixName.A)
        val matrixB = if (mode == MatrixMode.ADDITION || 
            mode == MatrixMode.SUBTRACTION || 
            mode == MatrixMode.MULTIPLICATION ||
            mode == MatrixMode.LINEARSOLVE) {
            getSymjaMatrix(MatrixName.B)
        } else null

        resultText = MatrixFunc.calculate(mode, matrixA, matrixB)
    }
}
