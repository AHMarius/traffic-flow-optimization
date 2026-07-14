package DAL


/**
 * @class DatabaseExecutor
 * Executes sql queries
 */
class DatabaseExecutor(
    private val connection: IConnection
) : IQueryExecutor {

    override fun execute(sql: String, params: List<Any>): QueryResult {
        connection.open()

        return try {
            connection.execute(sql, params)
        } finally {
            connection.close()
        }
    }
}