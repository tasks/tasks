package com.todoroo.astrid.gtasks;

import android.view.View;
import android.widget.RemoteViews;

import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.api.TaskDecoration;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.core.PluginServices;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.test.DatabaseTestCase;

public class GtasksDecorationExposerTest extends DatabaseTestCase {

    private GtasksTestPreferenceService preferences = new GtasksTestPreferenceService();
    private TaskDecoration result;

    public void testExposeNotLoggedIn() {
        givenLoggedInStatus(false);

        whenRequestingDecoration(gtasksFilter(), indentedTask(1));

        thenExpectNoDecoration();
    }

    public void testExposeLoggedInButNormalFilter() {
        givenLoggedInStatus(true);

        whenRequestingDecoration(nonGtasksFilter(), indentedTask(1));

        thenExpectNoDecoration();
    }

    public void testExposeIndentation() {
        givenLoggedInStatus(true);

        whenRequestingDecoration(gtasksFilter(), indentedTask(1));

        thenExpectDecoration(1);
    }

    public void testExposeIndentationWithTopLevelTask() {
        givenLoggedInStatus(true);

        whenRequestingDecoration(gtasksFilter(), nonIndentedTask());

        thenExpectNoDecoration();
    }

    public void testMoreIndentationIsWider() {
        givenLoggedInStatus(true);

        whenRequestingDecoration(gtasksFilter(), indentedTask(2));

        thenExpectWiderThan(indentedTask(1));
    }

    // --- helpers

    private void thenExpectWiderThan(Task otherTask) {
        assertNotNull(result);
        RemoteViews view = result.decoration;
        View inflated = view.apply(getContext(), null);
        inflated.measure(100, 100);
        int width = inflated.getMeasuredWidth();

        result = new GtasksDecorationExposer().expose(otherTask);
        View otherInflated = result.decoration.apply(getContext(), null);
        otherInflated.measure(100, 100);
        int otherWidth = otherInflated.getMeasuredWidth();
        assertTrue(width + " > " + otherWidth, width > otherWidth);
    }

    private Task nonIndentedTask() {
        Task task = new Task();
        PluginServices.getTaskService().save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        PluginServices.getMetadataService().save(metadata);
        return task;
    }

    private void thenExpectDecoration(int minWidth) {
        assertNotNull(result);
        RemoteViews view = result.decoration;
        View inflated = view.apply(getContext(), null);
        inflated.measure(100, 100);
        assertTrue("actual: " + inflated.getMeasuredWidth(), inflated.getMeasuredWidth() > minWidth);
    }

    private Filter gtasksFilter() {
        StoreObject list = new StoreObject();
        list.setValue(GtasksList.REMOTE_ID, "1");
        list.setValue(GtasksList.NAME, "lamo");
        return GtasksFilterExposer.filterFromList(list);
    }

    private Task indentedTask(int indentation) {
        Task task = new Task();
        PluginServices.getTaskService().save(task);
        Metadata metadata = GtasksMetadata.createEmptyMetadata(task.getId());
        metadata.setValue(GtasksMetadata.INDENT, indentation);
        PluginServices.getMetadataService().save(metadata);
        return task;
    }

    private Filter nonGtasksFilter() {
        return CoreFilterExposer.buildInboxFilter(getContext().getResources());
    }

    @Override
    protected void addInjectables() {
        super.addInjectables();
        testInjector.addInjectable("gtasksPreferenceService", preferences);
    }

    private void thenExpectNoDecoration() {
        assertNull(result);
    }

    private void whenRequestingDecoration(Filter filter, Task task) {
        result = new GtasksDecorationExposer().expose(task);
    }

    private void givenLoggedInStatus(boolean status) {
        preferences.setLoggedIn(status);
    }

}
