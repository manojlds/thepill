package com.stacktoheap.thepill.models


private object SettingsKeys {
    const val PROPERTY_BASED_LEAVES = "thepill.property_based_leaves"
    const val LEAVES_PROPERTY = "thepill.leaves_property"
}

data class Settings(val propertyBasedLeaves: Boolean, val leavesProperty: String?) {
    companion object {
        fun from(config: Map<String, String>): Settings {
            Settings(false, "")
            val propertyBasedLeaves =
                config.getOrDefault(SettingsKeys.PROPERTY_BASED_LEAVES, false).toString().toBoolean()
            val leavesProperty =
                config.getOrDefault(SettingsKeys.LEAVES_PROPERTY, "is_leaf")
            return Settings(propertyBasedLeaves = propertyBasedLeaves, leavesProperty = leavesProperty)
        }
    }
}