package com

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @class CliChannel
 * @brief Communication channel that interacts with the user through standard input/output.
 */
class CliChannel(
    dispatcher: CommandDispatcher,
) : ICommunicationChannel(dispatcher) {
    private var reader: BufferedReader? = null

    override fun establishConnection() {
        reader = BufferedReader(InputStreamReader(System.`in`))
    }

    /**
     * @brief Prints the execution status to stdout.
     * @param status The execution status to print.
     * @throws IllegalStateException if called before establishConnection().
     */
    override fun respond(status: ExecutionStatus) {
        if (reader == null) {
            throw IllegalStateException("No active connection. Call establishConnection() first.")
        }
        println(status.name)
    }

    /**
     * @return The next line read from stdin.
     * @throws IllegalStateException if called before establishConnection().
     * @throws ChannelClosedException if the input stream has reached EOF.
     */
    override fun readIncomingLine(): String {
        val r =
            reader
                ?: throw IllegalStateException("No active connection. Call establishConnection() first.")
        return r.readLine() ?: throw ChannelClosedException()
    }

    override fun close() {
        reader?.close()
        reader = null
    }
}
