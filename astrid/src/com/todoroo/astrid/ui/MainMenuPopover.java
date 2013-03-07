/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.service.ThemeService;
import com.todoroo.astrid.ui.TouchInterceptingFrameLayout.InterceptTouchListener;
import com.todoroo.astrid.utility.AstridPreferences;

public class MainMenuPopover extends FragmentPopover implements InterceptTouchListener {

    public static final int MAIN_MENU_ITEM_LISTS = R.string.TLA_menu_lists;
    public static final int MAIN_MENU_ITEM_FRIENDS = R.string.TLA_menu_friends;
    public static final int MAIN_MENU_ITEM_FEATURED_LISTS = R.string.TLA_menu_featured_lists;
    public static final int MAIN_MENU_ITEM_SEARCH = R.string.TLA_menu_search;
    public static final int MAIN_MENU_ITEM_SUGGESTIONS = R.string.TLA_menu_suggestions;
    public static final int MAIN_MENU_ITEM_SETTINGS = R.string.TLA_menu_settings;

    public interface MainMenuListener {
        public boolean shouldAddMenuItem(int itemId);
        public void mainMenuItemSelected(int item, Intent customIntent);
    }

    private MainMenuListener mListener;
    private final LayoutInflater inflater;
    private final LinearLayout content;
    private final LinearLayout topFixed;
    private final LinearLayout bottomFixed;
    private final int rowLayout;
    private boolean suppressNextKeyEvent = false;
    private final boolean isTablet;

    public void setMenuListener(MainMenuListener listener) {
        this.mListener = listener;
    }

    public MainMenuPopover(Context context, int layout, boolean isTablet, MainMenuListener listener) {
        super(context, layout);

        TouchInterceptingFrameLayout rootLayout = (TouchInterceptingFrameLayout) getContentView();
        rootLayout.setInterceptTouchListener(this);
        rootLayout.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                dismiss();
                return false;
            }
        });

        if (AstridPreferences.useTabletLayout(context))
            rowLayout = R.layout.main_menu_row_tablet;
        else
            rowLayout = R.layout.main_menu_row;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        content = (LinearLayout) getContentView().findViewById(R.id.content);

        topFixed = (LinearLayout) getContentView().findViewById(R.id.topFixedItems);
        bottomFixed = (LinearLayout) getContentView().findViewById(R.id.bottomFixedItems);

        this.isTablet = isTablet;

        mListener = listener;

        addFixedItems();
    }

    public boolean didInterceptTouch(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!suppressNextKeyEvent) {
            if ((keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK) && isShowing()) {
                dismiss();
                return true;
            }
        }
        suppressNextKeyEvent = false;
        return false;
    }

    public void suppressNextKeyEvent() {
        suppressNextKeyEvent = true;
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(null);
    }

    private void addFixedItems() {
        int themeFlags = isTablet ? ThemeService.FLAG_FORCE_DARK : 0;
        if (Preferences.getBoolean(R.string.p_show_menu_search, true))
            addMenuItem(R.string.TLA_menu_search,
                    ThemeService.getDrawable(R.drawable.icn_menu_search, themeFlags),
                    MAIN_MENU_ITEM_SEARCH, null, topFixed);

        addMenuItem(R.string.TLA_menu_lists,
                ThemeService.getDrawable(R.drawable.icn_menu_lists, themeFlags),
                MAIN_MENU_ITEM_LISTS, null, topFixed); // Lists item

        if (Preferences.getBoolean(R.string.p_show_friends_view, false) && Preferences.getBoolean(R.string.p_show_menu_friends, true))
            addMenuItem(R.string.TLA_menu_friends,
                    ThemeService.getDrawable(R.drawable.icn_menu_friends, themeFlags),
                    MAIN_MENU_ITEM_FRIENDS, null, topFixed);

        addMenuItem(R.string.TLA_menu_settings,
                ThemeService.getDrawable(R.drawable.icn_menu_settings, themeFlags),
                MAIN_MENU_ITEM_SETTINGS, null, bottomFixed); // Settings item
    }

    public void refreshFixedItems() {
        topFixed.removeAllViews();
        bottomFixed.removeAllViews();
        addFixedItems();
    }

    public void setFixedItemVisibility(int index, int visibility, boolean top) {
        LinearLayout container = top ? topFixed : bottomFixed;
        if (index < 0 || index >= container.getChildCount())
            return;

        container.getChildAt(index).setVisibility(visibility);
    }

    @Override
    protected int getArrowLeftMargin(View arrow) {
        return mRect.centerX() - arrow.getMeasuredWidth() / 2 - (int) (12 * metrics.density);
    }


    // --- Public interface ---
    public void addMenuItem(int title, int imageRes, int id) {
        addMenuItem(title, imageRes, id, null, content);
    }

    public void addMenuItem(int title, int imageRes, Intent customIntent, int id) {
        addMenuItem(title, imageRes, id, customIntent, content);
    }

    public void addMenuItem(CharSequence title, Drawable image, Intent customIntent, int id) {
        addMenuItem(title, image, id, customIntent, content);
    }

    public void addSeparator() {
        inflater.inflate(R.layout.fla_separator, content);
    }

    public void clear() {
        content.removeAllViews();
    }


    // --- Private helpers ---
    private void addMenuItem(int title, int imageRes, int id, Intent customIntent, ViewGroup container) {
        if (mListener != null && !mListener.shouldAddMenuItem(id))
            return;
        View item = setupItemWithParams(title, imageRes);
        addViewWithListener(item, container, id, customIntent);
    }

    private void addMenuItem(CharSequence title, Drawable image, int id, Intent customIntent, ViewGroup container) {
        if (mListener != null && !mListener.shouldAddMenuItem(id))
            return;
        View item = setupItemWithParams(title, image);
        addViewWithListener(item, container, id, customIntent);
    }

    private void addViewWithListener(View view, ViewGroup container, final int id, final Intent customIntent) {
        container.addView(view);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (mListener != null)
                    mListener.mainMenuItemSelected(id, customIntent);
            }
        });
    }

    private View setupItemWithParams(int title, int imageRes) {
        View itemRow = inflater.inflate(rowLayout, null);

        ImageView image = (ImageView) itemRow.findViewById(R.id.icon);
        image.setImageResource(imageRes);

        TextView name = (TextView) itemRow.findViewById(R.id.name);
        name.setText(title);

        return itemRow;
    }

    private View setupItemWithParams(CharSequence title, Drawable imageDrawable) {
        View itemRow = inflater.inflate(rowLayout, null);

        ImageView image = (ImageView) itemRow.findViewById(R.id.icon);
        image.setImageDrawable(imageDrawable);

        TextView name = (TextView) itemRow.findViewById(R.id.name);
        name.setText(title);

        return itemRow;
    }
}
