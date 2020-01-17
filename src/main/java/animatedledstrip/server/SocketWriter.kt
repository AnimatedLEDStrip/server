/*
 *  Copyright (c) 2018-2020 AnimatedLEDStrip
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package animatedledstrip.server

import org.pmw.tinylog.Configuration
import org.pmw.tinylog.LogEntry
import org.pmw.tinylog.writers.LogEntryValue
import org.pmw.tinylog.writers.PropertiesSupport
import org.pmw.tinylog.writers.Writer

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@PropertiesSupport(name = "socket", properties = [])
class SocketWriter : Writer {

    private val delimiter = ":"

    override fun init(p0: Configuration?) {
    }

    override fun getRequiredLogEntryValues(): MutableSet<LogEntryValue> {
        return mutableSetOf(LogEntryValue.LEVEL, LogEntryValue.MESSAGE)
    }

    override fun write(log: LogEntry) {
        SocketConnections.sendLog(
            "${log.level}$delimiter".padEnd(8, ' ') +
                    log.message
        )
    }

    override fun flush() {
    }

    override fun close() {
    }

}