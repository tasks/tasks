/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.core;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.core.CustomFilterActivity.CriterionInstance;
import com.todoroo.astrid.data.AddOn;

/**
 * Adapter for {@link AddOn}s
 *
 * @author Tim Su <tim@todoroo.com>
 *
  */
public class CustomFilterAdapter extends ArrayAdapter<CriterionInstance> {

    private final Activity activity;
    private final LayoutInflater inflater;

    public CustomFilterAdapter(Activity activity, List<CriterionInstance> objects) {
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

            // keep the filter options in the name context menu
            ((CustomFilterActivity)activity).menuItemInstance = viewHolder.item;
            ((CustomFilterActivity)activity).getListView().showContextMenu();
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

        String entryTitle = "";
        if(item.selectedIndex >= 0 && item.criterion.entryTitles != null &&
                item.selectedIndex < item.criterion.entryTitles.length) {
            entryTitle = item.criterion.entryTitles[item.selectedIndex];
        }
        String title = item.criterion.text.replace("?", entryTitle);

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
