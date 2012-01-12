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
    private Activity mActivity;
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

    public TaskRabbitMapOverlayItem(Drawable defaultMarker, Activity activity) {
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
                                mActivity.setResult(Activity.RESULT_OK, data);
                                mActivity.finish();
                            }
                        }).setNegativeButton(android.R.string.cancel, null);
        dialog.show();
        return true;
    }


    /*
    @Override
    public boolean onTouchEvent(MotionEvent event, MapView mapView) {
        final int action=event.getAction();
        final int x=(int)event.getX();
        final int y=(int)event.getY();
        boolean result=false;

        if (action==MotionEvent.ACTION_DOWN) {

            for (int i = 0; i < mOverlays.size(); i++) {
                OverlayItem item = mOverlays.get(i);
                Point p= new Point(0,0);

                mapView.getProjection().toPixels(item.getPoint(), p);

                if (hitTest(item, defaultMarker, x-p.x, y-p.y)) {
                    didTap = i;
                    tapPoint = new Point((int) event.getX(), (int) event.getY());

                    result=true;
                    inDrag=item;
                    mOverlays.remove(inDrag);
                    populate();

                    xDragTouchOffset=0;
                    yDragTouchOffset=0;

                    setDragImagePosition(p.x, p.y);
                    //            dragImage.setVisibility(View.VISIBLE);

                    xDragTouchOffset=x-p.x;
                    yDragTouchOffset=y-p.y;


                    break;
                }
            }
        }
        else if (action==MotionEvent.ACTION_MOVE && inDrag!=null) {
            setDragImagePosition(x, y);
            if (tapPoint != null && Math.abs(tapPoint.x - event.getX()) > 10 && Math.abs(tapPoint.x - event.getX()) > 10) {
                didTap = -1;
            }
            result=true;
        }
        else if (action==MotionEvent.ACTION_UP && inDrag!=null) {
            //        dragImage.setVisibility(View.GONE);
            if (didTap != -1) {
                this.onTap(didTap);
                didTap = -1;
                return true;
            }
            GeoPoint pt=mapView.getProjection().fromPixels(x-xDragTouchOffset,
                    y-yDragTouchOffset);
            OverlayItem toDrop=new OverlayItem(pt, inDrag.getTitle(),
                    inDrag.getSnippet());

            mOverlays.add(toDrop);
            populate();

            inDrag=null;
            result=true;
        }

        return(result || super.onTouchEvent(event, mapView));
    }

    private void setDragImagePosition(int x, int y) {
        getDragImage();
        RelativeLayout.LayoutParams lp=
            (RelativeLayout.LayoutParams)dragImage.getLayoutParams();

        if (lp != null) {
            lp.setMargins(x-xDragImageOffset-xDragTouchOffset,
                    y-yDragImageOffset-yDragTouchOffset, 0, 0);
        }
        dragImage.setLayoutParams(lp);
    }
*/


}

