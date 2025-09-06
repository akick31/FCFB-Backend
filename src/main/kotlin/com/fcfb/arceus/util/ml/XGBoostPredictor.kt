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
            val modelFile = File("src/main/resources/wpmodel.json")
            if (!modelFile.exists()) {
                logger.error("Model file not found: ${modelFile.absolutePath}")
                return
            }
            
            booster = XGBoost.loadModel(modelFile.absolutePath)
            logger.info("XGBoost model loaded successfully")
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
        val model = booster ?: run {
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
        eloDiffTime: Double
    ): DoubleArray {
        // Use the exact features from Python model - MUST match Python order exactly
        return doubleArrayOf(
            down.toDouble(),                    // down
            distance.toDouble(),                // distance  
            position.toDouble(),                // position
            margin.toDouble(),                  // margin
            secondsLeftGame.toDouble(),         // seconds_left_game
            secondsLeftHalf.toDouble(),         // seconds_left_half
            half.toDouble(),                    // half
            hadFirstPossession.toDouble(),      // had_first_possession
            eloDiffTime                         // elo_diff_time
        )
    }
}
