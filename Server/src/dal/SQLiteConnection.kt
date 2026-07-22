package dal

import java.sql.Connection
import java.sql.DriverManager

class SQLiteConnection(
    private var connectionString: String,
) : IConnection {
    private var connection: Connection? = null

    override fun open() {
        if (connection == null || connection!!.isClosed) {
            connection = DriverManager.getConnection(connectionString)
        }
    }

    override fun close() {
        connection?.close()
        connection = null
    }

    override fun execute(
        sql: String,
        params: List<Any>,
    ): QueryResult =
        try {
            open()

            val statement = connection!!.prepareStatement(sql)
            params.forEachIndexed { index, value -> statement.setObject(index + 1, value) }

            if (statement.execute()) {
                val rs = statement.resultSet
                val rows = mutableListOf<Map<String, Any>>()

                while (rs.next()) {
                    val row = mutableMapOf<String, Any>()
                    val meta = rs.metaData

                    for (i in 1..meta.columnCount) {
                        row[meta.getColumnName(i)] = rs.getObject(i) ?: ""
                    }

                    rows.add(row)
                }
                QueryResult(rows = rows, affectedRows = rows.size, success = true, errorMessage = "")
            } else {
                QueryResult(rows = emptyList(), affectedRows = statement.updateCount, success = true, errorMessage = "")
            }
        } catch (e: Exception) {
            QueryResult(
                rows = emptyList(),
                affectedRows = 0,
                success = false,
                errorMessage = e.message ?: "Unknown error",
            )
        }
}
