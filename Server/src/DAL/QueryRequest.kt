package DAL

import java.util.UUID

class QueryRequest(
    val id: UUID,
    val sql: String,
    val params: List<Any>
)