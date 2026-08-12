package com.fcfb.arceus.util

/**
 * Solves the square linear system A*x = b via Gaussian elimination with partial pivoting.
 * Used for the Colley Matrix method, which is defined as x = A^-1 * b.
 */
object LinearAlgebraUtils {
    fun solve(
        matrix: Array<DoubleArray>,
        vector: DoubleArray,
    ): DoubleArray {
        val n = vector.size
        val a = Array(n) { i -> matrix[i].copyOf() }
        val b = vector.copyOf()

        for (col in 0 until n) {
            var pivotRow = col
            var maxValue = Math.abs(a[col][col])
            for (row in col + 1 until n) {
                val value = Math.abs(a[row][col])
                if (value > maxValue) {
                    maxValue = value
                    pivotRow = row
                }
            }
            if (pivotRow != col) {
                val tmpRow = a[col]
                a[col] = a[pivotRow]
                a[pivotRow] = tmpRow
                val tmpValue = b[col]
                b[col] = b[pivotRow]
                b[pivotRow] = tmpValue
            }

            val pivot = a[col][col]
            if (pivot == 0.0) continue

            for (row in col + 1 until n) {
                val factor = a[row][col] / pivot
                if (factor == 0.0) continue
                for (k in col until n) a[row][k] -= factor * a[col][k]
                b[row] -= factor * b[col]
            }
        }

        val result = DoubleArray(n)
        for (row in n - 1 downTo 0) {
            var sum = b[row]
            for (col in row + 1 until n) sum -= a[row][col] * result[col]
            result[row] = if (a[row][row] != 0.0) sum / a[row][row] else 0.0
        }
        return result
    }
}
