package com.stacktoheap.thepill.results

import com.stacktoheap.thepill.schema.Labels
import org.neo4j.graphdb.Path

data class StepResult(@JvmField val result: Map<String, Any>) {
    companion object {
        private val DECISION_NODE_RESULT = listOf("name", "question", "parameters")
        private val LEAF_RESULT = listOf("value")
        fun from(path: Path): StepResult {
            val node = path.endNode()
            return when {
                node.hasLabel(Labels.Decision) -> StepResult(
                    node.allProperties.filterKeys { DECISION_NODE_RESULT.contains(it) })
                else -> StepResult(node.allProperties.filterKeys {
                    LEAF_RESULT.contains(
                        it
                    )
                })
            }
        }
    }
}