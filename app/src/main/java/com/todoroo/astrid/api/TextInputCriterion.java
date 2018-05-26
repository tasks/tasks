/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * <p>See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.api;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * CustomFilterCriteria allow users to build a custom filter by chaining together criteria
 *
 * @author Tim Su <tim@todoroo.com>
 */
public class TextInputCriterion extends CustomFilterCriterion implements Parcelable {

  /** Parcelable Creator Object */
  public static final Parcelable.Creator<TextInputCriterion> CREATOR =
      new Parcelable.Creator<TextInputCriterion>() {

        /** {@inheritDoc} */
        @Override
        public TextInputCriterion createFromParcel(Parcel source) {
          TextInputCriterion item = new TextInputCriterion();
          item.prompt = source.readString();
          item.hint = source.readString();
          item.readFromParcel(source);
          return item;
        }

        /** {@inheritDoc} */
        @Override
        public TextInputCriterion[] newArray(int size) {
          return new TextInputCriterion[size];
        }
      };
  /** Text area hint */
  public String hint;
  /** Text area prompt */
  private String prompt;

  /** Create a new CustomFilterCriteria object */
  public TextInputCriterion(
      String identifier,
      String title,
      String sql,
      String prompt,
      String hint,
      Bitmap icon,
      String name) {
    this.identifier = identifier;
    this.text = title;
    this.sql = sql;
    this.prompt = prompt;
    this.hint = hint;
    this.icon = icon;
    this.name = name;
  }

  // --- parcelable

  private TextInputCriterion() {
    // constructor for inflating from parceling
  }

  /** {@inheritDoc} */
  @Override
  public int describeContents() {
    return 0;
  }

  /** {@inheritDoc} */
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(prompt);
    dest.writeString(hint);
    super.writeToParcel(dest);
  }
}
