/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
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
package greendroid.widget;

import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;

import com.cyrilmottier.android.greendroid.R;

/**
 * A QuickActionBar displays a set of {@link QuickAction} on a single row. In
 * case too many items are added to the QuickActionBar, the user can
 * horizontally scroll QuickActions. Using a QuickActionBar is a great
 * replacement for the long click UI pattern. For instance,
 * {@link QuickActionBar} adds secondary actions to an item of a
 * {@link ListView}.
 *
 * @author Benjamin Fellous
 * @author Cyril Mottier
 */
public class QuickActionBar extends QuickActionWidget {

    private HorizontalScrollView mScrollView;
    private Animation mRackAnimation;
    private ViewGroup mRack;
    private ViewGroup mQuickActionItems;

    private List<QuickAction> mQuickActions;

    public QuickActionBar(Context context) {
        super(context);

        mRackAnimation = AnimationUtils.loadAnimation(context, R.anim.gd_rack);

        mRackAnimation.setInterpolator(new Interpolator() {
            public float getInterpolation(float t) {
                final float inner = (t * 1.55f) - 1.1f;
                return 1.2f - inner * inner;
            }
        });

        setContentView(R.layout.gd_quick_action_bar);

        final View v = getContentView();
        mRack = (ViewGroup) v.findViewById(R.id.gdi_rack);
        mQuickActionItems = (ViewGroup) v.findViewById(R.id.gdi_quick_action_items);
        mScrollView = (HorizontalScrollView) v.findViewById(R.id.gdi_scroll);
    }

    @Override
    public void show(View anchor) {
        super.show(anchor);
        mScrollView.scrollTo(0, 0);
    }

    @Override
    protected void onMeasureAndLayout(Rect anchorRect, View contentView) {

        contentView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentView.measure(MeasureSpec.makeMeasureSpec(getScreenWidth(), MeasureSpec.EXACTLY),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        int rootHeight = contentView.getMeasuredHeight();

        int offsetY = getArrowOffsetY();
        int dyTop = anchorRect.top;
        int dyBottom = getScreenHeight() - anchorRect.bottom;

        boolean onTop = (dyTop > dyBottom);
        int popupY = (onTop) ? anchorRect.top - rootHeight + offsetY : anchorRect.bottom - offsetY;

        setWidgetSpecs(popupY, onTop);
    }

    @Override
    protected void populateQuickActions(List<QuickAction> quickActions) {

        mQuickActions = quickActions;

        final LayoutInflater inflater = LayoutInflater.from(getContext());

        for (QuickAction action : quickActions) {
            TextView view = (TextView) inflater.inflate(R.layout.gd_quick_action_bar_item, mQuickActionItems, false);
            view.setText(action.mTitle);

            view.setCompoundDrawablesWithIntrinsicBounds(null, action.mDrawable, null, null);
            view.setOnClickListener(mClickHandlerInternal);
            mQuickActionItems.addView(view);
            action.mView = new WeakReference<View>(view);
        }
    }

    @Override
    protected void onClearQuickActions() {
        super.onClearQuickActions();
        mQuickActionItems.removeAllViews();
    }

    private OnClickListener mClickHandlerInternal = new OnClickListener() {

        public void onClick(View view) {

            final OnQuickActionClickListener listener = getOnQuickActionClickListener();

            if (listener != null) {
                final int itemCount = mQuickActions.size();
                for (int i = 0; i < itemCount; i++) {
                    if (view == mQuickActions.get(i).mView.get()) {
                        listener.onQuickActionClicked(QuickActionBar.this, i);
                        break;
                    }
                }
            }

            if (getDismissOnClick()) {
                dismiss();
            }
        }

    };

}
