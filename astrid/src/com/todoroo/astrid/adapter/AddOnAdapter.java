/**
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.astrid.model.AddOn;

/**
 * Adapter for {@link AddOn}s
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class AddOnAdapter extends ArrayAdapter<AddOn> {

    // --- instance variables

    private final Activity activity;
    private final LayoutInflater inflater;
    private final boolean installed;

    public AddOnAdapter(Activity activity, boolean installed, List<AddOn> objects) {
        super(activity, R.id.title, objects);
        this.installed = installed;
        this.activity = activity;
        inflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    // --- view construction

    View.OnClickListener intentClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = (Intent) v.getTag();
            activity.startActivity(intent);
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.addon_adapter_row, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.description = (TextView) convertView.findViewById(R.id.description);
            viewHolder.web = (ImageButton) convertView.findViewById(R.id.button_web);
            viewHolder.market = (ImageButton) convertView.findViewById(R.id.button_market);
            viewHolder.installedIcon = (ImageView) convertView.findViewById(R.id.check);
            convertView.setTag(viewHolder);

            viewHolder.web.setOnClickListener(intentClickListener);
            viewHolder.market.setOnClickListener(intentClickListener);

        }
        ((ViewHolder)convertView.getTag()).item = getItem(position);
        initializeView(convertView);

        return convertView;
    }

    private class ViewHolder {
        public AddOn item;
        public ImageView icon;
        public TextView title;
        public TextView description;
        public ImageButton web;
        public ImageButton market;
        public ImageView installedIcon;
    }

    private void initializeView(View convertView) {
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        AddOn item = viewHolder.item;

        viewHolder.icon.setImageBitmap(item.getIcon());
        viewHolder.title.setText(item.getTitle());
        viewHolder.description.setText(item.getDescription());

        // populate buttons
        if(item.getWebPage() != null) {
            viewHolder.web.setVisibility(View.VISIBLE);
            Intent webPageIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(item.getWebPage()));
            Drawable icon = getIntentIcon(webPageIntent);
            if(icon == null)
                viewHolder.web.setImageResource(
                        android.R.drawable.presence_online);
            else
                viewHolder.web.setImageDrawable(icon);
            viewHolder.web.setTag(webPageIntent);
        } else {
            viewHolder.web.setVisibility(View.GONE);
        }

        if(installed) {
            viewHolder.market.setVisibility(View.GONE);
            viewHolder.installedIcon.setVisibility(View.VISIBLE);
        } else {
            viewHolder.market.setVisibility(View.VISIBLE);
            viewHolder.installedIcon.setVisibility(View.GONE);
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pname:" + //$NON-NLS-1$
                            item.getPackageName()));
            marketIntent.setClassName("com.android.vending", //$NON-NLS-1$
                "com.android.vending.SearchAssetListActivity"); //$NON-NLS-1$
            Drawable icon = getIntentIcon(marketIntent);
            if(icon == null)
                viewHolder.market.setImageResource(
                        android.R.drawable.stat_sys_download);
            else
                viewHolder.market.setImageDrawable(icon);
        }
    }

    public Drawable getIntentIcon(Intent intent) {
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, 0);

        // if options > 1, display open with...
        if(resolveInfoList.size() > 0) {
            return resolveInfoList.get(0).activityInfo.loadIcon(pm);
        }

        return null;
    }

}
