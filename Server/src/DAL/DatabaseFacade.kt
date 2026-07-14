package DAL

/**
 * @class DatabaseFacade
 * Exposes the functionalities of the DAL to other layers
 */
class DatabaseFacade(
    private val queryExecutor: IQueryExecutor
) {

    fun submit(request: QueryRequest): QueryResult {
        return queryExecutor.execute(request.sql, request.params)
    }
}