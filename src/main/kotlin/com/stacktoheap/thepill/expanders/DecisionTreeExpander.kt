package com.stacktoheap.thepill.expanders

import com.stacktoheap.thepill.models.StateInfo
import com.stacktoheap.thepill.schema.Labels
import com.stacktoheap.thepill.schema.RelationshipTypes
import org.neo4j.graphdb.*
import org.neo4j.graphdb.traversal.BranchState
import org.neo4j.logging.Log
import javax.script.Bindings

class DecisionTreeExpander(val facts: Map<String, String>, val log: Log?):
    PathExpander<StateInfo> {
    private val scriptEngineManager = javax.script.ScriptEngineManager()


    override fun reverse(): PathExpander<StateInfo>? {
        return null
    }

    override fun expand(path: Path, state: BranchState<StateInfo>?): Iterable<Relationship> {
        val endNode = path.endNode()
        return when {
            endNode.hasLabel(Labels.Leaf) -> listOf()
            endNode.hasRelationship(
                Direction.OUTGOING,
                RelationshipTypes.HAS
            ) -> endNode.getRelationships(
                Direction.OUTGOING,
                RelationshipTypes.HAS
            )
            endNode.hasLabel(Labels.Decision) -> try {
                val engine = scriptEngineManager.getEngineByName("JavaScript")
                for(key in facts) {
                    engine.put(key.key, key.value)
                }
                val nodeProperties = endNode.allProperties
                val choiceScript = nodeProperties["choice"] as String

                engine.eval(choiceScript)

                val result = engine.get("result") as Map<String, Any>
                val relationshipType = result["relationship"] as String

                endNode.getRelationships(Direction.OUTGOING, RelationshipType.withName(relationshipType))
            } catch (e: Exception) {
                log!!.info("Decision Tree Traversal failed", e)
                listOf<Relationship>()
            }
            else -> listOf()
        }

    }

}