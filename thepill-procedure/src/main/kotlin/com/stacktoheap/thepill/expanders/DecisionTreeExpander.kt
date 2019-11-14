package com.stacktoheap.thepill.expanders

import com.stacktoheap.thepill.models.Settings
import com.stacktoheap.thepill.models.StateInfo
import com.stacktoheap.thepill.schema.Labels
import com.stacktoheap.thepill.schema.RelationshipTypes
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
                val nodeProperties = endNode.allProperties
                val parameters = nodeProperties["parameters"] as? Array<String> ?: arrayOf()

                when {
                    facts.keys.containsAll(parameters.asList()) -> evaluateChoiceScript(nodeProperties, endNode)
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
        nodeProperties: MutableMap<String, Any>,
        endNode: Node
    ): Iterable<Relationship> {
        val engine = scriptEngineManager.getEngineByName("JavaScript")
        for (key in facts) {
            engine.put(key.key, key.value)
        }

        val choiceScript = nodeProperties["choice"] as String

        engine.eval(choiceScript)

        val result = engine.get("result") as Map<String, Any>
        val relationshipType = result["relationship"] as String

        val matchingRelationships =
            endNode.getRelationships(Direction.OUTGOING, RelationshipType.withName(relationshipType))

        val expectedProps = result["properties"] as? Map<String, Any>

        return if (expectedProps.isNullOrEmpty()) {
            matchingRelationships
        } else {
            matchingRelationships.filter {
                expectedProps.entries.containsAll(it.allProperties.entries)
            }
        }
    }

}