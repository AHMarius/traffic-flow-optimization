package commonutils

/**
 * @class Graph
 * @brief Directed graph of nodes connected by edges, backed by an adjacency list.
 */
class Graph(
    val nodes: MutableSet<Node> = mutableSetOf(),
    private val adjacency: MutableMap<Node, MutableList<Edge>> = mutableMapOf(),
) {
    fun addNode(node: Node) {
        nodes.add(node)
        adjacency.putIfAbsent(node, mutableListOf())
    }

    /**
     * @brief Adds an edge, adding its source/target nodes first if not already present.
     */
    fun addEdge(edge: Edge) {
        addNode(edge.sourceNode)
        addNode(edge.targetNode)

        adjacency[edge.sourceNode]!!.add(edge)
    }

    /**
     * @return The nodes reachable from node via a single outgoing edge, or empty if node is absent.
     */
    fun getNeighbors(node: Node): List<Node> = adjacency[node]?.map { it.targetNode } ?: emptyList()

    /**
     * @brief Builds a new Graph containing only nodesOfInterest and the edges between them.
     * @param nodesOfInterest The nodes to retain.
     * @return A subgraph induced by nodesOfInterest.
     */
    fun getSubgraph(nodesOfInterest: Collection<Node>): Graph {
        val subgraph = Graph()

        nodesOfInterest.forEach { subgraph.addNode(it) }

        nodesOfInterest.forEach { node ->
            adjacency[node]?.forEach { edge ->
                if (edge.targetNode in nodesOfInterest) {
                    subgraph.addEdge(edge)
                }
            }
        }

        return subgraph
    }
}
