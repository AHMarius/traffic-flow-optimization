package dal

data class QueryResult(
    val rows: List<Map<String, Any>>,
    val affectedRows: Int,
    val success: Boolean,
    val errorMessage: String,
)
