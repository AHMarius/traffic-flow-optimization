package tmg

import java.util.UUID

data class MapSnapshotRow(
    val taskId: UUID,
    val stepIndex: Int,
    val timestamp: Double,
    val agentPositionsJson: String,
    val cohortStatesJson: String,
)
