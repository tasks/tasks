package org.tasks.logging

import okio.Path.Companion.toOkioPath
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun FileLogWriter(logDirectory: File): FileLogWriter = FileLogWriter(logDirectory.toOkioPath())

suspend fun FileLogWriter.zipLogFiles(
    zipFile: File,
    extras: suspend (ZipOutputStream) -> Unit = {},
): File {
    FileOutputStream(zipFile).use { fos ->
        ZipOutputStream(fos).use { zos ->
            extras(zos)
            flush()
            logFiles().forEach { path ->
                zos.putNextEntry(ZipEntry(path.name))
                path.toFile().inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
    return zipFile
}
