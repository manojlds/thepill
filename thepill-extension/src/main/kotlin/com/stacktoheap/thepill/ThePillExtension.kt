package com.stacktoheap.thepill
import com.stacktoheap.thepill.utils.JsonUtils
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.helpers.collection.MapUtil
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("")
class ThePillExtension(@Context val graphDb: GraphDatabaseService) {

    @UnstableDefault
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/make_decision/{decisionName}")
    fun makeDecision(@PathParam("decisionName") decisionName: String, body: String): Response {
        val queryArguments = MapUtil.map("decisionName", decisionName, "facts", parseMap(body))
        val result = graphDb.execute(DECISION_QUERY, queryArguments)
        val response = result.asSequence().map { it.mapValues { it.value } }.toList()

        return asResponse(response)
    }

    @UnstableDefault
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/next_step/{decisionName}")
    fun nextStep(@PathParam("decisionName") decisionName: String, @QueryParam("decisionId") decisionId: Long?, body: String): Response {
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
        const val DECISION_QUERY = "CALL com.stacktoheap.thepill.make_decision(\$decisionName, \$facts, true) yield path return last(nodes(path)).value as result"
        const val STEP_QUERY = "CALL com.stacktoheap.thepill.next_step(\$decisionName, \$facts) yield result return result as stepResult"
        const val STEP_QUERY_WITH_ID = "CALL com.stacktoheap.thepill.next_step(\$decisionName, \$facts, \$decisionId) yield result return result as stepResult"
    }

    @UnstableDefault
    private fun asResponse(result: List<Map<String, Any>>) = Response.ok().entity(formatMap(result)).build()

    @UnstableDefault
    private fun formatMap(result: List<Map<String, Any>>) = Json.stringify(JsonUtils.MapSerializer.list, result)

    @UnstableDefault
    private fun parseMap(value: String?): Map<String, Any?> =
        if (value == null || value.isNullOrBlank() || value == "null") emptyMap()
        else {
            val v = value.trim('"', ' ', '\t', '\n', '\r')
            Json.parse(JsonUtils.MapSerializer, v)
        }
}