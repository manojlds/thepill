package com.stacktoheap.thepill

import com.stacktoheap.thepill.schema.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.neo4j.driver.internal.value.NodeValue
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.ServerControls
import org.neo4j.harness.TestServerBuilders


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecisionTreeProcedureTests {

    private val embeddedDatabaseServer: ServerControls = TestServerBuilders
        .newInProcessBuilder()
        .withProcedure(Schema::class.java)
        .withProcedure(DecisionTreeProcedure::class.java)

        .withFixture(
             "CALL com.stacktoheap.thepill.schema.generate"
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
                            "CREATE (tree:Tree { name: 'neo2' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill', choice: 'result = {relationship: \"RED\"};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:RED]->(red)" +
                            "CREATE (pill)-[:BLUE]->(blue)"

                )

                val result = session.run("CALL com.stacktoheap.thepill.make_decision('neo2', {}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(result.get("value").asString() == "knowledge")
            }
        }
    }

    @Test
    fun `test decision tree traversal with facts`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo3' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill'," +
                                    "choice: 'result = {relationship: \"RED\"}; if(chosenColor === \"blue\") result = {relationship: \"BLUE\"};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:RED]->(red)" +
                            "CREATE (pill)-[:BLUE]->(blue)"

                )

                val result = session.run("CALL com.stacktoheap.thepill.make_decision('neo3', {chosenColor: \"blue\"}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(result.get("value").asString() == "ignorance")
            }
        }
    }

    @Test
    fun `test decision tree traversal with relationship properties`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo4' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill'," +
                                    "choice: 'result = {relationship: \"COLOR\", properties: {color: \"red\"}}; if(chosenColor === \"blue\") result = {relationship: \"COLOR\" , properties: {color: \"blue\"}};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:COLOR {color: 'red'}]->(red)" +
                            "CREATE (pill)-[:COLOR {color: 'blue'}]->(blue)"

                )

                val result = session.run("CALL com.stacktoheap.thepill.make_decision('neo4', {chosenColor: \"blue\"}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(result.get("value").asString() == "ignorance")
            }
        }
    }

    @Test
    fun `test multi decision result`() {
        GraphDatabase.driver(embeddedDatabaseServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo5' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill'," +
                                    "choice: 'result = {relationship: \"COLOR\" };'})" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:COLOR {color: 'red'}]->(red)" +
                            "CREATE (pill)-[:COLOR {color: 'blue'}]->(blue)"

                )

                val result = session.run("CALL com.stacktoheap.thepill.make_decision('neo5', {chosenColor: \"blue\"}) yield path return last(nodes(path)).value").list()
                assertThat(result.map { it.get(0).asString() }).containsAll(listOf("knowledge", "ignorance"))
            }
        }
    }
}