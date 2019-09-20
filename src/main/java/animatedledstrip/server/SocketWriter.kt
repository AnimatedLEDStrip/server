package animatedledstrip.server

import org.pmw.tinylog.Configuration
import org.pmw.tinylog.LogEntry
import org.pmw.tinylog.writers.LogEntryValue
import org.pmw.tinylog.writers.PropertiesSupport
import org.pmw.tinylog.writers.Writer

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@PropertiesSupport(name = "socket", properties = [])
class SocketWriter : Writer {

    val delimiter = ":"

    override fun init(p0: Configuration?) {
    }

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