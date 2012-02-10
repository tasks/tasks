package com.todoroo.astrid.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.timsu.astrid.R;

public class MainMenuPopover extends FragmentPopover {

    public static final int MAIN_MENU_ITEM_TASKS = 1;
    public static final int MAIN_MENU_ITEM_FRIENDS = 2;
    public static final int MAIN_MENU_ITEM_SUGGESTIONS = 3;
    public static final int MAIN_MENU_ITEM_TUTORIAL = 4;
    public static final int MAIN_MENU_ITEM_SETTINGS = 5;
    public static final int MAIN_MENU_ITEM_SUPPORT = 6;

    public interface MainMenuListener {
        public void mainMenuItemSelected(int item);
    }

    private MainMenuListener mListener;
    private final LayoutInflater inflater;
    private final LinearLayout content;

    public void setMenuListener(MainMenuListener listener) {
        this.mListener = listener;
    }

    public MainMenuPopover(Context context, int layout, boolean isTablet) {
        super(context, layout);

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        content = (LinearLayout) getContentView().findViewById(R.id.content);
        if (!isTablet)
            addListsItem();
        addFriendsItem();
        addSuggestionsItem();
        addTutorialItem();

        addSeparator();

        addSettingsItem();
        addSupportItem();
    }

    @Override
    protected int getArrowLeftMargin(View arrow) {
        return mRect.centerX() - arrow.getMeasuredWidth() / 2 - (int) (12 * metrics.density);
    }

    private void addMenuItem(int title, int imageRes, final int menuItemOption) {
        View item = setupItemWithParams(title, imageRes);
        content.addView(item);
        item.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (mListener != null)
                    mListener.mainMenuItemSelected(menuItemOption);
            }
        });
    }

    private void addSeparator() {
        inflater.inflate(R.layout.fla_separator, content);
    }

    private void addListsItem() {
        addMenuItem(R.string.TLA_menu_lists, R.drawable.icn_menu_tasks, MAIN_MENU_ITEM_TASKS);
    }

    private void addFriendsItem() {
        //addMenuItem(R.string.TLA_menu_friends, R.drawable.icn_friends, MAIN_MENU_ITEM_FRIENDS);
    }

    private void addSuggestionsItem() {
        //addMenuItem(R.string.TLA_menu_suggestions, R.drawable.icn_featured_lists, MAIN_MENU_ITEM_SUGGESTIONS);
    }

    private void addTutorialItem() {
        addMenuItem(R.string.TLA_menu_tutorial, R.drawable.icn_tutorial, MAIN_MENU_ITEM_TUTORIAL);
    }

    private void addSettingsItem() {
        addMenuItem(R.string.TLA_menu_settings, R.drawable.icn_settings, MAIN_MENU_ITEM_SETTINGS);
    }

    private void addSupportItem() {
        addMenuItem(R.string.TLA_menu_support, R.drawable.icn_support, MAIN_MENU_ITEM_SUPPORT);
    }

    private View setupItemWithParams(int title, int imageRes) {
        View itemRow = inflater.inflate(R.layout.main_menu_row, null);

        ImageView image = (ImageView) itemRow.findViewById(R.id.icon);
        image.setImageResource(imageRes);

        TextView name = (TextView) itemRow.findViewById(R.id.name);
        name.setText(title);

        return itemRow;
    }
}
