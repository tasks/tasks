/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.activity;

import static org.tasks.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.tasks.R;
import org.tasks.databinding.BeastModePrefActivityBinding;
import org.tasks.injection.ThemedInjectingAppCompatActivity;
import org.tasks.preferences.Preferences;
import org.tasks.preferences.beast.BeastModeRecyclerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BeastModePreferences extends ThemedInjectingAppCompatActivity
    implements Toolbar.OnMenuItemClickListener {

  private static final String BEAST_MODE_ORDER_PREF = "beast_mode_order_v6"; // $NON-NLS-1$
  private static final String BEAST_MODE_PREF_ITEM_SEPARATOR = ";";

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

    BeastModePrefActivityBinding binding = BeastModePrefActivityBinding.inflate(getLayoutInflater());
    Toolbar toolbar = binding.toolbar.toolbar;
    RecyclerView recyclerView = binding.recyclerView;
    setContentView(binding.getRoot());

    toolbar.setNavigationIcon(
        getDrawable(R.drawable.ic_outline_arrow_back_24px));
    toolbar.setNavigationOnClickListener(v -> finish());
    toolbar.inflateMenu(R.menu.beast_mode);
    toolbar.setOnMenuItemClickListener(this);
    themeColor.apply(toolbar);

    adapter = new BeastModeRecyclerAdapter(this, constructOrderedControlList(preferences, this));
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    adapter.applyToRecyclerView(recyclerView);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    if (item.getItemId() == R.id.menu_reset_to_defaults) {
      String[] prefsArray = getResources().getStringArray(R.array.TEA_control_sets_prefs);
      adapter.setItems(asList(prefsArray));
      return true;
    }
    return onOptionsItemSelected(item);
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
    if (isNullOrEmpty(oldValue) || !oldValue.equals(newValue)) {
      preferences.setString(BEAST_MODE_ORDER_PREF, newSetting.toString());
      setResult(RESULT_OK);
    }
    super.finish();
  }
}
