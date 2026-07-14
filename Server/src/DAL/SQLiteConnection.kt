package DAL

import java.sql.Connection
import java.sql.DriverManager

/**
 * @class SQLiteConnection
 * Provides a way to execute querries on SQLite
 */
class SQLiteConnection( private var connectionString:String): IConnection {

    private var connection: Connection? = null

    override fun open() {
        if(connection == null || connection!!.isClosed) {
            connection = DriverManager.getConnection(connectionString)
        }
    }

    override fun close()
    {
        connection?.close()
        connection = null
    }

    /**
     * Executes the specified SQL statement using the provided parameters.
     *
     * @param sql The SQL statement to execute.
     * @param params The parameters bound to the statement.
     * @return A {@code QueryResult} containing the execution result, including
     * retrieved rows or affected row count, execution status, and any error message.
     */
   override  fun execute(sql: String, params: List<Any>):QueryResult {
       return try {
           open()

           val statement = connection!!.prepareStatement(sql)
           params.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
           return if (statement.execute()) {

               val rs = statement.resultSet
               val rows = mutableListOf<Map<String, Object>>()

               while (rs.next()) {

                   val row = mutableMapOf<String, Object>()
                   val meta = rs.metaData

                   for (i in 1..meta.columnCount) {
                       row[meta.getColumnName(i)] = rs.getObject(i) as Object
                   }

                   rows.add(row)
               }
               QueryResult(rows = rows, affectedRows = rows.size, success = true, errorMessage = "")
           } else {
               QueryResult(rows = emptyList(), affectedRows = statement.updateCount, success = true, errorMessage = "")
           }
           }catch (e:Exception) {
               QueryResult(
                   rows = emptyList(),
                   affectedRows = 0,
                   success = false,
                   errorMessage = e.message ?: "Unknown error"
               )
           }
   }
}