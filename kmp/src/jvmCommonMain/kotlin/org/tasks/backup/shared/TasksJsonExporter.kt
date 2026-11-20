@file:Suppress("LongParameterList")

package org.tasks.backup.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Writer as JvmWriter

actual interface Writer {
    actual fun write(text: String)
    actual fun flush()
}

class JvmWriterAdapter(
    private val writer: JvmWriter,
) : Writer {
    override fun write(text: String) = writer.write(text)

    override fun flush() = writer.flush()
}

actual fun createWriter(filePath: String): Writer {
    val file = java.io.File(filePath)
    file.parentFile?.mkdirs()
    return JvmWriterAdapter(java.io.FileWriter(file))
}