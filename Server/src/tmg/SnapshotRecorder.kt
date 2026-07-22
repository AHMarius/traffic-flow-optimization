package tmg

import dal.DatabaseFacade
import dal.QueryRequest
import java.util.UUID

/**
 * @class SnapshotRecorder
 * @brief ISimulationObserver that persists each simulation step's map snapshot to the database.
 */
class SnapshotRecorder(
    var database: DatabaseFacade,
) : ISimulationObserver {
    /**
     * @brief Serializes snapshot into a MapSnapshotRow and inserts it into the snapshots table.
     * @throws IllegalStateException if the database insert fails.
     */
    override fun onStep(
        stepIndex: Int,
        timestamp: Double,
        snapshot: MapSnapshot,
    ) {
        val row = toRow(snapshot.taskId, stepIndex, timestamp, snapshot)

        val request =
            QueryRequest(
                id = UUID.randomUUID(),
                sql =
                    "INSERT INTO snapshots (task_id, step_index, timestamp, agent_positions, cohort_states) " +
                        "VALUES (?, ?, ?, ?, ?)",
                params =
                    listOf(
                        row.taskId.toString(),
                        row.stepIndex,
                        row.timestamp,
                        row.agentPositionsJson,
                        row.cohortStatesJson,
                    ),
            )

        val result = database.submit(request)
        check(result.success) { "Failed to persist snapshot: ${result.errorMessage}" }
    }

    protected fun toRow(
        taskId: UUID,
        stepIndex: Int,
        timestamp: Double,
        snapshot: MapSnapshot,
    ): MapSnapshotRow =
        MapSnapshotRow(
            taskId = taskId,
            stepIndex = stepIndex,
            timestamp = timestamp,
            agentPositionsJson = serializeAgentPositions(snapshot.agentPoisitions),
            cohortStatesJson = serializeCohortStates(snapshot.cohortStates),
        )

    private fun serializeAgentPositions(positions: List<AgentPosition>): String =
        positions.joinToString(prefix = "[", postfix = "]") { p ->
            "{\"agentId\":\"${p.agentId}\",\"edgeId\":\"${p.edgeId}\"," +
                "\"position\":${p.position},\"speed\":${p.speed}}"
        }

    private fun serializeCohortStates(states: List<CohortState>): String =
        states.joinToString(prefix = "[", postfix = "]") { c ->
            "{\"cohortId\":\"${c.cohortId}\",\"edgeId\":\"${c.edgeId}\"," +
                "\"vehicleCount\":${c.vehicleCount},\"density\":${c.density}," +
                "\"averageSpeed\":${c.averageSpeed}}"
        }
}
