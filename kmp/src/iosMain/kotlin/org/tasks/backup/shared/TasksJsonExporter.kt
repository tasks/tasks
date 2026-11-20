@file:Suppress("LongParameterList")

package org.tasks.backup.shared

import kotlinx.io.core.Writer as IosWriter
import kotlinx.io.bufferedWriter
import kotlinx.io.core.use
import platform.posix.fopen
import platform.posix.fclose

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

actual fun createWriter(filePath: String): Writer {
    val file = fopen(filePath, "w") ?: throw java.io.IOException("Cannot open file: $filePath")
    val writer = file.bufferedWriter()
    return IosWriterAdapter(writer)
}