package tmg

import java.util.Objects
import java.util.UUID

data class PartialResult(
    val taskId: UUID = UUID.randomUUID(),
    val data: Map<String, Object>,
)
