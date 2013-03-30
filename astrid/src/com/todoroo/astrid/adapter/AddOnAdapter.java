/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.astrid.data.AddOn;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.utility.Constants;

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
            ButtonTag buttonTag = (ButtonTag) v.getTag();
            if(buttonTag != null) {
                try {
                    activity.startActivity(buttonTag.intent);
                    StatisticsService.reportEvent("addon-" + buttonTag.event); //$NON-NLS-1$
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(activity, R.string.market_unavailable, Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.addon_adapter_row, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.free = (TextView) convertView.findViewById(R.id.free);
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.description = (TextView) convertView.findViewById(R.id.description);
            viewHolder.market = (ImageButton) convertView.findViewById(R.id.button_market);
            viewHolder.installedIcon = (ImageView) convertView.findViewById(R.id.check);
            convertView.setTag(viewHolder);

            viewHolder.market.setOnClickListener(intentClickListener);

        }
        ((ViewHolder)convertView.getTag()).item = getItem(position);
        initializeView(convertView);

        return convertView;
    }

    private class ViewHolder {
        public AddOn item;
        public ImageView icon;
        public TextView free;
        public TextView title;
        public TextView description;
        public ImageButton market;
        public ImageView installedIcon;
    }

    private class ButtonTag {
        String event;
        Intent intent;
        public ButtonTag(String message, Intent intent) {
            this.event = message;
            this.intent = intent;
        }
    }

    private void initializeView(View convertView) {
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        AddOn item = viewHolder.item;

        viewHolder.icon.setImageBitmap(item.getIcon());
        viewHolder.title.setText(item.getTitle());
        viewHolder.description.setText(item.getDescription());
        viewHolder.free.setVisibility(item.isFree() && !installed ? View.VISIBLE : View.GONE);

        // populate buttons

        if(installed) {
            viewHolder.market.setVisibility(View.GONE);
            viewHolder.installedIcon.setVisibility(View.VISIBLE);
        } else {
            viewHolder.market.setVisibility(View.VISIBLE);
            viewHolder.installedIcon.setVisibility(View.GONE);
            Intent marketIntent = Constants.MARKET_STRATEGY.generateMarketLink(item.getPackageName());
            if (marketIntent == null) {
                convertView.setVisibility(View.GONE);
            } else {
                convertView.setVisibility(View.VISIBLE);
                viewHolder.market.setTag(new ButtonTag("market-" + item.getPackageName(), //$NON-NLS-1$
                        marketIntent));
                Drawable icon = getIntentIcon(marketIntent);
                if(icon == null)
                    viewHolder.market.setImageResource(
                            android.R.drawable.stat_sys_download);
                else
                    viewHolder.market.setImageDrawable(icon);
            }
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
