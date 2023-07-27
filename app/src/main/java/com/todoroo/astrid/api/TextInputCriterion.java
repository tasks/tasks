/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */

package com.todoroo.astrid.api;

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
      new Parcelable.Creator<>() {

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
      String name) {
    this.identifier = identifier;
    this.text = title;
    this.sql = sql;
    this.prompt = prompt;
    this.hint = hint;
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
    writeToParcel(dest);
  }

  @Override
  public String toString() {
    return "TextInputCriterion{"
        + "hint='"
        + hint
        + '\''
        + ", prompt='"
        + prompt
        + '\''
        + ", valuesForNewTasks="
        + valuesForNewTasks
        + ", identifier='"
        + identifier
        + '\''
        + ", text='"
        + text
        + '\''
        + ", sql='"
        + sql
        + '\''
        + ", name='"
        + name
        + '\''
        + '}';
  }
}
