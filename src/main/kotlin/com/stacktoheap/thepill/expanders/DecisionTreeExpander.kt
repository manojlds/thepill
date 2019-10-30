package com.stacktoheap.thepill.expanders

import com.stacktoheap.thepill.models.StateInfo
import com.stacktoheap.thepill.schema.Labels
import com.stacktoheap.thepill.schema.RelationshipTypes
import org.neo4j.graphdb.*
import org.neo4j.graphdb.traversal.BranchState
import org.neo4j.logging.Log

class DecisionTreeExpander(val facts: Map<String, String>, val log: Log?):
    PathExpander<StateInfo> {
    override fun reverse(): PathExpander<StateInfo>? {
        return null
    }

    override fun expand(path: Path, state: BranchState<StateInfo>?): Iterable<Relationship> {
        return when {
            path.endNode().hasLabel(Labels.Leaf) -> listOf()
            path.endNode().hasRelationship(
                Direction.OUTGOING,
                RelationshipTypes.HAS
            ) -> path.endNode().getRelationships(
                Direction.OUTGOING,
                RelationshipTypes.HAS
            )
            path.endNode().hasLabel(Labels.Decision) -> try {
                path.endNode().getRelationships(Direction.OUTGOING, chooseNext(path.endNode()))
            } catch (e: Exception) {
                log!!.debug("Decision Tree Traversal failed", e)
                listOf<Relationship>()
            }
            else -> listOf()
        }

    }

    fun chooseNext(decision: Node): RelationshipType {
        val allProperties = decision.allProperties
        return RelationshipType.withName("")
    }

}