package dal

interface IConnection {
    fun open()

    fun close()

    fun execute(
        sql: String,
        params: List<Any>,
    ): QueryResult
}
