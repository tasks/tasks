package com.todoroo.astrid.taskrabbit;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;
import com.timsu.astrid.R;

public class TaskRabbitMapOverlayItem extends ItemizedOverlay {
    private final ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

    private Drawable defaultMarker;
    private TaskRabbitMapActivity mActivity;
    private OverlayItem inDrag;
    private int xDragImageOffset=0;
    private int yDragImageOffset=0;
    private final int xDragTouchOffset=0;
    private final int yDragTouchOffset=0;
    private ImageView dragImage=null;
    private OverlayItem selectedItem=null;
    private final int didTap = -1;
    private final Point tapPoint = null;
    public TaskRabbitMapOverlayItem(Drawable defaultMarker) {
        super(boundCenterBottom(defaultMarker));
    }

    public TaskRabbitMapOverlayItem(Drawable defaultMarker, TaskRabbitMapActivity activity) {
        super(boundCenterBottom(defaultMarker));
        this.defaultMarker = defaultMarker;
        mActivity = activity;

        getDragImage();
    }

    public ImageView getDragImage() {
        if (dragImage == null) {

            dragImage= new ImageView(mActivity);

            dragImage.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.icon_locale));
            //dragImage.setImageDrawable(R.drawable.gl_alarm);
            dragImage.setLayoutParams(new RelativeLayout.LayoutParams(50, 50));


            xDragImageOffset=dragImage.getDrawable().getIntrinsicWidth()/2;
            yDragImageOffset=dragImage.getDrawable().getIntrinsicHeight();
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
        dialog.setTitle("Set this as your location?");
        dialog.setMessage(selectedItem.getSnippet());
        dialog.setIcon(
                android.R.drawable.ic_dialog_alert).setPositiveButton(
                        android.R.string.ok, new DialogInterface.OnClickListener() {
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

