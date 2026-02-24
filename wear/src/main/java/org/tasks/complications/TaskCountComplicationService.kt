package org.tasks.complications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.datalayer.grpc.GrpcExtensions.grpcClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.tasks.GrpcProto.GetTaskCountRequest
import org.tasks.GrpcProto.GetTasksRequest
import org.tasks.GrpcProto.ListItemType
import org.tasks.GrpcProto.UiItem
import org.tasks.R
import org.tasks.WearServiceGrpcKt
import org.jetbrains.compose.resources.getString
import org.tasks.extensions.wearDataLayerRegistry
import org.tasks.presentation.MainActivity
import org.tasks.presentation.phoneTargetNodeId
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.all_done
import timber.log.Timber

const val EXTRA_COMPLICATION_FILTER = "extra_complication_filter"
const val EXTRA_ADD_TASK = "extra_add_task"

data class ComplicationState(
    val count: Int = 0,
    val completedCount: Int = 0,
    val nextTask: UiItem? = null,
)

@OptIn(ExperimentalHorologistApi::class)
abstract class BaseComplicationService : SuspendingComplicationDataSourceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    protected val wearService: WearServiceGrpcKt.WearServiceCoroutineStub by lazy {
        val registry = applicationContext.wearDataLayerRegistry(scope)
        val targetNodeId = applicationContext.phoneTargetNodeId()
        registry.grpcClient(
            nodeId = targetNodeId,
            coroutineScope = scope,
        ) {
            WearServiceGrpcKt.WearServiceCoroutineStub(it)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        applicationContext.clearComplicationFilter(complicationInstanceId)
        super.onComplicationDeactivated(complicationInstanceId)
    }

    protected suspend fun fetchState(complicationId: Int, needsNextTask: Boolean): ComplicationState {
        val filter = applicationContext.getComplicationFilter(complicationId)
        val showHidden = applicationContext.getComplicationShowHidden(complicationId)
        val countRequest = GetTaskCountRequest.newBuilder()
            .setShowHidden(showHidden)
            .setShowCompleted(false)
            .apply { if (filter != null) setFilter(filter) }
            .build()
        val countResponse = wearService.getTaskCount(countRequest)

        val nextTask = if (needsNextTask) {
            val sortMode = applicationContext.getComplicationSortMode(complicationId)
            val tasksRequest = GetTasksRequest.newBuilder()
                .setPosition(0)
                .setLimit(5)
                .setSortMode(sortMode)
                .setGroupMode(com.todoroo.astrid.core.SortHelper.GROUP_NONE)
                .setShowHidden(showHidden)
                .setShowCompleted(false)
                .apply { if (filter != null) setFilter(filter) }
                .build()
            val tasks = wearService.getTasks(tasksRequest)
            tasks.itemsList.firstOrNull { it.type == ListItemType.Item }
        } else {
            null
        }

        return ComplicationState(
            count = countResponse.count.toInt(),
            completedCount = countResponse.completedCount.toInt(),
            nextTask = nextTask,
        )
    }

    protected fun monoImage() = MonochromaticImage.Builder(
        Icon.createWithResource(this, R.drawable.ic_complication)
    ).build()

    protected fun tapIntent(complicationId: Int, filter: String?) = PendingIntent.getActivity(
        this,
        complicationId,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (filter != null) {
                putExtra(EXTRA_COMPLICATION_FILTER, filter)
            }
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

class TaskCountComplicationService : BaseComplicationService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val filter = applicationContext.getComplicationFilter(request.complicationInstanceId)
        val tapAction = tapIntent(request.complicationInstanceId, filter)
        val state = try {
            fetchState(request.complicationInstanceId, needsNextTask = false)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
        val label = state?.count?.toString() ?: "--"
        val text = PlainComplicationText.Builder(label).build()
        val contentDescription = PlainComplicationText.Builder("$label tasks").build()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(text, contentDescription)
                    .setMonochromaticImage(monoImage())
                    .setTapAction(tapAction)
                    .build()

            ComplicationType.MONOCHROMATIC_IMAGE ->
                MonochromaticImageComplicationData.Builder(monoImage(), contentDescription)
                    .setTapAction(tapAction)
                    .build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val text = PlainComplicationText.Builder("5").build()
        val contentDescription = PlainComplicationText.Builder("5 tasks").build()
        val monoImage = monoImage()

        return when (type) {
            ComplicationType.MONOCHROMATIC_IMAGE ->
                MonochromaticImageComplicationData.Builder(monoImage, contentDescription)
                    .build()

            else ->
                ShortTextComplicationData.Builder(text, contentDescription)
                    .setMonochromaticImage(monoImage)
                    .build()
        }
    }
}

class NextTaskComplicationService : BaseComplicationService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val filter = applicationContext.getComplicationFilter(request.complicationInstanceId)
        val tapAction = tapIntent(request.complicationInstanceId, filter)
        val contentDescription = PlainComplicationText.Builder("Tasks").build()
        val state = try {
            fetchState(request.complicationInstanceId, needsNextTask = true)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

        return when (request.complicationType) {
            ComplicationType.LONG_TEXT -> {
                val nextTask = state?.nextTask
                if (nextTask != null && nextTask.title.isNotBlank()) {
                    val taskText = PlainComplicationText.Builder(nextTask.title).build()
                    val titleText = if (nextTask.hasTimestamp()) {
                        PlainComplicationText.Builder(nextTask.timestamp).build()
                    } else {
                        PlainComplicationText.Builder("${state.count} tasks").build()
                    }
                    LongTextComplicationData.Builder(taskText, contentDescription)
                        .setTitle(titleText)
                        .setMonochromaticImage(monoImage())
                        .setTapAction(tapAction)
                        .build()
                } else if (state != null) {
                    LongTextComplicationData.Builder(
                        PlainComplicationText.Builder(getString(Res.string.all_done)).build(),
                        contentDescription,
                    )
                        .setMonochromaticImage(monoImage())
                        .setTapAction(tapAction)
                        .build()
                } else {
                    LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("--").build(),
                        contentDescription,
                    )
                        .setMonochromaticImage(monoImage())
                        .setTapAction(tapAction)
                        .build()
                }
            }

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val contentDescription = PlainComplicationText.Builder("5 tasks").build()
        return LongTextComplicationData.Builder(
            PlainComplicationText.Builder("Buy groceries").build(),
            contentDescription,
        )
            .setTitle(PlainComplicationText.Builder("Tomorrow").build())
            .setMonochromaticImage(monoImage())
            .build()
    }
}

