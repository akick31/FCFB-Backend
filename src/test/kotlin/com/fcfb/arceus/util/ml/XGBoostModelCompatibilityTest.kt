package com.fcfb.arceus.util.ml

import ml.dmlc.xgboost4j.java.DMatrix
import ml.dmlc.xgboost4j.java.XGBoost
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

class XGBoostModelCompatibilityTest {
    @Suppress("DEPRECATION")
    @ParameterizedTest
    @CsvSource(
        "1,10,50,0,3600,1800,1,0,0",
        "4,2,95,-7,30,30,2,1,-100",
        "2,7,30,14,900,900,2,0,200",
        "3,NaN,60,-3,120,120,2,1,NaN",
    )
    fun `stream loading and explicit missing value retain file model predictions`(
        down: Double,
        distance: Double,
        position: Double,
        margin: Double,
        secondsLeftGame: Double,
        secondsLeftHalf: Double,
        half: Double,
        hadFirstPossession: Double,
        eloDiffTime: Double,
    ) {
        val features =
            doubleArrayOf(down, distance, position, margin, secondsLeftGame, secondsLeftHalf, half, hadFirstPossession, eloDiffTime)
        val floats = features.map { it.toFloat() }.toFloatArray()
        val modelFile = File("src/main/resources/wpmodel.json")
        val fileModel = XGBoost.loadModel(modelFile.absolutePath)
        try {
            val streamModel = modelFile.inputStream().use { XGBoost.loadModel(it) }
            try {
                val legacyMatrix = DMatrix(floats, 1, floats.size)
                try {
                    val matrix = DMatrix(floats, 1, floats.size, Float.NaN)
                    try {
                        val expected = fileModel.predict(legacyMatrix)[0][0].toDouble()
                        assertEquals(expected, streamModel.predict(matrix)[0][0].toDouble())
                        val predictor = XGBoostPredictor()
                        try {
                            assertEquals(expected, predictor.predict(features))
                        } finally {
                            predictor.releaseModel()
                        }
                    } finally {
                        matrix.dispose()
                    }
                } finally {
                    legacyMatrix.dispose()
                }
            } finally {
                streamModel.dispose()
            }
        } finally {
            fileModel.dispose()
        }
    }
}
