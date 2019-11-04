# thepill
Decision Trees with Neo4J

![](https://github.com/manojlds/thepill/workflows/Build/badge.svg)

<p align="center">
<img src="docs/images/decision_tree.png" width="600px"/>
</p>

## Features

Model decision trees using Neo4j and Javascript based decision evaluation.

## Example

```
CREATE (tree:Tree { name: 'neo' })
CREATE (red:Leaf { value: 'knowledge' })
CREATE (blue:Leaf { value: 'ignorance' })
CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill', 
  choice: 'result = {relationship: \"RED\"}; if(chosenColor === \"blue\") result = {relationship: \"BLUE\"};' })
CREATE (tree)-[:HAS]->(pill)
CREATE (pill)-[:RED]->(red)
CREATE (pill)-[:BLUE]->(blue)
```
Traverse the decision tree and make a decision:

```
CALL com.stacktoheap.thepill.make_decision('neo', {chosenColor: "blue"}) yield path return path
```


## Example explained

Create the root of the tree:

```
CREATE (tree:Tree { name: 'neo' })
```

Create the final decision for the tree (the leaves):

```
CREATE (red:Leaf { value: 'knowledge' })
CREATE (blue:Leaf { value: 'ignorance' })
```

Create the decision node with result evaluation using JS code:

```
CREATE (pill: Decision { name: 'Red Pill Or Blue Pill', question: 'Red Pill Or Blue Pill', 
  choice: 'result = {relationship: \"RED\"}; if(chosenColor === \"blue\") result = {relationship: \"BLUE\"};' })
```

`result` is the variable that is used to make the decision. It is to be set to an object with two properties:

- `relationship` - The relationship type for the chosen decision
- `properties` - Map of properties that choose the next node (useful if there are multiple relationships of the same type)

The variables available in the choice script are facts supplied to the tree at evaluation time (see below)

Create the relationships:

```
CREATE (tree)-[:HAS]->(pill)
CREATE (pill)-[:RED]->(red)
CREATE (pill)-[:BLUE]->(blue)
```

Traverse the tree and make decision:

```
CALL com.stacktoheap.thepill.make_decision('neo', {chosenColor: "blue"}) yield path return path
```

<p align="center">
<img src="docs/images/traversal_example.png" width="600px"/>
</p>

 

## Installation

1. Download the latest jar from https://github.com/manojlds/thepill/releases

2. Copy the jar to the plugins folder of Neo4J instance - `<neo4j-home>/plugins`

3. (Re)Start the server

4. Apply the schema for thepill:

```cypher
CALL com.stacktoheap.thepill.schema.generate
```

## Build

Compile and run tests with `./gradlew build`

Create fat jar with `./gradlew shadowJar` 

## Note

Based on - https://github.com/maxdemarzi/decision_trees_with_rules

Differs in the following ways:

1. Uses Javascript for script evaluation rather than Java.
2. Ability to choose relationships based on properties.
3. No insecure APIs usage.
4. Updated for latest Neo4j and written in Kotlin.
