package com.fcfb.arceus.util.ml

import ml.dmlc.xgboost4j.java.Booster
import ml.dmlc.xgboost4j.java.DMatrix
import ml.dmlc.xgboost4j.java.XGBoost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.PreDestroy
import kotlin.concurrent.read
import kotlin.concurrent.write

@Component
class XGBoostPredictor {
    private val logger = LoggerFactory.getLogger(XGBoostPredictor::class.java)
    private val modelLock = ReentrantReadWriteLock()
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

                booster = classpathResource.openStream().use { XGBoost.loadModel(it) }
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

    fun predict(features: DoubleArray): Double =
        modelLock.read {
            val model =
                booster ?: run {
                    logger.error("Model not loaded, returning default probability")
                    return@read 0.5
                }

            if (features.size != 9) {
                logger.error("Expected 9 features, got ${features.size}")
                return@read 0.5
            }

            try {
                val floatFeatures = features.map { it.toFloat() }.toFloatArray()
                val dMatrix = DMatrix(floatFeatures, 1, features.size, Float.NaN)

                try {
                    val predictions = model.predict(dMatrix)
                    predictions[0][0].toDouble()
                } finally {
                    dMatrix.dispose()
                }
            } catch (e: Exception) {
                logger.error("Error during prediction", e)
                0.5
            }
        }

    @PreDestroy
    internal fun releaseModel() {
        modelLock.write {
            val model = booster
            booster = null
            model?.dispose()
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
