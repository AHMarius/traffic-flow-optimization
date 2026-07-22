package tmg

import commonutils.Edge
import java.util.UUID

/**
 * @class Cohort
 * @brief Macroscopic representation of vehicles on an edge (count, density, average speed),
 * convertible to/from individual Agent instances.
 */
class Cohort(
    val id: UUID = UUID.randomUUID(),
    var vehicleCount: Int,
    var density: Double,
    var averageSpeed: Double,
    val associatedEdge: Edge,
) {
    /**
     * @brief Expands this cohort into vehicleCount agents, evenly spaced along associatedEdge.
     */
    fun transformIntoAgents(): List<Agent> {
        val spacing = if (vehicleCount > 0) associatedEdge.distance / vehicleCount else 0.0

        return List(vehicleCount) { index ->
            Agent(
                position = spacing * index,
                speed = averageSpeed,
                route = listOf(associatedEdge),
            )
        }
    }

    /**
     * @brief Recomputes vehicleCount, averageSpeed, and density from the given agents.
     */
    fun aggregateFromAgents(agents: List<Agent>) {
        vehicleCount = agents.size
        averageSpeed = if (agents.isNotEmpty()) agents.sumOf { it.speed } / agents.size else 0.0
        density = if (associatedEdge.distance > 0.0) vehicleCount / associatedEdge.distance else 0.0
    }
}
