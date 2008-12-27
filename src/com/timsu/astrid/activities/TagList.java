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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.timsu.astrid.data.task.TaskIdentifier;


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

    private TagController controller;
    private ListView listView;

    private List<TagModelForView> tagArray;

    /** Called when loading up the activity for the first time */
    private void onLoad() {
        controller = new TagController(this);
        controller.open();

        listView = (ListView)findViewById(R.id.taglist);

        fillData();
    }

    /** Fill in the Tag List with our tags */
    private void fillData() {
        Resources r = getResources();

        tagArray = controller.getAllTags(this);

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

                menu.setHeaderTitle(tagArray.get(position).getName());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        fillData();
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
            // set up basic properties
            view.setTag(tag);

            List<TaskIdentifier> tasks = controller.getTaggedTasks(TagList.this,
                    tag.getTagIdentifier());

            final TextView name = ((TextView)view.findViewById(android.R.id.text1));
            name.setText(new StringBuilder(tag.getName()).
                    append(" (").append(tasks.size()).append(")"));
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
        case CONTEXT_CREATE_ID:
            TagModelForView tag = tagArray.get(item.getGroupId());
            createTask(tag);
            return true;
        case CONTEXT_DELETE_ID:
            tag = tagArray.get(item.getGroupId());
            deleteTag(tag.getTagIdentifier());
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