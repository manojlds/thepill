package com.stacktoheap.thepill

import com.stacktoheap.thepill.schema.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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

    data class Result(val result: String)

    @Test
    fun `test decision tree traversal`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill', parameters: ['chosenColor']," +
                            "choice: 'result = {relationship: \"COLOR\", properties: {color: \"red\"}}; if(chosenColor === \"blue\") result = {relationship: \"COLOR\" , properties: {color: \"blue\"}};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:COLOR {color: 'red'}]->(red)" +
                            "CREATE (pill)-[:COLOR {color: 'blue'}]->(blue)"

                )

                val response = HTTP.POST(embeddedDatabaseServer.httpURI().resolve("/thepill/make_decision/neo").toString(), "{}")
                assertTrue(response.status() == 200)
                val result = response.content<List<Map<String, String>>>()
                assertThat(result.flatMap { it.values }.containsAll(listOf("knowledge", "ignorance")))


            }
        }
    }

}