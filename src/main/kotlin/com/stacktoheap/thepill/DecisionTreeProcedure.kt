package com.stacktoheap.thepill

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Path
import org.neo4j.logging.Log
import org.neo4j.procedure.*
import java.io.IOException
import java.util.stream.Stream;


class PathResult(val path: Path)

class DecisionTreeProcedure {
    @Context
    var db: GraphDatabaseService? = null

    @Context
    var log: Log? = null

    @Procedure(name = "com.stacktoheap.thepill.make_decision", mode = Mode.READ)
    @Description("CALL com.stacktoheap.thepill.make_decision(tree, facts) - Apply the facts on the chosen decision tree")
    @Throws(IOException::class)
    fun makeDecision(@Name("tree") id: String, @Name("facts") facts: Map<String, String>): Stream<PathResult>? {
        return null
    }
}