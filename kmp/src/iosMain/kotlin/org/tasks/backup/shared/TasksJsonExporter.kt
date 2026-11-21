@file:Suppress("LongParameterList")
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.tasks.backup.shared

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.FILE
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.malloc
import platform.posix.free

actual interface Writer {
    actual fun write(text: String)
    actual fun flush()
}

class IosWriterAdapter(
    private val file: CPointer<FILE>,
) : Writer {
    override fun write(text: String) {
        memScoped {
            val cString = text.cstr()
            fwrite(cString.ptr, 1, text.length, file)
        }
    }

    override fun flush() {
        fflush(file)
    }
}

actual fun createWriter(filePath: String): Writer {
    val cPath = filePath.cstr()
    val file = fopen(cPath, "w".cstr()) ?: throw java.io.IOException("Cannot open file: $filePath")
    return IosWriterAdapter(file)
}