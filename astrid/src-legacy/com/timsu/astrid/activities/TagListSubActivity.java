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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.StaleDataException;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.flurry.android.FlurryAgent;
import com.timsu.astrid.R;
import com.timsu.astrid.data.tag.TagIdentifier;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskIdentifier;
import com.timsu.astrid.utilities.DialogUtilities;
import com.timsu.astrid.utilities.Preferences;
import com.todoroo.astrid.activity.TaskEditActivity;


/**
 * List all tags and allows a user to see all tasks for a given tag
 *
 * @author timsu
 *
 */
public class TagListSubActivity extends SubActivity {
    private static final int ACTIVITY_CREATE       = 0;

    private static final int MENU_SORT_ALPHA_ID    = Menu.FIRST;
    private static final int MENU_SORT_SIZE_ID     = Menu.FIRST + 1;
    private static final int CONTEXT_CREATE_ID     = Menu.FIRST + 10;
    private static final int CONTEXT_DELETE_ID     = Menu.FIRST + 11;
    private static final int CONTEXT_SHOWHIDE_ID   = Menu.FIRST + 12;
    private static final int CONTEXT_SHORTCUT_ID   = Menu.FIRST + 13;

    protected ListView listView;
    protected LinkedList<TagModelForView> tagArray;
    HashMap<TagModelForView, Integer> tagToTaskCount;
    protected Handler handler;
    protected TextView loadingText;
    protected boolean untaggedTagDisplayed;

    protected static SortMode sortMode = SortMode.SIZE;
    protected static boolean sortReverse = false;

    public TagListSubActivity(TaskList parent, int code, View view) {
    	super(parent, code, view);
    }

    @Override
    public void onDisplay(Bundle variables) {
        listView = (ListView)findViewById(R.id.taglist);
        handler = new Handler();
        loadingText = (TextView)findViewById(R.id.loading);

        // time to go!
        new Thread(new Runnable() {
            public void run() {
                loadTagListSort();
                fillData();
            }
        }).start();

        FlurryAgent.onEvent("view-tags"); //$NON-NLS-1$
    }

    // --- stuff for sorting

    private enum SortMode {
        ALPHA {
            @Override
            int compareTo(TagListSubActivity self, TagModelForView arg0, TagModelForView arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }
        },
        SIZE {
            @Override
            int compareTo(TagListSubActivity self, TagModelForView arg0, TagModelForView arg1) {
                synchronized(self) {
                    return self.tagToTaskCount.get(arg1) - self.tagToTaskCount.get(arg0);
                }
            }
        };

        abstract int compareTo(TagListSubActivity self, TagModelForView arg0, TagModelForView arg1);
    };

    /** Counts how many tasks appear in active task list */
    public static int countActiveTasks(HashSet<TaskIdentifier> activeTasks, LinkedList<TaskIdentifier> tasks) {
        int count = 0;
        if(tasks != null) {
            for(TaskIdentifier task : tasks)
                if(activeTasks.contains(task))
                    count++;
        }
        return count;
    }

    private synchronized void sortTagArray() {
        // get all tasks
        HashSet<TaskIdentifier> activeTasks =
            getTaskController().getActiveTaskIdentifiers();
        if(activeTasks == null)
        	activeTasks = new HashSet<TaskIdentifier>();

        // get task count for each tag
        tagToTaskCount = new HashMap<TagModelForView, Integer>();

        for(TagModelForView tag : tagArray) {
        	LinkedList<TaskIdentifier> tasks;
			tasks = getTagController().getTaggedTasks(tag.getTagIdentifier());
			int count = countActiveTasks(activeTasks, tasks);
        	tagToTaskCount.put(tag, count);
        }

        // do sort
        Collections.sort(tagArray, new Comparator<TagModelForView>() {
            public int compare(TagModelForView arg0, TagModelForView arg1) {
                return sortMode.compareTo(TagListSubActivity.this, arg0, arg1);
            }
        });

        // show "untagged" as a category at the top, in the proper language/localization
        String untaggedLabel = getResources().getString(R.string.tagList_untagged);
        TagModelForView untaggedModel = TagModelForView.getUntaggedModel(untaggedLabel);
        int count = countActiveTasks(activeTasks, getTagController().getUntaggedTasks());
        if(count > 0) {
        	untaggedTagDisplayed = true;
	        tagArray.addFirst(untaggedModel);
	        tagToTaskCount.put(untaggedModel, count);
        }

        if(sortReverse)
            Collections.reverse(tagArray);
    }

    /** Save the sorting mode to the preferences */
    private void saveTagListSort() {
        int sortId = sortMode.ordinal() + 1;

        if (sortReverse)
            sortId *= -1;

        Preferences.setTagListSort(getParent(), sortId);
    }

    /** Save the sorting mode to the preferences */
    protected void loadTagListSort() {
        try {
            int sortId = Preferences.getTagListSort(getParent());
            if (sortId == 0)
                return;
            sortReverse = sortId < 0;
            sortId = Math.abs(sortId) - 1;

            sortMode = SortMode.values()[sortId];
        } catch (Exception e) {
            // do nothing
        }
    }


    // --- fill data

