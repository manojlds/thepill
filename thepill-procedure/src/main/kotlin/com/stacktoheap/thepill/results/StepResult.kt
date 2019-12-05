package com.stacktoheap.thepill.results

import com.stacktoheap.thepill.schema.Labels
import com.stacktoheap.thepill.utils.parametersMap
import org.neo4j.graphdb.Path

data class StepResult(@JvmField val result: Map<String, Any>) {
    companion object {
        private val DECISION_NODE_RESULT = listOf("decisionId", "name", "question", "parameters")
        private val LEAF_RESULT = listOf("value")
        fun from(path: Path): StepResult {
            val node = path.endNode()
            val allProperties = node.allProperties
            return when {
                node.hasLabel(Labels.Decision) -> {
                    val parameters = node.parametersMap()
                    StepResult(
                        (allProperties.plus(listOf("parameters" to parameters, "decisionId" to node.id))).filterKeys { DECISION_NODE_RESULT.contains(it) })
                }
                else -> StepResult(allProperties.filterKeys {
                    LEAF_RESULT.contains(
                        it
                    )
                })
            }
        }
    }
}