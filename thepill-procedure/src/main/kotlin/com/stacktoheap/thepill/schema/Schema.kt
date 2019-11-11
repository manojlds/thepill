package com.stacktoheap.thepill.schema

import com.stacktoheap.thepill.results.StringResult
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.logging.Log
import org.neo4j.procedure.Context
import org.neo4j.procedure.Description
import org.neo4j.procedure.Mode
import org.neo4j.procedure.Procedure
import java.io.IOException
import java.util.stream.Stream

class Schema {

    @Context
    @JvmField
    var db: GraphDatabaseService? = null

    @Context
    @JvmField
    var log: Log? = null

    @Procedure(name = "com.stacktoheap.thepill.schema.generate", mode = Mode.SCHEMA)
    @Description("CALL com.stacktoheap.thepill.schema.generate - generate schema")
    @Throws(IOException::class)
    fun generate(): Stream<StringResult> {
        val schema = db!!.schema()
        if (!schema.getIndexes(Labels.Tree).iterator().hasNext()) {
            schema.constraintFor(Labels.Tree)
                .assertPropertyIsUnique("name")
                .create()
        }
        if (!schema.getIndexes(Labels.Decision).iterator().hasNext()) {
            schema.constraintFor(Labels.Decision)
                .assertPropertyIsUnique("name")
                .create()
        }
        if (!schema.getIndexes(Labels.Leaf).iterator().hasNext()) {
            schema.constraintFor(Labels.Leaf)
                .assertPropertyIsUnique("value")
                .create()
        }
        
        return Stream.of(StringResult("Schema Generated"))
    }
}