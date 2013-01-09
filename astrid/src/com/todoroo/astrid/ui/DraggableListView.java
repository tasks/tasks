/*
 * Copyright (c) 2010 CommonsWare, LLC
 * Portions Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.todoroo.astrid.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.commonsware.cwac.tlv.TouchListView.DragListener;
import com.commonsware.cwac.tlv.TouchListView.DropListener;
import com.commonsware.cwac.tlv.TouchListView.GrabberClickListener;
import com.commonsware.cwac.tlv.TouchListView.SwipeListener;
import com.timsu.astrid.R;
import com.todoroo.astrid.utility.Flags;

public class DraggableListView extends ListView {

    private static final int SWIPE_THRESHOLD = 40;

    private static final int MOVEMENT_THRESHOLD = 30;

    // --- drag status
    private float mTouchStartX, mTouchCurrentX, mTouchStartY, mTouchCurrentY;
    private boolean mDragging = false;

    private int mDragPos;      // which item is being dragged
    private int mFirstDragPos; // where was the dragged item originally
    private Point mDragPoint;    // at what offset inside the item did the user grab it
    private Point mCoordOffset;  // the difference between screen coordinates and coordinates in this view

    // --- drag drawing
	private ImageView mDragView;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWindowParams;
	private int mUpperBound;
	private int mLowerBound;
	private int mHeight;
	private final Rect mTempRect = new Rect();
	private Bitmap mDragBitmap;
	private final int mTouchSlop;
	private int dragndropBackgroundColor = 0x00000000;

	// --- listeners
    private DragListener mDragListener;
    private DropListener mDropListener;
    private SwipeListener mSwipeListener;
    private GrabberClickListener mClickListener;
    private GestureDetector mGestureDetector;

    // --- other instance variables
    private int mItemHeightNormal = -1;
    private Thread dragThread = null;

    // --- constructors

	public DraggableListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

    public DraggableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.TouchListView, 0, 0);

            mItemHeightNormal = a.getDimensionPixelSize(
                    R.styleable.TouchListView_normal_height, 0);
            dragndropBackgroundColor = a.getColor(
                    R.styleable.TouchListView_dragndrop_background, 0x00000000);

            a.recycle();
        }

        setSelector(R.drawable.none);
    }

    public void setItemHightNormal(int itemHeightNormal) {
        this.mItemHeightNormal = itemHeightNormal;
    }

    protected boolean isDraggableRow(@SuppressWarnings("unused") View view) {
        return true;
    }

    /*
     * pointToPosition() doesn't consider invisible views, but we need to, so
     * implement a slightly different version.
     */
    private int myPointToPosition(int x, int y) {
        Rect frame = mTempRect;
        final int count = getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getFirstVisiblePosition() + i;
            }
        }
        return INVALID_POSITION;
    }

    private int getItemForPosition(int y) {
        int adjustedy = y - mDragPoint.y - (mItemHeightNormal / 2);
        int pos = myPointToPosition(0, adjustedy);
        if (pos >= 0) {
            if (pos <= mFirstDragPos) {
                pos += 1;
            }
        } else if (adjustedy < 0) {
            pos = 0;
        }
        return pos;
    }

    private void adjustScrollBounds(int y) {
        if (y >= mHeight / 3) {
            mUpperBound = mHeight / 3;
        }
        if (y <= mHeight * 2 / 3) {
            mLowerBound = mHeight * 2 / 3;
        }
    }

    /*
     * Restore size and visibility for all list items
     */
    private void unExpandViews(boolean deletion) {
        for (int i = 0;; i++) {
            View v = getChildAt(i);
            if (v == null) {
                if (deletion) {
                    // HACK force update of mItemCount
                    int position = getFirstVisiblePosition();
                    int y = getChildAt(0).getTop();
                    setAdapter(getAdapter());
                    setSelectionFromTop(position, y);
                    // end hack
                }
                layoutChildren(); // force children to be recreated where needed
                v = getChildAt(i);
                if (v == null) {
                    break;
                }
            }

            if (isDraggableRow(v)) {
                ViewGroup.LayoutParams params = v.getLayoutParams();
                params.height = LayoutParams.WRAP_CONTENT;
                v.setLayoutParams(params);
                v.setVisibility(View.VISIBLE);
                v.setPadding(0, 0, 0, 0);
            }
        }
    }

    /*
     * Adjust visibility and size to make it appear as though an item is being
     * dragged around and other items are making room for it: If dropping the
     * item would result in it still being in the same place, then make the
     * dragged listitem's size normal, but make the item invisible. Otherwise,
     * if the dragged list item is still on screen, make it as small as possible
     * and expand the item below the insert point. If the dragged item is not on
     * screen, only expand the item below the current insert point.
     */
    private void doExpansion() {
        int childnum = mDragPos - getFirstVisiblePosition();
        if (mDragPos > mFirstDragPos) {
            childnum++;
        }

        View first = getChildAt(mFirstDragPos - getFirstVisiblePosition());

        for (int i = 0;; i++) {
            View vv = getChildAt(i);
            if (vv == null) {
                break;
            }
            int height = LayoutParams.WRAP_CONTENT;
            int marginTop = 0;
            int visibility = View.VISIBLE;
            if (vv.equals(first)) {
                // processing the item that is being dragged
                if (mDragPos == mFirstDragPos) {
                    // hovering over the original location
                    visibility = View.INVISIBLE;
                } else {
                    // not hovering over it
                    height = 1;
                }
            } else if (i == childnum) {
                if (mDragPos < getCount() - 1) {
                    marginTop = mItemHeightNormal;
                    height = 2 * mItemHeightNormal;
                }
            }

            if (isDraggableRow(vv)) {
                ViewGroup.LayoutParams params = vv.getLayoutParams();
                params.height = height;
                vv.setLayoutParams(params);
                vv.setVisibility(visibility);
                vv.setPadding(0, marginTop, 0, 0);
            }
        }
        // Request re-layout since we changed the items layout
        // and not doing this would cause bogus hitbox calculation
        // in myPointToPosition
        layoutChildren();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mGestureDetector != null)
            mGestureDetector.onTouchEvent(ev);

        mTouchCurrentX = ev.getX();
        mTouchCurrentY = ev.getY();

        int action = ev.getAction();
        switch (action) {
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            if(mDragging) {
                stopDragging();
            } else {
                if (dragThread != null && mClickListener != null) {
                    dragThread.interrupt();
                    dragThread = null;
                    if (action == MotionEvent.ACTION_UP)
                        mClickListener.onClick(viewAtPosition());
                }

                else if (mSwipeListener != null &&
                            Math.abs(mTouchCurrentY - mTouchStartY) < MOVEMENT_THRESHOLD) {
                    int dragPos = pointToPosition((int)mTouchCurrentX, (int)mTouchCurrentY);
                    if (mTouchCurrentX > mTouchStartX + SWIPE_THRESHOLD)
                        mSwipeListener.swipeRight(dragPos);
                    else if (mTouchCurrentX < mTouchStartX - SWIPE_THRESHOLD)
                        mSwipeListener.swipeLeft(dragPos);
                }
            }

            if(dragThread != null) {
                dragThread.interrupt();
                dragThread = null;
            }

            break;

        case MotionEvent.ACTION_DOWN:
            dragThread = new Thread(new DragRunnable(ev));
            dragThread.start();
            stopDragging();

            mTouchStartX = ev.getX();
            mTouchStartY = ev.getY();

        case MotionEvent.ACTION_MOVE:
            if(mDragging)
                dragView(ev);

            // detect scrolling
            if(dragThread != null && (Math.abs(mTouchCurrentX - mTouchStartX) > MOVEMENT_THRESHOLD ||
                    Math.abs(mTouchCurrentY - mTouchStartY) > MOVEMENT_THRESHOLD)) {
                dragThread.interrupt();
                dragThread = null;
            }

            break;
        }

        if(mDragging)
            return true;

        return super.onTouchEvent(ev);
    }

    private View viewAtPosition() {
        int itemNum = pointToPosition((int) mTouchCurrentX, (int) mTouchCurrentY);

        if (itemNum == AdapterView.INVALID_POSITION)
            return null;

        return (View) getChildAt(itemNum - getFirstVisiblePosition());
    }

    // --- drag logic

    private class DragRunnable implements Runnable {

        private final MotionEvent ev;

        public DragRunnable(MotionEvent ev) {
            this.ev = ev;
        }

        public void run() {
            try {
                Thread.sleep(300L);

                post(new Runnable() {
                    @Override
                    public void run() {
                        initiateDrag(ev);
                    }
                });

                Thread.sleep(1000L);

                post(new Runnable() {
                    public void run() {
                        stopDragging();
                        dragThread = null;
                        Vibrator v = (Vibrator) getContext().getSystemService(
                                Context.VIBRATOR_SERVICE);
                        v.vibrate(50);
                        mClickListener.onLongClick(viewAtPosition());
                    }
                });

            } catch (InterruptedException e) {
                // bye!
            }
        }
    };

    /**
     * @return true if drag was initiated
     */
    protected boolean initiateDrag(MotionEvent ev) {
        int x = (int) mTouchCurrentX;
        int y = (int) mTouchCurrentY;
        int itemNum = pointToPosition(x, y);

        if (itemNum == AdapterView.INVALID_POSITION)
            return false;

        View item = (View) getChildAt(itemNum - getFirstVisiblePosition());

        if(!isDraggableRow(item))
            return false;

        mDragPoint = new Point(x - item.getLeft(), y - item.getTop());
        mCoordOffset = new Point((int)ev.getRawX() - x, (int)ev.getRawY() - y);

        item.setDrawingCacheEnabled(true);

        // Create a copy of the drawing cache so that it does not get
        // recycled by the framework when the list tries to clean up memory
        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
        item.setDrawingCacheEnabled(false);

        Rect listBounds = new Rect();

        getGlobalVisibleRect(listBounds, null);

        startDragging(bitmap, listBounds.left, y);
        mDragPos = itemNum;
        mFirstDragPos = mDragPos;
        mHeight = getHeight();

        int touchSlop = mTouchSlop;
        mUpperBound = Math.min(y - touchSlop, mHeight / 3);
        mLowerBound = Math.max(y + touchSlop, mHeight * 2 / 3);

        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);

        return true;
    }

    private void startDragging(Bitmap bm, int x, int y) {
        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowParams.x = x;
        mWindowParams.y = y - mDragPoint.y + mCoordOffset.y;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;

        ImageView v = new ImageView(getContext());
        v.setBackgroundColor(dragndropBackgroundColor);
        v.setImageBitmap(bm);
        mDragBitmap = bm;

        mWindowManager = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
        mDragging = true;
        Flags.set(Flags.TLFP_NO_INTERCEPT_TOUCH);
    }

    private void dragView(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        mWindowParams.y = y - mDragPoint.y + mCoordOffset.y;

        if (mDragPos == mFirstDragPos && x > mTouchStartX + SWIPE_THRESHOLD)
            mDragView.setPadding(30, 1, 0, 1);
        else if (mDragPos == mFirstDragPos && x < mTouchStartX - SWIPE_THRESHOLD)
            mDragView.setPadding(-30, 2, 0, 2);
        else
            mDragView.setPadding(0, 0, 0, 0);

        mWindowManager.updateViewLayout(mDragView, mWindowParams);

        int itemnum = getItemForPosition(y);
        if (itemnum >= 0) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN
                    || itemnum != mDragPos) {
                if (mDragListener != null)
                    mDragListener.drag(mDragPos, itemnum);
                mDragPos = itemnum;
                doExpansion();
            }
            int speed = 0;
            adjustScrollBounds(y);
            if (y > mLowerBound) {
                // scroll the list up a bit
                speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
            } else if (y < mUpperBound) {
                // scroll the list down a bit
                speed = y < mUpperBound / 2 ? -16 : -4;
            }
            if (speed != 0) {
                int ref = pointToPosition(0, mHeight / 2);
                if (ref == AdapterView.INVALID_POSITION) {
                    // we hit a divider or an invisible view, check
                    // somewhere else
                    ref = pointToPosition(0, mHeight / 2
                            + getDividerHeight() + 64);
                }
                View v = getChildAt(ref - getFirstVisiblePosition());
                if (v != null) {
                    int pos = v.getTop();
                    setSelectionFromTop(ref, pos - speed);
                }
            }
        }
    }

    private void stopDragging() {
        if (mDragBitmap != null) {
            mDragBitmap.recycle();
            mDragBitmap = null;
        }

        unExpandViews(false);

        if (mDragView != null) {
            WindowManager wm = (WindowManager) getContext().getSystemService(
                    Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }

        if(mDragging) {
            if (mSwipeListener != null && mDragPos == mFirstDragPos) {
                if (mTouchCurrentX > mTouchStartX + SWIPE_THRESHOLD)
                    mSwipeListener.swipeRight(mFirstDragPos);
                else if (mTouchCurrentX < mTouchStartX - SWIPE_THRESHOLD)
                    mSwipeListener.swipeLeft(mFirstDragPos);
            } else if(mDropListener != null && mDragPos != mFirstDragPos &&
                    mDragPos >= 0 && mDragPos < getCount()) {
                if(mFirstDragPos < mDragPos)
                    mDragPos++;
                mDropListener.drop(mFirstDragPos, mDragPos);
            }
        }

        mDragging = false;
        Flags.checkAndClear(Flags.TLFP_NO_INTERCEPT_TOUCH);
    }

    // --- getters and setters

    public void setDragListener(DragListener l) {
        mDragListener = l;
    }

    public void setDropListener(DropListener l) {
        mDropListener = l;
    }

    public void setSwipeListener(SwipeListener l) {
        mSwipeListener = l;
    }

    public void setClickListener(GrabberClickListener listener) {
        this.mClickListener = listener;
    }

    public void setDragndropBackgroundColor(int color) {
        this.dragndropBackgroundColor = color;
    }

    @SuppressWarnings("nls")
    @Override
    final public void addHeaderView(View v, Object data, boolean isSelectable) {
        throw new RuntimeException(
                "Headers are not supported with TouchListView");
    }

    @SuppressWarnings("nls")
    @Override
    final public void addHeaderView(View v) {
        throw new RuntimeException(
                "Headers are not supported with TouchListView");
    }

}
