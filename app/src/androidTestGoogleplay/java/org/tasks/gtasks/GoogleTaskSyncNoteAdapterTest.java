package org.tasks.gtasks;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.todoroo.andlib.data.Property;
import com.todoroo.astrid.dao.Database;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gtasks.GtasksPreferenceService;

import junit.framework.Assert; 

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.tasks.R;
import org.tasks.injection.ApplicationComponent;
import org.tasks.injection.InjectingApplication;
import org.tasks.injection.SyncAdapterComponent;
import org.tasks.injection.SyncAdapterModule;
import org.tasks.preferences.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.natpryce.makeiteasy.MakeItEasy.with;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class GoogleTaskSyncNoteAdapterTest {

    private static int DEFAULT_WRITE_IMPORTANCE = Task.IMPORTANCE_SHOULD_DO;
    private static int DEFAULT_WRITE_REMINDER_FLAGS = Task.NOTIFY_AT_DEADLINE |  Task.NOTIFY_AFTER_DEADLINE;
    private static int DEFAULT_READ_IMPORTANCE = Task.IMPORTANCE_SHOULD_DO;
    private static int DEFAULT_READ_REMINDER_FLAGS = Task.NOTIFY_AT_DEADLINE |  Task.NOTIFY_AFTER_DEADLINE;

    private GoogleTaskSyncAdapter adapter;
    private TagDataDao tagDataDao;
    private Task task;
    private List<Metadata> inputMetadata;
    private Map<String, TagData> inputTags;
    private Set<String> persistedTags;
    private com.google.api.services.tasks.model.Task remoteModel;
    private String enhancedNotes;
    private boolean enableNoteMetadataSync = true;



    @Before
    public void setup() {
        SyncAdapterComponent syncAdapterComponent = Mockito.mock(SyncAdapterComponent.class);
        ApplicationComponent applicationComponent = Mockito.mock(ApplicationComponent.class);
        Mockito.when(applicationComponent.plus(Mockito.any(SyncAdapterModule.class))).thenReturn(syncAdapterComponent);
        InjectingApplication injectingApplication = Mockito.mock(InjectingApplication.class);
        Mockito.when(injectingApplication.getComponent()).thenReturn(applicationComponent);
        Context context = Mockito.mock(Context.class);
        Mockito.when(context.getApplicationContext()).thenReturn(injectingApplication);
        adapter = new GoogleTaskSyncAdapter(context, false);


        Preferences preferences = Mockito.mock(Preferences.class);
        Mockito.when(preferences.getIntegerFromString(Matchers.eq(R.string.p_default_importance_key), Mockito.anyInt())).thenReturn(DEFAULT_WRITE_IMPORTANCE);
        Mockito.when(preferences.getIntegerFromString(Matchers.eq(R.string.p_default_reminders_key), Mockito.anyInt())).thenReturn(DEFAULT_WRITE_REMINDER_FLAGS);
        adapter.preferences = preferences;
        GtasksPreferenceService gtasksPreferenceService = new GtasksPreferenceService(preferences) {
            public boolean getUseNoteForMetadataSync() {
                return enableNoteMetadataSync;
            }
        };
        adapter.gtasksPreferenceService = gtasksPreferenceService;


        MetadataDao metadataDao = Mockito.mock(MetadataDao.class);
        adapter.metadataDao = metadataDao;

        Database database = Mockito.mock(Database.class);
        inputTags = new HashMap<>();
        persistedTags = new HashSet<>();
        tagDataDao = new TagDataDao(database) {

            public TagData getTagByName(String name, Property<?>... properties) {
                return null;
            }

            public void persist(TagData tagData) {
                persistedTags.add(tagData.getName());
            }

            public TagData getByUuid(String uuid, Property<?>... properties) {
                return inputTags.get(uuid);
            }
        };
        adapter.tagDataDao = tagDataDao;

        inputMetadata = new ArrayList<>();
        Mockito.when(metadataDao.byTaskAndKey(Mockito.anyLong(), Mockito.anyString())).thenReturn(inputMetadata);


        task = new Task();
        task.setImportance(DEFAULT_WRITE_IMPORTANCE);
        task.setReminderFlags(DEFAULT_WRITE_REMINDER_FLAGS);
        remoteModel = new com.google.api.services.tasks.model.Task();
        remoteModel.setNotes(null);

    }

    private void addTag(String tagName) {
        String uuid = UUID.randomUUID().toString();
        Metadata md = new Metadata();
        md.setValue(Metadata.VALUE2, uuid);
        inputMetadata.add(md);
        TagData tag = new TagData();
        tag.setName(tagName);
        tag.setUUID(uuid);
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
        Assert.assertEquals("", enhancedNotes);
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
        Assert.assertEquals("", enhancedNotes);
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
        Assert.assertEquals("xxx" + GoogleTaskSyncAdapter.LINE_FEED + "" + GoogleTaskSyncAdapter.LINE_FEED + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataWithoutNote() {
        addTag("test");
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSyncAdapter.LINE_FEED + GoogleTaskSyncAdapter.LINE_FEED  + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }


    @Test
    public void testStoreMetadataTasksHide0() {
        addTag("test");
        task.setHideUntil(0l);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSyncAdapter.LINE_FEED + GoogleTaskSyncAdapter.LINE_FEED  + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataImportanceDefault() {
        addTag("test");
        task.setImportance(DEFAULT_WRITE_IMPORTANCE);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSyncAdapter.LINE_FEED + GoogleTaskSyncAdapter.LINE_FEED  + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataImportanceLow() {
        addTag("test");
        task.setImportance(Task.IMPORTANCE_NONE);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSyncAdapter.LINE_FEED + GoogleTaskSyncAdapter.LINE_FEED  + "{tasks:additionalMetadata{\"importance\":\"LOW\",\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataReminderNone() {
        addTag("test");
        task.setReminderFlags(0);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSyncAdapter.LINE_FEED + GoogleTaskSyncAdapter.LINE_FEED  + "{tasks:additionalMetadata{\"notifyAfterDeadline\":false,\"notifyAtDeadline\":false,\"tags\":[\"test\"]}}", enhancedNotes);
    }

    @Test
    public void testStoreMetadataReminderDefault() {
        addTag("test");
        task.setReminderFlags(DEFAULT_WRITE_REMINDER_FLAGS);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals(GoogleTaskSyncAdapter.LINE_FEED + GoogleTaskSyncAdapter.LINE_FEED  + "{tasks:additionalMetadata{\"tags\":[\"test\"]}}", enhancedNotes);
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
        task.setImportance(Task.IMPORTANCE_DO_OR_DIE);
        adapter.writeNotesIfNecessary(task, remoteModel);
        enhancedNotes = remoteModel.getNotes();
        Assert.assertEquals("xxx" + GoogleTaskSyncAdapter.LINE_FEED + "" + GoogleTaskSyncAdapter.LINE_FEED + "{tasks:additionalMetadata{\"hideUntil\":\"1970-01-13T20:38:31\",\"importance\":\"MUST\",\"notifyModeFive\":true,\"recurrence\":\"RRULE:FREQ\\u003dMONTHLY;INTERVAL\\u003d1\",\"repeatUntil\":\"1970-01-02T10:17:36\",\"tags\":[\"test\",\"test2\",\"test3\"]}}", enhancedNotes);
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
        Assert.assertEquals("xxx" + GoogleTaskSyncAdapter.LINE_FEED + "" + GoogleTaskSyncAdapter.LINE_FEED + "{tasks:additionalMetadata{\"tags\":[\"test\",\"test2\",\"test3\"]}}", enhancedNotes);
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
        task.setImportance(DEFAULT_READ_IMPORTANCE);
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
        Assert.assertEquals(DEFAULT_READ_IMPORTANCE, (int)task.getImportance());
    }

    @Test
    public void testReadMetadataImportanceLow() {
        testStoreMetadataImportanceLow();
        task = new Task();
        task.setImportance(DEFAULT_READ_IMPORTANCE);
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("", task.getNotes());
        Assert.assertTrue(persistedTags.contains("test"));
        Assert.assertEquals(Task.IMPORTANCE_NONE, (int)task.getImportance());
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
        Assert.assertEquals(Task.IMPORTANCE_DO_OR_DIE, (long)task.getImportance());
    }

    @Test
    public void testReadMetadataFromNoteRecurrenceWithoutRepeatUntil() {
        enhancedNotes = "xxx" + GoogleTaskSyncAdapter.LINE_FEED + "" + GoogleTaskSyncAdapter.LINE_FEED +  "{tasks:additionalMetadata{\"recurrence\":\"RRULE:FREQ=MONTHLY;INTERVAL=1\"}}";
        task = new Task();
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("xxx", task.getNotes());
        Assert.assertEquals("RRULE:FREQ=MONTHLY;INTERVAL=1", task.getRecurrence());
        Assert.assertEquals(0, (long)task.getRepeatUntil());
    }

    @Test
    public void testReadMetadataFromNoteCorruptJSON() {
        enhancedNotes = "xxx" + GoogleTaskSyncAdapter.LINE_FEED + "" + GoogleTaskSyncAdapter.LINE_FEED +  "{tasks:additionalMetadata{\"recurrence\":\"}}";
        task = new Task();
        task.setNotes(enhancedNotes);
        adapter.processNotes(task);
        Assert.assertEquals("xxx", task.getNotes());
        Assert.assertEquals("", task.getRecurrence());
        Assert.assertEquals(0, (long)task.getRepeatUntil());
    }

}
