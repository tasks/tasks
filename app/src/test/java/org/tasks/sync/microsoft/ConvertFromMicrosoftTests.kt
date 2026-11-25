package org.tasks.sync.microsoft

import com.natpryce.makeiteasy.MakeItEasy
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.TestUtilities
import org.tasks.TestUtilities.withTZ
import org.tasks.data.createHideUntil
import org.tasks.data.entity.Task
import org.tasks.makers.TaskMaker
import org.tasks.sync.microsoft.MicrosoftConverter.applyRemote
import org.tasks.time.DateTime

class ConvertFromMicrosoftTests {
    @Test
    fun titleFromRemote() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.json")
        assertEquals("Basic task", local.title)
    }

    @Test
    fun useNullForBlankBody() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.json")
        Assert.assertNull(local.notes)
    }

    @Test
    fun parseDescription() {
        val (local, _) = TestUtilities.mstodo("microsoft/task_with_description.json")
        assertEquals("Hello world\r\n", local.notes)
    }

    @Test
    fun keepPriority() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.json",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.MEDIUM)),
            defaultPriority = Task.Priority.LOW
        )
        assertEquals(Task.Priority.MEDIUM, local.priority)
    }

    @Test
    fun useDefaultPriority() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.json",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)),
            defaultPriority = Task.Priority.LOW
        )
        assertEquals(Task.Priority.LOW, local.priority)
    }

    @Test
    fun noPriorityWhenDefaultIsHigh() {
        val (local, _) = TestUtilities.mstodo(
            "microsoft/basic_task.json",
            task = TaskMaker.newTask(MakeItEasy.with(TaskMaker.PRIORITY, Task.Priority.HIGH)),
            defaultPriority = Task.Priority.HIGH
        )
        assertEquals(Task.Priority.NONE, local.priority)
    }

    @Test
    fun noCompletionDate() {
        val (local, _) = TestUtilities.mstodo("microsoft/basic_task.json")
        assertEquals(0, local.completionDate)
    }

    @Test
    fun parseCompletionDate() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/completed_task.json")
            assertEquals(DateTime(2022, 9, 18, 0, 0).millis, local.completionDate)
        }
    }

    @Test
    fun parseDueDate() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.json")
            assertEquals(DateTime(2023, 7, 19, 12, 0).millis, local.dueDate)
        }
    }

    @Test
    fun dueDateSetOutsideOfTasksIsNotADayEarly() {
        withTZ("America/Los_Angeles") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetOutsideOfTasksEastOfUtc() {
        withTZ("Pacific/Auckland") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetOutsideOfTasksEastOfUtcDuringDst() {
        withTZ("Pacific/Auckland") {
            assertEquals(
                DateTime(2023, 1, 19, 12, 0).millis,
                dueDate("2023-01-19T00:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSurvivesTimeZoneChange() {
        withTZ("America/Denver") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T05:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetWestOfDeviceTimeZone() {
        withTZ("Asia/Tokyo") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T05:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetEastOfDeviceTimeZone() {
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-18T15:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetOnDeviceFarEastOfUtcDuringDst() {
        withTZ("Pacific/Auckland") {
            assertEquals(
                DateTime(2023, 1, 19, 12, 0).millis,
                dueDate("2023-01-18T11:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateInRemoteTimeZone() {
        withTZ("Asia/Tokyo") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate(
                    dateTime = "2023-07-19T00:00:00.0000000",
                    timeZone = "America/Chicago",
                )
            )
        }
    }

    @Test
    fun parseDueDateEastOfUtc() {
        withTZ("Europe/Berlin") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-18T22:00:00.0000000")
            )
        }
    }

    @Test
    fun parseDueDateFarEastOfUtc() {
        withTZ("Pacific/Auckland") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-18T12:00:00.0000000")
            )
        }
    }

    @Test
    fun ignoreFractionalSecondsOnDueDate() {
        withTZ("America/Los_Angeles") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00.5230000")
            )
        }
    }

    @Test
    fun ignoreFractionalSecondsOnDueDateEastOfUtc() {
        withTZ("Pacific/Auckland") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00.5230000")
            )
        }
    }

    @Test
    fun ignoreFractionalSecondsOnDueDateSetFarWestOfUtcOnADeviceThatHasMoved() {
        withTZ("Europe/Berlin") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T10:00:00.5230000")
            )
        }
    }

    @Test
    fun keepDueTimeWhenDueDateChangesRemotely() {
        withTZ("America/Los_Angeles") {
            assertEquals(
                DateTime(2023, 7, 19, 15, 0, 1).millis,
                dueDate(
                    dateTime = "2023-07-19T00:00:00.0000000",
                    task = TaskMaker.newTask(
                        MakeItEasy.with(TaskMaker.DUE_TIME, DateTime(2023, 7, 18, 15, 0))
                    )
                )
            )
        }
    }

    @Test
    fun clearDueDateAndStartDate() {
        withTZ("America/Los_Angeles") {
            val local = TaskMaker.newTask(
                MakeItEasy.with(TaskMaker.DUE_TIME, DateTime(2023, 7, 18, 15, 0)),
                MakeItEasy.with(TaskMaker.START_DATE, DateTime(2023, 7, 17, 0, 0)),
            )
            local.applyRemote(Tasks.Task(), Task.Priority.NONE)
            assertEquals(0, local.dueDate)
            assertEquals(0, local.hideUntil)
        }
    }

    @Test
    fun keepDueDateAndStartDateWhenRemoteDueDateIsUnparseable() {
        withTZ("America/Los_Angeles") {
            val local = TaskMaker.newTask(
                MakeItEasy.with(TaskMaker.DUE_TIME, DateTime(2023, 7, 18, 15, 0)),
                MakeItEasy.with(TaskMaker.START_DATE, DateTime(2023, 7, 17, 0, 0)),
            )
            local.applyRemote(
                remote = Tasks.Task(
                    dueDateTime = Tasks.Task.DateTime(
                        dateTime = "19/07/2023",
                        timeZone = "UTC",
                    )
                ),
                defaultPriority = Task.Priority.NONE,
            )
            assertEquals(DateTime(2023, 7, 18, 15, 0, 1).millis, local.dueDate)
            assertEquals(DateTime(2023, 7, 17, 0, 0).millis, local.hideUntil)
        }
    }

    @Test
    fun parseCompletionDateWithFractionalSeconds() {
        withTZ("America/Chicago") {
            val local = Task()
            local.applyRemote(
                remote = Tasks.Task(
                    completedDateTime = Tasks.Task.DateTime(
                        dateTime = "2022-09-18T05:00:00.1230000",
                        timeZone = "UTC",
                    )
                ),
                defaultPriority = Task.Priority.NONE,
            )
            assertEquals(DateTime(2022, 9, 18, 0, 0, 0, 123).millis, local.completionDate)
        }
    }

    @Test
    fun keepCompletionDateWhenRemoteCompletionDateIsUnparseable() {
        withTZ("America/Chicago") {
            val local = TaskMaker.newTask(
                MakeItEasy.with(TaskMaker.COMPLETION_TIME, DateTime(2022, 9, 18, 0, 0))
            )
            local.applyRemote(
                remote = Tasks.Task(
                    completedDateTime = Tasks.Task.DateTime(
                        dateTime = "18/09/2022",
                        timeZone = "UTC",
                    )
                ),
                defaultPriority = Task.Priority.NONE,
            )
            assertEquals(DateTime(2022, 9, 18, 0, 0).millis, local.completionDate)
        }
    }

    @Test
    fun parseCreationDate() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.json")
            assertEquals(
                DateTime(2023, 7, 19, 23, 20, 56, 9).millis,
                local.creationDate
            )
        }
    }

    @Test
    fun parseModificationDate() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/basic_task_with_due_date.json")
            assertEquals(
                DateTime(2023, 7, 19, 23, 21, 6, 269).millis,
                local.modificationDate
            )
        }
    }

    @Test
    fun parseDailyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_daily.json")
            assertEquals("FREQ=DAILY", local.recurrence)
        }
    }

    @Test
    fun parseWeekdayRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_weekdays.json")
            assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,TU,WE,TH,FR", local.recurrence)
        }
    }

    @Test
    fun parseAbsoluteMonthlyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_monthly.json")
            assertEquals("FREQ=MONTHLY", local.recurrence)
        }
    }

    @Test
    fun parseAbsoluteYearlyRecurrence() {
        withTZ("America/Chicago") {
            val (local, _) = TestUtilities.mstodo("microsoft/repeat_yearly.json")
            assertEquals("FREQ=YEARLY", local.recurrence)
        }
    }

    @Test
    fun repeatedSyncsDoNotMoveADueTimeThatDoesNotExist() {
        withTZ("Pacific/Chatham") {
            // 2019-09-29 02:45 -> 03:45 does not exist in Chatham
            val local = TaskMaker.newTask(
                MakeItEasy.with(TaskMaker.DUE_TIME, DateTime(2022, 4, 3, 2, 50))
            )
            val remote = Tasks.Task(
                dueDateTime = Tasks.Task.DateTime(
                    dateTime = "2019-09-29T00:00:00.0000000",
                    timeZone = "UTC",
                )
            )

            local.applyRemote(remote = remote, defaultPriority = Task.Priority.NONE)
            assertEquals(DateTime(2019, 9, 29, 3, 50, 1).millis, local.dueDate)

            local.applyRemote(remote = remote, defaultPriority = Task.Priority.NONE)
            assertEquals(DateTime(2019, 9, 29, 3, 50, 1).millis, local.dueDate)
        }
    }

    @Test
    fun remoteDayIsNotPushedForwardWhenTheLocalDueTimeDoesNotExist() {
        withTZ("America/Nuuk") {
            // 2025-03-29 23:00 -> 24:00 does not exist in Nuuk
            val local = TaskMaker.newTask(
                MakeItEasy.with(TaskMaker.DUE_TIME, DateTime(2025, 3, 30, 23, 30))
            )

            local.applyRemote(
                remote = Tasks.Task(
                    dueDateTime = Tasks.Task.DateTime(
                        dateTime = "2025-03-29T00:00:00.0000000",
                        timeZone = "UTC",
                    )
                ),
                defaultPriority = Task.Priority.NONE,
            )

            assertEquals(
                DateTime(2025, 3, 29).millis,
                DateTime(local.dueDate).startOfDay().millis
            )
        }
    }

    @Test
    fun parseDueDateWithUtcSuffix() {
        withTZ("America/Los_Angeles") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00.0000000Z")
            )
        }
    }

    @Test
    fun parseDueDateWithUtcOffset() {
        withTZ("America/Denver") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00.0000000-05:00")
            )
        }
    }

    @Test
    fun parseDueDateWithUtcOffsetFarWestOfUtc() {
        withTZ("Europe/Berlin") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00.0000000-11:00")
            )
        }
    }

    @Test
    fun statedOffsetWinsOverCoincidentalDeviceMidnight() {
        withTZ("Pacific/Apia") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00.0000000-11:00")
            )
        }
    }

    @Test
    fun parseDueDateWithUtcSuffixSetEastOfUtc() {
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-18T23:00:00.0000000Z")
            )
        }
    }

    @Test
    fun parseDueDateWithCommaDecimalSeparator() {
        withTZ("America/Los_Angeles") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T00:00:00,0000000")
            )
        }
    }

    @Test
    fun parseDueDateWithoutTimeOfDay() {
        withTZ("America/Los_Angeles") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19")
            )
        }
    }

    @Test
    fun parseCompletionDateWithUtcSuffix() {
        withTZ("America/Chicago") {
            val local = Task()
            local.applyRemote(
                remote = Tasks.Task(
                    completedDateTime = Tasks.Task.DateTime(
                        dateTime = "2022-09-18T05:00:00.1230000Z",
                        timeZone = "UTC",
                    )
                ),
                defaultPriority = Task.Priority.NONE,
            )
            assertEquals(DateTime(2022, 9, 18, 0, 0, 0, 123).millis, local.completionDate)
        }
    }

    @Test
    fun parseCreatedDateWithoutUtcSuffix() {
        withTZ("America/Chicago") {
            val local = Task()
            local.applyRemote(
                remote = Tasks.Task(createdDateTime = "2022-09-18T05:00:00.0000000"),
                defaultPriority = Task.Priority.NONE,
            )
            assertEquals(DateTime(2022, 9, 18, 0, 0).millis, local.creationDate)
        }
    }

    @Test
    fun markCompleteWhenRemoteCompletionDateIsUnparseable() {
        withTZ("America/Chicago") {
            val now = DateTime(2022, 9, 18, 13, 0)
            freezeAt(now) {
                val local = Task()
                local.applyRemote(
                    remote = Tasks.Task(
                        completedDateTime = Tasks.Task.DateTime(
                            dateTime = "18/09/2022",
                            timeZone = "UTC",
                        )
                    ),
                    defaultPriority = Task.Priority.NONE,
                )
                assertEquals(now.millis, local.completionDate)
            }
        }
    }

    @Test
    fun dueDateSetFarEastOfUtcOnADeviceThatHasMoved() {
        withTZ("UTC") {
            assertEquals(
                DateTime(2023, 1, 19, 12, 0).millis,
                dueDate("2023-01-18T11:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetEastOfUtcOnADeviceThatHasMoved() {
        withTZ("UTC") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-18T12:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetInAQuarterHourOffsetOnADeviceThatHasMoved() {
        withTZ("UTC") {
            assertEquals(
                DateTime(2023, 1, 19, 12, 0).millis,
                dueDate("2023-01-18T10:15:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetFarWestOfUtcOnADeviceThatHasMoved() {
        withTZ("UTC") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T10:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateSetWestOfUtcOnADeviceThatHasMoved() {
        withTZ("UTC") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T05:00:00.0000000")
            )
        }
    }

    @Test
    fun ambiguousMidnightIsAssumedToBeEastOfUtc() {
        withTZ("Europe/Berlin") {
            assertEquals(
                DateTime(2023, 7, 20, 12, 0).millis,
                dueDate("2023-07-19T11:00:00.0000000")
            )
        }
    }

    @Test
    fun dueDateInRemoteTimeZoneThatIsNotMidnight() {
        withTZ("Asia/Tokyo") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate(
                    dateTime = "2023-07-19T19:00:00.0000000",
                    timeZone = "America/Chicago",
                )
            )
        }
    }

    @Test
    fun remoteTimeZoneWinsOverACoincidentalDeviceMidnight() {
        withTZ("UTC") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate(
                    dateTime = "2023-07-19T19:00:00.0000000",
                    timeZone = "America/Chicago",
                )
            )
        }
    }

    @Test
    fun dueDateLateInTheDayInARemoteTimeZoneWestOfUtc() {
        withTZ("Asia/Tokyo") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate(
                    dateTime = "2023-07-19T23:00:00.0000000",
                    timeZone = "America/Los_Angeles",
                )
            )
        }
    }

    @Test
    fun unknownRemoteTimeZoneStillInfersAgainstUtc() {
        withTZ("Europe/Berlin") {
            assertEquals(
                DateTime(2023, 7, 20, 12, 0).millis,
                dueDate(
                    dateTime = "2023-07-19T11:00:00.0000000",
                    timeZone = "Pacific Standard Time",
                )
            )
        }
    }

    @Test
    fun keepStartDateAtMidnightWhenPinnedToALegacyMidnightDueDate() {
        withTZ("America/Chicago") {
            val local = Task(dueDate = DateTime(2023, 7, 19).millis)
            local.hideUntil = local.createHideUntil(Task.HIDE_UNTIL_DUE, 0)
            assertEquals(local.dueDate, local.hideUntil)

            local.applyDueDate("2023-07-19T05:00:00.0000000")

            assertEquals(DateTime(2023, 7, 19, 12, 0).millis, local.dueDate)
            assertEquals(DateTime(2023, 7, 19).millis, local.hideUntil)
        }
    }

    @Test
    fun startDatePinnedToALegacyMidnightDueDateFollowsARemoteDayChange() {
        withTZ("America/Chicago") {
            val local = Task(dueDate = DateTime(2023, 7, 19).millis)
            local.hideUntil = local.createHideUntil(Task.HIDE_UNTIL_DUE, 0)

            local.applyDueDate("2023-07-25T05:00:00.0000000")

            assertEquals(DateTime(2023, 7, 25, 12, 0).millis, local.dueDate)
            assertEquals(DateTime(2023, 7, 25).millis, local.hideUntil)
        }
    }

    @Test
    fun repinAStartDateThatWasAlreadyDraggedToNoon() {
        withTZ("America/Chicago") {
            val local = Task(dueDate = DateTime(2023, 7, 19, 12, 0).millis)
            local.hideUntil = local.dueDate

            local.applyDueDate("2023-07-19T05:00:00.0000000")

            assertEquals(DateTime(2023, 7, 19, 12, 0).millis, local.dueDate)
            assertEquals(DateTime(2023, 7, 19).millis, local.hideUntil)
        }
    }

    @Test
    fun startDateStaysAtMidnightForACanonicalDueDate() {
        withTZ("America/Chicago") {
            val local = TaskMaker.newTask(
                MakeItEasy.with(TaskMaker.DUE_DATE, DateTime(2023, 7, 19)),
                MakeItEasy.with(TaskMaker.HIDE_TYPE, Task.HIDE_UNTIL_DUE),
            )
            assertEquals(DateTime(2023, 7, 19, 12, 0).millis, local.dueDate)
            assertEquals(DateTime(2023, 7, 19).millis, local.hideUntil)

            local.applyDueDate("2023-07-19T05:00:00.0000000")

            assertEquals(DateTime(2023, 7, 19, 12, 0).millis, local.dueDate)
            assertEquals(DateTime(2023, 7, 19).millis, local.hideUntil)
        }
    }

    @Test
    fun independentStartDateIsNotRepinnedToTheDueDate() {
        withTZ("America/Chicago") {
            val local = Task(dueDate = DateTime(2023, 7, 19).millis)
            local.hideUntil = DateTime(2023, 7, 17).millis

            local.applyDueDate("2023-07-19T05:00:00.0000000")

            assertEquals(DateTime(2023, 7, 19, 12, 0).millis, local.dueDate)
            assertEquals(DateTime(2023, 7, 17).millis, local.hideUntil)
        }
    }

    @Test
    fun offsetSuffixThatIsNotStartOfDayIsNotTakenAsTheWrittenDate() {
        withTZ("America/Chicago") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-18T23:00:00.0000000-05:00")
            )
        }
    }

    @Test
    fun allDayDateAtTenHundredUtcFavorsHawaiiOverKiritimati() {
        withTZ("Europe/Berlin") {
            assertEquals(
                DateTime(2023, 7, 19, 12, 0).millis,
                dueDate("2023-07-19T10:00:00.0000000")
            )
        }
    }

    private fun Task.applyDueDate(dateTime: String, timeZone: String = "UTC") {
        applyRemote(
            remote = Tasks.Task(
                dueDateTime = Tasks.Task.DateTime(dateTime = dateTime, timeZone = timeZone)
            ),
            defaultPriority = Task.Priority.NONE,
        )
    }

    private fun dueDate(
        dateTime: String,
        task: Task = Task(),
        timeZone: String = "UTC",
    ): Long {
        task.applyRemote(
            remote = Tasks.Task(
                dueDateTime = Tasks.Task.DateTime(dateTime = dateTime, timeZone = timeZone)
            ),
            defaultPriority = Task.Priority.NONE,
        )
        return task.dueDate
    }
}
