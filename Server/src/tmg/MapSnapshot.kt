package tmg

import java.util.UUID

data class MapSnapshot(
    var taskId: UUID,
    var stepIndex: Int,
    var timestamp: Double,
    var agentPoisitions: List<AgentPosition>,
    var cohortStates: List<CohortState>,
)
