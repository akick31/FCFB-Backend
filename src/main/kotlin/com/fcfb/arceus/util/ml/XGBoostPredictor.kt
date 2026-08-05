package com.fcfb.arceus.util.ml

import ml.dmlc.xgboost4j.java.Booster
import ml.dmlc.xgboost4j.java.DMatrix
import ml.dmlc.xgboost4j.java.XGBoost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

@Component
class XGBoostPredictor {
    private val logger = LoggerFactory.getLogger(XGBoostPredictor::class.java)
    private var booster: Booster? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            logger.info("Attempting to load XGBoost model...")

            val classpathResource = this::class.java.classLoader.getResource("wpmodel.json")
            if (classpathResource != null) {
                logger.info("Found model in classpath: ${classpathResource.path}")

                val tempFile = File.createTempFile("wpmodel", ".json")
                tempFile.deleteOnExit()

                classpathResource.openStream().use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                logger.info("Copied model to temporary file: ${tempFile.absolutePath}")
                booster = XGBoost.loadModel(tempFile.absolutePath)
                logger.info("XGBoost model loaded successfully from classpath")
                return
            }

            val modelFile = File("src/main/resources/wpmodel.json")
            logger.info("Checking for model file at: ${modelFile.absolutePath}")
            if (modelFile.exists()) {
                booster = XGBoost.loadModel(modelFile.absolutePath)
                logger.info("XGBoost model loaded successfully from file system")
                return
            }

            logger.error("Model file not found in classpath or file system")
        } catch (e: Exception) {
            logger.error("Error loading XGBoost model", e)
        }
    }

    fun predict(features: DoubleArray): Double {
        val model =
            booster ?: run {
                logger.error("Model not loaded, returning default probability")
                return 0.5
            }

        if (features.size != 9) {
            logger.error("Expected 9 features, got ${features.size}")
            return 0.5
        }

        try {
            val floatFeatures = features.map { it.toFloat() }.toFloatArray()
            val dMatrix = DMatrix(floatFeatures, 1, features.size)

            val predictions = model.predict(dMatrix)

            val rawPrediction = predictions[0][0].toDouble()
            return rawPrediction
        } catch (e: Exception) {
            logger.error("Error during prediction", e)
            return 0.5
        }
    }

    fun createFeatureArray(
        down: Int,
        distance: Int,
        position: Int,
        margin: Int,
        secondsLeftGame: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): DoubleArray {
        return doubleArrayOf(
            down.toDouble(),
            distance.toDouble(),
            position.toDouble(),
            margin.toDouble(),
            secondsLeftGame.toDouble(),
            secondsLeftHalf.toDouble(),
            half.toDouble(),
            hadFirstPossession.toDouble(),
            eloDiffTime,
        )
    }
}
