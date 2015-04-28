/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.reminders;

import android.app.PendingIntent;
import android.content.Intent;

import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

import org.tasks.Broadcaster;
import org.tasks.injection.TestModule;
import org.tasks.notifications.NotificationManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class NotificationTests extends DatabaseTestCase {

    @Module(addsTo = TestModule.class, injects = {NotificationTests.class})
    static class NotificationTestsModule {
        @Singleton
        @Provides
        public NotificationManager getNotificationManager() {
            return mock(NotificationManager.class);
        }

        @Singleton
        @Provides
        public Broadcaster getBroadcaster() {
            return mock(Broadcaster.class);
        }
    }

    @Inject TaskDao taskDao;
    @Inject Notifications notifications;
    @Inject NotificationManager notificationManager;
    @Inject Broadcaster broadcaster;

    @Override
    protected void tearDown() {
        super.tearDown();

        verifyNoMoreInteractions(notificationManager);
        verifyNoMoreInteractions(broadcaster);
    }

    public void testAlarmToNotification() {
        final Task task = new Task() {{
            setTitle("rubberduck");
            setDueDate(DateUtilities.now() - DateUtilities.ONE_DAY);
        }};
        taskDao.persist(task);

        notifications.handle(new Intent() {{
            putExtra(Notifications.ID_KEY, task.getId());
            putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
        }});

        verify(broadcaster).requestNotification(
                eq(task.getId()),
                any(PendingIntent.class),
                eq(ReminderService.TYPE_DUE),
                eq("rubberduck"),
                eq("Tasks"),
                eq(1));
    }

    public void testDeletedTaskDoesntTriggerNotification() {
        final Task task = new Task() {{
            setTitle("gooeyduck");
            setDeletionDate(DateUtilities.now());
        }};
        taskDao.persist(task);

        notifications.handle(new Intent() {{
            putExtra(Notifications.ID_KEY, task.getId());
            putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
        }});

        verify(notificationManager).cancel((int) task.getId());
    }

    public void testCompletedTaskDoesntTriggerNotification() {
        final Task task = new Task() {{
            setTitle("rubberduck");
            setCompletionDate(DateUtilities.now());
        }};
        taskDao.persist(task);

        notifications.handle(new Intent() {{
            putExtra(Notifications.ID_KEY, task.getId());
            putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
        }});

        verify(notificationManager).cancel((int) task.getId());
    }

//    public void testQuietHours() {
//        final Task task = new Task();
//        task.setTitle("rubberduck");
//        taskDao.persist(task);
//        Intent intent = new Intent();
//        intent.putExtra(Notifications.ID_KEY, task.getId());
//
//        int hour = newDate().getHours();
//        Preferences.setStringFromInteger(R.string.p_rmd_quietStart, hour - 1);
//        Preferences.setStringFromInteger(R.string.p_rmd_quietEnd, hour + 1);
//
//        // due date notification has vibrate
//        Notifications.setNotificationManager(new TestNotificationManager() {
//            public void notify(int id, Notification notification) {
//                assertNull(notification.sound);
//                assertTrue((notification.defaults & Notification.DEFAULT_SOUND) == 0);
//                assertNotNull(notification.vibrate);
//                assertTrue(notification.vibrate.length > 0);
//            }
//        });
//        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
//        notifications.onReceive(getContext(), intent);
//
//        // random notification does not
//        Notifications.setNotificationManager(new TestNotificationManager() {
//            public void notify(int id, Notification notification) {
//                assertNull(notification.sound);
//                assertTrue((notification.defaults & Notification.DEFAULT_SOUND) == 0);
//                assertTrue(notification.vibrate == null ||
//                        notification.vibrate.length == 0);
//            }
//        });
//        intent.removeExtra(Notifications.EXTRAS_TYPE);
//        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_RANDOM);
//        notifications.onReceive(getContext(), intent);
//
//        // wrapping works
//        Preferences.setStringFromInteger(R.string.p_rmd_quietStart, hour + 2);
//        Preferences.setStringFromInteger(R.string.p_rmd_quietEnd, hour + 1);
//
//        Notifications.setNotificationManager(new TestNotificationManager() {
//            public void notify(int id, Notification notification) {
//                assertNull(notification.sound);
//                assertTrue((notification.defaults & Notification.DEFAULT_SOUND) == 0);
//            }
//        });
//        intent.removeExtra(Notifications.EXTRAS_TYPE);
//        intent.putExtra(Notifications.EXTRAS_TYPE, ReminderService.TYPE_DUE);
//        notifications.onReceive(getContext(), intent);
//
//        // nonstop notification still sounds
//        task.setReminderFlags(Task.NOTIFY_MODE_NONSTOP);
//        taskDao.persist(task);
//        Notifications.setNotificationManager(new TestNotificationManager() {
//            public void notify(int id, Notification notification) {
//                assertTrue(notification.sound != null ||
//                        (notification.defaults & Notification.DEFAULT_SOUND) > 0);
//            }
//        });
//        notifications.onReceive(getContext(), intent);
//    }

    @Override
    protected Object getModule() {
        return new NotificationTestsModule();
    }
}
