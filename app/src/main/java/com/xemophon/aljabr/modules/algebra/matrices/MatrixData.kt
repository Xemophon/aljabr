package com.xemophon.aljabr.modules.algebra.matrices

import com.xemophon.aljabr.data.SymjaUtils

data class MatrixData(
    val rows: Int = 3,
    val columns: Int = 3,
    val elements: List<String> = List(9) { "" }
) {
    init {
        require(rows in 1..10 && columns in 1..10) { "Matrix dimensions must be between 1 and 10" }
    }

    fun getElement(index: Int): String {
        return if (index in elements.indices) elements[index] else ""
    }

    fun updateElement(index: Int, value: String): MatrixData {
        if (index !in elements.indices) return this
        val newElements = elements.toMutableList().apply { this[index] = value }
        return copy(elements = newElements)
    }

    fun resize(newRows: Int, newColumns: Int): MatrixData {
        val clampedRows = newRows.coerceIn(1, 10)
        val clampedCols = newColumns.coerceIn(1, 10)
        val newData = MutableList(clampedRows * clampedCols) { "" }

        for (r in 0 until minOf(rows, clampedRows)) {
            for (c in 0 until minOf(columns, clampedCols)) {
                val oldIndex = r * columns + c
                if (oldIndex in elements.indices) {
                    newData[r * clampedCols + c] = elements[oldIndex]
                }
            }
        }
        return MatrixData(clampedRows, clampedCols, newData)
    }

    fun clear(): MatrixData {
        return MatrixData(rows, columns, List(rows * columns) { "" })
    }

    fun toSymjaGrid(useRadians: Boolean = true): List<List<String>> {
        return List(rows) { r ->
            List(columns) { c ->
                val raw = getElement(r * columns + c)
                SymjaUtils.prepareForSymja(
                    if (raw.isBlank()) "0" else raw,
                    useRadians = useRadians
                )
            }
        }
    }
}
