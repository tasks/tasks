/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * Copyright (c) 2009 Tim Su
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.activities;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.timsu.astrid.R;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.utilities.Constants;


/** List all tags and allows a user to see all tasks for a given tag
 *
 * @author Tim Su (timsu@stanfordalumni.org)
 *
 */
public class TagList extends Activity {
    private static final int ACTIVITY_LIST         = 0;
    private static final int ACTIVITY_CREATE       = 1;

    private static final int MENU_SORT_ALPHA_ID    = Menu.FIRST;
    private static final int MENU_SORT_SIZE_ID     = Menu.FIRST + 1;
    private static final int CONTEXT_CREATE_ID     = Menu.FIRST + 10;
    private static final int CONTEXT_DELETE_ID     = Menu.FIRST + 11;
    private static final int CONTEXT_SHOWHIDE_ID   = Menu.FIRST + 12;

    private static final int FLING_THRESHOLD       = 50;

    private TagController controller;
    private TaskController taskController;
    private ListView listView;

    private List<TagModelForView> tagArray;
    private Map<Long, TaskModelForList> taskMap;
    private Map<TagModelForView, Integer> tagToTaskCount;
    private GestureDetector gestureDetector;

    private static SortMode sortMode = SortMode.SIZE;
    private static boolean sortReverse = false;

