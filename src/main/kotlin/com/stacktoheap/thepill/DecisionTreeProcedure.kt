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
    @Description("CALL com.stacktoheap.thepill.make_decision(tree, facts) - Apply the facts on the chosen decision tree")
    @Throws(IOException::class)
    fun makeDecision(@Name("tree") name: String, @Name("facts") facts: Map<String, String>): Stream<PathResult>? {
        val tree = db?.findNode(Labels.Tree, "name", name)

        return if (tree != null) {
            val makeDecisionTraversal = db!!.traversalDescription()
                .depthFirst()
                .expand(DecisionTreeExpander(facts, log))
                .evaluator(decisionTreeEvaluator)

            makeDecisionTraversal.traverse(tree).stream().map { PathResult(it) }
        } else null
    }
}