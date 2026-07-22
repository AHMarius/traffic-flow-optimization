package com

abstract class ICommunicationChannel(
    protected val dispatcher: CommandDispatcher,
) {
    fun parseRequest(input: String): Command = PacketParser.parse(input)

    abstract fun establishConnection()

    abstract fun readIncomingLine(): String

    abstract fun respond(status: ExecutionStatus)

    abstract fun close()

    /**
     * @brief Runs the channel's main loop: connects, then repeatedly reads a line, dispatches it
     * as a command, and responds with the resulting status, until the channel closes.
     * Blank lines are skipped; any dispatch failure is reported as ExecutionStatus.FAILED
     * rather than propagated. Always closes the channel on exit, even if a handler throws.
     */
    fun start() {
        establishConnection()
        try {
            while (true) {
                val line =
                    try {
                        readIncomingLine()
                    } catch (e: ChannelClosedException) {
                        break
                    }
                if (line.isBlank()) continue

                val status =
                    try {
                        dispatcher.dispatch(parseRequest(line))
                        ExecutionStatus.SUCCESS
                    } catch (e: PacketSyntaxException) {
                        ExecutionStatus.FAILED
                    } catch (e: Throwable) {
                        ExecutionStatus.FAILED
                    }
                respond(status)
            }
        } finally {
            close()
        }
    }
}

class ChannelClosedException : Exception()
