package com.fcfb.arceus.util.ml

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class XGBoostPredictor(
    val modelLoader: XGBoostModelLoader
) {
    private val logger = LoggerFactory.getLogger(XGBoostPredictor::class.java)

    /**
     * Predict win probability using the XGBoost model
     * @param features Array of 9 features in order: [score_diff, time_remaining, down, yards_to_go, ball_location, elo_diff, timeout_diff, field_position, quarter]
     * @return Win probability between 0 and 1
     */
    fun predict(features: DoubleArray): Double {
        val model = modelLoader.getModel() ?: run {
            logger.error("Model not loaded, returning default probability")
            return 0.5
        }

        if (features.size != modelLoader.getNumFeatures()) {
            logger.error("Expected ${modelLoader.getNumFeatures()} features, got ${features.size}")
            return 0.5
        }

        try {
            val trees = model.learner.gradient_booster.model.trees
            var prediction = modelLoader.getBaseScore()

            // Sum predictions from all trees
            for (tree in trees) {
                val treePrediction = predictTree(tree, features)
                prediction += treePrediction
            }

            // Apply sigmoid to get probability
            return sigmoid(prediction)
        } catch (e: Exception) {
            logger.error("Error during prediction", e)
            return 0.5
        }
    }

    private fun predictTree(tree: XGBoostModelLoader.Tree, features: DoubleArray): Double {
        var nodeIndex = 0
        
        while (true) {
            val leftChild = tree.left_children[nodeIndex]
            val rightChild = tree.right_children[nodeIndex]
            
            // Leaf node
            if (leftChild == -1 && rightChild == -1) {
                return tree.base_weights[nodeIndex]
            }
            
            val splitIndex = tree.split_indices[nodeIndex]
            val splitCondition = tree.split_conditions[nodeIndex]
            val featureValue = features[splitIndex]
            
            // Determine which child to go to
            val goLeft = if (tree.default_left[nodeIndex]) {
                featureValue <= splitCondition
            } else {
                featureValue < splitCondition
            }
            
            nodeIndex = if (goLeft) leftChild else rightChild
        }
    }

    private fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + Math.exp(-x))
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
        // Use the exact features from Python model - no scaling needed
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
