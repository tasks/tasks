package com.todoroo.astrid.taskrabbit;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;

public class TaskRabbitMapOverlayItem extends ItemizedOverlay<OverlayItem> {
    private final ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

    private TaskRabbitMapActivity mActivity;
    private ImageView dragImage = null;
    private OverlayItem selectedItem = null;

    public TaskRabbitMapOverlayItem(Drawable defaultMarker) {
        super(boundCenterBottom(defaultMarker));
    }

    public TaskRabbitMapOverlayItem(Drawable defaultMarker, TaskRabbitMapActivity activity) {
        super(boundCenterBottom(defaultMarker));
        mActivity = activity;

        getDragImage();
    }

    public ImageView getDragImage() {
        if (dragImage == null) {

            dragImage= new ImageView(mActivity);

            dragImage.setImageDrawable(mActivity.getResources().getDrawable(
                    android.R.drawable.star_big_on));
            dragImage.setLayoutParams(new RelativeLayout.LayoutParams(50, 50));
        }
        return dragImage;
    }

    public void addOverlay(OverlayItem overlay) {
        mOverlays.add(overlay);
        populate();
    }

    @Override
    protected OverlayItem createItem(int i) {
        return mOverlays.get(i);
    }

    @Override
    public int size() {
        return mOverlays.size();
    }
    @Override
    protected boolean onTap(int index) {
        if (index >= mOverlays.size()) {
            return false;
        }
        selectedItem = mOverlays.get(index);
        AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
        dialog.setTitle(mActivity.getString(R.string.tr_alert_location_clicked_title));
        dialog.setMessage(selectedItem.getSnippet());
        dialog.setIcon(
                android.R.drawable.ic_dialog_alert).setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
                            @SuppressWarnings("nls")
                            public void onClick(DialogInterface dialog, int which) {
                                Intent data = new Intent();
                                data.putExtra("lat",selectedItem.getPoint().getLatitudeE6());
                                data.putExtra("lng",selectedItem.getPoint().getLongitudeE6());
                                data.putExtra("name",mActivity.getSearchText());
                                mActivity.setResult(Activity.RESULT_OK, data);
                                mActivity.finish();
                            }
                        }).setNegativeButton(android.R.string.cancel, null);
        dialog.show();
        mActivity.setSearchTextForCurrentAddress();

        return true;
    }

}

