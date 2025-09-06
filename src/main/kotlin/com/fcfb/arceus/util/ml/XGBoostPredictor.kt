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

            // Try to load from classpath first (for JAR deployment)
            val classpathResource = this::class.java.classLoader.getResource("wpmodel.json")
            if (classpathResource != null) {
                logger.info("Found model in classpath: ${classpathResource.path}")

                // Copy resource to temporary file since XGBoost needs a file path
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

            // Fallback to file system (for development)
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

    /**
     * Predict win probability using the XGBoost model
     * @param features Array of 9 features in order: [down, distance, position, margin, seconds_left_game, seconds_left_half, half, had_first_possession, elo_diff_time]
     * @return Win probability between 0 and 1
     */
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
            // Create DMatrix for prediction - convert DoubleArray to FloatArray
            val floatFeatures = features.map { it.toFloat() }.toFloatArray()
            val dMatrix = DMatrix(floatFeatures, 1, features.size)

            // Get prediction from XGBoost
            val predictions = model.predict(dMatrix)

            // XGBoost4J returns Array<FloatArray>, so we need to get the first FloatArray and then the first Float
            val rawPrediction = predictions[0][0].toDouble()
            return rawPrediction
        } catch (e: Exception) {
            logger.error("Error during prediction", e)
            return 0.5
        }
    }

    /**
     * Create feature array from game context - matches Python model features
     * Features: down, distance, position, margin, seconds_left_game, seconds_left_half, half, had_first_possession, elo_diff_time
     */
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
        // Use the exact features from Python model - MUST match Python order exactly
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
