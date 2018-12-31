package org.tasks.gtasks;

import android.content.Context;
import androidx.test.runner.AndroidJUnit4;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.dao.Database;

import org.tasks.LocalBroadcastManager;
import org.tasks.billing.Inventory;
import org.tasks.data.GoogleTask;
import org.tasks.data.GoogleTaskDao;
import org.tasks.data.GoogleTaskListDao;
import org.tasks.data.TagDao;
import org.tasks.data.TagData;
import org.tasks.data.TagDataDao;

import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksListService;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;
import com.todoroo.astrid.gtasks.GtasksTaskListUpdater;
import com.todoroo.astrid.gtasks.sync.GtasksSyncService;
import com.todoroo.astrid.service.TaskCreator;
import com.todoroo.astrid.service.TaskDeleter;

import junit.framework.Assert; 

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.tasks.R;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.ForApplication;
import org.tasks.injection.InjectingApplication;
import org.tasks.notifications.NotificationManager;
import org.tasks.preferences.DefaultFilterProvider;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import org.tasks.analytics.Tracker;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class GoogleTaskSyncNoteAdapterTest {

    private static int DEFAULT_WRITE_IMPORTANCE = Task.Priority.LOW;
    private static int DEFAULT_WRITE_REMINDER_FLAGS = Task.NOTIFY_AT_DEADLINE |  Task.NOTIFY_AFTER_DEADLINE;
    private static int DEFAULT_READ_IMPORTANCE = Task.Priority.LOW;
    private static int DEFAULT_READ_REMINDER_FLAGS = Task.NOTIFY_AT_DEADLINE |  Task.NOTIFY_AFTER_DEADLINE;

    private GoogleTaskSynchronizer adapter;
    private Task task;
    private Map<String, TagData> inputTags;
    private Set<String> persistedTags;
    private com.google.api.services.tasks.model.Task remoteModel;
    private String enhancedNotes;
    private boolean enableNoteMetadataSync = true;

    private Context context;
    private GoogleTaskListDao googleTaskListDao;
    private GtasksSyncService gtasksSyncService;
    private GtasksListService gtasksListService;
    private GtasksTaskListUpdater gtasksTaskListUpdater;
    private Preferences preferences;
    private GtasksPreferenceService gtasksPreferenceService;
    private TaskDao taskDao;
    private TagDao tagDao;
    private TagDataDao tagDataDao;
    private Tracker tracker;
    private NotificationManager notificationManager;
    private GoogleTaskDao googleTaskDao;
    private TaskCreator taskCreator;
    private DefaultFilterProvider defaultFilterProvider;
    private PlayServices playServices;
    private PermissionChecker permissionChecker;
    private GoogleAccountManager googleAccountManager;
    private LocalBroadcastManager localBroadcastManager;
    private Inventory inventory;
    private TaskDeleter taskDeleter;

    @Before
    public void setup() {
        ApplicationComponent applicationComponent = Mockito.mock(ApplicationComponent.class);
        InjectingApplication injectingApplication = Mockito.mock(InjectingApplication.class);
        Mockito.when(injectingApplication.getComponent()).thenReturn(applicationComponent);
        context = Mockito.mock(Context.class);
        Mockito.when(context.getApplicationContext()).thenReturn(injectingApplication);

        preferences = Mockito.mock(Preferences.class);
        Mockito.when(preferences.getIntegerFromString(Matchers.eq(R.string.p_default_importance_key), Mockito.anyInt())).thenReturn(DEFAULT_WRITE_IMPORTANCE);
        Mockito.when(preferences.getIntegerFromString(Matchers.eq(R.string.p_default_reminders_key), Mockito.anyInt())).thenReturn(DEFAULT_WRITE_REMINDER_FLAGS);

        gtasksPreferenceService = new GtasksPreferenceService(preferences) {
            public boolean getUseNoteForMetadataSync() {
                return enableNoteMetadataSync;
            }
        };

        googleTaskDao = Mockito.mock(GoogleTaskDao.class);
        tagDao = Mockito.mock(TagDao.class);

        inputTags = new HashMap<>();
        persistedTags = new HashSet<>();
        tagDataDao = new TagDataDao() {

            public TagData getTagByName(String name) {
                return null;
            }

            public List<TagData> getAll() { return null; }

            public List<TagData> tagDataOrderedByName() { return getAll(); }

            public long insert(TagData tagData) {
                persistedTags.add(tagData.getName());
                return 0;
            }

            public void update(TagData tagData) {
                persistedTags.add(tagData.getName());
            }

            public void rename(String remoteId, String name) {}

            public void delete(Long id) {}

            public TagData getByUuid(String uuid) {
                return inputTags.get(uuid);
            }

        };

        adapter = new GoogleTaskSynchronizer(context,
                googleTaskListDao,
                gtasksSyncService,
                gtasksListService,
                gtasksTaskListUpdater,
                preferences,
                gtasksPreferenceService,
                taskDao,
                tagDao,
                tagDataDao,
                tracker,
                notificationManager,
                googleTaskDao,
                taskCreator,
                defaultFilterProvider,
                playServices,
                permissionChecker,
                googleAccountManager,
                localBroadcastManager,
                inventory,
                taskDeleter);

        task = new Task();
        task.setTags(new ArrayList<>());
        task.setPriority(DEFAULT_WRITE_IMPORTANCE);
        task.setReminderFlags(DEFAULT_WRITE_REMINDER_FLAGS);
        remoteModel = new com.google.api.services.tasks.model.Task();
        remoteModel.setNotes(null);
    }

    private void addTag(String tagName) {
        String uuid = UUID.randomUUID().toString();
        task.getTags().add(tagName);
        TagData tag = new TagData();
        tag.setName(tagName);
        tag.setRemoteId(uuid);
        inputTags.put(uuid, tag);
    }

    @Test
    public void testEmptyDisabled() {
        enableNoteMetadataSync = false;
        task.setNotes("");
        addTag("test");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes(); 
        Assert.assertEquals("", enhancedNotes);
    }

    @Test
    public void testNullDisabled() {
        enableNoteMetadataSync = false;
        task.setNotes(null);
        addTag("test");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes(); 
        Assert.assertEquals(null, enhancedNotes);
    }

    @Test
    public void testNotSetDisabled() {
        enableNoteMetadataSync = false;
        addTag("test");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes(); 
        Assert.assertEquals("", enhancedNotes);
    }

    @Test
    public void testStoreMetadataToNoteDisabled() {
        enableNoteMetadataSync = false;
        task.setNotes("xxx");
        addTag("test");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals("xxx", enhancedNotes);
    }

    @Test
    public void testNothingToDo() {
        task.setNotes("xxx");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals("xxx", enhancedNotes);
    }

    @Test
    public void testEmpty() {
        task.setNotes("");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals("", enhancedNotes);
    }

    @Test
    public void testNull() {
        task.setNotes(null);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(null, enhancedNotes);
    }

    @Test
    public void testNotSet() {
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals("", enhancedNotes);
    }

    @Test
    public void testStoreMetadataToNote() {
        task.setNotes("xxx");
        addTag("test");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals("xxx" + GoogleTaskSynchronizer.LINE_FEED + "" + GoogleTaskSynchronizer.LINE_FEED + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataWithoutNote() {
        addTag("test");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSynchronizer.LINE_FEED + GoogleTaskSynchronizer.LINE_FEED  + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }


    @Test
    public void testStoreMetadataTasksHide0() {
        addTag("test");
        task.setHideUntil(0l);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSynchronizer.LINE_FEED + GoogleTaskSynchronizer.LINE_FEED  + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataImportanceDefault() {
        addTag("test");
        task.setPriority(DEFAULT_WRITE_IMPORTANCE);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSynchronizer.LINE_FEED + GoogleTaskSynchronizer.LINE_FEED  + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataImportanceLow() {
        addTag("test");
        task.setPriority(Task.Priority.NONE);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSynchronizer.LINE_FEED + GoogleTaskSynchronizer.LINE_FEED  + "{tasks:additionalMetadata{\"importance\":\"LOW\",\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataReminderNone() {
        addTag("test");
        task.setReminderFlags(0);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSynchronizer.LINE_FEED + GoogleTaskSynchronizer.LINE_FEED  + "{tasks:additionalMetadata{\"notifyAfterDeadline\":false,\"notifyAtDeadline\":false,\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataReminderDefault() {
        addTag("test");
        task.setReminderFlags(DEFAULT_WRITE_REMINDER_FLAGS);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSynchronizer.LINE_FEED + GoogleTaskSynchronizer.LINE_FEED  + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataToNoteAll() {
        task.setNotes("xxx");
        addTag("test");
        addTag("test2");
        addTag("test3");
        addTag("test");
        task.setReminderFlags(Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE | Task.NOTIFY_MODE_FIVE);
        RRule rrule = new RRule();
        rrule.setFreq(Frequency.MONTHLY);
        rrule.setInterval(1);
        String recurrence = rrule.toIcal();
        task.setRecurrence(recurrence);
        task.setRepeatUntil(123456789l);
        task.setHideUntil(1111111111l);
        task.setPriority(Task.Priority.HIGH);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals("xxx" + GoogleTaskSynchronizer.LINE_FEED + "" + GoogleTaskSynchronizer.LINE_FEED + "{tasks:additionalMetadata{\"hideUntil\":\"1970-01-13T20:38:31\",\"importance\":\"MUST\",\"notifyModeFive\":true,\"recurrence\":\"RRULE:FREQ\\u003dMONTHLY;INTERVAL\\u003d1\",\"repeatUntil\":\"1970-01-02T10:17:36\",\"tags\":[\"test\",\"test2\",\"test3\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataSkipTags() {
        task.setNotes("xxx");
        addTag("test");
        addTag("test2");
        addTag("test4;");
        addTag("{test4");
        addTag("}test4");
        addTag("tes;t4");
        addTag("test4{");
        addTag("test3");
        addTag("");
        addTag("    ");
        addTag(null);
        addTag("test");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals("xxx" + GoogleTaskSynchronizer.LINE_FEED + "" + GoogleTaskSynchronizer.LINE_FEED + "{tasks:additionalMetadata{\"tags\":[\"test\",\"test2\",\"test3\"]}}", enhancedNotes);
    }

    @Test
    public void testReadMetadataToNote() {
        testStoreMetadataToNote();
        task = new Task();
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("xxx", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
    }

    @Test
    public void testReadMetadataWithoutNote() {
        testStoreMetadataWithoutNote();
        task = new Task();
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
    }

    @Test
    public void testReadMetadataTasksHide0() {
        testStoreMetadataTasksHide0();
        task = new Task();
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
        Assert.assertEquals(0l, (long)task.getHideUntil());
    }

    @Test
    public void testReadMetadataImportanceDefault() {
        testStoreMetadataImportanceDefault();
        task = new Task();
        task.setPriority(DEFAULT_READ_IMPORTANCE);
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
        Assert.assertEquals(DEFAULT_READ_IMPORTANCE, (int)task.getPriority());
    }

    @Test
    public void testReadMetadataImportanceLow() {
        testStoreMetadataImportanceLow();
        task = new Task();
        task.setPriority(DEFAULT_READ_IMPORTANCE);
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
        Assert.assertEquals(Task.Priority.NONE, (int)task.getPriority());
    }

    @Test
    public void testReadMetadataReminderDefault() {
        testStoreMetadataReminderDefault();
        task = new Task();
        task.setReminderFlags(DEFAULT_READ_REMINDER_FLAGS);
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
        Assert.assertEquals(DEFAULT_READ_REMINDER_FLAGS, (int)task.getReminderFlags());
    }


    @Test
    public void testReadMetadataReminderNone() {
        testStoreMetadataReminderNone();
        task = new Task();
        task.setReminderFlags(DEFAULT_WRITE_REMINDER_FLAGS);
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
        Assert.assertEquals(0, (int)task.getReminderFlags());
    }


    @Test
    public void testReadMetadataFromNoteAll() {
        testStoreMetadataToNoteAll();
        task = new Task();
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("xxx", task.getNotes());

        Assert.assertTrue(persistedTags.contains("test"));
        Assert.assertTrue(persistedTags.contains("test2"));
        Assert.assertTrue(persistedTags.contains("test3"));
        Assert.assertEquals(Task.NOTIFY_AT_DEADLINE | Task.NOTIFY_AFTER_DEADLINE  | Task.NOTIFY_MODE_FIVE, (int)task.getReminderFlags());
        Assert.assertEquals("RRULE:FREQ=MONTHLY;INTERVAL=1", task.getRecurrence());
        Assert.assertEquals(123456000l, (long)task.getRepeatUntil());
        Assert.assertEquals(1111111000l, (long)task.getHideUntil());
        Assert.assertEquals(Task.Priority.HIGH, (long)task.getPriority());
    }

    @Test
    public void testReadMetadataFromNoteRecurrenceWithoutRepeatUntil() {
        enhancedNotes = "xxx" + GoogleTaskSynchronizer.LINE_FEED + "" + GoogleTaskSynchronizer.LINE_FEED +  "{tasks:additionalMetadata{\"recurrence\":\"RRULE:FREQ=MONTHLY;INTERVAL=1\"}}";
        task = new Task();
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("xxx", task.getNotes());
        Assert.assertEquals("RRULE:FREQ=MONTHLY;INTERVAL=1", task.getRecurrence());
        Assert.assertEquals(0, (long)task.getRepeatUntil());
    }

    @Test
    public void testReadMetadataFromNoteCorruptJSON() {
        enhancedNotes = "xxx" + GoogleTaskSynchronizer.LINE_FEED + "" + GoogleTaskSynchronizer.LINE_FEED +  "{tasks:additionalMetadata{\"recurrence\":\"}}";
        task = new Task();
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("xxx", task.getNotes());
        Assert.assertEquals("", task.getRecurrence());
        Assert.assertEquals(0, (long)task.getRepeatUntil());
    }

}
