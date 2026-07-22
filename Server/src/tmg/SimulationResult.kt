package tmg

import java.util.UUID

data class SimulationResult(
    val taskId: UUID = UUID.randomUUID(),
    val data: MutableMap<String, Any>,
    val success: Boolean,
)
