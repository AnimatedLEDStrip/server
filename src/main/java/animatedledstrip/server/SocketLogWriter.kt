package animatedledstrip.server

import org.tinylog.Logger
import org.tinylog.core.LogEntry
import org.tinylog.core.LogEntryValue
import org.tinylog.writers.AbstractFormatPatternWriter

class SocketLogWriter(properties: Map<String, String>) : AbstractFormatPatternWriter(properties) {

    val delimiter = properties.getOrDefault("delimiter", ":")

    override fun getRequiredLogEntryValues(): MutableSet<LogEntryValue> {
        return mutableSetOf(LogEntryValue.LEVEL, LogEntryValue.MESSAGE)
    }

    override fun write(log: LogEntry?) {
        Logger.debug { log?.message }
        SocketConnections.localConnection?.sendString(
                "${log?.level.toString()}$delimiter".padEnd(8, ' ') +
                        "${log?.message}"
            )
    }

    override fun flush() {
    }

    override fun close() {
    }

}