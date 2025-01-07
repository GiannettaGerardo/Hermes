# Hermes
[![en](https://img.shields.io/badge/lang-en-red.svg)](https://github.com/GiannettaGerardo/Hermes/README.md)
[![it](https://img.shields.io/badge/lang-it-green.svg)](https://github.com/GiannettaGerardo/Hermes/README.it.md)

Hermes is a fast and easy to use concurrent process/workflow engine. It does not support any specific process creation standards and performs very few checks on the graph format, making Hermes very fast. The library instantiates the graph in memory and expects it to remain there for the duration of the process. Hermes can be used very well for frequently instantiated processes but shows its best on long-running processes in memory.

Hermes is thread-safe and concurrent, does not adopt blocking mutexes in any write or read operation while allowing only one thread to write at a time.

## Graph nodes

Hermes provides 4 types of nodes to create its graphs:

- **TASK**: the only type of nodes that can be read from the graph and on which the workflow stops waiting for a completion operation. It can be completed by inserting variables, the number of which must be made explicit;
- **FORWARD**: node on which the workflow never stops but always moves on, with all the advantages of conditional arcs and forks;
- **JOIN**: node used specifically to join multiple edges resulting from forks. Requires explicit entry of the *minimum* number of edges to join;
- **ENDING**: node used to establish the official end of the workflow. After the workflow has reached an ending node, no further operations on the graph will be possible and any fork branches left pending will no longer be able to be terminated.

## Graph arches

Hermes provides 2 types of arcs:

- **Normal**: contains the ID of the unique destination node;
- **Conditional**: contains an **IF** - **ELSE IF** - **ELSE** condition in the form of a list and using the [**Json Logic**](https://jsonlogic.com/) format for evaluation. Each element of the list must contain the ID of a target node.