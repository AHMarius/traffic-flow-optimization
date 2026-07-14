package DAL

interface IQueryExecutor {
    fun execute(sql: String, params: List<Any>):QueryResult
}