    /** Called when loading up the activity for the first time */
    private void onLoad() {
        controller = new TagController(this);
        controller.open();
        taskController = new TaskController(this);
        taskController.open();

        listView = (ListView)findViewById(R.id.taglist);

        fillData();
        gestureDetector = new GestureDetector(new TagListGestureDetector());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (gestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.onTouchEvent(ev);
    }

    class TagListGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if(e1.getX() - e2.getX() > FLING_THRESHOLD) {
                setResult(RESULT_CANCELED);
                finish();
                return true;
            }

            return false;
        }
    }

    // --- stuff for sorting

    private enum SortMode {
        ALPHA {
            @Override
            int compareTo(TagList self, TagModelForView arg0, TagModelForView arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }
        },
        SIZE {
            @Override
            int compareTo(TagList self, TagModelForView arg0, TagModelForView arg1) {
                return self.tagToTaskCount.get(arg1) - self.tagToTaskCount.get(arg0);
            }
        };

        abstract int compareTo(TagList self, TagModelForView arg0, TagModelForView arg1);
    };

    private void sortTagArray() {
        // get all tasks
        Cursor taskCursor = taskController.getActiveTaskListCursor();
        startManagingCursor(taskCursor);
        List<TaskModelForList> taskArray =
            taskController.createTaskListFromCursor(taskCursor);
        taskMap = new HashMap<Long, TaskModelForList>();
        for(TaskModelForList task : taskArray) {
            if(task.isHidden())
                continue;
            taskMap.put(task.getTaskIdentifier().getId(), task);
        }

        // get accurate task count for each tag
        tagToTaskCount = new HashMap<TagModelForView, Integer>();
        for(TagModelForView tag : tagArray) {
            int count = 0;
            List<TaskIdentifier> tasks = controller.getTaggedTasks(TagList.this,
                    tag.getTagIdentifier());

            for(TaskIdentifier taskId : tasks)
                if(taskMap.containsKey(taskId.getId()))
                    count++;
            tagToTaskCount.put(tag, count);
        }

        // do sort
        Collections.sort(tagArray, new Comparator<TagModelForView>() {
            @Override
            public int compare(TagModelForView arg0, TagModelForView arg1) {
                return sortMode.compareTo(TagList.this, arg0, arg1);
            }
        });
        if(sortReverse)
            Collections.reverse(tagArray);
    }

    // --- fill data

    /** Fill in the Tag List with our tags */
    private void fillData() {
        Resources r = getResources();

        tagArray = controller.getAllTags(this);

        // perform sort
        sortTagArray();

        // set up the title
        StringBuilder title = new StringBuilder().
            append(r.getString(R.string.tagList_titlePrefix)).
            append(" ").append(r.getQuantityString(R.plurals.Ntags,
                tagArray.size(), tagArray.size()));
        setTitle(title);

        // set up our adapter
        TagListAdapter tagAdapter = new TagListAdapter(this,
                android.R.layout.simple_list_item_1, tagArray);
        listView.setAdapter(tagAdapter);

        // list view listener
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TagModelForView tag = (TagModelForView)view.getTag();

                Intent intent = new Intent(TagList.this, TaskList.class);
                intent.putExtra(TaskList.TAG_TOKEN, tag.
                        getTagIdentifier().getId());
                startActivityForResult(intent, ACTIVITY_LIST);
            }
        });

        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                AdapterContextMenuInfo adapterMenuInfo =
                    (AdapterContextMenuInfo)menuInfo;
                int position = adapterMenuInfo.position;

                menu.add(position, CONTEXT_CREATE_ID, Menu.NONE,
                        R.string.tagList_context_create);
                menu.add(position, CONTEXT_DELETE_ID, Menu.NONE,
                        R.string.tagList_context_delete);

                int showHideLabel = R.string.tagList_context_hideTag;
                if(tagArray.get(position).shouldHideFromMainList())
                    showHideLabel = R.string.tagList_context_showTag;
                menu.add(position, CONTEXT_SHOWHIDE_ID, Menu.NONE,
                        showHideLabel);

                menu.setHeaderTitle(tagArray.get(position).getName());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        switch(resultCode) {
        case Constants.RESULT_GO_HOME:
            finish();
            break;

        default:
            fillData();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(hasFocus && TaskList.shouldCloseInstance) { // user wants to quit
            finish();
        }
    }

    // --- list adapter

    private class TagListAdapter extends ArrayAdapter<TagModelForView> {

        private List<TagModelForView> objects;
        private int resource;
        private LayoutInflater inflater;

        public TagListAdapter(Context context, int resource,
                List<TagModelForView> objects) {
            super(context, resource, objects);

            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.objects = objects;
            this.resource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            view = inflater.inflate(resource, parent, false);
            setupView(view, objects.get(position));

            return view;
        }

        public void setupView(View view, final TagModelForView tag) {
            Resources r = getResources();
            view.setTag(tag);

            final TextView name = ((TextView)view.findViewById(android.R.id.text1));
            name.setText(new StringBuilder(tag.getName()).
                    append(" (").append(tagToTaskCount.get(tag)).append(")"));

            if(tagToTaskCount.get(tag) == 0)
                name.setTextColor(r.getColor(R.color.task_list_done));
        }
    }

    // --- ui control handlers

    private void createTask(TagModelForView tag) {
        Intent intent = new Intent(this, TaskEdit.class);
        intent.putExtra(TaskEdit.TAG_NAME_TOKEN, tag.getName());
        startActivityForResult(intent, ACTIVITY_CREATE);
    }

    private void deleteTag(final TagIdentifier tagId) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_this_tag_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    controller.deleteTag(tagId);
                    fillData();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case MENU_SORT_ALPHA_ID:
            if(sortMode == SortMode.ALPHA)
                sortReverse = !sortReverse;
            else {
                sortMode = SortMode.ALPHA;
                sortReverse = false;
            }
            fillData();
            return true;
        case MENU_SORT_SIZE_ID:
            if(sortMode == SortMode.SIZE)
                sortReverse = !sortReverse;
            else {
                sortMode = SortMode.SIZE;
                sortReverse = false;
            }
            fillData();
            return true;
        case CONTEXT_CREATE_ID:
            TagModelForView tag = tagArray.get(item.getGroupId());
            createTask(tag);
            return true;
        case CONTEXT_DELETE_ID:
            tag = tagArray.get(item.getGroupId());
            deleteTag(tag.getTagIdentifier());
            return true;
        case CONTEXT_SHOWHIDE_ID:
            tag = tagArray.get(item.getGroupId());
            tag.toggleHideFromMainList();
            controller.saveTag(tag);
            fillData();
            return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    // --- creating stuff

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tag_list);

        onLoad();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controller.close();
        taskController.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem item;

        item = menu.add(Menu.NONE, MENU_SORT_ALPHA_ID, Menu.NONE,
                R.string.tagList_menu_sortAlpha);
        item.setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        item.setAlphabeticShortcut('a');

        item = menu.add(Menu.NONE, MENU_SORT_SIZE_ID, Menu.NONE,
                R.string.tagList_menu_sortSize);
        item.setIcon(android.R.drawable.ic_menu_sort_by_size);
        item.setAlphabeticShortcut('s');

        return true;
    }
}