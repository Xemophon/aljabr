package com.xemophon.aljabr.modules.algebra.matrices

import com.xemophon.aljabr.data.SymjaUtils

object MatrixFunc {

    fun formatMatrixForSymja(matrix: List<List<String>>): String {
        return "{" + matrix.joinToString(",") { row ->
            "{" + row.joinToString(",") + "}"
        } + "}"
    }

    fun calculate(
        mode: com.xemophon.aljabr.modules.algebra.matrices.MatrixMode,
        matrixA: List<List<String>>,
        matrixB: List<List<String>>? = null
    ): String {
        val symjaA = formatMatrixForSymja(matrixA)
        val symjaB = matrixB?.let { formatMatrixForSymja(it) }

        val expression = when (mode) {
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.ADDITION -> "$symjaA + $symjaB"
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.SUBTRACTION -> "$symjaA - $symjaB"
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.MULTIPLICATION -> "$symjaA . $symjaB" // Dot for matrix multiplication in Symja
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.DETERMINANT -> "Det($symjaA)"
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.INVERSE -> "Inverse($symjaA)"
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.TRANSPOSE -> "Transpose($symjaA)"
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.RANK -> "MatrixRank($symjaA)"
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.EIGENVALUES -> "Eigenvalues($symjaA)"
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.EIGENVECTORS -> "Eigenvectors($symjaA)"
            _root_ide_package_.com.xemophon.aljabr.modules.algebra.matrices.MatrixMode.LINEARSOLVE -> "LinearSolve($symjaA, $symjaB)"
        }

        return try {
            val result = SymjaUtils.evaluator.eval(expression)
            result.toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
