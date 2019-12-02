package com.stacktoheap.thepill.expanders

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.*
import com.stacktoheap.thepill.models.Parameter
import com.stacktoheap.thepill.models.Settings
import com.stacktoheap.thepill.models.StateInfo
import com.stacktoheap.thepill.schema.Labels
import com.stacktoheap.thepill.schema.RelationshipTypes
import com.stacktoheap.thepill.utils.JsonUtils
import com.stacktoheap.thepill.utils.parameters
import org.neo4j.graphdb.*
import org.neo4j.graphdb.traversal.BranchState
import org.neo4j.logging.Log

class DecisionTreeExpander(private val facts: Map<String, Any>, private val ignoreMissingParameters: Boolean, private val settings: Settings, private val log: Log?):
    PathExpander<StateInfo> {
    private val scriptEngineManager = javax.script.ScriptEngineManager()


    override fun reverse(): PathExpander<StateInfo>? {
        return null
    }

    override fun expand(path: Path, state: BranchState<StateInfo>?): Iterable<Relationship> {
        val endNode = path.endNode()
        return when {
            settings.propertyBasedLeaves and endNode.getProperty(settings.leavesProperty, false).toString().toBoolean()  -> listOf()
            !settings.propertyBasedLeaves and endNode.hasLabel(Labels.Leaf) -> listOf()
            endNode.hasRelationship(
                Direction.OUTGOING,
                RelationshipTypes.HAS
            ) -> endNode.getRelationships(
                Direction.OUTGOING,
                RelationshipTypes.HAS
            )
            endNode.hasLabel(Labels.Decision) -> try {
                val parameters = endNode.parameters()

                when {
                    facts.keys.containsAll(parameters.map { it.name  }) -> evaluateChoiceScript(endNode, parameters)
                    ignoreMissingParameters -> endNode.getRelationships(Direction.OUTGOING)
                    else -> listOf()
                }
            } catch (e: Exception) {
                log!!.info("Decision Tree Traversal failed", e)
                listOf<Relationship>()
            }
            else -> listOf()
        }

    }

    private fun evaluateChoiceScript(
        endNode: Node,
        parameters: List<Parameter>
    ): Iterable<Relationship> {
        val engine = scriptEngineManager.getEngineByName("JavaScript")
        for (parameter in parameters) {
            engine.put(parameter.name, parameter.valueFrom(facts))
        }
        val choiceScript = endNode.allProperties["choice"] as String

        engine.eval(choiceScript)

        val result = engine.get("result") as Map<String, Any>
        val relationshipType = result["relationship"] as String

        val matchingRelationships =
            endNode.getRelationships(Direction.OUTGOING, RelationshipType.withName(relationshipType))

        val expectedProps = sanitizeType(result["properties"] as? Map<String, Any>)

        return if (expectedProps.isNullOrEmpty()) {
            matchingRelationships
        } else {
            matchingRelationships.filter {
                expectedProps.entries.containsAll(it.allProperties.entries)
            }
        }
    }

    private fun sanitizeType(map: Map<String, Any>?): Map<String, Any>? {
        return map?.mapValues { it: Map.Entry<String, Any> ->
            when (it.value) {
                is Int -> (it.value as Int).toLong()
                else -> it.value
            }
        }
    }

}