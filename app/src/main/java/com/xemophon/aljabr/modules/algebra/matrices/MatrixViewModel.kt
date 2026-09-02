package com.xemophon.aljabr.modules.algebra.matrices

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xemophon.aljabr.modules.graphMaker.GraphGenerator
import com.xemophon.aljabr.data.SettingsRepository
import com.xemophon.aljabr.data.StorageUtils
import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.buttons.CalcButtonAction
import com.xemophon.aljabr.ui.components.buttons.Constants
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class MatrixMode {
    ADDITION, SUBTRACTION, MULTIPLICATION, DETERMINANT, INVERSE, TRANSPOSE, RANK, EIGENVALUES, EIGENVECTORS, LINEARSOLVE
}

val SingleMatrixModes = listOf(
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.DETERMINANT,
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.INVERSE,
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.TRANSPOSE,
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.RANK,
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.EIGENVALUES,
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.EIGENVECTORS
)

val SquareMatrixModes = listOf(
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.DETERMINANT,
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.INVERSE,
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.EIGENVALUES,
    _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.EIGENVECTORS
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

    var mode by mutableStateOf(_root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.ADDITION)
        private set

    var activeMatrix by mutableStateOf(_root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A)
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
    val rows: Int get() = if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) rowsA else rowsB
    val columns: Int get() = if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) columnsA else columnsB
    val matrixData: List<String> get() = if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) _matrixDataA else _matrixDataB

    var selectedIndex by mutableIntStateOf(0)
        private set

    var isFocusedMode by mutableStateOf(false)
        private set

    var resultText by mutableStateOf("")
        private set

    fun onModeChange(newMode: com.xemophon.aljabr.modules.algebra.matrices.MatrixMode) {
        mode = newMode
        if (newMode in _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.SingleMatrixModes) {
            activeMatrix = _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A
        }
        applyAlgebraicConstraints()
    }

    private fun applyAlgebraicConstraints() {
        when (mode) {
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.ADDITION, _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.SUBTRACTION -> {
                updateRowsB(rowsA)
                updateColumnsB(columnsA)
            }
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.MULTIPLICATION -> {
                updateRowsB(columnsA)
            }
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.LINEARSOLVE -> {
                updateRowsB(rowsA)
                updateColumnsB(1)
            }
            in _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.SquareMatrixModes -> {
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
        activeMatrix = if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.B else _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A
        // Reset selection when switching matrices to avoid index out of bounds
        if (selectedIndex >= matrixData.size) {
            selectedIndex = 0
        }
    }

    fun updateRows(newRows: Int) {
        if (newRows in 1..10) {
            if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) {
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
            if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) {
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
        val data = if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) _matrixDataA else _matrixDataB
        val current = data[selectedIndex]
        data[selectedIndex] = current + symbol
    }

    fun handleBackspace() {
        val data = if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) _matrixDataA else _matrixDataB
        val current = data[selectedIndex]
        if (current.isNotEmpty()) {
            data[selectedIndex] = current.dropLast(1)
        }
    }

    fun handleClear() {
        val data = if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) _matrixDataA else _matrixDataB
        data[selectedIndex] = ""

        // Clear App Cache and Graph Cache if enabled
        _root_ide_package_.com.xemophon.aljabr.modules.graphMaker.GraphGenerator.clearCache()
        if (autoClearCache) {
            StorageUtils.clearAppCache(getApplication())
        }
    }

    fun clearCurrentMatrix() {
        val data = if (activeMatrix == _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixName.A) _matrixDataA else _matrixDataB
        for (i in data.indices) {
            data[i] = ""
        }
        
        // Clear App Cache and Graph Cache if enabled
        if (autoClearCache) {
            GraphGenerator.clearCache()
            StorageUtils.clearAppCache(getApplication())
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

        // Clear App Cache and Graph Cache if enabled
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

            // Improved parser for Symja matrix/vector strings
            val rowsRaw = mutableListOf<List<String>>()
            
            // Case 1: Matrix {{a,b},{c,d}} or {{a},{b}}
            if (content.startsWith("{{")) {
                // Split by "},{" but handle nested structures if any
                // A more robust way is to find balanced braces
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
                        current = StringBuilder() // Skip commas between rows
                    }
                }
            } 
            // Case 2: Vector {a,b,c}
            else {
                val elements = content.removeSurrounding("{", "}").split(",").map { it.trim() }
                // Default vectors to column vectors (Nx1) for better algebraic consistency
                elements.forEach { rowsRaw.add(listOf(it)) }
            }

            if (rowsRaw.isNotEmpty()) {
                val newRows = rowsRaw.size
                val newCols = rowsRaw.first().size

                if (newRows in 1..10 && newCols in 1..10) {
                    if (name == MatrixName.A) {
                        rowsA = newRows
                        columnsA = newCols
                        _matrixDataA.clear()
                        rowsRaw.flatten().forEach { _matrixDataA.add(it) }
                    } else {
                        rowsB = newRows
                        columnsB = newCols
                        _matrixDataB.clear()
                        rowsRaw.flatten().forEach { _matrixDataB.add(it) }
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

        var res = MatrixFunc.calculate(mode, matrixA, matrixB)

        // Smart Unwrap: convert 1x1 matrix {{x}} to scalar x for cleaner display
        if (res.startsWith("{{") && res.endsWith("}}")) {
            val inner = res.removeSurrounding("{{", "}}")
            if (!inner.contains("},{") && !inner.contains(",")) {
                res = inner
            }
        }

        resultText = res
    }
}
