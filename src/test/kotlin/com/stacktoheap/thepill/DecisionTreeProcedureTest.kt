package com.stacktoheap.thepill

import com.stacktoheap.thepill.schema.Schema
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.ServerControls
import org.neo4j.harness.TestServerBuilders
import org.junit.jupiter.api.Assertions.assertTrue


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecisionTreeProcedureTests {

    private val embeddedDatabaseServer: ServerControls = TestServerBuilders
        .newInProcessBuilder()
        .withProcedure(Schema::class.java)
        .withProcedure(DecisionTreeProcedure::class.java)

        .withFixture(
            ""

            + "CALL com.stacktoheap.thepill.schema.generate"
        )
        .newServer();

    @Test
    fun `test decision tree root creation`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                val result = session.run("CREATE (tree:Tree { name: 'neo1' }) RETURN tree.name")
                    .single().get(0).asString()
                assertTrue(result == "neo1")
            }
        }
    }

    @Test
    fun `test leaf creation`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                val result = session.run("CREATE (leaf:Leaf { value: 'red pill' }) RETURN leaf.value")
                    .single().get(0).asString()
                assertTrue(result == "red pill")
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
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill', choice: 'RED' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:RED]->(red)" +
                            "CREATE (pill)-[:BLUE]->(blue)"

                )

                val result = session.run("CALL com.stacktoheap.thepill.make_decision('neo', {}) yield path return path")
                    .single().get(0).asString()

                assertTrue(result == "knowledge")
            }
        }
    }
}