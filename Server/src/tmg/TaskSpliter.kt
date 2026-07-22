package tmg

import commonutils.Graph
import commonutils.Node

/**
 * @class TaskSpliter
 * @brief Splits a Task into micro parts (one per connected component of the subdomain) and a
 * single macro part covering the rest of the domain.
 */
class TaskSpliter {
    /**
     * @brief Partitions task. subdomain into connected components (each becoming a micro Task)
     * and derives a macro Task from the remaining domain nodes.
     */
    fun split(task: Task): SplitTask {
        val microGraph = task.subdomain
        val macroGraph = complementOf(full = task.domain, detail = microGraph)

        val microParts =
            connectedComponents(microGraph).map { componentNodes ->
                val componentGraph = microGraph.getSubgraph(componentNodes)
                Task(
                    domain = componentGraph,
                    subdomain = componentGraph,
                    scenarioCommand = task.scenarioCommand,
                    parameters = task.parameters,
                )
            }

        val macroPart =
            Task(
                domain = macroGraph,
                subdomain = macroGraph,
                scenarioCommand = task.scenarioCommand,
                parameters = task.parameters,
            )

        return SplitTask(
            microParts = microParts,
            macroPart = macroPart,
        )
    }

    /**
     * @return The subgraph of full induced by nodes not present in detail.
     */
    private fun complementOf(
        full: Graph,
        detail: Graph,
    ): Graph {
        val remainingNodes = full.nodes - detail.nodes
        return full.getSubgraph(remainingNodes)
    }

    /**
     * @brief Finds connected components of graph, treating its edges as undirected, via BFS.
     */
    private fun connectedComponents(graph: Graph): List<Set<Node>> {
        val undirected = mutableMapOf<Node, MutableSet<Node>>()
        graph.nodes.forEach { node -> undirected.getOrPut(node) { mutableSetOf() } }
        graph.nodes.forEach { node ->
            graph.getNeighbors(node).forEach { neighbor ->
                undirected.getOrPut(node) { mutableSetOf() }.add(neighbor)
                undirected.getOrPut(neighbor) { mutableSetOf() }.add(node)
            }
        }

        val visited = mutableSetOf<Node>()
        val components = mutableListOf<Set<Node>>()

        graph.nodes.forEach { start ->
            if (start !in visited) {
                val component = mutableSetOf<Node>()
                val queue = ArrayDeque<Node>()
                queue.add(start)
                visited.add(start)

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    component.add(current)
                    undirected[current]?.forEach { neighbor ->
                        if (neighbor !in visited) {
                            visited.add(neighbor)
                            queue.add(neighbor)
                        }
                    }
                }

                components.add(component)
            }
        }

        return components
    }
}
