package com.stacktoheap.thepill
import org.codehaus.jackson.map.ObjectMapper
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.helpers.collection.MapUtil
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("")
class ThePillExtension(@Context val graphDb: GraphDatabaseService) {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/make_decision/{decisionName}")
    fun make_decision(@PathParam("decisionName") decisionName: String, body: String): Response {
        val queryArguments = MapUtil.map("decisionName", decisionName, "facts", parseMap(body))
        val result = graphDb.execute(DECISION_QUERY, queryArguments)
        val response = result.asSequence().map { it.mapValues { it.value } }.toList()

        return asResponse(response)
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/next_step/{decisionName}")
    fun next_step(@PathParam("decisionName") decisionName: String, @QueryParam("decisionId") decisionId: Long?, body: String): Response {
        val (query, queryArguments) =
            when(decisionId) {
                null -> Pair(STEP_QUERY, MapUtil.map("decisionName", decisionName, "facts", parseMap(body)))
                else -> Pair(STEP_QUERY_WITH_ID, MapUtil.map("decisionName", decisionName, "facts", parseMap(body), "decisionId", decisionId))
            }

        val result = graphDb.execute(query, queryArguments)
        val response = result.asSequence().map { it.mapValues { it.value } }.toList()

        return asResponse(response)
    }

    companion object {
        val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
        val DECISION_QUERY = "CALL com.stacktoheap.thepill.make_decision(\$decisionName, \$facts, true) yield path return last(nodes(path)).value as result"
        val STEP_QUERY = "CALL com.stacktoheap.thepill.next_step(\$decisionName, \$facts) yield result return result as stepResult"
        val STEP_QUERY_WITH_ID = "CALL com.stacktoheap.thepill.next_step(\$decisionName, \$facts, \$decisionId) yield result return result as stepResult"
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