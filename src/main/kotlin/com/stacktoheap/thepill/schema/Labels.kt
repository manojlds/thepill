package com.stacktoheap.thepill.schema

import org.neo4j.graphdb.Label

sealed class Labels(private val ordinal: Int) : Label {
    fun ordinal()= ordinal;
    override fun name(): String {
        return this.javaClass.simpleName;
    }

    object Tree : Labels(0)
    object Leaf : Labels(1)
    object Decision : Labels(2)
}