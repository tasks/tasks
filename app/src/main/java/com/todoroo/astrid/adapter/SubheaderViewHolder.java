package com.todoroo.astrid.adapter;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.tasks.LocalBroadcastManager;
import org.tasks.R;
import org.tasks.data.CaldavDao;
import org.tasks.data.GoogleTaskDao;
import org.tasks.filters.NavigationDrawerSubheader;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.SyncPreferences;
import org.tasks.themes.DrawableUtil;

class SubheaderViewHolder extends RecyclerView.ViewHolder {

  private final Preferences preferences;
  private final GoogleTaskDao googleTaskDao;
  private final CaldavDao caldavDao;
  private final LocalBroadcastManager localBroadcastManager;
  @BindView(R.id.text)
  TextView text;

  @BindView(R.id.icon_error)
  ImageView errorIcon;

  private NavigationDrawerSubheader subheader;

  SubheaderViewHolder(
      @NonNull View itemView,
      Activity activity,
      Preferences preferences,
      GoogleTaskDao googleTaskDao,
      CaldavDao caldavDao,
      LocalBroadcastManager localBroadcastManager) {
    super(itemView);
    this.preferences = preferences;
    this.googleTaskDao = googleTaskDao;
    this.caldavDao = caldavDao;
    this.localBroadcastManager = localBroadcastManager;

    ButterKnife.bind(this, itemView);

    errorIcon.setOnClickListener(
        v -> activity.startActivity(new Intent(activity, SyncPreferences.class)));
  }

  @OnClick(R.id.subheader_row)
  public void onClick() {
    boolean collapsed = !subheader.isCollapsed();
    switch (subheader.getSubheaderType()) {
      case PREFERENCE:
        preferences.setBoolean((int) subheader.getId(), collapsed);
        break;
      case GOOGLE_TASKS:
        googleTaskDao.setCollapsed(subheader.getId(), collapsed);
        break;
      case CALDAV:
        caldavDao.setCollapsed(subheader.getId(), collapsed);
        break;
    }
    localBroadcastManager.broadcastRefreshList();
  }

  public void bind(NavigationDrawerSubheader subheader) {
    this.subheader = subheader;
    text.setText(subheader.listingTitle);
    errorIcon.setVisibility(subheader.error ? View.VISIBLE : View.GONE);
    DrawableUtil.setRightDrawable(
        itemView.getContext(),
        text,
        subheader.isCollapsed()
            ? R.drawable.ic_keyboard_arrow_up_black_18dp
            : R.drawable.ic_keyboard_arrow_down_black_18dp);
  }
}
