package commonutils

import java.time.LocalDateTime
import java.util.UUID

class Scenario(
    val id: UUID,
    var name: String,
    var graph: Graph,
    var timeInterval: String,
    var environmentConditions: MutableMap<String, Any>,
    val creationDate: LocalDateTime,
)
