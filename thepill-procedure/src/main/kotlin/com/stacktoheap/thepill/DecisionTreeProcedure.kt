package com.stacktoheap.thepill

import com.stacktoheap.thepill.evaluators.DecisionTreeEvaluator
import com.stacktoheap.thepill.expanders.DecisionTreeExpander
import com.stacktoheap.thepill.results.PathResult
import com.stacktoheap.thepill.schema.Labels
import org.neo4j.graphdb.*
import org.neo4j.logging.Log
import org.neo4j.procedure.*
import java.io.IOException
import java.util.stream.Stream


class DecisionTreeProcedure {
    @Context
    @JvmField
    var db: GraphDatabaseService? = null

    @Context
    @JvmField
    var log: Log? = null

    companion object {
        val decisionTreeEvaluator = DecisionTreeEvaluator()
    }

    @Procedure(name = "com.stacktoheap.thepill.make_decision", mode = Mode.READ)
    @Description("CALL com.stacktoheap.thepill.make_decision(tree, facts, ignoreMissingParameters) - Apply the facts on the chosen decision tree / node")
    @Throws(IOException::class)
    fun makeDecision(@Name("tree") name: String, @Name("facts") facts: Map<String, Any>, @Name("ignoreMissingParameters", defaultValue = "false") ignoreMissingParameters: Boolean = false): Stream<PathResult>? {
        val startNode = db?.findNode(Labels.Tree, "name", name) ?: db?.findNode(Labels.Decision, "name", name)

        return if (startNode != null) {
            val makeDecisionTraversal = db!!.traversalDescription()
                .depthFirst()
                .expand(DecisionTreeExpander(facts, ignoreMissingParameters, log))
                .evaluator(decisionTreeEvaluator)

            makeDecisionTraversal.traverse(startNode).stream().map { PathResult(it) }
        } else null
    }
}