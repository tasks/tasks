package com.todoroo.astrid.core;

import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static org.tasks.Strings.isNullOrEmpty;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.todoroo.andlib.utility.AndroidUtilities;
import com.todoroo.astrid.api.CustomFilterCriterion;
import com.todoroo.astrid.api.MultipleSelectCriterion;
import com.todoroo.astrid.api.TextInputCriterion;
import com.todoroo.astrid.helper.UUIDHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.tasks.filters.FilterCriteriaProvider;
import timber.log.Timber;

public class CriterionInstance {

  public static final int TYPE_ADD = 0;
  public static final int TYPE_SUBTRACT = 1;
  public static final int TYPE_INTERSECT = 2;
  public static final int TYPE_UNIVERSE = 3;
  public CustomFilterCriterion criterion;
  public int selectedIndex = -1;
  public String selectedText = null;
  public int type = TYPE_INTERSECT;
  public int end;
  public int start;
  public int max;
  private String id = UUIDHelper.newUUID();

  public CriterionInstance() {}

  public CriterionInstance(CriterionInstance other) {
    id = other.id;
    criterion = other.criterion;
    selectedIndex = other.selectedIndex;
    selectedText = other.selectedText;
    type = other.type;
    end = other.end;
    start = other.start;
    max = other.max;
  }

  public static List<CriterionInstance> fromString(
      FilterCriteriaProvider provider, String criterion) {
    if (isNullOrEmpty(criterion)) {
      return Collections.emptyList();
    }
    List<CriterionInstance> entries = new ArrayList<>();
    for (String row : criterion.split("\n")) {
      List<String> split =
          transform(
              Splitter.on(AndroidUtilities.SERIALIZATION_SEPARATOR).splitToList(row),
              CriterionInstance::unescape);
      if (split.size() != 4 && split.size() != 5) {
        Timber.e("invalid row: %s", row);
        return Collections.emptyList();
      }

      CriterionInstance entry = new CriterionInstance();
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

  private static String escape(String item) {
    if (item == null) {
      return ""; // $NON-NLS-1$
    }
    return item.replace(
        AndroidUtilities.SERIALIZATION_SEPARATOR, AndroidUtilities.SEPARATOR_ESCAPE);
  }

  private static String unescape(String item) {
    if (isNullOrEmpty(item)) {
      return "";
    }
    return item.replace(
        AndroidUtilities.SEPARATOR_ESCAPE, AndroidUtilities.SERIALIZATION_SEPARATOR);
  }

  public String getId() {
    return id;
  }

  public String getTitleFromCriterion() {
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CriterionInstance)) {
      return false;
    }
    CriterionInstance that = (CriterionInstance) o;
    return selectedIndex == that.selectedIndex
        && type == that.type
        && end == that.end
        && start == that.start
        && max == that.max
        && Objects.equals(criterion, that.criterion)
        && Objects.equals(selectedText, that.selectedText)
        && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(criterion, selectedIndex, selectedText, type, end, start, max, id);
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

  public static String serialize(List<CriterionInstance> criterion) {
    return Joiner.on("\n").join(transform(criterion, CriterionInstance::serialize));
  }

  private String serialize() {
    // criterion|entry|text|type|sql
    return Joiner.on(AndroidUtilities.SERIALIZATION_SEPARATOR)
        .join(
            asList(
                escape(criterion.identifier),
                escape(getValueFromCriterion()),
                escape(criterion.text),
                type,
                criterion.sql == null ? "" : criterion.sql));
  }
}
