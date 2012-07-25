/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.TextInputCriterion;
import com.todoroo.astrid.core.CustomFilterActivity.CriterionInstance;
import com.todoroo.astrid.data.AddOn;

/**
 * Adapter for {@link AddOn}s
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class CustomFilterAdapter extends ArrayAdapter<CriterionInstance> {

    private final CustomFilterActivity activity;
    private final LayoutInflater inflater;

    public CustomFilterAdapter(CustomFilterActivity activity, List<CriterionInstance> objects) {
        super(activity, R.id.name, objects);
        this.activity = activity;
        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    // --- view event handling

    View.OnClickListener filterClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ViewHolder viewHolder = (ViewHolder) v.getTag();
            if(viewHolder == null)
                return;
            if(viewHolder.item.type == CriterionInstance.TYPE_UNIVERSE)
                return;

            showOptionsFor(viewHolder.item, new Runnable() {
                @Override
                public void run() {
                    activity.updateList();
                    notifyDataSetInvalidated();
                }
            });
        }
    };

    public void onCreateContextMenu(ContextMenu menu, View v) {
        // view holder
        ViewHolder viewHolder = (ViewHolder) v.getTag();
        if(viewHolder == null || viewHolder.item.type == CriterionInstance.TYPE_UNIVERSE)
            return;

        int index = getPosition(viewHolder.item);

        menu.setHeaderTitle(viewHolder.name.getText());
        if(viewHolder.icon.getVisibility() == View.VISIBLE)
            menu.setHeaderIcon(viewHolder.icon.getDrawable());


        MenuItem item = menu.add(CustomFilterActivity.MENU_GROUP_CONTEXT_TYPE, CriterionInstance.TYPE_INTERSECT, index,
                activity.getString(R.string.CFA_context_chain,
                        activity.getString(R.string.CFA_type_intersect)));
        item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_INTERSECT);
        item = menu.add(CustomFilterActivity.MENU_GROUP_CONTEXT_TYPE, CriterionInstance.TYPE_ADD, index,
                activity.getString(R.string.CFA_context_chain,
                        activity.getString(R.string.CFA_type_add)));
        item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_ADD);

        item = menu.add(CustomFilterActivity.MENU_GROUP_CONTEXT_TYPE, CriterionInstance.TYPE_SUBTRACT, index,
                activity.getString(R.string.CFA_context_chain,
                        activity.getString(R.string.CFA_type_subtract)));
        item.setChecked(viewHolder.item.type == CriterionInstance.TYPE_SUBTRACT);
        menu.setGroupCheckable(CustomFilterActivity.MENU_GROUP_CONTEXT_TYPE, true, true);

        menu.add(CustomFilterActivity.MENU_GROUP_CONTEXT_DELETE, 0, index,
                R.string.CFA_context_delete);
    }

    /**
     * Show options menu for the given criterioninstance
     * @param item
     */
    public void showOptionsFor(final CriterionInstance item, final Runnable onComplete) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity).
            setTitle(item.criterion.name);

        if(item.criterion instanceof MultipleSelectCriterion) {
            MultipleSelectCriterion multiSelectCriterion = (MultipleSelectCriterion) item.criterion;
            final String[] titles = multiSelectCriterion.entryTitles;
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
                    android.R.layout.simple_spinner_dropdown_item, titles);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface click, int which) {
                    item.selectedIndex = which;
                    if(onComplete != null)
                        onComplete.run();
                }
            };
            dialog.setAdapter(adapter, listener);
        } else if(item.criterion instanceof TextInputCriterion) {
            TextInputCriterion textInCriterion = (TextInputCriterion) item.criterion;
            FrameLayout frameLayout = new FrameLayout(activity);
            frameLayout.setPadding(10, 0, 10, 0);
            final EditText editText = new EditText(activity);
            editText.setText(item.selectedText);
            editText.setHint(textInCriterion.hint);
            frameLayout.addView(editText, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.FILL_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT));
            dialog.setMessage(textInCriterion.prompt).setView(frameLayout).
                setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        item.selectedText = editText.getText().toString();
                        if(onComplete != null)
                            onComplete.run();
                    }
                });
        }

        dialog.show().setOwnerActivity(activity);
    }

    // --- view construction

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.custom_filter_row, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.type = (ImageView) convertView.findViewById(R.id.type);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.name= (TextView) convertView.findViewById(R.id.name);
            viewHolder.filterView = (FilterView) convertView.findViewById(R.id.filter);
            convertView.setTag(viewHolder);
        }

        ViewHolder viewHolder = (ViewHolder)convertView.getTag();
        viewHolder.item = getItem(position);
        initializeView(convertView);

        // listeners
        convertView.setOnCreateContextMenuListener(activity);
        convertView.setOnClickListener(filterClickListener);

        return convertView;
    }

    private class ViewHolder {
        public CriterionInstance item;
        public ImageView type;
        public ImageView icon;
        public TextView name;
        public FilterView filterView;
    }

    @SuppressWarnings("nls")
    private void initializeView(View convertView) {
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        CriterionInstance item = viewHolder.item;

        String title = item.getTitleFromCriterion();

        viewHolder.type.setVisibility(item.type == CriterionInstance.TYPE_UNIVERSE ?
                View.GONE : View.VISIBLE);
        switch(item.type) {
        case CriterionInstance.TYPE_ADD:
            viewHolder.type.setImageResource(R.drawable.arrow_join);
            title = activity.getString(R.string.CFA_type_add) + " " + title;
            break;
        case CriterionInstance.TYPE_SUBTRACT:
            viewHolder.type.setImageResource(R.drawable.arrow_branch);
            title = activity.getString(R.string.CFA_type_subtract) + " " + title;
            break;
        case CriterionInstance.TYPE_INTERSECT:
            viewHolder.type.setImageResource(R.drawable.arrow_down);
            break;
        }

        viewHolder.icon.setVisibility(item.criterion.icon == null ? View.GONE :
            View.VISIBLE);
        if(item.criterion.icon != null)
            viewHolder.icon.setImageBitmap(item.criterion.icon);

        viewHolder.name.setText(title);

        viewHolder.filterView.setMax(item.max);
        viewHolder.filterView.setStart(item.start);
        viewHolder.filterView.setEnd(item.end);
    }


}
