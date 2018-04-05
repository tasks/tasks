/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.ui;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import org.tasks.locale.Locale;
import timber.log.Timber;

public class MultilineListPreference extends ListPreference {

  public MultilineListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onBindView(View view) {
    try {
      super.onBindView(view);
    } catch (Exception e) {
      // happens on 4.0 emulators
      Timber.e(e);
    }
    MultilineHelper.makeMultiline(view);
  }

  @Override
  protected void showDialog(Bundle state) {
    super.showDialog(state);
    Locale.getInstance(getContext()).applyDirectionality(getDialog());
  }

  @Override
  public void setValue(String value) {
    super.setValue(value);
    setSummary(getEntry());
  }
}
