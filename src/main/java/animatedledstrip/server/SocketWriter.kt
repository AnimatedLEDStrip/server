package animatedledstrip.server

import org.tinylog.core.LogEntry
import org.tinylog.core.LogEntryValue
import org.tinylog.writers.Writer

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class SocketWriter(properties: java.util.Map<String, String>) : Writer {

    val delimiter = properties.getOrDefault("delimiter", ":")

    override fun getRequiredLogEntryValues(): MutableSet<LogEntryValue> {
        return mutableSetOf(LogEntryValue.LEVEL, LogEntryValue.MESSAGE)
    }

    override fun write(log: LogEntry?) {
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