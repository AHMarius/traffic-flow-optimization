package tmg

import java.util.UUID

data class AgentPosition(
    var agentId: UUID,
    var edgeId: UUID,
    var position: Double,
    var speed: Double,
)
