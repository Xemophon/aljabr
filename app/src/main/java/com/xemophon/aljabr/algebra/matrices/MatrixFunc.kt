package com.xemophon.aljabr.algebra.matrices

import com.xemophon.aljabr.data.SymjaUtils

object MatrixFunc {

    fun formatMatrixForSymja(matrix: List<List<String>>): String {
        return "{" + matrix.joinToString(",") { row ->
            "{" + row.joinToString(",") + "}"
        } + "}"
    }

    fun calculate(
        mode: MatrixMode,
        matrixA: List<List<String>>,
        matrixB: List<List<String>>? = null
    ): String {
        val symjaA = formatMatrixForSymja(matrixA)
        val symjaB = matrixB?.let { formatMatrixForSymja(it) }

        val expression = when (mode) {
            MatrixMode.ADDITION -> "$symjaA + $symjaB"
            MatrixMode.SUBTRACTION -> "$symjaA - $symjaB"
            MatrixMode.MULTIPLICATION -> "$symjaA . $symjaB" // Dot for matrix multiplication in Symja
            MatrixMode.DETERMINANT -> "Det($symjaA)"
            MatrixMode.INVERSE -> "Inverse($symjaA)"
            MatrixMode.TRANSPOSE -> "Transpose($symjaA)"
            MatrixMode.RANK -> "MatrixRank($symjaA)"
            MatrixMode.EIGENVALUES -> "Eigenvalues($symjaA)"
            MatrixMode.EIGENVECTORS -> "Eigenvectors($symjaA)"
        }

        return try {
            val result = SymjaUtils.evaluator.eval(expression)
            result.toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