    /** Fill in the Tag List with our tags */
    protected synchronized void fillData() {
        try {
            tagArray = getTagController().getAllTags();

            sortTagArray();  // count and sort each tag
        } catch (StaleDataException e) {
            // happens when you rotate the screen while the thread is
            // still running. i don't think it's avoidable?
            Log.w("astrid", "StaleDataException", e); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        } catch (Exception e) {
            Log.e("astrid", "Error loading list", e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        handler.post(new Runnable() {
            public void run() {
                synchronized(TagListSubActivity.this) {
                    // set up our adapter
                    final TagListAdapter tagAdapter = new TagListAdapter(getParent(),
                            android.R.layout.simple_list_item_1, tagArray,
                            tagToTaskCount);
                    // set up ui components
                    setUpListUI(tagAdapter);
                }
                loadingText.setVisibility(View.GONE);
            }
        });
    }

    /** Set up list handlers and adapter. run on the UI thread */
    protected void setUpListUI(ListAdapter adapter) {
        // set up the title
        Resources r = getResources();
        int tags = tagArray.size();
        if(untaggedTagDisplayed && tags > 0)
        	tags--;
        StringBuilder title = new StringBuilder().
            append(r.getString(R.string.tagList_titlePrefix)).
            append(" ").append(r.getQuantityString(R.plurals.Ntags,
                tags, tags));
        final CharSequence finalTitle = title;
        handler.post(new Runnable() {
            public void run() {
                setTitle(finalTitle);
            }
        });

        listView.setAdapter(adapter);

        // list view listener
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                TagModelForView tag = (TagModelForView)view.getTag();

                Bundle bundle = new Bundle();
                bundle.putLong(TaskListSubActivity.TAG_TOKEN, tag.getTagIdentifier().getId());
                switchToActivity(TaskList.AC_TASK_LIST_W_TAG, bundle);
            }
        });

        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
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

                menu.add(position, CONTEXT_SHORTCUT_ID, Menu.NONE,
                        R.string.tagList_context_shortcut);

                menu.setHeaderTitle(tagArray.get(position).getName());
            }
        });


        listView.setOnTouchListener(getGestureListener());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        fillData();
    }

    // --- list adapter

    private void createTask(TagModelForView tag) {
        Intent intent = new Intent(getParent(), TaskEditActivity.class);
        launchActivity(intent, ACTIVITY_CREATE);
    }

    private void deleteTag(final TagIdentifier tagId) {
        new AlertDialog.Builder(getParent())
            .setTitle(R.string.delete_title)
            .setMessage(R.string.delete_this_tag_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    getTagController().deleteTag(tagId);
                    fillData();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    @Override
    /** Handle back button by moving to task list */
    protected boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK) {
    		switchToActivity(TaskList.AC_TASK_LIST, null);
    		return true;
    	}

    	return false;
    }

    @Override
    /** Picked item in the options list */
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case MENU_SORT_ALPHA_ID:
            if(sortMode == SortMode.ALPHA)
                sortReverse = !sortReverse;
            else {
                sortMode = SortMode.ALPHA;
                sortReverse = false;
            }
            saveTagListSort();
            fillData();
            return true;
        case MENU_SORT_SIZE_ID:
            if(sortMode == SortMode.SIZE)
                sortReverse = !sortReverse;
            else {
                sortMode = SortMode.SIZE;
                sortReverse = false;
            }
            saveTagListSort();
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
            try {
                getTagController().saveTag(tag);
            } catch (Exception e) {
                DialogUtilities.okDialog(getParent(), "Error: You probably " +
                        "already have a tag named '" + tag.getName() + "'!",
                        null);
            }
            fillData();
            return true;
        case CONTEXT_SHORTCUT_ID:
            tag = tagArray.get(item.getGroupId());
            Resources r = getResources();
            Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
            shortcutIntent.setComponent(new ComponentName(
                    getParent().getApplicationContext(), TagView.class));
            shortcutIntent.setData(Uri.parse("tag:" + tag.getTagIdentifier().getId())); //$NON-NLS-1$

            Intent createShortcutIntent = new Intent();
            createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            String label = tag.getName();
            if(tag.shouldHideFromMainList())
                label = label.substring(1);

            // add the @ sign if the task starts with a letter, for clarity
            if(Character.isLetterOrDigit(label.charAt(0)))
                label = "@" + label; //$NON-NLS-1$

            createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            createShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON,
                    ((BitmapDrawable)r.getDrawable(R.drawable.icon_tag)).getBitmap());
            createShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT"); //$NON-NLS-1$

            getParent().sendBroadcast(createShortcutIntent);
            Toast.makeText(getParent(), R.string.tagList_shortcut_created, Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    // --- creating stuff

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
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

    // --------------------------------------------------- tag list adapter

    class TagListAdapter extends ArrayAdapter<TagModelForView> {

    	private final List<TagModelForView> objects;
        private final int resource;
        private final LayoutInflater inflater;
        private HashMap<TagModelForView, Integer> tagCount;

        public TagListAdapter(Context context, int resource,
                List<TagModelForView> objects, HashMap<TagModelForView, Integer>
                tagToTaskCount) {
            super(context, resource, objects);

            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.objects = objects;
            this.resource = resource;
            this.tagCount = tagToTaskCount;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	View view = convertView;

            if(view == null) {
                view = inflater.inflate(resource, parent, false);
            }
            setupView(view, objects.get(position), false);

            return view;
        }

        public void setupView(View view, final TagModelForView tag, boolean retry) {
            Resources r = getResources();
            view.setTag(tag);

            final TextView name = ((TextView)view.findViewById(android.R.id.text1));
            try {
                if(tagCount == null)
                    tagCount = tagToTaskCount;

                name.setText(new StringBuilder(tag.getName()).
                        append(" (").append(tagCount.get(tag)).append(")"));

                if(tagCount == null || tagCount.get(tag) == null || tagCount.get(tag) == 0)
                    name.setTextColor(r.getColor(R.color.task_list_done));
                else
                	name.setTextColor(r.getColor(android.R.color.white));
            } catch (Exception e) {
                Log.e("astrid", "Error loading tag list", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

}