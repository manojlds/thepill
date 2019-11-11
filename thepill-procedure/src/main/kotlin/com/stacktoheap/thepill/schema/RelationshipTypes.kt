package com.stacktoheap.thepill.schema

import org.neo4j.graphdb.RelationshipType

sealed class RelationshipTypes(private val ordinal: Int): RelationshipType {
    fun ordinal()= ordinal;
    override fun name(): String {
        return this.javaClass.simpleName;
    }

    object HAS : RelationshipTypes(0)
}