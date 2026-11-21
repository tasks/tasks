@file:Suppress("LongParameterList")

package org.tasks.backup.shared

import kotlinx.io.core.Writer as IosWriter
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSString

actual interface Writer {
    actual fun write(text: String)
    actual fun flush()
}

class IosWriterAdapter(
    private val fileHandle: NSFileHandle,
) : Writer {
    override fun write(text: String) {
        val data = text.encodeToByteArray().toNSData()
        fileHandle.writeData(data)
    }

    override fun flush() {
        fileHandle.synchronizeFile()
    }
}

actual fun createWriter(filePath: String): Writer {
    val fileManager = NSFileManager.defaultManager
    val url = NSURL.fileURLWithPath(filePath)
    fileManager.createFileAtPath(filePath, contents = null, attributes = null)
    val fileHandle = NSFileHandle.fileHandleForWritingToURL(url, error = null)
    return IosWriterAdapter(fileHandle!!)
}