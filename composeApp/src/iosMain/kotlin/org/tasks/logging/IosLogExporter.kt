package org.tasks.logging

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.tasks.extensions.keyWindow
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSFileCoordinatorReadingForUploading
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

private const val TAG = "IosLogExporter"

@OptIn(ExperimentalForeignApi::class)
class IosLogExporter(
    private val fileLogWriter: FileLogWriter,
) : LogExporter {
    override suspend fun export() {
        val archive = withContext(Dispatchers.IO) { archive() } ?: return
        withContext(Dispatchers.Main) { share(archive) }
    }

    private suspend fun archive(): NSURL? {
        fileLogWriter.flush()
        val staging = (NSTemporaryDirectory() + "logs").toPath()
        with(FileSystem.SYSTEM) {
            deleteRecursively(staging, mustExist = false)
            createDirectories(staging)
            fileLogWriter.logFiles().forEach { copy(it, staging / it.name) }
            write(staging / "device.txt") { writeUtf8(debugInfo()) }
        }
        val archive = NSURL.fileURLWithPath(NSTemporaryDirectory() + "tasks-logs.zip")
        val fileManager = NSFileManager.defaultManager
        fileManager.removeItemAtURL(archive, null)
        NSFileCoordinator(filePresenter = null).coordinateReadingItemAtURL(
            NSURL.fileURLWithPath(staging.toString(), isDirectory = true),
            NSFileCoordinatorReadingForUploading,
            null,
        ) { snapshot ->
            snapshot?.let { fileManager.moveItemAtURL(it, archive, null) }
        }
        return if (fileManager.fileExistsAtPath(archive.path!!)) {
            archive
        } else {
            Logger.e(TAG) { "Failed to archive logs" }
            null
        }
    }

    private fun share(archive: NSURL) {
        var presenter: UIViewController = keyWindow().rootViewController ?: return
        while (true) {
            presenter = presenter.presentedViewController ?: break
        }
        val controller = UIActivityViewController(
            activityItems = listOf(archive, debugInfo()),
            applicationActivities = null,
        )
        controller.popoverPresentationController?.apply {
            sourceView = presenter.view
            sourceRect = presenter.view.bounds.useContents {
                CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
            }
        }
        presenter.presentViewController(controller, animated = true, completion = null)
    }
}
