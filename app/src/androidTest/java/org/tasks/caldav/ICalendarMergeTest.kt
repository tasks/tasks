package org.tasks.caldav

import com.natpryce.makeiteasy.MakeItEasy.with
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.model.property.XProperty
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.data.entity.CaldavAccount
import org.tasks.injection.InjectingTestCase
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.TITLE
import org.tasks.makers.TaskMaker.newTask
import javax.inject.Inject

@HiltAndroidTest
class ICalendarMergeTest : InjectingTestCase() {

    @Inject lateinit var iCal: iCalendar

    @Test
    fun unknownPropertyIsNotClobberedWhenPushingLocalEdit() = runBlocking {
        val remote = Ical4androidTaskAdapter(
            at.bitfire.ical4android.Task().apply {
                uid = "1234"
                unknownProperties.add(XProperty("X-CUSTOM-PROP", "bar"))
            }
        )

        val data = iCal.toVtodo(
            account = CaldavAccount(),
            caldavTask = newCaldavTask(with(REMOTE_ID, "1234")),
            task = newTask(with(TITLE, "local title")),
            remoteModel = remote,
        )

        val parsed = fromVtodo(String(data))!!
        assertEquals("local title", parsed.summary)
        assertEquals(
            "bar",
            parsed.unknownProperties.find { it.name == "X-CUSTOM-PROP" }?.value
        )
    }
}
