package com.todoroo.astrid.core;

import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.TextInputCriterion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tasks.filters.FilterCriteriaProvider;
import timber.log.Timber;

public class CriterionInstance {

  public static final int TYPE_ADD = 0;
  public static final int TYPE_SUBTRACT = 1;
  public static final int TYPE_INTERSECT = 2;
  public static final int TYPE_UNIVERSE = 3;

  public static List<CriterionInstance> fromString(
      FilterCriteriaProvider provider, String criterion) {
    if (Strings.isNullOrEmpty(criterion)) {
      return Collections.emptyList();
    }
    List<CriterionInstance> entries = new ArrayList<>();
    for (String row : criterion.split("\n")) {
      CriterionInstance entry = new CriterionInstance();
      List<String> split =
          transform(
              Splitter.on(AndroidUtilities.SERIALIZATION_SEPARATOR).splitToList(row),
              CriterionInstance::unescape);
      if (split.size() != 4 && split.size() != 5) {
        Timber.e("invalid row: %s", row);
        return Collections.emptyList();
      }

      entry.criterion = provider.getFilterCriteria(split.get(0));
      String value = split.get(1);
      if (entry.criterion instanceof TextInputCriterion) {
        entry.selectedText = value;
      } else if (entry.criterion instanceof MultipleSelectCriterion) {
        MultipleSelectCriterion multipleSelectCriterion = (MultipleSelectCriterion) entry.criterion;
        if (multipleSelectCriterion.entryValues != null) {
          entry.selectedIndex = asList(multipleSelectCriterion.entryValues).indexOf(value);
        }
      } else {
        Timber.d("Ignored value %s for %s", value, entry.criterion);
      }
      entry.type = Integer.parseInt(split.get(3));
      entry.criterion.sql = split.get(4);
      Timber.d("%s -> %s", row, entry);
      entries.add(entry);
    }
    return entries;
  }

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
  public int start;

  public int max;

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

  public String getValueFromCriterion() {
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

  public static String escape(String item) {
    if (item == null) {
      return ""; // $NON-NLS-1$
    }
    return item.replace(
        AndroidUtilities.SERIALIZATION_SEPARATOR, AndroidUtilities.SEPARATOR_ESCAPE);
  }

  private static String unescape(String item) {
    if (Strings.isNullOrEmpty(item)) {
      return "";
    }
    return item.replace(
        AndroidUtilities.SEPARATOR_ESCAPE, AndroidUtilities.SERIALIZATION_SEPARATOR);
  }
}