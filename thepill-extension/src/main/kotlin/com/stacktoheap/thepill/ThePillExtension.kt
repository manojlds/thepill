package com.stacktoheap.thepill
import org.neo4j.string.UTF8
import org.neo4j.graphdb.GraphDatabaseService
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.codehaus.jackson.map.ObjectMapper
import org.neo4j.helpers.collection.MapUtil

@Path("/make_decision")
class ThePillExtension(@Context val graphDb: GraphDatabaseService) {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{decisionName}")
    fun make_decision(@PathParam("decisionName") decisionName: String, body: String): Response {
        val queryArguments = MapUtil.map("decisionName", decisionName, "facts", parseMap(body))
        val result = graphDb.execute(DECISION_QUERY, queryArguments)
        val response = result.asSequence().map { it.mapValues { it.value } }.toList()

        return asResponse(response)
    }

    companion object {
        val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
        val DECISION_QUERY = "CALL com.stacktoheap.thepill.make_decision(\$decisionName, \$facts, true) yield path return last(nodes(path)).value as result"
    }

    fun asResponse(result: List<Map<String, Any?>>) = Response.ok().entity(formatMap(result)).build()

    private fun formatMap(result: List<Map<String, Any?>>) = OBJECT_MAPPER.writeValueAsString(result)

    private fun parseMap(value: String?): Map<String, Any> =
        if (value == null || value.isNullOrBlank() || value == "null") emptyMap()
        else {
            val v = value.trim('"', ' ', '\t', '\n', '\r')
            OBJECT_MAPPER.readValue(v, Map::class.java) as Map<String, Any>
        }
}