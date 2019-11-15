package com.stacktoheap.thepill

import com.stacktoheap.thepill.evaluators.DecisionTreeEvaluator
import com.stacktoheap.thepill.evaluators.StepEvaluator
import com.stacktoheap.thepill.expanders.DecisionTreeExpander
import com.stacktoheap.thepill.models.Settings
import com.stacktoheap.thepill.results.PathResult
import com.stacktoheap.thepill.results.StepResult
import com.stacktoheap.thepill.schema.Labels
import org.neo4j.graphdb.*
import org.neo4j.logging.Log
import org.neo4j.procedure.*
import java.io.IOException
import java.util.stream.Stream


class ThePillProcedure {
    @Context
    @JvmField
    var db: GraphDatabaseService? = null

    @Context
    @JvmField
    var log: Log? = null

    companion object {
        lateinit var decisionTreeEvaluator: DecisionTreeEvaluator
        lateinit var stepEvaluator: StepEvaluator

        lateinit var settings: Settings

        fun initSettings(config: Map<String, String>): Settings {
            this.settings = Settings.from(config)
            this.decisionTreeEvaluator = DecisionTreeEvaluator(this.settings)
            this.stepEvaluator = StepEvaluator(this.settings)
            return this.settings
        }
    }
    @Procedure(name = "com.stacktoheap.thepill.make_decision", mode = Mode.READ)
    @Description("CALL com.stacktoheap.thepill.make_decision(tree, facts, ignoreMissingParameters) - Apply the facts on the chosen decision tree / node")
    @Throws(IOException::class)
    fun makeDecision(@Name("tree") name: String, @Name("facts") facts: Map<String, Any>, @Name("ignoreMissingParameters", defaultValue = "false") ignoreMissingParameters: Boolean = false): Stream<PathResult>? {
        val startNode = db?.findNode(Labels.Tree, "name", name) ?: db?.findNode(Labels.Decision, "name", name)

        return if (startNode != null) {
            val makeDecisionTraversal = db!!.traversalDescription()
                .depthFirst()
                .expand(DecisionTreeExpander(facts, ignoreMissingParameters, settings, log))
                .evaluator(decisionTreeEvaluator)

            makeDecisionTraversal.traverse(startNode).stream().map { PathResult(it) }
        } else null
    }

    @Procedure(name = "com.stacktoheap.thepill.next_step", mode = Mode.READ)
    @Description("CALL com.stacktoheap.thepill.next_step(current_node_name, node_id, facts) - Apply decisions one step at a time")
    @Throws(IOException::class)
    fun nextStep(@Name("current_node") name: String, @Name("facts") facts: Map<String, Any>, @Name("node_id", defaultValue = "-1") nodeId: Long = -1): Stream<StepResult>? {
        val startNodes = when {
            nodeId != -1L -> listOf(db?.getNodeById(nodeId))
            else -> nodesByName(name)
        }

        return if (startNodes?.isNotEmpty() == true) {
            val makeDecisionTraversal = db!!.traversalDescription()
                .depthFirst()
                .expand(DecisionTreeExpander(facts, false, settings, log))
                .evaluator(stepEvaluator)

            makeDecisionTraversal.traverse(startNodes).stream().map { StepResult.from(it) }
        } else null
    }

    private fun nodesByName(name: String): List<Node?>? {
        val treeNode = db?.findNode(Labels.Tree, "name", name)
        return if (treeNode != null) {
            listOf(treeNode)
        } else {
            db?.findNodes(Labels.Decision, "name", name)?.asSequence()?.toList()
        }
    }

}