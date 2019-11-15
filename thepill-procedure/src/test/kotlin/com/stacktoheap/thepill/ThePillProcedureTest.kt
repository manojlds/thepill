package com.stacktoheap.thepill

import com.stacktoheap.thepill.schema.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.neo4j.driver.internal.value.MapValue
import org.neo4j.driver.internal.value.NodeValue
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.ServerControls
import org.neo4j.harness.TestServerBuilder
import org.neo4j.harness.TestServerBuilders


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThePillProcedureLeafEntityTest {

    private val dbServer: ServerControls = getDbServer().newServer();

    private fun getDbServer(): TestServerBuilder = TestServerBuilders
        .newInProcessBuilder()
        .withProcedure(Schema::class.java)
        .withProcedure(ThePillProcedure::class.java)
        .withFixture(
            "CALL com.stacktoheap.thepill.schema.generate"
        )

    @BeforeEach
    fun `set up`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run("MATCH (n) DETACH DELETE n")
            }
        }
    }

    @Test
    fun `test decision tree root creation`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                val result = session.run("CREATE (tree:Tree { name: 'neo' }) RETURN tree.name")
                    .single().get(0).asString()
                assertTrue(result == "neo")
            }
        }
    }

    @Test
    fun `test leaf creation`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                val result = session.run("CREATE (leaf:Leaf { value: 'red pill' }) RETURN leaf.value")
                    .single().get(0).asString()
                assertTrue(result == "red pill")
            }
        }
    }

    @Test
    fun `test decision tree traversal`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill', choice: 'result = {relationship: \"RED\"};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:RED]->(red)" +
                            "CREATE (pill)-[:BLUE]->(blue)"

                )

                val resultFromTree = session.run("CALL com.stacktoheap.thepill.make_decision('neo', {}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(resultFromTree.get("value").asString() == "knowledge")

                val resultFromDecision = session.run("CALL com.stacktoheap.thepill.make_decision('Red Pill Or Blue Pill', {}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(resultFromDecision.get("value").asString() == "knowledge")
            }
        }
    }

    @Test
    fun `test decision step traversal`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', parameters:['chosenColor'], question: 'Red Pill Or Blue Pill', choice: 'result = {relationship: \"RED\"};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:RED]->(red)" +
                            "CREATE (pill)-[:BLUE]->(blue)"

                )

                val resultFromTree = session.run("CALL com.stacktoheap.thepill.next_step('neo', {}) yield result return result")
                    .single().get(0) as MapValue

                assertTrue(resultFromTree.get("decisionId").asLong() > 0)
                assertTrue(resultFromTree.get("name").asString() == "Red Pill Or Blue Pill")
                assertTrue(resultFromTree.get("question").asString() == "Red Pill Or Blue Pill")
                assertThat(resultFromTree.get("parameters").asList().map {it.toString()}.containsAll(listOf("chosenColor")))

                val resultFromDecision = session.run("CALL com.stacktoheap.thepill.next_step('Red Pill Or Blue Pill', {chosenColor: 'red'}) yield result return result")
                    .single().get(0) as MapValue

                assertThat(resultFromDecision.get("value").asString()).isEqualTo("knowledge")
            }
        }
    }

    @Test
    fun `test decision step traversal from multiple decisions`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill1: Decision { name: 'pill_decision', parameters:['chosenColor'], question: 'Red Pill Or Blue Pill?', choice: 'result = {relationship: \"RED\"};' })" +
                            "CREATE (pill2: Decision { name: 'pill_decision', parameters:['chosenColor'], question: 'Blue Pill or Red Pill?', choice: 'result = {relationship: \"BLUE\"};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill1)" +
                            "CREATE (tree)-[:HAS]->(pill2)" +
                            "CREATE (pill1)-[:RED]->(red)" +
                            "CREATE (pill2)-[:RED]->(red)" +
                            "CREATE (pill1)-[:BLUE]->(blue)" +
                            "CREATE (pill2)-[:BLUE]->(blue)"

                )

                val resultFromDecision = session.run("CALL com.stacktoheap.thepill.next_step('pill_decision', {chosenColor: 'red'}) yield result return result")
                    .list()

                assertThat(resultFromDecision.map{ it.get("result").get("value")}.map{it.asString()}).containsAll(listOf("knowledge", "ignorance"))
            }
        }
    }

    @Test
    fun `test decision step traversal disambiguation with id`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (ready: Decision { name: 'are_you_ready', parameters:['ready'], question: 'Are you ready?', choice: 'result = {relationship: \"YES\"}; if(!ready) result = {relationship: \"NO\"};' })" +
                            "CREATE (pill1: Decision { name: 'pill_decision', parameters:['chosenColor'], question: 'Red Pill Or Blue Pill?', choice: 'result = {relationship: \"RED\"};' })" +
                            "CREATE (pill2: Decision { name: 'pill_decision', parameters:['chosenColor'], question: 'Blue Pill or Red Pill?', choice: 'result = {relationship: \"BLUE\"};' })" +
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
                assertTrue(nextDecisionName == "pill_decision")
                assertTrue(resultFromReadyDecision.get("question").asString() == "Red Pill Or Blue Pill?")
                assertThat(resultFromReadyDecision.get("parameters").asList().map {it.toString()}.containsAll(listOf("chosenColor")))

                val resultFromNextStep = session.run("CALL com.stacktoheap.thepill.next_step('$nextDecisionName', {chosenColor: 'blue'}, $nextDecisionId) yield result return result")
                    .single().get(0) as MapValue

                assertTrue(resultFromNextStep.get("value").asString() == "knowledge")

                val resultWithoutDisambiguation = session.run("CALL com.stacktoheap.thepill.next_step('$nextDecisionName', {chosenColor: 'blue'}) yield result return result")
                    .list()

                assertThat(resultWithoutDisambiguation.map{ it.get("result").get("value")}.map{it.asString()}).containsAll(listOf("knowledge", "ignorance"))
            }
        }
    }

    @Test
    fun `test decision tree traversal with facts`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill'," +
                                    "choice: 'result = {relationship: \"RED\"}; if(chosenColor === \"blue\") result = {relationship: \"BLUE\"};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:RED]->(red)" +
                            "CREATE (pill)-[:BLUE]->(blue)"

                )

                val resultFromTree = session.run("CALL com.stacktoheap.thepill.make_decision('neo', {chosenColor: \"blue\"}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(resultFromTree.get("value").asString() == "ignorance")

                val resultFromDecision = session.run("CALL com.stacktoheap.thepill.make_decision('Red Pill Or Blue Pill', {chosenColor: \"blue\"}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(resultFromDecision.get("value").asString() == "ignorance")
            }
        }
    }

    @Test
    fun `test decision tree traversal with relationship properties`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill'," +
                                    "choice: 'result = {relationship: \"COLOR\", properties: {color: \"red\"}}; if(chosenColor === \"blue\") result = {relationship: \"COLOR\" , properties: {color: \"blue\"}};' })" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:COLOR {color: 'red'}]->(red)" +
                            "CREATE (pill)-[:COLOR {color: 'blue'}]->(blue)"

                )

                val result = session.run("CALL com.stacktoheap.thepill.make_decision('neo', {chosenColor: \"blue\"}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(result.get("value").asString() == "ignorance")
            }
        }
    }

    @Test
    fun `test multi decision result`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill'," +
                                    "choice: 'result = {relationship: \"COLOR\" };'})" +
                            "CREATE (red:Leaf { value: 'knowledge' })" +
                            "CREATE (blue:Leaf { value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:COLOR {color: 'red'}]->(red)" +
                            "CREATE (pill)-[:COLOR {color: 'blue'}]->(blue)"

                )

                val result = session.run("CALL com.stacktoheap.thepill.make_decision('neo', {chosenColor: \"blue\"}) yield path return last(nodes(path)).value").list()
                assertThat(result.map { it.get(0).asString() }).containsAll(listOf("knowledge", "ignorance"))
            }
        }
    }

    @Test
    fun `test traversal with missing parameters ignored`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
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

                val result = session.run("CALL com.stacktoheap.thepill.make_decision('neo', {}, true) yield path return last(nodes(path)).value").list()
                assertThat(result.map { it.get(0).asString() }).containsAll(listOf("knowledge", "ignorance"))
            }
        }
    }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThePillProcedureLeafPropertyTest {

    private val dbServer: ServerControls =
        getDbServer().withConfig("thepill.property_based_leaves", "true").newServer()

    private fun getDbServer(): TestServerBuilder = TestServerBuilders
        .newInProcessBuilder()
        .withProcedure(Schema::class.java)
        .withProcedure(ThePillProcedure::class.java)
        .withFixture(
            "CALL com.stacktoheap.thepill.schema.generate"
        )

    @BeforeEach
    fun `set up`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run("MATCH (n) DETACH DELETE n")
            }
        }
    }

    @Test
    fun `test decision tree traversal with property based leaves`() {
        GraphDatabase.driver(dbServer.boltURI()).use { driver ->
            driver.session().use { session ->
                session.run(
                    "" +
                            "CREATE (tree:Tree { name: 'neo' })" +
                            "CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill', choice: 'result = {relationship: \"RED\"};' })" +
                            "CREATE (red:Pill { is_leaf: true, value: 'knowledge' })" +
                            "CREATE (blue:Pill { is_leaf: true, value: 'ignorance' })" +
                            "CREATE (tree)-[:HAS]->(pill)" +
                            "CREATE (pill)-[:RED]->(red)" +
                            "CREATE (pill)-[:BLUE]->(blue)"

                )

                val resultFromTree = session.run("CALL com.stacktoheap.thepill.make_decision('neo', {}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(resultFromTree.get("value").asString() == "knowledge")

                val resultFromDecision = session.run("CALL com.stacktoheap.thepill.make_decision('Red Pill Or Blue Pill', {}) yield path return last(nodes(path))")
                    .single().get(0) as NodeValue

                assertTrue(resultFromDecision.get("value").asString() == "knowledge")
            }
        }
    }
}