package com.todoroo.astrid.core;

import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.TextInputCriterion;

public class CriterionInstance {

  static final int TYPE_ADD = 0;
  static final int TYPE_SUBTRACT = 1;
  static final int TYPE_INTERSECT = 2;
  static final int TYPE_UNIVERSE = 3;

  /** criteria for this instance */
  public CustomFilterCriterion criterion;

  /** which of the entries is selected (MultipleSelect) */
  int selectedIndex = -1;

  /** text of selection (TextInput) */
  String selectedText = null;

  /** type of join */
  public int type = TYPE_INTERSECT;

  public int end;
  /** statistics for filter count */
  int start;

  int max;

  String getTitleFromCriterion() {
    if (criterion instanceof MultipleSelectCriterion) {
      if (selectedIndex >= 0
          && ((MultipleSelectCriterion) criterion).entryTitles != null
          && selectedIndex < ((MultipleSelectCriterion) criterion).entryTitles.length) {
        String title = ((MultipleSelectCriterion) criterion).entryTitles[selectedIndex];
        return criterion.text.replace("?", title);
      }
      return criterion.text;
    } else if (criterion instanceof TextInputCriterion) {
      if (selectedText == null) {
        return criterion.text;
      }
      return criterion.text.replace("?", selectedText);
    }
    throw new UnsupportedOperationException("Unknown criterion type"); // $NON-NLS-1$
  }

  String getValueFromCriterion() {
    if (type == TYPE_UNIVERSE) {
      return null;
    }
    if (criterion instanceof MultipleSelectCriterion) {
      if (selectedIndex >= 0
          && ((MultipleSelectCriterion) criterion).entryValues != null
          && selectedIndex < ((MultipleSelectCriterion) criterion).entryValues.length) {
        return ((MultipleSelectCriterion) criterion).entryValues[selectedIndex];
      }
      return criterion.text;
    } else if (criterion instanceof TextInputCriterion) {
      return selectedText;
    }
    throw new UnsupportedOperationException("Unknown criterion type"); // $NON-NLS-1$
  }

  @Override
  public String toString() {
    return "CriterionInstance{"
        + "criterion="
        + criterion
        + ", selectedIndex="
        + selectedIndex
        + ", selectedText='"
        + selectedText
        + '\''
        + ", type="
        + type
        + ", end="
        + end
        + ", start="
        + start
        + ", max="
        + max
        + '}';
  }
}