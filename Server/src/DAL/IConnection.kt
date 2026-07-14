package DAL

import java.util.Objects

interface IConnection {

    fun open()
    fun close()
    fun execute(sql: String, params: List<Any>):QueryResult
}