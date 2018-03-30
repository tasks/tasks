/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.activity;

import static java.util.Arrays.asList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.injection.ActivityComponent;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.beast.BeastModeRecyclerAdapter;
import org.tasks.ui.MenuColorizer;

public class BeastModePreferences extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  private static final String BEAST_MODE_ORDER_PREF = "beast_mode_order_v3"; // $NON-NLS-1$
  private static final String BEAST_MODE_PREF_ITEM_SEPARATOR = ";";

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;

  @Inject Preferences preferences;
  private BeastModeRecyclerAdapter adapter;

  public static void setDefaultOrder(Preferences preferences, Context context) {
    if (preferences.getStringValue(BEAST_MODE_ORDER_PREF) != null) {
      return;
    }

    ArrayList<String> list = constructOrderedControlList(preferences, context);
    StringBuilder newSetting = new StringBuilder();
    for (String item : list) {
      newSetting.append(item);
      newSetting.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
    }
    preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
  }

  public static ArrayList<String> constructOrderedControlList(
      Preferences preferences, Context context) {
    String order = preferences.getStringValue(BEAST_MODE_ORDER_PREF);
    ArrayList<String> list = new ArrayList<>();
    String[] itemsArray;
    if (order == null) {
      itemsArray = context.getResources().getStringArray(R.array.TEA_control_sets_prefs);
    } else {
      itemsArray = order.split(BEAST_MODE_PREF_ITEM_SEPARATOR);
    }

    Collections.addAll(list, itemsArray);

    if (order == null) {
      return list;
    }

    itemsArray = context.getResources().getStringArray(R.array.TEA_control_sets_prefs);
    for (int i = 0; i < itemsArray.length; i++) {
      if (!list.contains(itemsArray[i])) {
        list.add(i, itemsArray[i]);
      }
    }
    return list;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.beast_mode_pref_activity);
    ButterKnife.bind(this);

    toolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_24dp));
    toolbar.setNavigationOnClickListener(v -> finish());
    toolbar.inflateMenu(R.menu.beast_mode);
    toolbar.setOnMenuItemClickListener(this);
    MenuColorizer.colorToolbar(this, toolbar);

    adapter = new BeastModeRecyclerAdapter(this, constructOrderedControlList(preferences, this));
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    adapter.applyToRecyclerView(recyclerView);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_reset_to_defaults:
        String[] prefsArray = getResources().getStringArray(R.array.TEA_control_sets_prefs);
        adapter.setItems(asList(prefsArray));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void finish() {
    List<String> items = adapter.getItems();
    StringBuilder newSetting = new StringBuilder();
    for (int i = 0; i < items.size(); i++) {
      newSetting.append(items.get(i));
      newSetting.append(BEAST_MODE_PREF_ITEM_SEPARATOR);
    }
    String oldValue = preferences.getStringValue(BEAST_MODE_ORDER_PREF);
    String newValue = newSetting.toString();
    if (Strings.isNullOrEmpty(oldValue) || !oldValue.equals(newValue)) {
      preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
      setResult(RESULT_OK);
    }
    super.finish();
  }
}
