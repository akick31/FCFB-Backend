package com.fcfb.arceus.util.ml

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class XGBoostModelLoader {
    private val logger = LoggerFactory.getLogger(XGBoostModelLoader::class.java)
    private val objectMapper = ObjectMapper()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class XGBoostModel @JsonCreator constructor(
        @JsonProperty("learner") val learner: Learner,
        @JsonProperty("name") val name: String? = "gbtree"
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Learner @JsonCreator constructor(
        @JsonProperty("attributes") val attributes: Map<String, String> = emptyMap(),
        @JsonProperty("gradient_booster") val gradient_booster: GradientBooster,
        @JsonProperty("learner_model_param") val learner_model_param: LearnerModelParam,
        @JsonProperty("objective") val objective: Objective
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GradientBooster @JsonCreator constructor(
        @JsonProperty("model") val model: Model,
        @JsonProperty("name") val name: String? = "gbtree"
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Model @JsonCreator constructor(
        @JsonProperty("gbtree_model_param") val gbtree_model_param: GBTreeModelParam,
        @JsonProperty("tree_info") val tree_info: List<Int> = emptyList(),
        @JsonProperty("trees") val trees: List<Tree> = emptyList()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GBTreeModelParam @JsonCreator constructor(
        @JsonProperty("num_trees") val num_trees: String = "0",
        @JsonProperty("size_leaf_vector") val size_leaf_vector: String = "0"
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Tree @JsonCreator constructor(
        @JsonProperty("base_weights") val base_weights: List<Double> = emptyList(),
        @JsonProperty("categories") val categories: List<Any> = emptyList(),
        @JsonProperty("categories_nodes") val categories_nodes: List<Any> = emptyList(),
        @JsonProperty("categories_segments") val categories_segments: List<Any> = emptyList(),
        @JsonProperty("categories_sizes") val categories_sizes: List<Any> = emptyList(),
        @JsonProperty("default_left") val default_left: List<Boolean> = emptyList(),
        @JsonProperty("id") val id: Int = 0,
        @JsonProperty("left_children") val left_children: List<Int> = emptyList(),
        @JsonProperty("loss_changes") val loss_changes: List<Double> = emptyList(),
        @JsonProperty("parents") val parents: List<Int> = emptyList(),
        @JsonProperty("right_children") val right_children: List<Int> = emptyList(),
        @JsonProperty("split_conditions") val split_conditions: List<Double> = emptyList(),
        @JsonProperty("split_indices") val split_indices: List<Int> = emptyList(),
        @JsonProperty("split_type") val split_type: List<Int> = emptyList(),
        @JsonProperty("sum_hessian") val sum_hessian: List<Double> = emptyList(),
        @JsonProperty("tree_param") val tree_param: TreeParam
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TreeParam @JsonCreator constructor(
        @JsonProperty("num_deleted") val num_deleted: String = "0",
        @JsonProperty("num_feature") val num_feature: String = "0",
        @JsonProperty("num_nodes") val num_nodes: String = "0",
        @JsonProperty("size_leaf_vector") val size_leaf_vector: String = "0"
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LearnerModelParam @JsonCreator constructor(
        @JsonProperty("base_score") val base_score: String = "0.5",
        @JsonProperty("num_class") val num_class: String = "0",
        @JsonProperty("num_feature") val num_feature: String = "0"
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Objective @JsonCreator constructor(
        @JsonProperty("name") val name: String = "binary:logistic"
    )

    // Feature names based on the model (9 features)
    private val featureNames = listOf(
        "score_diff",
        "time_remaining", 
        "down",
        "yards_to_go",
        "ball_location",
        "elo_diff",
        "timeout_diff",
        "field_position",
        "quarter"
    )

    private var model: XGBoostModel? = null

    fun loadModel(): XGBoostModel? {
        if (model != null) {
            return model
        }

        return try {
            val resource = ClassPathResource("wpmodel.json")
            val modelJson = resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            model = objectMapper.readValue(modelJson, XGBoostModel::class.java)
            logger.info("Successfully loaded XGBoost model with ${model?.learner?.gradient_booster?.model?.trees?.size} trees")
            model
        } catch (e: IOException) {
            logger.error("Failed to load XGBoost model from wpmodel.json", e)
            null
        } catch (e: Exception) {
            logger.error("Failed to parse XGBoost model", e)
            null
        }
    }

    fun getFeatureNames(): List<String> = featureNames

    fun getModel(): XGBoostModel? = model ?: loadModel()

    fun getNumFeatures(): Int = featureNames.size

    fun getNumTrees(): Int = model?.learner?.gradient_booster?.model?.trees?.size ?: 0

    fun getBaseScore(): Double = model?.learner?.learner_model_param?.base_score?.toDouble() ?: 0.5
}
