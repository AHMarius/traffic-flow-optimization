package tmg

import java.util.UUID

data class CohortState(
    val cohortId: UUID,
    val edgeId: UUID,
    val vehicleCount: UInt,
    val density: Double,
    val averageSpeed: Double,
)
