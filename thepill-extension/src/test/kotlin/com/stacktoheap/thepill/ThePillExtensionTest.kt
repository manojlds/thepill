package com.stacktoheap.thepill

import com.stacktoheap.thepill.schema.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.neo4j.driver.internal.value.MapValue
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.ServerControls
import org.neo4j.harness.TestServerBuilders
import org.neo4j.test.server.HTTP

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThePillExtensionTest {
    private val embeddedDatabaseServer: ServerControls = TestServerBuilders
        .newInProcessBuilder()
        .withProcedure(Schema::class.java)
        .withProcedure(ThePillProcedure::class.java)
        .withExtension("/thepill", ThePillExtension::class.java)
        .withFixture(
            "CALL com.stacktoheap.thepill.schema.generate"
        )
        .newServer();

    @BeforeEach
    fun `set up`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run("MATCH (n) DETACH DELETE n")
            }
        }
    }

    @Test
    fun `test decision tree traversal`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill',parameters:'[{\"name\": \"chosenColor\", \"type\": \"string\"}]'," +
                            "choice: 'result = {relationship: \"COLOR\", properties: {color: \"red\"}}; if(chosenColor === \"blue\") result = {relationship: \"COLOR\" , properties: {color: \"blue\"}};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:COLOR {color: 'red'}]->(red)" +
                            "CREATE (pill)-[:COLOR {color: 'blue'}]->(blue)"

                )
                Thread.sleep(1000) //TODO: HACK. Failing randomly without this
                val response = HTTP.POST(embeddedDatabaseServer.httpURI().resolve("/thepill/make_decision/neo").toString(), mapOf<String, String>())
                assertTrue(response.status() == 200)
                val result = response.content<List<Map<String, String>>>()
                assertThat(result.flatMap { it.values }).containsAll(listOf("knowledge", "ignorance"))
            }
        }
    }

    @Test
    fun `test decision step traversal`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', parameters:'[{\"name\": \"chosenColor\", \"type\": \"string\", \"metadata\":{\"possibleValues\":[{\"displayName\":\"No\",\"value\":false},{\"displayName\":\"Yes\",\"value\":true}]}}]', question: 'Red Pill Or Blue Pill', choice: 'result = {relationship: \"RED\"};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:RED]->(red)" +
                            "CREATE (pill)-[:BLUE]->(blue)"

                )

                Thread.sleep(1000) //TODO: HACK. Failing randomly without this
                val response = HTTP.POST(embeddedDatabaseServer.httpURI().resolve("/thepill/next_step/neo").toString(), mapOf<String, String>())
                assertTrue(response.status() == 200)

                val result = response.content<List<Map<String, Map<String, String>>>>()
                assertThat(result.size).isEqualTo(1)

                val decisionResult = result[0]["stepResult"]
                assertThat(decisionResult!!["question"]).isEqualTo("Red Pill Or Blue Pill")
            }
        }
    }

    @Test
    fun `test decision step traversal with id`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (ready: Decision { name: 'are_you_ready', parameters:'[{\"name\": \"ready\", \"type\": \"boolean\"}]', question: 'Are you ready?', choice: 'result = {relationship: \"YES\"}; if(!ready) result = {relationship: \"NO\"};' })" +
                            "CREATE (pill1: Decision { name: 'pill_decision', parameters:'[{\"name\": \"chosenColor\", \"type\": \"string\"}]', question: 'Red Pill Or Blue Pill?', choice: 'result = {relationship: \"RED\"};' })" +
                            "CREATE (pill2: Decision { name: 'pill_decision', parameters:'[{\"name\": \"chosenColor\", \"type\": \"string\"}]', question: 'Blue Pill or Red Pill?', choice: 'result = {relationship: \"BLUE\"};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(ready)" +
                            "CREATE (ready)-[:YES]->(pill1)" +
                            "CREATE (ready)-[:NO]->(pill2)" +
                            "CREATE (pill1)-[:RED]->(red)" +
                            "CREATE (pill2)-[:RED]->(red)" +
                            "CREATE (pill1)-[:BLUE]->(blue)" +
                            "CREATE (pill2)-[:BLUE]->(blue)"

                )

                val resultFromReadyDecision = session.run("CALL com.stacktoheap.thepill.next_step('are_you_ready', {ready: true}) yield result return result")
                    .single().get(0) as MapValue

                val nextDecisionId = resultFromReadyDecision.get("decisionId").asLong()
                assertTrue(nextDecisionId > 0)
                val nextDecisionName = resultFromReadyDecision.get("name").asString()

                val response = HTTP.POST(embeddedDatabaseServer.httpURI().resolve("/thepill/next_step/$nextDecisionName?decisionId=$nextDecisionId").toString(), mapOf("chosenColor" to "red"))
                assertTrue(response.status() == 200)
                val result = response.content<List<Map<String, Map<String, String>>>>()

                assertThat(result.size).isEqualTo(1)
                val decisionResult = result[0]["stepResult"]

                assertThat(decisionResult!!["value"]).isEqualTo("knowledge")
            }
        }
    }

}