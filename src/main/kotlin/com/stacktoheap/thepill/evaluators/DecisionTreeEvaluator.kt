package com.stacktoheap.thepill.evaluators

import com.stacktoheap.thepill.models.StateInfo
import com.stacktoheap.thepill.schema.Labels
import org.neo4j.graphdb.Path
import org.neo4j.graphdb.traversal.BranchState
import org.neo4j.graphdb.traversal.Evaluation
import org.neo4j.graphdb.traversal.PathEvaluator

class DecisionTreeEvaluator : PathEvaluator<StateInfo> {

    override fun evaluate(path: Path, branchState: BranchState<StateInfo>): Evaluation {
        return when {
            path.endNode().hasLabel(Labels.Leaf) -> Evaluation.INCLUDE_AND_PRUNE
            else -> Evaluation.EXCLUDE_AND_CONTINUE
        }
    }

    override fun evaluate(path: Path): Evaluation? {
        return null
    }
}