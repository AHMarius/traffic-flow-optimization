package DAL

class QueryResult {
    private val rows: List<Map<String,Any>>
    private val affectedRows: Int
    private var success: Boolean
    private var errorMessage: String
    constructor(rows: List<Map<String,Any>>, affectedRows: Int, success: Boolean, errorMessage: String) {
        this.rows = rows
        this.affectedRows = affectedRows
        this.success = success
        this.errorMessage = errorMessage
    }
}