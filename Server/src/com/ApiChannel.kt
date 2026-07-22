package com

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * @class ApiChannel
 * @brief TCP socket based implementation of ICommunicationChannel; listens on a port, accepts a single client connection, and exchanges line-based text commands/status responses with it.
 */
class ApiChannel(
    dispatcher: CommandDispatcher,
    private val port: Int = 8080,
) : ICommunicationChannel(dispatcher) {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    /**
     * @brief Opens a ServerSocket on the configured port and blocks until a client connects, then initializes the reader/writer streams for that connection.
     */
    override fun establishConnection() {
        serverSocket = ServerSocket(port)
        val socket = serverSocket!!.accept()
        clientSocket = socket
        reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        writer = PrintWriter(socket.getOutputStream(), true)
    }

    /**
     * @brief Sends the given status name as a line to the connected client.
     * @param status The execution status to send.
     * @throws IllegalStateException if called before establishConnection().
     */
    override fun respond(status: ExecutionStatus) {
        val w =
            writer
                ?: throw IllegalStateException("No active connection. Call establishConnection() first.")
        w.println(status.name)
    }

    /**
     * @return The next line read from the client.
     * @throws IllegalStateException if called before establishConnection().
     * @throws ChannelClosedException if the client closes the connection.
     */
    override fun readIncomingLine(): String {
        val r =
            reader
                ?: throw IllegalStateException("No active connection. Call establishConnection() first.")
        return r.readLine() ?: throw ChannelClosedException()
    }

    override fun close() {
        reader?.close()
        writer?.close()
        clientSocket?.close()
        serverSocket?.close()
    }
}
