package tmg

import commonutils.Edge
import commonutils.Node
import java.util.UUID

/**
 * @class Agent
 * @brief A vehicle moving along a route of edges, tracked by its progress on the current edge.
 */
class Agent(
    val id: UUID = UUID.randomUUID(),
    var position: Double,
    var speed: Double,
    var route: List<Edge>,
) {
    val currentEdge: Edge
        get() = route.first()

    val currentNode: Node
        get() = currentEdge.sourceNode

    /**
     * @brief Advances the agent along its route by speed * deltaT, crossing into subsequent
     * edges (and picking up each edge's average speed) as needed until the distance is spent
     * or the route ends.
     */
    fun updatePosition(deltaT: Double) {
        var remainingDistance = speed * deltaT

        while (remainingDistance > 0.0 && !hasReachedDestination()) {
            val distanceToEnd = currentEdge.distance - position

            if (remainingDistance < distanceToEnd) {
                position += remainingDistance
                remainingDistance = 0.0
            } else {
                remainingDistance -= distanceToEnd
                route = route.drop(1)
                position = 0.0

                if (!hasReachedDestination()) {
                    speed = currentEdge.averageSpeed
                }
            }
        }
    }

    fun hasReachedDestination(): Boolean = route.isEmpty()
}
