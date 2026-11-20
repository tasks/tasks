@file:Suppress("LongParameterList")

package org.tasks.backup.shared

import kotlinx.io.Writer as IosWriter

actual interface Writer {
    actual fun write(text: String)
    actual fun flush()
}

class IosWriterAdapter(
    private val writer: IosWriter,
) : Writer {
    override fun write(text: String) = writer.write(text)

    override fun flush() = writer.flush()
}

actual fun createWriter(javaWriter: java.io.Writer): Writer = IosWriterAdapter(javaWriter as IosWriter)