class TaskProgressComplicationService : BaseComplicationService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val filter = applicationContext.getComplicationFilter(request.complicationInstanceId)
        val tapAction = tapIntent(request.complicationInstanceId, filter)
        val response = try {
            val showHidden = applicationContext.getComplicationShowHidden(request.complicationInstanceId)
            wearService.getTaskCount(
                GetTaskCountRequest.newBuilder()
                    .setShowHidden(showHidden)
                    .setShowCompleted(true)
                    .apply { if (filter != null) setFilter(filter) }
                    .build()
            )
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
        val incompleteCount = response?.count ?: 0
        val completedCount = response?.completedCount ?: 0
        val label = if (response != null) "$incompleteCount" else "--"
        val text = PlainComplicationText.Builder(label).build()
        val contentDescription = PlainComplicationText.Builder("$label tasks").build()
        val total = (completedCount + incompleteCount).toFloat().coerceAtLeast(1f)

        return when (request.complicationType) {
            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(
                    value = completedCount.toFloat(),
                    min = 0f,
                    max = total,
                    contentDescription = contentDescription,
                )
                    .setText(text)
                    .setMonochromaticImage(monoImage())
                    .setTapAction(tapAction)
                    .build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val contentDescription = PlainComplicationText.Builder("5 tasks").build()
        return RangedValueComplicationData.Builder(
            value = 3f,
            min = 0f,
            max = 8f,
            contentDescription = contentDescription,
        )
            .setText(PlainComplicationText.Builder("5").build())
            .setMonochromaticImage(monoImage())
            .build()
    }
}

class AddTaskComplicationService : SuspendingComplicationDataSourceService() {

    private fun smallImage() = SmallImage.Builder(
        Icon.createWithResource(this, R.drawable.ic_add_task),
        SmallImageType.ICON,
    ).build()

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val contentDescription = PlainComplicationText.Builder("Add task").build()
        val tapIntent = PendingIntent.getActivity(
            this,
            request.complicationInstanceId,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_ADD_TASK, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return when (request.complicationType) {
            ComplicationType.SMALL_IMAGE ->
                SmallImageComplicationData.Builder(smallImage(), contentDescription)
                    .setTapAction(tapIntent)
                    .build()

            ComplicationType.MONOCHROMATIC_IMAGE ->
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, R.drawable.ic_add_task)
                    ).build(),
                    contentDescription,
                )
                    .setTapAction(tapIntent)
                    .build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val contentDescription = PlainComplicationText.Builder("Add task").build()

        return when (type) {
            ComplicationType.MONOCHROMATIC_IMAGE ->
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(this, R.drawable.ic_add_task)
                    ).build(),
                    contentDescription,
                )
                    .build()

            else ->
                SmallImageComplicationData.Builder(smallImage(), contentDescription)
                    .build()
        }
    }
}

class ShortcutComplicationService : BaseComplicationService() {

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val filter = applicationContext.getComplicationFilter(request.complicationInstanceId)
        val label = applicationContext.getComplicationFilterTitle(request.complicationInstanceId)
            ?: "Tasks"
        val contentDescription = PlainComplicationText.Builder(label).build()
        val monoImage = monoImage()
        val tapAction = tapIntent(request.complicationInstanceId, filter)

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(label).build(),
                    contentDescription,
                )
                    .setMonochromaticImage(monoImage)
                    .setTapAction(tapAction)
                    .build()

            ComplicationType.MONOCHROMATIC_IMAGE ->
                MonochromaticImageComplicationData.Builder(monoImage, contentDescription)
                    .setTapAction(tapAction)
                    .build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val contentDescription = PlainComplicationText.Builder("Tasks").build()
        val monoImage = monoImage()

        return when (type) {
            ComplicationType.MONOCHROMATIC_IMAGE ->
                MonochromaticImageComplicationData.Builder(monoImage, contentDescription)
                    .build()

            else ->
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("Tasks").build(),
                    contentDescription,
                )
                    .setMonochromaticImage(monoImage)
                    .build()
        }
    }
}
